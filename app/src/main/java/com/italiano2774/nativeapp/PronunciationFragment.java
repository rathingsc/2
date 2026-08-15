package com.italiano2774.nativeapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Listening discrimination + short repeat drills for common Italian pronunciation traps. */
public class PronunciationFragment extends Fragment {
    private static final int REQ_AUDIO=772;
    private static class Drill{String a,b,focus,note;Drill(String a,String b,String f,String n){this.a=a;this.b=b;focus=f;note=n;}}
    private final List<Drill> drills= Arrays.asList(
            new Drill("pala","palla","双辅音 ll","意大利语双辅音要明显延长；pala 和 palla 的节奏不同。"),
            new Drill("sete","sette","双辅音 tt","sette 的 t 要停顿更明显，不要读成 sete。"),
            new Drill("casa","cassa","双辅音 ss","cassa 的 s 持续更长。"),
            new Drill("pena","penna","双辅音 nn","penna 中间要有明显的 n 延长。"),
            new Drill("copia","coppia","双辅音 pp","coppia 的 p 是双辅音，嘴唇闭合停顿更明显。"),
            new Drill("fato","fatto","双辅音 tt","fatto 的 t 比 fato 更长、更有阻塞感。"),
            new Drill("caro","carro","单 r / 双 rr","carro 的 r 更强，通常颤动更明显。"),
            new Drill("capello","cappello","双辅音 pp","cappello 的 pp 必须读出来，否则听起来像另一个词。"),
            new Drill("note","notte","双辅音 tt","notte 的 t 更长。"),
            new Drill("nono","nonno","双辅音 nn","nonno 的 n 要延长。"),
            new Drill("filo","figlio","gli /ʎ/","figlio 的 gli 不是普通的 li，要让舌面贴近上颚。"),
            new Drill("mano","bagno","gn /ɲ/","bagno 的 gn 类似西班牙语 ñ，不要拆成 g+n。"),
            new Drill("cena","cane","ce / ca","cena 的 c 是 /tʃ/，cane 的 c 是 /k/。"),
            new Drill("chi","ci","chi / ci","chi 是 /ki/，ci 是 /tʃi/。"),
            new Drill("giro","gatto","gi / ga","giro 的 g 是 /dʒ/，gatto 的 g 是 /g/。"),
            new Drill("rosa","rossa","单 s / 双 ss","rossa 的 ss 更长，而且通常是清音。")
    );
    private final Random random=new Random();private int index=0;private String target="";private long started=0L;
    private TextView stats,focus,note,result;private MaterialButton a,b,speak,next,replay;private AudioPlayer audio;private ProgressStore progress;private SpeechRecognizer recognizer;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_pronunciation,parent,false);progress=new ProgressStore(requireContext());audio=new AudioPlayer(requireContext(),progress);stats=v.findViewById(R.id.text_pron_stats);focus=v.findViewById(R.id.text_pron_focus);note=v.findViewById(R.id.text_pron_note);result=v.findViewById(R.id.text_pron_result);a=v.findViewById(R.id.button_pron_a);b=v.findViewById(R.id.button_pron_b);speak=v.findViewById(R.id.button_pron_speak);next=v.findViewById(R.id.button_pron_next);replay=v.findViewById(R.id.button_pron_replay);
        v.findViewById(R.id.button_pron_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());a.setOnClickListener(x->answer(a.getText().toString()));b.setOnClickListener(x->answer(b.getText().toString()));replay.setOnClickListener(x->playTarget());speak.setOnClickListener(x->requestOrStartSpeech());next.setOnClickListener(x->{index=(index+1)%drills.size();newQuestion();});newQuestion();return v;
    }
    private Drill current(){return drills.get(index%drills.size());}
    private void newQuestion(){Drill d=current();target=random.nextBoolean()?d.a:d.b;a.setText(d.a);b.setText(d.b);focus.setText("专项："+d.focus);note.setText("先听，再判断你听到哪个词。答完后可以自己跟读。\n"+d.note);result.setText("");a.setEnabled(true);b.setEnabled(true);speak.setVisibility(View.GONE);next.setVisibility(View.GONE);stats.setText("发音专项 "+(index+1)+" / "+drills.size()+" · 历史正确率 "+progress.auxiliaryAccuracy("pronunciation")+"%");started=System.currentTimeMillis();playTarget();}
    private void playTarget(){audio.speak(target);}
    private void answer(String value){if(!a.isEnabled())return;boolean ok=value.equalsIgnoreCase(target);long ms=Math.max(1,System.currentTimeMillis()-started);progress.recordAuxiliaryResult("pronunciation",ok,ms);result.setText((ok?"✓ 听对了":"✗ 你听到的是 "+target)+"\n"+current().note);result.setTextColor(ContextCompat.getColor(requireContext(),ok?R.color.success:R.color.error));a.setEnabled(false);b.setEnabled(false);speak.setVisibility(View.VISIBLE);next.setVisibility(View.VISIBLE);stats.setText("发音专项 "+(index+1)+" / "+drills.size()+" · 历史正确率 "+progress.auxiliaryAccuracy("pronunciation")+"%");}
    private void requestOrStartSpeech(){if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}startSpeech();}
    private void startSpeech(){if(!SpeechRecognizer.isRecognitionAvailable(requireContext())){result.setText("这台设备没有可用的系统语音识别服务。");return;}if(recognizer!=null)recognizer.destroy();recognizer=SpeechRecognizer.createSpeechRecognizer(requireContext());recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){speak.setText("正在听…");}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){speak.setText("🎙 再跟读一次");}public void onError(int e){result.setText("没有识别清楚，再说一次目标词："+target);speak.setText("🎙 再跟读一次");}public void onResults(Bundle b){ArrayList<String> xs=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);String heard=xs==null||xs.isEmpty()?"":xs.get(0);boolean ok=norm(heard).contains(norm(target));progress.recordAuxiliaryResult("pronunciation",ok,0);result.setText((ok?"✓ 跟读识别正确":"继续调整发音")+"\n识别到："+heard+"\n目标："+target+"\n"+current().note);result.setTextColor(ContextCompat.getColor(requireContext(),ok?R.color.success:R.color.error));}public void onPartialResults(Bundle b){}public void onEvent(int t,Bundle b){}});Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"it-IT");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);recognizer.startListening(i);}
    private String norm(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ITALIAN).replaceAll("[^a-z]","");}
    @Override public void onRequestPermissionsResult(int r,@NonNull String[] p,@NonNull int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_AUDIO&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startSpeech();}
    @Override public void onDestroyView(){if(recognizer!=null)recognizer.destroy();if(audio!=null)audio.release();super.onDestroyView();}
}
