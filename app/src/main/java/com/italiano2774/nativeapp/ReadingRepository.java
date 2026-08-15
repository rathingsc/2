package com.italiano2774.nativeapp;
import android.content.Context;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public class ReadingRepository {
    private static ReadingRepository instance; private final List<ReadingPassage> passages=new ArrayList<>();
    private ReadingRepository(Context c){
        try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getAssets().open("readings.json"), StandardCharsets.UTF_8))){
            StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);JSONArray arr=new JSONArray(sb.toString());
            for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i);ReadingPassage p=new ReadingPassage();p.id=o.optString("id");p.level=o.optString("level");p.title=o.optString("title");p.titleZh=o.optString("titleZh");p.text=o.optString("text");p.translation=o.optString("translation");JSONArray qs=o.optJSONArray("questions");if(qs!=null)for(int j=0;j<qs.length();j++){JSONObject q=qs.getJSONObject(j);ReadingQuestion r=new ReadingQuestion();r.q=q.optString("q");r.zh=q.optString("zh");r.answer=q.optInt("answer",0);JSONArray os=q.optJSONArray("options");if(os!=null)for(int k=0;k<os.length();k++)r.options.add(os.optString(k));p.questions.add(r);}passages.add(p);}
        }catch(Exception e){throw new RuntimeException("Failed to load readings.json",e);}
    }
    public static synchronized ReadingRepository get(Context c){if(instance==null)instance=new ReadingRepository(c.getApplicationContext());return instance;}
    public List<ReadingPassage> all(){return passages;}
    public List<ReadingPassage> byLevel(String level){List<ReadingPassage> out=new ArrayList<>();for(ReadingPassage p:passages)if(p.level.equalsIgnoreCase(level))out.add(p);return out;}
    public ReadingPassage byId(String id){for(ReadingPassage p:passages)if(p.id.equals(id))return p;return null;}
}
