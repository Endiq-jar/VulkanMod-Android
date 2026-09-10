# VulkanMod for Android

<img src="src/main/resources/assets/vulkanmod/vulkan-mod_android.png" width="200" height="200">

A Vulkan renderer mod for Minecraft: Java Edition, ported to **Android**. This project is
kept in sync with the upstream renderer
**[xCollateral/VulkanMod](https://github.com/xCollateral/VulkanMod)** and layers the
Android-specific changes on top, for every Minecraft version that upstream supports.

**Credits:** [xCollateral](https://github.com/xCollateral) (upstream VulkanMod),
[Endiq](https://github.com/Endiq-jar) (Android port).

---

## What this repository is

The original VulkanMod-Android releases were distributed as prebuilt jars only (the
repository contained a decompiled jar rather than source). This repository turns that into
a proper, buildable source project:

* **Synced with upstream** — the full `xCollateral/VulkanMod` source (latest `dev` branch)
  lives in `src/main/` and can be refreshed with `scripts/sync-upstream.sh`.
* **Android source reconstructed** — the Android-specific code (`net.vulkanmod.android.*`)
  was recovered from the decompiled 1.21.5 jar and rewritten as maintainable Java.
* **Multi-version** — one project that builds for the current version and a version-matrix
  script/workflow that produces Android builds for the older Minecraft versions too.

## Features

* **Vulkan rendering** — replaces the OpenGL renderer with Vulkan for higher frame rates,
  lower CPU overhead and better frame pacing.
* **Android pre-rotation** — creates the swapchain with an identity surface transform and
  handles device rotation on our side instead of letting the compositor rotate every
  frame (see [`AndroidSwapChain`](src/main/java/net/vulkanmod/android/AndroidSwapChain.java)).
* **Android queue selection** — picks present/transfer queues via surface support probing
  (merged upstream for recent versions; kept for older branches).
* **ARM64 & ARM32 natives** — `libshaderc.so` and `liblwjgl_vma.so` for `linux/arm64` and
  `linux/arm32` are bundled directly into the jar.

## Compatibility

| | |
| --- | --- |
| OS | Android 10+ |
| Architecture | ARM64 (arm32 natives included) |
| Vulkan | 1.2+ required |
| Mod loader | Fabric |
| Launchers | Zalith, PojavLauncher, FoldCraft, Turtle, Cryonix, HyperX, RX, MJ/Mojo, SolCraft, Copper |

This mod is fundamentally incompatible with most mods that directly touch the OpenGL
rendering pipeline (many OptiFine-style / shader mods).

## Version matrix

Every Minecraft version that upstream VulkanMod supports has an Android patch. Each
upstream branch maps to one of three renderer "generations" (see `android/patches/`):

| Minecraft | Upstream branch | Android patch | Status |
| --- | --- | --- | --- |
| 1.21.11 (latest) | `dev` | in-tree `src/` | ✔ synced |
| 1.21 | `1.21` | `android/patches/1.21.x` | ✔ |
| 1.20.4 | `1.20.x` | `android/patches/1.21.x` | ✔ |
| 1.19.4 | `1.19.4` | `android/patches/1.19.4` | ✔ |
| 1.19.2 | `1.19.2` | `android/patches/1.18.2` | ✔ |
| 1.18.2 | `1.18.2` | `android/patches/1.18.2` | ✔ |

> "Every single Minecraft version" is bounded by what upstream VulkanMod itself supports
> (see its [branches](https://github.com/xCollateral/VulkanMod/branches)). Supporting an
> arbitrary Minecraft release requires the matching upstream branch to exist; the tooling
> here (below) makes adding one a one-line change.

## Building

Requirements: JDK 21, and network access to Maven (Fabric / Maven Central / Gradle).

```bash
# Clone and build the current (latest) version
git clone https://github.com/Endiq-jar/VulkanMod-Android.git
cd VulkanMod-Android
./gradlew build            # → build/libs/VulkanMod-Android-*.jar
```

The Gradle wrapper jar is generated on first run (`./gradlew wrapper`), or use a local
Gradle 9.x install. CI (below) uses `gradle/actions` so it does not need the wrapper jar.

Build a specific Minecraft version (checks out the matching upstream branch, applies the
Android patch and builds):

```bash
scripts/build-version.sh 1.21
scripts/build-version.sh 1.20.x
scripts/build-version.sh 1.19.4
```

The `.github/workflows/build.yml` matrix does exactly this on every push.

## Keeping in sync with upstream

```bash
scripts/sync-upstream.sh           # sync with upstream dev (latest)
scripts/sync-upstream.sh 1.21      # sync with a specific branch
```

The script overwrites the Java sources, shaders, mixin config and access widener with the
upstream state while preserving `net/vulkanmod/android/*`, the Android mixins config, the
Android icon and the extra translations. Review the diff afterwards — `build.gradle` and
`gradle.properties` are intentionally left untouched (they carry the Android additions).

## Repository layout

```
src/main/java/net/vulkanmod/…         upstream VulkanMod source (synced)
src/main/java/net/vulkanmod/android/…  Android pre-rotation + present handling (mixins)
src/main/resources/…                  shaders, lang, fabric.mod.json, mixins, access widener
linux/arm64, linux/arm32/…            bundled Android natives (shaderc, vma)
android/android-natives.gradle        jar packaging for the natives
android/patches/1.21.x/…              Android patch for the 1.20.x / 1.21 renderer generation
android/patches/1.19.4/…              Android patch for 1.19.4
android/patches/1.18.2/…              Android patch for 1.18.2 / 1.19.2
scripts/sync-upstream.sh              sync tool
scripts/build-version.sh              per-version build tool
.github/workflows/build.yml           multi-version CI build
```

## Installation

1. Grab the matching jar from the [Releases](https://github.com/Endiq-jar/VulkanMod-Android/releases)
   (or build one above).
2. Drop the `.jar` into the `mods` folder of one of the supported launchers, with the
   Fabric mod loader installed for your Minecraft version.

## Known limitations

* **Pre-rotation is minimal.** The reference 1.21.5 artifact forces an identity surface
  transform and ignores `VK_SUBOPTIMAL_KHR` while rotated, but never applied the rotation
  matrix to the projection (`applyPreRotation` / `getTransformExtent` were present but
  unused, and `preRotateMat` was left as identity). This works on the launchers listed
  above because they present with an identity surface transform. The helpers are
  reconstructed and the rotation matrix is now populated in `AndroidSwapChain#setupTransform`,
  but `applyPreRotation` is still not wired into the projection — full pre-rotation for
  rotated surfaces remains a TODO (see `VRenderSystemMixin` in `android/patches/1.21.x`).
* The patches are faithful to the 1.21.5 jar and adapted per renderer generation; each
  upstream branch targets a different Minecraft mapping, so the `@Redirect` targets are
  written against the stable LWJGL entry points (`VkSwapchainCreateInfoKHR#preTransform`,
  `vkAcquireNextImageKHR`, `vkQueuePresentKHR`) rather than version-specific game classes
  wherever possible.

## License

* This project: MIT ([`LICENSE`](LICENSE)).
* Upstream VulkanMod: LGPL-3.0 ([`LICENSE_VulkanMod_1.21.5`](LICENSE_VulkanMod_1.21.5)).
