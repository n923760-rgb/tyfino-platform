# Queued licensing presentation ownership

Task: TYFINO-20261031-licensing-queued-presentation (CI INVESTIGATION, then bounded correction if reproduced).
Official base: main@57664b503b7b63e556d1c3e24639df05410ff5b2. No open PRs at the initial live gate; official-main Validate41129681117 completed SUCCESS. Continued owner implementation and reviewed-merge authorization applies.

Source finding: initialize and onForeground capture presentationGeneration only after acquiring actionMutex. A request that already started but is queued behind another operation can therefore adopt a newer explicit code-entry generation and publish over it. This is a source interleaving, not observed physical-device behavior.

Diagnostic regression: suspend foreground refresh inside the fake API; start initialize UNDISPATCHED so it reaches the occupied mutex; explicitly showActivation; release refresh; require no queued initialization publications and the final ActivationEntry state. No delays or weakened predicates. Existing product source and all139 native cases remain unchanged for this diagnostic head. JVM test execution requires hosted Actions because local Kotlin/Gradle/Android SDK are unavailable.

If reproduced, capture the presentation generation at method entry, check it again after acquiring the mutex, and preserve the existing completion check. Stale queued background presentation must do no work; a later explicit initialize for cancellation must still restore authoritative state. Repository commit/session/offline rules, API endpoints, package/signing identity and IPTV data remain outside scope. Physical TV/provider/real-code acceptance, production signing/publication/deployment are NOT RUN and excluded.

Exact diagnostic and corrected source results belong to the task PR. Do not merge a failing diagnostic head or infer a physical race fix from emulator success.

Diagnostic head31e565ac2ff869dbd8edbd25036d6386fa7fe43a, Validate41144764920, Android job124367184260 reproduced the new queuedInitializationCannotReplaceNewerActivationEntry assertion failure:202 tests,1 failure. The201 other JVM cases passed; API/admin/deployment-config/backup-restore passed. Native jobs were skipped after the expected Android failure, not qualified on that diagnostic source. Full Android log reviewed.

Bounded correction: capture generation before waiting on actionMutex in initialize/onForeground; reject stale queued work immediately after acquisition, retaining completion ownership checks. The regression also requires a later explicit initialize to restore Active state, so cancellation remains usable. Required corrected-head qualification: all seven gates,202 JVM cases and all139 unchanged cases/device. Physical acceptance remains NOT RUN.
