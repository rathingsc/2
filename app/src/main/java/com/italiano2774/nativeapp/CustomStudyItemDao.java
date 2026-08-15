package com.italiano2774.nativeapp;
import androidx.room.*;import java.util.List;
@Dao public interface CustomStudyItemDao{
 @Insert long insert(CustomStudyItem item);
 @Update void update(CustomStudyItem item);
 @Delete void delete(CustomStudyItem item);
 @Query("SELECT * FROM custom_study_items ORDER BY createdAt DESC") List<CustomStudyItem> all();
 @Query("SELECT * FROM custom_study_items WHERE dueEpochDay<=:day ORDER BY dueEpochDay ASC, attempts ASC") List<CustomStudyItem> due(long day);
 @Query("SELECT COUNT(*) FROM custom_study_items") int count();
 @Query("SELECT COUNT(*) FROM custom_study_items WHERE dueEpochDay<=:day") int dueCount(long day);
 @Query("SELECT * FROM custom_study_items WHERE kind=:kind AND lower(italian)=lower(:italian) LIMIT 1") CustomStudyItem find(String kind,String italian);
 @Query("DELETE FROM custom_study_items") void clear();
}
