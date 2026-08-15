package com.italiano2774.nativeapp;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Persistent learning state. v1.9 combines four-dimensional mastery with an
 * FSRS-inspired scheduler, adaptive new-word load, checkpoint exams and Room event logging.
 */
public class ProgressStore {
    public static final String DEFAULT_START="2026-08-15";
    public static final int DIM_MEANING=0, DIM_LISTENING=1, DIM_SPELLING=2, DIM_SPEAKING=3;
    public static final int PRON_ALWAYS=0, PRON_AUTO=1, PRON_TAP=2, PRON_NEVER=3;
    private final SharedPreferences p;
    private final Context context;

    public ProgressStore(Context c){context=c.getApplicationContext();p=context.getSharedPreferences("italiano2774_native",Context.MODE_PRIVATE);}
    private int clamp(int x,int lo,int hi){return Math.max(lo,Math.min(hi,x));}
    private int defaultInterval(int level){switch(level){case 1:return 1;case 2:return 3;case 3:return 7;case 4:return 14;case 5:return 30;default:return 1;}}
    private String dimPrefix(int dim){switch(dim){case DIM_LISTENING:return "dl_";case DIM_SPELLING:return "ds_";case DIM_SPEAKING:return "dp_";default:return "dm_";}}
    private String dimKey(int id,int dim){return dimPrefix(dim)+id;}

    // ---------- Overall + four-dimensional mastery ----------
    public int mastery(int id){return p.getInt("m_"+id,0);}
    public int dimensionLevel(int id,int dim){String k=dimKey(id,dim);return p.contains(k)?p.getInt(k,0):mastery(id);}
    public int meaningLevel(int id){return dimensionLevel(id,DIM_MEANING);}
    public int listeningLevel(int id){return dimensionLevel(id,DIM_LISTENING);}
    public int spellingLevel(int id){return dimensionLevel(id,DIM_SPELLING);}
    public int speakingLevel(int id){return dimensionLevel(id,DIM_SPEAKING);}

    public void setMastery(int id,int v){
        int level=clamp(v,0,5);SharedPreferences.Editor e=p.edit().putInt("m_"+id,level)
                .putInt(dimKey(id,DIM_MEANING),level).putInt(dimKey(id,DIM_LISTENING),level)
                .putInt(dimKey(id,DIM_SPELLING),level).putInt(dimKey(id,DIM_SPEAKING),level);
        if(level==0)e.remove("due_"+id).remove("iv_"+id).remove("fs_s_"+id).remove("fs_d_"+id);else{int iv=defaultInterval(level);e.putInt("iv_"+id,iv).putLong("due_"+id,LocalDate.now().plusDays(iv).toEpochDay()).putFloat("fs_s_"+id,Math.max(1f,iv)).putFloat("fs_d_"+id,5.5f);}
        e.apply();recordDailyCard();
    }

    public void setDimensions(int id,int meaning,int listening,int spelling,int speaking){
        p.edit().putInt(dimKey(id,DIM_MEANING),clamp(meaning,0,5)).putInt(dimKey(id,DIM_LISTENING),clamp(listening,0,5))
                .putInt(dimKey(id,DIM_SPELLING),clamp(spelling,0,5)).putInt(dimKey(id,DIM_SPEAKING),clamp(speaking,0,5)).apply();
        recomputeOverall(id);
    }

    private void recomputeOverall(int id){
        int m=meaningLevel(id),l=listeningLevel(id),s=spellingLevel(id),sp=speakingLevel(id);
        int overall=clamp((int)Math.round(m*0.40+l*0.25+s*0.20+sp*0.15),0,5);
        p.edit().putInt("m_"+id,overall).apply();
    }

    public int weakestDimension(int id){
        int[] v={meaningLevel(id),listeningLevel(id),spellingLevel(id),speakingLevel(id)};int idx=0;for(int i=1;i<v.length;i++)if(v[i]<v[idx])idx=i;return idx;
    }
    public String weakestDimensionName(int id){switch(weakestDimension(id)){case DIM_LISTENING:return "听力";case DIM_SPELLING:return "拼写";case DIM_SPEAKING:return "口语";default:return "识义";}}
    public boolean b1Ready(int id){return meaningLevel(id)>=4&&listeningLevel(id)>=3&&spellingLevel(id)>=2&&speakingLevel(id)>=2;}
    public int dimensionAverage(List<Word> words,int dim){if(words.isEmpty())return 0;long n=0;for(Word w:words)n+=dimensionLevel(w.id,dim);return (int)Math.round(n*100.0/(words.size()*5.0));}

