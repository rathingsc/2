package com.italiano2774.nativeapp;
import android.content.Context;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public class DialogueRepository{
    private static DialogueRepository instance;private final List<DialogueScenario> dialogues=new ArrayList<>();
    private DialogueRepository(Context c){
        try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getAssets().open("dialogues.json"),StandardCharsets.UTF_8))){StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);JSONArray arr=new JSONArray(sb.toString());for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i);DialogueScenario d=new DialogueScenario();d.id=o.optString("id");d.title=o.optString("title");d.emoji=o.optString("emoji");JSONArray ts=o.optJSONArray("turns");if(ts!=null)for(int j=0;j<ts.length();j++){JSONObject q=ts.getJSONObject(j);DialogueTurn t=new DialogueTurn();t.npc=q.optString("npc");t.npcZh=q.optString("npcZh");t.reply=q.optString("reply");t.replyZh=q.optString("replyZh");t.correct=q.optInt("correct",0);JSONArray cs=q.optJSONArray("choices");if(cs!=null)for(int k=0;k<cs.length();k++)t.choices.add(cs.optString(k));d.turns.add(t);}dialogues.add(d);}}catch(Exception e){throw new RuntimeException("Failed to load dialogues.json",e);}
    }
    public static synchronized DialogueRepository get(Context c){if(instance==null)instance=new DialogueRepository(c.getApplicationContext());return instance;}
    public List<DialogueScenario> all(){return dialogues;}
    public DialogueScenario find(String id){for(DialogueScenario d:dialogues)if(d.id.equals(id))return d;return null;}
}
