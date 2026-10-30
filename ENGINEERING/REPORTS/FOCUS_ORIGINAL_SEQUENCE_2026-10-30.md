# Restore the original directional regression sequence

Task: TYFINO-20261030-focus-original-sequence (CI INVESTIGATION).
Base: official main028b001db77b8213342342e8d27cd1cb1769cb0f.
Owner requests implementation of the30 October weekly review: causal resolution of #205, then physical Release Candidate qualification before production signing/publication.

Source review confirms #208 inserted Espresso window observation and semantics snapshots between the first Fit-focus assertion and Down injection. Those additional synchronization checkpoints can affect timing; successful instrumented runs cannot establish absence of the original race.

Restore the exact original assertion/Down chain. Capture the Zoom-focus boolean before collecting any new window diagnostics. Only after a failed captured verdict, collect a clearly labeled post-failure window/focus snapshot. Diagnostic errors must not replace the captured false verdict. Retain Enter-only selection, reopening/Back behavior, all138 cases and the seven-gate workflow with retained artifacts. Product code is unchanged. Post-failure snapshots are later state, not a transition trace proving what happened before failure.

Local diff check and hosted exact-source outcomes belong to the PR result. Local Android runtime is unavailable; use existing GitHub Actions. A successful run alone is not a fix, and a diagnostic failure needs full evidence review before changing production code. #205 remains open. Physical phone/TV/provider/accessibility/performance and production signing/publication remain separate gates.
