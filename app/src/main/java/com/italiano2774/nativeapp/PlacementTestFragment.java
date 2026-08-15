package com.italiano2774.nativeapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PlacementTestFragment extends Fragment {
    private WordRepository repo;private ProgressStore progress;private final Random random=new Random();
    private final List<Word> sample=new ArrayList<>();private final List<Integer> strongIds=new ArrayList<>();private final int[] bandCorrect=new int[8];
    private final List<MaterialButton> buttons=new ArrayList<>();private TextView word,ipa,progressText,result,intro;private ProgressBar bar;private int index=0;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_placement_test,container,false);repo=WordRepository.get(requireContext());progress=new ProgressStore(requireContext());sample.addAll(repo.placementSample());
        word=v.findViewById(R.id.text_placement_word);ipa=v.findViewById(R.id.text_placement_ipa);progressText=v.findViewById(R.id.text_placement_progress);result=v.findViewById(R.id.text_placement_result);intro=v.findViewById(R.id.text_placement_intro);bar=v.findViewById(R.id.progress_placement);
        buttons.add(v.findViewById(R.id.placement_answer_1));buttons.add(v.findViewById(R.id.placement_answer_2));buttons.add(v.findViewById(R.id.placement_answer_3));buttons.add(v.findViewById(R.id.placement_answer_4));
        v.findViewById(R.id.button_placement_back).setOnClickListener(x->((MainActivity)requireActivity()).openToday(java.time.LocalDate.now()));v.findViewById(R.id.button_placement_finish).setOnClickListener(x->((MainActivity)requireActivity()).openToday(java.time.LocalDate.now()));
        showQuestion();return v;
    }

    private void showQuestion(){
        if(index>=sample.size()){finish();return;}Word target=sample.get(index);progressText.setText("第 "+(index+1)+" / "+sample.size()+" 题 · 选择最常用的中文意思");bar.setProgress((int)Math.round(index*100.0/sample.size()));word.setText(target.word);ipa.setText(target.ipa);
        List<Word> opts=options(target);for(int i=0;i<4;i++){MaterialButton b=buttons.get(i);Word w=opts.get(i);b.setEnabled(true);b.setText(chinese(w));b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.line)));b.setTag(w.id);b.setOnClickListener(x->answer(target,w));}
    }
    private void answer(Word target,Word chosen){boolean ok=target.id==chosen.id;if(ok){bandCorrect[index/5]++;strongIds.add(target.id);}for(MaterialButton b:buttons)b.setEnabled(false);index++;word.postDelayed(this::showQuestion,180);}
    private List<Word> options(Word target){List<Word> candidates=new ArrayList<>();int pos=Math.max(0,target.id-1),from=Math.max(0,pos-120),to=Math.min(repo.size(),pos+121);for(int i=from;i<to;i++){Word w=repo.all().get(i);if(w.id!=target.id&&!chinese(w).equals(chinese(target)))candidates.add(w);}Collections.shuffle(candidates,random);List<Word> out=new ArrayList<>();out.add(target);for(Word w:candidates){if(out.size()==4)break;out.add(w);}while(out.size()<4){Word w=repo.all().get(random.nextInt(repo.size()));if(w.id!=target.id&&!out.contains(w))out.add(w);}Collections.shuffle(out,random);return out;}
    private String chinese(Word w){return w.chinese==null||w.chinese.trim().isEmpty()?w.english:w.chinese;}
    private void finish(){
        int passed=0;for(int b=0;b<8;b++){if(bandCorrect[b]>=4)passed++;else break;}int known=repo.placementBoundaryForPassedBands(passed);progress.applyPlacement(repo.all(),known,strongIds);bar.setProgress(100);progressText.setText("测试完成");
        word.setText("完成 🎯");ipa.setText("");for(MaterialButton b:buttons)b.setVisibility(View.GONE);intro.setVisibility(View.GONE);result.setVisibility(View.VISIBLE);result.setText("保守估算：你已经认识约 "+known+" 个课程词条。\n\n已连续通过 "+passed+" / 8 个课程区段。系统把这些词标记为“已接触/能认出”，以后不会当成全新词重复灌输，但听力、拼写和口语仍会按弱项继续复习。\n\n这不会直接增加B1可用词汇，避免水平被高估。");requireView().findViewById(R.id.button_placement_finish).setVisibility(View.VISIBLE);
    }
}
