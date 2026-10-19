package net.vulkanmod.android;

import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;

import java.nio.FloatBuffer;

import static org.lwjgl.vulkan.KHRSurface.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_SURFACE_TRANSFORM_ROTATE_180_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_SURFACE_TRANSFORM_ROTATE_270_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

/**
 * Android-specific swap-chain pre-rotation support.
 *
 * <p>On Android the surface orientation is reported through
 * {@link VkSurfaceCapabilitiesKHR#currentTransform()}. Instead of asking the
 * compositor to rotate the final image (which is done by setting the swapchain's
 * {@code preTransform} to {@code currentTransform}), we create the swapchain with the
 * identity transform and apply the rotation ourselves. This is the technique described in
 * <a href="https://android-developers.googleblog.com/2020/02/handling-device-orientation-efficiently.html">Google's
 * "Handling device orientation efficiently"</a> article and avoids a compositor copy on
 * every present.
 *
 * <p>Pre-rotation is only enabled when the surface actually advertises
 * {@code VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR} as a supported transform. Drivers that do
 * not support it would reject the swapchain creation with {@code VK_ERROR_INITIALIZATION_FAILED},
 * which used to crash the game on launch. In that case the compositor transform is kept and
 * pre-rotation is disabled, which is exactly the behaviour of a non-Android build.
 *
 * <p>The whole class is defensive on purpose: a failure in the optional pre-rotation path
 * must never abort rendering.
 *
 * <p>This class was reconstructed from the decompiled VulkanMod-Android 1.21.5 artifact
 * (the original port shipped without sources).
 */
public class AndroidSwapChain {

    /** Mask of the transform bits that describe a rotation (identity excluded). */
    private static final int ROTATION_MASK =
            VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR | VK_SURFACE_TRANSFORM_ROTATE_180_BIT_KHR | VK_SURFACE_TRANSFORM_ROTATE_270_BIT_KHR;

    /** Raw transform reported by the driver, as passed to {@code VkSwapchainCreateInfoKHR#preTransform}. */
    private static int surfaceTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;

    /** Rotation reported by the driver (identity masked out). */
    public static int currentTransform;

    /**
     * Rotation matrix applied to the projection matrix to pre-rotate the rendered frame.
     */
    public static Matrix4f preRotateMat = new Matrix4f();

    /** True when we own the rotation (identity swapchain + rotated projection). */
    public static boolean hasPreRotation = false;

    /** Scratch extent reused by {@link #getTransformExtent(int, int)}. */
    public static Extent extent = new Extent();

    private AndroidSwapChain() {
    }

    /**
     * Captures the current surface transform and derives the pre-rotation state.
     */
    public static void setupTransform(VkSurfaceCapabilitiesKHR capabilities) {
        setupTransform(capabilities.currentTransform(), capabilities.supportedTransforms());
    }

    /**
     * Captures the surface transform (as passed to
     * {@code VkSwapchainCreateInfoKHR#preTransform(int)}) and derives the pre-rotation state,
     * assuming that an identity swapchain is supported.
     */
    public static void setupTransform(int transform) {
        setupTransform(transform, VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR);
    }

    /**
     * Captures the surface transform and decides whether pre-rotation can be used.
     *
     * @param transform           the transform reported by the driver
     * @param supportedTransforms the transform bits the surface supports
     */
    public static void setupTransform(int transform, int supportedTransforms) {
        try {
            surfaceTransform = transform;
            currentTransform = transform & ROTATION_MASK;

            if (currentTransform == 0) {
                // Identity surface: every launcher used on Android presents like this.
                preRotateMat.identity();
                hasPreRotation = false;
                return;
            }

            if ((supportedTransforms & VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR) == 0) {
                // The driver refuses identity swapchains; keep the compositor rotation
                // instead of failing vkCreateSwapchainKHR.
                Initializer.LOGGER.warn(
                        "Surface does not support identity preTransform (supported: 0x{}), keeping the compositor transform",
                        Integer.toHexString(supportedTransforms));
                preRotateMat.identity();
                hasPreRotation = false;
                return;
            }

            switch (currentTransform) {
                case VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR -> preRotateMat.rotationZ((float) Math.toRadians(90.0f));
                case VK_SURFACE_TRANSFORM_ROTATE_180_BIT_KHR -> preRotateMat.rotationZ((float) Math.toRadians(180.0f));
                case VK_SURFACE_TRANSFORM_ROTATE_270_BIT_KHR -> preRotateMat.rotationZ((float) Math.toRadians(270.0f));
                default -> preRotateMat.identity();
            }

            hasPreRotation = true;
        } catch (Throwable t) {
            // The Android layer is optional: never let it take the game down.
            preRotateMat.identity();
            hasPreRotation = false;
            Initializer.LOGGER.warn("Failed to set up Android pre-rotation, falling back to the compositor transform", t);
        }
    }

    /**
     * @return the value to pass to {@code VkSwapchainCreateInfoKHR#preTransform}. Identity
     *         when we handle the rotation ourselves, the driver transform otherwise.
     */
    public static int swapchainPreTransform() {
        if (hasPreRotation)
            return VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
        return surfaceTransform;
    }

    /**
     * Queries the transforms supported by the current surface.
     *
     * @return the supported transform bits, or {@code VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR}
     *         if the query fails (so that pre-rotation stays enabled)
     */
    public static int querySupportedTransforms() {
        try {
            VkPhysicalDevice physicalDevice = DeviceManager.physicalDevice;
            long surface = Vulkan.getSurface();

            if (physicalDevice == null || surface == VK_NULL_HANDLE)
                return VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;

            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
                vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, capabilities);
                return capabilities.supportedTransforms();
            }
        } catch (Throwable t) {
            Initializer.LOGGER.warn("Unable to query surface transform support", t);
            return VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
        }
    }

    /**
     * Applies the pre-rotation matrix to the given projection matrix in place.
     *
     * @param projection the projection matrix to rotate
     */
    public static void applyPreRotation(Matrix4f projection) {
        preRotateMat.mul(projection, projection);
    }

    /** Scratch matrix reused by {@link #applyPreRotation(FloatBuffer)} to avoid allocations. */
    private static final Matrix4f TEMP = new Matrix4f();

    /**
     * Applies the pre-rotation matrix to a column-major 4x4 matrix stored in a
     * {@link FloatBuffer} (16 floats), in place. A no-op when the surface transform is
     * identity.
     *
     * @param columnMajor buffer holding the matrix; its position is restored on return
     */
    public static void applyPreRotation(FloatBuffer columnMajor) {
        if (!hasPreRotation)
            return;

        try {
            int pos = columnMajor.position();
            columnMajor.position(0);
            TEMP.set(columnMajor);
            preRotateMat.mul(TEMP, TEMP);
            columnMajor.position(0);
            TEMP.get(columnMajor);
            columnMajor.position(pos);
        } catch (Throwable t) {
            hasPreRotation = false;
            Initializer.LOGGER.warn("Failed to apply Android pre-rotation, disabling it", t);
        }
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
