package tv.mimo.app;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.*;
import java.net.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;

/** Bounded, alpha-preserving FIT_CENTER loader shared by TV and phone. */
public final class ChannelLogoLoader {
 private static final Handler MAIN=new Handler(Looper.getMainLooper());
 private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(12*1024*1024){
  @Override protected int sizeOf(String k,Bitmap b){return b.getAllocationByteCount();}
 };
 private static final ThreadPoolExecutor IO=new ThreadPoolExecutor(3,3,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(96),new ThreadPoolExecutor.DiscardOldestPolicy());
 public static Bitmap placeholder(int w,int h){
  Bitmap b=Bitmap.createBitmap(Math.max(1,w),Math.max(1,h),Bitmap.Config.ARGB_8888);
  Canvas c=new Canvas(b);c.drawColor(TvStyle.PANEL);Paint p=new Paint(3);p.setColor(TvStyle.MUTED);p.setTextSize(Math.min(w,h)*.4f);p.setTextAlign(Paint.Align.CENTER);c.drawText("▣",w/2f,h*.62f,p);return b;
 }
 public static int dp(Context c,float d){return Math.round(d*c.getResources().getDisplayMetrics().density);}
 public static Bitmap placeholder(Context c,int w,int h){return placeholder(dp(c,w),dp(c,h));}
 public static void load(ImageView v,String url,int w,int h,Context c){load(v,url,dp(c,w),dp(c,h));}
 /** This overload takes physical pixels, matching the existing TV callers. */
 public static void load(ImageView v,String url,int w,int h){
  int width=Math.max(1,Math.min(768,w)),height=Math.max(1,Math.min(768,h));
  Object token=new Object();v.setTag(token);v.setScaleType(ImageView.ScaleType.FIT_CENTER);v.setAdjustViewBounds(false);
  int pad=Math.max(1,Math.min(width,height)/16);v.setPadding(pad,pad,pad,pad);v.setImageBitmap(placeholder(width,height));
  if(url==null||!M3uParser.isHttp(url))return;
  String key=url+"#"+width+"x"+height;Bitmap cached=CACHE.get(key);if(cached!=null){v.setImageBitmap(cached);return;}
  WeakReference<ImageView> ref=new WeakReference<>(v);File dir=new File(v.getContext().getCacheDir(),"channel-logos");dir.mkdirs();
  IO.execute(()->{
   ImageView current=ref.get();if(current==null||current.getTag()!=token)return;
   Bitmap bitmap=CACHE.get(key);
   if(bitmap==null)try{
    File disk=new File(dir,java.util.UUID.nameUUIDFromBytes(url.getBytes(java.nio.charset.StandardCharsets.UTF_8))+".img");byte[] bytes;
    if(disk.exists()&&System.currentTimeMillis()-disk.lastModified()<7*86400000L){try(InputStream in=new FileInputStream(disk)){bytes=read(in);}}
    else{
     HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(7000);c.setReadTimeout(10000);c.setRequestProperty("User-Agent","MIMO-TV/1.0");
     try{if(c.getResponseCode()!=200)return;try(InputStream in=c.getInputStream()){bytes=read(in);}}finally{c.disconnect();}
     synchronized(ChannelLogoLoader.class){
      File[] files=dir.listFiles();long total=0;
      if(files!=null){for(File f:files)total+=f.length();java.util.Arrays.sort(files,java.util.Comparator.comparingLong(File::lastModified));for(File f:files){if(total<64*1024*1024)break;long size=f.length();if(f.delete())total-=size;}}
      try(FileOutputStream out=new FileOutputStream(disk)){out.write(bytes);}
     }
    }
    bitmap=decode(bytes,width,height);if(bitmap==null){disk.delete();return;}CACHE.put(key,bitmap);
   }catch(Exception ignored){return;}
   Bitmap result=bitmap;MAIN.post(()->{ImageView target=ref.get();if(target!=null&&target.getTag()==token)target.setImageBitmap(result);});
  });
 }
 private static byte[] read(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>4*1024*1024)throw new IOException("Logo too large");out.write(buf,0,n);}return out.toByteArray();}
 static Bitmap decode(byte[] bytes,int w,int h){
  String prefix=new String(bytes,0,Math.min(bytes.length,2048),java.nio.charset.StandardCharsets.UTF_8);
  if(prefix.contains("<svg"))try{
   String xml=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
   if(xml.contains("<!ENTITY")||xml.contains("<!DOCTYPE"))return null;
   com.caverock.androidsvg.SVG svg=com.caverock.androidsvg.SVG.getFromString(xml);
   svg.setDocumentPreserveAspectRatio(com.caverock.androidsvg.PreserveAspectRatio.LETTERBOX);
   svg.setDocumentWidth(w);svg.setDocumentHeight(h);
   Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);svg.renderToCanvas(new Canvas(bitmap));return bitmap;
  }catch(Exception ignored){return null;}
  BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
  if(o.outWidth<=0||o.outHeight<=0||o.outWidth>20000||o.outHeight>20000)return null;
  o.inSampleSize=1;while(o.outWidth/o.inSampleSize>w*2||o.outHeight/o.inSampleSize>h*2)o.inSampleSize*=2;
  o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.ARGB_8888;Bitmap raw=BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(raw==null)return null;
  float scale=Math.min((float)w/raw.getWidth(),(float)h/raw.getHeight());Bitmap result=Bitmap.createScaledBitmap(raw,Math.max(1,Math.round(raw.getWidth()*scale)),Math.max(1,Math.round(raw.getHeight()*scale)),true);if(result!=raw)raw.recycle();return result;
 }
 public static void cancel(){}
}
