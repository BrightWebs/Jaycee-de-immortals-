package com.brightwebs.jaycee.client;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.neoforged.fml.loading.FMLPaths;

/** Local visual preferences in config/jaycee-client.properties; restart to reload edited HUD settings. */
public final class ClientPreferences {
    private static final Properties VALUES = new Properties();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("jaycee-client.properties");
    private static boolean loaded;
    private static boolean face3d;
    private static int hudX, hudY;
    private static float hudScale = 1;

    private ClientPreferences() {}

    public static synchronized boolean face3d() {
        load();
        return face3d;
    }

    public static synchronized void setFace3d(boolean value) {
        load();
        face3d = value;
        save();
    }

    public static synchronized int hudX() { load(); return hudX; }
    public static synchronized int hudY() { load(); return hudY; }
    public static synchronized float hudScale() { load(); return hudScale; }

    private static void load() {
        if (loaded) return;
        loaded = true;
        if (Files.exists(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) {
                VALUES.load(in);
            } catch (IOException | IllegalArgumentException exception) {
                LogUtils.getLogger().warn("Could not read Jaycee client preferences; using safe defaults", exception);
            }
        }
        face3d = Boolean.parseBoolean(VALUES.getProperty("face3d", "false").trim());
        hudX = (int) number("hud.x", 0, -4096, 4096);
        hudY = (int) number("hud.y", 0, -4096, 4096);
        hudScale = (float) number("hud.scale", 1, .5, 2);
        if (!Files.exists(FILE)) save();
    }

    private static double number(String key, double fallback, double min, double max) {
        try {
            double value = Double.parseDouble(VALUES.getProperty(key, Double.toString(fallback)).trim());
            return Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static void save() {
        VALUES.setProperty("face3d", Boolean.toString(face3d));
        VALUES.setProperty("hud.x", Integer.toString(hudX));
        VALUES.setProperty("hud.y", Integer.toString(hudY));
        VALUES.setProperty("hud.scale", Float.toString(hudScale));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                VALUES.store(out, "Jaycee local visuals. hud.x/y: offsets in GUI pixels; hud.scale: 0.5 to 2.0. Restart to reload.");
            }
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Could not save Jaycee client preferences", exception);
        }
    }
}
