package com.italiano2774.nativeapp;

import android.os.Bundle;import android.view.*;import androidx.annotation.*;import androidx.fragment.app.Fragment;import androidx.recyclerview.widget.*;
public class ScenarioFragment extends Fragment{
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){View v=i.inflate(R.layout.fragment_scenarios,c,false);RecyclerView rv=v.findViewById(R.id.recycler_scenarios);rv.setLayoutManager(new LinearLayoutManager(requireContext()));ScenarioRepository repo=ScenarioRepository.get(requireContext());rv.setAdapter(new ScenarioAdapter(repo.all(),x->((MainActivity)requireActivity()).openScenario(x.id)));v.findViewById(R.id.button_scenario_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());return v;}
}
