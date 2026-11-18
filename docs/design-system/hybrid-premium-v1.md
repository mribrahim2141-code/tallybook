# TallyBook Hybrid Premium — Design System v1

**Status:** Phase 1 foundation  
**Branch:** `feat/tallybook-hybrid-design-system-v1`  
**Scope:** Design tokens and implementation rules only. This phase intentionally does not restyle existing screens.

## Product direction

A premium personal-finance experience that combines the clarity and confidence of a modern fintech product with warm, calm surfaces and restrained Liquid Glass details.

- **Brand character:** confident, calm, precise, approachable.
- **Primary identity:** deep navy and royal blue.
- **Warmth:** soft ivory canvas in light mode; never pure white as the dominant page background.
- **Glass:** translucent layers, subtle edge highlights and restrained depth. Glass is decorative; text and financial values remain on opaque/high-contrast surfaces where needed.
- **Financial semantics:** green for positive/inflow, red for negative/outflow, amber for caution. Never communicate meaning by color alone.

## Token files

| Resource | Purpose |
|---|---|
| `values/tallybook_design_tokens.xml` | Light palette and shared semantic colors |
| `values-night/tallybook_design_tokens.xml` | Dark palette overrides |
| `values/tallybook_design_dimensions.xml` | Spacing, corner, touch-target, icon and type scales |

All resource names use the `tally_` prefix to avoid collisions with legacy app resources.

## Surface and mode rules

1. **Light:** warm ivory canvas, near-white content surfaces, navy text, royal-blue primary actions.
2. **Dark:** deep navy canvas, elevated blue-slate surfaces, near-white text, softer royal-blue accents.
3. **Deep Dark:** retain the app's existing black-mode behavior; the next implementation phase must explicitly decide which tokens should map to true black without reducing contrast.
4. Use one primary accent per screen; reserve semantic colors for financial meaning and status.
5. Glass surfaces should use low-to-moderate opacity, a visible but subtle outline, and restrained elevation. Avoid stacking multiple translucent layers.
6. Respect Android 24+ compatibility and provide a non-blur fallback. Never make blur a prerequisite for legibility.

## Typography and Arabic/RTL

- Preserve the app's existing font infrastructure until a bundled font is verified for licensing, APK size, and Arabic glyph coverage.
- Use the shared type scale as a sizing contract, not as a mandate to shrink existing financial values.
- Arabic and Latin numerals must remain legible; align currency values consistently and do not mirror digits or financial chart axes.
- Use start/end layout semantics and locale-aware formatting. Do not add physical left/right margins in new redesigned components.
- Keep touch targets at least 48dp.

## Component rules for subsequent phases

- **Cards:** 18–24dp radius, clear surface separation, consistent internal inset, restrained stroke/shadow.
- **Primary actions:** royal blue in light mode; accessible light-blue accent in dark mode.
- **Secondary actions:** tonal surface, not competing filled buttons.
- **Inputs:** persistent labels, clear focus/error states, comfortable vertical spacing.
- **Charts:** consistent semantic palette, readable labels, no decorative gradients that obscure values.
- **Navigation:** consistent icon weight and selected state; preserve existing destinations and behavior.
- **Motion:** short, purposeful transitions; respect system animation settings and reduced-motion preferences.

## Safety boundaries

This foundation is resource-only. It does not alter databases, migrations, repositories, financial calculations, account balances, user preferences, app identity, signing, navigation destinations, or business logic. Existing screens do not consume these tokens until a separately reviewed implementation phase.

## Acceptance checks

- XML resources parse and compile for all configured product flavors.
- Light and night resource names match one-to-one.
- No duplicate resource names.
- Contrast and actual device appearance are checked in the next visual implementation phase.
- No database or financial-domain files are changed.
