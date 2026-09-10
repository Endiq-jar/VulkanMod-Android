#!/usr/bin/env bash
set -euo pipefail

# Sync everything from upstream xCollateral/VulkanMod into this repository.
#
# The Android-specific files (net/vulkanmod/android/*, the Android mixins config, the
# Android icon and the extra translations) are preserved. Everything else is overwritten
# with the upstream state.
#
# Usage:
#   scripts/sync-upstream.sh [branch]      # default: dev (the latest Minecraft version)

BRANCH="${1:-dev}"
REPO="https://github.com/xCollateral/VulkanMod.git"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "==> Cloning upstream branch '$BRANCH'..."
git clone --depth 1 --branch "$BRANCH" "$REPO" "$TMP/VulkanMod"

echo "==> Syncing Java sources (preserving net/vulkanmod/android/...)..."
rsync -a --delete --exclude 'android/' \
    "$TMP/VulkanMod/src/main/java/" "$ROOT/src/main/java/"

echo "==> Syncing resources (preserving Android-specific resources)..."
rsync -a --delete \
    --exclude 'fabric.mod.json' \
    --exclude 'vulkanmod.android.mixins.json' \
    --exclude 'vulkan-mod_android.png' \
    --exclude 'lang/de_de.json' \
    --exclude 'lang/es_es.json' \
    --exclude 'lang/fr_fr.json' \
    --exclude 'lang/pt_br.json' \
    "$TMP/VulkanMod/src/main/resources/" "$ROOT/src/main/resources/"

echo "==> Syncing build scaffolding..."
cp "$TMP/VulkanMod/settings.gradle" "$ROOT/settings.gradle"
cp "$TMP/VulkanMod/gradle/wrapper/gradle-wrapper.properties" "$ROOT/gradle/wrapper/gradle-wrapper.properties"
cp "$TMP/VulkanMod/gradlew" "$ROOT/gradlew"
cp "$TMP/VulkanMod/gradlew.bat" "$ROOT/gradlew.bat"
chmod +x "$ROOT/gradlew"

# Pull the upstream version numbers into gradle.properties, but keep the Android-specific
# archives name / mod version suffix.
UPSTREAM_PROPS="$TMP/VulkanMod/gradle.properties"
for key in minecraft_version yarn_mappings loader_version fabric_version; do
    val="$(grep -E "^${key}\s*=" "$UPSTREAM_PROPS" | head -n1 | sed 's/^[^=]*=\s*//')"
    if [ -n "$val" ]; then
        sed -i "s|^${key} = .*|${key} = ${val}|" "$ROOT/gradle.properties"
    fi
done

echo "==> Done. Review the diff (build.gradle / gradle.properties are intentionally"
echo "    not overwritten so the Android additions are kept), then commit."
