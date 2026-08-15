package com.italiano2774.nativeapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Four-step whole-sentence shadowing: listen, read+listen, shadow, recall without subtitles. */
public class ShadowingFragment extends Fragment {
    private static final int REQ_AUDIO=771;
    private final List<Line> lines=new ArrayList<>();private int index=0,stage=0;private boolean played=false;private float rate=1.0f;private long started=0L;
    private TextView stats,stageText,it,zh,result;private MaterialButton stageButton,record,next;private TextToSpeech tts;private SpeechRecognizer recognizer;private ProgressStore progress;
    private static class Line{String it,zh,topic;Line(String i,String z,String t){it=i;zh=z;topic=t;}}

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_shadowing,parent,false);progress=new ProgressStore(requireContext());stats=v.findViewById(R.id.text_shadow_stats);stageText=v.findViewById(R.id.text_shadow_stage);it=v.findViewById(R.id.text_shadow_it);zh=v.findViewById(R.id.text_shadow_zh);result=v.findViewById(R.id.text_shadow_result);stageButton=v.findViewById(R.id.button_shadow_stage);record=v.findViewById(R.id.button_shadow_record);next=v.findViewById(R.id.button_shadow_next);
        buildLines();tts=new TextToSpeech(requireContext().getApplicationContext(),status->{if(status==TextToSpeech.SUCCESS){tts.setLanguage(Locale.ITALIAN);tts.setSpeechRate(rate);}});
        MaterialButtonToggleGroup speeds=v.findViewById(R.id.group_shadow_speed);speeds.check(R.id.button_speed_normal);speeds.addOnButtonCheckedListener((g,id,checked)->{if(!checked)return;rate=id==R.id.button_speed_slow?0.75f:(id==R.id.button_speed_fast?1.15f:1.0f);if(tts!=null)tts.setSpeechRate(rate);});
        v.findViewById(R.id.button_shadow_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());stageButton.setOnClickListener(x->advanceStage());record.setOnClickListener(x->requestOrStartSpeech());next.setOnClickListener(x->{index=(index+1)%lines.size();stage=0;played=false;show();});show();return v;
    }
    private void buildLines(){for(Scenario s:ScenarioRepository.get(requireContext()).all())for(ScenarioPhrase p:s.phrases)if(p.it!=null&&!p.it.trim().isEmpty())lines.add(new Line(p.it,p.zh,s.title));if(lines.isEmpty())lines.add(new Line("Vorrei un caffè, per favore.","我想要一杯咖啡，谢谢。","基础交流"));}
    private Line current(){return lines.get(index%lines.size());}
    private void speak(){if(tts!=null){tts.setSpeechRate(rate);tts.speak(current().it,TextToSpeech.QUEUE_FLUSH,null,"shadow");}}
    private void show(){Line l=current();result.setText("");record.setVisibility(stage>=2?View.VISIBLE:View.GONE);it.setText(l.it);zh.setText(l.zh);stats.setText("Shadowing "+(index+1)+" / "+lines.size()+" · 历史正确率 "+progress.auxiliaryAccuracy("shadowing")+"% · "+l.topic);applyStage();}
    private void applyStage(){
        record.setVisibility(stage>=2?View.VISIBLE:View.GONE);
        if(stage==0){stageText.setText("第1遍 · 只听，不看字幕");it.setVisibility(View.INVISIBLE);zh.setVisibility(View.INVISIBLE);stageButton.setText("🔊 只听一遍");}
        else if(stage==1){stageText.setText("第2遍 · 看文字再听");it.setVisibility(View.VISIBLE);zh.setVisibility(View.VISIBLE);stageButton.setText("🔊 看文再听");}
        else if(stage==2){stageText.setText("第3遍 · 跟着原句一起说");it.setVisibility(View.VISIBLE);zh.setVisibility(View.GONE);stageButton.setText("🔊 播放后跟读");}
        else{stageText.setText("第4遍 · 隐藏字幕，自己复述");it.setVisibility(View.INVISIBLE);zh.setVisibility(View.INVISIBLE);stageButton.setText("🔊 最后听一次");}
    }
    private void advanceStage(){
        if(!played){speak();played=true;stageButton.setText(stage<3?"进入下一遍 →":"已听完 · 现在自己复述");return;}
        if(stage<3){stage++;played=false;applyStage();}else{record.setVisibility(View.VISIBLE);started=System.currentTimeMillis();}
    }
    private void requestOrStartSpeech(){if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}startSpeech();}
    private void startSpeech(){if(!SpeechRecognizer.isRecognitionAvailable(requireContext())){result.setText("这台设备没有可用的系统语音识别服务。");return;}if(recognizer!=null)recognizer.destroy();recognizer=SpeechRecognizer.createSpeechRecognizer(requireContext());recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){record.setText("正在听…");}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){record.setText("🎙 评估跟读");}public void onError(int e){result.setText("没有识别清楚，请再说一次。");record.setText("🎙 再说一次");}public void onResults(Bundle b){ArrayList<String> xs=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);String heard=xs==null||xs.isEmpty()?"":xs.get(0);score(heard);}public void onPartialResults(Bundle b){}public void onEvent(int t,Bundle b){}});Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"it-IT");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);started=System.currentTimeMillis();recognizer.startListening(i);}
    private void score(String heard){int s=similarity(current().it,heard);boolean ok=s>=72;long ms=Math.max(1,System.currentTimeMillis()-started);progress.recordAuxiliaryResult("shadowing",ok,ms);result.setText((ok?"✓ 跟读清楚":"继续练一次")+" · 内容匹配 "+s+"%\n识别到："+heard+"\n目标："+current().it);result.setTextColor(ContextCompat.getColor(requireContext(),ok?R.color.success:R.color.error));stats.setText("Shadowing "+(index+1)+" / "+lines.size()+" · 历史正确率 "+progress.auxiliaryAccuracy("shadowing")+"% · "+current().topic);record.setText("🎙 再说一次");}
    private int similarity(String a,String b){String x=norm(a),y=norm(b);if(x.isEmpty()||y.isEmpty())return 0;int max=Math.max(x.length(),y.length()),d=edit(x,y);return (int)Math.round(Math.max(0,1-d/(double)max)*100);}
    private String norm(String s){String n=Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ITALIAN).replace('’','\'');return n.replaceAll("[^a-z0-9' ]"," ").replaceAll("\\s+"," ").trim();}
    private int edit(String a,String b){int[] p=new int[b.length()+1],c=new int[b.length()+1];for(int j=0;j<=b.length();j++)p[j]=j;for(int i=1;i<=a.length();i++){c[0]=i;for(int j=1;j<=b.length();j++){int z=a.charAt(i-1)==b.charAt(j-1)?0:1;c[j]=Math.min(Math.min(c[j-1]+1,p[j]+1),p[j-1]+z);}int[] t=p;p=c;c=t;}return p[b.length()];}
    @Override public void onRequestPermissionsResult(int r,@NonNull String[] p,@NonNull int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_AUDIO&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startSpeech();}
    @Override public void onDestroyView(){if(recognizer!=null)recognizer.destroy();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroyView();}
}
