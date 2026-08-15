package com.italiano2774.nativeapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class MainActivity extends AppCompatActivity {
    private static final int EXPORT=401,IMPORT=402,NOTIFY=403;
    private ProgressStore progress;private WordRepository repo;private String pending;

    @Override protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_main);
        progress=new ProgressStore(this);repo=WordRepository.get(this);ReminderScheduler.createChannel(this);

        View root=findViewById(R.id.root_main);
        BottomNavigationView nav=findViewById(R.id.bottom_navigation);
        final int navLeft=nav.getPaddingLeft(),navTop=nav.getPaddingTop(),navRight=nav.getPaddingRight(),navBottom=nav.getPaddingBottom();
        final int baseNavHeight=(int)(64*getResources().getDisplayMetrics().density+0.5f);
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // 页面只吃顶部安全区；底部手势/导航条空间交给 BottomNavigationView 自己。
            view.setPadding(bars.left,bars.top,bars.right,0);
            ViewGroup.LayoutParams lp=nav.getLayoutParams();
            lp.height=baseNavHeight+bars.bottom;
            nav.setLayoutParams(lp);
            nav.setPadding(navLeft,navTop,navRight,navBottom+bars.bottom);
            return insets;
        });

        nav.setOnItemSelectedListener(item->{
            int id=item.getItemId();
            if(id==R.id.nav_today){show(TodayFragment.newInstance(LocalDate.now()));return true;}
            if(id==R.id.nav_calendar){show(new CalendarFragment());return true;}
            if(id==R.id.nav_practice){show(new PracticeFragment());return true;}
            if(id==R.id.nav_vocabulary){show(new VocabularyFragment());return true;}
            if(id==R.id.nav_settings){show(new SettingsFragment());return true;}
            return false;
        });
        if(s==null)nav.setSelectedItemId(R.id.nav_today);
    }

    private void show(Fragment f){getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,f).commit();}
    public void openToday(LocalDate d){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_today).setChecked(true);show(TodayFragment.newInstance(d));}
    public void openStudy(LocalDate d){show(StudySessionFragment.newInstance(d));}
    public void openAdaptiveStudy(){show(StudySessionFragment.newAdaptiveInstance());}
    public void openAdaptiveStudy(int maxCards){show(StudySessionFragment.newAdaptiveInstance(maxCards));}
    public void openPlacementTest(){show(new PlacementTestFragment());}
    public void openPractice(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new PracticeFragment());}
    public void openScenarios(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new ScenarioFragment());}
    public void openScenario(String id){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(ScenarioDetailFragment.newInstance(id));}
    public void openSentencePatterns(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new SentencePatternFragment());}
    public void openSentencePatterns(String patternId){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(SentencePatternFragment.newInstance(patternId));}
    public void openGrammarDiagnosis(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new GrammarDiagnosisFragment());}
    public void openFreeConversation(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new FreeConversationFragment());}
    public void openFreeConversation(String scenarioId){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(FreeConversationFragment.newInstance(scenarioId));}
    public void openPersonalCourse(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_today).setChecked(true);show(new PersonalCourseFragment());}
    public void openDialogueTraining(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new DialogueTrainingFragment());}
    public void openDialogueTraining(String id){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(DialogueTrainingFragment.newInstance(id));}
    public void openCommute(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new CommuteFragment());}
    public void openShadowing(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new ShadowingFragment());}
    public void openPronunciation(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new PronunciationFragment());}
    public void openLevelExam(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new LevelExamFragment());}
    public void openReadingList(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new ReadingFragment());}
    public void openReading(String id){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(ReadingDetailFragment.newInstance(id));}
    public void openWeaknessCenter(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new WeaknessCenterFragment());}
    public void openEmergency(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new EmergencyFragment());}
    public void openCustomLibrary(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_vocabulary).setChecked(true);show(new CustomLibraryFragment());}
    public void openPracticeMode(String mode){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(PracticeFragment.newInstance(mode));}

    public void requestNotificationPermissionIfNeeded(){
        if(Build.VERSION.SDK_INT>=33&&ActivityCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFY);
    }

    public void exportProgress(){
        Toast.makeText(this,"正在准备备份…",Toast.LENGTH_SHORT).show();
        new Thread(()->{
            try{
                JSONObject root=progress.exportJson(repo.all());JSONArray custom=new JSONArray();
                for(CustomStudyItem x:LearningDatabase.get(this).customStudyItemDao().all()){JSONObject o=new JSONObject();o.put("id",x.id);o.put("kind",x.kind);o.put("italian",x.italian);o.put("chinese",x.chinese);o.put("note",x.note);o.put("createdAt",x.createdAt);o.put("dueEpochDay",x.dueEpochDay);o.put("intervalDays",x.intervalDays);o.put("attempts",x.attempts);o.put("correct",x.correct);o.put("stability",x.stability);o.put("difficulty",x.difficulty);custom.put(o);}
                root.put("customItems",custom);String json=root.toString(2);
                runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"终学意语_backup_v2.json");pending=json;startActivityForResult(i,EXPORT);});
            }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"导出失败："+e.getMessage(),Toast.LENGTH_LONG).show());}
        }).start();
    }

    public void importProgress(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");startActivityForResult(i,IMPORT);
    }

    @Override protected void onActivityResult(int req,int result,@Nullable Intent data){
        super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
        try{
            if(req==EXPORT&&pending!=null){
                try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(pending.getBytes(StandardCharsets.UTF_8));}
                pending=null;Toast.makeText(this,"进度已导出",Toast.LENGTH_SHORT).show();
            }else if(req==IMPORT){
                StringBuilder sb=new StringBuilder();
                try(BufferedReader br=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri),StandardCharsets.UTF_8))){
                    String line;while((line=br.readLine())!=null)sb.append(line);
                }
                JSONObject root=new JSONObject(sb.toString());progress.importJson(root);
                JSONArray custom=root.optJSONArray("customItems");
                new Thread(()->{try{CustomStudyItemDao dao=LearningDatabase.get(this).customStudyItemDao();if(custom!=null){dao.clear();for(int i=0;i<custom.length();i++){JSONObject o=custom.getJSONObject(i);CustomStudyItem x=new CustomStudyItem();x.kind=o.optString("kind","word");x.italian=o.optString("italian");x.chinese=o.optString("chinese");x.note=o.optString("note");x.createdAt=o.optLong("createdAt",System.currentTimeMillis());x.dueEpochDay=o.optLong("dueEpochDay",LocalDate.now().toEpochDay());x.intervalDays=o.optInt("intervalDays",0);x.attempts=o.optInt("attempts",0);x.correct=o.optInt("correct",0);x.stability=o.optDouble("stability",1.0);x.difficulty=o.optDouble("difficulty",5.0);dao.insert(x);}}}catch(Exception ignored){}}).start();
                if(progress.reminderEnabled())ReminderScheduler.schedule(this,progress.reminderHour(),progress.reminderMinute());
                Toast.makeText(this,"进度和个人词句库已导入",Toast.LENGTH_SHORT).show();show(new SettingsFragment());
            }
        }catch(Exception e){Toast.makeText(this,"文件处理失败："+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
}