    /** Records one exercise in one dimension and updates adaptive scheduling once. */
    public void recordDimensionResult(int id,int dim,boolean correct,long responseMs){recordDimensionResults(id,new int[]{dim},correct,responseMs);}
    public void recordDimensionResults(int id,int[] dims,boolean correct,long responseMs){
        SharedPreferences.Editor e=p.edit();
        for(int dim:dims){int cur=dimensionLevel(id,dim),next;if(correct){next=Math.min(5,cur+(responseMs>0&&responseMs>9000?0:1));if(next==0)next=1;}else next=Math.max(cur>0?1:0,cur-1);e.putInt(dimKey(id,dim),next);}e.apply();
        recomputeOverall(id);updateAdaptiveSchedule(id,correct,responseMs);recordDailyPractice(correct,responseMs);
        for(int dim:dims)LearningDatabase.log(context,"word",String.valueOf(id),dim,correct,responseMs,"dimension");
    }

    /** Backwards-compatible generic exercise = meaning recognition. */
    public void recordWrong(int id){recordDimensionResult(id,DIM_MEANING,false,0L);}
    public void recordCorrect(int id){recordDimensionResult(id,DIM_MEANING,true,0L);}
    public void recordPracticeResult(int id,boolean correct,long responseMs){recordDimensionResult(id,DIM_MEANING,correct,responseMs);}

    private void updateAdaptiveSchedule(int id,boolean correct,long responseMs){
        int attBefore=attempts(id),att=attBefore+1,cor=correctAnswers(id)+(correct?1:0);
        long oldAvg=avgResponseMs(id),avg=responseMs>0?(oldAvg<=0?responseMs:Math.round(oldAvg*0.75+responseMs*0.25)):oldAvg;
        LocalDate today=LocalDate.now(),last=lastReviewed(id);int elapsed=last==null?0:(int)Math.max(0,java.time.temporal.ChronoUnit.DAYS.between(last,today));
        int rating=!correct?1:(responseMs>0&&responseMs>9000?2:(responseMs>0&&responseMs<3000?4:3));
        FsrsScheduler.Result fs=FsrsScheduler.schedule(memoryStability(id),memoryDifficulty(id),rating,elapsed,desiredRetention());
        SharedPreferences.Editor e=p.edit().putInt("att_"+id,att).putInt("cor_"+id,cor).putLong("last_"+id,today.toEpochDay()).putLong("avgms_"+id,avg)
                .putFloat("fs_s_"+id,(float)fs.stability).putFloat("fs_d_"+id,(float)fs.difficulty).putInt("iv_"+id,fs.intervalDays)
                .putLong("due_"+id,today.plusDays(fs.intervalDays).toEpochDay());
        if(correct){e.putInt("w_"+id,Math.max(0,wrongCount(id)-1)).putInt("ok_"+id,correctStreak(id)+1);}
        else{e.putInt("w_"+id,wrongCount(id)+1).putInt("ok_"+id,0);}
        e.apply();
    }

    public double desiredRetention(){return Math.max(0.80,Math.min(0.97,p.getFloat("fs_retention",0.90f)));}
    public void setDesiredRetention(double value){p.edit().putFloat("fs_retention",(float)Math.max(0.80,Math.min(0.97,value))).apply();}
    public double memoryStability(int id){return p.getFloat("fs_s_"+id,0f);}
    public double memoryDifficulty(int id){return p.getFloat("fs_d_"+id,0f);}
    public int memoryRetrievability(int id){LocalDate last=lastReviewed(id);if(last==null||memoryStability(id)<=0)return mastery(id)>0?80:0;int elapsed=(int)Math.max(0,java.time.temporal.ChronoUnit.DAYS.between(last,LocalDate.now()));return (int)Math.round(FsrsScheduler.retrievability(memoryStability(id),elapsed)*100.0);}

