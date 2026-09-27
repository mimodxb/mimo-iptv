package tv.mimo.app.mobile;
import android.content.Context;
import android.os.*;
import tv.mimo.app.Repository;
import java.util.concurrent.*;
import java.util.function.Consumer;
/** Serializes refreshes so category tabs never download/parse eight thousand channels independently. */
public final class MobileCatalog {
 private static final ExecutorService IO=Executors.newSingleThreadExecutor();
 private static final Handler MAIN=new Handler(Looper.getMainLooper());
 private static Repository.Catalog cached;
 private static long refreshed;
 public static void invalidate(){IO.execute(()->{cached=null;refreshed=0;});}
 public static void load(Context context,boolean force,Consumer<Repository.Catalog> callback){
  Context app=context.getApplicationContext();
  IO.execute(()->{
   boolean auto=androidx.preference.PreferenceManager.getDefaultSharedPreferences(app).getBoolean("pref_auto_refresh",true);
   if(cached==null||force||(auto&&System.currentTimeMillis()-refreshed>300000)){
    Repository repo=new Repository(app);
    cached=repo.load(force||auto);
    if(cached.channels.isEmpty()&&!force&&!auto)cached=repo.load(false);
    refreshed=System.currentTimeMillis();
   }
   Repository.Catalog result=cached;MAIN.post(()->callback.accept(result));
  });
 }
}

