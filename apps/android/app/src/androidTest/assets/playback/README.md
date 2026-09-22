# Credential-free playback fixtures

These files are test-only synthetic media generated from FFmpeg's `testsrc` source. They contain no IPTV provider material, credentials, hostnames, playback URLs, or third-party media.

They exist only to exercise TYFINO's real Media3 + `BoundedRedirectDataSource` path deterministically on managed Android devices.

Generated with FFmpeg 7.1.5:

```sh
ffmpeg -f lavfi -i testsrc=size=96x54:rate=10 -t 1.5 \
  -c:v libx264 -profile:v baseline -level 3.0 -pix_fmt yuv420p \
  -preset ultrafast -crf 35 -movflags +faststart -an movie.mp4

ffmpeg -f lavfi -i testsrc=size=96x54:rate=10 -t 1.5 \
  -c:v libx264 -profile:v baseline -level 3.0 -pix_fmt yuv420p \
  -preset ultrafast -crf 35 -an -f hls -hls_time 2 -hls_list_size 0 \
  -hls_segment_filename 'segment%03d.ts' index.m3u8
```

SHA-256:

- `movie.mp4`: `ddfe7bfdcd58a8810787f8334f81785ca3fc2d4fbb99d0aaee82b909150fda16`
- `live/index.m3u8`: `aa986699b27160487e71c04ae7150a9cccb6cc1634c56c48808cd770193e2215`
- `live/segment000.ts`: `06c7ee0073fd3a86cfa746087d7547e9132858c460437094ac4727525e3563c1`

Do not replace these with captured provider media.
