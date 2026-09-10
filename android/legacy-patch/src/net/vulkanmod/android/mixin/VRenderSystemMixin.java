package net.vulkanmod.android.mixin;

import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Shadows the projection-matrix buffer.
 *
 * <p>In the reference 1.21.5 artifact this mixin only declared the {@code projectionMatrix}
 * shadow plus a scratch {@code temp} matrix and contained no injections (and was never
 * registered in {@code vulkanmod.android.mixins.json}). It is the natural hook point for
 * applying {@code AndroidSwapChain#applyPreRotation} to the projection matrix. Kept for
 * completeness and as documentation of that intent.
 */
@Mixin(VRenderSystem.class)
public class VRenderSystemMixin {

    @Shadow
    private static MappedBuffer projectionMatrix;

    private static final Matrix4f temp = new Matrix4f();
}
