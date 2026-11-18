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
    private static final String[] TOGGLES = {"glass_enabled", "depth", "chromatic", "release_haptics"};
    private static final boolean[] TOGGLE_DEFAULTS = {true, true, false, true};

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

    /** Persists the selected preset independently from the individual slider values. */
    public static int getSelection(Context context, String key, int fallback) {
        if (!"appearance_preset".equals(key) && !"motion_preset".equals(key)) return fallback;
        return Math.max(0, Math.min(3, prefs(context).getInt(PREFIX + key, fallback)));
    }

    public static void putSelection(Context context, String key, int value) {
        if (!"appearance_preset".equals(key) && !"motion_preset".equals(key)) return;
        prefs(context).edit().putInt(PREFIX + key, Math.max(0, Math.min(3, value))).apply();
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

    /** Applies the app-wide Liquid Glass surface treatment to every CardView in the view tree. */
    public static void applyTo(CardView card, int baseColor) {
        Context context = card.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        if (!isEnabled(context, "glass_enabled")) {
            card.setCardBackgroundColor(baseColor);
            card.setRadius(16f * density);
            card.setCardElevation(2f * density);
            if (card instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) card).setStrokeWidth(0);
            }
            return;
        }

        int tint = get(context, "tint");
        int vibrancy = get(context, "vibrancy");
        int brightness = get(context, "brightness");
        int gloss = get(context, "gloss");
        int overlay = Color.argb(Math.round(20 + tint * 0.55f), 61, 117, 242);
        int surface = ColorUtils.blendARGB(baseColor, overlay, tint / 100f * 0.42f);
        surface = ColorUtils.blendARGB(surface, Color.WHITE, brightness / 100f * 0.10f);
        surface = ColorUtils.blendARGB(surface, baseColor, (100 - vibrancy) / 100f * 0.06f);
        int surfaceAlpha = ColorUtils.calculateLuminance(baseColor) > 0.5 ? 238 : 226;
        card.setCardBackgroundColor(ColorUtils.setAlphaComponent(surface, surfaceAlpha));
        card.setRadius((16 + get(context, "corner_radius") * 0.20f) * density);
        card.setCardElevation((isEnabled(context, "depth") ? 3 + gloss * 0.045f : 1.5f) * density);
        card.setUseCompatPadding(true);

        if (card instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView materialCard =
                    (com.google.android.material.card.MaterialCardView) card;
            int strokeAlpha = Math.round(70 + gloss * 0.75f);
            int stroke = ColorUtils.setAlphaComponent(Color.parseColor("#78A8FF"), strokeAlpha);
            materialCard.setStrokeColor(android.content.res.ColorStateList.valueOf(stroke));
            materialCard.setStrokeWidth(Math.max(1, Math.round((0.6f + gloss / 100f * 0.8f) * density)));
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
