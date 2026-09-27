import seedLogos from "./logos.json" with { type: "json" };
let logoIndex:Record<string,string>={...seedLogos};
let logosUpdated=0;
let logosPending:Promise<void>|null=null;
async function loadLogos(){
 if(Date.now()-logosUpdated<6*3600000)return;
 if(logosPending)return logosPending;
 logosPending=(async()=>{
  try{
   const r=await fetch('https://iptv-org.github.io/api/logos.json',{signal:AbortSignal.timeout(8000)});
   if(!r.ok)return;
   const bytes=await r.text();if(bytes.length>10*1024*1024)return;
   const rows=JSON.parse(bytes);const next:Record<string,string>={};
   for(const row of rows)if(row.in_use&&row.channel&&!bad(row.url)&&['PNG','JPEG','JPG','WEBP','GIF'].includes(row.format))next[row.channel.toLowerCase()]=row.url;
   logoIndex={...next,...seedLogos};logosUpdated=Date.now();
  }catch{}finally{logosPending=null;}
 })();return logosPending;
}
const SOURCE = "https://dearbulut.github.io/iptv/playlists/online.m3u";
const EPG = "https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-epg-merged";
const VERSION = "9";

const BLOCKED = new Set([
  "youtube.com","www.youtube.com","youtu.be","m.youtube.com","twitch.tv","www.twitch.tv",
  "dailymotion.com","www.dailymotion.com","facebook.com","www.facebook.com","fb.watch",
  "vk.com","www.vk.com","ok.ru","www.ok.ru","rutube.ru","www.rutube.ru"
]);
const ATTR_RE = /([\w-]+)="([^"]*)"/g;

type Entry = {
  ext: string; opts: string[]; url: string; attrs: Record<string,string>;
  name: string; country: string; origin: string; validation: string;
};
type Probe = { url:string; ok:boolean; status:number|null; reason:string; final_url?:string };

function attrsFrom(ext:string){
  const o:Record<string,string> = {};
  for(const m of ext.matchAll(ATTR_RE)) o[m[1]] = m[2];
  return o;
}
function parse(text:string){
  const out:Entry[]=[]; let ext:string|null=null, opts:string[]=[];
  for(const raw of text.replace(/\r\n?/g,"\n").split("\n")){
    const line=raw.trim(); if(!line) continue;
    if(line.startsWith("#EXTINF")){ ext=line; opts=[]; continue; }
    if(ext && line.startsWith("#")){ opts.push(line); continue; }
    if(ext && !line.startsWith("#")){
      const a=attrsFrom(ext);
      const name=ext.includes(",") ? ext.slice(ext.lastIndexOf(",")+1).trim() : (a["tvg-name"]||"");
      out.push({ext,opts:[...opts],url:line,attrs:a,name,country:a["tvg-country"]||"",origin:"health-source",validation:"upstream-health-pass"});
      ext=null; opts=[];
    }
  }
  return out;
}
function bad(url:string){
  try { const u=new URL(url); return !["http:","https:"].includes(u.protocol) || BLOCKED.has(u.hostname.toLowerCase()); }
  catch { return true; }
}
function esc(s:string){ return String(s).replaceAll('"',''); }
function manual(name:string,id:string,url:string,validation:string):Entry{
  return {
    ext:`#EXTINF:-1 tvg-id="${esc(id)}" tvg-name="${esc(name)}" tvg-country="AZ" group-title="Azerbaijan",${name}`,
    opts:[], url, attrs:{"tvg-id":id,"tvg-name":name,"tvg-country":"AZ","group-title":"Azerbaijan"},
    name,country:"AZ",origin:"azerbaijan-repair",validation
  };
}
function norm(s:string){
  return s.toLocaleLowerCase("az").normalize("NFKD").replace(/[\u0300-\u036f]/g,"").replace(/[^a-z0-9]+/g,"");
}
function entryKey(e:Entry){
  const id=(e.attrs["tvg-id"]||"").trim().toLowerCase();
  return id ? `id:${id}` : `name:${norm(e.name)}`;
}
function cleanSource(e:Entry){
  const a=e.attrs, cc=e.country;
  const grp=cc==="AZ" ? "Azerbaijan" : (a["group-title"]||cc||"International");
  const nm=a["tvg-name"]||e.name||"Channel"; const id=a["tvg-id"]||"";
  const bits=["#EXTINF:-1"]; if(id) bits.push(`tvg-id="${esc(id)}"`);
  bits.push(`tvg-name="${esc(nm)}"`); if(cc) bits.push(`tvg-country="${esc(cc)}"`);
  const logo=a["tvg-logo"] || (logoIndex as Record<string,string>)[id.toLowerCase()] || "";
  if(logo && !bad(logo)) bits.push(`tvg-logo="${esc(logo)}"`);
  bits.push(`group-title="${esc(grp)}"`);
  return {...e,ext:bits.join(" ")+","+nm,opts:e.opts.filter(o=>o.startsWith("#EXTVLCOPT")||o.startsWith("#KODIPROP")),name:nm};
}

