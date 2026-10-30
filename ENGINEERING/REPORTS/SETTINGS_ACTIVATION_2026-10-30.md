# Activate a trial license from Settings

Task: TYFINO-20261030-settings-activation (IMPLEMENTATION).
Official base: main28e440c91369a2faaed2bdae4a3d523d7dc07503.
Owner explicitly requests an Activate app button in Settings to enter an activation code during the seven-day trial.

The trial license panel now exposes localized Activate app / تفعيل التطبيق. Explicit callbacks through the app shell open the existing activation-code screen. Its existing normalization, code clearing, secure repository and server endpoint remain authoritative. A valid server grant replaces the trial immediately; there is no client bypass or trial extension. Paid licenses omit the trial-upgrade action. Saved Xtream credentials/catalog/history are not removed. Returning may recreate the browsing shell, while stored account data remains intact.

Back from this Settings entry reloads authoritative local licensing state and refreshes if due, rather than offering a new trial. Hardware Back is consumed while submitting. Failure offers an explicit return action. Foreground refresh does not replace the Settings code-entry screen; the controller checks presentation generation before publishing earlier refresh/initialization results. Production API already supports activation for an installation with a trial; backend source is unchanged.

Explicit activation failures no longer fall back to an offline trial Active outcome. They show the existing safe failure/retry UI, retaining a usable trial snapshot unless the existing terminal failure policy invalidates it. Normal refresh offline policy is unchanged. The existing code input is in-memory only, cleared on valid submission, and disposed on Back; it is never added to persistence or logs.

Validation: JVM cases cover immediate trial-to-paid replacement, failed activation preserving the trial without success fallback, return-state restoration, and an in-flight refresh rejected after explicit code entry. Native Settings coverage checks scrollable touch/Enter activation and removal after a paid grant. All previous tests remain. Exact-head hosted build/unit/lint/native results belong to the PR. Local SDK/Gradle runtime unavailable. Physical TV/provider/real-code acceptance remains NOT RUN; signing/release/deployment are excluded.
