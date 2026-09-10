package net.vulkanmod.android.mixin;

import net.vulkanmod.android.AndroidSwapChain;
import net.vulkanmod.vulkan.SwapChain;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.lwjgl.vulkan.VK10.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;

/**
 * Android pre-rotation support for the 1.19.4 swapchain
 * ({@code net.vulkanmod.vulkan.SwapChain}, which predates the
 * {@code net.vulkanmod.vulkan.framebuffer.SwapChain} refactor).
 */
@Mixin(SwapChain.class)
public class SwapChainMixin {

    @Redirect(
            method = "createSwapChain",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/VkSwapchainCreateInfoKHR;preTransform(I)Lorg/lwjgl/vulkan/VkSwapchainCreateInfoKHR;"
            ),
            remap = false
    )
    private VkSwapchainCreateInfoKHR redirectPreTransform(VkSwapchainCreateInfoKHR instance, int transform) {
        AndroidSwapChain.setupTransform(transform);
        return instance.preTransform(VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR);
    }
}
