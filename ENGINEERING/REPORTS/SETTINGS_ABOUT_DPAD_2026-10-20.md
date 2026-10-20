# Read-only About information with directional input

Verified source: official main09c1590e3b54330f6c97923b58870691336585f1, default main, no open PRs. Postmerge Validate38571196794 completed SUCCESS. Scratch source was restored from the canonical repository after workspace maintenance, not from a stale artifact. Owner requests continued app refinement; standing reviewed merge remains conditional on all seven exact-head PASS.

Source finding: the new About version/development facts are plain non-focusable Text. This round adds explicit read-only focus stops with a visible primary outline so directional users can visit these facts. No physical-device failure or old-runtime baseline is asserted.

Each existing fact becomes individually focusable with wrapping text and8dp internal padding, retaining its text/test identity. Focus decoration is ephemeral local state; there is no click/Enter handler, network, persistence, clipboard, new setting or account/entitlement/signing data. Initial Settings account focus and provider/account callbacks remain authoritative. Version/package/signing/release policies are unchanged.

One new native scenario joins all136 baseline cases (137 expected/device). Actual Settings at280x320dp, Arabic font1 and font2, confirmed real host-window focus/keyboard input mode: Down from last provider button reaches version, Down reaches development notice, Up returns; Enter has no action, account callbacks stay zero and provider preset stays unchanged. Existing Arabic/font2 text-density and scrolling cases remain. This is managed-device/synthetic composition evidence, not physical TV/OEM/TalkBack/provider/performance proof.

Local native execution BLOCKED (no qualified SDK/Gradle). Source/XML/locale checks and six governance validators run locally; exact-source existing Actions must qualify builds/unit/lint, both137-case suites and exact owner APK API33 installation before merge. No gates weakened or skipped. Self-review is not independent review. No signing/publication/deployment; physical owner testing remains deferred.
