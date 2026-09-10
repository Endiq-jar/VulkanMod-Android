package net.vulkanmod.android.mixin;

import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.queue.Queue;
import net.vulkanmod.vulkan.queue.Queue.QueueFamilyIndices;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Android queue-family selection.
 *
 * <p>On Android (GLFW/EGL surfaces) a dedicated present queue is frequently not exposed
 * the same way it is on desktop. This overwrite selects queue families by probing present
 * support through {@code vkGetPhysicalDeviceSurfaceSupportKHR}, and falls back to the
 * graphics queue for transfers and the compute queue for presents when needed.
 *
 * <p>Note: upstream has since merged equivalent present-support probing into
 * {@code Queue#findQueueFamilies} (1.20.x+), so on recent versions this overwrite is
 * redundant. It remains useful for 1.19.4 / 1.20.x forks.
 */
@Mixin(Queue.class)
public class QueueMixin {

    @Overwrite(remap = false)
    public static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {
        QueueFamilyIndices indices = new QueueFamilyIndices();

        try (MemoryStack stack = stackPush()) {
            IntBuffer queueFamilyCount = stack.ints(0);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.mallocStack(queueFamilyCount.get(0), stack);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

            IntBuffer presentSupport = stack.ints(0);

            for (int i = 0; i < queueFamilies.capacity(); i++) {
                int queueFlags = queueFamilies.get(i).queueFlags();

                if ((queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.graphicsFamily = i;

                    vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Vulkan.getSurface(), presentSupport);

                    if (presentSupport.get(0) == VK_TRUE) {
                        indices.presentFamily = i;
                    }
                }
                else if ((queueFlags & VK_QUEUE_GRAPHICS_BIT) == 0
                        && (queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                    indices.computeFamily = i;
                }
                else if ((queueFlags & (VK_QUEUE_COMPUTE_BIT | VK_QUEUE_GRAPHICS_BIT)) == 0
                        && (queueFlags & VK_QUEUE_TRANSFER_BIT) != 0) {
                    indices.transferFamily = i;
                }

                if (indices.presentFamily == -1) {
                    vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Vulkan.getSurface(), presentSupport);

                    if (presentSupport.get(0) == VK_TRUE) {
                        indices.presentFamily = i;
                    }
                }

                if (indices.isComplete())
                    break;
            }

            if (indices.presentFamily == -1) {
                // Some drivers will not report present support even if a queue supports it.
                // Fall back to the compute queue.
                indices.presentFamily = indices.computeFamily;
                Initializer.LOGGER.warn("Using compute queue as present fallback");
            }

            // No dedicated transfer queue: choose another one, preferably different from the
            // already selected queues.
            if (indices.transferFamily == -1) {
                int transferIndex = -1;

                for (int i = 0; i < queueFamilies.capacity(); i++) {
                    int queueFlags = queueFamilies.get(i).queueFlags();

                    if ((queueFlags & VK_QUEUE_TRANSFER_BIT) != 0) {
                        if (transferIndex == -1)
                            transferIndex = i;

                        if ((queueFlags & VK_QUEUE_GRAPHICS_BIT) == 0) {
                            indices.transferFamily = i;

                            if (i != indices.computeFamily)
                                break;

                            transferIndex = i;
                        }
                    }
                }

                if (transferIndex == -1)
                    transferIndex = indices.graphicsFamily;

                if (transferIndex == -1)
                    throw new RuntimeException("Failed to find queue family with transfer support");

                indices.transferFamily = transferIndex;
            }

            if (indices.computeFamily == -1) {
                for (int i = 0; i < queueFamilies.capacity(); i++) {
                    int queueFlags = queueFamilies.get(i).queueFlags();

                    if ((queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                        indices.computeFamily = i;
                        break;
                    }
                }
            }

            if (indices.graphicsFamily == -1)
                throw new RuntimeException("Unable to find queue family with graphics support.");
            if (indices.presentFamily == -1)
                throw new RuntimeException("Unable to find queue family with present support.");
            if (indices.computeFamily == -1)
                throw new RuntimeException("Unable to find queue family with compute support.");

            return indices;
        }
    }
}
