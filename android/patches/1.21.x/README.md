# Android patch — 1.21.x (modern renderer)

Applies to upstream branches **`1.21`** and **`1.20.x`** (Minecraft 1.21 / 1.20.4).

These versions have the post-refactor renderer layout:

* `net.vulkanmod.vulkan.Renderer` (image acquire + present in `beginFrame`/`submitFrame`)
* `net.vulkanmod.vulkan.framebuffer.SwapChain` (`createSwapChain`)
* `net.vulkanmod.vulkan.queue.Queue` (`findQueueFamilies`)
* `net.vulkanmod.render.chunk.buffer.DrawBuffers` (six-argument `bindBuffers`)

This patch is a faithful source reconstruction of the original VulkanMod-Android 1.21.5
jar (which shipped without sources):

| File | Role |
| --- | --- |
| `AndroidSwapChain.java` | Surface-transform / pre-rotation helpers |
| `mixin/SwapChainMixin.java` | Captures the surface transform, forces `preTransform = IDENTITY` |
| `mixin/RendererMixin.java` | Treats `VK_SUBOPTIMAL_KHR` as success while pre-rotation is active |
| `mixin/QueueMixin.java` | Queue-family selection via surface present support (kept for fidelity; upstream 1.20.x+ already probes present support) |
| `mixin/DrawBuffersMixin.java` | Pins the six-argument `bindBuffers` implementation |
| `mixin/FramebufferMixin.java` | Shadows `Framebuffer#width`/`height` (as in the original jar) |
| `mixin/RendererAccessor.java` | Accessor for `Renderer` (present in the jar but unregistered) |
| `mixin/VRenderSystemMixin.java` | Applies the surface pre-rotation to the projection matrix (completes the original jar's latent intent) |

`vulkanmod.android.mixins.json` registers the active mixins.
