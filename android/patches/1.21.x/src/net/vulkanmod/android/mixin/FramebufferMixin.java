package net.vulkanmod.android.mixin;

import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes the framebuffer dimensions to the Android mixins.
 *
 * <p>In the reference 1.21.5 artifact this mixin only shadowed {@code width}/{@code height}
 * and contained no injections - it was effectively dead weight (the fields are already
 * {@code protected} in {@code Framebuffer}). It is kept here for completeness.
 */
@Mixin(Framebuffer.class)
public class FramebufferMixin {

    @Shadow
    protected int width;

    @Shadow
    protected int height;
}
