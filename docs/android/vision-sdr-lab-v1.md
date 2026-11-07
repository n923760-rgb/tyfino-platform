# TYFINO Vision SDR laboratory v1

Status: EXPERIMENTAL DEBUG ONLY — no live-playback or release qualification.

Owner request: investigate a distinctive optional viewing enhancement. This first bounded slice is a Settings experiment using a bundled, silent, self-generated BT.709 SDR sample. The normal Live/Movie/Series player, provider transport, account ownership, licensing, resume state, and release UI remain unchanged. Release has a no-op entry; the effect dependency, shader, sample and localized experiment UI exist only in Debug.

## Image and controls

Original is the default. Balanced applies one mild monotone curve to Media3's linear SDR RGB, with strength 0–100 in 25-point steps. Compare with original temporarily uses identity processing without losing the chosen strength; zero strength is identity too. State is memory-only and resets on exit. It is neither true HDR nor AI upscaling, denoising, sharpening or invented detail. Subjective improvement remains unqualified.

For channel x in [0,1] and strength s in [0,1], y = x + 0.35s x(1-x)(x-0.18). The derivative is positive throughout that interval (minimum at x=1,s=1 is 0.713). Black/white endpoints and channel ordering are preserved; this is not a calibrated display transform. HDR is rejected before custom shader allocation. The only accepted input is the fixed bundled SDR asset; there is no arbitrary file/network picker or provider playback route.

Media3 1.11.0's renderer creates its GPU video sink even when initialized with an empty effect list. Consequently Original inside the experiment is an identity GPU path, **not a direct-decoder/performance bypass**. Normal playback never invokes setVideoEffects and retains its existing direct surface path. No performance claim follows from the small shader.

## Ownership and failure

One local ExoPlayer belongs to the visible experiment. Settings has no concurrent playback destination. The video repeats silently, with no transport controls or persisted state. A single custom shader is registered before prepare for the whole session. Controls publish a validated atomic scalar; the GL thread reads it once per frame and updates its uniform. Original/comparison use zero in that same shader. Controls do not replace effects, re-register streams, prepare or replace media. A processing/player error stops playback, disables effect controls, and leaves Close/Back available with focus recovery. Background/stop dismisses the experiment; disposal detaches the view, removes observers/listeners and releases the player. No retries or automatic HDR fallback are introduced.

## Qualification

Automated coverage includes profile bounds, identity/comparison behavior, actual controls, English keyboard selection, Arabic RTL with 2x fonts in a short viewport, recoverable failure controls, and rejection of HDR before GL allocation. A real decoded-surface PixelCopy test must observe a nonblank known SDR sample, the Balanced pixel delta, preserved black/white endpoints, return to Original/zero, unchanged player/media and no decoder restart during adjustment, followed by decoder release on hardware Back. Software-rendered emulator evidence proves only that synthetic path.

Before extending this to real playback: record subjective A/B comparisons on varied SDR media, real TCL/phone/low-power GL compatibility, dropped frames and frame pacing, startup latency, memory/CPU/GPU/power/thermal behavior, and subtitles/aspect/orientation/lifecycle behavior. Define and qualify an actual direct-decoder Off/recovery path and HDR/DRM bypass before integrating. Physical TCL is owner-deferred. No signing, release, tag or deployment is authorized by this experiment.

Fixture: run `python3 apps/android/scripts/generate-vision-fixture.py` with FFmpeg/libx264. It generates four seconds of 320x180,10fps H.264 baseline/yuv420p with explicit limited-range BT.709 matrix/transfer/primaries, eight synthetic color bars, no audio and no external media. Tool version affects encoded bytes; commit the generated fixture and qualify its exact hash.

Source references inspected at pinned version:

- https://github.com/androidx/media/blob/1.11.0/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/video/MediaCodecVideoRenderer.java
- https://github.com/androidx/media/blob/1.11.0/libraries/effect/src/main/java/androidx/media3/effect/BaseGlShaderProgram.java
- https://github.com/androidx/media/blob/1.11.0/libraries/effect/src/main/java/androidx/media3/effect/RgbMatrix.java
