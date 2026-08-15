package com.italiano2774.nativeapp;
import androidx.annotation.NonNull;import androidx.room.Entity;import androidx.room.Index;import androidx.room.PrimaryKey;
@Entity(tableName="custom_study_items",indices={@Index(value={"dueEpochDay"}),@Index(value={"kind","italian"})})
public class CustomStudyItem{
 @PrimaryKey(autoGenerate=true) public long id;
 @NonNull public String kind="word";
 @NonNull public String italian="";
 @NonNull public String chinese="";
 @NonNull public String note="";
 public long createdAt;
 public long dueEpochDay;
 public int intervalDays;
 public int attempts;
 public int correct;
 public double stability=1.0;
 public double difficulty=5.0;
}
