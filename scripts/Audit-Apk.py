"""Read-only local package audit. Never prints signing secrets or protected device addresses."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("directory", type=Path)
parser.add_argument("--output", type=Path, required=True)
parser.add_argument("--source-ref", help="Compare a CI APK with exact committed assets, avoiding checkout newline differences")
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
def source_bytes(path):
    if args.source_ref:
        return subprocess.check_output(["git", "show", f"{args.source_ref}:{path}"], cwd=root)
    return (root / path).read_bytes()
build = source_bytes("app/build.gradle.kts").decode("utf-8")
version = re.search(r'versionName = "([^"]+)"', build)[1]
code = re.search(r'versionCode = (\d+)', build)[1]
sdk = Path(os.environ["ANDROID_HOME"]) / "build-tools/37.0.0"
certificate = "33d71fe4cf85185651c1e254a2e62000cd7b348d5e50a45ff1b8dfa6656e1208"
protected_config = root / ".tools/device-constraints.json"
needles = [b"QaInstrumentation", b"FeaturesQa103", b"SearchQa103", b"QaAuth054"]
if protected_config.exists():
    needles += list(set(re.findall(rb"192\.168\.\d{1,3}\.\d{1,3}", protected_config.read_bytes())))
results = []
apks = sorted(args.directory.glob("*.apk"))
assert len(apks) == 2, "Expected exactly the signed universal and ARM candidates"
for apk in apks:
    badging = subprocess.check_output([str(sdk / "aapt.exe"), "dump", "badging", str(apk)], text=True, encoding="utf-8")
    assert f"versionCode='{code}'" in badging and f"versionName='{version}'" in badging
    assert "name='com.cbdog.ptvbox'" in badging
    signing = subprocess.check_output([str(sdk / "apksigner.bat"), "verify", "--print-certs", str(apk)], text=True, encoding="utf-8")
    assert certificate in signing.lower(), "Signing identity changed"
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        assert not any(n.lower().endswith((".jks", ".keystore", ".dpapi", ".db", ".sqlite", ".log")) for n in names)
        for name in names:
            data = archive.read(name)
            assert not any(needle in data for needle in needles), "Debug class or protected address found"
            assert not re.search(rb"-----BEGIN (?:RSA |EC )?PRIVATE KEY-----\s+[A-Za-z0-9+/=\r\n]{40,}", data)
        for asset in ["default-sites.json", "international-sites.json", "anime-mappings103.json"]:
            assert archive.read("assets/" + asset) == source_bytes("app/src/main/assets/" + asset)
        mapping = json.loads(archive.read("assets/anime-mappings103.json"))
        abis = sorted({n.split("/")[1] for n in names if n.startswith("lib/")})
        if apk.name.endswith("-ARM.apk"):
            assert abis == ["arm64-v8a", "armeabi-v7a"]
        else:
            assert "x86" in abis and "x86_64" in abis
    results.append({"file": apk.name, "versionName": version, "versionCode": int(code),
                    "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(), "bytes": apk.stat().st_size,
                    "certificateSha256": certificate, "abis": abis, "animeMappings": len(mapping["rows"]),
                    "noDebugOrPrivateMaterial": True, "assetsMatchSource": True,
                    "assetSourceReference": args.source_ref or "working-tree"})
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(results, indent=2) + "\n", encoding="utf-8")
print(json.dumps(results, ensure_ascii=True))
