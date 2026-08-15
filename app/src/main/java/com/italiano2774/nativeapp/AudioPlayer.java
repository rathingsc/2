package com.italiano2774.nativeapp;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class AudioPlayer {
    private final Context context;private final ProgressStore progress;private MediaPlayer player;private TextToSpeech tts;
    public AudioPlayer(Context c,ProgressStore p){
        context=c.getApplicationContext();progress=p;
        tts=new TextToSpeech(context,status->{if(status==TextToSpeech.SUCCESS){tts.setLanguage(Locale.ITALIAN);tts.setSpeechRate(0.92f);}});
    }
    public void play(Word w){stop();if(progress.preferOriginalAudio()&&w.duoAudio!=null&&!w.duoAudio.isEmpty())playRemote(w);else playLocal(w);}
    public void speak(String text){stop();if(tts!=null&&text!=null&&!text.trim().isEmpty())tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"example");}
    private void playRemote(Word w){
        try{player=new MediaPlayer();player.setDataSource(w.duoAudio);player.setOnPreparedListener(MediaPlayer::start);player.setOnErrorListener((mp,a,b)->{releasePlayer();playLocal(w);return true;});player.prepareAsync();}
        catch(Exception e){playLocal(w);}
    }
    private void playLocal(Word w){
        try{AssetFileDescriptor afd=context.getAssets().openFd("audio/"+w.localAudio);player=new MediaPlayer();player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());afd.close();player.setOnPreparedListener(MediaPlayer::start);player.prepareAsync();}
        catch(Exception e){if(tts!=null)tts.speak(w.word,TextToSpeech.QUEUE_FLUSH,null,"word");}
    }
    private void releasePlayer(){if(player!=null){try{player.release();}catch(Exception ignored){}player=null;}}
    public void stop(){if(player!=null){try{player.stop();}catch(Exception ignored){}releasePlayer();}if(tts!=null)tts.stop();}
    public void release(){stop();if(tts!=null){tts.shutdown();tts=null;}}
}
