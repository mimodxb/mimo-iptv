package tv.mimo.app;

import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.text.*;
import java.util.*;

/** On-demand XMLTV now/next. Missing programme data is never replaced by invented listings. */
public final class Epg {
    public static final class Programme {
        public final long start, stop; public final String title;
        Programme(long start,long stop,String title){this.start=start;this.stop=stop;this.title=title;}
    }
    public static Map<String,List<Programme>> parse(String xml,Set<String> ids,long now) throws Exception {
        if(xml.contains("<!DOCTYPE") || xml.contains("<!ENTITY")) throw new IOException("Unsupported XML declarations.");
        XmlPullParser parser=Xml.newPullParser();parser.setInput(new StringReader(xml));
        Map<String,List<Programme>> out=new HashMap<>();
        String id=null,title="";long start=0,stop=0;boolean wanted=false;int programmes=0;
        int event=parser.getEventType();
        while(event!=XmlPullParser.END_DOCUMENT) {
            if(event==XmlPullParser.START_TAG && parser.getName().equals("programme")) {
                if(++programmes>500000) throw new IOException("Guide is too large for this preview.");
                id=parser.getAttributeValue(null,"channel");start=time(parser.getAttributeValue(null,"start"));stop=time(parser.getAttributeValue(null,"stop"));title="";
                wanted=ids.contains(id)&&stop>now&&start<now+86400000L;
            } else if(event==XmlPullParser.START_TAG && parser.getName().equals("title") && wanted && title.isEmpty()) title=parser.nextText();
            else if(event==XmlPullParser.END_TAG && parser.getName().equals("programme") && wanted) {
                List<Programme> list=out.get(id);if(list==null){list=new ArrayList<>();out.put(id,list);}
                if(list.size()<100) list.add(new Programme(start,stop,title.isEmpty()?"Untitled programme":title));
                wanted=false;
            }
            event=parser.next();
        }
        for(List<Programme> list:out.values()) list.sort(Comparator.comparingLong(p->p.start));
        return out;
    }
    static long time(String raw) {
        if(raw==null)return 0;
        try {SimpleDateFormat fmt=new SimpleDateFormat(raw.trim().contains(" ")?"yyyyMMddHHmmss Z":"yyyyMMddHHmmss",Locale.ROOT);
            fmt.setLenient(false);fmt.setTimeZone(TimeZone.getTimeZone("UTC"));Date d=fmt.parse(raw.trim());return d==null?0:d.getTime();}
        catch(ParseException e){return 0;}
    }
}
