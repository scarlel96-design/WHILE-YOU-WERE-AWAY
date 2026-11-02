"""Synthesize original railway cloth, warning and receding footsteps. No playback occurs."""
from pathlib import Path
import json
import numpy as np
import soundfile as sf
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources/assets/whileaway'
RATE=44100
rng=np.random.default_rng(203)
def foot(seconds):
    t=np.arange(int(seconds*RATE))/RATE
    noise=rng.normal(0,1,len(t));noise=np.convolve(noise,np.ones(17)/17,mode='same')
    envelope=(1-np.exp(-t*65))*np.exp(-t*7)
    return (noise*.7+np.sin(2*np.pi*91*t)*.11)*envelope

def generate():
    drag=foot(.8)
    t=np.arange(int(2.15*RATE))/RATE
    warning=(np.sin(2*np.pi*(156*t+5*t*t))+.35*np.sin(2*np.pi*239*t))*np.sin(np.pi*t/2.15)**2
    warning+=np.convolve(rng.normal(0,1,len(t)),np.ones(31)/31,mode='same')*.23*np.sin(np.pi*t/2.15)**2
    departure=np.zeros(int(3.6*RATE))
    for start,gain in [(.05,.8),(.74,.5),(1.62,.25),(2.65,.10)]:
        piece=foot(.7)*gain;i=int(start*RATE);departure[i:i+len(piece)]+=piece
    metadata=json.loads((ROOT/'evidence/audio-assets.json').read_text(encoding='utf-8'))
    sounds=json.loads((R/'sounds.json').read_text(encoding='utf-8'))
    for name,samples in [('wayfarer_drag',drag),('wayfarer_warning',warning),('wayfarer_depart',departure)]:
        samples=samples/(max(np.max(np.abs(samples)),1e-9))*.52
        sf.write(R/'sounds'/f'{name}.ogg',samples,RATE,format='OGG',subtype='VORBIS')
        metadata[name]={'seconds':len(samples)/RATE,'channels':1,'source':'Original deterministic synthesis; tools/generate-wayfarer-audio.py'}
        sounds[name]={'subtitle':'subtitle.whileaway.'+name,'sounds':[{'name':'whileaway:'+name,'stream':False}]}
    (ROOT/'evidence/audio-assets.json').write_text(json.dumps(metadata,indent=2)+'\n',encoding='utf-8')
    (R/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n',encoding='utf-8')
    labels={'en_us':{'wayfarer_drag':'Uneven dragging steps','wayfarer_warning':'A strained brace creaks','wayfarer_depart':'Steps recede along the track'},'ko_kr':{'wayfarer_drag':'불균형한 끌림과 발걸음','wayfarer_warning':'버티던 관절이 삐걱거림','wayfarer_depart':'선로 쪽으로 멀어지는 발걸음'}}
    for lang,entries in labels.items():
        path=R/'lang'/f'{lang}.json';d=json.loads(path.read_text(encoding='utf-8'))
        d.update({'subtitle.whileaway.'+k:v for k,v in entries.items()});path.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('PASS synthesized original mono OGG: drag / warning / departure; playback=NONE')
if __name__=='__main__':generate()
