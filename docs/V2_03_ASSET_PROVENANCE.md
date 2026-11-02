# V2-03 자산 제작 기록

## 귀로꾼 재질

도구: 내장 imagegen. 외부 이미지 입력은 사용하지 않았다.
파일: src/main/resources/assets/whileaway/textures/entity/wayfarer/workcoat-v2.png
상태: ORIGINAL CANDIDATE. 실제 모델 화면의 미적 품질은 HUMAN NOT TESTED다.
원본 출력은 그대로 복사했다. 이미지 크기나 픽셀을 후처리하지 않았다.
모델은 128×128 논리 UV 좌표를 사용한다. 실제 atlas는 정사각형이다.
상단 왼쪽은 작업복, 오른쪽은 가면 재질이다.
하단 왼쪽은 장화, 오른쪽은 녹슨 금속·보수 천이다.
바닐라 엔티티 재질과 음수 entity-ID 재질 분기를 대체한다.

### 사용한 최종 프롬프트

```text
Production game asset for an original Minecraft railway horror mod, WHILE YOU WERE AWAY. Generate ONE square opaque PNG material texture atlas, straight-on flat 2D, no perspective, no character render, no text, no labels, no border, no watermarks. Atlas coordinates treated as 128 by 128 logical pixels, exact four equal quadrants with hard seams at x64 y64. TOP LEFT: worn charcoal-indigo railway worker's wool coat material, subtle woven square pixels, seams, muted oxidized copper buttons, salt and rain wear, broadly consistent dark material. TOP RIGHT: pale weathered linen and chalk-gray stitched mask material, subtle irregular diagonal black stitch seam, not a face portrait, no eyes, no teeth, no blood. BOTTOM LEFT: near-black cracked boot leather with cold gray scuffs, low contrast, directional tread wear. BOTTOM RIGHT: dark desaturated rusty steel and frayed slate-blue work cloth. Art style deliberately crafted low-resolution voxel-game pixel texture, crisp pixel clusters, restrained 128-pixel-style detail, no smooth photorealism, no gradients or baked lighting, no shiny effects. This atlas will skin a bent humanoid who follows an abandoned return route: believable working clothes and a blank worn cloth mask. Original design; no existing Minecraft mob skins or other franchise imagery.
```

## 원본 음향

도구: tools/generate-wayfarer-audio.py / seed 203 / 44,100Hz mono Vorbis.
Python 3.12, numpy와 soundfile을 사용한다.
wayfarer_drag·wayfarer_warning·wayfarer_depart를 합성한다.
외부 녹음·상업 음원·다른 작품의 샘플을 사용하지 않는다.
재생 없이 디코딩·길이·채널·유한값·peak를 검사한다.
AUDIO EXPERIENCE와 청각적 공포감은 NOT TESTED다.

## 구조물과 렌더

구조물 NBT는 프로젝트 코드로 제작했다.
바닐라 블록과 바닐라 블록 재질 참조를 사용한다.
Minecraft 원본 블록 텍스처 파일을 재배포하지 않는다.
새 가로보는 weathered_cut_copper 모델을 고정 충돌 지형 위에 렌더한다.
장면 설명과 생성 조건은 V2_03_CONTENT_STATUS.md를 참조한다.

Atlas SHA-256: c121c6e2c0650830618a302ee8fc5a40b65a6a3a28567c6e7f410b7325e4a82c
실제 크기: (1254, 1254)
