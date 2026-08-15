package com.italiano2774.nativeapp;
import android.content.Context;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public class SentencePatternRepository{
    private static SentencePatternRepository instance;private final List<SentencePattern> patterns=new ArrayList<>();
    private SentencePatternRepository(Context c){
        try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getAssets().open("sentence_patterns.json"),StandardCharsets.UTF_8))){
            StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);JSONArray arr=new JSONArray(sb.toString());
            for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i);SentencePattern p=new SentencePattern();p.id=o.optString("id");p.title=o.optString("title");p.formula=o.optString("formula");p.explanation=o.optString("explanation");JSONArray ex=o.optJSONArray("exercises");if(ex!=null)for(int j=0;j<ex.length();j++){JSONObject q=ex.getJSONObject(j);PatternExercise e=new PatternExercise();e.prompt=q.optString("prompt");e.answer=q.optString("answer");e.it=q.optString("it");e.zh=q.optString("zh");p.exercises.add(e);}patterns.add(p);}
        }catch(Exception e){throw new RuntimeException("Failed to load sentence_patterns.json",e);}
    }
    public static synchronized SentencePatternRepository get(Context c){if(instance==null)instance=new SentencePatternRepository(c.getApplicationContext());return instance;}
    public List<SentencePattern> all(){return patterns;}
}
