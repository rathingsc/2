package com.italiano2774.nativeapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CalendarFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){
        View v=i.inflate(R.layout.fragment_calendar,c,false);WordRepository r=WordRepository.get(requireContext());ProgressStore p=new ProgressStore(requireContext());int total=r.totalDays(p.startDate(),p.perDay());
        ((TextView)v.findViewById(R.id.text_calendar_summary)).setText("从 "+p.startDate()+" 开始 · 每天 "+p.perDay()+" 项 · 共 "+total+" 天");
        ((TextView)v.findViewById(R.id.text_stat_b1)).setText("B1 可用\n"+p.b1Count(r.all())+" / 1600");
        ((TextView)v.findViewById(R.id.text_stat_mastered)).setText("完全掌握\n"+p.strongCount(r.all())+" / "+r.size());
        ((TextView)v.findViewById(R.id.text_stat_accuracy)).setText("练习正确率\n"+p.totalAccuracy(r.all())+"%");
        ((TextView)v.findViewById(R.id.text_stat_streak)).setText("连续学习\n"+p.activityStreak()+" 天");
        renderWeek(v,p);
        RecyclerView rv=v.findViewById(R.id.recycler_calendar);rv.setLayoutManager(new GridLayoutManager(requireContext(),2));rv.setAdapter(new CalendarAdapter(r,p,d->((MainActivity)requireActivity()).openToday(d)));return v;
    }

    private void renderWeek(View root,ProgressStore p){
        LinearLayout box=root.findViewById(R.id.layout_week_stats);box.removeAllViews();LocalDate today=LocalDate.now();int max=1,totalPractice=0,totalCards=0,totalCorrect=0;
        for(int i=6;i>=0;i--){LocalDate d=today.minusDays(i);max=Math.max(max,p.dailyActivity(d));totalPractice+=p.dailyAttempts(d);totalCards+=p.dailyCards(d);totalCorrect+=p.dailyCorrect(d);}
        int accuracy=totalPractice==0?0:(int)Math.round(totalCorrect*100.0/totalPractice);((TextView)root.findViewById(R.id.text_week_summary)).setText("学习动作 "+(totalPractice+totalCards)+" 次 · 练习 "+totalPractice+" 题 · 正确率 "+accuracy+"%");
        for(int i=6;i>=0;i--){
            LocalDate d=today.minusDays(i);int act=p.dailyActivity(d);LinearLayout cell=new LinearLayout(requireContext());cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(48),ViewGroup.LayoutParams.MATCH_PARENT);cp.setMargins(dp(3),0,dp(3),0);cell.setLayoutParams(cp);
            TextView count=new TextView(requireContext());count.setText(String.valueOf(act));count.setTextSize(11);count.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));count.setGravity(Gravity.CENTER);cell.addView(count,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(20)));
            FrameLayout track=new FrameLayout(requireContext());track.setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.bg));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(dp(22),dp(58));cell.addView(track,tp);
            View bar=new View(requireContext());bar.setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.blue));int h=act==0?dp(3):Math.max(dp(5),(int)Math.round(dp(56)*act/(double)max));FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,h,Gravity.BOTTOM);track.addView(bar,bp);
            TextView date=new TextView(requireContext());date.setText(d.format(DateTimeFormatter.ofPattern("M/d")));date.setTextSize(10);date.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));date.setGravity(Gravity.CENTER);cell.addView(date,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(24)));box.addView(cell);
        }
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
