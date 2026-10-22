#!/usr/bin/env bash
#
# Oxbow release: builds the classic and clone APKs signed with the Oxbow key and
# publishes them as a GitHub release (what the in-app updater and Obtainium read).
#
#   ./release.sh <instagram.apkm> [--draft]
#
# Needs ~/.config/oxbow/release.env (keystore password, path, alias) and `gh` logged in.
# The tag is the Instagram version, e.g. v452-0-0-48-64: the updater compares it with
# the installed versionName.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APK="${1:?usage: ./release.sh <instagram.apkm> [--draft]}"
DRAFT="${2:-}"
# shellcheck disable=SC1091
source "$HOME/.config/oxbow/release.env"

"$DIR/build.sh" "$APK"
"$DIR/build.sh" "$APK" --clone

AAPT2="$(ls -t "${ANDROID_HOME:-$HOME/Android/Sdk}"/build-tools/*/aapt2 | head -1)"
VERSION="$("$AAPT2" dump badging "$DIR/oxbow.apk" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p")"
TAG="v${VERSION//./-}"
SUMS="$(cd "$DIR" && sha256sum oxbow.apk oxbow-clone.apk)"

NOTES="$(cat <<EOF
Oxbow for Instagram ${VERSION}.

- **oxbow.apk** replaces Instagram (uninstall Instagram first).
- **oxbow-clone.apk** installs next to Instagram.

Obtainium: add \`https://github.com/aic0d3r/oxbow\` and set the APK filter to \`oxbow\\.apk\$\` or \`oxbow-clone\\.apk\$\`.

\`\`\`txt
${SUMS}
\`\`\`
EOF
)"

gh release create "$TAG" "$DIR/oxbow.apk" "$DIR/oxbow-clone.apk" \
    -R aic0d3r/oxbow --title "Oxbow $VERSION" --notes "$NOTES" ${DRAFT:+--draft}
echo "Released $TAG"
