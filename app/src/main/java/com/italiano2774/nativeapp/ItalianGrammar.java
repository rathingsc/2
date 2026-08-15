package com.italiano2774.nativeapp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Lightweight Italian grammar helpers for the vocabulary cards. */
public final class ItalianGrammar {
    private ItalianGrammar(){}
    private static final String[] PERSONS={"io","tu","lui/lei","noi","voi","loro"};
    private static final Map<String,String[]> IRREGULAR=new HashMap<>();
    private static final Set<String> ISC=new HashSet<>(Arrays.asList(
            "capire","finire","preferire","pulire","spedire","costruire","restituire","suggerire","proibire","definire","guarire","obbedire","chiarire","impedire","dimagrire","unire","ferire","fallire","colpire"
    ));
    static{
        put("essere","sono","sei","è","siamo","siete","sono");
        put("avere","ho","hai","ha","abbiamo","avete","hanno");
        put("andare","vado","vai","va","andiamo","andate","vanno");
        put("fare","faccio","fai","fa","facciamo","fate","fanno");
        put("dare","do","dai","dà","diamo","date","danno");
        put("stare","sto","stai","sta","stiamo","state","stanno");
        put("dire","dico","dici","dice","diciamo","dite","dicono");
        put("venire","vengo","vieni","viene","veniamo","venite","vengono");
        put("potere","posso","puoi","può","possiamo","potete","possono");
        put("volere","voglio","vuoi","vuole","vogliamo","volete","vogliono");
        put("dovere","devo","devi","deve","dobbiamo","dovete","devono");
        put("sapere","so","sai","sa","sappiamo","sapete","sanno");
        put("uscire","esco","esci","esce","usciamo","uscite","escono");
        put("bere","bevo","bevi","beve","beviamo","bevete","bevono");
        put("tenere","tengo","tieni","tiene","teniamo","tenete","tengono");
        put("rimanere","rimango","rimani","rimane","rimaniamo","rimanete","rimangono");
        put("scegliere","scelgo","scegli","sceglie","scegliamo","scegliete","scelgono");
        put("salire","salgo","sali","sale","saliamo","salite","salgono");
        put("morire","muoio","muori","muore","moriamo","morite","muoiono");
        put("sedere","siedo","siedi","siede","sediamo","sedete","siedono");
        put("piacere","piaccio","piaci","piace","piacciamo","piacete","piacciono");
        put("togliere","tolgo","togli","toglie","togliamo","togliete","tolgono");
        put("porre","pongo","poni","pone","poniamo","ponete","pongono");
        put("tradurre","traduco","traduci","traduce","traduciamo","traducete","traducono");
        put("condurre","conduco","conduci","conduce","conduciamo","conducete","conducono");
        put("produrre","produco","produci","produce","produciamo","producete","producono");
        put("riuscire","riesco","riesci","riesce","riusciamo","riuscite","riescono");
        put("inviare","invio","invii","invia","inviamo","inviate","inviano");
        put("sciare","scio","scii","scia","sciamo","sciate","sciano");
        put("appartenere","appartengo","appartieni","appartiene","apparteniamo","appartenete","appartengono");
    }
    private static void put(String lemma,String... forms){IRREGULAR.put(lemma,forms);}

    public static boolean isVerb(Word w){
        String l=safe(w.lemma).toLowerCase(Locale.ROOT);
        String f=safe(w.formInfo);
        return l.endsWith("are")||l.endsWith("ere")||l.endsWith("ire")||f.contains("直陈式")||f.contains("动词原形")||f.contains("不定式");
    }

