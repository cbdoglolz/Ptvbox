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
5. Create an annotated tag exactly matching `vVERSION`, for example `v1.0.0`, and push the tag.
6. GitHub Actions runs `scripts/Build-GitHub.sh` and creates a draft Release.

The Release title and body contain only `Ptvbox VERSION`. Attachments contain only:

- `Ptvbox-VERSION.apk`
- `Ptvbox-VERSION-ARM.apk`

Build logs, test reports, screenshots, personal subscriptions, credentials, signing keys and databases must never be Release assets.

Releases remain drafts under the owner's account. The owner can download the APK assets before publishing.
