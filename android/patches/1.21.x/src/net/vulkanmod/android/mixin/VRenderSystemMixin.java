package net.vulkanmod.android.mixin;

import net.vulkanmod.android.AndroidSwapChain;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.FloatBuffer;

/**
 * Applies Android surface pre-rotation to the projection matrix.
 *
 * <p>When the surface is rotated (see {@link AndroidSwapChain#hasPreRotation}), the
 * projection matrix is rotated by the surface transform before it is uploaded, so the
 * rendered frame matches the device orientation. On identity surfaces — the common case on
 * Android launchers — this is a strict no-op.
 *
 * <p>This completes the intent of the reference 1.21.5 artifact, whose
 * {@code VRenderSystemMixin} only shadowed {@code projectionMatrix} and held a scratch
 * {@code Matrix4f} but never applied the rotation.
 */
@Mixin(VRenderSystem.class)
public class VRenderSystemMixin {

    @Shadow
    private static MappedBuffer projectionMatrix;

    @Overwrite(remap = false)
    public static void applyProjectionMatrix(Matrix4f mat) {
        FloatBuffer fb = projectionMatrix.buffer.asFloatBuffer();
        mat.get(fb);

        if (AndroidSwapChain.hasPreRotation) {
            AndroidSwapChain.applyPreRotation(fb);
        }
    }
}
