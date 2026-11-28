# GitHub release process

## Decimal version rule

Ptvbox version names contain exactly three dot-separated decimal digits:

```text
MAJOR.MINOR.PATCH
```

Each component is a single digit from `0` through `9`. Carry to the next component instead of writing a two-digit component.

```text
0.9.1 -> 0.9.2
0.9.9 -> 1.0.0
1.0.9 -> 1.1.0
1.9.9 -> 2.0.0
```

`0.10.0`, `0.5.11`, `1.10.0` and similar two-digit component names are invalid. Android's internal `versionCode` remains a separate monotonically increasing integer.

## Required release steps

1. Update `versionName` and increment `versionCode` in `app/build.gradle.kts`.
2. Update the report, requirement checklist and `PROJECT-CONTINUITY.md`.
3. Run the JVM tests, build universal and ARM APKs, verify the original signing certificate, and audit the package contents.
4. Commit the source and push `main`.
5. Create an annotated tag exactly matching `vVERSION`, for example `v1.0.1`, and push the tag.
6. GitHub Actions runs `scripts/Build-GitHub.sh` and publishes a public Release so in-app update checks work directly.

The Release title and body contain only `Ptvbox VERSION`. Attachments contain only:

- `Ptvbox-VERSION.apk`
- `Ptvbox-VERSION-ARM.apk`

Build logs, test reports, screenshots, personal subscriptions, credentials, signing keys and databases must never be Release assets.

## Pre-release candidate iterations (`-c1`, `-c2`, ...)

- Unreleased local test builds use `X.Y.Z-c1`, `X.Y.Z-c2`, etc. (increment `versionCode` for each delivered candidate and the formal release), built into a single reused `artifacts/candidate-vX.Y.Z/` directory where each new candidate replaces the previous candidate APKs to save disk space.
- All iteration history stays recorded in `PROJECT-CONTINUITY.md` and `PROGRESS-CHECKLIST.md`.
- Once the formal `X.Y.Z` release is published, remove `artifacts/candidate-vX.Y.Z/` after verifying the public release and both downloaded APKs.
