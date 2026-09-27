package tv.mimo.app;
import android.graphics.*;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LogoRenderingTest {
 @Test public void wideAndTallLogosKeepAspectRatioAndAlpha(){
  for(int[] shape:new int[][]{{400,100},{100,400}}){
   Bitmap input=Bitmap.createBitmap(shape[0],shape[1],Bitmap.Config.ARGB_8888);
   input.eraseColor(Color.TRANSPARENT);ByteArrayOutputStream out=new ByteArrayOutputStream();input.compress(Bitmap.CompressFormat.PNG,100,out);
   Bitmap result=ChannelLogoLoader.decode(out.toByteArray(),120,120);
   assertNotNull(result);assertEquals((double)shape[0]/shape[1],(double)result.getWidth()/result.getHeight(),.05);
   assertTrue(result.hasAlpha());assertTrue(result.getWidth()<=120&&result.getHeight()<=120);
  }
 }
 @Test public void absentLogoInvalidatesAnEarlierBinding(){
  ImageView view=new ImageView(RuntimeEnvironment.getApplication());Object old=new Object();view.setTag(old);
  ChannelLogoLoader.load(view,"",80,80);assertNotSame(old,view.getTag());assertEquals(ImageView.ScaleType.FIT_CENTER,view.getScaleType());
 }
 @Test public void invalidImageDoesNotDecode(){assertNull(ChannelLogoLoader.decode("<html>Error</html>".getBytes(),100,100));}
 @Test public void svgFitsInsideSquareWithoutStretching(){
  Bitmap image=ChannelLogoLoader.decode("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 400 100'><rect width='400' height='100' fill='red'/></svg>".getBytes(),120,120);
  assertNotNull(image);assertEquals(0,Color.alpha(image.getPixel(60,0)));assertEquals(Color.RED,image.getPixel(60,60));
 }
}
