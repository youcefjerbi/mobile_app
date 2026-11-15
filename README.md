# CarePulse — Animated React Mobile Prototype

> **Part of [CareCatalyst](https://github.com/HandanMajied2024/CareCatalyst)** (Junction 2026 · AstraZeneca *Equal Care Across Finland*).
> Concept B = the vision: everyone has a personal health agent. Concept A (today, the county Care Pathway Agent): [HandanMajied2024/CareCatalyzer](https://github.com/HandanMajied2024/CareCatalyzer).
> All people and data are synthetic. The agent never diagnoses. MIT licensed, see [LICENSE](LICENSE).

The current frontend is a **React 19 + Vite** mobile app prototype in `frontend/`. It uses a blue 2D visual design with an animated SVG companion, character illustrations, chat transitions, typing indicators, bottom sheets, and an animated coordination timeline. No backend is implemented or required.

```sh
npm install
npm run dev
```

Open **http://127.0.0.1:4174**. The app fills a phone screen; on desktop it appears in a mobile-sized frame.

- **Today:** personal companion, next care step, email/calendar entry points, and remembered daily life.
- **My agent:** scripted contextual conversation and optional browser read-aloud.
- **Care plan:** proposed visit and accessible transport, recipient permissions, and sequential simulated coordination.
- **My life:** editable personal context; changes clear earlier permissions and the simulated plan. Includes an animations switch.
- **Connections:** sample Gmail/Google Calendar or Outlook flows, fictional inbox messages and calendar events, message details, and per-service/all-account disconnect controls. No OAuth, tokens, account access, email sending or calendar writes.

All data stays in React memory and resets on reload. Sample inbox/calendar content is not shared with simulated institutional agents. The prototype makes no network calls for account data or conversational AI. Local font and SVG illustration assets are bundled. Browser read-aloud uses the browser/OS speech engine and is optional.

Animations respect `prefers-reduced-motion` and the in-app setting. Modal dialogs use native focus trapping and Escape dismissal. Forms have labels, required fields and length limits. No cancer diagnosis or risk score is calculated; proposed care, transport and benefit availability remain unverified.

```sh
npm test        # model, consent, recipient isolation and sample data checks
npm run build  # production frontend build in dist/
npm run preview
```

The previous Android implementation and browser companion are retained below for reference. They are separate from this React prototype. No changes are uploaded to the original AI Studio project.

---


Native Android patient application written in Kotlin and Jetpack Compose, with Google authentication and a Firebase Firestore repository. The project also includes a separate, interactive browser demo for reviewing the patient experience without an emulator.

## Implemented

- Google sign-in and sign-out; existing provider accounts are rejected without changing their role.
- Separate sample-patient mode that does not write demo records to Firebase.
- Today dashboard with date-aware medication progress and the next upcoming visit.
- Add daily medications, mark a dose taken, undo the mark, and remove an entry after confirmation.
- Record existing appointments, inspect details, and mark visits completed.
- Edit patient name, insurance provider, allergies, and emergency contact while preserving other profile fields.
- Firestore listeners, loading states, retry on record-loading failures, save-in-progress controls, and save/error feedback.
- Input validation for required values, field lengths, medication time, and calendar-valid appointment dates.
- Gradle wrapper launchers and wrapper JAR restored; local debug builds use Android's default debug signing when the AI Studio debug keystore is absent.

## Run Android

1. Open this directory in Android Studio with a compatible JDK and install the SDK requested by `app/build.gradle.kts` (compile SDK 36.1).
2. Set `sdk.dir` in local.properties if Android Studio does not configure it automatically.
3. Sync Gradle. Network access is required for the pinned Gradle 9.3.1 distribution and Maven dependencies.
4. Use the supplied Firebase project configuration only if you have access to that project. Otherwise replace `app/google-services.json` and the Firestore database/App Check resources with your own project settings.
5. For live sign-in, enable Google authentication and register the signing certificate fingerprints in Firebase. Register the development App Check debug token for debug builds; configure the existing release App Check integration before release testing.
6. Run `./gradlew :app:testDebugUnitTest :app:assembleDebug`, then install the generated `app/build/outputs/apk/debug/app-debug.apk` on a device or emulator.

The patient UI does not need a Gemini API key. The inherited AI service remains unconnected to these screens.

Release signing uses the existing KEYSTORE_PATH, STORE_PASSWORD, and KEY_PASSWORD environment configuration. No signing keys are included. Existing Firestore rules are supplied but were not deployed by this implementation work.

## Personal agent pitch journey

The Junction pitch is implemented as a separate fictional mobile app experience in both the browser companion and Android. The browser presents a phone-sized app with bottom navigation at every viewport, and fills the screen on a phone. Conversation, care plan, permission and profile views use single-column layouts and touch targets of at least 44px. In Android, choose **Explore personal agent journey** on the sign-in screen; no authentication is required, and the journey never reads or writes live patient records.

- Overview introduces Eero in Espoo, his accessibility and transport barriers, his dog Jenny, and bingo.
- My agent offers an explicitly scripted conversation about the sample follow-up invitation, transport, appointment timing, and remembered context. Browser read-aloud is optional when supported.
- Care plan proposes a next-day 08:00 follow-up at a fictional Helsinki care centre with a 07:00 transport request. Appointment availability, return travel and travel support remain unverified.
- Each simulated clinic, insurance and transport handoff requires a separate selection, displays its exact data categories, and appears in a local coordination log. Clinic permission gates the simulation; other recipients are optional.
- My situation edits update the agent context and clear earlier permissions and coordination results. No personal data is persisted by the browser pitch. Android demo state can survive activity recreation but is not a permanent record.

No risk score is calculated. No real institution or agent is contacted, no benefits are verified, and no booking, purchase or transport request is sent. The demo is intentionally offline and uses sample data. External clinical/insurance/transport integrations and live conversational AI remain future work.

Run `node scripts/pitch-checks.cjs` to check navigation, consent gating, recipient isolation, duplicate prevention, context changes, permission reset, escaping and scripted responses.

## Browser companion

Open `preview/index.html` in a browser to explore the personal agent pitch. For local serving, run `python3 -m http.server 4173 --bind 127.0.0.1` and visit `http://127.0.0.1:4173/preview/`. The earlier routine tracker implementation remains in `preview/app.js`, with its existing logic checks; the new pitch is in `preview/pitch.js` and `preview/pitch.css`. All browser data is fictional and resets on reload.

Run browser logic checks with `node scripts/preview-checks.cjs` (Node 22 or another working recent Node installation).

## Verification on this machine

- Passed: standalone Kotlin checks for daily rollover, dose status, required fields, length limits, 24-hour time, leap years, and strict date parsing.
- Passed: Kotlin syntax parsing for all 19 Kotlin source files. This is not Android compilation/type checking.
- Passed: browser logic checks for progress, undo, record creation/removal, validation, visit completion, profile escaping, navigation, and modal rendering.
- Passed: shell syntax validation of the Gradle launcher.
- Blocked: full Android build and Android/JUnit execution. The wrapper attempt failed downloading Gradle with UnknownHostException for services.gradle.org; no Android SDK is installed here.
- Not verified: live Firebase sign-in, App Check, Firestore integration, emulator/device UI, or visual browser rendering. Existing Firestore emulator tests were not executed.

## Release limits

This is a patient MVP source handoff, not a verified production release or signed APK. Before distribution, build and run on a device; test Firebase authorization, two-account isolation, offline/reconnect behavior, midnight rollover, form rotation, and large-font layouts.

Medication tracking supports one daily dose per entry. It is not a full dose history, reminder scheduler, or prescription-management system. Demo state lasts for the ViewModel lifetime on Android and until reload in the browser. Live records persist through the existing Firestore repository; synchronization requires a correctly configured backend. If a save times out, check records before retrying because Firestore may still synchronize a pending write.

Appointments are personal records: adding or completing one does not book or cancel a visit, notify a provider, or send messages. Provider/insurer workflows, AI recommendations, insurance verification, calendar/email syncing, and notifications are not exposed in this MVP.

The older repository helpers and AI/coordination service remain in the source for future development. They have not been production-validated as part of this handoff.
