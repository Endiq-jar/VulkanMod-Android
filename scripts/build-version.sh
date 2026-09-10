#!/usr/bin/env bash
set -euo pipefail

# Build the Android mod for a specific Minecraft version.
#
# For each Minecraft version, the matching upstream branch is checked out, the Android
# patch for that renderer generation (android/patches/<generation>) plus the bundled
# Android natives are applied on top, and the project is built with Gradle.
#
# Usage:
#   scripts/build-version.sh <branch> [gradle-args...]
#
#   scripts/build-version.sh dev          # latest — builds the in-tree source directly
#   scripts/build-version.sh 1.21         # patch: android/patches/1.21.x
#   scripts/build-version.sh 1.20.x       # patch: android/patches/1.21.x
#   scripts/build-version.sh 1.19.4       # patch: android/patches/1.19.4
#   scripts/build-version.sh 1.19.2       # patch: android/patches/1.18.2
#   scripts/build-version.sh 1.18.2       # patch: android/patches/1.18.2
#
# The mapping between Minecraft versions and upstream branches is documented in the README
# (see "Version matrix").

BRANCH="${1:?usage: build-version.sh <branch> [gradle-args...]}"
shift || true

REPO="https://github.com/xCollateral/VulkanMod.git"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# The in-tree source already contains the Android code for the latest (dev) version.
if [ "$BRANCH" = "dev" ]; then
    echo "==> Building in-tree source (synced with upstream dev)..."
    cd "$ROOT"
    exec "$ROOT/gradlew" build "$@"
fi

# Select the Android patch matching the renderer generation of the target branch.
case "$BRANCH" in
    1.21|1.20.x)
        PATCH="$ROOT/android/patches/1.21.x"
        ;;
    1.19.4)
        PATCH="$ROOT/android/patches/1.19.4"
        ;;
    1.18.2|1.19.2)
        PATCH="$ROOT/android/patches/1.18.2"
        ;;
    *)
        echo "error: no Android patch mapping for branch '$BRANCH'" >&2
        exit 2
        ;;
esac

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "==> Cloning upstream branch '$BRANCH'..."
git clone --depth 1 --branch "$BRANCH" "$REPO" "$TMP/VulkanMod"
cd "$TMP/VulkanMod"

echo "==> Applying the Android patch ($PATCH)..."
# Android Java sources (AndroidSwapChain + mixins).
cp -r "$PATCH/src/." src/main/java/
# Android mixin config.
cp "$PATCH/vulkanmod.android.mixins.json" src/main/resources/
# Android natives (arm32/arm64 shaderc + vma).
cp -r "$ROOT/linux" .

echo "==> Registering the Android mixins and icon in fabric.mod.json..."
python3 - "$ROOT" <<'PY'
import json, sys, os, shutil
root = sys.argv[1]
path = "src/main/resources/fabric.mod.json"
with open(path) as f:
    mod = json.load(f)
mixins = mod.setdefault("mixins", [])
if "vulkanmod.android.mixins.json" not in mixins:
    mixins.append("vulkanmod.android.mixins.json")
icon = os.path.join(root, "src/main/resources/assets/vulkanmod/vulkan-mod_android.png")
dest = "src/main/resources/assets/vulkanmod/vulkan-mod_android.png"
os.makedirs(os.path.dirname(dest), exist_ok=True)
shutil.copyfile(icon, dest)
mod["icon"] = "assets/vulkanmod/vulkan-mod_android.png"
with open(path, "w") as f:
    json.dump(mod, f, indent=2)
    f.write("\n")
PY

echo "==> Wiring the Android natives into the build..."
cp "$ROOT/android/android-natives.gradle" .
printf '\napply from: "android-natives.gradle"\n' >> build.gradle

echo "==> Building..."
./gradlew build "$@"

echo "==> Copying artifacts to $ROOT/build/libs/"
mkdir -p "$ROOT/build/libs"
cp build/libs/*.jar "$ROOT/build/libs/" 2>/dev/null || true
echo "==> Done. Jar: $ROOT/build/libs/"
