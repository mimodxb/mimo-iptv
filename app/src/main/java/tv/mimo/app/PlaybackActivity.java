package tv.mimo.app;

import android.app.Activity;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import java.util.*;
import java.util.concurrent.*;
import static tv.mimo.app.TvStyle.*;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class PlaybackActivity extends Activity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Repository repository;
    private ExoPlayer player;
    private PlayerView video;
    private FrameLayout root;
    private LinearLayout top,controls;
    private TextView status,play,favorite;
    private Channel channel;
    private RecoveryPlan recovery;
    private final List<Channel.Stream> candidates=new ArrayList<>();
    private boolean active,recovering,chrome=true;
    private int generation;
    private String key,name;
    private Runnable bufferTimeout;
    private final Runnable hide=()->setChrome(false);

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);immersive(this);repository=new Repository(this);
        key=getIntent().getStringExtra("key");name=getIntent().getStringExtra("name");
        if(key==null){finish();return;}
        root=new FrameLayout(this);root.setBackgroundColor(android.graphics.Color.BLACK);root.setFocusable(true);
        video=new PlayerView(this);video.setUseController(false);video.setKeepScreenOn(true);root.addView(video,new FrameLayout.LayoutParams(-1,-1));
        top=column(this);top.setPadding(dp(this,22),dp(this,12),dp(this,22),dp(this,12));top.setBackground(shape(0xe6091516,dp(this,12),0));
        TextView title=text(this,name==null?"MIMO TV":name,24,INK);title.setTypeface(null,android.graphics.Typeface.BOLD);top.addView(title);
        status=text(this,"Opening channel…",13,MUTED);top.addView(status);
        FrameLayout.LayoutParams header=new FrameLayout.LayoutParams(-1,dp(this,87),Gravity.TOP);header.setMargins(dp(this,36),dp(this,28),dp(this,36),0);root.addView(top,header);
        controls=row(this);controls.setPadding(dp(this,12),dp(this,12),dp(this,12),dp(this,12));controls.setBackground(shape(0xee091516,dp(this,12),0));
        addControl("‹  Back",this::finish);play=addControl("Pause",this::togglePlayback);
        favorite=addControl(repository.favorites().contains(key)?"♥  Saved":"♡  Favorite",()->{repository.toggleFavorite(key);favorite.setText(repository.favorites().contains(key)?"♥  Saved":"♡  Favorite");scheduleHide();});
        addControl("↻  Retry",this::begin);
        FrameLayout.LayoutParams footer=new FrameLayout.LayoutParams(-1,dp(this,72),Gravity.BOTTOM);footer.setMargins(dp(this,36),0,dp(this,36),dp(this,28));root.addView(controls,footer);
        setContentView(root);play.requestFocus();
    }
    private TextView addControl(String label,Runnable action){TextView b=button(this,label,action);b.setGravity(Gravity.CENTER);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMarginEnd(dp(this,8));controls.addView(b,p);return b;}
    @Override protected void onStart(){super.onStart();if(key!=null){active=true;begin();}}
    @Override protected void onStop(){active=false;generation++;cancelTimers();release();super.onStop();}
    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
    private void release(){if(player!=null){video.setPlayer(null);player.release();player=null;}}
    private void cancelTimers(){handler.removeCallbacks(hide);if(bufferTimeout!=null)handler.removeCallbacks(bufferTimeout);}
    private void begin(){
        if(!active)return;int token=++generation;cancelTimers();release();recovery=new RecoveryPlan();candidates.clear();recovering=true;
        setChrome(true);status.setText("Opening channel…");
        io.execute(()->{
            Channel found=null;for(Channel c:repository.load(false).channels)if(c.key.equals(key)){found=c;break;}
            Channel result=found;runOnUiThread(()->{if(!active||generation!=token)return;channel=result;recovering=false;
                if(channel==null){status.setText("This channel is no longer in an enabled playlist.");return;}
                candidates.addAll(channel.streams);playNext();
            });
        });
    }
    private void playNext(){
        if(!active)return;
        Channel.Stream stream=recovery.next(candidates);
        if(stream==null){recover();return;}
        openStream(stream);
    }
    private void openStream(Channel.Stream stream){
        release();recovering=false;status.setText("Connecting…  ·  Source "+recovery.attempts());setChrome(true);play.setText("Pause");
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setUserAgent("MIMO-TV/0.1 AndroidTV")
            .setConnectTimeoutMs(10000).setReadTimeoutMs(12000).setAllowCrossProtocolRedirects(true).setDefaultRequestProperties(stream.headers);
        player=new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(this).setDataSourceFactory(http)).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
        player.setHandleAudioBecomingNoisy(true);video.setPlayer(player);int token=generation;
        player.addListener(new Player.Listener(){
            @Override public void onPlaybackStateChanged(int state){
                if(!active||generation!=token||recovering)return;
                if(bufferTimeout!=null)handler.removeCallbacks(bufferTimeout);
                if(state==Player.STATE_READY){status.setText("Live TV  ·  "+name);scheduleHide();}
                else if(state==Player.STATE_BUFFERING){status.setText("Buffering…");armTimeout(token);}
                else if(state==Player.STATE_ENDED){status.setText("The live stream ended.");recover();}
            }
            @Override public void onIsPlayingChanged(boolean playing){if(active){video.setKeepScreenOn(playing);play.setText(playing?"Pause":"Play");}}
            @Override public void onPlayerError(PlaybackException error){if(active&&generation==token)recover();}
        });
        player.setMediaItem(MediaItem.fromUri(stream.url));player.prepare();player.play();armTimeout(token);
    }
    private void armTimeout(int token){
        if(bufferTimeout!=null)handler.removeCallbacks(bufferTimeout);
        bufferTimeout=()->{if(active&&generation==token&&player!=null&&player.getPlaybackState()==Player.STATE_BUFFERING&&player.getPlayWhenReady())recover();};
        handler.postDelayed(bufferTimeout,25000);
    }
    private void recover(){
        if(!active||recovering)return;recovering=true;cancelTimers();release();setChrome(true);
        if(channel!=null&&channel.priority()&&recovery.claimRefresh()){
            status.setText("Refreshing Mimo’s backend for another source…");int token=generation;
            io.execute(()->{List<Channel.Stream> refreshed;try{refreshed=repository.refreshPriority(key);}catch(Exception e){refreshed=Collections.emptyList();}
                List<Channel.Stream> result=refreshed;runOnUiThread(()->{if(!active||generation!=token)return;recovering=false;candidates.addAll(0,result);playNext();});
            });return;
        }
        Channel.Stream next=recovery.next(candidates);
        if(next!=null){
            openStream(next);return;
        }
        recovering=false;status.setText("This channel is unavailable. Automatic recovery finished. Choose Retry or Back.");play.setText("Play");
    }
    private void togglePlayback(){if(player==null){begin();return;}if(player.isPlaying())player.pause();else player.play();play.setText(player.getPlayWhenReady()?"Pause":"Play");scheduleHide();}
    private void scheduleHide(){handler.removeCallbacks(hide);if(player!=null&&player.isPlaying())handler.postDelayed(hide,6000);}
    private void setChrome(boolean visible){chrome=visible;top.setVisibility(visible?View.VISIBLE:View.GONE);controls.setVisibility(visible?View.VISIBLE:View.GONE);if(!visible)root.requestFocus();}
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        int k=event.getKeyCode();boolean remote=k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_DPAD_UP||k==KeyEvent.KEYCODE_DPAD_DOWN||k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT||k==KeyEvent.KEYCODE_ENTER;
        if(event.getAction()==KeyEvent.ACTION_DOWN){
            if(k==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE){togglePlayback();return true;}
            if(k==KeyEvent.KEYCODE_MEDIA_PLAY){if(player!=null)player.play();return true;}
            if(k==KeyEvent.KEYCODE_MEDIA_PAUSE){if(player!=null)player.pause();setChrome(true);return true;}
            if(remote&&!chrome){setChrome(true);play.requestFocus();scheduleHide();return true;}
            if(remote)scheduleHide();
        }
        return super.dispatchKeyEvent(event);
    }
}
