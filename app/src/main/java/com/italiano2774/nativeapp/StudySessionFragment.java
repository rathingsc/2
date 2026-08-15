package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StudySessionFragment extends Fragment {
    private WordRepository repo;private ProgressStore progress;private AudioPlayer audio;private LocalDate date;private boolean adaptive=false;private int maxCards=0;
    private final List<Word> session=new ArrayList<>();private int index=0,again=0,hard=0,good=0,easy=0;
    private TextView progressText,topic,word,ipa,pron,chinese,english,lemma,grammar,example,exampleZh,ratingHint,summary;
    private ProgressBar progressBar;private LinearLayout answerPanel,examplePanel;private GridLayout ratings;private MaterialButton showAnswer;

    public static StudySessionFragment newInstance(LocalDate d){StudySessionFragment f=new StudySessionFragment();Bundle b=new Bundle();b.putString("date",d.toString());b.putBoolean("adaptive",false);f.setArguments(b);return f;}
    public static StudySessionFragment newAdaptiveInstance(){return newAdaptiveInstance(0);}
    public static StudySessionFragment newAdaptiveInstance(int maxCards){StudySessionFragment f=new StudySessionFragment();Bundle b=new Bundle();b.putString("date",LocalDate.now().toString());b.putBoolean("adaptive",true);b.putInt("maxCards",maxCards);f.setArguments(b);return f;}

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_study_session,container,false);repo=WordRepository.get(requireContext());progress=new ProgressStore(requireContext());audio=new AudioPlayer(requireContext(),progress);
        date=getArguments()!=null?LocalDate.parse(getArguments().getString("date",LocalDate.now().toString())):LocalDate.now();adaptive=getArguments()!=null&&getArguments().getBoolean("adaptive",false);maxCards=getArguments()!=null?getArguments().getInt("maxCards",0):0;
        progressText=v.findViewById(R.id.text_session_progress);progressBar=v.findViewById(R.id.progress_session);topic=v.findViewById(R.id.text_session_topic);word=v.findViewById(R.id.text_session_word);ipa=v.findViewById(R.id.text_session_ipa);
        pron=v.findViewById(R.id.text_session_pron);chinese=v.findViewById(R.id.text_session_chinese);english=v.findViewById(R.id.text_session_english);lemma=v.findViewById(R.id.text_session_lemma);grammar=v.findViewById(R.id.text_session_grammar);example=v.findViewById(R.id.text_session_example);exampleZh=v.findViewById(R.id.text_session_example_zh);
        answerPanel=v.findViewById(R.id.panel_answer);examplePanel=v.findViewById(R.id.panel_example);ratings=v.findViewById(R.id.panel_ratings);ratingHint=v.findViewById(R.id.text_rating_hint);summary=v.findViewById(R.id.text_session_summary);showAnswer=v.findViewById(R.id.button_show_answer);
        v.findViewById(R.id.button_back_today).setOnClickListener(x->((MainActivity)requireActivity()).openToday(LocalDate.now()));
        v.findViewById(R.id.button_session_audio).setOnClickListener(x->{Word w=current();if(w!=null)audio.play(w);});
        v.findViewById(R.id.button_example_audio).setOnClickListener(x->{Word w=current();if(w!=null)audio.speak(w.example);});
        showAnswer.setOnClickListener(x->reveal());
        v.findViewById(R.id.button_rating_again).setOnClickListener(x->rate(0));v.findViewById(R.id.button_rating_hard).setOnClickListener(x->rate(1));v.findViewById(R.id.button_rating_good).setOnClickListener(x->rate(2));v.findViewById(R.id.button_rating_easy).setOnClickListener(x->rate(3));
        if(adaptive){List<Word> adaptiveWords=repo.adaptivePlan(progress).words;int n=maxCards>0?Math.min(maxCards,adaptiveWords.size()):adaptiveWords.size();session.addAll(adaptiveWords.subList(0,n));}else{List<Word> today=repo.forDate(progress.startDate(),date,progress.perDay());for(Word w:today)if(progress.mastery(w.id)<4)session.add(w);}
        showCard();return v;
    }

    private Word current(){return index>=0&&index<session.size()?session.get(index):null;}
    private void showCard(){
        answerPanel.setVisibility(View.GONE);ratings.setVisibility(View.GONE);ratingHint.setVisibility(View.GONE);summary.setVisibility(View.GONE);showAnswer.setVisibility(View.VISIBLE);
        if(session.isEmpty()){
            progressText.setText(adaptive?"今天的智能任务已经完成":"今天的新词已经全部达到4级+");progressBar.setProgress(100);topic.setText("今日完成");word.setText("🎉");ipa.setText("没有待学内容");pron.setVisibility(View.GONE);showAnswer.setVisibility(View.GONE);summary.setVisibility(View.VISIBLE);summary.setText("可以去“练习”继续处理听力、拼写、口语和易混词弱项。");return;
        }
        if(index>=session.size()){
            int total=session.size();progressText.setText(total+" / "+total+" 完成");progressBar.setProgress(100);topic.setText(adaptive?"智能任务完成":"学习完成");word.setText("做得很好 🎉");ipa.setText("这一轮已经结束");pron.setVisibility(View.GONE);showAnswer.setVisibility(View.GONE);summary.setVisibility(View.VISIBLE);
            summary.setText("不会 "+again+" · 模糊 "+hard+" · 会 "+good+" · 很熟 "+easy+"\n系统已经根据你的选择自动安排下次复习。四维弱项可继续在练习中心专项训练。");return;
        }
        Word w=current();int pct=(int)Math.round(index*100.0/session.size());progressBar.setProgress(pct);progressText.setText((index+1)+" / "+session.size()+(adaptive?" · 智能今日任务":" · 今天待学"));topic.setText(w.level+" · #"+w.num+" · 弱项："+progress.weakestDimensionName(w.id));word.setText(w.word);ipa.setText(w.ipa==null?"":w.ipa);
        pron.setText(w.zhPron==null?"":w.zhPron);pron.setVisibility(progress.shouldShowPronunciation(w.id,false)?View.VISIBLE:View.GONE);chinese.setText(safe(w.chinese,w.english));english.setText("英文参考："+w.english);
        boolean hasLemma=w.lemma!=null&&!w.lemma.trim().isEmpty(),hasForm=w.formInfo!=null&&!w.formInfo.trim().isEmpty();if(hasLemma||hasForm){lemma.setVisibility(View.VISIBLE);String head=hasLemma?(w.lemma.equalsIgnoreCase(w.word)?("原形："+w.word):("词形："+w.word+" → "+w.lemma)):"词形提示";lemma.setText(head+(hasForm?"\n"+w.formInfo:""));}else lemma.setVisibility(View.GONE);
        String grammarText=ItalianGrammar.grammarPanel(w);grammar.setVisibility(grammarText.isEmpty()?View.GONE:View.VISIBLE);if(!grammarText.isEmpty())grammar.setText(grammarText);
        boolean hasExample=w.example!=null&&!w.example.trim().isEmpty();examplePanel.setVisibility(hasExample?View.VISIBLE:View.GONE);if(hasExample){example.setText(w.example);exampleZh.setText(w.exampleZh==null?"":w.exampleZh);}
    }
    private String safe(String a,String b){return a==null||a.trim().isEmpty()?b:a;}
    private void reveal(){answerPanel.setVisibility(View.VISIBLE);ratings.setVisibility(View.VISIBLE);ratingHint.setVisibility(View.VISIBLE);showAnswer.setVisibility(View.GONE);Word w=current();if(w!=null&&progress.shouldShowPronunciation(w.id,true))pron.setVisibility(View.VISIBLE);}
    private void rate(int rating){Word w=current();if(w==null)return;progress.recordStudyRating(w.id,rating);if(rating==0)again++;else if(rating==1)hard++;else if(rating==2)good++;else easy++;index++;showCard();}
    @Override public void onDestroyView(){if(audio!=null)audio.release();super.onDestroyView();}
}
