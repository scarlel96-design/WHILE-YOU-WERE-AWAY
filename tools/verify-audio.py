"""Decode without playback. Requires numpy and soundfile; never opens an audio device."""
import json
from pathlib import Path
import numpy as np
import soundfile as sf

root = Path(__file__).resolve().parents[1]
metadata = json.loads((root / 'evidence/audio-assets.json').read_text(encoding='utf-8'))
for name, expected in metadata.items():
    samples, rate = sf.read(root / f'src/main/resources/assets/whileaway/sounds/{name}.ogg', always_2d=True)
    assert rate == 44100, name
    assert samples.shape[1] == expected['channels'], name
    assert abs(len(samples) / rate - expected['seconds']) < 0.002, name
    assert np.isfinite(samples).all(), name
    assert 0 < np.max(np.abs(samples)) < 0.95, name
print(f'PASS audio files={len(metadata)}; decoded/finite/duration/mono/44100Hz/peak<0.95; playback=NONE')
