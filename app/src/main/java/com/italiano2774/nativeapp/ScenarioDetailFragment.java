package com.italiano2774.nativeapp;

import android.os.Bundle;import android.view.*;import android.widget.TextView;import androidx.annotation.*;import androidx.fragment.app.Fragment;import androidx.recyclerview.widget.*;
public class ScenarioDetailFragment extends Fragment{
    private AudioPlayer audio;
    public static ScenarioDetailFragment newInstance(String id){ScenarioDetailFragment f=new ScenarioDetailFragment();Bundle b=new Bundle();b.putString("id",id);f.setArguments(b);return f;}
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle state){View v=i.inflate(R.layout.fragment_scenario_detail,c,false);String id=getArguments()==null?"":getArguments().getString("id","");Scenario s=ScenarioRepository.get(requireContext()).find(id);audio=new AudioPlayer(requireContext(),new ProgressStore(requireContext()));v.findViewById(R.id.button_scenario_detail_back).setOnClickListener(x->((MainActivity)requireActivity()).openScenarios());v.findViewById(R.id.button_scenario_dialogue).setOnClickListener(x->((MainActivity)requireActivity()).openDialogueTraining(id));if(s!=null){((TextView)v.findViewById(R.id.text_scenario_detail_title)).setText(s.emoji+"  "+s.title);((TextView)v.findViewById(R.id.text_scenario_detail_subtitle)).setText(s.subtitle+" · 点击句子或喇叭听发音");RecyclerView rv=v.findViewById(R.id.recycler_scenario_phrases);rv.setLayoutManager(new LinearLayoutManager(requireContext()));rv.setAdapter(new ScenarioPhraseAdapter(s.phrases,audio));}return v;}
    @Override public void onDestroyView(){if(audio!=null)audio.release();super.onDestroyView();}
}