    /** One-card study rating mainly trains meaning recall. */
    public void recordStudyRating(int id,int rating){
        rating=clamp(rating,0,3);int cur=meaningLevel(id),target;
        if(rating==0)target=Math.max(1,cur-1);else if(rating==1)target=Math.max(1,cur);else if(rating==2)target=Math.max(3,Math.min(5,cur+1));else target=Math.max(4,Math.min(5,cur+1));
        p.edit().putInt(dimKey(id,DIM_MEANING),target).apply();recomputeOverall(id);
        boolean correct=rating>=2;long ms=rating==3?2200:(rating==2?4500:10000);updateAdaptiveSchedule(id,correct,ms);recordDailyCard();LearningDatabase.log(context,"study_rating",String.valueOf(id),DIM_MEANING,correct,ms,"rating="+rating);
    }

    // ---------- Scheduling + metrics ----------
    public int intervalDays(int id){return p.getInt("iv_"+id,mastery(id)>0?defaultInterval(mastery(id)):1);}
    public long dueEpochDay(int id){return p.getLong("due_"+id,Long.MIN_VALUE);}
    public LocalDate nextDueDate(int id){long d=dueEpochDay(id);return d==Long.MIN_VALUE?null:LocalDate.ofEpochDay(d);}
    public boolean dueForReview(int id,LocalDate date){if(mastery(id)<=0)return false;long due=dueEpochDay(id);return due==Long.MIN_VALUE||due<=date.toEpochDay();}
    public int wrongCount(int id){return p.getInt("w_"+id,0);}
    public int correctStreak(int id){return p.getInt("ok_"+id,0);}
    public int attempts(int id){return p.getInt("att_"+id,0);}
    public int correctAnswers(int id){return p.getInt("cor_"+id,0);}
    public long avgResponseMs(int id){return p.getLong("avgms_"+id,0L);}
    public LocalDate lastReviewed(int id){long d=p.getLong("last_"+id,Long.MIN_VALUE);return d==Long.MIN_VALUE?null:LocalDate.ofEpochDay(d);}
    public int wrongTotal(List<Word> words){int n=0;for(Word w:words)if(wrongCount(w.id)>0)n++;return n;}
    public int dueCount(List<Word> words,LocalDate date){int n=0;for(Word w:words)if(dueForReview(w.id,date))n++;return n;}
    public boolean favorite(int id){return p.getBoolean("f_"+id,false);}
    public void setFavorite(int id,boolean v){p.edit().putBoolean("f_"+id,v).apply();}

    // ---------- Confusable-word tracking ----------
    private String confusionKey(int a,int b){int x=Math.min(a,b),y=Math.max(a,b);return "conf_"+x+"_"+y;}
    public void recordConfusion(int a,int b){if(a<=0||b<=0||a==b)return;String k=confusionKey(a,b);p.edit().putInt(k,p.getInt(k,0)+1).apply();}
    public int confusionScore(int a,int b){return p.getInt(confusionKey(a,b),0);}
    public List<int[]> learnedConfusions(){
        List<int[]> out=new ArrayList<>();for(Map.Entry<String,?> en:p.getAll().entrySet()){String k=en.getKey();if(!k.startsWith("conf_"))continue;try{String[] s=k.substring(5).split("_");int score=(Integer)en.getValue();out.add(new int[]{Integer.parseInt(s[0]),Integer.parseInt(s[1]),score});}catch(Exception ignored){}}
        out.sort((a,b)->Integer.compare(b[2],a[2]));return out;
    }

