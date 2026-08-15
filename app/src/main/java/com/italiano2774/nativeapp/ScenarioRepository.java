package com.italiano2774.nativeapp;

import android.content.Context;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public class ScenarioRepository {
    private static ScenarioRepository instance;private final List<Scenario> scenarios=new ArrayList<>();
    private ScenarioRepository(Context c){
        try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getAssets().open("scenarios.json"),StandardCharsets.UTF_8))){
            StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);JSONArray arr=new JSONArray(sb.toString());
            for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i);Scenario s=new Scenario();s.id=o.optString("id");s.title=o.optString("title");s.subtitle=o.optString("subtitle");s.emoji=o.optString("emoji");JSONArray ps=o.optJSONArray("phrases");if(ps!=null)for(int j=0;j<ps.length();j++){JSONObject q=ps.getJSONObject(j);ScenarioPhrase p=new ScenarioPhrase();p.it=q.optString("it");p.zh=q.optString("zh");p.note=q.optString("note");s.phrases.add(p);}scenarios.add(s);}
        }catch(Exception e){throw new RuntimeException("Failed to load scenarios.json",e);}
    }
    public static synchronized ScenarioRepository get(Context c){if(instance==null)instance=new ScenarioRepository(c.getApplicationContext());return instance;}
    public List<Scenario> all(){return scenarios;}
    public Scenario find(String id){for(Scenario s:scenarios)if(s.id.equals(id))return s;return null;}
}
