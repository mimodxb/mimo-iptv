package tv.mimo.app;

import android.app.Activity;
import android.os.*;
import android.util.Log;
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
    static final int ST_CLOSED=0,ST_OPENING=1,ST_BUFFERING=2,ST_PLAYING=3,ST_PAUSED=4,ST_RECOVERING=5,ST_UNAVAILABLE=6;
    private static final long INFO_DELAY=4000,CHROME_DELAY=6000,BUF_TIMEOUT=25000,SWITCH_DEBOUNCE=500;
    private static final long STALL_CHECK_INTERVAL_MS = 5000; // Check for stalls every 5 seconds
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler h=new Handler(Looper.getMainLooper());
    private Repository repo;
    private ExoPlayer player;
    private PlayerView video;
    private FrameLayout root;
    private Repository.Catalog catalog;
    private LinearLayout top,controls,infoOverlay;
    private TextView status,play,favBtn,infoName,infoCat;
    private ImageView infoLogo;
    private Channel ch;
    private RecoveryPlan recovery;
    private final List<Channel.Stream> cands=new ArrayList<>();
    private boolean active,recovering,chrome=true;
    private int gen;
    private String key,name;
    private ArrayList<String> chanKeys;
    private int curIdx=-1;
    private boolean switching;
    private int state=ST_CLOSED;
    private Runnable bufTimeout;
    private final Runnable hideChrome=()->setChrome(false);
    private final Runnable hideInfo=()->showInfo(false);
    private Runnable stallCheckRunnable;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);immersive(this);repo=new Repository(this);
        key=getIntent().getStringExtra("key");name=getIntent().getStringExtra("name");
        chanKeys=getIntent().getStringArrayListExtra("chan_keys");
        curIdx=getIntent().getIntExtra("idx",-1);
        if(key==null){finish();return;}
        if(curIdx<0&&chanKeys!=null)curIdx=chanKeys.indexOf(key);
        root=new FrameLayout(this);root.setBackgroundColor(android.graphics.Color.BLACK);root.setFocusable(true);
        video=new PlayerView(this);video.setUseController(false);video.setKeepScreenOn(true);
        root.addView(video,new FrameLayout.LayoutParams(-1,-1));
        buildInfo();buildTop();buildControls();
        setContentView(root);play.requestFocus();
    }
    @Override protected void onStart(){super.onStart();if(key!=null){active=true;begin();}}
    @Override protected void onResume(){super.onResume();immersive(this);if(play!=null)play.requestFocus();}
    @Override protected void onStop(){active=false;gen++;cancelTimers();release();super.onStop();}
    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}

    private void buildInfo(){
        infoOverlay=row(this);infoOverlay.setPadding(dp(this,16),dp(this,10),dp(this,16),dp(this,10));
        infoOverlay.setBackground(shape(0xDD091516,dp(this,10),0));
        infoLogo=new ImageView(this);infoLogo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(dp(this,36),dp(this,36));ilp.setMarginEnd(dp(this,10));
        infoOverlay.addView(infoLogo,ilp);
        LinearLayout t=column(this);
        infoName=text(this,"",15,INK);infoName.setTypeface(null,android.graphics.Typeface.BOLD);t.addView(infoName,new LinearLayout.LayoutParams(-1,-2));
        infoCat=text(this,"",11,MUTED);t.addView(infoCat,new LinearLayout.LayoutParams(-1,-2));
        infoOverlay.addView(t,new LinearLayout.LayoutParams(-1,-2));
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START);fp.setMargins(dp(this,28),dp(this,28),0,0);
        root.addView(infoOverlay,fp);infoOverlay.setVisibility(View.GONE);
    }
    private void buildTop(){
        top=column(this);top.setPadding(dp(this,22),dp(this,12),dp(this,22),dp(this,12));top.setBackground(shape(0xe6091516,dp(this,12),0));
        TextView t=text(this,name==null?"MIMO TV":name,24,INK);t.setTypeface(null,android.graphics.Typeface.BOLD);top.addView(t);
        status=text(this,tr("play_opening"),13,MUTED);top.addView(status);
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(-1,dp(this,87),Gravity.TOP);fp.setMargins(dp(this,36),dp(this,28),dp(this,36),0);
        root.addView(top,fp);
    }
    private void buildControls(){
        controls=row(this);controls.setPadding(dp(this,12),dp(this,12),dp(this,12),dp(this,12));controls.setBackground(shape(0xee091516,dp(this,12),0));
        addBtn(tr("play_back"),this::finish);
        play=addBtn(tr("play_pause"),this::togglePlayback);
        addBtn(tr("play_prev"),this::prevChan);
        addBtn(tr("play_next"),this::nextChan);
        addBtn(tr("play_last"),this::lastChan);
        favBtn=addBtn(repo.favorites().contains(key)?tr("play_fav_done"):tr("play_fav_add"),()->{repo.toggleFavorite(key);favBtn.setText(repo.favorites().contains(key)?tr("play_fav_done"):tr("play_fav_add"));scheduleHide();});
        addBtn(tr("play_retry"),this::begin);
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(-1,dp(this,72),Gravity.BOTTOM);fp.setMargins(dp(this,36),0,dp(this,36),dp(this,28));
        root.addView(controls,fp);
    }
    private TextView addBtn(String label,Runnable action){
        TextView b=button(this,label,action);b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMarginEnd(dp(this,8));
        controls.addView(b,p);return b;
    }

    private void release(){if(player!=null){video.setPlayer(null);player.release();player=null;}}
    private void cancelTimers(){h.removeCallbacks(hideChrome);h.removeCallbacks(hideInfo);if(bufTimeout!=null)h.removeCallbacks(bufTimeout); if (stallCheckRunnable != null) h.removeCallbacks(stallCheckRunnable);}

    private void begin(){
        if(!active)return;int tok=++gen;cancelTimers();release();recovery=new RecoveryPlan();cands.clear();recovering=true;
        setState(ST_OPENING);setChrome(true);
        io.execute(()->{
            if(catalog==null)catalog=repo.load(false);
            Channel found=null;for(Channel c:catalog.channels)if(c.key.equals(key)){found=c;break;}
            Channel r=found;h.post(()->{if(!active||gen!=tok)return;ch=r;recovering=false;
                if(ch==null){setState(ST_UNAVAILABLE);return;}
                cands.addAll(ch.streams);playNext();});
        });
    }
    private void playNext(){
        if(!active)return;
        Channel.Stream s=recovery.next(cands);
        if(s==null){recover();return;}
        openStream(s);
    }
    private void openStream(Channel.Stream s){
        release();recovering=false;setState(ST_OPENING);setChrome(true);
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setUserAgent("MIMO-TV/0.1 AndroidTV")
            .setConnectTimeoutMs(10000).setReadTimeoutMs(12000).setAllowCrossProtocolRedirects(true).setDefaultRequestProperties(s.headers);
        player=new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(this).setDataSourceFactory(http)).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
        player.setHandleAudioBecomingNoisy(true);video.setPlayer(player);int tok=gen;
        player.addListener(new Player.Listener(){
            @Override public void onPlaybackStateChanged(int st){
                if(!active||gen!=tok||recovering)return;
                if(bufTimeout!=null)h.removeCallbacks(bufTimeout);
                if(st==Player.STATE_READY){if(player!=null&&player.isPlaying()){setState(ST_PLAYING);showInfo(true);scheduleHide(); recovery.onPlaybackStarted(); startStallDetection();}}
                else if(st==Player.STATE_BUFFERING){setState(ST_BUFFERING);armTimeout(tok);}
                else if(st==Player.STATE_ENDED){status.setText(tr("play_ended"));recover();}
            }
            @Override public void onIsPlayingChanged(boolean playing){
                if(!active)return;video.setKeepScreenOn(playing);
                play.setText(playing?tr("play_pause"):tr("play_resume"));
                if(playing){setState(ST_PLAYING);showInfo(true);scheduleHide(); recovery.onPlaybackStarted(); startStallDetection();}
                else{setState(ST_PAUSED);status.setText(tr("play_paused"));setChrome(true);h.removeCallbacks(hideChrome);}
            }
            @Override public void onPlayerError(PlaybackException error){if(active&&gen==tok)recover();}
        });
        player.setMediaItem(MediaItem.fromUri(s.url));player.prepare();player.play();armTimeout(tok);
    }
    private void armTimeout(int tok){
        if(bufTimeout!=null)h.removeCallbacks(bufTimeout);
        bufTimeout=()->{if(active&&gen==tok&&player!=null&&player.getPlaybackState()==Player.STATE_BUFFERING&&player.getPlayWhenReady())recover();};
        h.postDelayed(bufTimeout,BUF_TIMEOUT);
    }
    
