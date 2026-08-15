package com.italiano2774.nativeapp;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Lightweight offline grammar diagnosis. It combines exercise history with a few high-confidence checks. */
public final class GrammarDiagnostics {
    private GrammarDiagnostics(){}
    private static final LinkedHashMap<String,GrammarPoint> POINTS=new LinkedHashMap<>();
    static{
        add("vorrei","礼貌表达 Vorrei","办事、点餐、购物时优先用 Vorrei + 名词/动词原形。","vorrei");
        add("bisogno","Avere bisogno di","固定结构是 avere bisogno di，后面接名词或动词原形。","bisogno");
        add("potere","Potere + 动词原形","posso / puoi / può 后面接动词原形。","potere");
        add("dovere","Dovere + 动词原形","devo / devi / deve 后面接动词原形。","dovere");
        add("c_e","C'è / Ci sono","单数用 c'è，复数用 ci sono。","c_e");
        add("stare_gerundio","正在做 Stare + gerundio","sto/stai/sta + -ando/-endo 表示正在做。","stare_gerundio");
        add("piacere","Mi piace / Mi piacciono","单数或动词用 piace，复数名词用 piacciono。","piacere");
        add("da_quanto","Da quanto tempo","持续到现在的动作常用现在时 + da。","da_quanto");
        add("prima_dopo","Prima di / Dopo aver","prima di + 原形；dopo aver + 过去分词。","prima_dopo");
        add("se_presente","Se + 现在时","真实条件常用 se + 现在时，再接现在/将来。","se_presente");
        add("perche","Perché / Per + 动词","perché 表原因；per + 原形常表示目的。","perche");
        add("quanto_costa","Quanto costa / costano","单数 costa，复数 costano。","quanto_costa");
        add("dove_posso","Dove posso + 动词原形","问地点时可直接用 Dove posso + 动词原形。","dove_posso");
        add("mi_serve","Mi serve / Mi servono","单数 serve，复数 servono。","mi_serve");
        add("passato_prossimo","近过去时 Passato prossimo","avere/essere + 过去分词，注意助动词和一致。","passato_prossimo");
        add("imperfetto","未完成过去时 Imperfetto","描述过去背景、习惯、持续状态时常用未完成过去时。","imperfetto");
        add("comparativo","比较级 più / meno","più/meno + 形容词 + di/che。","comparativo");
        add("ce_l_ho","Ce l'ho","表达“我有这个/我没有这个”常用 ce l'ho / non ce l'ho。","ce_l_ho");
        add("ci_vuole","Ci vuole / Ci vogliono","单数用 ci vuole，复数用 ci vogliono。","ci_vuole");
        add("vorrei_sapere","Vorrei sapere se","礼貌询问信息：Vorrei sapere se...","vorrei_sapere");
        add("non_ho_capito","没听懂时的表达","Non ho capito. Può ripetere/parlare più lentamente?","non_ho_capito");
        add("verb_agreement","主语与动词一致","先确认主语，再选择正确的人称变位。",null);
    }
    private static void add(String id,String title,String tip,String practice){POINTS.put(id,new GrammarPoint(id,title,tip,practice));}
    public static List<GrammarPoint> all(){return new ArrayList<>(POINTS.values());}
    public static GrammarPoint get(String id){GrammarPoint p=POINTS.get(id);return p==null?new GrammarPoint(id,id,"继续通过句型练习巩固。",null):p;}

