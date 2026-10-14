# GALAKTOGON Android Home Widget (Prototype)

This is an **isolated Android Studio project** for a real Android home-screen widget. It does not change the deployed GAP website.

## Current scope

- Signs in with an existing GAP Supabase email/password account.
- Reads that account's `galaktogon_data` row using the authenticated access token.
- Shows open SA (`tasks`), SP (`exams`) and Quests (`quests`), up to seven entries.
- Includes manual refresh and opens the widget app when tapped.
- Stores the session tokens in app-private SharedPreferences; the password is not stored.
- Attempts to refresh an expired access token using the Supabase refresh token.

## Important beta limitations

- This source has **not yet been built or tested on a real device**. Do not treat it as a released or verified APK.
- The widget reads the signed-in user's own data row. Parent mode / selecting a linked child's data is not implemented.
- Completion-state detection currently recognizes common fields (`completed`, `done`, `isDone`, `complete`, or `status=completed`). Before release, verify these against the exact task objects produced by GAP.
- Android's widget refresh interval is system-controlled (configured to 30 minutes); tap ↻ for a manual refresh.
- The Supabase project must allow authenticated users to select their own `galaktogon_data` row under its existing Row Level Security policies. This project does not change database policies.
- No changes have been made to the live GAP site or database.

## Build and install for testing

1. Open this `android-widget` directory in Android Studio.
2. Let Android Studio sync Gradle; use JDK 17 and Android SDK 35.
3. Build the debug APK and install it on your Android phone.
4. Open **GAP Widget**, sign in with the same GAP account used on the website.
5. Long-press the Android home screen, choose **Widgets**, find **GAP Widget**, and place it.
6. Verify the displayed SA, SP and Quests against GAP before relying on the widget.

Do not share the generated APK publicly until login, RLS behavior, task completion fields, token refresh and widget refresh have been tested.

## Project structure

- `app/src/main/java/de/galaktogon/gaphomewidget/LoginActivity.kt` — sign-in screen
- `app/src/main/java/de/galaktogon/gaphomewidget/GapWidgetProvider.kt` — widget and data refresh
- `app/src/main/res/layout/gap_widget.xml` — widget UI
- `app/src/main/res/xml/gap_widget_info.xml` — Android widget metadata
