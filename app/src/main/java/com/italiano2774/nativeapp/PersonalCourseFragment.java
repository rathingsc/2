package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PersonalCourseFragment extends Fragment {
    private WordRepository repo;private ProgressStore progress;private PersonalizedCourse course;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_personal_course,parent,false);repo=WordRepository.get(requireContext());progress=new ProgressStore(requireContext());course=new PersonalizedCourseEngine(requireContext(),repo,progress).build();
        ((TextView)v.findViewById(R.id.text_course_title)).setText("今日 "+course.minutes+" 分钟个性课程");
        ((TextView)v.findViewById(R.id.text_course_summary)).setText(course.shortSummary());
        ((TextView)v.findViewById(R.id.text_course_reason)).setText(course.focusReason);
        ((TextView)v.findViewById(R.id.text_course_vocab)).setText("词汇与复习 · "+course.vocabularyCards+" 张\n"+course.vocabularyPlan.summary());
        ((TextView)v.findViewById(R.id.text_course_grammar)).setText("语法弱点 · "+course.grammarQuestions+" 题\n"+course.grammarTitle);
        ((TextView)v.findViewById(R.id.text_course_chat)).setText("自由对话 · "+course.conversationTurns+" 轮\n"+course.scenarioTitle);
        ((TextView)v.findViewById(R.id.text_course_shadow)).setText("整句 Shadowing + 发音辨音\n当前最弱："+course.weakDimensionName+"。建议至少完成3句跟读，再做1组发音专项。");
        ((TextView)v.findViewById(R.id.text_course_listen)).setText("通勤听力 · "+course.listeningMinutes+" 分钟\n优先巩固今天任务、到期复习和弱项词");
        ((TextView)v.findViewById(R.id.text_course_reading)).setText("分级阅读 · "+course.readingLevel+"\n"+course.readingTitle+"\n用上下文巩固词汇，并完成2道理解题。");
        v.findViewById(R.id.button_course_back).setOnClickListener(x->((MainActivity)requireActivity()).openToday(java.time.LocalDate.now()));
        v.findViewById(R.id.button_course_vocab).setOnClickListener(x->((MainActivity)requireActivity()).openAdaptiveStudy(course.vocabularyCards));
        v.findViewById(R.id.button_course_grammar).setOnClickListener(x->((MainActivity)requireActivity()).openSentencePatterns(course.grammarPatternId));
        v.findViewById(R.id.button_course_chat).setOnClickListener(x->((MainActivity)requireActivity()).openFreeConversation(course.scenarioId));
        v.findViewById(R.id.button_course_shadow).setOnClickListener(x->((MainActivity)requireActivity()).openShadowing());
        v.findViewById(R.id.button_course_pron).setOnClickListener(x->((MainActivity)requireActivity()).openPronunciation());
        v.findViewById(R.id.button_course_listen).setOnClickListener(x->((MainActivity)requireActivity()).openCommute());
        v.findViewById(R.id.button_course_reading).setOnClickListener(x->{if(course.readingId!=null)((MainActivity)requireActivity()).openReading(course.readingId);else ((MainActivity)requireActivity()).openReadingList();});
        return v;
    }
}
