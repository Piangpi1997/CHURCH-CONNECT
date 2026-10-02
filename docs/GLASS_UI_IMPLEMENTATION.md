# Glass UI implementation

The Android Compose interface now uses a shared CMF Glass theme while keeping the visual layer separate from repository, authentication, registration, payment-review, QR-verification, and permission logic.

## Visual system

- Light and dark palettes use the existing deep-blue/cyan church identity with readable neutral text and semantic success/error colors.
- `GlassTokens` centralizes the main spacing, content widths, radii, touch-target, elevation, and motion values. `GlassTheme` supplies the matching Material 3 color and shape schemes.
- `GlassCard`, `GlassButton`, `GlassIconButton`, and `GlassBadge` are reusable surfaces/actions. Cards use a subtle edge, high-opacity translucent fill, and restrained elevation.
- The background uses a low-cost gradient; it does not use per-item blur or continuous decorative animation. The existing premium launcher artwork anchors authentication, the app bar, Home, and the digital member card.
- Content is capped on wider screens while remaining scrollable on phones. The secure QR stays on a plain white, high-contrast area and retains its existing verification payload and accessibility description.

## Screens and preserved behavior

The refreshed presentation covers the existing authentication/create-account screen, Home dashboard, registration and manual payment-reference steps, account/profile summary, digital member ID, calendar, announcements, notification inbox, More/account tools, and the Android admin/review queue. Existing bottom-navigation routes, Firebase-backed state, offline demo boundary, configured registration fees, backend-gated admin actions, localization selection, and secure QR behavior are unchanged. No new display strings or sample records were added.

## Product boundaries

This repository does not currently contain a separate in-app splash/welcome state, a member profile-photo field, or a desktop/web admin application. The redesign reuses the Android OS launch experience, renders only profile data already present in the model, and styles the existing Android admin queue rather than fabricating those missing products. Tedim remains the project's English-until-reviewed fallback; this UI update does not claim a new translation.

## Verification

See [`BUILD_STATUS.md`](BUILD_STATUS.md) for the current automated test and build results. Device-specific font-scaling, TalkBack, QR scanning, and screenshot checks require an attached Android emulator or physical test device; automated compilation alone does not establish those checks.
