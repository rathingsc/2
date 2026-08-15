package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class GrammarDiagnosisFragment extends Fragment {
    private ProgressStore progress;private LinearLayout container;private TextView summary;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_grammar_diagnosis,parent,false);progress=new ProgressStore(requireContext());container=v.findViewById(R.id.container_grammar_weak);summary=v.findViewById(R.id.text_grammar_summary);
        v.findViewById(R.id.button_grammar_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());
        v.findViewById(R.id.button_grammar_general_practice).setOnClickListener(x->((MainActivity)requireActivity()).openSentencePatterns());
        render();return v;
    }
    private void render(){
        container.removeAllViews();List<GrammarPoint> weak=GrammarDiagnostics.weakest(progress,6);int total=0,correct=0;for(GrammarPoint p:GrammarDiagnostics.all()){total+=progress.grammarAttempts(p.id);correct+=progress.grammarCorrect(p.id);}int accuracy=total==0?0:(int)Math.round(correct*100.0/total);
        summary.setText(total==0?"还没有足够的语法答题数据。做几组句型训练或自由对话后，这里会自动找出最容易出错的语法点。":"已分析 "+total+" 次语法作答 · 总正确率 "+accuracy+"% · 优先显示最值得补的弱点");
        if(weak.isEmpty()){if(total>0)summary.setText("已分析 "+total+" 次语法作答 · 总正确率 "+accuracy+"% · 暂时没有明显语法弱点，继续保持。");addEmptyCard();return;}for(GrammarPoint gp:weak)addPoint(gp);
    }
    private void addEmptyCard(){TextView t=new TextView(requireContext());t.setText("建议先练：Vorrei、Avere bisogno di、Potere、Dovere、C'è / Ci sono。\n完成后回来查看个性化诊断。");t.setTextColor(getResources().getColor(R.color.text_secondary,requireContext().getTheme()));t.setTextSize(14);t.setPadding(dp(14),dp(14),dp(14),dp(14));container.addView(t,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));}
    private void addPoint(GrammarPoint gp){
        MaterialCardView card=new MaterialCardView(requireContext());card.setRadius(dp(18));card.setCardElevation(0);card.setCardBackgroundColor(getResources().getColor(R.color.surface,requireContext().getTheme()));card.setStrokeColor(getResources().getColor(R.color.line,requireContext().getTheme()));card.setStrokeWidth(dp(1));
        LinearLayout box=new LinearLayout(requireContext());box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(15),dp(13),dp(15),dp(13));
        TextView title=new TextView(requireContext());title.setText(gp.title);title.setTextSize(17);title.setTextColor(getResources().getColor(R.color.text_primary,requireContext().getTheme()));title.setTypeface(null,android.graphics.Typeface.BOLD);box.addView(title);
        int att=progress.grammarAttempts(gp.id),mist=progress.grammarMistakes(gp.id),acc=progress.grammarAccuracy(gp.id);TextView metric=new TextView(requireContext());metric.setText("练习 "+att+" 次 · 错误 "+mist+" 次 · 正确率 "+acc+"%");metric.setTextSize(12);metric.setTextColor(getResources().getColor(R.color.error,requireContext().getTheme()));metric.setPadding(0,dp(4),0,0);box.addView(metric);
        TextView tip=new TextView(requireContext());tip.setText(gp.tip);tip.setTextSize(13);tip.setTextColor(getResources().getColor(R.color.text_secondary,requireContext().getTheme()));tip.setPadding(0,dp(7),0,0);box.addView(tip);
        if(gp.practicePatternId!=null){MaterialButton b=new MaterialButton(requireContext());b.setText("立即补这个语法点");b.setAllCaps(false);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));bp.topMargin=dp(9);box.addView(b,bp);b.setOnClickListener(x->((MainActivity)requireActivity()).openSentencePatterns(gp.practicePatternId));}
        card.addView(box);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.topMargin=dp(9);container.addView(card,lp);
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
