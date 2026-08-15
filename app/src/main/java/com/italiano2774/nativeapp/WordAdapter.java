package com.italiano2774.nativeapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordAdapter extends RecyclerView.Adapter<WordAdapter.Holder> {
    public interface ChangeListener{void onChanged();}
    private final Context context;private final ProgressStore progress;private final AudioPlayer audio;private final ChangeListener listener;
    private final List<Word> items=new ArrayList<>();
    private final Set<Integer> expanded=new HashSet<>();
    private final List<String> masteryLabels=Arrays.asList("0 陌生","1 见过","2 能认出","3 能听懂","4 能主动说","5 已掌握");

    public WordAdapter(Context c,ProgressStore p,AudioPlayer a,ChangeListener l){context=c;progress=p;audio=a;listener=l;}
    public void submit(List<Word> list){items.clear();items.addAll(list);notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){return new Holder(LayoutInflater.from(context).inflate(R.layout.item_word,parent,false));}

    @Override public void onBindViewHolder(@NonNull Holder h,int pos){
        Word w=items.get(pos);
        h.word.setText(w.word);h.ipa.setText(w.ipa);h.pron.setText(w.zhPron);
        h.chinese.setText(w.chinese==null||w.chinese.isEmpty()?"中文释义待补充":w.chinese);
        String badge="";if("verb".equals(w.partOfSpeech)){String l=w.lemma==null||w.lemma.trim().isEmpty()?w.word:w.lemma;badge="动词 · "+l;}else if("noun".equals(w.partOfSpeech)&&w.gender!=null&&!w.gender.isEmpty()){badge="名词 · "+("f".equals(w.gender)?"阴性":"阳性")+(w.article==null||w.article.isEmpty()?"":" · "+w.article);}
        h.grammarBadge.setVisibility(badge.isEmpty()?View.GONE:View.VISIBLE);if(!badge.isEmpty())h.grammarBadge.setText(badge);
        h.english.setText("英文："+w.english);
        h.topic.setText(w.level+" · #"+w.num);

        boolean hasLemma=w.lemma!=null&&!w.lemma.trim().isEmpty();boolean hasFormInfo=w.formInfo!=null&&!w.formInfo.trim().isEmpty();boolean showLemma=hasLemma||hasFormInfo;
        h.lemma.setVisibility(showLemma?View.VISIBLE:View.GONE);
        if(showLemma){String head=hasLemma?(w.lemma.equalsIgnoreCase(w.word)?("原形："+w.word):("词形："+w.word+" → "+w.lemma)):"词形提示";h.lemma.setText(head+(hasFormInfo?"\n"+w.formInfo:""));}

        java.time.LocalDate due=progress.nextDueDate(w.id);String dueText=due==null?"尚未安排复习":("下次复习："+due+" · 间隔 "+progress.intervalDays(w.id)+"天");if(progress.memoryStability(w.id)>0)dueText+=" · 记忆保持约 "+progress.memoryRetrievability(w.id)+"% · 稳定度 "+String.format(java.util.Locale.US,"%.1f",progress.memoryStability(w.id))+"天";if(progress.wrongCount(w.id)>0)dueText+=" · 错词 "+progress.wrongCount(w.id)+"次";h.reviewInfo.setText(dueText);
        h.dimensions.setText("四维掌握  ·  识义 "+progress.meaningLevel(w.id)+"  ·  听力 "+progress.listeningLevel(w.id)+"  ·  拼写 "+progress.spellingLevel(w.id)+"  ·  口语 "+progress.speakingLevel(w.id)+"\n当前弱项："+progress.weakestDimensionName(w.id));

        String grammar=ItalianGrammar.grammarPanel(w);h.grammar.setVisibility(grammar.isEmpty()?View.GONE:View.VISIBLE);if(!grammar.isEmpty())h.grammar.setText(grammar);

        boolean hasExample=w.example!=null&&!w.example.trim().isEmpty();
        h.examplePanel.setVisibility(hasExample?View.VISIBLE:View.GONE);
        if(hasExample){h.example.setText(w.example);h.exampleZh.setText(w.exampleZh);h.exampleAudio.setOnClickListener(v->audio.speak(w.example));}

        h.audio.setOnClickListener(v->audio.play(w));
        boolean fav=progress.favorite(w.id);h.favorite.setImageResource(fav?R.drawable.ic_star_filled:R.drawable.ic_star_outline);
        h.favorite.setOnClickListener(v->{progress.setFavorite(w.id,!progress.favorite(w.id));int ap=h.getBindingAdapterPosition();if(ap>=0)notifyItemChanged(ap);if(listener!=null)listener.onChanged();});

        int current=progress.mastery(w.id);applyMasteryStyle(h,current);
        ArrayAdapter<String> a=new ArrayAdapter<>(context,android.R.layout.simple_spinner_item,masteryLabels);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);h.mastery.setAdapter(a);
        h.mastery.setTag(w.id);h.mastery.setSelection(current,false);
        h.mastery.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> p){}
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){
                Object tag=h.mastery.getTag();
                if(tag instanceof Integer&&(Integer)tag==w.id&&progress.mastery(w.id)!=position){
                    progress.setMastery(w.id,position);applyMasteryStyle(h,position);if(listener!=null)listener.onChanged();
                }
            }
        });

        boolean isExpanded=expanded.contains(w.id);
        h.pron.setVisibility(progress.shouldShowPronunciation(w.id,isExpanded)?View.VISIBLE:View.GONE);
        h.detailPanel.setVisibility(isExpanded?View.VISIBLE:View.GONE);
        h.expandHint.setText(isExpanded?"收起详情  ▴":"语法、四维进度与例句  ▾");
        h.card.setOnClickListener(v->{
            if(expanded.contains(w.id))expanded.remove(w.id);else expanded.add(w.id);
            int ap=h.getBindingAdapterPosition();if(ap>=0)notifyItemChanged(ap);
        });
    }

    private void applyMasteryStyle(Holder h,int level){
        int[] colors={R.color.level0,R.color.level1,R.color.level2,R.color.level3,R.color.level4,R.color.level5};
        h.card.setStrokeColor(ContextCompat.getColor(context,colors[Math.max(0,Math.min(5,level))]));
        h.card.setCardBackgroundColor(ContextCompat.getColor(context,level>=4?R.color.mastered_bg:R.color.surface));
    }

    @Override public int getItemCount(){return items.size();}

    static class Holder extends RecyclerView.ViewHolder{
        MaterialCardView card;TextView word,ipa,pron,grammarBadge,chinese,english,topic,lemma,grammar,dimensions,example,exampleZh,reviewInfo,expandHint;
        ImageButton audio,favorite,exampleAudio;Spinner mastery;View examplePanel,detailPanel;
        Holder(View v){
            super(v);card=v.findViewById(R.id.card_word);word=v.findViewById(R.id.text_word);ipa=v.findViewById(R.id.text_ipa);
            pron=v.findViewById(R.id.text_pron);grammarBadge=v.findViewById(R.id.text_grammar_badge);chinese=v.findViewById(R.id.text_chinese);english=v.findViewById(R.id.text_english);
            topic=v.findViewById(R.id.text_topic);lemma=v.findViewById(R.id.text_lemma);grammar=v.findViewById(R.id.text_grammar);dimensions=v.findViewById(R.id.text_dimensions);example=v.findViewById(R.id.text_example);exampleZh=v.findViewById(R.id.text_example_zh);
            examplePanel=v.findViewById(R.id.example_panel);detailPanel=v.findViewById(R.id.detail_panel);expandHint=v.findViewById(R.id.text_expand_hint);
            audio=v.findViewById(R.id.button_audio);favorite=v.findViewById(R.id.button_favorite);exampleAudio=v.findViewById(R.id.button_example_audio);reviewInfo=v.findViewById(R.id.text_review_info);mastery=v.findViewById(R.id.spinner_mastery);
        }
    }
}
