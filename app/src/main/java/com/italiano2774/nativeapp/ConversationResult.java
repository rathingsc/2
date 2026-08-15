package com.italiano2774.nativeapp;

import java.util.ArrayList;
import java.util.List;

public class ConversationResult {
    public boolean understood;
    public int score;
    public String replyIt,replyZh,coach,suggested;
    public final List<GrammarIssue> grammarIssues=new ArrayList<>();
}
