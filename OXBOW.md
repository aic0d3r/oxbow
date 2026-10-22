# Oxbow

Fork of [FeurStagram](https://github.com/jean-voila/FeurStagram). An oxbow is a calm lake
cut off from the river's main current: Instagram off the stream, keeping what matters.

**Direction: calm + keep.** Upstream's wellbeing patches stay the core (we rebase on
`upstream` for new Instagram versions). On top of them we add utilities that let you *keep*
things, not ones that pull you back into scrolling.

## Roadmap

1. **Downloads** (first)
   - [x] "Download" in the native ⋯ menu of posts, reels, stories and highlights
         (`patches/.../download/DownloadPatch.kt`, `extension/Download.java`)
   - [x] Carousels: the slide on screen
   - [x] Saves originals to `Pictures/Oxbow` / `Movies/Oxbow` via DownloadManager (no permission)
   - [ ] Full-res profile pictures (profile menu rows are a fixed enum; needs another hook)
   - [ ] Reels opened from DMs (separate Clips viewer menu, untested)
2. **Ghost mode**: hide seen/typing receipts in DMs and seen on stories (upstream #53)
3. **Clean share links**: strip `igsh` and other tracking params from copied links (#88)
4. **F-Droid / Obtainium**: reproducible builds and release metadata (#136, PR #41)
5. **Kill remaining scroll**: block comments (#138), the Reels scroll in Search (#141) and
   the Inspiration tab (#149), and hide the Home button (#127/#137)

## Rebrand (pending)

The code still uses `com.feurstagram` and the FeurStagram keystore. We'll rename to
`app.oxbow` with a new signing key before the first Oxbow release. Users will need a
fresh install at that point.
