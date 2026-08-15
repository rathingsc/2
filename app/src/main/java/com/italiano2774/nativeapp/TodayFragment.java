package com.italiano2774.nativeapp;

import android.os.Bundle;import android.view.*;import android.widget.*;import androidx.annotation.*;import androidx.fragment.app.Fragment;import androidx.recyclerview.widget.LinearLayoutManager;import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import java.time.LocalDate;import java.time.format.DateTimeFormatter;import java.util.List;import java.util.Locale;

public class TodayFragment extends Fragment {
    private WordRepository repo;private ProgressStore progress;private AudioPlayer audio;private WordAdapter adapter;private LocalDate date;
    private TextView textDate,textTitle,textRange,textB1,textDue,textProgress,textWordSection,textPlan;private ProgressBar bar;private MaterialButtonToggleGroup timeGroup;
    public static TodayFragment newInstance(LocalDate d){TodayFragment f=new TodayFragment();Bundle b=new Bundle();b.putString("date",d.toString());f.setArguments(b);return f;}
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_today,container,false);repo=WordRepository.get(requireContext());progress=new ProgressStore(requireContext());audio=new AudioPlayer(requireContext(),progress);
        date=getArguments()!=null?LocalDate.parse(getArguments().getString("date",LocalDate.now().toString())):LocalDate.now();
        textDate=v.findViewById(R.id.text_date);textTitle=v.findViewById(R.id.text_day_title);textRange=v.findViewById(R.id.text_range);textB1=v.findViewById(R.id.text_b1);textDue=v.findViewById(R.id.text_due);textProgress=v.findViewById(R.id.text_today_progress);textWordSection=v.findViewById(R.id.text_word_section);textPlan=v.findViewById(R.id.text_adaptive_plan);bar=v.findViewById(R.id.progress_today);timeGroup=v.findViewById(R.id.group_time_budget);
        RecyclerView rv=v.findViewById(R.id.recycler_words);rv.setLayoutManager(new LinearLayoutManager(requireContext()));adapter=new WordAdapter(requireContext(),progress,audio,this::refresh);rv.setAdapter(adapter);
        v.findViewById(R.id.button_start_session).setOnClickListener(x->((MainActivity)requireActivity()).openAdaptiveStudy());
        v.findViewById(R.id.button_placement_test).setOnClickListener(x->((MainActivity)requireActivity()).openPlacementTest());
        v.findViewById(R.id.button_personal_course).setOnClickListener(x->((MainActivity)requireActivity()).openPersonalCourse());
        v.findViewById(R.id.button_prev).setOnClickListener(x->{date=date.minusDays(1);refresh();});v.findViewById(R.id.button_today).setOnClickListener(x->{date=LocalDate.now();refresh();});v.findViewById(R.id.button_next).setOnClickListener(x->{date=date.plusDays(1);refresh();});
        v.findViewById(R.id.button_mark3).setOnClickListener(x->{for(Word w:repo.forDate(progress.startDate(),date,progress.perDay()))progress.setMastery(w.id,3);refresh();});v.findViewById(R.id.button_mark4).setOnClickListener(x->{for(Word w:repo.forDate(progress.startDate(),date,progress.perDay()))progress.setMastery(w.id,4);refresh();});
        timeGroup.addOnButtonCheckedListener((group,checked,isChecked)->{if(!isChecked)return;int m=checked==R.id.button_time_5?5:(checked==R.id.button_time_15?15:(checked==R.id.button_time_60?60:30));if(progress.sessionMinutes()!=m){progress.setSessionMinutes(m);refreshPlan();}});
        setTimeSelection();refresh();return v;}
    private void setTimeSelection(){int id=progress.sessionMinutes()==5?R.id.button_time_5:(progress.sessionMinutes()==15?R.id.button_time_15:(progress.sessionMinutes()==60?R.id.button_time_60:R.id.button_time_30));timeGroup.check(id);}
    private void refreshPlan(){DailyPlan plan=repo.adaptivePlan(progress);PersonalizedCourse course=new PersonalizedCourseEngine(requireContext(),repo,progress).build();String weak="识义 "+plan.meaningWeak+" · 听力 "+plan.listeningWeak+" · 拼写 "+plan.spellingWeak+" · 口语 "+plan.speakingWeak;textPlan.setText("FSRS目标保持率 "+(int)Math.round(progress.desiredRetention()*100)+"% · 最近7天正确率 "+progress.sevenDayAccuracy()+"%\n个性课程："+course.shortSummary()+"\n动态新词：建议 "+plan.newQuota+" 个 · 当前任务实际加入 "+plan.newCount+" 个\n当前重点："+course.weakDimensionName+" · 语法："+course.grammarTitle+"\n弱项分布："+weak);}
    private void refresh(){
        List<Word> list=repo.forDate(progress.startDate(),date,progress.perDay());int idx=repo.dayIndex(progress.startDate(),date),total=repo.totalDays(progress.startDate(),progress.perDay());
        textDate.setText(date.format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE",Locale.CHINA)));textTitle.setText(idx>=0&&idx<total?"第 "+(idx+1)+" / "+total+" 天":"这一天没有新词");
        if(list.isEmpty())textRange.setText("请调整日期或学习计划");else textRange.setText(list.get(0).num+" – "+list.get(list.size()-1).num+" · "+list.get(0).level+" → "+list.get(list.size()-1).level);
        int done=progress.countAtLeast(list,3),pct=list.isEmpty()?0:(int)Math.round(done*100.0/list.size());bar.setProgress(pct);textProgress.setText(done+" / "+list.size());textB1.setText(progress.b1Count(repo.all())+" / 1600");textDue.setText("待复习 "+progress.dueCount(repo.all(),LocalDate.now()));
        String placement=progress.placementCompleted()?" · 测试跳过约 "+progress.placementKnownEstimate()+"词":"";textWordSection.setText(list.isEmpty()?"课程顺序词表":"课程顺序词表 · "+list.size()+"个"+placement);adapter.submit(list);refreshPlan();
    }
    @Override public void onDestroyView(){if(audio!=null)audio.release();super.onDestroyView();}
}