const STATIC_REPAIRS:Entry[] = [
  manual("ATV Azerbaijan","ATV.az","https://lives.atv.az:5443/ATV_TV_STREAM/streams/atvcanli.m3u8","user-confirmed-working"),
  manual("İctimai TV","IctimaiTV.az","https://live.itv.az/itv.m3u8","user-confirmed-working"),
  manual("Real TV","RealTV.az","https://str.yodacdn.net/real/playlist.m3u8","user-confirmed-working"),
  manual("İdman TV","IdmanTV.az","https://live.itv.az/idman.m3u8","tcl-media3-verified"),
  manual("ARB 24","ARB24.az","http://erlyvideo.izone.az:80/arb24/mono.m3u8","tcl-media3-verified"),
  {...manual("CBC","CBC.az","https://stream.castr.com/6994359f4093355bcd876a4c/live_dfbe52f00be311f1952faf8c24dd1b5c/index.m3u8","official-player-media-probed"),
    opts:["#EXTVLCOPT:http-referrer=https://player.castr.com/","#EXTVLCOPT:http-user-agent=Mozilla/5.0"]},
  manual("CBC Sport","CBCSport.az","https://cbcsports-live.lg.mncdn.com/cbcsports_live/cbcsports/playlist.m3u8","official-player-media-probed"),
  manual("Baku TV","BakuTV.az","https://rtmp.baku.tv/hls/bakutv.m3u8","current-public-direct-stream"),
  manual("AzTV","AzTV.az","https://str.yodacdn.net/azertv/index.m3u8","current-public-direct-stream"),
  manual("Mədəniyyət TV","MedeniyyetTV.az","https://str.yodacdn.net/medeniyyettele/index.m3u8","current-public-direct-stream")
];

const PRIORITY = [
  {
    name:"Xəzər TV", id:"XezerTV.az", aliases:["xezer tv","xəzər tv","xezer xeber","xəzər xəbər"],
    candidates:[
      "https://www.xezerxeber.az/stream/index.m3u8",
      "https://xezerxeber.az/stream/main_stream.m3u8"
    ]
  },
  {
    name:"Space TV", id:"SpaceTV.az", aliases:["space tv","space"],
    candidates:[
      "http://213.239.195.222/azerbaijan/space_stream_sd_2023/playlist.m3u8",
      "https://streams.livetv.az/azerbaijan/space_stream/playlist.m3u8"
    ]
  }
];

async function probeHls(url:string):Promise<Probe>{
  const ctl=new AbortController(); const timer=setTimeout(()=>ctl.abort(),6500);
  try{
    const r=await fetch(url,{signal:ctl.signal,redirect:"follow",headers:{
      "user-agent":"Mozilla/5.0 (Android TV) Mimo-IPTV/9.0",
      "accept":"application/vnd.apple.mpegurl,application/x-mpegURL,text/plain,*/*"
    }});
    if(!r.ok) return {url,ok:false,status:r.status,reason:`HTTP ${r.status}`,final_url:r.url};
    const text=await r.text();
    let hls=text.trimStart().startsWith("#EXTM3U");
    let manifest=text, base=r.url, mediaChecked=false;
    for(let depth=0;hls&&depth<3;depth++){
      const next=manifest.split(/\r?\n/).map(x=>x.trim()).find(x=>x&&!x.startsWith("#"));
      if(!next){hls=false;break;}
      const target=new URL(next,base).href;
      if(bad(target)){hls=false;break;}
      const child=await fetch(target,{signal:ctl.signal,headers:{"range":"bytes=0-1023","user-agent":"Mozilla/5.0"}});
      if(!child.ok){hls=false;await child.body?.cancel();break;}
      if(manifest.includes("#EXT-X-STREAM-INF")){manifest=await child.text();base=child.url;hls=manifest.trimStart().startsWith("#EXTM3U");}
      else {const reader=child.body?.getReader();const first=await reader?.read();await reader?.cancel();hls=!!first?.value?.length;mediaChecked=hls;break;}
    }
    hls=hls&&mediaChecked;
    return {url,ok:hls,status:r.status,reason:hls?"manifest-and-media-bytes":"manifest-or-media-unavailable",final_url:r.url};
  }catch(e){
    return {url,ok:false,status:null,reason:e instanceof Error?e.message:String(e)};
  }finally{ clearTimeout(timer); }
}
async function resolvePriority(p:(typeof PRIORITY)[number]){
  const probes:Probe[]=[];
  for(const url of p.candidates){
    const result=await probeHls(url); probes.push(result);

  }
  const entries=probes.filter(r=>r.ok).map(r=>manual(p.name,p.id,r.final_url||r.url,"manifest-and-segment-probe-pass"));
  return {entry:entries[0]||null,entries,probes};
}
async function fetchSource(){
  const ctl=new AbortController(); const timer=setTimeout(()=>ctl.abort(),25000);
  try{
    const r=await fetch(SOURCE,{signal:ctl.signal,headers:{"user-agent":"Mozilla/5.0 Mimo-IPTV/8.0","accept":"text/plain,*/*"}});
    if(!r.ok) throw new Error(`Source HTTP ${r.status}`);
    const txt=await r.text(); if(!txt.trimStart().startsWith("#EXTM3U")) throw new Error("Source did not return M3U");
    return txt;
  }finally{ clearTimeout(timer); }
}
async function sha256(s:string){
  const d=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(s));
  return [...new Uint8Array(d)].map(b=>b.toString(16).padStart(2,"0")).join("");
}

