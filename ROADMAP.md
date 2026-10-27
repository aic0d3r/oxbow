# Roadmap

Oxbow's direction is a calm Instagram that is set up from the first launch and holds you to
your choices. Planned work is judged against that. This list may change.

## Next

- Device-test the Instagram 453 build (three regression fixes) and release it.
  452.1.0.53.64 stays the fallback if 453 cannot be made to work.
- Kill the story-viewer swipe-up gesture (Piko #2134). The reply bar and quick
  reactions can be hidden at the view layer, but the keyboard is opened through a
  handler that survived every static hook tried (the delegate swipe handlers, the
  composer-open funnel, the composer focus call, the show-keyboard lambda) — it
  needs runtime tracing on a device to pin down.

## Reliability

- Keep up with new Instagram versions, and say on each release which one it supports.
- Document known issues per release.
- Improve clone APK behavior where possible.
- Add basic smoke tests where possible.

## Privacy and trust

- Publish SHA256 hashes for every release artifact (done for APKs and the patch bundle).
- Document what each patch changes.
- Improve build-from-source instructions.
- Document blocked endpoints or telemetry-related changes when relevant.

## Installation

- Make the difference between classic APK and clone APK clearer.
- Improve troubleshooting for Android installation warnings.

## Features

Possible later, if they fit the calm direction:

- more granular Reels blocking;
- better story controls;
- optional settings import/export.

## Community

- Label beginner-friendly issues.
- Keep bug and feature templates current.
- Document contribution workflow.
