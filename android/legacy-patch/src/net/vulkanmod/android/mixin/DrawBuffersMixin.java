package net.vulkanmod.android.mixin;

import net.vulkanmod.render.chunk.buffer.AreaBuffer;
import net.vulkanmod.render.chunk.buffer.DrawBuffers;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.shader.Pipeline;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT16;
import static org.lwjgl.vulkan.VK10.nvkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer;

/**
 * Pins the (1.21-era) {@code bindBuffers} implementation used by the Android port.
 *
 * <p>This is a straight copy of the upstream method for the old six-parameter signature.
 * Upstream has since changed {@code bindBuffers} (it now takes a section-data UBO and fade
 * uniforms), so this overwrite only applies to the older version branches.
 */
@Mixin(DrawBuffers.class)
public class DrawBuffersMixin {

    @Shadow
    private AreaBuffer indexBuffer;

    @Shadow
    public abstract AreaBuffer getAreaBuffer(TerrainRenderType r);

    @Shadow
    public abstract void updateChunkAreaOrigin(VkCommandBuffer commandBuffer, Pipeline pipeline,
                                               double camX, double camY, double camZ, MemoryStack stack);

    @Overwrite(remap = false)
    public void bindBuffers(VkCommandBuffer commandBuffer, Pipeline pipeline,
                            TerrainRenderType terrainRenderType,
                            double camX, double camY, double camZ) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            AreaBuffer vertexBuffer = getAreaBuffer(terrainRenderType);
            nvkCmdBindVertexBuffers(commandBuffer, 0, 1,
                    stack.nlong(vertexBuffer.getId()), stack.nlong(0L));
            updateChunkAreaOrigin(commandBuffer, pipeline, camX, camY, camZ, stack);
        }

        if (terrainRenderType == TerrainRenderType.TRANSLUCENT && this.indexBuffer != null) {
            vkCmdBindIndexBuffer(commandBuffer, this.indexBuffer.getId(), 0, VK_INDEX_TYPE_UINT16);
        }
    }
}
