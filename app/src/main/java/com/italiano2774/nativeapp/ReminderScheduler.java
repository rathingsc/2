package com.italiano2774.nativeapp;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public class ReminderScheduler {
    public static final String CHANNEL_ID="italiano_review";
    private static final int REQUEST_CODE=7182;

    public static void createChannel(Context c){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel channel=new NotificationChannel(CHANNEL_ID,"每日复习提醒",NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("提醒你完成当天的意大利语复习");
            NotificationManager nm=c.getSystemService(NotificationManager.class);if(nm!=null)nm.createNotificationChannel(channel);
        }
    }

    public static void schedule(Context c,int hour,int minute){
        createChannel(c);
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return;
        PendingIntent pi=pending(c);
        Calendar cal=Calendar.getInstance();cal.set(Calendar.HOUR_OF_DAY,hour);cal.set(Calendar.MINUTE,minute);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);
        if(cal.getTimeInMillis()<=System.currentTimeMillis())cal.add(Calendar.DAY_OF_YEAR,1);
        am.cancel(pi);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),AlarmManager.INTERVAL_DAY,pi);
    }

    public static void cancel(Context c){
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am!=null)am.cancel(pending(c));
    }

    private static PendingIntent pending(Context c){
        Intent i=new Intent(c,ReminderReceiver.class);i.setAction("com.italiano2774.REVIEW_REMINDER");
        return PendingIntent.getBroadcast(c,REQUEST_CODE,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
}
