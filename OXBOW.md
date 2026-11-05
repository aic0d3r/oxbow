# Oxbow

Fork of [FeurStagram](https://github.com/jean-voila/FeurStagram). An oxbow is a calm lake
cut off from the river's main current: Instagram off the stream, keeping what matters.

**Direction: calm + keep.** Upstream's wellbeing patches stay the core (we merge `upstream`
for new Instagram versions). On top of them, utilities that let you *keep* things, never ones
that pull you back into scrolling.

## Features (Instagram 454.0.0.54.77)

On-device status below is from a smoke test of the 454 clone on the test phone (classic docked
tab bar, no pill highlight, no Create tab). Rows marked *not retested on 454* last passed on 453
and build + hook were verified on 454, not exercised on the phone.

| Feature | Patch / extension | Tested on device |
|---|---|---|
| Download: posts, carousel slide, videos | `download/DownloadPatch.kt`, `Download.java` | not retested on 454 (yes on 453) |
| Download: stories, highlights | same | yes on 454 (story photo saved); highlights not retested |
| Download: reels viewer (any menu variant) | same (`addClipsRow`) | not retested on 454 (yes on 453) |
| HD profile picture: long-press avatar | same (`profileAvatarLongPress`) | not retested on 454 (yes on 453, 1080px) |
| Clean share links (allowlist: `img_index`, `story_media_id`, `comment_id`, `hl`) | `privacy/CleanLinksPatch.kt`, `ShareLinks.java` | not retested on 454 (yes on 453); the video share's link parameter was re-checked in the 454 bytecode |
| Ghost: stories not marked seen | `privacy/GhostModePatch.kt` + `Block.java` | stories open fine on 454; "not in viewer list" needs a 2nd account |
| Ghost: no DM read receipts | same (`holdReceipt`, inbox seen check) | 453: yes (two accounts: sender stays on Delivered, the chat clears and stays cleared after a restart). 454: patch applies (the inbox unread answer needed a new match) and the hooks are in the bytecode; not exercised on the phone |
| Ghost: no typing indicator | same | not exercised (needs a real chat) |
| Ghost: Hide screenshots (view-once / replayable / vanish-mode photo or video; approach adapted from HushGram) | same (both screenshot detectors' hand-off), `Config.isScreenshotHidden` | build + hook verified, end-to-end not confirmed (needs a second account to send a view-once photo). Verified: both hand-offs begin with the gate in the 454 bytecode, the settings row renders, the New message shortcut flips it with the other three |
| Ghost: long-press New message flips the Ghost switches | `Hiders.java` (`GhostShortcut`) | yes on 454 (toast, all four switches on) |
| Comments off (feed + reels buttons, composer, sheet, endpoints) | `distraction/CommentsAndScrollPatch.kt`, `Scroll.java`, `Block.java` | not retested on 454 (yes on 453); the Reels comment-state hook is in the 454 bytecode |
| No endless scrolling (reel viewer swipe, "more like this" feeds, Inspiration reels) | same | not retested on 454 (yes on 453) |
| No story swipe-up (reply box, reactions, keyboard; Send message still replies) | same (`StorySwipeUpFingerprint`) | yes on 454 (photo story: no keyboard, no reply bar) |
| Feed ads (sponsored posts dropped at parse time) | `network/FeedItemFilterPatch.kt`, `Block.java` | build + hook verified (454 is back on the per-field dispatcher path); the feed is blocked on the test account, so not seen |
| Hide Home: Back leaves the app from the first tab left (Back history) | `settings/BackHistoryPatch.kt`, `Hiders.backHistoryDepth` (from FeurStagram) | build verified; Home-hidden Back not tested on the phone |
| Hide Meta AI / Muse (share sheet target, Meet Muse promo cards, Vibes / Meta AI / Imagine feed units) | `metaai/HideMetaAiPatch.kt`, `MetaAi.java`, `Block.java` | build + no crash on 453; this account never gets the Muse target, so removal not seen. Not retested on 454 |
| Hide Home (long-press Messages opens settings) | `Hiders.java`, `HomeTabWatcher.java`, `Config.java` | settings open from the Messages long-press on 454 (yes); hiding Home itself not retested on 454 |

Settings: *Keep and privacy* page (downloads, clean links, ghost mode); comments and endless
scrolling live in *Blocked surfaces* so the permanent lock covers them.

## Defaults (fresh install)

Calm first: distraction is blocked, privacy extras are opt-in. Source of truth: `Config.java`.
A changed default only reaches toggles the user never touched.

| On | Off |
|---|---|
| Block feed, Reels, Explore, Friends lane, Instants, Notes, suggested, ads, Meta AI | Block stories, comments, notifications button |
| Block endless scrolling, story swipe-up | Following-only feed |
| Downloads | Download all carousel slides |
| Clean links | Ghost: story seen, DM read receipts, typing, screenshots |
| Hide Instagram popups | Permanent lock |
| Force SDR | Reels tab icon (hidden) |
| Update check on launch | |

Landing page: Home. All other tab icons are shown.

## How the patches stay version-proof

- Anchors are stable things: named classes (`LiveTreeJNI`, `TreeJNI`, `ViewPager2`,
  `ClipboardManager`), API strings (`mark_thread_seen-`, `indicate_activity`,
  `media/seen/?reel=...`), `__redex_internal_original_name` values, and method shapes.
  Purge-log strings are matched by suffix (their `android_purge_26_q3_` prefix changes).
- Media and user data are read from Instagram's Pando trees by **API field name**: the tree
  keys every field by `String.hashCode()` of its name, so `"image_versions2".hashCode()` works
  in every version.
- Menu rows are Instagram's own (`MediaOption$Option.DOWNLOAD`, its icon and label), added
  through Instagram's own row builders found by signature.

## Releasing

```sh
./release.sh instagram.apkm          # builds oxbow.apk + oxbow-clone.apk, publishes vX-Y-Z
```

Signing: `oxbow.keystore` (git-ignored) with the password in `~/.config/oxbow/release.env`;
a backup lives in `~/.config/oxbow/`. Lose both and existing installs can't update.

## Rebrand notes

User-visible names, the clone package (`com.instagram.android.oxbow`), the updater
(`aic0d3r/oxbow`) and the signing key are Oxbow's. Internal Java/Kotlin packages stay
`com.feurstagram.*` on purpose, so upstream merges stay clean.

## Not done / known limits

- F-Droid can't ship Oxbow (it builds from source only); Obtainium works from GitHub releases.
- DM read receipts off leaves the message unread for the sender, which is the point. On this
  phone the chat still clears when you open it or reply, and stays cleared after a refresh or a
  restart, but counts Instagram keeps on its own side (the account switcher, another device)
  keep showing the chat unread.
- Story-seen blocking: stories you watch while it is on will be reported if you switch it off
  later and they are still in your tray (Instagram re-sends locally seen ids with the tray).
- Re-downloading the same item creates `name-1.jpg` (Android's DownloadManager), not a skip.
