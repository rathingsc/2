package com.italiano2774.nativeapp;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "study_events", indices = {
        @Index(value = {"createdAt"}),
        @Index(value = {"itemType", "itemId"})
})
public class StudyEvent {
    @PrimaryKey(autoGenerate = true) public long id;
    public long createdAt;
    @NonNull public String itemType = "";
    @NonNull public String itemId = "";
    public int dimension = -1;
    public boolean correct;
    public long responseMs;
    @NonNull public String detail = "";
}
