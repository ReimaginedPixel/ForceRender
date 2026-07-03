package net.reimaginedpixel.forcerender.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.reimaginedpixel.forcerender.ForceRenderConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Companion to {@link ItemTranslucencyMixin} for standard items.
 *
 * <p>Most items in the world (dropped items, item-frame contents, and items held
 * or worn by armor stands) are drawn from the block atlas via
 * {@code TexturedRenderLayers.getItemTranslucentCull}, not the per-texture layer
 * in {@link net.minecraft.client.render.RenderLayers}.  Swapping that atlas layer
 * for the matching {@code getEntityCutout} layer — same atlas texture, cutout
 * instead of translucent — moves those items into the depth-writing cutout pass
 * so they composite correctly against water and glass.  See
 * {@link ItemTranslucencyMixin} for the full rationale and trade-off.
 */
@Environment(EnvType.CLIENT)
@Mixin(TexturedRenderLayers.class)
public class ItemAtlasTranslucencyMixin {

    @Inject(method = "getItemTranslucentCull", at = @At("HEAD"), cancellable = true)
    private static void forceCutoutForAtlasItems(CallbackInfoReturnable<RenderLayer> callbackInfo) {
        if (ForceRenderConfig.enabled && ForceRenderConfig.fixTranslucency) {
            callbackInfo.setReturnValue(TexturedRenderLayers.getEntityCutout());
        }
    }
}