    // ---------- Placement test ----------
    public boolean placementCompleted(){return p.getBoolean("placement_done",false);}
    public int placementKnownEstimate(){return p.getInt("placement_known",0);}
    public void applyPlacement(List<Word> words,int knownCount,List<Integer> strongSampleIds){
        int n=Math.min(Math.max(0,knownCount),words.size());SharedPreferences.Editor e=p.edit();
        for(int i=0;i<n;i++){Word w=words.get(i);if(mastery(w.id)==0){e.putInt("m_"+w.id,2).putInt(dimKey(w.id,DIM_MEANING),3).putInt(dimKey(w.id,DIM_LISTENING),1).putInt(dimKey(w.id,DIM_SPELLING),1).putInt(dimKey(w.id,DIM_SPEAKING),1).putInt("iv_"+w.id,7).putLong("due_"+w.id,LocalDate.now().plusDays(7).toEpochDay());}}
        for(Integer id:strongSampleIds){e.putInt("m_"+id,3).putInt(dimKey(id,DIM_MEANING),4).putInt(dimKey(id,DIM_LISTENING),2).putInt(dimKey(id,DIM_SPELLING),2).putInt(dimKey(id,DIM_SPEAKING),1);}
        e.putBoolean("placement_done",true).putInt("placement_known",n).apply();
    }
    public void clearPlacement(){p.edit().remove("placement_done").remove("placement_known").apply();}

    // ---------- Time budget + pronunciation ----------
    public int sessionMinutes(){int v=p.getInt("session_minutes",30);return (v==5||v==15||v==30||v==60)?v:30;}
    public void setSessionMinutes(int m){if(m!=5&&m!=15&&m!=30&&m!=60)m=30;p.edit().putInt("session_minutes",m).apply();}
    public int pronunciationMode(){return clamp(p.getInt("pron_mode",PRON_AUTO),PRON_ALWAYS,PRON_NEVER);}
    public void setPronunciationMode(int m){p.edit().putInt("pron_mode",clamp(m,PRON_ALWAYS,PRON_NEVER)).apply();}
    public boolean shouldShowPronunciation(int id,boolean expandedOrAnswer){int mode=pronunciationMode();if(mode==PRON_ALWAYS)return true;if(mode==PRON_NEVER)return false;if(mode==PRON_TAP)return expandedOrAnswer;return mastery(id)<2;}

    // ---------- Daily statistics ----------
    private String dayKey(String prefix,LocalDate d){return prefix+d.toString();}
    private void recordDailyCard(){LocalDate d=LocalDate.now();p.edit().putInt(dayKey("day_cards_",d),dailyCards(d)+1).apply();}
    private void recordDailyPractice(boolean correct,long responseMs){LocalDate d=LocalDate.now();SharedPreferences.Editor e=p.edit().putInt(dayKey("day_att_",d),dailyAttempts(d)+1);if(correct)e.putInt(dayKey("day_cor_",d),dailyCorrect(d)+1);if(responseMs>0)e.putLong(dayKey("day_ms_",d),dailyResponseMs(d)+responseMs);e.apply();}
    public int dailyCards(LocalDate d){return p.getInt(dayKey("day_cards_",d),0);}
    public int dailyAttempts(LocalDate d){return p.getInt(dayKey("day_att_",d),0);}
    public int dailyCorrect(LocalDate d){return p.getInt(dayKey("day_cor_",d),0);}
    public long dailyResponseMs(LocalDate d){return p.getLong(dayKey("day_ms_",d),0L);}
    public int dailyActivity(LocalDate d){return dailyCards(d)+dailyAttempts(d);}
    public int dailyAccuracy(LocalDate d){int a=dailyAttempts(d);return a==0?0:(int)Math.round(dailyCorrect(d)*100.0/a);}
    public long dailyAvgResponseMs(LocalDate d){int a=dailyAttempts(d);return a==0?0:dailyResponseMs(d)/a;}
    public int sevenDayAccuracy(){int a=0,c=0;LocalDate d=LocalDate.now();for(int i=0;i<7;i++){a+=dailyAttempts(d.minusDays(i));c+=dailyCorrect(d.minusDays(i));}return a==0?85:(int)Math.round(c*100.0/a);}
    public int sevenDayAttempts(){int a=0;LocalDate d=LocalDate.now();for(int i=0;i<7;i++)a+=dailyAttempts(d.minusDays(i));return a;}
    public int recommendedNewWords(List<Word> words){int base=sessionMinutes()==5?3:(sessionMinutes()==15?8:(sessionMinutes()==60?28:15));int acc=sevenDayAccuracy(),due=dueCount(words,LocalDate.now()),wrong=wrongTotal(words);double factor=acc<70?0.45:(acc<80?0.70:(acc>=92?1.18:1.0));if(due>60)factor*=0.55;else if(due>35)factor*=0.72;else if(due>20)factor*=0.88;if(wrong>25)factor*=0.8;int q=(int)Math.round(base*factor);return Math.max(2,Math.min(sessionMinutes()==60?35:24,q));}
    public int activityStreak(){LocalDate d=LocalDate.now();int n=0;if(dailyActivity(d)==0)d=d.minusDays(1);for(int i=0;i<365;i++){if(dailyActivity(d)>0){n++;d=d.minusDays(1);}else break;}return n;}
    public int totalAttempts(List<Word> words){int n=0;for(Word w:words)n+=attempts(w.id);return n;}
    public int totalCorrect(List<Word> words){int n=0;for(Word w:words)n+=correctAnswers(w.id);return n;}
    public int totalAccuracy(List<Word> words){int a=totalAttempts(words);return a==0?0:(int)Math.round(totalCorrect(words)*100.0/a);}

