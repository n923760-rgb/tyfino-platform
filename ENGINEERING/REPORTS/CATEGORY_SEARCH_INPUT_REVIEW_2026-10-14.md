# Category search input/accessibility review

Verified official source: main12a3eb9c81cec2114f1d53fedeb77e26f9c672bb, default main, clean local source, no open PRs. PR200 exact-head Validate38504637753 passed all seven jobs and130 cases per device; official-main38506454322 jobs were all completed SUCCESS at this gate. Reviewed source includes recent player size/track/EPG viewport, Favorites guidance and category picker/search; physical/product release acceptance is not inferred from their source/CI.

Owner requests continued improvement and review of completed work. Standing reviewed-merge authorization remains conditional on all seven exact-head PASS. Task: strengthen the existing category search's native input/RTL/accessibility surface, with no new product subsystem.

## Change and boundaries

The existing search field exposes localized matching-count/loading/no-match stateDescription and a polite live region, without another visible row or another input target. It does not merge or replace child category labels, change search ownership, move focus on results, load provider data or change licensing/identity/signing. Loading never invents zero matches; successful counts derive only from the current published/filtered list. Semantics metadata is not proof of audible TalkBack behavior.

Three native scenarios join all130 previous cases (133 expected per device): actual filtered Dialog focus/Enter with exact choice; native Back after hiding IME with no selection; production Arabic/font2 renderer at280x320dp with actual text-density proof, exact count/no-match semantics, full Close bounds and actual pointer click. Fixture rendering is not physical TV/OEM IME/provider/TalkBack/performance proof.

Local SDK/Gradle runtime remains BLOCKED. Existing exact-source Actions must qualify builds, unit/lint, all seven jobs and exact configured APK API33 installation before merge. No required gates weakened, skipped or blindly retried. Full source/self-review, local XML/resource parity and governance validation are recorded on the PR. No independent review or production deployment/publication/signing is claimed. Physical comprehensive owner testing remains deferred.

## First qualification correction

Head dd146ef064147d00ada236e71991c6eaca4e161b / Validate38508139807: five non-instrumentation jobs PASS, both instrumentation jobs FAIL during compileDebugAndroidTestKotlin, before any cases ran. Full phone115671495275 and tablet115671495237 logs identify the same first cause: unresolved click at CatalogCategoryPickerTest.kt305. The new pointer test omitted the androidx.compose.ui.test.click extension import. Add that import only, preserving the actual pointer action, full-bounds assertion and production code. This is a test-source correction, not a product defect, runner retry or runtime PASS. Exact corrected-head qualification follows separately on the PR.
