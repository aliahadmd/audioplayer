#!/usr/bin/env bash
set -euo pipefail

VERSION=${1:-0.0.3}
TAG="v${VERSION}"

echo "Creating annotated tag ${TAG}"

git tag -a "${TAG}" -m "SoundVault ${VERSION}"
echo "Tag ${TAG} created locally. Push it with: git push origin ${TAG}"
