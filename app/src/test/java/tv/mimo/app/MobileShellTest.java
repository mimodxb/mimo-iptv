package tv.mimo.app;
import android.content.Context;
import android.view.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import tv.mimo.app.mobile.MobileMainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
@LooperMode(LooperMode.Mode.PAUSED)
public class MobileShellTest {
 @Before public void offline(){Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("mimo",0).edit().clear().putString("sources","[]").commit();androidx.preference.PreferenceManager.getDefaultSharedPreferences(c).edit().putBoolean("pref_auto_refresh",false).commit();}
 @Test public void allMobileDestinationsInflate(){check();}
 @Test @Config(qualifiers="land") public void allMobileDestinationsInflateInLandscape(){check();}
 @Test @Config(qualifiers="sw600dp-land") public void tabletDestinationsInflate(){check();}
 private void check(){
  try(ActivityController<MobileMainActivity> controller=Robolectric.buildActivity(MobileMainActivity.class).setup()){
   MobileMainActivity activity=controller.get();
   BottomNavigationView navigation=activity.findViewById(R.id.bottom_navigation);
   for(int id:new int[]{R.id.nav_live_tv,R.id.nav_favorites,R.id.nav_guide,R.id.nav_settings,R.id.nav_home}){
    navigation.setSelectedItemId(id);activity.getSupportFragmentManager().executePendingTransactions();
    assertNotNull(activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container));
   }
  }
 }
}
