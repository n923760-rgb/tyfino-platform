# Playback native-view disposal

## Task and live gate

Task `TYFINO-20261114-player-view-detach`, IMPLEMENTATION, 2026-11-14 UTC. Owner requests continued TYFINO development; standing scoped reviewed-merge-after-success authority applies. No signing, release, publication or deployment. Controller/executor Codex; sequential self-review, not independent review.

Verified repository `n923760-rgb/tyfino-platform`, official/default `main`, HEAD `e7bbc8331310d80268fbafe37442921d22b4fb48`, tree `b4ef53c1550e0385534611f8655acc2a911f64da`. No open/conflicting PRs; clean local source matched official HEAD before task branch `fix/player-view-detach-20261114`. Baseline official Validate42881765185 passed all seven jobs,167 cases/device and configured API33 installation; exact source evidence is on merged#230.

Read root AGENTS, project profile, Android README, canonical roadmap and playback ownership/lifecycle contract. Only root AGENTS applies. Local Git/source/shell available; qualified local JDK17/Android SDK/emulator and distribution access remain unavailable. Hosted existing CI supplies native execution evidence.

## Confirmed source gap

The playback contract requires the destination's native PlayerView to detach from its external player on exit. `PlaybackVideoView` updates its binding while mounted but supplies no AndroidView release unbinding. A removed native view can therefore retain the player reference/listeners while that view remains referenced; no measured Activity leak or OEM failure is claimed.

Pinned Media3 source inspected: `https://github.com/androidx/media/blob/1.11.0/libraries/ui/src/main/java/androidx/media3/ui/PlayerView.java`. `setPlayer(null)` removes the view listener and clears its video surface, controller and subtitle bindings; there is no PlayerView `onDetachedFromWindow` override performing this unbinding. Native window detachment alone is not the explicit player-unbinding contract.

## Bounded change

Add `onRelease = { view -> view.player = null }` to the existing AndroidView. Binding updates, native controls, aspect modes, transport, account/resume authority and externally owned ExoPlayer release remain unchanged. This callback detaches the discarded native view; it does not claim a new ordering guarantee between the screen's player release and Compose disposal.

One actual Compose/PlayerView regression mounts and discards the view three times with one externally owned ExoPlayer. A ForwardingPlayer delegates real behavior and observes listener registration/release calls. Retain each discarded view strongly, then require its player reference is null, all wrapper-observed native view/control listeners are removed, each new native view differs from discarded views and the external engine has not been released by the view. Finally release the external engine exactly once from its owner. The fixture has no media and does not qualify decoder or Activity garbage collection behavior; retained existing media/recreation cases supply the affected baseline.

Scope: one product callback, one native test, this report and canonical roadmap. All167 prior native cases remain unchanged;168/device expected. No dependency, CI, package, production endpoint, signing or release-policy change.

## Validation and result

PASS: all six local governance validators, whitespace and complete four-file diff/secret review. Existing167 native cases are retained unchanged. Require existing seven-job exact-head Validate,168 cases/device and configured API33 install/launch before acceptance. Record exact head/tree and hosted results on the task PR. No pre-change runtime FAIL is claimed; source inspection identifies the gap and the new native case must prove disposal behavior.

Local Android execution remains BLOCKED by previously verified distribution network failure and absent qualified SDK/JDK17/emulator; no redundant unchanged-environment provisioning attempt is claimed. Stop dependent mutation for source drift, conflict, secrets or required failure; inspect complete evidence and preserve original assertions. No blind retry or weakened verdict.

Physical TCL remains owner-deferred; real-provider/media, physical/OEM lifecycle, low-memory, TalkBack and performance acceptance remain separate. This callback is not a memory benchmark or a claim that all lifecycle leaks are fixed. Vision remains the isolated Debug SDR laboratory under its existing contract.