private void startStallDetection() {
        if (stallCheckRunnable != null) h.removeCallbacks(stallCheckRunnable);
        stallCheckRunnable = () -> {
            if (!active || player == null || recovering) return;
            long currentPos = player.getCurrentPosition();
            if (recovery.checkForStall(currentPos)) {
                // Stall detected - trigger recovery/fallback
                Log.w("MIMO_DIAG", "PlaybackActivity: Stall detected for priority channel, triggering fallback");
                recover();
            } else {
                // Schedule next check
                h.postDelayed(stallCheckRunnable, 5000);
            }
        };
        h.postDelayed(stallCheckRunnable, 10000); // Start checking after 10 seconds of playback
    }
    private void recover(){
        if(!active||recovering)return;recovering=true;cancelTimers();release();setChrome(true);
        if(ch!=null&&ch.priority()&&recovery.claimRefresh()){
            setState(ST_RECOVERING);status.setText(tr("play_refreshing"));int tok=gen;
            io.execute(()->{List<Channel.Stream> refreshed;try{refreshed=repo.refreshPriority(key);}catch(Exception e){refreshed=Collections.emptyList();}
                List<Channel.Stream> r=refreshed;h.post(()->{if(!active||gen!=tok)return;recovering=false;cands.addAll(0,r);playNext();});
            });return;
        }
        Channel.Stream next=recovery.next(cands);
        if(next!=null){openStream(next);return;}
        setState(ST_UNAVAILABLE);
    }
    private void togglePlayback(){
        if(player==null){begin();return;}
        if(player.isPlaying()){
            player.pause();
            play.setText(tr("play_resume"));
            setState(ST_PAUSED);status.setText(tr("play_paused"));
            setChrome(true);h.removeCallbacks(hideChrome);
        }else{
            player.play();
            play.setText(tr("play_pause"));
            setState(ST_PLAYING);status.setText(tr("play_live")+"  ·  "+name);
            showInfo(true);scheduleHide();
        }
    }
    private void scheduleHide(){h.removeCallbacks(hideChrome);if(player!=null&&player.isPlaying())h.postDelayed(hideChrome,CHROME_DELAY);}

    private void setState(int s){
        state=s;
        if(status==null)return;
        switch(s){
            case ST_OPENING:status.setText(tr("play_opening"));break;
            case ST_BUFFERING:status.setText(tr("play_buffering"));break;
            case ST_PLAYING:status.setText(tr("play_live")+"  ·  "+name);break;
            case ST_PAUSED:status.setText(tr("play_paused"));break;
            case ST_RECOVERING:break;
            case ST_UNAVAILABLE:status.setText(tr("play_unavailable"));setChrome(true);break;
        }
    }

    private void showInfo(boolean visible){
        if(!visible||ch==null){infoOverlay.setVisibility(View.GONE);return;}
        infoName.setText(ch.name);
        String cat=ch.category();String co=ch.country;
        infoCat.setText(co!=null&&!co.isEmpty()?cat+"  ·  "+co.toUpperCase(Locale.ROOT):cat);
        if(ch.logo!=null&&!ch.logo.isEmpty())ChannelLogoLoader.load(infoLogo,ch.logo,36,36,this);
        else infoLogo.setImageBitmap(ChannelLogoLoader.placeholder(dp(this,36),dp(this,36)));
        infoOverlay.setVisibility(View.VISIBLE);
        h.removeCallbacks(hideInfo);h.postDelayed(hideInfo,INFO_DELAY);
    }

    private void setChrome(boolean v){chrome=v;top.setVisibility(v?View.VISIBLE:View.GONE);controls.setVisibility(v?View.VISIBLE:View.GONE);if(!v)root.requestFocus();}

    private void prevChan(){
        if(chanKeys==null||chanKeys.isEmpty()||switching)return;
        int next=curIdx<=0?chanKeys.size()-1:curIdx-1;
        switchTo(chanKeys.get(next),next);
    }
    private void nextChan(){
        if(chanKeys==null||chanKeys.isEmpty()||switching)return;
        int next=curIdx>=chanKeys.size()-1?0:curIdx+1;
        switchTo(chanKeys.get(next),next);
    }
    private void lastChan(){
        String prev=repo.previousChannel();
        if(prev==null||prev.isEmpty()||prev.equals(key))return;
        int idx=chanKeys!=null?chanKeys.indexOf(prev):-1;
        switchTo(prev,idx);
    }
    private void switchTo(String newKey,int newIdx){
        if(switching||newKey.equals(key))return;switching=true;
        key=newKey;curIdx=newIdx;
        name=newKey;
        if(catalog!=null)for(Channel c:catalog.channels)if(c.key.equals(newKey)){name=c.name;break;}
        ((TextView)top.getChildAt(0)).setText(name);
        favBtn.setText(repo.favorites().contains(key)?tr("play_fav_done"):tr("play_fav_add"));
        repo.setLastChannel(key);repo.addRecentlyWatched(key);
        begin();
        h.postDelayed(()->switching=false,SWITCH_DEBOUNCE);
    }

    private String tr(String k){return Strings.get(k,repo.language());}

    @Override public boolean dispatchKeyEvent(KeyEvent ev){
        int k=ev.getKeyCode();
        boolean remote=k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_DPAD_UP||k==KeyEvent.KEYCODE_DPAD_DOWN||k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT||k==KeyEvent.KEYCODE_ENTER;
        if(ev.getAction()==KeyEvent.ACTION_DOWN){
            if(k==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE){togglePlayback();return true;}
            if(k==KeyEvent.KEYCODE_MEDIA_PLAY){if(player!=null){player.play();play.setText(tr("play_pause"));setState(ST_PLAYING);status.setText(tr("play_live")+"  ·  "+name);showInfo(true);scheduleHide();}return true;}
            if(k==KeyEvent.KEYCODE_MEDIA_PAUSE){if(player!=null){player.pause();play.setText(tr("play_resume"));setState(ST_PAUSED);status.setText(tr("play_paused"));setChrome(true);h.removeCallbacks(hideChrome);}return true;}
            if(k==KeyEvent.KEYCODE_CHANNEL_UP){nextChan();return true;}
            if(k==KeyEvent.KEYCODE_CHANNEL_DOWN){prevChan();return true;}
            if(k==KeyEvent.KEYCODE_MENU){lastChan();return true;}
            if(!chrome&&k==KeyEvent.KEYCODE_DPAD_LEFT){prevChan();return true;}
            if(!chrome&&k==KeyEvent.KEYCODE_DPAD_RIGHT){nextChan();return true;}
            if(remote&&!chrome){setChrome(true);play.requestFocus();showInfo(true);scheduleHide();return true;}
            if(remote)scheduleHide();
        }
        return super.dispatchKeyEvent(ev);
    }

    static int nextIndex(int cur,int size){if(size<=0)return-1;return(cur+1)%size;}
    static int prevIndex(int cur,int size){if(size<=0)return-1;return(cur-1+size)%size;}
}