    // ---------- Sentence-pattern / dialogue practice ----------
    private String auxKey(String type,String suffix){return "aux_"+type+"_"+suffix;}
    public void recordAuxiliaryResult(String type,boolean correct,long responseMs){
        int att=auxiliaryAttempts(type)+1,cor=auxiliaryCorrect(type)+(correct?1:0);
        p.edit().putInt(auxKey(type,"att"),att).putInt(auxKey(type,"cor"),cor).apply();
        recordDailyPractice(correct,responseMs);LearningDatabase.log(context,"aux",type,-1,correct,responseMs,"");
    }
    public int auxiliaryAttempts(String type){return p.getInt(auxKey(type,"att"),0);}
    public int auxiliaryCorrect(String type){return p.getInt(auxKey(type,"cor"),0);}
    public int auxiliaryAccuracy(String type){int a=auxiliaryAttempts(type);return a==0?0:(int)Math.round(auxiliaryCorrect(type)*100.0/a);}

    // ---------- Grammar weakness diagnosis ----------
    private String grammarKey(String id,String suffix){return "gram_"+id+"_"+suffix;}
    public void recordGrammarResult(String id,boolean correct,long responseMs){
        if(id==null||id.trim().isEmpty())return;int att=grammarAttempts(id)+1,cor=grammarCorrect(id)+(correct?1:0);
        p.edit().putInt(grammarKey(id,"att"),att).putInt(grammarKey(id,"cor"),cor).putLong(grammarKey(id,"last"),LocalDate.now().toEpochDay()).apply();
        LearningDatabase.log(context,"grammar",id,-1,correct,responseMs,"");
    }
    public void recordPatternResult(String patternId,boolean correct,long responseMs){recordAuxiliaryResult("pattern",correct,responseMs);recordGrammarResult(patternId,correct,responseMs);}
    public int grammarAttempts(String id){return p.getInt(grammarKey(id,"att"),0);}
    public int grammarCorrect(String id){return p.getInt(grammarKey(id,"cor"),0);}
    public int grammarMistakes(String id){return Math.max(0,grammarAttempts(id)-grammarCorrect(id));}
    public int grammarAccuracy(String id){int a=grammarAttempts(id);return a==0?0:(int)Math.round(grammarCorrect(id)*100.0/a);}

    // ---------- CEFR checkpoint exams ----------
    public void saveExamScore(String level,int score){if(level==null)return;String k="exam_"+level.toUpperCase(java.util.Locale.ROOT);int best=Math.max(score,p.getInt(k+"_best",0));p.edit().putInt(k+"_last",score).putInt(k+"_best",best).putLong(k+"_day",LocalDate.now().toEpochDay()).apply();LearningDatabase.log(context,"exam",level,-1,score>=70,0,"score="+score);}
    public int lastExamScore(String level){return p.getInt("exam_"+level.toUpperCase(java.util.Locale.ROOT)+"_last",0);}
    public int bestExamScore(String level){return p.getInt("exam_"+level.toUpperCase(java.util.Locale.ROOT)+"_best",0);}

    // ---------- Graded reading ----------
    private String readingKey(String id){return "read_"+id+"_best";}
    public int readingBest(String id){return p.getInt(readingKey(id),0);}
    public void saveReadingScore(String id,int score){if(id==null||id.isEmpty())return;int best=Math.max(score,readingBest(id));p.edit().putInt(readingKey(id),best).apply();LearningDatabase.log(context,"reading",id,-1,score>=70,0,"score="+score);}

