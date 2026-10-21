# Oxbow

Fork of [FeurStagram](https://github.com/jean-voila/FeurStagram). An oxbow is a calm lake
cut off from the river's main current: Instagram off the stream, keeping what matters.

**Direction: calm + keep.** Upstream's wellbeing patches stay the core (we rebase on
`upstream` for new Instagram versions). On top of them we add utilities that let you *keep*
things, not ones that pull you back into scrolling.

## Roadmap

1. **Downloads** (first)
   - "Download" item in the native ⋯ menu on posts, reels and stories
   - Carousels: current slide, plus "all slides"
   - Full-res profile pictures and highlight stories
   - Saves to `Pictures/Oxbow` / `Movies/Oxbow` through MediaStore (no storage permission)
2. **Ghost mode**: hide seen/typing receipts in DMs and seen on stories (upstream #53)
3. **Clean share links**: strip `igsh` and other tracking params from copied links (#88)
4. **F-Droid / Obtainium**: reproducible builds and release metadata (#136, PR #41)
5. **Kill remaining scroll**: block comments (#138), the Reels scroll in Search (#141) and
   the Inspiration tab (#149), and hide the Home button (#127/#137)

## Rebrand (pending)

The code still uses `com.feurstagram` and the FeurStagram keystore. We'll rename to
`app.oxbow` with a new signing key before the first Oxbow release. Users will need a
fresh install at that point.