Deno.serve(async(req:Request)=>{
  try{
    const [txt, ...resolved] = await Promise.all([fetchSource(), ...PRIORITY.map(resolvePriority)]);
    await loadLogos();
    const src=parse(txt as string).filter(e=>!bad(e.url)).map(cleanSource);
    const dynamic=resolved.flatMap((x:any)=>x.entries) as Entry[];
    const repairs=[...dynamic,...STATIC_REPAIRS];

    const overrideKeys=new Set(repairs.map(entryKey));
    const overrideNames=new Set<string>();
    for(const p of PRIORITY){
      if(dynamic.some(e=>e.attrs["tvg-id"]===p.id)) for(const n of p.aliases) overrideNames.add(norm(n));
    }
    for(const e of STATIC_REPAIRS) overrideNames.add(norm(e.name));

    const selected:Entry[]=[...repairs];
    for(const e of src){
      if(e.country==="AZ" && (overrideKeys.has(entryKey(e)) || overrideNames.has(norm(e.name)))) continue;
      selected.push(e);
    }

    const final:Entry[]=[]; const seenUrls=new Set<string>(), seenAzKeys=new Set<string>();
    for(const e of selected){
      const u=e.url.trim(); if(bad(u)||seenUrls.has(u)) continue;
      if(e.country==="AZ" && !PRIORITY.some(p=>p.id===e.attrs["tvg-id"])){
        const k=entryKey(e); if(seenAzKeys.has(k)) continue; seenAzKeys.add(k);
      }
      seenUrls.add(u); final.push(e);
    }

    const lines=[`#EXTM3U x-tvg-url="${EPG}" url-tvg="${EPG}"`];
    for(const e of final) lines.push(cleanSource(e).ext,...e.opts,e.url);
    const body=lines.join("\n")+"\n"; const hash=await sha256(body);
    const az=final.filter(e=>e.country==="AZ");
    const sourceCount=final.filter(e=>e.origin==="health-source").length;
    const priorityChecks=PRIORITY.map((p,i)=>({
      name:p.name,id:p.id,selected_url:(resolved[i] as any).entry?.url||null,
      working:Boolean((resolved[i] as any).entry),probes:(resolved[i] as any).probes
    }));
    const priorityPass=priorityChecks.every(x=>x.working);
    const pass=final.length>=3000 && seenUrls.size===final.length && final.every(e=>!bad(e.url));
    const url=new URL(req.url);
    if(url.searchParams.get("check")==="1") return Response.json({
      status:pass&&priorityPass?"PASS":"DEGRADED", version:VERSION, channels:final.length,
      health_source_entries:sourceCount, azerbaijan_entries:az.length,
      priority_channels:priorityChecks,
      azerbaijan:az.map(e=>({name:e.name,url:e.url,validation:e.validation,tvg_id:e.attrs["tvg-id"]||""})),
      logos_with_metadata:final.filter(e=>attrsFrom(cleanSource(e).ext)["tvg-logo"]).length,
      epg_url:EPG, sha256:hash
    },{headers:{"cache-control":"no-store"}});
    if(!pass) return Response.json({status:"FAIL",version:VERSION,channels:final.length},{status:502});
    return new Response(body,{status:200,headers:{
      "content-type":"application/x-mpegURL; charset=utf-8","cache-control":"no-cache, max-age=300",
      "x-mimo-channels":String(final.length),"x-mimo-version":VERSION,"x-mimo-priority":priorityPass?"pass":"degraded","x-mimo-sha256":hash
    }});
  }catch(e){
    return new Response(`Playlist build error: ${e instanceof Error?e.message:String(e)}`,{status:502});
  }
});
export {cleanSource, parse, probeHls, resolvePriority};
