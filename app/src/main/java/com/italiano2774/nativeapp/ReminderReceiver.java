package com.italiano2774.nativeapp;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import java.time.LocalDate;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        ProgressStore progress=new ProgressStore(context);
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            if(progress.reminderEnabled())ReminderScheduler.schedule(context,progress.reminderHour(),progress.reminderMinute());
            return;
        }
        if(!progress.reminderEnabled())return;
        WordRepository repo=WordRepository.get(context);
        int due=progress.dueCount(repo.all(),LocalDate.now()),wrong=progress.wrongTotal(repo.all());
        Intent open=new Intent(context,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String text=due>0?("今天有 "+due+" 个词待复习"):("今天继续学习 "+progress.perDay()+" 个新词");
        if(wrong>0)text+="，错词本还有 "+wrong+" 个词";
        NotificationCompat.Builder b=new NotificationCompat.Builder(context,ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher).setContentTitle("终学意语 学习提醒").setContentText(text)
                .setAutoCancel(true).setContentIntent(pi).setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if(Build.VERSION.SDK_INT<33||ActivityCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED){
            NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);if(nm!=null)nm.notify(2774,b.build());
        }
    }
}
