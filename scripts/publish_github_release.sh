#!/usr/bin/env bash
set -euo pipefail

if ! command -v gh >/dev/null 2>&1; then
  echo "The GitHub CLI (gh) is required. Install it from https://cli.github.com/" >&2
  exit 1
fi

VERSION=${1:-0.0.3}
TAG="v${VERSION}"
APK_PATH="app/build/outputs/apk/release/app-release.apk"
UNSIGNED_APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"

if [ ! -f "${APK_PATH}" ]; then
  if [ -f "${UNSIGNED_APK_PATH}" ]; then
    echo "Unsigned APK found at ${UNSIGNED_APK_PATH}, but publishing requires a signed release APK." >&2
    echo "Configure SOUNDVAULT_RELEASE_STORE_FILE, SOUNDVAULT_RELEASE_STORE_PASSWORD, SOUNDVAULT_RELEASE_KEY_ALIAS, and SOUNDVAULT_RELEASE_KEY_PASSWORD, then rebuild with ./gradlew :app:assembleRelease." >&2
    exit 1
  fi
  echo "APK not found at ${APK_PATH}. Build it first with ./gradlew :app:assembleRelease" >&2
  exit 1
fi

RELEASE_TITLE="SoundVault ${VERSION}"
NOTES_FILE=$(mktemp)
cat <<'NOTES' > "${NOTES_FILE}"
## What’s new
- Renamed the app brand to SoundVault.

## Checks
- Signed with the release keystore configured outside the repo.
NOTES

gh release create "${TAG}" "${APK_PATH}" \
  --title "${RELEASE_TITLE}" \
  --notes-file "${NOTES_FILE}"

echo "GitHub release ${TAG} published with ${APK_PATH}."
