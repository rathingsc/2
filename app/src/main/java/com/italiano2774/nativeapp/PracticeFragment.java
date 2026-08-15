package com.italiano2774.nativeapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class PracticeFragment extends Fragment {
    public static PracticeFragment newInstance(String mode){PracticeFragment f=new PracticeFragment();Bundle b=new Bundle();b.putString("initialMode",mode==null?"review":mode);f.setArguments(b);return f;}
    private static final int MODE_REVIEW=0,MODE_LISTEN=1,MODE_ZH=2,MODE_WRONG=3,MODE_SPELL=4,MODE_DICTATION=5,MODE_SPEAKING=6,MODE_CONFUSION=7;
    private static final int REQ_AUDIO=551;
    private WordRepository repo;private ProgressStore progress;private AudioPlayer audio;private int mode=MODE_REVIEW;
    private TextView stats,modeText,question,hint,feedback;private MaterialButton replay,next,submit,speakButton;private EditText input;private LinearLayout choicesPanel,typingPanel;
    private final List<MaterialButton> answers=new ArrayList<>();private final Random random=new Random();
    private List<Word> pool=new ArrayList<>();private Word current;private boolean answered=false;private long questionStarted=0L;private SpeechRecognizer recognizer;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_practice,container,false);repo=WordRepository.get(requireContext());progress=new ProgressStore(requireContext());audio=new AudioPlayer(requireContext(),progress);
        stats=v.findViewById(R.id.text_practice_stats);modeText=v.findViewById(R.id.text_mode);question=v.findViewById(R.id.text_question);hint=v.findViewById(R.id.text_hint);feedback=v.findViewById(R.id.text_feedback);replay=v.findViewById(R.id.button_replay);next=v.findViewById(R.id.button_next_question);speakButton=v.findViewById(R.id.button_speak_answer);
        choicesPanel=v.findViewById(R.id.panel_choices);typingPanel=v.findViewById(R.id.panel_typing);input=v.findViewById(R.id.edit_answer);submit=v.findViewById(R.id.button_submit_answer);
        answers.add(v.findViewById(R.id.answer_1));answers.add(v.findViewById(R.id.answer_2));answers.add(v.findViewById(R.id.answer_3));answers.add(v.findViewById(R.id.answer_4));
        v.findViewById(R.id.button_mode_review).setOnClickListener(x->setMode(MODE_REVIEW));v.findViewById(R.id.button_mode_listen).setOnClickListener(x->setMode(MODE_LISTEN));v.findViewById(R.id.button_mode_zh).setOnClickListener(x->setMode(MODE_ZH));v.findViewById(R.id.button_mode_wrong).setOnClickListener(x->setMode(MODE_WRONG));v.findViewById(R.id.button_mode_spell).setOnClickListener(x->setMode(MODE_SPELL));v.findViewById(R.id.button_mode_dictation).setOnClickListener(x->setMode(MODE_DICTATION));v.findViewById(R.id.button_mode_speaking).setOnClickListener(x->setMode(MODE_SPEAKING));v.findViewById(R.id.button_mode_confusion).setOnClickListener(x->setMode(MODE_CONFUSION));v.findViewById(R.id.button_mode_scenario).setOnClickListener(x->((MainActivity)requireActivity()).openScenarios());v.findViewById(R.id.button_mode_pattern).setOnClickListener(x->((MainActivity)requireActivity()).openSentencePatterns());v.findViewById(R.id.button_mode_dialogue).setOnClickListener(x->((MainActivity)requireActivity()).openDialogueTraining());v.findViewById(R.id.button_mode_commute).setOnClickListener(x->((MainActivity)requireActivity()).openCommute());v.findViewById(R.id.button_mode_freechat).setOnClickListener(x->((MainActivity)requireActivity()).openFreeConversation());v.findViewById(R.id.button_mode_grammar_diag).setOnClickListener(x->((MainActivity)requireActivity()).openGrammarDiagnosis());v.findViewById(R.id.button_mode_personal_course).setOnClickListener(x->((MainActivity)requireActivity()).openPersonalCourse());v.findViewById(R.id.button_mode_shadowing).setOnClickListener(x->((MainActivity)requireActivity()).openShadowing());v.findViewById(R.id.button_mode_pronunciation).setOnClickListener(x->((MainActivity)requireActivity()).openPronunciation());v.findViewById(R.id.button_mode_level_exam).setOnClickListener(x->((MainActivity)requireActivity()).openLevelExam());v.findViewById(R.id.button_mode_reading).setOnClickListener(x->((MainActivity)requireActivity()).openReadingList());v.findViewById(R.id.button_mode_weakness_center).setOnClickListener(x->((MainActivity)requireActivity()).openWeaknessCenter());v.findViewById(R.id.button_mode_emergency).setOnClickListener(x->((MainActivity)requireActivity()).openEmergency());v.findViewById(R.id.button_mode_custom_library).setOnClickListener(x->((MainActivity)requireActivity()).openCustomLibrary());
        replay.setOnClickListener(x->{if(current!=null)audio.play(current);});next.setOnClickListener(x->newQuestion());submit.setOnClickListener(x->submitTyped());speakButton.setOnClickListener(x->requestOrStartSpeech());
        input.setOnEditorActionListener((tv,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){submitTyped();return true;}return false;});String initial=getArguments()==null?"review":getArguments().getString("initialMode","review");setMode(modeFromString(initial));return v;
    }

    private int modeFromString(String s){if(s==null)return MODE_REVIEW;switch(s){case "listen":return MODE_LISTEN;case "zh":return MODE_ZH;case "wrong":return MODE_WRONG;case "spell":return MODE_SPELL;case "dictation":return MODE_DICTATION;case "speaking":return MODE_SPEAKING;case "confusion":return MODE_CONFUSION;default:return MODE_REVIEW;}}

    private void setMode(int m){
        mode=m;
        if(mode==MODE_REVIEW){pool=repo.reviewDue(progress,LocalDate.now());modeText.setText("今日复习 · 到期优先");}
        else if(mode==MODE_LISTEN){pool=repo.weaknessPool(progress,ProgressStore.DIM_LISTENING);modeText.setText("听音选词 · 听力专项");}
        else if(mode==MODE_ZH){pool=repo.weaknessPool(progress,ProgressStore.DIM_MEANING);modeText.setText("中文 → 意大利语 · 识义专项");}
        else if(mode==MODE_WRONG){pool=repo.wrongWords(progress);modeText.setText("错词本 · 优先修复");}
        else if(mode==MODE_SPELL){pool=repo.weaknessPool(progress,ProgressStore.DIM_SPELLING);modeText.setText("拼写练习 · 拼写专项");}
        else if(mode==MODE_DICTATION){pool=repo.weaknessPool(progress,ProgressStore.DIM_LISTENING);modeText.setText("听写 · 听力 + 拼写");}
        else if(mode==MODE_SPEAKING){pool=repo.weaknessPool(progress,ProgressStore.DIM_SPEAKING);modeText.setText("口语跟读 · 意大利语识别");}
        else{pool=repo.confusionPool(progress);modeText.setText("易混词专项 · 对比记忆");}
        if(pool==null||pool.isEmpty())pool=repo.quizPool(progress);refreshStats();newQuestion();
    }

    private void refreshStats(){LocalDate now=LocalDate.now();stats.setText("待复习 "+progress.dueCount(repo.all(),now)+" · 错词 "+progress.wrongTotal(repo.all())+" · 今日正确率 "+progress.dailyAccuracy(now)+"% · 自由对话 "+progress.auxiliaryAttempts("freechat")+"轮\n四维：识义 "+progress.dimensionAverage(repo.all(),ProgressStore.DIM_MEANING)+"% · 听力 "+progress.dimensionAverage(repo.all(),ProgressStore.DIM_LISTENING)+"% · 拼写 "+progress.dimensionAverage(repo.all(),ProgressStore.DIM_SPELLING)+"% · 口语 "+progress.dimensionAverage(repo.all(),ProgressStore.DIM_SPEAKING)+"%");}
    private boolean typingMode(){return mode==MODE_SPELL||mode==MODE_DICTATION;}
    private boolean speakingMode(){return mode==MODE_SPEAKING;}

    private void newQuestion(){
        answered=false;feedback.setText("");next.setVisibility(View.GONE);input.setText("");input.setEnabled(true);submit.setEnabled(true);speakButton.setEnabled(true);
        choicesPanel.setVisibility((typingMode()||speakingMode())?View.GONE:View.VISIBLE);typingPanel.setVisibility(typingMode()?View.VISIBLE:View.GONE);speakButton.setVisibility(speakingMode()?View.VISIBLE:View.GONE);
        for(MaterialButton b:answers){b.setEnabled(true);b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.line)));b.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_primary));}
        if(pool==null||pool.isEmpty()){current=null;question.setText("这一专项目前没有待练内容 🎉");hint.setText("可以切换其他模式继续训练。");replay.setVisibility(View.GONE);choicesPanel.setVisibility(View.GONE);typingPanel.setVisibility(View.GONE);speakButton.setVisibility(View.GONE);return;}
        current=pool.get(random.nextInt(pool.size()));questionStarted=System.currentTimeMillis();
        if(speakingMode()){question.setText(current.word);hint.setText(safeChinese(current)+"\n先听标准发音，再按“开始跟读”。识别语言：Italiano (Italia)");replay.setVisibility(View.VISIBLE);question.postDelayed(()->{if(current!=null)audio.play(current);},250);return;}
        if(typingMode()){
            if(mode==MODE_DICTATION){question.setText("听发音，把你听到的意大利语写出来");hint.setText("同时训练听力和拼写；大小写不计，重音符号暂不强制。");replay.setVisibility(View.VISIBLE);question.postDelayed(()->{if(current!=null)audio.play(current);},250);}else{question.setText(safeChinese(current));hint.setText("请完整输入对应的意大利语单词或短语。");replay.setVisibility(View.GONE);}input.requestFocus();return;
        }
        List<Word> options=mode==MODE_CONFUSION?makeConfusionOptions(current):makeOptions(current,pool);
        if(mode==MODE_LISTEN){question.setText("听发音，选择你听到的意大利语");hint.setText("只靠声音判断，训练听力识别。");replay.setVisibility(View.VISIBLE);for(int i=0;i<4;i++)bindAnswer(answers.get(i),options.get(i),options.get(i).word);question.postDelayed(()->{if(current!=null)audio.play(current);},250);}
        else if(mode==MODE_ZH){question.setText(safeChinese(current));hint.setText("选择对应的意大利语。");replay.setVisibility(View.GONE);for(int i=0;i<4;i++)bindAnswer(answers.get(i),options.get(i),options.get(i).word);}
        else if(mode==MODE_CONFUSION){Word partner=repo.confusionPartner(current,progress);question.setText(safeChinese(current));hint.setText(partner==null?"选择正确词。":"重点对比："+current.word+" ↔ "+partner.word);replay.setVisibility(View.GONE);for(int i=0;i<4;i++)bindAnswer(answers.get(i),options.get(i),options.get(i).word);}
        else{question.setText(current.word);hint.setText(current.ipa+(progress.shouldShowPronunciation(current.id,false)?("  ·  "+current.zhPron):""));replay.setVisibility(View.VISIBLE);for(int i=0;i<4;i++)bindAnswer(answers.get(i),options.get(i),safeChinese(options.get(i)));}
    }

    private String safeChinese(Word w){return w.chinese==null||w.chinese.trim().isEmpty()?w.english:w.chinese;}
    private void bindAnswer(MaterialButton b,Word w,String label){b.setText(label);b.setTag(w.id);b.setOnClickListener(v->answerChoice(b,w));}
    private List<Word> makeOptions(Word target,List<Word> source){List<Word> candidates=new ArrayList<>();for(Word w:source)if(w.id!=target.id&&w.level.equals(target.level))candidates.add(w);if(candidates.size()<3)for(Word w:repo.all())if(w.id!=target.id&&!candidates.contains(w))candidates.add(w);Collections.shuffle(candidates,random);List<Word> out=new ArrayList<>();out.add(target);for(Word w:candidates){if(out.size()>=4)break;out.add(w);}Collections.shuffle(out,random);return out;}
    private List<Word> makeConfusionOptions(Word target){List<Word> out=new ArrayList<>();out.add(target);Word partner=repo.confusionPartner(target,progress);if(partner!=null&&partner.id!=target.id)out.add(partner);List<Word> rest=new ArrayList<>(repo.confusionPool(progress));Collections.shuffle(rest,random);for(Word w:rest){if(out.size()>=4)break;if(w.id!=target.id&&!out.contains(w))out.add(w);}while(out.size()<4){Word w=repo.all().get(random.nextInt(repo.size()));if(w.id!=target.id&&!out.contains(w))out.add(w);}Collections.shuffle(out,random);return out;}

    private void recordCurrent(boolean correct,long ms){
        if(mode==MODE_LISTEN)progress.recordDimensionResult(current.id,ProgressStore.DIM_LISTENING,correct,ms);
        else if(mode==MODE_SPELL)progress.recordDimensionResult(current.id,ProgressStore.DIM_SPELLING,correct,ms);
        else if(mode==MODE_DICTATION)progress.recordDimensionResults(current.id,new int[]{ProgressStore.DIM_LISTENING,ProgressStore.DIM_SPELLING},correct,ms);
        else if(mode==MODE_SPEAKING)progress.recordDimensionResult(current.id,ProgressStore.DIM_SPEAKING,correct,ms);
        else progress.recordDimensionResult(current.id,ProgressStore.DIM_MEANING,correct,ms);
    }
    private void answerChoice(MaterialButton clicked,Word chosen){if(answered||current==null)return;boolean correct=chosen.id==current.id;long ms=System.currentTimeMillis()-questionStarted;answered=true;if(!correct)progress.recordConfusion(current.id,chosen.id);recordCurrent(correct,ms);showFeedback(correct,ms);clicked.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),correct?R.color.success:R.color.error)));if(!correct)for(MaterialButton b:answers){Object tag=b.getTag();if(tag instanceof Integer&&(Integer)tag==current.id)b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.success)));}for(MaterialButton b:answers)b.setEnabled(false);finishQuestion();}
    private void submitTyped(){if(answered||current==null)return;String typed=input.getText()==null?"":input.getText().toString();if(typed.trim().isEmpty()){feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.error));feedback.setText("请先输入答案");return;}boolean correct=normalize(typed).equals(normalize(current.word));long ms=System.currentTimeMillis()-questionStarted;answered=true;recordCurrent(correct,ms);showFeedback(correct,ms);input.setEnabled(false);submit.setEnabled(false);finishQuestion();}

    private void requestOrStartSpeech(){if(current==null||answered)return;if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}startSpeech();}
    private void startSpeech(){
        if(!SpeechRecognizer.isRecognitionAvailable(requireContext())){feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.error));feedback.setText("这台手机暂时没有可用的语音识别服务。可以继续使用听力、听写和拼写模式。");return;}audio.stop();
        if(recognizer==null){recognizer=SpeechRecognizer.createSpeechRecognizer(requireContext());recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle p){feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.blue));feedback.setText("🎙 正在听，请说意大利语…");}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){feedback.setText("正在识别…");}public void onError(int e){if(!isAdded())return;feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.error));feedback.setText(speechError(e)+"\n可以再按一次“开始跟读”。");speakButton.setEnabled(true);}public void onResults(Bundle b){handleSpeechResults(b);}public void onPartialResults(Bundle b){}public void onEvent(int type,Bundle b){}});}
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"it-IT");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"it-IT");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);speakButton.setEnabled(false);recognizer.startListening(i);
    }
    private void handleSpeechResults(Bundle b){if(!isAdded()||current==null||answered)return;ArrayList<String> list=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(list==null||list.isEmpty()){feedback.setText("没有识别到内容，请再试一次。");speakButton.setEnabled(true);return;}String best=list.get(0);int bestScore=-1;for(String s:list){int score=similarity(normalize(current.word),normalize(s));if(score>bestScore){bestScore=score;best=s;}}long ms=System.currentTimeMillis()-questionStarted;boolean correct=bestScore>=82;answered=true;recordCurrent(correct,ms);feedback.setTextColor(ContextCompat.getColor(requireContext(),correct?R.color.success:R.color.error));feedback.setText((correct?"✓ 跟读识别良好":"△ 还可以再练一次")+"\n识别到："+best+"\n匹配度："+bestScore+"% · 目标："+current.word);speakButton.setEnabled(false);finishQuestion();}
    private int similarity(String a,String b){if(a.equals(b))return 100;if(a.isEmpty()||b.isEmpty())return 0;int n=a.length(),m=b.length();int[] prev=new int[m+1],cur=new int[m+1];for(int j=0;j<=m;j++)prev[j]=j;for(int i=1;i<=n;i++){cur[0]=i;for(int j=1;j<=m;j++){int cost=a.charAt(i-1)==b.charAt(j-1)?0:1;cur[j]=Math.min(Math.min(cur[j-1]+1,prev[j]+1),prev[j-1]+cost);}int[] t=prev;prev=cur;cur=t;}return Math.max(0,(int)Math.round(100.0*(1.0-prev[m]/(double)Math.max(n,m))));}
    private String speechError(int e){switch(e){case SpeechRecognizer.ERROR_NO_MATCH:return "没有听清楚";case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:return "没有检测到语音";case SpeechRecognizer.ERROR_NETWORK:return "语音识别网络暂不可用";case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:return "缺少麦克风权限";case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:return "语音识别正在忙";default:return "语音识别失败（"+e+"）";}}
    @Override public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==REQ_AUDIO&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)startSpeech();else if(requestCode==REQ_AUDIO){feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.error));feedback.setText("需要麦克风权限才能进行跟读识别。");}}

    private String normalize(String s){String n=Normalizer.normalize(s.trim().toLowerCase(Locale.ITALIAN),Normalizer.Form.NFD).replaceAll("\\p{M}+","");n=n.replace('’','\'');return n.replaceAll("[^a-z0-9' ]","").replaceAll("\\s+"," ").trim();}
    private void showFeedback(boolean correct,long ms){feedback.setTextColor(ContextCompat.getColor(requireContext(),correct?R.color.success:R.color.error));String speed=ms<3000?" · 反应很快":(ms<8000?"":" · 建议再复习");feedback.setText((correct?"✓ 正确":"✗ 正确答案："+current.word)+"\n"+current.word+" = "+safeChinese(current)+speed+"\n四维：识义 "+progress.meaningLevel(current.id)+" · 听力 "+progress.listeningLevel(current.id)+" · 拼写 "+progress.spellingLevel(current.id)+" · 口语 "+progress.speakingLevel(current.id));}
    private void finishQuestion(){next.setVisibility(View.VISIBLE);refreshStats();if(mode==MODE_REVIEW)pool=repo.reviewDue(progress,LocalDate.now());else if(mode==MODE_WRONG)pool=repo.wrongWords(progress);else if(mode==MODE_CONFUSION)pool=repo.confusionPool(progress);}
    @Override public void onDestroyView(){if(recognizer!=null){recognizer.destroy();recognizer=null;}if(audio!=null)audio.release();super.onDestroyView();}
}
