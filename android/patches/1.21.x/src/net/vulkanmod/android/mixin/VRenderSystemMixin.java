package net.vulkanmod.android.mixin;

import net.vulkanmod.android.AndroidSwapChain;
import net.vulkanmod.vulkan.VRenderSystem;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies Android surface pre-rotation to the projection matrix.
 *
 * <p>When the surface is rotated (see {@link AndroidSwapChain#hasPreRotation}), the
 * uploaded projection matrix is rotated by the surface transform, so the rendered frame
 * matches the device orientation. On identity surfaces — the common case on Android
 * launchers — this is a strict no-op.
 *
 * <p>The rotation is applied with a {@code TAIL} injection instead of an {@code @Overwrite}
 * for two reasons: an overwrite turns any upstream signature change into a hard crash
 * ("Overwrite target not found"), and it conflicts with other mods that inject into the
 * same method. With {@code require = 0} a missing target simply disables pre-rotation.
 */
@Mixin(VRenderSystem.class)
public class VRenderSystemMixin {

    @Inject(
            method = "applyProjectionMatrix(Lorg/joml/Matrix4f;)V",
            at = @At("TAIL"),
            remap = false,
            require = 0
    )
    private static void vulkanmod$preRotateProjection(Matrix4f mat, CallbackInfo ci) {
        if (AndroidSwapChain.hasPreRotation) {
            AndroidSwapChain.applyPreRotation(VRenderSystem.projectionMatrix.buffer.asFloatBuffer());
        }
    }
}
