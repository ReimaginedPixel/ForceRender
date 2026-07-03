package net.reimaginedpixel.forcerender;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class ForceRenderMod implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("ForceRender");

    @Override
    public void onInitializeClient() {
        ForceRenderConfig.load();
        LOGGER.info("[ForceRender] Loaded — enabled={}, range={}, fixTranslucency={}",
                ForceRenderConfig.enabled, ForceRenderConfig.renderRange, ForceRenderConfig.fixTranslucency);
    }
}
