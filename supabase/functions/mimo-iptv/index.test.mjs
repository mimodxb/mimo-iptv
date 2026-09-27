import {test} from 'node:test';
import assert from 'node:assert/strict';
globalThis.Deno={serve(){}};
const {cleanSource,parse,probeHls,resolvePriority}=await import('./index.ts');
test('preserves upstream logos and playback headers',()=>{
 const [entry]=parse('#EXTM3U\n#EXTINF:-1 tvg-id="Test.us" tvg-logo="https://example.org/wide.png" tvg-country="US",Test\n#EXTVLCOPT:http-referrer=https://example.org/\nhttps://example.org/live.m3u8');
 const clean=cleanSource(entry);
 assert.match(clean.ext,/tvg-logo="https:\/\/example.org\/wide.png"/);
 assert.equal(clean.opts.length,1);
});
test('enriches exact known IDs but never guesses unknown channel identities',()=>{
 const known=cleanSource(parse('#EXTINF:-1 tvg-id="SpaceTV.az",Space\nhttps://example.org/live')[0]);
 assert.match(known.ext,/tvg-logo=/);
 const unknown=cleanSource(parse('#EXTINF:-1 tvg-id="Missing.zz",Unknown\nhttps://example.org/live')[0]);
 assert.doesNotMatch(unknown.ext,/tvg-logo=/);
});
test('manifest success alone does not count as healthy and all good candidates survive',async()=>{
 const original=globalThis.fetch;
 globalThis.fetch=async input=>{
  const url=String(input);
  let response=url.includes('broken.ts')?new Response('down',{status:503}):
   new Response(url.endsWith('.m3u8')?'#EXTM3U\n#EXTINF:6,\n'+(url.includes('bad')?'broken.ts':'segment.ts'):'media-bytes');
  Object.defineProperty(response,'url',{value:url});return response;
 };
 try{
  assert.equal((await probeHls('https://example.org/bad.m3u8')).ok,false);
  const result=await resolvePriority({name:'Test',id:'Test.az',aliases:[],candidates:['https://example.org/good.m3u8','https://example.org/other.m3u8']});
  assert.equal(result.entries.length,2);
 }finally{globalThis.fetch=original;}
});
