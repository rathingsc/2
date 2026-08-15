package com.italiano2774.nativeapp;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Offline intent/similarity engine for open-response scenario practice. */
public class FreeConversationEngine {
    private static String norm(String s){if(s==null)return"";String n=Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ITALIAN).replace('’','\'');return n.replaceAll("[^a-z0-9' ]"," ").replaceAll("\\s+"," ").trim();}
    private static Set<String> tokens(String s){Set<String> out=new HashSet<>();for(String x:norm(s).split(" "))if(x.length()>1&&!isStop(x))out.add(x);return out;}
    private static boolean isStop(String s){return s.equals("il")||s.equals("lo")||s.equals("la")||s.equals("i")||s.equals("gli")||s.equals("le")||s.equals("un")||s.equals("una")||s.equals("di")||s.equals("a")||s.equals("e")||s.equals("per")||s.equals("con")||s.equals("che");}
    private static int editDistance(String a,String b){int[] prev=new int[b.length()+1],cur=new int[b.length()+1];for(int j=0;j<=b.length();j++)prev[j]=j;for(int i=1;i<=a.length();i++){cur[0]=i;for(int j=1;j<=b.length();j++){int c=a.charAt(i-1)==b.charAt(j-1)?0:1;cur[j]=Math.min(Math.min(cur[j-1]+1,prev[j]+1),prev[j-1]+c);}int[] t=prev;prev=cur;cur=t;}return prev[b.length()];}
    private static double similarity(String a,String b){String x=norm(a),y=norm(b);if(x.isEmpty()||y.isEmpty())return 0;Set<String> xa=tokens(x),ya=tokens(y);int common=0;for(String t:xa)if(ya.contains(t))common++;double token=ya.isEmpty()?0:common/(double)ya.size();int max=Math.max(x.length(),y.length());double lev=max==0?1:1-editDistance(x,y)/(double)max;return Math.max(token,lev*0.85);}
    public ConversationResult evaluate(DialogueTurn turn,String user){
        ConversationResult r=new ConversationResult();String expected=turn.choices.isEmpty()?"":turn.choices.get(Math.max(0,Math.min(turn.correct,turn.choices.size()-1)));r.suggested=expected;
        double best=similarity(user,expected);r.score=(int)Math.round(best*100);r.understood=best>=0.42||norm(user).length()>=4&&tokens(user).size()>=2&&best>=0.30;
        r.grammarIssues.addAll(GrammarDiagnostics.analyze(user));
        if(r.understood){r.replyIt=turn.reply;r.replyZh=turn.replyZh;r.coach=r.grammarIssues.isEmpty()?"意思表达清楚，可以继续对话。":"意思能听懂，但有一个语法点值得马上修正。";}
        else{r.replyIt="Mi scusi, può dirlo in un altro modo?";r.replyZh="不好意思，您可以换一种说法吗？";r.coach="这句话和当前场景的目标表达差得比较远。先参考建议，再自己说一遍。";}
        return r;
    }
}
