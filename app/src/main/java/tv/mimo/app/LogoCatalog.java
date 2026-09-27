package tv.mimo.app;
import android.content.Context;
import java.io.*;
import java.util.*;
import org.json.JSONObject;
/** Exact channel-ID lookups only; never borrow a similarly named broadcaster's logo. */
public final class LogoCatalog {
 private static JSONObject index;
 public static synchronized void fill(Context context,List<Channel> channels){
  try{
   if(index==null)try(InputStream in=context.getAssets().open("channel-logos.json")){index=new JSONObject(Repository.readBounded(in,8*1024*1024));}
   for(Channel c:channels)if(c.logo==null||c.logo.isEmpty()||c.streams.stream().anyMatch(s->s.sourceId.equals("mimo")))
       c.logo=index.optString(c.tvgId.toLowerCase(Locale.ROOT),c.logo==null?"":c.logo);
  }catch(Exception ignored){}
 }
}
