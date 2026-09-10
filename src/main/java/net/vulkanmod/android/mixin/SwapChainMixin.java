package net.vulkanmod.android.mixin;

import net.vulkanmod.android.AndroidSwapChain;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static org.lwjgl.vulkan.VK10.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;

/**
 * Android pre-rotation support for the swapchain.
 *
 * <ul>
 *   <li>{@link #setupPrerotation} records the surface transform reported by the driver.</li>
 *   <li>{@link #injExtent} forces the swapchain to the identity transform so the image is
 *       never rotated by the compositor.</li>
 * </ul>
 */
@Mixin(SwapChain.class)
public class SwapChainMixin {

    @Inject(
            method = "createSwapChain",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/vulkanmod/vulkan/framebuffer/SwapChain;getExtent(Lorg/lwjgl/vulkan/VkSurfaceCapabilitiesKHR;)Lorg/lwjgl/vulkan/VkExtent2D;",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            remap = false
    )
    private void setupPrerotation(CallbackInfo ci, MemoryStack stack, VkDevice device,
                                  DeviceManager.SurfaceProperties surfaceProperties,
                                  VkSurfaceFormatKHR surfaceFormat, int presentMode) {
        AndroidSwapChain.setupTransform(surfaceProperties.capabilities);
    }

    @Redirect(
            method = "createSwapChain",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/VkSwapchainCreateInfoKHR;preTransform(I)Lorg/lwjgl/vulkan/VkSwapchainCreateInfoKHR;"
            ),
            remap = false
    )
    private VkSwapchainCreateInfoKHR injExtent(VkSwapchainCreateInfoKHR instance, int transform) {
        // Always present in the identity orientation. The compositor will not rotate the
        // image; rotation is handled on our side (see AndroidSwapChain).
        return instance.preTransform(VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR);
    }
}