    // ---------- General settings ----------
    public LocalDate startDate(){try{return LocalDate.parse(p.getString("start",DEFAULT_START));}catch(Exception e){return LocalDate.parse(DEFAULT_START);}}
    public void setStartDate(LocalDate d){p.edit().putString("start",d.toString()).apply();}
    public int perDay(){return p.getInt("perDay",30);}
    public void setPerDay(int n){p.edit().putInt("perDay",n).apply();}
    public boolean preferOriginalAudio(){return p.getBoolean("originalAudio",true);}
    public void setPreferOriginalAudio(boolean b){p.edit().putBoolean("originalAudio",b).apply();}
    public boolean reminderEnabled(){return p.getBoolean("reminderEnabled",false);}
    public void setReminderEnabled(boolean b){p.edit().putBoolean("reminderEnabled",b).apply();}
    public int reminderHour(){return p.getInt("reminderHour",19);}
    public int reminderMinute(){return p.getInt("reminderMinute",0);}
    public void setReminderTime(int h,int m){p.edit().putInt("reminderHour",h).putInt("reminderMinute",m).apply();}

    public int countAtLeast(List<Word> words,int level){int n=0;for(Word w:words)if(mastery(w.id)>=level)n++;return n;}
    public int b1Count(List<Word> words){int n=0;for(Word w:words)if(b1Ready(w.id))n++;return n;}
    public int strongCount(List<Word> words){return countAtLeast(words,5);}
    public int introducedCount(List<Word> words){return countAtLeast(words,1);}

    // ---------- Backup ----------
    public JSONObject exportJson(List<Word> words) throws Exception {
        JSONObject o=new JSONObject();o.put("version",10);o.put("startDate",startDate().toString());o.put("perDay",perDay());o.put("preferOriginalAudio",preferOriginalAudio());
        o.put("reminderEnabled",reminderEnabled());o.put("reminderHour",reminderHour());o.put("reminderMinute",reminderMinute());o.put("sessionMinutes",sessionMinutes());o.put("pronMode",pronunciationMode());o.put("placementDone",placementCompleted());o.put("placementKnown",placementKnownEstimate());o.put("desiredRetention",desiredRetention());
        JSONArray m=new JSONArray(),f=new JSONArray(),wj=new JSONArray(),due=new JSONArray(),metrics=new JSONArray(),dims=new JSONArray(),conf=new JSONArray(),fsrs=new JSONArray();
        for(Word word:words){int v=mastery(word.id);if(v>0){JSONObject x=new JSONObject();x.put("id",word.id);x.put("level",v);m.put(x);}if(favorite(word.id))f.put(word.id);int wc=wrongCount(word.id);if(wc>0){JSONObject x=new JSONObject();x.put("id",word.id);x.put("count",wc);wj.put(x);}long d=dueEpochDay(word.id);if(d!=Long.MIN_VALUE){JSONObject x=new JSONObject();x.put("id",word.id);x.put("day",d);due.put(x);}if(v>0||p.contains(dimKey(word.id,DIM_MEANING))){JSONObject x=new JSONObject();x.put("id",word.id);x.put("meaning",meaningLevel(word.id));x.put("listening",listeningLevel(word.id));x.put("spelling",spellingLevel(word.id));x.put("speaking",speakingLevel(word.id));dims.put(x);}if(attempts(word.id)>0||p.contains("iv_"+word.id)){JSONObject x=new JSONObject();x.put("id",word.id);x.put("interval",intervalDays(word.id));x.put("attempts",attempts(word.id));x.put("correct",correctAnswers(word.id));x.put("avgMs",avgResponseMs(word.id));x.put("last",p.getLong("last_"+word.id,Long.MIN_VALUE));metrics.put(x);}if(memoryStability(word.id)>0){JSONObject x=new JSONObject();x.put("id",word.id);x.put("stability",memoryStability(word.id));x.put("difficulty",memoryDifficulty(word.id));fsrs.put(x);}}
        for(int[] c:learnedConfusions()){JSONObject x=new JSONObject();x.put("a",c[0]);x.put("b",c[1]);x.put("score",c[2]);conf.put(x);}
        JSONArray daily=new JSONArray();LocalDate today=LocalDate.now();for(int i=0;i<180;i++){LocalDate d=today.minusDays(i);if(dailyActivity(d)>0){JSONObject x=new JSONObject();x.put("date",d.toString());x.put("cards",dailyCards(d));x.put("attempts",dailyAttempts(d));x.put("correct",dailyCorrect(d));x.put("ms",dailyResponseMs(d));daily.put(x);}}
        JSONObject aux=new JSONObject();for(String type:new String[]{"pattern","dialogue","freechat","shadowing","pronunciation","level_exam","reading"}){JSONObject x=new JSONObject();x.put("attempts",auxiliaryAttempts(type));x.put("correct",auxiliaryCorrect(type));aux.put(type,x);}
        JSONArray grammar=new JSONArray();for(GrammarPoint gp:GrammarDiagnostics.all()){if(grammarAttempts(gp.id)>0){JSONObject x=new JSONObject();x.put("id",gp.id);x.put("attempts",grammarAttempts(gp.id));x.put("correct",grammarCorrect(gp.id));grammar.put(x);}}
        JSONObject exams=new JSONObject();for(String level:new String[]{"A1","A2","B1"}){JSONObject x=new JSONObject();x.put("last",lastExamScore(level));x.put("best",bestExamScore(level));exams.put(level,x);}JSONObject readingScores=new JSONObject();for(Map.Entry<String,?> en:p.getAll().entrySet()){String k=en.getKey();if(k.startsWith("read_")&&k.endsWith("_best")){String id=k.substring(5,k.length()-5);Object val=en.getValue();if(val instanceof Integer)readingScores.put(id,(Integer)val);}}o.put("readingScores",readingScores);o.put("examStats",exams);o.put("auxStats",aux);o.put("grammarStats",grammar);o.put("fsrs",fsrs);o.put("mastery",m);o.put("dimensions",dims);o.put("favorites",f);o.put("wrong",wj);o.put("due",due);o.put("metrics",metrics);o.put("confusions",conf);o.put("daily",daily);return o;
    }

