# Berry Music

A Kotlin Multiplatform music app (Compose UI) for Android: local library playback,
OpenSubsonic servers, Logto (OIDC) sign-in, and a sandboxed QuickJS plugin runtime for
lyrics lookups and additional search sources.

## Build

Requires JDK 17+ and the Android SDK (compileSdk 37).

    ./gradlew :androidApp:assembleDebug

This repository vendors a native QuickJS submodule (`quickjs/src/main/cpp/quickjs-ng`).
Clone recursively so it is present:

    git clone --recurse-submodules <repo-url>

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).
