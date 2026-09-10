package net.vulkanmod.android;

import org.joml.Matrix4f;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;

import static org.lwjgl.vulkan.KHRSurface.VK_SURFACE_TRANSFORM_ROTATE_180_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_SURFACE_TRANSFORM_ROTATE_270_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR;

/**
 * Android-specific swap-chain pre-rotation support.
 *
 * <p>On Android the surface orientation is reported through
 * {@link VkSurfaceCapabilitiesKHR#currentTransform()}. Instead of asking the
 * compositor to rotate the final image (which is done by setting the swapchain's
 * {@code preTransform} to {@code currentTransform}), we create the swapchain with the
 * identity transform and apply the rotation ourselves. This is the technique described in
 * <a href="https://android-developers.googleblog.com/2020/02/handling-device-orientation-efficiently.html">
 * Google's "Handling device orientation efficiently"</a> article and avoids a compositor
 * copy on every present.
 *
 * <p>This class was reconstructed from the decompiled VulkanMod-Android 1.21.5 artifact
 * (the original port shipped without sources).
 */
public class AndroidSwapChain {

    /** Surface transform reported by the driver, masked to the ROTATE_90/180/270 bits. */
    public static int currentTransform;

    /**
     * Rotation matrix applied to the projection matrix to pre-rotate the rendered frame.
     * Note: the reference 1.21.5 artifact never populated this matrix (it stayed identity
     * and {@link #applyPreRotation} was effectively a no-op because the launchers it
     * targeted - PojavLauncher et al. - present with an identity surface transform).
     */
    public static Matrix4f preRotateMat = new Matrix4f();

    /** True when the surface is rotated (a non-identity transform was reported). */
    public static boolean hasPreRotation = false;

    /** Scratch extent reused by {@link #getTransformExtent(int, int)}. */
    public static Extent extent = new Extent();

    private AndroidSwapChain() {
    }

    /**
     * Captures the current surface transform and derives the pre-rotation state.
     */
    public static void setupTransform(VkSurfaceCapabilitiesKHR capabilities) {
        setupTransform(capabilities.currentTransform());
    }

    /**
     * Captures the surface transform (as passed to
     * {@code VkSwapchainCreateInfoKHR#preTransform(int)}) and derives the pre-rotation state.
     */
    public static void setupTransform(int transform) {
        currentTransform = transform & 0b1110; // ROTATE_90 | ROTATE_180 | ROTATE_270

        // Complete the rotation matrix. The reference artifact shipped this as identity;
        // it is populated here so that pre-rotation works on launchers that do expose a
        // rotated surface. This is a no-op while hasPreRotation stays false.
        switch (currentTransform) {
            case VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR -> preRotateMat.rotationZ((float) Math.toRadians(90.0f));
            case VK_SURFACE_TRANSFORM_ROTATE_180_BIT_KHR -> preRotateMat.rotationZ((float) Math.toRadians(180.0f));
            case VK_SURFACE_TRANSFORM_ROTATE_270_BIT_KHR -> preRotateMat.rotationZ((float) Math.toRadians(270.0f));
            default -> preRotateMat.identity();
        }

        hasPreRotation = currentTransform != 0;
    }

    /**
     * Applies the pre-rotation matrix to the given projection matrix in place.
     *
     * @param projection the projection matrix to rotate
     */
    public static void applyPreRotation(Matrix4f projection) {
        preRotateMat.mul(projection, projection);
    }

    /**
     * Returns the transformed (swapped, if rotated 90/270) width and height.
     */
    public static Extent getTransformExtent(int width, int height) {
        if (currentTransform == VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR
                || currentTransform == VK_SURFACE_TRANSFORM_ROTATE_270_BIT_KHR) {
            extent.width = height;
            extent.height = width;
        }
        else {
            extent.width = width;
            extent.height = height;
        }

        return extent;
    }

    /**
     * Transforms a viewport rectangle defined in swapchain space
     * ({@code x}, {@code y}, {@code width}, {@code height}) into the matching rectangle in
     * framebuffer space ({@code framebufferWidth} x {@code framebufferHeight}), taking the
     * surface rotation into account.
     */
    public static Extent getTransformExtent(int framebufferWidth, int framebufferHeight,
                                            int x, int y, int width, int height) {
        switch (currentTransform) {
            case VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR -> {
                extent.x = framebufferWidth - height - y;
                extent.y = x;
                extent.width = height;
                extent.height = width;
            }
            case VK_SURFACE_TRANSFORM_ROTATE_180_BIT_KHR -> {
                extent.x = framebufferWidth - width - x;
                extent.y = framebufferHeight - height - y;
                extent.width = width;
                extent.height = height;
            }
            case VK_SURFACE_TRANSFORM_ROTATE_270_BIT_KHR -> {
                extent.x = y;
                extent.y = framebufferWidth - width - x;
                extent.width = height;
                extent.height = width;
            }
            default -> {
                extent.x = x;
                extent.y = y;
                extent.width = width;
                extent.height = height;
            }
        }

        return extent;
    }

    /**
     * A 2D rectangle. Reused as scratch storage to avoid per-frame allocations.
     */
    public static class Extent {
        public int x;
        public int y;
        public int width;
        public int height;
    }
}
