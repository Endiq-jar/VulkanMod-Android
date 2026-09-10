#!/usr/bin/env bash
set -euo pipefail

# Build the Android mod for a specific Minecraft version.
#
# For each Minecraft version, the matching upstream branch is checked out, the Android
# patch (android/legacy-patch + android natives) is applied on top, and the project is
# built with Gradle. The produced jar lands in build/libs/.
#
# Usage:
#   scripts/build-version.sh <branch> [gradle-args...]
#
#   scripts/build-version.sh 1.21
#   scripts/build-version.sh dev         # latest (1.21.x) — uses the in-tree source instead
#
# The mapping between Minecraft versions and upstream branches is documented in the README
# (see "Version matrix").

BRANCH="${1:?usage: build-version.sh <branch> [gradle-args...]}"
shift || true

REPO="https://github.com/xCollateral/VulkanMod.git"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [ "$BRANCH" = "dev" ]; then
    echo "==> Building in-tree source (synced with upstream dev)..."
    cd "$ROOT"
    exec "$ROOT/gradlew" build "$@"
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "==> Cloning upstream branch '$BRANCH'..."
git clone --depth 1 --branch "$BRANCH" "$REPO" "$TMP/VulkanMod"
cd "$TMP/VulkanMod"

echo "==> Applying the Android patch..."
# Android Java sources (AndroidSwapChain + mixins).
cp -r "$ROOT/android/legacy-patch/src/." src/main/java/
# Android mixin config.
cp "$ROOT/android/legacy-patch/vulkanmod.android.mixins.json" src/main/resources/
# Android natives (arm32/arm64 shaderc + vma).
cp -r "$ROOT/linux" .

echo "==> Registering the Android mixins and icon in fabric.mod.json..."
python3 - "$ROOT" <<'PY'
import json, sys, os
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
import shutil
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
echo "==> Done. Jar: $TMP/VulkanMod/build/libs/"
