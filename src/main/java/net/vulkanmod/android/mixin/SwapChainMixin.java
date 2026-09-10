package net.vulkanmod.android.mixin;

import net.vulkanmod.android.AndroidSwapChain;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.lwjgl.vulkan.KHRSurface.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;

/**
 * Android pre-rotation support for the swapchain.
 *
 * <p>The driver reports the surface orientation through the {@code preTransform} argument
 * of {@link VkSwapchainCreateInfoKHR#preTransform(int)}. We capture that transform (for
 * {@link AndroidSwapChain#hasPreRotation}) and force the swapchain to the identity
 * transform so the compositor never rotates the image; rotation is handled on our side
 * instead (see {@link AndroidSwapChain}).
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
