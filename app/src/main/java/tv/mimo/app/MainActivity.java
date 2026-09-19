package tv.mimo.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.text.*;
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
    private String page="Home",category="Azerbaijan",query="",lastChannel="";
    private boolean loading,guideLoading,reloadQueued;
    private final Map<String,TextView> nav=new LinkedHashMap<>();
    private static final String[] CATEGORIES={"Azerbaijan","Russia","Turkey","Europe","World"};
    private int devTapCount;private long devTapTime;
    private String guideState;

    private String lang(){return repository.language();}
    private String tr(String key){return Strings.get(key,lang());}
    private String tr(String key,Object... args){return Strings.get(key,lang(),args);}

    @Override public void onCreate(Bundle state){
        super.onCreate(state);immersive(this);repository=new Repository(this);
        if(state!=null){page=state.getString("page","Home");category=state.getString("category","Azerbaijan");query=state.getString("query","");lastChannel=state.getString("channel","");}
        guideState=tr("guide_load");
        render();reload();
    }
    @Override protected void onResume(){super.onResume();immersive(this);if(adapter!=null){updateGrid();restoreChannelFocus();}}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("page",page);out.putString("category",category);out.putString("query",query);out.putString("channel",lastChannel);}
    @Override protected void onDestroy(){ChannelLogoLoader.cancel();handler.removeCallbacksAndMessages(null);io.shutdownNow();super.onDestroy();}
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
        String[][] navItems={{"Home",tr("nav_home"),"⌂"},{"Search",tr("nav_search"),"⌕"},{"Favorites",tr("nav_favorites"),"♡"},{"Guide",tr("nav_guide"),"▤"},{"Settings",tr("nav_settings"),"⚙"}};
        for(String[] item:navItems){
            TextView button=button(this,item[2]+"   "+item[1],()->navigate(item[0]));button.setTextSize(14);
            if(item[0].equals(page)){button.setTextColor(GREEN);button.setBackground(shape(TEAL,d(9),0));}
            nav.put(item[0],button);add(rail,button,44);gap(rail,8);
        }
        rail.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));
        TextView collective=text(this,tr("home_footer"),10,MUTED);collective.setLineSpacing(d(5),1);add(rail,collective,42);
        body=column(this);root.addView(body,new LinearLayout.LayoutParams(0,-1,1));setContentView(root);
        grid=null;adapter=null;
        if(page.equals("Settings"))renderSettings();else renderBrowse();
        TextView active=nav.get(page);if(active!=null)active.requestFocus();
    }

    private void renderBrowse(){
        LinearLayout heading=row(this);
        String headline=page.equals("Home")?tr("home_headline"):page.equals("Guide")?tr("guide_headline"):page.equals("Favorites")?tr("fav_headline"):tr("search_headline");
        TextView title=text(this,headline,29,INK);title.setTypeface(null,Typeface.BOLD);heading.addView(title,new LinearLayout.LayoutParams(0,d(40),1));
        TextView live=text(this,tr("live_tv"),11,GREEN);heading.addView(live);add(body,heading,40);
        status=text(this,loading?tr("refreshing"):tr("together"),12,MUTED);add(body,status,20);
        if(page.equals("Home")){
            gap(body,8);LinearLayout hero=row(this);hero.setBackground(shape(TEAL,d(14),0));hero.setPadding(d(22),d(8),d(18),d(8));
            LinearLayout copy=column(this);TextView label=text(this,tr("home_hero_label"),10,GOLD);label.setLetterSpacing(.14f);add(copy,label,22);
            TextView home=text(this,tr("home_hero_title"),21,INK);home.setTypeface(null,Typeface.BOLD);add(copy,home,49);
            add(copy,text(this,tr("home_hero_sub"),12,MUTED),21);hero.addView(copy,new LinearLayout.LayoutParams(0,-1,1));
            ImageView mark=new ImageView(this);mark.setImageResource(R.drawable.mimo_emblem);mark.setContentDescription("MIMO TV botanical monogram");mark.setScaleType(ImageView.ScaleType.FIT_CENTER);hero.addView(mark,new LinearLayout.LayoutParams(d(88),d(88)));add(body,hero,108);gap(body,10);
        }
        if(page.equals("Search")){
            EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(INK);search.setTextSize(18);search.setHintTextColor(MUTED);search.setHint(tr("search_hint"));search.setContentDescription(tr("nav_search"));search.setText(query);search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);add(body,search,52);
            search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){query=s.toString();updateGrid();}public void afterTextChanged(Editable e){}});
            search.setOnEditorActionListener((t,action,event)->{((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);if(grid!=null)grid.requestFocus();return true;});gap(body,12);
        }
        if(page.equals("Home")||page.equals("Guide")){
            LinearLayout categories=row(this);
            for(String c:CATEGORIES){TextView tab=button(this,c,()->{category=c;render();});tab.setTextSize(12);tab.setGravity(Gravity.CENTER);tab.setPadding(d(7),d(8),d(7),d(8));
                if(c.equals(category)){tab.setTextColor(GREEN);tab.setBackground(shape(TEAL,d(9),0));}
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(39),1);p.setMarginEnd(d(6));categories.addView(tab,p);
                tab.setOnClickListener(v->{category=c;updateGrid();for(int i=0;i<categories.getChildCount();i++)((TextView)categories.getChildAt(i)).setTextColor(categories.getChildAt(i)==tab?GREEN:INK);});
            }add(body,categories,40);gap(body,6);
        }
        if(page.equals("Guide")){
            add(body,button(this,guideLoading?tr("guide_loading"):tr("guide_load"),this::loadGuide),44);gap(body,6);
            TextView info=text(this,guideState,12,MUTED);info.setMaxLines(2);add(body,info,38);
        }
        titleCount=text(this,"",13,INK);add(body,titleCount,26);
        grid=new RecyclerView(this);grid.setId(View.generateViewId());grid.setClipToPadding(false);grid.setPadding(d(3),d(4),d(3),d(6));
        grid.setLayoutManager(new GridLayoutManager(this,page.equals("Guide")?2:4));grid.setItemAnimator(null);grid.setPreserveFocusAfterLayout(true);
        adapter=new ChannelsAdapter();grid.setAdapter(adapter);body.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        TextView hint=text(this,tr("hint_dpad"),10,MUTED);add(body,hint,21);updateGrid();
    }

    private void reload(){
        if(loading){reloadQueued=true;return;}
        loading=true;if(status!=null)status.setText(tr("refreshing"));
        io.execute(()->{
            Repository.Catalog cached=repository.load(false);
            runOnUiThread(()->{if(!isDestroyed()){catalog=cached;updateGrid();}});
            Repository.Catalog fresh=repository.load(true);
            runOnUiThread(()->{if(!isDestroyed()){loading=false;if(reloadQueued){reloadQueued=false;reload();return;}catalog=fresh;updateGrid();if(page.equals("Settings"))render();}});
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
        adapter.items=filtered;adapter.notifyDataSetChanged();
        titleCount.setText(filtered.isEmpty()?(page.equals("Favorites")?tr("fav_empty"):tr("no_channels")):
            (page.equals("Home")||page.equals("Guide")?category+"  ·  ":"")+filtered.size()+" "+tr("channels"));
        if(status!=null)status.setText(loading?tr("refreshing"):catalog.notices.isEmpty()?
            tr("together")+"  ·  "+catalog.channels.stream().filter(c->!c.streams.isEmpty()).count()+" "+tr("available"):String.join("  ",catalog.notices));
    }

    private void restoreChannelFocus(){
        if(grid==null||adapter==null||lastChannel.isEmpty())return;
        for(int i=0;i<adapter.items.size();i++)if(adapter.items.get(i).key.equals(lastChannel)){
            int position=i;grid.scrollToPosition(position);grid.post(()->{RecyclerView.ViewHolder holder=grid.findViewHolderForAdapterPosition(position);if(holder!=null)holder.itemView.requestFocus();});break;
        }
    }

    private void watch(Channel c){
        lastChannel=c.key;repository.setLastChannel(c.key);repository.addRecentlyWatched(c.key);
        Intent intent=new Intent(this,PlaybackActivity.class);intent.putExtra("key",c.key);intent.putExtra("name",c.name);
        ArrayList<String> keys=new ArrayList<>();for(Channel ch:adapter.items)keys.add(ch.key);
        intent.putStringArrayListExtra("chan_keys",keys);intent.putExtra("idx",adapter.items.indexOf(c));
        startActivity(intent);
    }
    private void favorite(Channel c){boolean added=repository.toggleFavorite(c.key);Toast.makeText(this,added?tr("added_fav"):tr("removed_fav"),Toast.LENGTH_SHORT).show();updateGrid();restoreChannelFocus();}

    private String programme(Channel c){
        List<Epg.Programme> list=guide.get(c.tvgId);if(list==null||list.isEmpty())return "Programme information unavailable";
        long now=System.currentTimeMillis();String current="Now  ·  No listing",next="";SimpleDateFormat time=new SimpleDateFormat("HH:mm",Locale.getDefault());
        for(Epg.Programme p:list){if(p.start<=now&&p.stop>now)current="Now  ·  "+p.title;else if(p.start>now){next="\n"+time.format(new Date(p.start))+"  ·  "+p.title;break;}}
        return current+next;
    }

    /* ── Channel card adapter with logo ── */
    private final class ChannelsAdapter extends RecyclerView.Adapter<CardHolder>{
        List<Channel> items=new ArrayList<>();
        @Override public CardHolder onCreateViewHolder(ViewGroup parent,int type){
            LinearLayout card=column(MainActivity.this);card.setPadding(d(12),d(10),d(12),d(10));focus(card,PANEL);
            RecyclerView.LayoutParams p=new RecyclerView.LayoutParams(-1,d(page.equals("Guide")?119:106));p.setMargins(d(4),d(4),d(4),d(4));card.setLayoutParams(p);
            LinearLayout top=row(MainActivity.this);
            ImageView logo=new ImageView(MainActivity.this);logo.setScaleType(ImageView.ScaleType.CENTER_CROP);logo.setAdjustViewBounds(false);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(d(40),d(40));lp.setMarginEnd(d(8));top.addView(logo,lp);
            LinearLayout texts=column(MainActivity.this);
            TextView label=text(MainActivity.this,"",9,GOLD);label.setLetterSpacing(.08f);texts.addView(label,new LinearLayout.LayoutParams(-1,d(21)));
            TextView name=text(MainActivity.this,"",18,INK);name.setTypeface(null,Typeface.BOLD);name.setMaxLines(1);name.setEllipsize(TextUtils.TruncateAt.END);texts.addView(name,new LinearLayout.LayoutParams(-1,d(29)));
            top.addView(texts,new LinearLayout.LayoutParams(0,-1,1));add(card,top,-1);
            TextView description=text(MainActivity.this,"",11,MUTED);description.setMaxLines(2);description.setEllipsize(TextUtils.TruncateAt.END);add(card,description,-1);
            return new CardHolder(card,label,name,description,logo);
        }
        @Override public void onBindViewHolder(CardHolder h,int pos){
            Channel c=items.get(pos);boolean fav=repository.favorites().contains(c.key);
            h.label.setText((fav?"♥  ":"")+(c.priority()?"PRIORITY":"LIVE TV")+"  /  "+c.country.toUpperCase(Locale.ROOT));h.name.setText(c.name);
            h.description.setText(c.streams.isEmpty()?tr("source_unavail")+" · OK":page.equals("Guide")?programme(c):c.category()+"  ·  "+tr("watch_live"));
            if(!c.logo.isEmpty()){ChannelLogoLoader.load(h.logo,c.logo,d(40),d(40));}else{h.logo.setImageBitmap(ChannelLogoLoader.placeholder(d(40),d(40)));}
            String desc=c.name+(c.streams.isEmpty()?", "+tr("source_unavail"):"")+(fav?", favorite":"")+". OK.";
            h.itemView.setContentDescription(desc);
            h.itemView.setOnClickListener(v->watch(c));h.itemView.setOnLongClickListener(v->{lastChannel=c.key;favorite(c);return true;});
            h.itemView.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_MENU&&event.getAction()==KeyEvent.ACTION_UP){lastChannel=c.key;favorite(c);return true;}
                if(key==KeyEvent.KEYCODE_DPAD_LEFT&&event.getAction()==KeyEvent.ACTION_DOWN&&h.getBindingAdapterPosition()%((GridLayoutManager)grid.getLayoutManager()).getSpanCount()==0){nav.get(page).requestFocus();return true;}return false;});
        }
        @Override public int getItemCount(){return items.size();}
    }
    private static final class CardHolder extends RecyclerView.ViewHolder {
        final TextView label,name,description;final ImageView logo;
        CardHolder(View root,TextView label,TextView name,TextView description,ImageView logo){super(root);this.label=label;this.name=name;this.description=description;this.logo=logo;}
    }

    /* ── Guide ── */
    private void loadGuide(){
        if(guideLoading)return;
        Set<String> urls=new LinkedHashSet<>(catalog.epgUrls);if(urls.isEmpty()){guideState=tr("guide_none");render();return;}
        Set<String> ids=new HashSet<>();for(Channel c:catalog.channels)ids.add(c.tvgId);
        guideLoading=true;guideState=tr("guide_loading");render();
        io.execute(()->{
            Map<String,List<Epg.Programme>> result=new HashMap<>();int failures=0;
            for(String url:urls)try{Map<String,List<Epg.Programme>> incoming=Epg.parse(Repository.fetch(url,32*1024*1024),ids,System.currentTimeMillis());
                for(Map.Entry<String,List<Epg.Programme>> e:incoming.entrySet())if(!result.containsKey(e.getKey()))result.put(e.getKey(),e.getValue());
            }catch(Exception e){failures++;}
            String message=result.size()+" "+tr("guide_channels")+(failures>0?tr("guide_some_fail"):tr("guide_times"));
            runOnUiThread(()->{if(!isDestroyed()){guideLoading=false;guide.clear();guide.putAll(result);guideState=message;if(page.equals("Guide"))render();}});
        });
    }

    /* ── Settings ── */
    private void renderSettings(){
        add(body,text(this,tr("settings_headline"),29,INK),46);add(body,text(this,tr("settings_sub"),13,MUTED),30);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);LinearLayout content=column(this);scroll.addView(content);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        gap(content,12);add(content,text(this,tr("settings_playlists"),11,GOLD),28);
        for(Source s:repository.sources()){
            String host;try{host=java.net.URI.create(s.url).getHost();}catch(Exception e){host="Playlist";}
            TextView entry=button(this,s.name+"  ·  "+(s.enabled?"On":"Off")+"\n"+host,()->sourceActions(s));entry.setTextSize(14);add(content,entry,64);gap(content,8);
        }
        add(content,button(this,tr("settings_add"),()->editSource(null)),46);gap(content,8);
        add(content,button(this,loading?tr("settings_refreshing"):tr("settings_refresh"),this::reload),46);gap(content,8);
        if(repository.sources().stream().noneMatch(s->s.url.equals(Repository.PLAYLIST))) {
            add(content,button(this,tr("settings_restore"),()->{List<Source> sources=repository.sources();sources.add(0,new Source("mimo", "Mimo IPTV", Repository.PLAYLIST, Repository.EPG,true));repository.saveSources(sources);render();reload();}),46);gap(content,8);
        }

        // Language switcher
        gap(content,12);add(content,text(this,tr("settings_lang"),11,GOLD),28);
        String langLabel=lang().equals("az")?tr("settings_az"):tr("settings_en");
        add(content,button(this,tr("settings_lang")+":  "+langLabel,()->{
            String newLang=lang().equals("az")?"en":"az";
            repository.setLanguage(newLang);render();reload();
        }),46);gap(content,8);

        // About screen — normal click shows About dialog
        gap(content,16);
        TextView aboutBtn=button(this,tr("settings_about"),this::showAbout);aboutBtn.setTextSize(11);
        content.addView(aboutBtn,new LinearLayout.LayoutParams(-1,d(36)));

        // Version text — 5-tap hidden gate for developer diagnostics
        gap(content,6);
        TextView versionBtn=text(this,tr("about_version"),10,MUTED);versionBtn.setPadding(d(12),d(4),d(12),d(4));
        versionBtn.setOnClickListener(v->{
            long now=System.currentTimeMillis();
            if(now-devTapTime>3000)devTapCount=0;
            devTapCount++;devTapTime=now;
            if(devTapCount>=5){devTapCount=0;showDiagnostics();}
        });
        content.addView(versionBtn,new LinearLayout.LayoutParams(-1,d(28)));
    }

    private void showAbout(){
        LinearLayout layout=column(this);layout.setPadding(d(32),d(24),d(32),d(24));
        ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.mimo_emblem);icon.setContentDescription("MIMO TV");
        LinearLayout.LayoutParams iconLp=new LinearLayout.LayoutParams(d(72),d(72));iconLp.gravity=Gravity.CENTER_HORIZONTAL;iconLp.bottomMargin=d(16);
        layout.addView(icon,iconLp);
        TextView title=text(this,tr("about_title"),24,INK);title.setTypeface(null,Typeface.BOLD);title.setGravity(Gravity.CENTER);
        layout.addView(title,new LinearLayout.LayoutParams(-1,-2));
        TextView collective=text(this,tr("about_collective"),16,GOLD);collective.setGravity(Gravity.CENTER);collective.setLetterSpacing(.08f);
        layout.addView(collective,new LinearLayout.LayoutParams(-1,-2));
        layout.addView(new View(this),new LinearLayout.LayoutParams(-1,d(12)));
        TextView desc=text(this,tr("about_desc"),14,MUTED);desc.setGravity(Gravity.CENTER);
        layout.addView(desc,new LinearLayout.LayoutParams(-1,-2));
        layout.addView(new View(this),new LinearLayout.LayoutParams(-1,d(8)));
        TextView creator=text(this,tr("about_creator"),13,MUTED);creator.setGravity(Gravity.CENTER);
        layout.addView(creator,new LinearLayout.LayoutParams(-1,-2));
        new AlertDialog.Builder(this).setView(layout).setPositiveButton(tr("close"),null).show();
    }

    private void showDiagnostics(){
        LinearLayout diag=column(this);diag.setPadding(d(24),d(12),d(24),d(12));
        diag.addView(text(this,tr("settings_dev"),13,GOLD),new LinearLayout.LayoutParams(-1,d(28)));
        add(diag,button(this,"Check Xəzər / Space backend status",this::diagnostics),46);gap(diag,8);
        add(diag,button(this,tr("settings_refresh"),this::reload),46);
        new AlertDialog.Builder(this).setView(diag).setPositiveButton(tr("close"),null).show();
    }

    private void sourceActions(Source source){
        String lang=lang();
        new AlertDialog.Builder(this).setTitle(source.name).setItems(new String[]{tr("edit_playlist"),source.enabled?tr("disable"):tr("enable"),tr("remove")},(dialog,index)->{
            if(index==0){editSource(source);return;}
            List<Source> sources=repository.sources();
            if(index==1){for(int i=0;i<sources.size();i++)if(sources.get(i).id.equals(source.id))sources.set(i,new Source(source.id,source.name,source.url,source.epg,!source.enabled));repository.saveSources(sources);render();reload();}
            else new AlertDialog.Builder(this).setTitle(tr("remove_playlist",source.name)).setMessage(tr("remove_playlist_msg")).setNegativeButton(tr("cancel"),null).setPositiveButton(tr("remove"),(d,w)->{sources.removeIf(s->s.id.equals(source.id));repository.saveSources(sources);render();reload();}).show();
        }).show();
    }
    private EditText field(LinearLayout parent,String hint,String value,boolean url){
        EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(16);input.setTextColor(INK);input.setHintTextColor(MUTED);input.setHint(hint);input.setContentDescription(hint);input.setText(value);
        input.setInputType(url?android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI:android.text.InputType.TYPE_CLASS_TEXT);
        add(parent,input,54);return input;
    }
    private void editSource(Source existing){
        String lang=lang();
        LinearLayout form=column(this);form.setPadding(d(24),d(8),d(24),d(8));
        EditText name=field(form,tr("playlist_name"),existing==null?"":existing.name,false);
        EditText url=field(form,tr("m3u_url"),existing==null?"":existing.url,true);
        EditText epg=field(form,tr("epg_url"),existing==null?"":existing.epg,true);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?tr("add_playlist"):tr("edit_playlist")).setView(form).setNegativeButton(tr("cancel"),null).setPositiveButton(tr("save"),null).create();
        dialog.setOnShowListener(dlg->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String n=name.getText().toString().trim(),u=url.getText().toString().trim(),e=epg.getText().toString().trim();
            if(n.isEmpty()){name.setError(tr("enter_name"));name.requestFocus();return;}
            if(!M3uParser.isHttp(u)){url.setError(tr("enter_url"));url.requestFocus();return;}
            if(!e.isEmpty()&&!M3uParser.isHttp(e)){epg.setError(tr("enter_epg"));epg.requestFocus();return;}
            List<Source> sources=repository.sources();if(sources.stream().anyMatch(s->s.url.equals(u)&&(existing==null||!s.id.equals(existing.id)))){url.setError(tr("already_added"));return;}
            Source saved=new Source(existing==null?UUID.randomUUID().toString():existing.id,n,u,e,existing==null||existing.enabled);
            if(existing==null)sources.add(saved);else for(int i=0;i<sources.size();i++)if(sources.get(i).id.equals(existing.id))sources.set(i,saved);
            repository.saveSources(sources);dialog.dismiss();render();reload();
        }));dialog.show();
    }
    private void diagnostics(){
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Priority channel status").setMessage("Checking Mimo's backend…").setPositiveButton(tr("close"),null).show();
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
