package net.vulkanmod.android.mixin;

import net.vulkanmod.android.AndroidSwapChain;
import net.vulkanmod.vulkan.Drawer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.IntBuffer;

import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * Android present handling for the 1.19.4/1.18.x/1.19.2 renderer, where image acquisition
 * and presentation live in {@code Drawer#drawFrame}.
 *
 * <p>With pre-rotation active, {@code VK_SUBOPTIMAL_KHR} is reported by the driver every
 * time the surface is rotated; it is treated as success instead of triggering a swapchain
 * recreation.
 */
@Mixin(Drawer.class)
public class DrawerMixin {

    @Redirect(
            method = "drawFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/KHRSwapchain;vkAcquireNextImageKHR(Lorg/lwjgl/vulkan/VkDevice;JJJJLjava/nio/IntBuffer;)I"
            ),
            remap = false
    )
    private int checkSubOptimalSwapChain(VkDevice device, long swapchain, long timeout,
                                         long semaphore, long fence, IntBuffer pImageIndex) {
        return checkSubOptimalSwapChain(
                vkAcquireNextImageKHR(device, swapchain, timeout, semaphore, fence, pImageIndex));
    }

    @Redirect(
            method = "drawFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/KHRSwapchain;vkQueuePresentKHR(Lorg/lwjgl/vulkan/VkQueue;Lorg/lwjgl/vulkan/VkPresentInfoKHR;)I"
            ),
            remap = false
    )
    private int checkSubOptimalSwapChain(VkQueue queue, VkPresentInfoKHR pPresentInfo) {
        return checkSubOptimalSwapChain(vkQueuePresentKHR(queue, pPresentInfo));
    }

    private int checkSubOptimalSwapChain(int vkResult) {
        if (vkResult == VK_SUBOPTIMAL_KHR && AndroidSwapChain.hasPreRotation) {
            return VK_SUCCESS;
        }
        return vkResult;
    }
}
