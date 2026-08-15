package com.italiano2774.nativeapp;

import java.util.ArrayList;
import java.util.List;

public class DailyPlan {
    public final List<Word> words=new ArrayList<>();
    public int minutes;
    public int newCount;
    public int newQuota;
    public int reviewCount;
    public int wrongCount;
    public int weakCount;
    public int listeningWeak;
    public int spellingWeak;
    public int speakingWeak;
    public int meaningWeak;

    public int total(){return words.size();}
    public String summary(){
        return minutes+"分钟 · 新词 "+newCount+"/"+newQuota+" · 到期复习 "+reviewCount+" · 错词 "+wrongCount+" · 弱项 "+weakCount;
    }
}
