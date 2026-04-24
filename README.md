# Trinity-AV (Rootless, Zero-Trust Skeleton)

Trinity-AV is a multi-module Android antivirus project with a Kotlin/Compose frontend and a native C++ scanning engine bridge.

## Implemented capabilities

- Rootless app/service/component management architecture with Shizuku integration points.
- Full-file scan pipeline with coroutine pause/resume on every detection.
- Archive cracking for ZIP/APK/JAR using `ZipInputStream` and nested-entry scanning.
- Detection categories for malware, ransomware, RAT, and suspicious archives.
- Controlled folder access policy seeds for `/sdcard/Documents`, `/sdcard/DCIM`, `/sdcard/Download`.
- Whitelist package storage and excluded path storage through DataStore.
- Native-backed first-run sandbox flow for new apps (3-minute WIP observation window + post-analysis verdict).
- Quarantine encryption with AES before moving files into app-private storage.
- JNI scanner that loads signature strings from `signatures.db` and scans files/byte streams.

## Modules

- `app`: UI, ViewModels, DI, runtime services.
- `domain`: models, repository contracts, use-cases.
- `data`: repository implementations, Shizuku helpers, Room/DataStore, JNI bridge, native code.

## Build prerequisites

1. Android Studio (latest stable)
2. Android SDK 35
3. NDK + CMake installed from SDK Manager

## Build command

```bash
./gradlew :app:assembleDebug
```

## Signature database

The native engine reads custom signatures from:

`<dbPath>/signatures.db`

One signature per line. Built-in signatures include EICAR plus common RAT/ransomware markers.

## Shizuku bootstrap

1. Install Shizuku app.
2. Enable Developer Options + Wireless debugging.
3. Start Shizuku server.
4. Launch Trinity-AV and grant Shizuku permission.
