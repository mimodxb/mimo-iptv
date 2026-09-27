package tv.mimo.app.mobile;
import android.content.*;
import java.util.*;
import tv.mimo.app.Channel;
public final class MobileNavigation {
 public static void openLive(Context context,String category){
  while(context instanceof android.content.ContextWrapper){
   if(context instanceof MobileMainActivity){((MobileMainActivity)context).openLive(category,"");return;}
   context=((android.content.ContextWrapper)context).getBaseContext();
  }
 }
 public static void play(Context context,Channel channel,List<Channel> channels){
  int index=channels.indexOf(channel),start=Math.max(0,index-100);
  ArrayList<String> keys=new ArrayList<>();
  for(int i=start;i<Math.min(channels.size(),index+101);i++)keys.add(channels.get(i).key);
  Intent intent=new Intent(context,MobilePlaybackActivity.class);
  intent.putExtra("key",channel.key).putExtra("name",channel.name).putExtra("idx",index-start);
  intent.putStringArrayListExtra("chan_keys",keys);context.startActivity(intent);
 }
}

