package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Internal CEFR-style checkpoint. Course-position based; not an official certification test. */
public class LevelExamFragment extends Fragment {
    private static final int MEANING=0,LISTENING=1,SPELLING=2,GRAMMAR=3,ARTICLE=4;
    private static class Q{int type;String prompt,answer,category,patternId;Word word;PatternExercise exercise;List<String> choices=new ArrayList<>();}
    private WordRepository words;private SentencePatternRepository patterns;private ProgressStore progress;private AudioPlayer audio;private final List<Q> questions=new ArrayList<>();private final Random random=new Random();
    private View intro,examPanel,choicePanel,typePanel,resultPanel;private TextView history,title,progressText,category,question,feedback,result;private EditText input;private MaterialButton replay,next,submit;private final List<MaterialButton> choiceButtons=new ArrayList<>();
    private String level="A1";private int index=0,correct=0;private long started=0L;private final int[] catTotal=new int[5],catCorrect=new int[5];

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_level_exam,parent,false);words=WordRepository.get(requireContext());patterns=SentencePatternRepository.get(requireContext());progress=new ProgressStore(requireContext());audio=new AudioPlayer(requireContext(),progress);
        intro=v.findViewById(R.id.panel_exam_intro);examPanel=v.findViewById(R.id.panel_exam_question);choicePanel=v.findViewById(R.id.panel_exam_choices);typePanel=v.findViewById(R.id.panel_exam_typing);resultPanel=v.findViewById(R.id.panel_exam_result);history=v.findViewById(R.id.text_exam_history);title=v.findViewById(R.id.text_exam_title);progressText=v.findViewById(R.id.text_exam_progress);category=v.findViewById(R.id.text_exam_category);question=v.findViewById(R.id.text_exam_question);feedback=v.findViewById(R.id.text_exam_feedback);result=v.findViewById(R.id.text_exam_result);input=v.findViewById(R.id.edit_exam_answer);replay=v.findViewById(R.id.button_exam_replay);next=v.findViewById(R.id.button_exam_next);submit=v.findViewById(R.id.button_exam_submit);
        choiceButtons.add(v.findViewById(R.id.button_exam_c1));choiceButtons.add(v.findViewById(R.id.button_exam_c2));choiceButtons.add(v.findViewById(R.id.button_exam_c3));choiceButtons.add(v.findViewById(R.id.button_exam_c4));
        v.findViewById(R.id.button_exam_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());v.findViewById(R.id.button_exam_a1).setOnClickListener(x->start("A1"));v.findViewById(R.id.button_exam_a2).setOnClickListener(x->start("A2"));v.findViewById(R.id.button_exam_b1).setOnClickListener(x->start("B1"));v.findViewById(R.id.button_exam_restart).setOnClickListener(x->showIntro());
        for(MaterialButton b:choiceButtons)b.setOnClickListener(x->answerChoice(((MaterialButton)x).getText().toString()));submit.setOnClickListener(x->answerTyped());next.setOnClickListener(x->advance());replay.setOnClickListener(x->{Q q=current();if(q!=null&&q.word!=null)audio.play(q.word);});input.setOnEditorActionListener((tv,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){answerTyped();return true;}return false;});showIntro();return v;
    }
    private void showIntro(){intro.setVisibility(View.VISIBLE);examPanel.setVisibility(View.GONE);resultPanel.setVisibility(View.GONE);history.setText("最好成绩：A1 "+progress.bestExamScore("A1")+"% · A2 "+progress.bestExamScore("A2")+"% · B1 "+progress.bestExamScore("B1")+"%\n每级20题：词义、听力、拼写、冠词、句型。用于学习诊断，不是官方CEFR证书考试。");}
    private void start(String l){level=l;questions.clear();Arrays.fill(catTotal,0);Arrays.fill(catCorrect,0);correct=0;index=0;buildQuestions(l);intro.setVisibility(View.GONE);resultPanel.setVisibility(View.GONE);examPanel.setVisibility(View.VISIBLE);title.setText(l+" 阶段自测");showQuestion();}
    private int[] range(String l){int n=words.size();if("A1".equals(l))return new int[]{0,Math.min(750,n)};if("A2".equals(l))return new int[]{Math.min(550,n),Math.min(1700,n)};return new int[]{Math.min(1400,n),n};}
    private void buildQuestions(String l){int[] r=range(l);List<Word> pool=new ArrayList<>(words.all().subList(r[0],r[1]));Collections.shuffle(pool,new Random(LocalDate.now().toEpochDay()+l.hashCode()));int cursor=0;
        for(int i=0;i<4&&cursor<pool.size();i++)questions.add(makeVocab(pool.get(cursor++),MEANING,pool));
        for(int i=0;i<4&&cursor<pool.size();i++)questions.add(makeVocab(pool.get(cursor++),LISTENING,pool));
        for(int i=0;i<4&&cursor<pool.size();i++)questions.add(makeVocab(pool.get(cursor++),SPELLING,pool));
        List<SentencePattern> ps=patterns.all();int pStart="A1".equals(l)?0:("A2".equals(l)?Math.min(7,ps.size()):Math.min(14,ps.size()));int pEnd="A1".equals(l)?Math.min(7,ps.size()):("A2".equals(l)?Math.min(14,ps.size()):ps.size());
        for(int i=0;i<4;i++){SentencePattern p=ps.get(pStart+(i%Math.max(1,pEnd-pStart)));PatternExercise e=p.exercises.get(i%p.exercises.size());Q q=new Q();q.type=GRAMMAR;q.category="句型语法";q.prompt=e.prompt;q.answer=e.answer;q.patternId=p.id;q.exercise=e;questions.add(q);}
        List<Word> nouns=new ArrayList<>();for(Word w:pool)if(w.article!=null&&!w.article.isEmpty())nouns.add(w);for(int i=0;i<4&&i<nouns.size();i++){Word w=nouns.get(i);Q q=new Q();q.type=ARTICLE;q.category="冠词与名词";q.word=w;q.prompt="选择正确冠词：___ "+w.word;q.answer=w.article;q.choices=articleChoices(w.article);questions.add(q);}
        while(questions.size()<20&&cursor<pool.size())questions.add(makeVocab(pool.get(cursor++),MEANING,pool));Collections.shuffle(questions,new Random(LocalDate.now().toEpochDay()*31+l.hashCode()));}
    private Q makeVocab(Word w,int type,List<Word> pool){Q q=new Q();q.type=type;q.word=w;if(type==SPELLING){q.category="拼写";q.prompt="请写出意大利语："+w.chinese;q.answer=w.word;}else{q.category=type==LISTENING?"听力理解":"词义识别";q.prompt=type==LISTENING?"听发音，选择正确中文意思":w.word;q.answer=w.chinese;List<String> opts=new ArrayList<>();opts.add(w.chinese);int guard=0;while(opts.size()<4&&guard++<100){String z=pool.get(random.nextInt(pool.size())).chinese;if(z!=null&&!z.isEmpty()&&!opts.contains(z))opts.add(z);}Collections.shuffle(opts);q.choices=opts;}return q;}
    private List<String> articleChoices(String correct){List<String> all=new ArrayList<>(Arrays.asList("il","lo","la","l'","i","gli","le"));all.remove(correct);Collections.shuffle(all);List<String> out=new ArrayList<>();out.add(correct);for(int i=0;i<3&&i<all.size();i++)out.add(all.get(i));Collections.shuffle(out);return out;}
    private Q current(){return index>=0&&index<questions.size()?questions.get(index):null;}
    private void showQuestion(){Q q=current();if(q==null){finish();return;}progressText.setText("第 "+(index+1)+" / "+questions.size()+" 题");category.setText(q.category);question.setText(q.prompt);feedback.setText("");next.setVisibility(View.GONE);replay.setVisibility(q.type==LISTENING?View.VISIBLE:View.GONE);choicePanel.setVisibility(q.type==MEANING||q.type==LISTENING||q.type==ARTICLE?View.VISIBLE:View.GONE);typePanel.setVisibility(q.type==SPELLING||q.type==GRAMMAR?View.VISIBLE:View.GONE);input.setText("");input.setEnabled(true);submit.setEnabled(true);for(int i=0;i<choiceButtons.size();i++){MaterialButton b=choiceButtons.get(i);b.setEnabled(true);b.setText(i<q.choices.size()?q.choices.get(i):"");b.setVisibility(i<q.choices.size()?View.VISIBLE:View.GONE);}started=System.currentTimeMillis();if(q.type==LISTENING&&q.word!=null)audio.play(q.word);}
    private void answerChoice(String value){Q q=current();if(q==null||next.getVisibility()==View.VISIBLE)return;grade(norm(value).equals(norm(q.answer)));}
    private void answerTyped(){Q q=current();if(q==null||next.getVisibility()==View.VISIBLE)return;String value=input.getText()==null?"":input.getText().toString();grade(norm(value).equals(norm(q.answer)));}
    private void grade(boolean ok){Q q=current();long ms=Math.max(1,System.currentTimeMillis()-started);catTotal[q.type]++;if(ok){correct++;catCorrect[q.type]++;}feedback.setText(ok?"✓ 正确":"✗ 正确答案："+q.answer);feedback.setTextColor(ContextCompat.getColor(requireContext(),ok?R.color.success:R.color.error));for(MaterialButton b:choiceButtons)b.setEnabled(false);input.setEnabled(false);submit.setEnabled(false);next.setVisibility(View.VISIBLE);progress.recordAuxiliaryResult("level_exam",ok,ms);if(q.word!=null){int dim=q.type==LISTENING?ProgressStore.DIM_LISTENING:(q.type==SPELLING?ProgressStore.DIM_SPELLING:ProgressStore.DIM_MEANING);progress.recordDimensionResult(q.word.id,dim,ok,ms);}if(q.type==GRAMMAR&&q.patternId!=null)progress.recordGrammarResult(q.patternId,ok,ms);if(q.type==ARTICLE)progress.recordGrammarResult("article",ok,ms);}
    private void advance(){index++;if(index>=questions.size())finish();else showQuestion();}
    private void finish(){examPanel.setVisibility(View.GONE);resultPanel.setVisibility(View.VISIBLE);int score=questions.isEmpty()?0:(int)Math.round(correct*100.0/questions.size());progress.saveExamScore(level,score);String verdict=score>=85?"基础很稳，可以继续提高难度。":(score>=70?"达到本阶段学习检查线，继续补弱项。":"这个阶段还有明显漏洞，先补弱项再测一次。");result.setText(level+" 得分："+score+"%（"+correct+" / "+questions.size()+"）\n\n"+breakdown()+"\n\n"+verdict+"\n最好成绩："+progress.bestExamScore(level)+"%\n\n说明：这是基于当前2774词课程与App句型库的内部阶段自测，不等同于官方CEFR考试或证书。");}
    private String breakdown(){return "词义 "+pct(MEANING)+"% · 听力 "+pct(LISTENING)+"% · 拼写 "+pct(SPELLING)+"%\n语法 "+pct(GRAMMAR)+"% · 冠词 "+pct(ARTICLE)+"%";}
    private int pct(int t){return catTotal[t]==0?0:(int)Math.round(catCorrect[t]*100.0/catTotal[t]);}
    private String norm(String s){if(s==null)return"";String n=Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ITALIAN).replace('’','\'');return n.replaceAll("[^a-z0-9' ]"," ").replaceAll("\\s+"," ").trim();}
    @Override public void onDestroyView(){if(audio!=null)audio.release();super.onDestroyView();}
}
