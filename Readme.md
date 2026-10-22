# Oxbow — Instagram without Reels, feed or ads (Android)

**Keep Instagram for what you open it for — messages, stories, the people you know —
and lose the feed, Reels and ads built to keep you scrolling.** Oxbow is a free,
open-source patched Instagram for Android that also adds downloads, ghost mode and
tracker-free links.

**[Download the latest APK](https://github.com/aic0d3r/oxbow/releases/latest)** ·
[Patch it yourself with Morphe](#install) · [How it compares](#how-it-compares) ·
[FAQ](#faq)

[![Release](https://img.shields.io/github/v/release/aic0d3r/oxbow?color=2ea44f)](https://github.com/aic0d3r/oxbow/releases/latest)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE)
[![Instagram 452.0.0.48.64](https://img.shields.io/badge/works%20with-Instagram%20452.0.0.48.64-8a2be2)](https://github.com/aic0d3r/oxbow/releases)

| Download from Instagram's own ⋯ menu | Settings: long-press the Home tab |
|---|---|
| ![A Reel's ⋯ menu showing a native Download row above Report](docs/screenshots/download-row.png) | ![Oxbow settings, Keep and privacy page](docs/screenshots/settings.png) |

## What Oxbow removes

Blocked by default, each with its own switch:

- **Home feed, Reels and Explore**, and the suggested posts mixed into them
- **Ads**, plus Notes and Instants
- **Endless scrolling** — a reel opened from a DM, profile or search stays one reel,
  with no "more like this" feed under it

Off by default, one tap away: **comments** (no comment buttons or sheets), **stories**
and the **notifications button**. Telemetry is always blocked.

Want your choices to stick? **Permanent lock** lets you tighten blocks but not loosen
them until you reinstall.

## What Oxbow adds

- **Downloads** — posts, carousel slides, videos, Reels, stories and highlights, from a
  native "Download" row in Instagram's own `⋯` menu. Saved in the highest quality
  Instagram serves, to `Pictures/Oxbow` and `Movies/Oxbow`, with no storage permission.
- **HD profile pictures** — long-press any avatar on a profile.
- **Ghost mode** (off by default) — watch stories without appearing as a viewer, read
  DMs without sending read receipts, type without the typing indicator.
- **Tracker-free links** — copied and shared links keep only what's needed to open
  them; `igsh`, `utm_*`, `obrf` and the rest are stripped.

Downloads use media the app has already loaded, so Oxbow makes no extra requests to
Instagram.

## Install

**Option 1 — prebuilt APK** (simplest). From
[Releases](https://github.com/aic0d3r/oxbow/releases/latest):

- `oxbow.apk` replaces Instagram (uninstall Instagram first)
- `oxbow-clone.apk` installs next to your existing Instagram

The app checks for updates on launch. For [Obtainium](https://github.com/ImranR98/Obtainium),
add `https://github.com/aic0d3r/oxbow` and filter APKs by `oxbow\.apk$` (or
`oxbow-clone\.apk$`).

**Option 2 — patch it yourself with [Morphe Manager](https://morphe.software)**, so no
prebuilt APK is involved and updates arrive through the Manager. On your phone, open
[`morphe.software/add-source?github=aic0d3r/oxbow`](https://morphe.software/add-source?github=aic0d3r/oxbow&name=Oxbow&app=com.instagram.android)
(or add the source `github.com/aic0d3r/oxbow` by hand), then patch an Instagram
`.apkm` bundle from APKMirror. Enable *Clone* to keep the official app too.

## How it compares

| | FeurStagram | piko | **Oxbow** |
|---|---|---|---|
| Blocks feed, Reels, Explore, ads | Yes | Partly | **Yes** |
| Permanent lock | Yes | Focus Lock | **Yes** |
| Downloads | No | Yes | **Yes**, native menu row + HD avatars |
| Ghost mode | No | Yes | **Yes** |
| Tracker-free links | No | Yes, misses `obrf` | **Yes** |
| Comments off, one reel at a time | No | Yes | **Yes** |
| Works on Instagram 452 | Yes | No | **Yes** |

## FAQ

**Can I turn off Reels in the official Instagram app?** No — Instagram has no setting
for it. Oxbow removes the Reels tab, Reels in the feed and the endless Reels viewer.

**Is there an iPhone version?** No. Oxbow is Android only.

**Do I need root?** No. It's a regular APK you install, with your normal Instagram login.

**Can I keep the official Instagram app?** Yes — install `oxbow-clone.apk`, or enable
*Clone* in Morphe Manager. You log in separately.

**Is it safe? Can my account get banned?** Oxbow doesn't collect, proxy or store your
credentials, and every patch is open source. But it's an unofficial modified client,
not affiliated with Meta: using it may break Instagram's terms, and ghost mode or
downloads give Instagram more signs of a modified client. Use it at your own risk.

**Why isn't it on F-Droid?** F-Droid only ships apps it builds from source, and Oxbow
is a patch on top of Meta's closed Instagram APK.

## Build it yourself

Needs JDK 21, the Android SDK, a GitHub token with `read:packages` (for the Morphe
patcher) and an Instagram `.apkm` from APKMirror. Details in
[docs/BUILD_FROM_SOURCE.md](docs/BUILD_FROM_SOURCE.md).

```sh
./build.sh instagram.apkm            # -> oxbow.apk
./build.sh instagram.apkm --clone    # -> oxbow-clone.apk
./release.sh instagram.apkm          # both, signed, published as a GitHub release
```

How the patches find their way into Instagram's obfuscated code, and why they survive
updates: [OXBOW.md](OXBOW.md).

## Credits & license

Oxbow is a fork of [FeurStagram](https://github.com/jean-voila/FeurStagram) by Jean
Herail. Its blocking, settings page and update system come from there, and Oxbow
follows it for new Instagram versions. GPL-3.0: see [LICENSE](LICENSE) and
[NOTICE](NOTICE).

*Why "Oxbow"? An oxbow is a calm lake cut off from a river's main current.*
