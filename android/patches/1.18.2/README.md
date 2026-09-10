# Android patch — 1.18.2 / 1.19.2

Applies to upstream branches **`1.18.2`** and **`1.19.2`**.

These are the oldest branches, with the entire device/swapchain stack inside
`net.vulkanmod.vulkan.Vulkan`:

* `Vulkan#createSwapChain()` — swapchain creation (`preTransform`)
* `Drawer#drawFrame()` — image acquire + present
* `Vulkan#findQueueFamilies()` — already probes present support upstream, so no queue mixin
  is needed here

| File | Role |
| --- | --- |
| `AndroidSwapChain.java` | Surface-transform / pre-rotation helpers |
| `mixin/VulkanMixin.java` | Captures the surface transform, forces `preTransform = IDENTITY` |
| `mixin/DrawerMixin.java` | Treats `VK_SUBOPTIMAL_KHR` as success while pre-rotation is active |