    /** Present indicative forms. Returns null when a safe pattern is unavailable. */
    public static String[] presentIndicative(Word w){
        String lemma=safe(w.lemma).toLowerCase(Locale.ROOT);
        if(lemma.isEmpty()&&isInfinitive(w.word))lemma=w.word.toLowerCase(Locale.ROOT);
        if(!isInfinitive(lemma))return null;
        if(IRREGULAR.containsKey(lemma))return IRREGULAR.get(lemma);
        if(lemma.endsWith("are"))return regularAre(lemma);
        if(lemma.endsWith("ere"))return regularEre(lemma);
        if(lemma.endsWith("ire"))return regularIre(lemma,ISC.contains(lemma));
        return null;
    }
    private static boolean isInfinitive(String s){return s!=null&&(s.endsWith("are")||s.endsWith("ere")||s.endsWith("ire"));}
    private static String[] regularAre(String l){
        String stem=l.substring(0,l.length()-3);
        if(l.endsWith("care")||l.endsWith("gare")){
            return new String[]{stem+"o",stem+"hi",stem+"a",stem+"hiamo",stem+"ate",stem+"ano"};
        }
        if(stem.endsWith("i")){
            return new String[]{stem+"o",stem,stem+"a",stem+"amo",stem+"ate",stem+"ano"};
        }
        return new String[]{stem+"o",stem+"i",stem+"a",stem+"iamo",stem+"ate",stem+"ano"};
    }
    private static String[] regularEre(String l){String s=l.substring(0,l.length()-3);return new String[]{s+"o",s+"i",s+"e",s+"iamo",s+"ete",s+"ono"};}
    private static String[] regularIre(String l,boolean isc){
        String s=l.substring(0,l.length()-3);
        if(isc)return new String[]{s+"isco",s+"isci",s+"isce",s+"iamo",s+"ite",s+"iscono"};
        return new String[]{s+"o",s+"i",s+"e",s+"iamo",s+"ite",s+"ono"};
    }

    public static String verbPanel(Word w){
        if(!isVerb(w))return "";
        String lemma=safe(w.lemma);if(lemma.isEmpty())lemma=w.word;
        StringBuilder sb=new StringBuilder("动词 · 原形 ").append(lemma);
        if(!safe(w.formInfo).isEmpty())sb.append("\n当前词形：").append(w.formInfo);
        String[] forms=presentIndicative(w);
        if(forms!=null){
            sb.append("\n直陈式现在时");
            for(int i=0;i<6;i++)sb.append(i%2==0?"\n":"    ").append(PERSONS[i]).append(" ").append(forms[i]);
        }
        return sb.toString();
    }

    public static String nounPanel(Word w){
        if(!"noun".equalsIgnoreCase(safe(w.partOfSpeech)))return "";
        String gender=safe(w.gender),number=safe(w.number),article=safe(w.article),plural=safe(w.plural);
        if(gender.isEmpty())return "";
        StringBuilder sb=new StringBuilder("名词 · ").append("f".equals(gender)?"阴性":"阳性");
        if(!number.isEmpty())sb.append(" · ").append("plural".equals(number)?"复数":"单数");
        if(!article.isEmpty())sb.append("\n冠词：").append(joinArticle(article,w.word));
        if(!plural.isEmpty()&&!plural.equalsIgnoreCase(w.word))sb.append("\n复数：").append(joinArticle(pluralArticle(gender,plural),plural));
        return sb.toString();
    }
    private static String joinArticle(String article,String word){return article.endsWith("\'")?article+word:article+" "+word;}
    private static String pluralArticle(String gender,String word){
        String s=word.toLowerCase(Locale.ROOT);
        if(Arrays.asList("uova","braccia","dita","ginocchia","orecchie","paia").contains(s))return "le";
        if("dei".equals(s))return "gli";
        if("f".equals(gender))return "le";
        return startsVowel(s)||specialMasculine(s)?"gli":"i";
    }
    private static boolean startsVowel(String s){return !s.isEmpty()&&"aeiouàèéìòóù".indexOf(s.charAt(0))>=0;}
    private static boolean specialMasculine(String s){return s.startsWith("z")||s.startsWith("gn")||s.startsWith("ps")||s.startsWith("x")||s.startsWith("y")||(s.startsWith("s")&&s.length()>1&&"aeiou".indexOf(s.charAt(1))<0);}

    public static String grammarPanel(Word w){
        String v=verbPanel(w);if(!v.isEmpty())return v;
        return nounPanel(w);
    }
    private static String safe(String s){return s==null?"":s.trim();}
}
