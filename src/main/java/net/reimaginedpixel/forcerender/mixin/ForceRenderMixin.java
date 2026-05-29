package net.reimaginedpixel.forcerender.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.reimaginedpixel.forcerender.ForceRenderConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bypasses frustum culling for armor stands and item frames that are within
 * the player-configured range.  Vanilla uses the entity's physical bounding
 * box for the frustum test, which is far smaller than the custom display
 * models that resource packs (e.g. ItemsAdder) attach — causing those models
 * to pop out of view as soon as the tiny box leaves the screen.
 */
@Environment(EnvType.CLIENT)
@Mixin(EntityRenderer.class)
public class ForceRenderMixin<T extends Entity> {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void bypassCullingWithinRange(
            T entity,
            Frustum frustum,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfoReturnable<Boolean> callbackInfo) {

        if (!ForceRenderConfig.enabled) {
            return;
        }

        boolean isTargetEntity = entity instanceof ArmorStandEntity
                              || entity instanceof ItemFrameEntity;
        if (!isTargetEntity) {
            return;
        }

        double dx = entity.getX() - cameraX;
        double dy = entity.getY() - cameraY;
        double dz = entity.getZ() - cameraZ;
        double distanceSquared = dx * dx + dy * dy + dz * dz;

        double rangeLimit = ForceRenderConfig.renderRange;
        boolean withinRange = distanceSquared <= rangeLimit * rangeLimit;

        if (withinRange) {
            callbackInfo.setReturnValue(true);
        }
    }
}
