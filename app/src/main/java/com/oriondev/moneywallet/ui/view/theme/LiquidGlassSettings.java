package com.oriondev.moneywallet.ui.view.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.ColorUtils;

/** Small, isolated preference store for optional Liquid Glass presentation settings. */
public final class LiquidGlassSettings {
    private static final String FILE = "liquid_glass_settings";
    private static final String PREFIX = "glass_";
    private static final int[] DEFAULTS = {42, 34, 45, 28, 72, 100, 48, 28, 42, 100, 18, 55, 45, 30};
    private static final String[] KEYS = {"blur", "refraction_height", "refraction_amount", "tint",
            "vibrancy", "brightness", "gloss", "corner_radius", "bounce", "speed",
            "compression", "pull_stretch", "long_press", "haptics_strength"};
    private static final String[] TOGGLES = {"depth", "chromatic", "release_haptics"};
    private static final boolean[] TOGGLE_DEFAULTS = {true, false, true};

    private LiquidGlassSettings() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static int get(Context context, String key) {
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(key)) {
                try { return clamp(prefs(context).getInt(PREFIX + key, DEFAULTS[i]), 0, 100); }
                catch (RuntimeException ignored) { return DEFAULTS[i]; }
            }
        }
        return 0;
    }

    public static boolean isEnabled(Context context, String key) {
        for (int i = 0; i < TOGGLES.length; i++) {
            if (TOGGLES[i].equals(key)) {
                try { return prefs(context).getBoolean(PREFIX + key, TOGGLE_DEFAULTS[i]); }
                catch (RuntimeException ignored) { return TOGGLE_DEFAULTS[i]; }
            }
        }
        return false;
    }

    public static void put(Context context, String key, int value) {
        prefs(context).edit().putInt(PREFIX + key, clamp(value, 0, 100)).apply();
    }

    public static void put(Context context, String key, boolean value) {
        prefs(context).edit().putBoolean(PREFIX + key, value).apply();
    }

    public static void reset(Context context, boolean motionOnly) {
        SharedPreferences.Editor editor = prefs(context).edit();
        for (int i = 0; i < KEYS.length; i++) {
            boolean motion = i >= 8;
            if (motion == motionOnly) editor.putInt(PREFIX + KEYS[i], DEFAULTS[i]);
        }
        for (int i = 0; i < TOGGLES.length; i++) {
            boolean motionToggle = "release_haptics".equals(TOGGLES[i]);
            if (motionToggle == motionOnly) {
                editor.putBoolean(PREFIX + TOGGLES[i], TOGGLE_DEFAULTS[i]);
            }
        }
        editor.apply();
    }

    /** Applies the supported glass surface properties to every themed CardView in the app. */
    public static void applyTo(CardView card, int baseColor) {
        Context context = card.getContext();
        int tint = get(context, "tint");
        int vibrancy = get(context, "vibrancy");
        int brightness = get(context, "brightness");
        int overlay = Color.argb(Math.round(12 + tint * 0.48f), 133, 104, 255);
        int surface = ColorUtils.blendARGB(baseColor, overlay, tint / 100f * 0.34f);
        surface = ColorUtils.blendARGB(surface, Color.WHITE, brightness / 100f * 0.12f);
        surface = ColorUtils.blendARGB(surface, baseColor, (100 - vibrancy) / 100f * 0.08f);
        card.setCardBackgroundColor(surface);
        float density = context.getResources().getDisplayMetrics().density;
        card.setRadius((12 + get(context, "corner_radius") * 0.20f) * density);
        card.setCardElevation((isEnabled(context, "depth") ? 2 + get(context, "gloss") * 0.035f : 1) * density);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
