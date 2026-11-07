#!/usr/bin/env python3
"""Generate TYFINO's own silent, static SDR sample. Requires FFmpeg with libx264."""
import pathlib
import subprocess
import tempfile

root = pathlib.Path(__file__).resolve().parents[1]
output = root / 'app/src/debug/assets/vision-sdr.mp4'
colors = [(0, 0, 0), (64, 64, 64), (128, 128, 128), (192, 192, 192),
          (255, 255, 255), (160, 96, 80), (64, 128, 64), (64, 128, 192)]
with tempfile.TemporaryDirectory() as folder:
    image = pathlib.Path(folder) / 'bars.ppm'
    row = b''.join(bytes(color) * 40 for color in colors)
    image.write_bytes(b'P6\n320 180\n255\n' + row * 180)
    output.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-loop', '1',
                    '-framerate', '10', '-i', str(image), '-t', '4', '-an',
                    '-c:v', 'libx264', '-profile:v', 'baseline', '-level', '3.0',
                    '-x264-params', 'colorprim=bt709:transfer=bt709:colormatrix=bt709',
                    '-pix_fmt', 'yuv420p', '-color_range', 'tv', '-color_primaries', 'bt709',
                    '-color_trc', 'bt709', '-colorspace', 'bt709', '-movflags', '+faststart', str(output)],
                   check=True)
print(output)
