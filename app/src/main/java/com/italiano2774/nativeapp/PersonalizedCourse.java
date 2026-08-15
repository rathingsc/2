package com.italiano2774.nativeapp;

public class PersonalizedCourse {
    public int minutes;
    public int vocabularyCards;
    public int grammarQuestions;
    public int conversationTurns;
    public int listeningMinutes;
    public int weakDimension;
    public String weakDimensionName;
    public String grammarPatternId;
    public String grammarTitle;
    public String scenarioId;
    public String scenarioTitle;
    public String focusReason;
    public String readingId;
    public String readingTitle;
    public String readingLevel;
    public DailyPlan vocabularyPlan;

    public String shortSummary(){
        return minutes+"分钟课程 · 词汇 "+vocabularyCards+" · 句型 "+grammarQuestions+"题 · 对话 "+conversationTurns+"轮 · 阅读 "+(readingLevel==null?"A1":readingLevel)+" · 听力 "+listeningMinutes+"分钟";
    }
}
