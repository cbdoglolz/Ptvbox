"""Build a compact, title-independent mapping snapshot from Fribb/Anime-Lists factual IDs.

No runtime credentials are used. Pin each refresh to a reviewed upstream commit.
"""
import argparse
import hashlib
import json
from pathlib import Path
from urllib.request import urlopen

REVISION = "1a80d77d3d36aff50a3c559d6e78715d4c3b410b"
parser = argparse.ArgumentParser()
parser.add_argument("--input", type=Path)
args = parser.parse_args()
url = f"https://raw.githubusercontent.com/Fribb/anime-lists/{REVISION}/anime-list-full.json"
raw = args.input.read_bytes() if args.input else urlopen(url, timeout=45).read(16_000_000)
rows = []
for item in json.loads(raw):
    if item.get("type") not in ("TV", "ONA") or not isinstance(item.get("anilist_id"), int):
        continue
    tmdb = item.get("themoviedb_id", {}).get("tv", 0)
    season = item.get("season", {}).get("tmdb", 0)
    offset = item.get("episode_offset", {}).get("tmdb", 0)
    logical = item.get("season", {}).get("tvdb", 0)
    if isinstance(tmdb, int) and isinstance(season, int) and isinstance(offset, int) and season > 0 and offset >= 0:
        rows.append([item["anilist_id"], tmdb, season, offset, logical if isinstance(logical, int) else 0])
rows.sort()
result = {"source": "Fribb/anime-lists + Anime-Lists/anime-lists", "revision": REVISION,
          "url": url, "sha256": hashlib.sha256(raw).hexdigest(),
          "columns": ["anilist", "tmdb", "tmdbSeason", "episodeOffset", "tvdbSeason"], "rows": rows}
dest = Path(__file__).resolve().parents[1] / "app/src/main/assets/anime-mappings103.json"
dest.write_text(json.dumps(result, separators=(",", ":")) + "\n", encoding="utf-8")
print(f"Mapped {len(rows)} anime entries; {sum(r[3] > 0 for r in rows)} with episode offsets")
