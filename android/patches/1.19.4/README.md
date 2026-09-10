# Android patch — 1.19.4

Applies to upstream branch **`1.19.4`** (Minecraft 1.19.4).

1.19.4 sits between the old and new renderer layouts:

* `net.vulkanmod.vulkan.SwapChain` (`createSwapChain(int)`) — the pre-`framebuffer` package
* `net.vulkanmod.vulkan.Drawer` (`drawFrame`) — image acquire + present
* `net.vulkanmod.vulkan.queue.Queue` — already probes present support upstream, so no queue
  mixin is needed here

| File | Role |
| --- | --- |
| `AndroidSwapChain.java` | Surface-transform / pre-rotation helpers |
| `mixin/SwapChainMixin.java` | Captures the surface transform, forces `preTransform = IDENTITY` |
| `mixin/DrawerMixin.java` | Treats `VK_SUBOPTIMAL_KHR` as success while pre-rotation is active |
