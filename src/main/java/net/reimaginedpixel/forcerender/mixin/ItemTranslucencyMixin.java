package net.reimaginedpixel.forcerender.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.reimaginedpixel.forcerender.ForceRenderConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes translucent items (held / worn / framed) that vanish or get clipped when
 * they sit behind or near translucent blocks such as water and glass.
 *
 * <p>Vanilla renders translucent entities and translucent blocks in two separate
 * passes and only depth-sorts render layers <em>within</em> each pass, never
 * across them.  A translucent item and a translucent block therefore have no
 * defined blend order relative to one another, so the item's alpha pixels are
 * composited incorrectly — most visibly they disappear behind water or glass.
 *
 * <p>The {@code getItemEntityTranslucentCull} render layer is the one Minecraft
 * uses to draw items in the world.  Swapping it for the equivalent
 * {@code getEntityCutout} layer moves those items into the cutout pass, which is
 * alpha-tested and writes depth like an opaque surface.  Depth then resolves the
 * item against translucent blocks correctly.  The trade-off is the usual one for
 * cutout: no soft alpha fade (a pixel is either fully drawn or discarded), which
 * is imperceptible for the overwhelming majority of item textures.
 */
@Environment(EnvType.CLIENT)
@Mixin(RenderLayer.class)
public class ItemTranslucencyMixin {

    @Inject(method = "getItemEntityTranslucentCull", at = @At("HEAD"), cancellable = true)
    private static void forceCutoutForItems(
            Identifier texture,
            CallbackInfoReturnable<RenderLayer> callbackInfo) {

        if (ForceRenderConfig.enabled && ForceRenderConfig.fixTranslucency) {
            callbackInfo.setReturnValue(RenderLayer.getEntityCutout(texture));
        }
    }
}
