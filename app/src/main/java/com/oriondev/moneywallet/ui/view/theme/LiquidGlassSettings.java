package com.oriondev.moneywallet.ui.view.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.ColorUtils;

/** Persistent, Android-version-safe presentation settings for the app's glass surfaces. */
public final class LiquidGlassSettings {
    private static final String FILE = "liquid_glass_settings";
    private static final String PREFIX = "glass_";
    private static final int[] DEFAULTS = {42, 34, 45, 28, 72, 100, 48, 28, 42, 100, 18, 55, 45, 30};
    private static final String[] KEYS = {"blur", "refraction_height", "refraction_amount", "tint",
            "vibrancy", "brightness", "gloss", "corner_radius", "bounce", "speed",
            "compression", "pull_stretch", "long_press", "haptics_strength"};
    private static final String[] TOGGLES = {"glass_enabled", "depth", "chromatic", "release_haptics"};
    private static final boolean[] TOGGLE_DEFAULTS = {true, true, false, true};
    private static final int ROYAL_BLUE = Color.rgb(61, 117, 242);

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
            if (motionToggle == motionOnly) editor.putBoolean(PREFIX + TOGGLES[i], TOGGLE_DEFAULTS[i]);
        }
        editor.apply();
    }

    /**
     * Applies a visible translucent blue-glass treatment to themed CardViews.
     * Blur/refraction are intentionally not faked by blurring card contents; Android 12+
     * backdrop blur is not consistently available to ordinary app surfaces on all devices.
     */
    public static void applyTo(CardView card, int baseColor) {
        Context context = card.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        boolean enabled = isEnabled(context, "glass_enabled");
        if (!enabled) {
            card.setCardBackgroundColor(baseColor);
            card.setRadius(16f * density);
            card.setCardElevation(2f * density);
            if (card instanceof com.google.android.material.card.MaterialCardView) {
                com.google.android.material.card.MaterialCardView material =
                        (com.google.android.material.card.MaterialCardView) card;
                material.setStrokeWidth(0);
            }
            return;
        }

        int tint = get(context, "tint");
        int vibrancy = get(context, "vibrancy");
        int brightness = get(context, "brightness");
        int gloss = get(context, "gloss");

        // A stronger blue tint and lower surface alpha make the glass treatment visible
        // against both light and dark app backgrounds instead of looking like a solid card.
        float tintStrength = 0.18f + (tint / 100f) * 0.34f;
        int surface = ColorUtils.blendARGB(baseColor, ROYAL_BLUE, tintStrength);
        surface = ColorUtils.blendARGB(surface, Color.WHITE, brightness / 100f * 0.12f);
        surface = ColorUtils.blendARGB(surface, ROYAL_BLUE, (100 - vibrancy) / 100f * 0.10f);
        int surfaceAlpha = ColorUtils.calculateLuminance(baseColor) > 0.5 ? 214 : 198;
        card.setCardBackgroundColor(ColorUtils.setAlphaComponent(surface, surfaceAlpha));

        float radius = (20f + get(context, "corner_radius") * 0.22f) * density;
        card.setRadius(radius);
        card.setCardElevation((isEnabled(context, "depth")
                ? 5f + gloss * 0.065f : 2f) * density);

        if (card instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView material =
                    (com.google.android.material.card.MaterialCardView) card;
            int strokeAlpha = Math.round(115 + gloss * 1.05f);
            int stroke = ColorUtils.setAlphaComponent(Color.WHITE, Math.min(225, strokeAlpha));
            material.setStrokeColor(android.content.res.ColorStateList.valueOf(stroke));
            material.setStrokeWidth(Math.max(1, Math.round((1f + gloss / 100f) * density)));
        }
    }

    /**
     * Applies the same glass language to themed page surfaces, toolbars and scrolling panels.
     * This is intentionally limited to containers whose theme code already owns their background;
     * it never replaces arbitrary content backgrounds or text-bearing child views.
     */
    public static void applySurface(View view, int baseColor) {
        applySurface(view, baseColor, true);
    }

    /** Full-bleed variant for page roots and app bars that must meet the screen edges. */
    public static void applySurface(View view, int baseColor, boolean rounded) {
        Context context = view.getContext();
        // Deep Dark surfaces are intentionally pure black; keep their native ColorDrawable
        // so system-theme contrast and existing theme consumers remain compatible.
        if (baseColor == Color.BLACK) {
            view.setBackgroundColor(Color.BLACK);
            view.setElevation(0f);
            return;
        }

        if (!isEnabled(context, "glass_enabled")) {
            view.setBackgroundColor(baseColor);
            view.setElevation(0f);
            return;
        }

        float density = context.getResources().getDisplayMetrics().density;
        int tint = get(context, "tint");
        int gloss = get(context, "gloss");
        int brightness = get(context, "brightness");
        int vibrancy = get(context, "vibrancy");

        int blue = Color.rgb(61, 117, 242);
        int top = ColorUtils.blendARGB(baseColor, blue, 0.16f + tint / 100f * 0.26f);
        int bottom = ColorUtils.blendARGB(baseColor, blue, 0.24f + (100 - vibrancy) / 100f * 0.12f);
        top = ColorUtils.blendARGB(top, Color.WHITE, brightness / 100f * 0.12f);
        bottom = ColorUtils.blendARGB(bottom, Color.WHITE, brightness / 100f * 0.06f);

        int topAlpha = ColorUtils.calculateLuminance(baseColor) > 0.5 ? 238 : 232;
        int bottomAlpha = ColorUtils.calculateLuminance(baseColor) > 0.5 ? 222 : 218;
        GradientDrawable glass = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] {
                        ColorUtils.setAlphaComponent(top, topAlpha),
                        ColorUtils.setAlphaComponent(bottom, bottomAlpha)
                });
        glass.setCornerRadius((rounded ? 18f : 0f) * density);
        int strokeAlpha = Math.min(220, 105 + gloss);
        glass.setStroke(Math.max(1, Math.round(density)),
                ColorUtils.setAlphaComponent(Color.WHITE, strokeAlpha));
        view.setBackground(glass);
        view.setElevation((isEnabled(context, "depth") ? 3f + gloss * 0.035f : 0f) * density);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
