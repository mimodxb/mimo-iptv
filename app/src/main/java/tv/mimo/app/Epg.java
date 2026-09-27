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
        return parse(new StringReader(xml),ids,now);
    }
    /** Stream the large merged guide instead of keeping its entire XML in the Android heap. */
    public static Map<String,List<Programme>> fetch(String url,Set<String> ids,long now)throws Exception{
        if(!M3uParser.isHttp(url))throw new IOException("Invalid guide URL");
        java.net.HttpURLConnection connection=(java.net.HttpURLConnection)new java.net.URL(url).openConnection();
        connection.setConnectTimeout(12000);connection.setReadTimeout(25000);
        connection.setRequestProperty("Accept-Encoding","gzip");
        try{
            if(connection.getResponseCode()!=200)throw new IOException("Guide unavailable");
            try(PushbackInputStream peek=new PushbackInputStream(connection.getInputStream(),2)){
                byte[] prefix=new byte[2];int n=peek.read(prefix);if(n>0)peek.unread(prefix,0,n);
                InputStream stream=n==2&&(prefix[0]&255)==31&&(prefix[1]&255)==139?new java.util.zip.GZIPInputStream(peek):peek;
                Reader reader=new FilterReader(new InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)){
                    long total;
                    @Override public int read(char[] b,int off,int len)throws IOException{int count=super.read(b,off,len);if(count>0&&(total+=count)>64*1024*1024)throw new IOException("Guide too large");return count;}
                };
                return parse(reader,ids,now);
            }
        }finally{connection.disconnect();}
    }
    static Map<String,List<Programme>> parse(Reader reader,Set<String> ids,long now) throws Exception {
        XmlPullParser parser=Xml.newPullParser();parser.setInput(reader);
        Map<String,List<Programme>> out=new HashMap<>();
        String id=null,title="";long start=0,stop=0;boolean wanted=false;int programmes=0;
        int event=parser.getEventType();
        while(event!=XmlPullParser.END_DOCUMENT) {
            if(event==XmlPullParser.DOCDECL)throw new IOException("Unsupported XML declarations.");
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
            event=parser.nextToken();
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
