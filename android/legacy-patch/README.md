# Legacy Android patch

This directory contains a faithful source reconstruction of the Android-specific code that
shipped in the original **VulkanMod-Android 1.21.5** jar (which was distributed without
sources). It is applied on top of an upstream `xCollateral/VulkanMod` branch to produce an
Android build for the corresponding Minecraft version.

## What's here

| File | Purpose |
| --- | --- |
| `src/net/vulkanmod/android/AndroidSwapChain.java` | Surface-transform / pre-rotation helpers |
| `src/net/vulkanmod/android/mixin/SwapChainMixin.java` | Forces `preTransform = IDENTITY` and captures the surface transform |
| `src/net/vulkanmod/android/mixin/RendererMixin.java` | Treats `VK_SUBOPTIMAL_KHR` as success while pre-rotation is active |
| `src/net/vulkanmod/android/mixin/QueueMixin.java` | Queue-family selection via surface present support |
| `src/net/vulkanmod/android/mixin/DrawBuffersMixin.java` | Pins the old six-argument `bindBuffers` implementation |
| `src/net/vulkanmod/android/mixin/FramebufferMixin.java` | Shadows `Framebuffer#width`/`height` (kept for completeness, unused) |
| `src/net/vulkanmod/android/mixin/RendererAccessor.java` | Accessor for `Renderer` (present in the jar but unregistered) |
| `src/net/vulkanmod/android/mixin/VRenderSystemMixin.java` | Shadows the projection-matrix buffer (present in the jar but unregistered) |
| `vulkanmod.android.mixins.json` | Registers the active Android mixins (as in the original jar) |

## Which versions does it apply to?

The patch targets the version branches whose renderer architecture matches the 1.21.5
base:

* **1.21 / 1.20.x** — applies as-is (matches the original artifact).
* **1.19.4** — applies with one adaptation: `Queue.QueueFamilyIndices` uses `Integer`
  (null = unset) on 1.19.4, so `QueueMixin` needs `null` checks instead of `-1`.
* **1.18.2 / 1.19.2** — the queue/device structure differs (no `Queue#findQueueFamilies`),
  so these need a deeper, per-version port.

The in-tree source (`src/main/…`), synced with upstream `dev` (latest Minecraft), uses the
minimal Android surface (no `QueueMixin`, since present-support probing is already in
upstream, and no stale `bindBuffers` overwrite).

## Usage

```bash
scripts/build-version.sh 1.21     # applies this patch to upstream 1.21 and builds
```
