package com.italiano2774.nativeapp;

import android.content.Context;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Builds a small daily syllabus from review debt, four-dimensional mastery and grammar history. */
public class PersonalizedCourseEngine {
    private final Context context;private final WordRepository words;private final ProgressStore progress;
    public PersonalizedCourseEngine(Context c,WordRepository w,ProgressStore p){context=c.getApplicationContext();words=w;progress=p;}
    public PersonalizedCourse build(){
        PersonalizedCourse c=new PersonalizedCourse();c.minutes=progress.sessionMinutes();c.vocabularyPlan=words.adaptivePlan(progress);
        List<Word> studied=new ArrayList<>();for(Word w:words.all())if(progress.mastery(w.id)>0)studied.add(w);
        int weakest=ProgressStore.DIM_MEANING,best=101;
        for(int d=0;d<4;d++){int avg=studied.isEmpty()?0:progress.dimensionAverage(studied,d);if(avg<best){best=avg;weakest=d;}}
        c.weakDimension=weakest;c.weakDimensionName=dimName(weakest);
        GrammarPoint gp=GrammarDiagnostics.recommended(progress);c.grammarPatternId=gp.practicePatternId==null?"vorrei":gp.practicePatternId;c.grammarTitle=gp.title;
        Scenario scenario=chooseScenario(weakest);c.scenarioId=scenario.id;c.scenarioTitle=scenario.title;
        if(c.minutes==5){c.vocabularyCards=Math.min(6,c.vocabularyPlan.total());c.grammarQuestions=1;c.conversationTurns=1;c.listeningMinutes=1;}
        else if(c.minutes==15){c.vocabularyCards=Math.min(12,c.vocabularyPlan.total());c.grammarQuestions=3;c.conversationTurns=2;c.listeningMinutes=3;}
        else if(c.minutes==60){c.vocabularyCards=Math.min(45,c.vocabularyPlan.total());c.grammarQuestions=8;c.conversationTurns=5;c.listeningMinutes=12;}
        else{c.vocabularyCards=Math.min(24,c.vocabularyPlan.total());c.grammarQuestions=5;c.conversationTurns=3;c.listeningMinutes=6;}
        if(weakest==ProgressStore.DIM_LISTENING)c.listeningMinutes+=2;
        if(weakest==ProgressStore.DIM_SPEAKING)c.conversationTurns+=1;
        if(progress.grammarAttempts(gp.id)>0&&progress.grammarAccuracy(gp.id)<70)c.grammarQuestions+=2;
        String checkpoint="";int checkpointScore=0;
        for(String level:new String[]{"A1","A2","B1"}){int sc=progress.lastExamScore(level);if(sc>0&&sc<70){checkpoint=level;checkpointScore=sc;break;}}
        if(!checkpoint.isEmpty()){c.grammarQuestions+=2;c.conversationTurns+=1;c.listeningMinutes+=2;c.vocabularyCards=Math.min(c.vocabularyCards,Math.max(8,c.vocabularyPlan.total()-c.vocabularyPlan.newCount/2));}
        String readLevel=progress.lastExamScore("A1")<70?"A1":(progress.lastExamScore("A2")<70?"A2":"B1");
        List<ReadingPassage> reads=ReadingRepository.get(context).byLevel(readLevel);if(!reads.isEmpty()){ReadingPassage rp=reads.get(Math.floorMod(LocalDate.now().getDayOfYear(),reads.size()));c.readingId=rp.id;c.readingTitle=rp.title+" · "+rp.titleZh;c.readingLevel=readLevel;}
        int due=progress.dueCount(words.all(),LocalDate.now()),wrong=progress.wrongTotal(words.all());
        c.focusReason="当前最弱："+c.weakDimensionName+"（已学词平均约 "+best+"%） · 最近7天正确率 "+progress.sevenDayAccuracy()+"% · 今日建议新词 "+c.vocabularyPlan.newQuota+" · 到期复习 "+due+" · 错词 "+wrong+"。"+(progress.grammarAttempts(gp.id)>0?"语法重点："+gp.title+"，正确率 "+progress.grammarAccuracy(gp.id)+"%。":"先从高频句型 "+gp.title+" 建立基础。" )+(!checkpoint.isEmpty()?" "+checkpoint+"阶段自测仅 "+checkpointScore+"%，今日自动减少新词并增加语法、听力和输出训练。":"");
        return c;
    }
    private String dimName(int d){switch(d){case ProgressStore.DIM_LISTENING:return "听力";case ProgressStore.DIM_SPELLING:return "拼写";case ProgressStore.DIM_SPEAKING:return "口语";default:return "识义";}}
    private Scenario chooseScenario(int weak){
        List<Scenario> list=ScenarioRepository.get(context).all();if(list.isEmpty()){Scenario s=new Scenario();s.id="bar_restaurant";s.title="Bar 和餐厅";return s;}
        int offset=LocalDate.now().getDayOfYear();if(weak==ProgressStore.DIM_SPEAKING)offset+=3;else if(weak==ProgressStore.DIM_LISTENING)offset+=6;
        return list.get(Math.floorMod(offset,list.size()));
    }
}
