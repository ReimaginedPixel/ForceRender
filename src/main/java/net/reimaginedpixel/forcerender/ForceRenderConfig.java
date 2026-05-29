package net.reimaginedpixel.forcerender;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ForceRenderConfig {

    private static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("forcerender.properties");

    public static boolean enabled     = true;
    public static int     renderRange = 32;   // blocks; 1–256

    private ForceRenderConfig() {}

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) {
            save();
            return;
        }
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(CONFIG_FILE)) {
            props.load(reader);
            enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
            try {
                renderRange = clampRange(Integer.parseInt(props.getProperty("renderRange", "32")));
            } catch (NumberFormatException e) {
                ForceRenderMod.LOGGER.warn("[ForceRender] Invalid renderRange in config ({}), using default", e.getMessage());
            }
        } catch (IOException e) {
            ForceRenderMod.LOGGER.error("[ForceRender] Failed to read config, using defaults", e);
        }
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("enabled",     String.valueOf(enabled));
        props.setProperty("renderRange", String.valueOf(renderRange));
        try (var writer = Files.newBufferedWriter(CONFIG_FILE)) {
            props.store(writer, "ForceRender — edit here or use the in-game Mod Menu settings screen");
        } catch (IOException e) {
            ForceRenderMod.LOGGER.error("[ForceRender] Failed to save config", e);
        }
    }

    static int clampRange(int value) {
        return Math.max(1, Math.min(256, value));
    }
}
