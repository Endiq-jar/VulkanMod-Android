package net.vulkanmod.android.mixin;

import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for {@code Renderer}'s private members.
 *
 * <p>Present in the reference 1.21.5 artifact but never registered in
 * {@code vulkanmod.android.mixins.json}, so it is currently unused. Kept for completeness.
 */
@Mixin(Renderer.class)
public interface RendererAccessor {

    @Accessor("boundFramebuffer")
    Framebuffer getBoundFramebuffer();

    @Accessor("recordingCmds")
    boolean isRecordingCmds();
}