    public void importJson(JSONObject o) throws Exception {
        SharedPreferences.Editor e=p.edit().clear();e.putString("start",o.optString("startDate",DEFAULT_START)).putInt("perDay",o.optInt("perDay",30)).putBoolean("originalAudio",o.optBoolean("preferOriginalAudio",true));
        e.putBoolean("reminderEnabled",o.optBoolean("reminderEnabled",false)).putInt("reminderHour",o.optInt("reminderHour",19)).putInt("reminderMinute",o.optInt("reminderMinute",0)).putInt("session_minutes",o.optInt("sessionMinutes",30)).putInt("pron_mode",o.optInt("pronMode",PRON_AUTO)).putBoolean("placement_done",o.optBoolean("placementDone",false)).putInt("placement_known",o.optInt("placementKnown",0)).putFloat("fs_retention",(float)o.optDouble("desiredRetention",0.90));
        JSONArray m=o.optJSONArray("mastery");if(m!=null)for(int i=0;i<m.length();i++){JSONObject x=m.getJSONObject(i);e.putInt("m_"+x.getInt("id"),x.getInt("level"));}
        JSONArray dims=o.optJSONArray("dimensions");if(dims!=null)for(int i=0;i<dims.length();i++){JSONObject x=dims.getJSONObject(i);int id=x.getInt("id");e.putInt(dimKey(id,DIM_MEANING),x.optInt("meaning",0));e.putInt(dimKey(id,DIM_LISTENING),x.optInt("listening",0));e.putInt(dimKey(id,DIM_SPELLING),x.optInt("spelling",0));e.putInt(dimKey(id,DIM_SPEAKING),x.optInt("speaking",0));}
        JSONArray f=o.optJSONArray("favorites");if(f!=null)for(int i=0;i<f.length();i++)e.putBoolean("f_"+f.getInt(i),true);
        JSONArray wj=o.optJSONArray("wrong");if(wj!=null)for(int i=0;i<wj.length();i++){JSONObject x=wj.getJSONObject(i);e.putInt("w_"+x.getInt("id"),x.getInt("count"));}
        JSONArray due=o.optJSONArray("due");if(due!=null)for(int i=0;i<due.length();i++){JSONObject x=due.getJSONObject(i);e.putLong("due_"+x.getInt("id"),x.getLong("day"));}
        JSONArray metrics=o.optJSONArray("metrics");if(metrics!=null)for(int i=0;i<metrics.length();i++){JSONObject x=metrics.getJSONObject(i);int id=x.getInt("id");e.putInt("iv_"+id,x.optInt("interval",1));e.putInt("att_"+id,x.optInt("attempts",0));e.putInt("cor_"+id,x.optInt("correct",0));e.putLong("avgms_"+id,x.optLong("avgMs",0));long last=x.optLong("last",Long.MIN_VALUE);if(last!=Long.MIN_VALUE)e.putLong("last_"+id,last);}
        JSONArray fsrs=o.optJSONArray("fsrs");if(fsrs!=null)for(int i=0;i<fsrs.length();i++){JSONObject x=fsrs.getJSONObject(i);int id=x.getInt("id");e.putFloat("fs_s_"+id,(float)x.optDouble("stability",0));e.putFloat("fs_d_"+id,(float)x.optDouble("difficulty",5.5));}
        JSONArray conf=o.optJSONArray("confusions");if(conf!=null)for(int i=0;i<conf.length();i++){JSONObject x=conf.getJSONObject(i);e.putInt(confusionKey(x.getInt("a"),x.getInt("b")),x.optInt("score",1));}
        JSONArray daily=o.optJSONArray("daily");if(daily!=null)for(int i=0;i<daily.length();i++){JSONObject x=daily.getJSONObject(i);LocalDate d=LocalDate.parse(x.getString("date"));e.putInt(dayKey("day_cards_",d),x.optInt("cards",0));e.putInt(dayKey("day_att_",d),x.optInt("attempts",0));e.putInt(dayKey("day_cor_",d),x.optInt("correct",0));e.putLong(dayKey("day_ms_",d),x.optLong("ms",0));}
        JSONObject aux=o.optJSONObject("auxStats");if(aux!=null)for(String type:new String[]{"pattern","dialogue","freechat","shadowing","pronunciation","level_exam","reading"}){JSONObject x=aux.optJSONObject(type);if(x!=null){e.putInt(auxKey(type,"att"),x.optInt("attempts",0));e.putInt(auxKey(type,"cor"),x.optInt("correct",0));}}
        JSONArray grammar=o.optJSONArray("grammarStats");if(grammar!=null)for(int i=0;i<grammar.length();i++){JSONObject x=grammar.getJSONObject(i);String id=x.optString("id");if(!id.isEmpty()){e.putInt(grammarKey(id,"att"),x.optInt("attempts",0));e.putInt(grammarKey(id,"cor"),x.optInt("correct",0));}}JSONObject exams=o.optJSONObject("examStats");if(exams!=null)for(String level:new String[]{"A1","A2","B1"}){JSONObject x=exams.optJSONObject(level);if(x!=null){String k="exam_"+level;e.putInt(k+"_last",x.optInt("last",0));e.putInt(k+"_best",x.optInt("best",0));}}JSONObject readingScores=o.optJSONObject("readingScores");if(readingScores!=null){java.util.Iterator<String> keys=readingScores.keys();while(keys.hasNext()){String id=keys.next();e.putInt(readingKey(id),readingScores.optInt(id,0));}}e.apply();
    }

    public void reset(){String start=startDate().toString();int per=perDay(),minutes=sessionMinutes(),pron=pronunciationMode();double retention=desiredRetention();boolean audio=preferOriginalAudio(),reminder=reminderEnabled();int rh=reminderHour(),rm=reminderMinute();p.edit().clear().putString("start",start).putInt("perDay",per).putBoolean("originalAudio",audio).putBoolean("reminderEnabled",reminder).putInt("reminderHour",rh).putInt("reminderMinute",rm).putInt("session_minutes",minutes).putInt("pron_mode",pron).putFloat("fs_retention",(float)retention).apply();LearningDatabase.clear(context);}
}
