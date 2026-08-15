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
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FreeConversationFragment extends Fragment {
    private static final int REQ_AUDIO=772;
    private DialogueRepository dialogueRepo;private ScenarioRepository scenarioRepo;private ProgressStore progress;private AudioPlayer audio;private SpeechRecognizer recognizer;
    private Spinner spinner;private TextView stats,transcript,npc,npcZh,feedback;private ScrollView transcriptScroll;private EditText input;private MaterialButton send,mic,next,hint,replay;
    private DialogueScenario current;private int turnIndex=0;private long startedAt=0L;private String requestedScenario="";private final StringBuilder history=new StringBuilder();

    public static FreeConversationFragment newInstance(String scenarioId){FreeConversationFragment f=new FreeConversationFragment();Bundle b=new Bundle();b.putString("scenarioId",scenarioId);f.setArguments(b);return f;}

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_free_conversation,container,false);dialogueRepo=DialogueRepository.get(requireContext());scenarioRepo=ScenarioRepository.get(requireContext());progress=new ProgressStore(requireContext());audio=new AudioPlayer(requireContext(),progress);
        spinner=v.findViewById(R.id.spinner_free_scenario);stats=v.findViewById(R.id.text_free_stats);transcript=v.findViewById(R.id.text_free_transcript);transcriptScroll=v.findViewById(R.id.scroll_free_transcript);npc=v.findViewById(R.id.text_free_npc);npcZh=v.findViewById(R.id.text_free_npc_zh);feedback=v.findViewById(R.id.text_free_feedback);input=v.findViewById(R.id.edit_free_answer);send=v.findViewById(R.id.button_free_send);mic=v.findViewById(R.id.button_free_mic);next=v.findViewById(R.id.button_free_next);hint=v.findViewById(R.id.button_free_hint);replay=v.findViewById(R.id.button_free_replay);
        v.findViewById(R.id.button_free_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());
        List<String> names=new ArrayList<>();for(DialogueScenario d:dialogueRepo.all())names.add(d.emoji+" "+d.title);ArrayAdapter<String> a=new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_item,names);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(a);
        requestedScenario=getArguments()!=null?getArguments().getString("scenarioId",""):"";
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View vv,int pos,long id){loadScenario(pos);}});
        if(!requestedScenario.isEmpty()){for(int i=0;i<dialogueRepo.all().size();i++)if(dialogueRepo.all().get(i).id.equals(requestedScenario)){spinner.setSelection(i);break;}}
        send.setOnClickListener(x->submit());mic.setOnClickListener(x->requestOrStartSpeech());next.setOnClickListener(x->advance());hint.setOnClickListener(x->showHint());replay.setOnClickListener(x->{DialogueTurn t=turn();if(t!=null)audio.speak(t.npc);});
        input.setOnEditorActionListener((tv,action,event)->{if(action==EditorInfo.IME_ACTION_SEND||action==EditorInfo.IME_ACTION_DONE){submit();return true;}return false;});refreshStats();return v;
    }
    private void loadScenario(int pos){if(pos<0||pos>=dialogueRepo.all().size())return;current=dialogueRepo.all().get(pos);turnIndex=0;history.setLength(0);feedback.setText("");next.setVisibility(View.GONE);appendSystem("场景："+current.emoji+" "+current.title+"。不要选答案，直接用自己的意大利语回答。\n");showTurn();}
    private DialogueTurn turn(){return current==null||current.turns.isEmpty()?null:current.turns.get(Math.floorMod(turnIndex,current.turns.size()));}
    private void showTurn(){DialogueTurn t=turn();if(t==null){npc.setText("暂无对话");npcZh.setText("");return;}npc.setText(t.npc);npcZh.setText(t.npcZh);input.setText("");input.setEnabled(true);send.setEnabled(true);mic.setEnabled(true);next.setVisibility(View.GONE);feedback.setText("");startedAt=System.currentTimeMillis();appendNpc(t.npc,t.npcZh);npc.postDelayed(()->audio.speak(t.npc),180);}
    private void submit(){DialogueTurn t=turn();if(t==null)return;String user=input.getText()==null?"":input.getText().toString().trim();if(user.isEmpty()){feedback.setText("先说或输入一句意大利语。");return;}long ms=Math.max(1,System.currentTimeMillis()-startedAt);ConversationResult r=new FreeConversationEngine().evaluate(t,user);appendUser(user);appendNpc(r.replyIt,r.replyZh);progress.recordAuxiliaryResult("freechat",r.understood,ms);for(GrammarIssue issue:r.grammarIssues)progress.recordGrammarResult(issue.id,false,ms);
        StringBuilder fb=new StringBuilder();fb.append(r.understood?"✓ 对方能理解你的意思":"△ 还需要换一种表达").append(" · 匹配度 ").append(r.score).append("%\n").append(r.coach);
        if(!r.grammarIssues.isEmpty()){GrammarIssue g=r.grammarIssues.get(0);fb.append("\n\n语法提醒：").append(g.title).append("\n").append(g.message).append("\n").append(g.suggestion);}
        if(!r.understood)fb.append("\n\n建议表达：").append(r.suggested).append(zhFor(r.suggested));
        feedback.setText(fb.toString());feedback.setTextColor(ContextCompat.getColor(requireContext(),r.understood?R.color.success:R.color.error));input.setEnabled(!r.understood);send.setEnabled(!r.understood);mic.setEnabled(!r.understood);next.setVisibility(r.understood?View.VISIBLE:View.GONE);refreshStats();}
    private void advance(){turnIndex++;showTurn();}
    private void showHint(){DialogueTurn t=turn();if(t==null||t.choices.isEmpty())return;String s=t.choices.get(Math.max(0,Math.min(t.correct,t.choices.size()-1)));feedback.setText("提示：先自己组织，再参考这个自然说法。\n"+s+zhFor(s));feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));}
    private String zhFor(String it){Scenario s=current==null?null:scenarioRepo.find(current.id);if(s==null)return"";String target=norm(it);for(ScenarioPhrase p:s.phrases){if(norm(p.it).equals(target))return"\n"+p.zh;}return"";}
    private String norm(String s){if(s==null)return"";return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ITALIAN).replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();}
    private void appendSystem(String s){history.append("💡 ").append(s).append('\n');renderHistory();}
    private void appendNpc(String it,String zh){history.append("🇮🇹 对方：").append(it);if(zh!=null&&!zh.isEmpty())history.append("\n   ").append(zh);history.append("\n\n");renderHistory();}
    private void appendUser(String s){history.append("🗣 你：").append(s).append("\n\n");renderHistory();}
    private void renderHistory(){transcript.setText(history.toString());transcriptScroll.post(()->transcriptScroll.fullScroll(View.FOCUS_DOWN));}
    private void refreshStats(){stats.setText("自由对话 "+progress.auxiliaryAttempts("freechat")+" 轮 · 理解率 "+progress.auxiliaryAccuracy("freechat")+"% · 语音和文字都可以");}

    private void requestOrStartSpeech(){if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}startSpeech();}
    private void startSpeech(){if(!SpeechRecognizer.isRecognitionAvailable(requireContext())){feedback.setText("这台手机暂时没有可用的系统语音识别服务。");return;}if(recognizer!=null){recognizer.destroy();recognizer=null;}recognizer=SpeechRecognizer.createSpeechRecognizer(requireContext());recognizer.setRecognitionListener(new RecognitionListener(){
        public void onReadyForSpeech(Bundle b){mic.setText("🎙 正在听...");}public void onBeginningOfSpeech(){}public void onRmsChanged(float rms){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){mic.setText("🎙 语音输入");}public void onError(int error){mic.setText("🎙 语音输入");feedback.setText("没有识别清楚，再说一次即可。");}
        public void onResults(Bundle b){mic.setText("🎙 语音输入");ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty()){input.setText(r.get(0));input.setSelection(input.length());}}
        public void onPartialResults(Bundle b){}public void onEvent(int t,Bundle b){}
    });Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"it-IT");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"it-IT");i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Parla in italiano");recognizer.startListening(i);}
    @Override public void onRequestPermissionsResult(int req,@NonNull String[] perms,@NonNull int[] grants){super.onRequestPermissionsResult(req,perms,grants);if(req==REQ_AUDIO&&grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)startSpeech();}
    @Override public void onDestroyView(){if(recognizer!=null)recognizer.destroy();if(audio!=null)audio.release();super.onDestroyView();}
}
