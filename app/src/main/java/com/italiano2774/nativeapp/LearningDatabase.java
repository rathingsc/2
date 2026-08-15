package com.italiano2774.nativeapp;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {StudyEvent.class,CustomStudyItem.class}, version = 2, exportSchema = false)
public abstract class LearningDatabase extends RoomDatabase {
    public abstract StudyEventDao studyEventDao();
    public abstract CustomStudyItemDao customStudyItemDao();
    private static volatile LearningDatabase INSTANCE;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private static final Migration MIGRATION_1_2 = new Migration(1,2){
        @Override public void migrate(SupportSQLiteDatabase db){
            db.execSQL("CREATE TABLE IF NOT EXISTS `custom_study_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `kind` TEXT NOT NULL, `italian` TEXT NOT NULL, `chinese` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `dueEpochDay` INTEGER NOT NULL, `intervalDays` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, `correct` INTEGER NOT NULL, `stability` REAL NOT NULL, `difficulty` REAL NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_study_items_dueEpochDay` ON `custom_study_items` (`dueEpochDay`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_study_items_kind_italian` ON `custom_study_items` (`kind`, `italian`)");
        }
    };

    public static LearningDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (LearningDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), LearningDatabase.class,
                            "italiano2774_learning.db").addMigrations(MIGRATION_1_2).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }

    public static void clear(Context context) {
        final Context app = context.getApplicationContext();
        IO.execute(() -> { try { get(app).studyEventDao().clear(); } catch (Exception ignored) {} });
    }

    public static void log(Context context, String type, String itemId, int dimension,
                           boolean correct, long responseMs, String detail) {
        final Context app = context.getApplicationContext();
        IO.execute(() -> {
            try {
                StudyEvent e = new StudyEvent();
                e.createdAt = System.currentTimeMillis();
                e.itemType = type == null ? "" : type;
                e.itemId = itemId == null ? "" : itemId;
                e.dimension = dimension;
                e.correct = correct;
                e.responseMs = responseMs;
                e.detail = detail == null ? "" : detail;
                get(app).studyEventDao().insert(e);
            } catch (Exception ignored) {}
        });
    }
}
