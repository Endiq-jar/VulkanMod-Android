package net.vulkanmod.android.mixin;

import net.vulkanmod.android.AndroidSwapChain;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Android pre-rotation support for the swapchain.
 *
 * <p>The driver reports the surface orientation through the {@code preTransform} argument
 * of {@link VkSwapchainCreateInfoKHR#preTransform(int)}. We capture that transform (for
 * {@link AndroidSwapChain#hasPreRotation}) and force the swapchain to the identity
 * transform so the compositor never rotates the image; rotation is handled on our side
 * instead (see {@link AndroidSwapChain}).
 *
 * <p>Identity is only used when the surface actually supports it. When it does not, the
 * driver transform is passed through untouched and pre-rotation stays disabled, so the
 * swapchain is never created with an unsupported transform (which would fail with
 * {@code VK_ERROR_INITIALIZATION_FAILED} and crash the game).
 *
 * <p>{@code require = 0} keeps the mod loadable if a future upstream version changes the
 * swapchain creation code: the injection is then simply skipped and rendering falls back
 * to the compositor rotation.
 */
@Mixin(SwapChain.class)
public class SwapChainMixin {

    @Redirect(
            method = "createSwapChain",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/VkSwapchainCreateInfoKHR;preTransform(I)Lorg/lwjgl/vulkan/VkSwapchainCreateInfoKHR;"
            ),
            remap = false,
            require = 0
    )
    private VkSwapchainCreateInfoKHR redirectPreTransform(VkSwapchainCreateInfoKHR instance, int transform) {
        AndroidSwapChain.setupTransform(transform, AndroidSwapChain.querySupportedTransforms());
        return instance.preTransform(AndroidSwapChain.swapchainPreTransform());
    }
}