    private static String norm(String s){
        if(s==null)return "";String n=Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ITALIAN).replace('’','\'');
        return n.replaceAll("[^a-z0-9' ]"," ").replaceAll("\\s+"," ").trim();
    }
    private static boolean matches(String n,String regex){return Pattern.compile(regex).matcher(n).find();}

    /** Only high-confidence checks are returned so the coach avoids noisy corrections. */
    public static List<GrammarIssue> analyze(String input){
        String n=norm(input);List<GrammarIssue> out=new ArrayList<>();if(n.isEmpty())return out;
        if(matches(n,"\\bio\\s+(e|sei|sono|siamo|siete)\\b")||matches(n,"\\btu\\s+(e|sono|siamo|siete)\\b"))
            out.add(new GrammarIssue("verb_agreement","主语与动词一致","主语和 essere 的人称可能不一致。","例如：Io sono... / Tu sei..."));
        if(matches(n,"\\bmi piace\\s+(i|gli|le)\\b")||matches(n,"\\bmi piacciono\\s+(il|lo|la|un|una)\\b"))
            out.add(new GrammarIssue("piacere","Mi piace / Mi piacciono","piacere 要和后面的事物保持单复数一致。","单数：Mi piace...；复数：Mi piacciono..."));
        if(matches(n,"\\bc'?e\\s+(due|tre|quattro|cinque|molti|molte|diversi|diverse)\\b")||matches(n,"\\bci sono\\s+(un|uno|una)\\b"))
            out.add(new GrammarIssue("c_e","C'è / Ci sono","存在句的单复数可能用反了。","单数用 c'è；复数用 ci sono。"));
        if(matches(n,"\\bposso\\s+(vado|faccio|parlo|mangio|prendo|pago|cerco|chiedo)\\b"))
            out.add(new GrammarIssue("potere","Potere + 动词原形","posso 后面不要再用第一人称变位。","例如：Posso andare / fare / pagare..."));
        if(matches(n,"\\bdevo\\s+(vado|faccio|parlo|mangio|prendo|pago|cerco|chiedo)\\b"))
            out.add(new GrammarIssue("dovere","Dovere + 动词原形","devo 后面接动词原形。","例如：Devo andare / fare / pagare..."));
        if(matches(n,"\\bbisogno\\s+(a|per)\\b"))
            out.add(new GrammarIssue("bisogno","Avere bisogno di","bisogno 后常用 di。","例如：Ho bisogno di aiuto."));
        if(matches(n,"\\bmi serve\\s+(i|gli|le)\\b")||matches(n,"\\bmi servono\\s+(il|lo|la|un|una)\\b"))
            out.add(new GrammarIssue("mi_serve","Mi serve / Mi servono","serve/servono 要和需要的东西保持单复数一致。","单数 mi serve；复数 mi servono。"));
        if(matches(n,"\\bquanto costa\\s+(i|gli|le)\\b")||matches(n,"\\bquanto costano\\s+(il|lo|la|un|una)\\b"))
            out.add(new GrammarIssue("quanto_costa","Quanto costa / costano","costa/costano 要和名词保持单复数一致。","单数 Quanto costa...? 复数 Quanto costano...?"));
        return out;
    }

    public static List<GrammarPoint> weakest(ProgressStore progress,int limit){
        List<GrammarPoint> all=all();Collections.sort(all,new Comparator<GrammarPoint>(){
            @Override public int compare(GrammarPoint a,GrammarPoint b){
                int aa=progress.grammarAttempts(a.id),ba=progress.grammarAttempts(b.id);
                int as=weakScore(progress,a.id),bs=weakScore(progress,b.id);
                if(as!=bs)return Integer.compare(bs,as);return Integer.compare(ba,aa);
            }
        });
        List<GrammarPoint> out=new ArrayList<>();for(GrammarPoint p:all){if(progress.grammarAttempts(p.id)>0&&(progress.grammarMistakes(p.id)>0||progress.grammarAccuracy(p.id)<85)){out.add(p);if(out.size()>=limit)break;}}
        return out;
    }
    public static int weakScore(ProgressStore p,String id){int a=p.grammarAttempts(id);if(a==0)return 0;return p.grammarMistakes(id)*20+(100-p.grammarAccuracy(id));}
    public static GrammarPoint recommended(ProgressStore progress){List<GrammarPoint> weak=weakest(progress,1);return weak.isEmpty()?POINTS.get("vorrei"):weak.get(0);}
}
