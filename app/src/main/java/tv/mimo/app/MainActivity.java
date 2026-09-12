package tv.mimo.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.text.*;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import static tv.mimo.app.TvStyle.*;

public final class MainActivity extends Activity {
    private Repository repository;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Repository.Catalog catalog=new Repository.Catalog(new ArrayList<>(),new ArrayList<>(),new LinkedHashSet<>());
    private final Map<String,List<Epg.Programme>> guide=new HashMap<>();
    private LinearLayout rail,body;
    private TextView status,titleCount;
    private RecyclerView grid;
    private ChannelsAdapter adapter;
    private String page="Home",category="Azerbaijan",query="",lastChannel="",guideState="Load programme information from your playlist guides.";
    private boolean loading,guideLoading,reloadQueued;
    private final Map<String,TextView> nav=new LinkedHashMap<>();
    private static final String[] CATEGORIES={"Azerbaijan","Russia","Turkey","Europe","World"};

    @Override public void onCreate(Bundle state){
        super.onCreate(state);immersive(this);repository=new Repository(this);
        if(state!=null){page=state.getString("page","Home");category=state.getString("category","Azerbaijan");query=state.getString("query","");lastChannel=state.getString("channel","");}
        render();reload();
    }
    @Override protected void onResume(){super.onResume();immersive(this);if(adapter!=null){updateGrid();restoreChannelFocus();}}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("page",page);out.putString("category",category);out.putString("query",query);out.putString("channel",lastChannel);}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);io.shutdownNow();super.onDestroy();}
    private int d(float v){return dp(this,v);}
    private void add(LinearLayout parent,View child,int height){parent.addView(child,new LinearLayout.LayoutParams(-1,height<0?height:d(height)));}
    private void gap(LinearLayout parent,int height){add(parent,new View(this),height);}
    private void navigate(String next){page=next;query="";lastChannel="";render();}
    private void render(){
        LinearLayout root=row(this);root.setGravity(Gravity.TOP);root.setBackgroundColor(BG);root.setPadding(d(28),d(24),d(30),d(24));
        rail=column(this);rail.setPadding(0,0,d(18),0);root.addView(rail,new LinearLayout.LayoutParams(d(153),-1));
        LinearLayout brand=row(this);ImageView emblem=new ImageView(this);emblem.setImageResource(R.drawable.mimo_emblem);emblem.setContentDescription("Mimo's Collective emblem");brand.addView(emblem,new LinearLayout.LayoutParams(d(34),d(34)));
        TextView wordmark=text(this,"  MIMO TV",19,INK);wordmark.setTypeface(null,Typeface.BOLD);brand.addView(wordmark);add(rail,brand,48);gap(rail,27);
        nav.clear();
        for(String name:new String[]{"Home","Search","Favorites","Guide","Settings"}){
            String symbol=name.equals("Home")?"⌂":name.equals("Search")?"⌕":name.equals("Favorites")?"♡":name.equals("Guide")?"▤":"⚙";
            TextView button=button(this,symbol+"   "+name,()->navigate(name));button.setTextSize(14);
            if(name.equals(page)){button.setTextColor(GREEN);button.setBackground(shape(TEAL,d(9),0));}
            nav.put(name,button);add(rail,button,44);gap(rail,8);
        }
        rail.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));
        TextView collective=text(this,"MIMO’S COLLECTIVE\nMade for your living room",10,MUTED);collective.setLineSpacing(d(5),1);add(rail,collective,42);
        body=column(this);root.addView(body,new LinearLayout.LayoutParams(0,-1,1));setContentView(root);
        grid=null;adapter=null;
        if(page.equals("Settings"))renderSettings();else renderBrowse();
        TextView active=nav.get(page);if(active!=null)active.requestFocus();
    }
    private void renderBrowse(){
        LinearLayout heading=row(this);TextView title=text(this,page.equals("Home")?"A little closer to home.":page.equals("Guide")?"What’s on.":page.equals("Favorites")?"Your favorites.":"Find your channel.",29,INK);
        title.setTypeface(null,Typeface.BOLD);heading.addView(title,new LinearLayout.LayoutParams(0,d(40),1));
        TextView live=text(this,"●  LIVE TV",11,GREEN);heading.addView(live);add(body,heading,40);
        status=text(this,loading?"Refreshing your playlists…":"Your channels, together.",12,MUTED);add(body,status,20);
        if(page.equals("Home")){
            gap(body,8);LinearLayout hero=row(this);hero.setBackground(shape(TEAL,d(14),0));hero.setPadding(d(22),d(8),d(18),d(8));
            LinearLayout copy=column(this);TextView label=text(this,"FROM MIMO’S COLLECTIVE",10,GOLD);label.setLetterSpacing(.14f);add(copy,label,22);
            TextView home=text(this,"Familiar voices.\nOne place to watch.",21,INK);home.setTypeface(null,Typeface.BOLD);add(copy,home,49);
            add(copy,text(this,"Azerbaijan first. A world of channels beyond.",12,MUTED),21);hero.addView(copy,new LinearLayout.LayoutParams(0,-1,1));
            ImageView mark=new ImageView(this);mark.setImageResource(R.drawable.mimo_emblem);mark.setContentDescription("MIMO TV botanical monogram");mark.setScaleType(ImageView.ScaleType.FIT_CENTER);hero.addView(mark,new LinearLayout.LayoutParams(d(88),d(88)));add(body,hero,108);gap(body,10);
        }
        if(page.equals("Search")){
            EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(INK);search.setTextSize(18);search.setHintTextColor(MUTED);search.setHint("Channel name or country");search.setContentDescription("Search channels");search.setText(query);search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);add(body,search,52);
            search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){query=s.toString();updateGrid();}public void afterTextChanged(Editable e){}});
            search.setOnEditorActionListener((t,action,event)->{((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);if(grid!=null)grid.requestFocus();return true;});gap(body,12);
        }
        if(page.equals("Home")||page.equals("Guide")){
            LinearLayout categories=row(this);
            for(String c:CATEGORIES){TextView tab=button(this,c,()->{category=c;render();});tab.setTextSize(12);tab.setGravity(Gravity.CENTER);tab.setPadding(d(7),d(8),d(7),d(8));
                if(c.equals(category)){tab.setTextColor(GREEN);tab.setBackground(shape(TEAL,d(9),0));}
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(39),1);p.setMarginEnd(d(6));categories.addView(tab,p);
                // Switching category preserves focus on the matching tab rather than jumping to the rail.
                tab.setOnClickListener(v->{category=c;updateGrid();for(int i=0;i<categories.getChildCount();i++)((TextView)categories.getChildAt(i)).setTextColor(categories.getChildAt(i)==tab?GREEN:INK);});
            }add(body,categories,40);gap(body,6);
        }
        if(page.equals("Guide")){
            add(body,button(this,guideLoading?"Loading guide…":"↻  Load / refresh programme guide",this::loadGuide),44);gap(body,6);
            TextView info=text(this,guideState,12,MUTED);info.setMaxLines(2);add(body,info,38);
        }
        titleCount=text(this,"",13,INK);add(body,titleCount,26);
        grid=new RecyclerView(this);grid.setId(View.generateViewId());grid.setClipToPadding(false);grid.setPadding(d(3),d(4),d(3),d(6));
        grid.setLayoutManager(new GridLayoutManager(this,page.equals("Guide")?2:4));grid.setItemAnimator(null);grid.setPreserveFocusAfterLayout(true);
        adapter=new ChannelsAdapter();grid.setAdapter(adapter);body.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        TextView hint=text(this,"D-pad  Move     OK  Watch     Hold OK  Favorite     Back  Return",10,MUTED);add(body,hint,21);updateGrid();
    }
    private void reload(){
        if(loading){Log.w("MIMO_DIAG","reload() QUEUED");reloadQueued=true;return;}
        loading=true;if(status!=null)status.setText("Refreshing your playlists…");
        Log.w("MIMO_DIAG","reload() starting on thread="+Thread.currentThread().getName());
        io.execute(()->{
            Log.w("MIMO_DIAG","reload() IO thread start");
            Repository.Catalog cached=repository.load(false);
            Log.w("MIMO_DIAG","reload() cached: channels="+cached.channels.size()+" posting UI");
            runOnUiThread(()->{if(!isDestroyed()){catalog=cached;updateGrid();Log.w("MIMO_DIAG","reload() cached UI posted, loading="+loading);}});
            Repository.Catalog fresh=repository.load(true);
            Log.w("MIMO_DIAG","reload() fresh: channels="+fresh.channels.size()+" posting UI");
            runOnUiThread(()->{if(!isDestroyed()){loading=false;if(reloadQueued){reloadQueued=false;Log.w("MIMO_DIAG","reload() REQUEUED");reload();return;}catalog=fresh;updateGrid();if(page.equals("Settings"))render();Log.w("MIMO_DIAG","reload() DONE");}else{Log.w("MIMO_DIAG","reload() activity DESTROYED");}});
        });
    }
    private void updateGrid(){
        if(adapter==null)return;
        Set<String> favorites=repository.favorites();List<Channel> filtered=new ArrayList<>();String term=Channel.normalize(query);
        for(Channel c:catalog.channels){
            if(page.equals("Favorites")&&!favorites.contains(c.key))continue;
            if((page.equals("Home")||page.equals("Guide"))&&!c.category().equals(category))continue;
            if(page.equals("Search")&&!Channel.normalize(c.name+" "+c.category()+" "+c.country).contains(term))continue;
            filtered.add(c);
        }
        Log.w("MIMO_DIAG","updateGrid: page="+page+" cat="+category+" total="+catalog.channels.size()+" filtered="+filtered.size()+" loading="+loading);
        adapter.items=filtered;adapter.notifyDataSetChanged();
        titleCount.setText(filtered.isEmpty()?(page.equals("Favorites")?"No favorites yet — hold OK on a channel to save it.":"No channels found. Check your playlists in Settings."):
            (page.equals("Home")||page.equals("Guide")?category+"  ·  ":"")+filtered.size()+" channels");
        if(status!=null)status.setText(loading?"Refreshing your playlists…":catalog.notices.isEmpty()?
            "Your channels, together.  ·  "+catalog.channels.stream().filter(c->!c.streams.isEmpty()).count()+" available entries":String.join("  ",catalog.notices));
    }
    private void restoreChannelFocus(){
        if(grid==null||adapter==null||lastChannel.isEmpty())return;
        for(int i=0;i<adapter.items.size();i++)if(adapter.items.get(i).key.equals(lastChannel)){
            int position=i;grid.scrollToPosition(position);grid.post(()->{RecyclerView.ViewHolder holder=grid.findViewHolderForAdapterPosition(position);if(holder!=null)holder.itemView.requestFocus();});break;
        }
    }
    private void watch(Channel c){lastChannel=c.key;Intent intent=new Intent(this,PlaybackActivity.class);intent.putExtra("key",c.key);intent.putExtra("name",c.name);startActivity(intent);}
    private void favorite(Channel c){boolean added=repository.toggleFavorite(c.key);Toast.makeText(this,added?"Added to favorites":"Removed from favorites",Toast.LENGTH_SHORT).show();updateGrid();restoreChannelFocus();}
    private String programme(Channel c){
        List<Epg.Programme> list=guide.get(c.tvgId);if(list==null||list.isEmpty())return "Programme information unavailable";
        long now=System.currentTimeMillis();String current="Now  ·  No listing",next="";SimpleDateFormat time=new SimpleDateFormat("HH:mm",Locale.getDefault());
        for(Epg.Programme p:list){if(p.start<=now&&p.stop>now)current="Now  ·  "+p.title;else if(p.start>now){next="\n"+time.format(new Date(p.start))+"  ·  "+p.title;break;}}
        return current+next;
    }
    private final class ChannelsAdapter extends RecyclerView.Adapter<CardHolder>{
        List<Channel> items=new ArrayList<>();
        @Override public CardHolder onCreateViewHolder(ViewGroup parent,int type){
            LinearLayout card=column(MainActivity.this);card.setPadding(d(12),d(10),d(12),d(10));focus(card,PANEL);
            RecyclerView.LayoutParams p=new RecyclerView.LayoutParams(-1,d(page.equals("Guide")?119:106));p.setMargins(d(4),d(4),d(4),d(4));card.setLayoutParams(p);
            TextView label=text(MainActivity.this,"",9,GOLD);label.setLetterSpacing(.08f);add(card,label,21);
            TextView name=text(MainActivity.this,"",18,INK);name.setTypeface(null,Typeface.BOLD);name.setMaxLines(1);name.setEllipsize(TextUtils.TruncateAt.END);add(card,name,29);
            TextView description=text(MainActivity.this,"",11,MUTED);description.setMaxLines(2);description.setEllipsize(TextUtils.TruncateAt.END);add(card,description,-1);
            return new CardHolder(card,label,name,description);
        }
        @Override public void onBindViewHolder(CardHolder h,int pos){
            Channel c=items.get(pos);boolean fav=repository.favorites().contains(c.key);
            h.label.setText((fav?"♥  ":"")+(c.priority()?"PRIORITY":"LIVE TV")+"  /  "+c.country.toUpperCase(Locale.ROOT));h.name.setText(c.name);
            h.description.setText(c.streams.isEmpty()?"Source unavailable · OK to refresh":page.equals("Guide")?programme(c):c.category()+"  ·  Watch live");
            h.itemView.setContentDescription(c.name+(c.streams.isEmpty()?", source unavailable":"")+(fav?", favorite":"")+". Press OK to watch. Hold OK to toggle favorite.");
            h.itemView.setOnClickListener(v->watch(c));h.itemView.setOnLongClickListener(v->{lastChannel=c.key;favorite(c);return true;});
            h.itemView.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_MENU&&event.getAction()==KeyEvent.ACTION_UP){lastChannel=c.key;favorite(c);return true;}
                if(key==KeyEvent.KEYCODE_DPAD_LEFT&&event.getAction()==KeyEvent.ACTION_DOWN&&h.getBindingAdapterPosition()%((GridLayoutManager)grid.getLayoutManager()).getSpanCount()==0){nav.get(page).requestFocus();return true;}return false;});
        }
        @Override public int getItemCount(){return items.size();}
    }
    private static final class CardHolder extends RecyclerView.ViewHolder {
        final TextView label,name,description;
        CardHolder(View root,TextView label,TextView name,TextView description){super(root);this.label=label;this.name=name;this.description=description;}
    }
    private void loadGuide(){
        if(guideLoading)return;
        Set<String> urls=new LinkedHashSet<>(catalog.epgUrls);if(urls.isEmpty()){guideState="No XMLTV guide configured. Add one in playlist settings.";render();return;}
        Set<String> ids=new HashSet<>();for(Channel c:catalog.channels)ids.add(c.tvgId);
        guideLoading=true;guideState="Loading programme information…";render();
        io.execute(()->{
            Map<String,List<Epg.Programme>> result=new HashMap<>();int failures=0;
            for(String url:urls)try{Map<String,List<Epg.Programme>> incoming=Epg.parse(Repository.fetch(url,32*1024*1024),ids,System.currentTimeMillis());
                for(Map.Entry<String,List<Epg.Programme>> e:incoming.entrySet())if(!result.containsKey(e.getKey()))result.put(e.getKey(),e.getValue());
            }catch(Exception e){failures++;}
            String message=result.size()+" channels with listings"+(failures>0?". Some guides could not be loaded.":". Times shown in your TV’s time zone.");
            runOnUiThread(()->{if(!isDestroyed()){guideLoading=false;guide.clear();guide.putAll(result);guideState=message;if(page.equals("Guide"))render();}});
        });
    }
    private void renderSettings(){
        add(body,text(this,"Make yourself at home.",29,INK),46);add(body,text(this,"Playlists, programme guides, and Mimo’s Collective.",13,MUTED),30);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);LinearLayout content=column(this);scroll.addView(content);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        gap(content,12);add(content,text(this,"YOUR PLAYLISTS",11,GOLD),28);
        for(Source s:repository.sources()){
            String host;try{host=java.net.URI.create(s.url).getHost();}catch(Exception e){host="Playlist";}
            TextView entry=button(this,s.name+"  ·  "+(s.enabled?"On":"Off")+"\n"+host,()->sourceActions(s));entry.setTextSize(14);add(content,entry,64);gap(content,8);
        }
        add(content,button(this,"+  Add playlist",()->editSource(null)),46);gap(content,8);
        add(content,button(this,loading?"Refreshing…":"↻  Refresh all playlists",this::reload),46);gap(content,8);
        add(content,button(this,"Check Xəzər / Space backend status",this::diagnostics),46);gap(content,8);
        if(repository.sources().stream().noneMatch(s->s.url.equals(Repository.PLAYLIST))) {
            add(content,button(this,"Restore Mimo IPTV",()->{List<Source> sources=repository.sources();sources.add(0,new Source("mimo", "Mimo IPTV", Repository.PLAYLIST, Repository.EPG,true));repository.saveSources(sources);render();reload();}),46);gap(content,8);
        }
        gap(content,20);add(content,text(this,"MIMO TV  /  PHASE 01",11,GOLD),24);
        add(content,text(this,"Native Android TV · Media3 playback\nFavorites and playlists are saved on this TV.\nHTTP streams are supported for legacy broadcasters.\nGuide coverage depends on your sources.",13,MUTED),92);
        ImageView original=new ImageView(this);original.setImageResource(R.drawable.mimo_original);original.setContentDescription("Original Mimo’s Collective logo, by Movsum Mirzazada");original.setScaleType(ImageView.ScaleType.FIT_START);add(content,original,210);
    }
    private void sourceActions(Source source){
        new AlertDialog.Builder(this).setTitle(source.name).setItems(new String[]{"Edit playlist",source.enabled?"Disable":"Enable","Remove"},(dialog,index)->{
            if(index==0){editSource(source);return;}
            List<Source> sources=repository.sources();
            if(index==1){for(int i=0;i<sources.size();i++)if(sources.get(i).id.equals(source.id))sources.set(i,new Source(source.id,source.name,source.url,source.epg,!source.enabled));repository.saveSources(sources);render();reload();}
            else new AlertDialog.Builder(this).setTitle("Remove "+source.name+"?").setMessage("Removes this playlist from MIMO TV on this television.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{sources.removeIf(s->s.id.equals(source.id));repository.saveSources(sources);render();reload();}).show();
        }).show();
    }
    private EditText field(LinearLayout parent,String hint,String value,boolean url){
        EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(16);input.setTextColor(INK);input.setHintTextColor(MUTED);input.setHint(hint);input.setContentDescription(hint);input.setText(value);
        input.setInputType(url?android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI:android.text.InputType.TYPE_CLASS_TEXT);
        add(parent,input,54);return input;
    }
    private void editSource(Source existing){
        LinearLayout form=column(this);form.setPadding(d(24),d(8),d(24),d(8));
        EditText name=field(form,"Playlist name",existing==null?"":existing.name,false);
        EditText url=field(form,"M3U URL (http or https)",existing==null?"":existing.url,true);
        EditText epg=field(form,"XMLTV guide URL (optional)",existing==null?"":existing.epg,true);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?"Add playlist":"Edit playlist").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(dlg->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String n=name.getText().toString().trim(),u=url.getText().toString().trim(),e=epg.getText().toString().trim();
            if(n.isEmpty()){name.setError("Enter a playlist name");name.requestFocus();return;}
            if(!M3uParser.isHttp(u)){url.setError("Enter a full HTTP or HTTPS playlist URL");url.requestFocus();return;}
            if(!e.isEmpty()&&!M3uParser.isHttp(e)){epg.setError("Enter an HTTP or HTTPS guide URL");epg.requestFocus();return;}
            List<Source> sources=repository.sources();if(sources.stream().anyMatch(s->s.url.equals(u)&&(existing==null||!s.id.equals(existing.id)))){url.setError("This playlist is already added");return;}
            Source saved=new Source(existing==null?UUID.randomUUID().toString():existing.id,n,u,e,existing==null||existing.enabled);
            if(existing==null)sources.add(saved);else for(int i=0;i<sources.size();i++)if(sources.get(i).id.equals(existing.id))sources.set(i,saved);
            repository.saveSources(sources);dialog.dismiss();render();reload();
        }));dialog.show();
    }
    private void diagnostics(){
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Priority channel status").setMessage("Checking Mimo’s backend…").setPositiveButton("Close",null).show();
        io.execute(()->{
            String message;
            try{JSONObject json=new JSONObject(Repository.fetch(Repository.PLAYLIST+"?check=1",512*1024));JSONArray channels=json.getJSONArray("priority_channels");StringBuilder result=new StringBuilder("Backend v"+json.optString("version")+" · "+json.optString("status")+"\n\n");
                for(int i=0;i<channels.length();i++){JSONObject c=channels.getJSONObject(i);result.append(c.optString("name")).append(c.optBoolean("working")?": manifest check passed":": source unavailable").append("\n");}
                message=result+"\nA manifest check does not confirm video playback on this TV.";
            }catch(Exception e){message="The backend check is unavailable. Try again later.";}
            String result=message;runOnUiThread(()->{if(!isDestroyed()&&dialog.isShowing())dialog.setMessage(result);});
        });
    }
    @Override public void onBackPressed(){if(!page.equals("Home")){navigate("Home");}else if(!nav.get("Home").hasFocus())nav.get("Home").requestFocus();else super.onBackPressed();}
}
