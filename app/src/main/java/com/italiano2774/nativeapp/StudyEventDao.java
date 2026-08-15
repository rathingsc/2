package com.italiano2774.nativeapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface StudyEventDao {
    @Insert long insert(StudyEvent event);
    @Query("SELECT COUNT(*) FROM study_events") long count();
    @Query("SELECT * FROM study_events ORDER BY createdAt DESC LIMIT :limit") List<StudyEvent> recent(int limit);
    @Query("DELETE FROM study_events") void clear();
}
