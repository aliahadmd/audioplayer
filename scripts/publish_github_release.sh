#!/usr/bin/env bash
set -euo pipefail

if ! command -v gh >/dev/null 2>&1; then
  echo "The GitHub CLI (gh) is required. Install it from https://cli.github.com/" >&2
  exit 1
fi

VERSION=${1:-0.0.1}
TAG="v${VERSION}"
APK_PATH="app/build/outputs/apk/release/app-release.apk"

if [ ! -f "${APK_PATH}" ]; then
  echo "APK not found at ${APK_PATH}. Build it first with ./gradlew :app:assembleRelease" >&2
  exit 1
fi

RELEASE_TITLE="Audio Player ${VERSION}"
NOTES_FILE=$(mktemp)
cat <<'NOTES' > "${NOTES_FILE}"
## What’s new
- Initial public release build.

## Checks
- Signed with the release keystore stored in repo under keystore/release.jks.
NOTES

gh release create "${TAG}" "${APK_PATH}" \
  --title "${RELEASE_TITLE}" \
  --notes-file "${NOTES_FILE}"

echo "GitHub release ${TAG} published with ${APK_PATH}."
