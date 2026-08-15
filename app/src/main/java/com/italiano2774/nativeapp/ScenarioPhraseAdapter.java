package com.italiano2774.nativeapp;

import android.view.*;import android.widget.*;import androidx.annotation.NonNull;import androidx.recyclerview.widget.RecyclerView;import java.util.*;
public class ScenarioPhraseAdapter extends RecyclerView.Adapter<ScenarioPhraseAdapter.Holder>{
    private final List<ScenarioPhrase> items;private final AudioPlayer audio;
    public ScenarioPhraseAdapter(List<ScenarioPhrase> items,AudioPlayer audio){this.items=items;this.audio=audio;}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup p,int v){return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_scenario_phrase,p,false));}
    @Override public void onBindViewHolder(@NonNull Holder h,int pos){ScenarioPhrase x=items.get(pos);h.it.setText(x.it);h.zh.setText(x.zh);h.note.setText(x.note==null||x.note.trim().isEmpty()?"常用生活句":x.note);h.play.setOnClickListener(v->audio.speak(x.it));h.itemView.setOnClickListener(v->audio.speak(x.it));}
    @Override public int getItemCount(){return items.size();}
    static class Holder extends RecyclerView.ViewHolder{TextView it,zh,note;ImageButton play;Holder(View v){super(v);it=v.findViewById(R.id.text_phrase_it);zh=v.findViewById(R.id.text_phrase_zh);note=v.findViewById(R.id.text_phrase_note);play=v.findViewById(R.id.button_phrase_audio);}}
}
