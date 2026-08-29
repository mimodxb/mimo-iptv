#!/usr/bin/env python3
import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlsplit
from urllib.request import Request, urlopen

SOURCES = [
    ("Free-TV worldwide", "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"),
    ("iptv-org Europe", "https://iptv-org.github.io/iptv/regions/eur.m3u"),
    ("iptv-org CIS", "https://iptv-org.github.io/iptv/regions/cis.m3u"),
    ("iptv-org Middle East", "https://iptv-org.github.io/iptv/regions/mideast.m3u"),
    ("iptv-org Azerbaijan", "https://iptv-org.github.io/iptv/countries/az.m3u"),
    ("iptv-org Russia", "https://iptv-org.github.io/iptv/countries/ru.m3u"),
    ("iptv-org Turkey", "https://iptv-org.github.io/iptv/countries/tr.m3u"),
    ("iptv-org Movies", "https://iptv-org.github.io/iptv/categories/movies.m3u"),
]

# These entries are deliberately first. They are direct media/playlist URLs selected
# for TiviMate compatibility instead of YouTube/VK/other webpage URLs.
AZ_PRIORITY = [
    ("AzTV", "AzTV.az", "https://str.yodacdn.net/azertv/index.m3u8"),
    ("ATV Azerbaijan", "AzadTV.az", "https://lives.atv.az:5443/ATV_TV_STREAM/streams/atvcanli.m3u8"),
    ("ARB", "ARB.az", "https://raw.githubusercontent.com/UzunMuhalefet/streams/main/myvideo-az/arb.m3u8"),
    ("ARB 24", "ARB24.az", "https://raw.githubusercontent.com/UzunMuhalefet/streams/main/myvideo-az/arb-24.m3u8"),
    ("Baku TV", "BakuTV.az", "https://rtmp.baku.tv/hls/bakutv.m3u8"),
    ("CBC", "CBC.az", "https://stream.cbctv.az:5443/LiveApp/streams/cbctv.m3u8"),
    ("CBC Sport", "CBCSport.az", "https://mn-nl.mncdn.com/cbcsports_live/cbcsports/playlist.m3u8"),
    ("Dünya TV", "DunyaTV.az", "https://raw.githubusercontent.com/UzunMuhalefet/streams/refs/heads/main/myvideo-az/dunya-tv.m3u8"),
    ("İctimai TV", "IctimaiTV.az", "https://live.itv.az/itv.m3u8"),
    ("İdman TV", "IdmanTV.az", "https://str.yodacdn.net/idman/index.m3u8"),
    ("Kanal S", "KanalS.az", "https://lives.atv.az:5443/KANAL-S/streams/kanals.m3u8"),
    ("Mədəniyyət TV", "MedeniyyetTV.az", "https://str.yodacdn.net/medeniyyettele/index.m3u8"),
    ("MTV Azerbaijan", "MTVAzerbaijan.az", "https://raw.githubusercontent.com/UzunMuhalefet/streams/refs/heads/main/myvideo-az/mtv-azerbaycan.m3u8"),
    ("Real TV", "RealTV.az", "https://tv.mobyservice.ru/Real/tracks-v1a1/mono.m3u8"),
    ("Space TV", "SpaceTV.az", "https://raw.githubusercontent.com/UzunMuhalefet/streams/main/myvideo-az/space-tv.m3u8"),
    ("Xəzər TV", "XezerTV.az", "https://raw.githubusercontent.com/UzunMuhalefet/streams/main/myvideo-az/xezer-tv.m3u8"),
]

BLOCKED_PAGE_HOSTS = {
    "youtube.com", "www.youtube.com", "youtu.be", "m.youtube.com",
    "twitch.tv", "www.twitch.tv", "dailymotion.com", "www.dailymotion.com",
    "facebook.com", "www.facebook.com", "fb.watch", "vk.com", "www.vk.com",
    "ok.ru", "www.ok.ru", "rutube.ru", "www.rutube.ru",
}

MIN_CHANNELS = 3000
MANDATORY_AZ = {
    "AzTV", "ATV Azerbaijan", "ARB", "ARB 24", "Baku TV", "CBC Sport",
    "İctimai TV", "İdman TV", "Kanal S", "Mədəniyyət TV",
    "MTV Azerbaijan", "Real TV", "Space TV", "Xəzər TV",
}


