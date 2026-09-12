package tv.mimo.app;

import android.content.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@LooperMode(LooperMode.Mode.PAUSED)
public class AndroidShellTest {
    private Context context;
    @Before public void isolate(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("mimo",0).edit().clear().putString("sources","[]").commit();}
    private TextView find(View view,String text){
        if(view instanceof TextView && ((TextView)view).getText().toString().contains(text))return (TextView)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){TextView result=find(group.getChildAt(i),text);if(result!=null)return result;}}
        return null;
    }
    @Test public void shellLaunchesAndEveryNavigationEntryOpens(){
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
            MainActivity activity=controller.get();View root=activity.getWindow().getDecorView();
            assertNotNull(find(root,"A little closer to home."));assertTrue(find(root,"Home").isFocusable());
            for(String[] route:new String[][]{{"Search","Find your channel."},{"Favorites","Your favorites."},{"Guide","What’s on."},{"Settings","Make yourself at home."},{"Home","A little closer to home."}}){
                assertTrue(find(root,route[0]).performClick());assertNotNull(find(root,route[1]));
            }
        }
    }
    @Test public void sourceListAndEnabledFlagPersist(){
        Repository repo=new Repository(context);repo.saveSources(Arrays.asList(new Source("one","One","https://example.org/a.m3u","",false),new Source("two","Two","https://example.org/b.m3u","https://example.org/g.xml",true)));
        List<Source> loaded=new Repository(context).sources();assertEquals(2,loaded.size());assertFalse(loaded.get(0).enabled);assertEquals("https://example.org/g.xml",loaded.get(1).epg);
    }
    @Test public void favoritesPersistAndToggleOff(){Repository repo=new Repository(context);assertTrue(repo.toggleFavorite("xezertv.az"));assertTrue(new Repository(context).favorites().contains("xezertv.az"));assertFalse(repo.toggleFavorite("xezertv.az"));assertFalse(repo.favorites().contains("xezertv.az"));}
    @Test public void emptySourcesStayEmptyAcrossRestart(){assertTrue(new Repository(context).sources().isEmpty());assertTrue(new Repository(context).load(false).channels.isEmpty());}
    @Test public void backendPriorityPlaceholdersRemainWhenOffline(){new Repository(context).saveSources(Arrays.asList(new Source("mimo","Mimo IPTV",Repository.PLAYLIST,Repository.EPG,true)));Repository.Catalog catalog=new Repository(context).load(false);assertEquals(2,catalog.channels.size());assertTrue(catalog.channels.get(0).priority());assertTrue(catalog.channels.get(0).streams.isEmpty());assertEquals("spacetv.az",catalog.channels.get(1).key);}
    @Test public void xmltvMatchesExactIdAndHonorsTimezone()throws Exception{
        long now=Epg.time("20260912140000 +0400");String xml="<tv><programme channel='XezerTV.az' start='20260912133000 +0400' stop='20260912143000 +0400'><title>News</title></programme><programme channel='Other.az' start='20260912133000 +0400' stop='20260912143000 +0400'><title>Other</title></programme></tv>";
        Map<String,List<Epg.Programme>> parsed=Epg.parse(xml,new HashSet<>(Arrays.asList("XezerTV.az")),now);assertEquals(1,parsed.size());assertEquals("News",parsed.get("XezerTV.az").get(0).title);assertEquals(Epg.time("20260912100000 +0000"),now);
    }
    @Test(expected=java.io.IOException.class)public void xmltvRejectsEntityDeclarations()throws Exception{Epg.parse("<!DOCTYPE tv [<!ENTITY x SYSTEM 'file:///private'>]><tv/>",Collections.emptySet(),0);}
}
