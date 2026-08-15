package com.italiano2774.nativeapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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

public class SentencePatternFragment extends Fragment {
    public static SentencePatternFragment newInstance(String patternId){SentencePatternFragment f=new SentencePatternFragment();Bundle b=new Bundle();b.putString("patternId",patternId);f.setArguments(b);return f;}
    private SentencePatternRepository repository;private ProgressStore progress;private AudioPlayer audio;
    private Spinner spinner;private TextView stats,title,formula,explanation,progressText,prompt,feedback,example,microBody,microProgress;
    private EditText input;private MaterialButton check,next,audioButton,microStart;private View microPanel;
    private SentencePattern currentPattern;private int exerciseIndex=0;private long startedAt=0L;private boolean answered=false;
    private boolean remedialMode=false;private int remedialDone=0,remedialCorrect=0;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle state){
        View v=i.inflate(R.layout.fragment_sentence_patterns,c,false);repository=SentencePatternRepository.get(requireContext());progress=new ProgressStore(requireContext());audio=new AudioPlayer(requireContext(),progress);
        spinner=v.findViewById(R.id.spinner_pattern);stats=v.findViewById(R.id.text_pattern_stats);title=v.findViewById(R.id.text_pattern_title);formula=v.findViewById(R.id.text_pattern_formula);explanation=v.findViewById(R.id.text_pattern_explanation);progressText=v.findViewById(R.id.text_pattern_progress);prompt=v.findViewById(R.id.text_pattern_prompt);feedback=v.findViewById(R.id.text_pattern_feedback);example=v.findViewById(R.id.text_pattern_example);input=v.findViewById(R.id.edit_pattern_answer);check=v.findViewById(R.id.button_pattern_check);next=v.findViewById(R.id.button_pattern_next);audioButton=v.findViewById(R.id.button_pattern_audio);
        microPanel=v.findViewById(R.id.micro_lesson_panel);microBody=v.findViewById(R.id.text_micro_lesson);microProgress=v.findViewById(R.id.text_micro_progress);microStart=v.findViewById(R.id.button_micro_start);
        v.findViewById(R.id.button_pattern_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());
        List<String> names=new ArrayList<>();for(SentencePattern p:repository.all())names.add(p.title);ArrayAdapter<String> a=new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_item,names);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(a);
        String wanted=getArguments()!=null?getArguments().getString("patternId",""):"";if(!wanted.isEmpty()){for(int idx=0;idx<repository.all().size();idx++)if(repository.all().get(idx).id.equals(wanted)){spinner.setSelection(idx);break;}}
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){loadPattern(pos);}});if(currentPattern==null)loadPattern(spinner.getSelectedItemPosition());
        check.setOnClickListener(x->checkAnswer());next.setOnClickListener(x->nextExercise());audioButton.setOnClickListener(x->{PatternExercise e=currentExercise();if(e!=null)audio.speak(e.it);});microStart.setOnClickListener(x->startMicroDrill());
        input.setOnEditorActionListener((tv,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){checkAnswer();return true;}return false;});refreshStats();return v;
    }

    private void loadPattern(int pos){
        if(pos<0||pos>=repository.all().size())return;currentPattern=repository.all().get(pos);exerciseIndex=0;remedialMode=false;remedialDone=0;remedialCorrect=0;microPanel.setVisibility(View.GONE);
        title.setText(currentPattern.title);formula.setText(currentPattern.formula);explanation.setText(currentPattern.explanation);showExercise();
    }
    private PatternExercise currentExercise(){if(currentPattern==null||currentPattern.exercises.isEmpty())return null;return currentPattern.exercises.get(exerciseIndex%currentPattern.exercises.size());}
    private void showExercise(){
        PatternExercise e=currentExercise();if(e==null)return;answered=false;startedAt=System.currentTimeMillis();
        progressText.setText(remedialMode?("20秒微课强化 · 第 "+(remedialDone+1)+" / 3 题"):("第 "+(exerciseIndex+1)+" / "+currentPattern.exercises.size()+" 题"));
        prompt.setText(e.prompt);input.setText("");input.setEnabled(true);check.setEnabled(true);feedback.setText("");example.setVisibility(View.GONE);next.setText(remedialMode?"下一道强化题 →":(exerciseIndex+1>=currentPattern.exercises.size()?"重新练这一句型":"下一题 →"));
    }
    private String normalize(String s){if(s==null)return"";String n=Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ITALIAN).trim();n=n.replace('’','\'');return n.replaceAll("[^a-z0-9' ]"," ").replaceAll("\\s+"," ").trim();}

    private void checkAnswer(){
        if(answered)return;PatternExercise e=currentExercise();if(e==null)return;String user=input.getText()==null?"":input.getText().toString();boolean ok=normalize(user).equals(normalize(e.answer));answered=true;long ms=Math.max(1,System.currentTimeMillis()-startedAt);progress.recordPatternResult(currentPattern.id,ok,ms);
        feedback.setText(ok?"✓ 正确":"✗ 正确答案："+e.answer);feedback.setTextColor(ContextCompat.getColor(requireContext(),ok?R.color.success:R.color.error));example.setText(e.it+"\n"+e.zh);example.setVisibility(View.VISIBLE);input.setEnabled(false);check.setEnabled(false);audio.speak(e.it);refreshStats();
        if(remedialMode){remedialDone++;if(ok)remedialCorrect++;microProgress.setText("强化进度 "+remedialDone+" / 3 · 正确 "+remedialCorrect);if(remedialDone>=3){remedialMode=false;microStart.setVisibility(View.VISIBLE);microStart.setText(remedialCorrect>=2?"✓ 已完成 · 再练3题":"再来3题，直到稳定");microProgress.setText("本轮 "+remedialCorrect+" / 3 正确"+(remedialCorrect>=2?" · 可以继续":" · 建议立刻再练"));}}
        else if(!ok){showMicroLesson(e);}
    }

    private void showMicroLesson(PatternExercise e){
        microPanel.setVisibility(View.VISIBLE);microStart.setVisibility(View.VISIBLE);microStart.setText("开始3题即时强化");microProgress.setText("先用约20秒看懂规则，再做3题");
        microBody.setText(currentPattern.formula+"\n\n"+currentPattern.explanation+"\n\n刚才这题：\n"+e.it+"\n"+e.zh);
    }
    private void startMicroDrill(){
        if(currentPattern==null||currentPattern.exercises.isEmpty())return;remedialMode=true;remedialDone=0;remedialCorrect=0;microStart.setVisibility(View.GONE);microProgress.setText("强化进度 0 / 3");exerciseIndex=(exerciseIndex+1)%currentPattern.exercises.size();showExercise();
    }
    private void nextExercise(){if(currentPattern==null)return;exerciseIndex++;if(exerciseIndex>=currentPattern.exercises.size())exerciseIndex=0;showExercise();}
    private void refreshStats(){stats.setText("句型练习："+progress.auxiliaryAttempts("pattern")+" 题 · 正确率 "+progress.auxiliaryAccuracy("pattern")+"% · 答错后自动弹出20秒微课");}
    @Override public void onDestroyView(){if(audio!=null)audio.release();super.onDestroyView();}
}
