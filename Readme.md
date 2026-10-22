# Oxbow

**Instagram, off the stream.** An oxbow is a calm lake cut off from a river's main current.
Oxbow is a patched Instagram for Android that removes the endless parts (feed, Reels, Explore,
endless scrolling, ads, telemetry) and adds the few things worth having: downloads, ghost mode,
clean links.

Oxbow is a fork of [FeurStagram](https://github.com/jean-voila/FeurStagram) by Jean Herail
(GPL-3.0). All of FeurStagram's blocking and its settings page come from there; Oxbow keeps
up with it for new Instagram versions.

## What it adds

| Feature | Where | Default |
|---|---|---|
| **Download** posts, carousel slides, videos, Reels, stories, highlights | Instagram's own `⋯` menu, a native "Download" row | On |
| **Profile picture** in full resolution | Long-press the avatar on someone's profile | On |
| **Clean links**: copied and shared links lose `igsh`, `utm_*`... | Automatic | On |
| **No endless scrolling**: a reel opened from a DM, profile or search stays one reel; no "more like this" feed under search results | Automatic | On |
| **Comments off**: no comment buttons, no comment sheets | Blocked surfaces | Off |
| **Ghost mode**: don't mark stories seen, no read receipts, no typing indicator | Keep and privacy | Off |

Files go to `Pictures/Oxbow` and `Movies/Oxbow` through Android's download manager (progress
notification, no storage permission). The file comes straight from Instagram's CDN, using
data the app already loaded: nothing extra is asked from Instagram.

Everything FeurStagram does is still there: home feed, Reels, Explore, suggestions, ads and
telemetry blocked, per-surface toggles, navigation bar and landing page, permanent lock.
See [FeurStagram's README](https://github.com/jean-voila/FeurStagram#readme) for those.

**Settings:** long-press the Home tab. Oxbow's own options are under **Keep and privacy**;
comments and endless scrolling are in **Blocked surfaces**, so the permanent lock covers them.

## Install

Download from [Releases](https://github.com/aic0d3r/oxbow/releases/latest):

- `oxbow.apk` replaces Instagram (uninstall Instagram first).
- `oxbow-clone.apk` installs next to Instagram.

**Updates:** Oxbow checks this repository on launch. Or use
[Obtainium](https://github.com/ImranR98/Obtainium): add `https://github.com/aic0d3r/oxbow` and set
*Filter APKs by regular expression* to `oxbow\.apk$` (or `oxbow-clone\.apk$`).

*F-Droid:* not possible. F-Droid only ships apps it builds from source, and Oxbow is a patch on
top of Meta's closed Instagram APK.

## Build it yourself

Needs JDK 21, the Android SDK, a GitHub token with `read:packages` (for the Morphe patcher) and
an Instagram `.apkm` from APKMirror. See [docs/BUILD_FROM_SOURCE.md](docs/BUILD_FROM_SOURCE.md).

```sh
./build.sh instagram.apkm            # -> oxbow.apk
./build.sh instagram.apkm --clone    # -> oxbow-clone.apk
./release.sh instagram.apkm          # both, signed, published as a GitHub release
```

How the patches find their way into Instagram's obfuscated code, and why they survive updates:
[OXBOW.md](OXBOW.md).

## Is it safe?

Same answer as FeurStagram: it does not collect, proxy or store your credentials, and every patch
is open source. It is an unofficial modified client, not affiliated with Meta; using it may
violate Instagram's terms. Ghost mode and downloads are client-side, but Instagram can still
notice modified clients. Use it at your own risk.

## License

GPL-3.0, like FeurStagram. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
