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

# --- Java sources: sync upstream, preserving net/vulkanmod/android/... ---
ANDROID_JAVA="$ROOT/src/main/java/net/vulkanmod/android"
if [ -d "$ANDROID_JAVA" ]; then
    mv "$ANDROID_JAVA" "$TMP/android-java"
fi

echo "==> Syncing Java sources (preserving net/vulkanmod/android/...)..."
rm -rf "$ROOT/src/main/java"
cp -r "$TMP/VulkanMod/src/main/java" "$ROOT/src/main/java"
if [ -d "$TMP/android-java" ]; then
    mkdir -p "$ROOT/src/main/java/net/vulkanmod"
    mv "$TMP/android-java" "$ANDROID_JAVA"
fi

# --- Resources: sync upstream, preserving Android-specific resources ---
RES="$ROOT/src/main/resources"
UP_RES="$TMP/VulkanMod/src/main/resources"
KEEP="$TMP/res-keep"
mkdir -p "$KEEP/assets/vulkanmod/lang"

echo "==> Syncing resources (preserving Android-specific resources)..."
for f in fabric.mod.json vulkanmod.android.mixins.json; do
    [ -f "$RES/$f" ] && mv "$RES/$f" "$KEEP/$f"
done
[ -f "$RES/assets/vulkanmod/vulkan-mod_android.png" ] && \
    mv "$RES/assets/vulkanmod/vulkan-mod_android.png" "$KEEP/assets/vulkanmod/vulkan-mod_android.png"
for f in de_de.json es_es.json fr_fr.json pt_br.json; do
    [ -f "$RES/assets/vulkanmod/lang/$f" ] && \
        mv "$RES/assets/vulkanmod/lang/$f" "$KEEP/assets/vulkanmod/lang/$f"
done

rm -rf "$RES"
cp -r "$UP_RES" "$RES"

for f in fabric.mod.json vulkanmod.android.mixins.json; do
    [ -f "$KEEP/$f" ] && mv "$KEEP/$f" "$RES/$f"
done
[ -f "$KEEP/assets/vulkanmod/vulkan-mod_android.png" ] && \
    mv "$KEEP/assets/vulkanmod/vulkan-mod_android.png" "$RES/assets/vulkanmod/vulkan-mod_android.png"
for f in de_de.json es_es.json fr_fr.json pt_br.json; do
    [ -f "$KEEP/assets/vulkanmod/lang/$f" ] && \
        mv "$KEEP/assets/vulkanmod/lang/$f" "$RES/assets/vulkanmod/lang/$f"
done

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
