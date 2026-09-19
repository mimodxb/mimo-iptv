package tv.mimo.app.mobile;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import tv.mimo.app.*;
import java.util.*;
import java.util.concurrent.*;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class MobilePlaybackActivity extends Activity {
    private static final int ST_CLOSED=0, ST_OPENING=1, ST_BUFFERING=2, ST_PLAYING=3, ST_PAUSED=4, ST_RECOVERING=5, ST_UNAVAILABLE=6;
    private static final long INFO_DELAY=4000, CHROME_DELAY=6000, BUF_TIMEOUT=25000, SWITCH_DEBOUNCE=500;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler h=new Handler(Looper.getMainLooper());
    private Repository repo;
    private ExoPlayer player;
    private PlayerView video;
    private FrameLayout root;
    private Repository.Catalog catalog;
    private LinearLayout topBar, controls, infoOverlay;
    private TextView statusText, playPauseBtn, favBtn, infoName, infoCat;
    private ImageView infoLogo;
    private Channel ch;
    private RecoveryPlan recovery;
    private final List<Channel.Stream> cands=new ArrayList<>();
    private boolean active, recovering, chrome=true;
    private int gen;
    private String key, name;
    private ArrayList<String> chanKeys;
    private int curIdx=-1;
    private boolean switching;
    private int state=ST_CLOSED;
    private Runnable bufTimeout;
    private final Runnable hideChrome=()->setChrome(false);
    private final Runnable hideInfo=()->showInfo(false);

    // History guard: only record channel once after genuine playback starts
    private String recordedKey = null;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        repo=new Repository(this);
        key=getIntent().getStringExtra("key");
        name=getIntent().getStringExtra("name");
        chanKeys=getIntent().getStringArrayListExtra("chan_keys");
        curIdx=getIntent().getIntExtra("idx",-1);
        if(key==null){finish();return;}
        if(curIdx<0&&chanKeys!=null)curIdx=chanKeys.indexOf(key);

        root=new FrameLayout(this);
        root.setBackgroundColor(android.graphics.Color.BLACK);
        root.setFocusable(true);

        video=new PlayerView(this);
        video.setUseController(false);
        video.setKeepScreenOn(true);
        root.addView(video, new FrameLayout.LayoutParams(-1,-1));

        buildInfo();
        buildTopBar();
        buildControls();
        setContentView(root);
        playPauseBtn.requestFocus();
    }

    @Override protected void onStart(){super.onStart(); if(key!=null){active=true; begin();}}
    @Override protected void onResume(){super.onResume(); if(playPauseBtn!=null) playPauseBtn.requestFocus();}
    @Override protected void onPause(){super.onPause();}
    @Override protected void onStop(){active=false; gen++; cancelTimers(); release(); super.onStop();}
    @Override protected void onDestroy(){io.shutdownNow(); super.onDestroy();}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (video != null && player != null) {
            video.getLayoutParams().width = -1;
            video.getLayoutParams().height = -1;
            video.requestLayout();
        }
    }

    private void buildInfo(){
        infoOverlay=MobileStyle.row(this);
        infoOverlay.setPadding(MobileStyle.dp(this,16), MobileStyle.dp(this,10), MobileStyle.dp(this,16), MobileStyle.dp(this,10));
        infoOverlay.setBackground(MobileStyle.shape(MobileStyle.PANEL, MobileStyle.dp(this,10), 0));
        infoLogo=new ImageView(this);
        infoLogo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(MobileStyle.dp(this,36), MobileStyle.dp(this,36));
        ilp.setMarginEnd(MobileStyle.dp(this,10));
        infoOverlay.addView(infoLogo, ilp);
        LinearLayout t=MobileStyle.column(this);
        infoName=MobileStyle.text(this, "", 15, MobileStyle.INK);
        infoName.setTypeface(null, android.graphics.Typeface.BOLD);
        t.addView(infoName, new LinearLayout.LayoutParams(-1,-2));
        infoCat=MobileStyle.text(this, "", 11, MobileStyle.MUTED);
        t.addView(infoCat, new LinearLayout.LayoutParams(-1,-2));
        infoOverlay.addView(t, new LinearLayout.LayoutParams(-1,-2));
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(-2,-2, Gravity.TOP|Gravity.START);
        fp.setMargins(MobileStyle.dp(this,28), MobileStyle.dp(this,28), 0, 0);
        root.addView(infoOverlay, fp);
        infoOverlay.setVisibility(View.GONE);
    }

    private void buildTopBar(){
        topBar=MobileStyle.column(this);
        topBar.setPadding(MobileStyle.dp(this,22), MobileStyle.dp(this,12), MobileStyle.dp(this,22), MobileStyle.dp(this,12));
        topBar.setBackground(MobileStyle.shape(0xe6091516, MobileStyle.dp(this,12), 0));
        TextView t=MobileStyle.text(this, name==null?"MIMO TV":name, 24, MobileStyle.INK);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        topBar.addView(t);
        statusText=MobileStyle.text(this, tr("play_opening"), 13, MobileStyle.MUTED);
        topBar.addView(statusText);
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(-1, MobileStyle.dp(this,87), Gravity.TOP);
        fp.setMargins(MobileStyle.dp(this,36), MobileStyle.dp(this,28), MobileStyle.dp(this,36), 0);
        root.addView(topBar, fp);
    }

    private void buildControls(){
        controls=MobileStyle.row(this);
        controls.setPadding(MobileStyle.dp(this,12), MobileStyle.dp(this,12), MobileStyle.dp(this,12), MobileStyle.dp(this,12));
        controls.setBackground(MobileStyle.shape(0xee091516, MobileStyle.dp(this,12), 0));
        addBtn(tr("play_back"), this::finish);
        playPauseBtn=addBtn(tr("play_pause"), this::togglePlayback);
        addBtn(tr("play_prev"), this::prevChan);
        addBtn(tr("play_next"), this::nextChan);
        addBtn(tr("play_last"), this::lastChan);
        favBtn=addBtn(repo.favorites().contains(key) ? tr("play_fav_done") : tr("play_fav_add"),
            () -> { repo.toggleFavorite(key); favBtn.setText(repo.favorites().contains(key) ? tr("play_fav_done") : tr("play_fav_add")); scheduleHide(); });
        addBtn(tr("play_retry"), this::begin);
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(-1, MobileStyle.dp(this,72), Gravity.BOTTOM);
        fp.setMargins(MobileStyle.dp(this,36), 0, MobileStyle.dp(this,36), MobileStyle.dp(this,28));
        root.addView(controls, fp);
    }

    private TextView addBtn(String label, Runnable action){
        TextView b=MobileStyle.button(this, label, action);
        b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0, -1, 1);
        p.setMarginEnd(MobileStyle.dp(this,8));
        controls.addView(b, p);
        return b;
    }

    private void release(){
        if(player!=null){
            video.setPlayer(null);
            player.release();
            player=null;
        }
    }

    private void cancelTimers(){
        h.removeCallbacks(hideChrome);
        h.removeCallbacks(hideInfo);
        if(bufTimeout!=null) h.removeCallbacks(bufTimeout);
    }

    private void begin(){
        if(!active) return;
        recordedKey = null; // Reset history guard for new channel
        int tok=++gen;
        cancelTimers();
        release();
        recovery=new RecoveryPlan();
        cands.clear();
        recovering=true;
        setState(ST_OPENING);
        setChrome(true);
        io.execute(()->{
            if(catalog==null) catalog=repo.load(false);
            Channel found=null;
            for(Channel c: catalog.channels) if(c.key.equals(key)){ found=c; break; }
            Channel r=found;
            h.post(()->{
                if(!active || gen!=tok) return;
                ch=r;
                recovering=false;
                if(ch==null){ setState(ST_UNAVAILABLE); return; }
                cands.addAll(ch.streams);
                playNext();
            });
        });
    }

    private void playNext(){
        if(!active) return;
        Channel.Stream s=recovery.next(cands);
        if(s==null){ recover(); return; }
        openStream(s);
    }

    private void openStream(Channel.Stream s){
        release();
        recovering=false;
        setState(ST_OPENING);
        setChrome(true);
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory()
            .setUserAgent("MIMO-TV/0.1 AndroidMobile")
            .setConnectTimeoutMs(10000)
            .setReadTimeoutMs(12000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(s.headers);
        player=new ExoPlayer.Builder(this)
            .setMediaSourceFactory(new DefaultMediaSourceFactory(this).setDataSourceFactory(http))
            .build();
        player.setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build(), true);
        player.setHandleAudioBecomingNoisy(true);
        video.setPlayer(player);
        int tok=gen;
        player.addListener(new Player.Listener(){
            @Override public void onPlaybackStateChanged(int st){
                if(!active || gen!=tok || recovering) return;
                if(bufTimeout!=null) h.removeCallbacks(bufTimeout);
                if(st==Player.STATE_READY){
                    if(player!=null && player.isPlaying()){
                        setState(ST_PLAYING);
                        showInfo(true);
                        scheduleHide();
                        recordHistoryIfNeeded();
                    }
                } else if(st==Player.STATE_BUFFERING){
                    setState(ST_BUFFERING);
                    armTimeout(tok);
                } else if(st==Player.STATE_ENDED){
                    statusText.setText(tr("play_ended"));
                    recover();
                }
            }
            @Override public void onIsPlayingChanged(boolean playing){
                if(!active) return;
                video.setKeepScreenOn(playing);
                playPauseBtn.setText(playing ? tr("play_pause") : tr("play_resume"));
                if(playing){
                    setState(ST_PLAYING);
                    showInfo(true);
                    scheduleHide();
                    recordHistoryIfNeeded();
                } else {
                    setState(ST_PAUSED);
                    statusText.setText(tr("play_paused"));
                    setChrome(true);
                    h.removeCallbacks(hideChrome);
                }
            }
            @Override public void onPlayerError(PlaybackException error){
                if(active && gen==tok) recover();
            }
        });
        player.setMediaItem(MediaItem.fromUri(s.url));
        player.prepare();
        player.play();
        armTimeout(tok);
    }

    private void recordHistoryIfNeeded(){
        if (recordedKey == null && key != null) {
            repo.setLastChannel(key);
            repo.addRecentlyWatched(key);
            recordedKey = key;
        }
    }

    private void armTimeout(int tok){
        if(bufTimeout!=null) h.removeCallbacks(bufTimeout);
        bufTimeout=()->{
            if(active && gen==tok && player!=null && player.getPlaybackState()==Player.STATE_BUFFERING && player.getPlayWhenReady())
                recover();
        };
        h.postDelayed(bufTimeout, BUF_TIMEOUT);
    }

    private void recover(){
        if(!active || recovering) return;
        recovering=true;
        cancelTimers();
        release();
        setChrome(true);
        if(ch!=null && ch.priority() && recovery.claimRefresh()){
            setState(ST_RECOVERING);
            statusText.setText(tr("play_refreshing"));
            int tok=gen;
            io.execute(()->{
                List<Channel.Stream> refreshed;
                try{ refreshed=repo.refreshPriority(key); }
                catch(Exception e){ refreshed=Collections.emptyList(); }
                List<Channel.Stream> r=refreshed;
                h.post(()->{
                    if(!active || gen!=tok) return;
                    recovering=false;
                    cands.addAll(0, r);
                    playNext();
                });
            });
            return;
        }
        Channel.Stream next=recovery.next(cands);
        if(next!=null){ openStream(next); return; }
        setState(ST_UNAVAILABLE);
    }

    private void togglePlayback(){
        if(player==null){ begin(); return; }
        if(player.isPlaying()){
            player.pause();
            playPauseBtn.setText(tr("play_resume"));
            setState(ST_PAUSED);
            statusText.setText(tr("play_paused"));
            setChrome(true);
            h.removeCallbacks(hideChrome);
        } else {
            player.play();
            playPauseBtn.setText(tr("play_pause"));
            setState(ST_PLAYING);
            statusText.setText(tr("play_live")+"  ·  "+name);
            showInfo(true);
            scheduleHide();
        }
    }

    private void scheduleHide(){
        h.removeCallbacks(hideChrome);
        if(player!=null && player.isPlaying()) h.postDelayed(hideChrome, CHROME_DELAY);
    }

    private void setState(int s){
        state=s;
        if(statusText==null) return;
        switch(s){
            case ST_OPENING: statusText.setText(tr("play_opening")); break;
            case ST_BUFFERING: statusText.setText(tr("play_buffering")); break;
            case ST_PLAYING: statusText.setText(tr("play_live")+"  ·  "+name); break;
            case ST_PAUSED: statusText.setText(tr("play_paused")); break;
            case ST_RECOVERING: break;
            case ST_UNAVAILABLE: statusText.setText(tr("play_unavailable")); setChrome(true); break;
        }
    }

    private void showInfo(boolean visible){
        if(!visible || ch==null){ infoOverlay.setVisibility(View.GONE); return; }
        infoName.setText(ch.name);
        String cat=ch.category();
        String co=ch.country;
        infoCat.setText(co!=null && !co.isEmpty() ? cat+"  ·  "+co.toUpperCase(Locale.ROOT) : cat);
        if(ch.logo!=null && !ch.logo.isEmpty())
            ChannelLogoLoader.load(infoLogo, ch.logo, MobileStyle.dp(this,36), MobileStyle.dp(this,36));
        else
            infoLogo.setImageBitmap(ChannelLogoLoader.placeholder(MobileStyle.dp(this,36), MobileStyle.dp(this,36)));
        infoOverlay.setVisibility(View.VISIBLE);
        h.removeCallbacks(hideInfo);
        h.postDelayed(hideInfo, INFO_DELAY);
    }

    private void setChrome(boolean v){
        chrome=v;
        topBar.setVisibility(v?View.VISIBLE:View.GONE);
        controls.setVisibility(v?View.VISIBLE:View.GONE);
        if(!v) root.requestFocus();
    }

    private void prevChan(){
        if(chanKeys==null || chanKeys.isEmpty() || switching) return;
        int next=curIdx<=0 ? chanKeys.size()-1 : curIdx-1;
        switchTo(chanKeys.get(next), next);
    }

    private void nextChan(){
        if(chanKeys==null || chanKeys.isEmpty() || switching) return;
        int next=curIdx>=chanKeys.size()-1 ? 0 : curIdx+1;
        switchTo(chanKeys.get(next), next);
    }

    private void lastChan(){
        String prev=repo.previousChannel();
        if(prev==null || prev.isEmpty() || prev.equals(key)) return;
        int idx=chanKeys!=null ? chanKeys.indexOf(prev) : -1;
        switchTo(prev, idx);
    }

    private void switchTo(String newKey, int newIdx){
        if(switching || newKey.equals(key)) return;
        switching=true;
        key=newKey;
        curIdx=newIdx;
        name=newKey;
        if(catalog!=null) for(Channel c: catalog.channels) if(c.key.equals(newKey)){ name=c.name; break; }
        ((TextView)topBar.getChildAt(0)).setText(name);
        favBtn.setText(repo.favorites().contains(key) ? tr("play_fav_done") : tr("play_fav_add"));
        // Do NOT record history here - wait for genuine playback
        begin();
        h.postDelayed(()->switching=false, SWITCH_DEBOUNCE);
    }

    private String tr(String k){ return Strings.get(k, repo.language()); }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            if(chrome){ setChrome(false); }
            else { setChrome(true); showInfo(true); scheduleHide(); }
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent ev){
        int k=ev.getKeyCode();
        if(ev.getAction()==KeyEvent.ACTION_DOWN){
            if(k==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE){ togglePlayback(); return true; }
            if(k==KeyEvent.KEYCODE_MEDIA_PLAY){
                if(player!=null){ player.play(); playPauseBtn.setText(tr("play_pause")); setState(ST_PLAYING); statusText.setText(tr("play_live")+"  ·  "+name); showInfo(true); scheduleHide(); }
                return true;
            }
            if(k==KeyEvent.KEYCODE_MEDIA_PAUSE){
                if(player!=null){ player.pause(); playPauseBtn.setText(tr("play_resume")); setState(ST_PAUSED); statusText.setText(tr("play_paused")); setChrome(true); h.removeCallbacks(hideChrome); }
                return true;
            }
            if(k==KeyEvent.KEYCODE_BACK && chrome){
                setChrome(false);
                return true;
            }
        }
        return super.dispatchKeyEvent(ev);
    }
}