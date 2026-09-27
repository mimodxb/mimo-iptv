package tv.mimo.app.mobile;
import android.content.Context;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import tv.mimo.app.*;
/** Shared persisted sources; blank EPG is valid for playlists carrying their own guide URL. */
final class SourceEditor {
 static void show(Context c){
  Repository repo=new Repository(c);List<Source> sources=repo.sources();
  String[] names=new String[sources.size()];
  for(int i=0;i<names.length;i++)names[i]=(sources.get(i).enabled?"✓ ":"○ ")+sources.get(i).name;
  new AlertDialog.Builder(c).setTitle(R.string.settings_playlists).setItems(names,(d,i)->edit(c,sources,i))
   .setPositiveButton(R.string.add_source,(d,w)->edit(c,sources,-1)).setNegativeButton(android.R.string.cancel,null).show();
 }
 private static void edit(Context c,List<Source> sources,int index){
  Source existing=index<0?null:sources.get(index);
  LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);int pad=ChannelLogoLoader.dp(c,20);box.setPadding(pad,pad,pad,pad);
  EditText name=field(c,box,R.string.source_name,existing==null?"":existing.name);
  EditText url=field(c,box,R.string.playlist_url,existing==null?"":existing.url);
  EditText epg=field(c,box,R.string.epg_url,existing==null?"":existing.epg);
  url.setInputType(17);epg.setInputType(17);
  CheckBox enabled=new CheckBox(c);enabled.setText(R.string.source_enabled);enabled.setChecked(existing==null||existing.enabled);box.addView(enabled);
  AlertDialog dialog=new AlertDialog.Builder(c).setTitle(R.string.settings_playlists).setView(box)
   .setPositiveButton(R.string.save,null).setNegativeButton(android.R.string.cancel,null)
   .setNeutralButton(existing==null?android.R.string.cancel:R.string.remove_source,(d,w)->{if(index>=0){sources.remove(index);new Repository(c).saveSources(sources);MobileCatalog.invalidate();}}).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   String n=name.getText().toString().trim(),u=url.getText().toString().trim(),e=epg.getText().toString().trim();
   if(n.isEmpty()){name.setError(c.getString(R.string.source_name));return;}
   if(!M3uParser.isHttp(u)){url.setError(c.getString(R.string.url_invalid));return;}
   if(!e.isEmpty()&&!M3uParser.isHttp(e)){epg.setError(c.getString(R.string.url_invalid));return;}
   Source source=new Source(existing==null?UUID.randomUUID().toString():existing.id,n,u,e,enabled.isChecked());
   if(index<0)sources.add(source);else sources.set(index,source);
   new Repository(c).saveSources(sources);MobileCatalog.invalidate();dialog.dismiss();
  }));dialog.show();
 }
 private static EditText field(Context c,LinearLayout box,int hint,String value){EditText edit=new EditText(c);edit.setHint(hint);edit.setText(value);edit.setSingleLine(true);box.addView(edit);return edit;}
}