def fetch_text(url: str) -> str:
    req = Request(url, headers={
        "User-Agent": "Mozilla/5.0 Mimo-IPTV-Builder/1.0",
        "Accept": "*/*",
    })
    with urlopen(req, timeout=60) as response:
        data = response.read()
    return data.decode("utf-8-sig", errors="replace")


def parse_entries(text: str):
    entries = []
    pending = None
    for raw in text.replace("\r\n", "\n").replace("\r", "\n").split("\n"):
        line = raw.strip()
        if not line:
            continue
        if line.startswith("#EXTINF"):
            pending = [line]
            continue
        if pending is not None and line.startswith("#"):
            pending.append(line)
            continue
        if pending is not None and not line.startswith("#"):
            entries.append((pending, line))
            pending = None
    return entries


def blocked_page(url: str) -> bool:
    try:
        host = (urlsplit(url).hostname or "").lower()
    except Exception:
        return True
    return host in BLOCKED_PAGE_HOSTS


def normalize_url(url: str) -> str:
    return url.strip()


def priority_entry(name: str, tvg_id: str, url: str):
    extinf = (
        f'#EXTINF:-1 tvg-id="{tvg_id}" tvg-name="{name}" '
        f'tvg-country="AZ" group-title="Azerbaijan",{name}'
    )
    return ([extinf], url)


def channel_name(meta_lines):
    extinf = meta_lines[0]
    return extinf.rsplit(",", 1)[-1].strip() if "," in extinf else ""


def build():
    output_entries = []
    seen_urls = set()
    blocked = 0
    duplicate_urls = 0
    source_report = []

    # Guaranteed Azerbaijan priority block.
    for name, tvg_id, url in AZ_PRIORITY:
        key = normalize_url(url)
        if key not in seen_urls:
            output_entries.append(priority_entry(name, tvg_id, url))
            seen_urls.add(key)

    successful_sources = 0
    for source_name, source_url in SOURCES:
        try:
            text = fetch_text(source_url)
            parsed = parse_entries(text)
            kept = 0
            source_blocked = 0
            source_dupes = 0
            for meta, url in parsed:
                if blocked_page(url):
                    blocked += 1
                    source_blocked += 1
                    continue
                key = normalize_url(url)
                if key in seen_urls:
                    duplicate_urls += 1
                    source_dupes += 1
                    continue
                seen_urls.add(key)
                output_entries.append((meta, url))
                kept += 1
            source_report.append({
                "source": source_name,
                "url": source_url,
                "status": "ok",
                "parsed": len(parsed),
                "kept": kept,
                "blocked_webpages": source_blocked,
                "duplicate_urls": source_dupes,
            })
            successful_sources += 1
        except Exception as exc:
            source_report.append({
                "source": source_name,
                "url": source_url,
                "status": "error",
                "error": f"{type(exc).__name__}: {exc}",
            })

    priority_names = {channel_name(meta) for meta, _ in output_entries[:len(AZ_PRIORITY)]}
    missing_az = sorted(MANDATORY_AZ - priority_names)

    report = {
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "status": "PASS",
        "successful_sources": successful_sources,
        "configured_sources": len(SOURCES),
        "unique_channels": len(output_entries),
        "blocked_webpage_entries": blocked,
        "duplicate_stream_urls_removed": duplicate_urls,
        "mandatory_azerbaijan_missing": missing_az,
        "sources": source_report,
    }

    failures = []
    if successful_sources < 4:
        failures.append(f"only {successful_sources} of {len(SOURCES)} sources fetched successfully")
    if len(output_entries) < MIN_CHANNELS:
        failures.append(f"only {len(output_entries)} unique entries; minimum is {MIN_CHANNELS}")
    if missing_az:
        failures.append("missing mandatory Azerbaijan channels: " + ", ".join(missing_az))
    if any(blocked_page(url) for _, url in output_entries):
        failures.append("known webpage URL survived compatibility filter")

    if failures:
        report["status"] = "FAIL"
        report["failures"] = failures
        Path("build-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(report, ensure_ascii=False, indent=2))
        return 1

    lines = ["#EXTM3U"]
    for meta, url in output_entries:
        lines.extend(meta)
        lines.append(url)
    Path("playlist.m3u").write_text("\n".join(lines) + "\n", encoding="utf-8")
    Path("build-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(build())
