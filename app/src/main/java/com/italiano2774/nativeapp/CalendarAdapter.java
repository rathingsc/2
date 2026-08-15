package com.italiano2774.nativeapp;

import android.view.*;import android.widget.*;import androidx.annotation.NonNull;import androidx.recyclerview.widget.RecyclerView;import java.time.LocalDate;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.Holder>{
    public interface Click{void open(LocalDate d);}private final WordRepository repo;private final ProgressStore progress;private final Click click;private final LocalDate start;private final int perDay,total;
    public CalendarAdapter(WordRepository r,ProgressStore p,Click c){repo=r;progress=p;click=c;start=p.startDate();perDay=p.perDay();total=r.totalDays(start,perDay);}
    @NonNull public Holder onCreateViewHolder(@NonNull ViewGroup p,int t){return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_day,p,false));}
    public void onBindViewHolder(@NonNull Holder h,int pos){LocalDate d=start.plusDays(pos);java.util.List<Word> words=repo.forDate(start,d,perDay);int done=progress.countAtLeast(words,3);int pct=words.isEmpty()?0:(int)Math.round(done*100.0/words.size());h.day.setText("第 "+(pos+1)+" 天");h.date.setText(d.toString());h.range.setText(words.isEmpty()?"":words.get(0).num+"–"+words.get(words.size()-1).num+" · "+words.get(0).level);h.bar.setProgress(pct);h.progress.setText(done+" / "+words.size()+" 达到3级+");h.itemView.setOnClickListener(v->click.open(d));}
    public int getItemCount(){return total;}
    static class Holder extends RecyclerView.ViewHolder{TextView day,date,range,progress;ProgressBar bar;Holder(View v){super(v);day=v.findViewById(R.id.text_day_number);date=v.findViewById(R.id.text_day_date);range=v.findViewById(R.id.text_day_range);progress=v.findViewById(R.id.text_day_progress);bar=v.findViewById(R.id.progress_day);}}
}
