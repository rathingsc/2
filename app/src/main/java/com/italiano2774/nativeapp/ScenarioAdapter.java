package com.italiano2774.nativeapp;

import android.view.*;import android.widget.TextView;import androidx.annotation.NonNull;import androidx.recyclerview.widget.RecyclerView;import java.util.*;
public class ScenarioAdapter extends RecyclerView.Adapter<ScenarioAdapter.Holder>{
    public interface Listener{void onOpen(Scenario s);}private final List<Scenario> items;private final Listener listener;
    public ScenarioAdapter(List<Scenario> items,Listener listener){this.items=items;this.listener=listener;}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup p,int v){return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_scenario,p,false));}
    @Override public void onBindViewHolder(@NonNull Holder h,int pos){Scenario s=items.get(pos);h.emoji.setText(s.emoji);h.title.setText(s.title);h.subtitle.setText(s.subtitle);h.count.setText(s.phrases.size()+" 个核心句");h.itemView.setOnClickListener(v->listener.onOpen(s));}
    @Override public int getItemCount(){return items.size();}
    static class Holder extends RecyclerView.ViewHolder{TextView emoji,title,subtitle,count;Holder(View v){super(v);emoji=v.findViewById(R.id.text_scenario_emoji);title=v.findViewById(R.id.text_scenario_title);subtitle=v.findViewById(R.id.text_scenario_subtitle);count=v.findViewById(R.id.text_scenario_count);}}
}
