"""스토리보드 덱(deck/) → PDF + 미리보기. 슬라이드를 한 페이지로 이어 붙여 헤드리스 Chrome 으로 출력한다.
사용: python docs/storyboard/source/render.py   (Pillow 필요, Chrome 은 CHROME 환경변수로 바꿀 수 있음)
결과: docs/storyboard/TUTIPAL-storyboard.pdf, 임시 폴더에 4장씩 모은 미리보기 sheetN.png
덱 이미지는 Artifact 에 올린 /_blob/<id> 를 쓰므로, 아래 BLOBS 가 그 id → screens/ 파일 이름 짝 (캡처를 새로 올리면 갱신).
"""
import json
import os
import re
import subprocess
import tempfile

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__)).replace('\\', '/')
DECK = HERE + '/../deck/project/'
SHOTS = HERE + '/../screens/'
PDF = HERE + '/../TUTIPAL-storyboard.pdf'
CHROME = os.environ.get('CHROME', r'C:\Program Files\Google\Chrome\Application\chrome.exe')
OUT = tempfile.mkdtemp(prefix='storyboard-')
BLOBS = dict(zip(
    ['c2d380bb8f387f6eb73e62f1b9084ef0', '0cd5bad35b5e1d814a128095b42af137', '4d1a75d986c5844657e10a5baecd5fe4',
     'ac1a322a8185eb2bfc3f68d4f73c77c5', '2d002f4da9bbbe01a711880bb5ae2df6', 'c94b0a0d5a8e9298d7088f9af4253044',
     '582cb6f8910b42cc2e73c3468496213d', '4dd8900cb675493b8543ac1479cf96ec', 'a1c4a381cc8d2b6ae2777f907c87afb3',
     '300d3d62ce4ba88110e3b3b89c6a4bd8', '41e43d44ec56ce6ca7145b430efa9e8c', 'cd9424a35a826649364a557b75ab11e1',
     'eaaeac619c9939fc2cef309c0aa24384', 'fa267210475bbd9ef15cd5a5abfc1d34', '80b72b97c6227d531398d7de3086ba1a',
     'f9661a0ee18c246acd167cb4c08e1ece', 'c5cc388ea58d384172493a449f0047f8', 'c0c9bef01f2596706280985a4f81a50e',
     '0333bd014aede79720855f3d58180a78', '797aa6bb68403b5a865713d6da7c0174', '035848c13f2f6285fc56dd915873d1a4',
     '3d3db73d6d178b5201a6d04d35b16700', '3ddb5aa853c80abcc468670f218ef6d6', 'e905a997e24182b156a8259104a45cf7',
     'aaef037ff88dafbaacceefa4ce7709a3'],
    sorted(os.listdir(SHOTS))))

BASE = """* { margin: 0 } body { font: 32px/1.2 sans-serif }
body > section { width: 1920px !important; height: 1080px !important; overflow: hidden !important; box-sizing: border-box !important; position: relative; isolation: isolate }
section[hidden], aside { display: none !important }
h1 { font-size: 96px; font-weight: 600 } h2 { font-size: 64px; font-weight: 600 } h3 { font-size: 44px; font-weight: 600 }
p, li, h1, h2, h3 { overflow-wrap: break-word }
.body, .body-col, .flow-grid { min-height: 0 }"""

deck = json.load(open(DECK + 'deck.json', encoding='utf-8'))
css = open(DECK + 'deck.css', encoding='utf-8').read()
parts = []
for sid in deck['order']:
    html = open(DECK + f'slides/{sid}.html', encoding='utf-8').read()
    html = re.sub(r'/_blob/([0-9a-f]{32})', lambda m: 'file:///' + SHOTS + BLOBS[m[1]], html)
    parts.append(html)
font = next(iter(deck['faces'].values()))['href']
page = f'<!doctype html><meta charset="utf-8"><link rel="stylesheet" href="{font}"><style>{BASE}\n{css}</style>' + '\n'.join(parts)
open(os.path.join(OUT, 'deck.html'), 'w', encoding='utf-8').write(page)

n = len(deck['order'])
full = os.path.join(OUT, 'full.png')
url = 'file:///' + os.path.join(OUT, 'deck.html').replace('\\', '/')
chrome = [CHROME, '--headless=new', '--disable-gpu', '--allow-file-access-from-files', f'--user-data-dir={OUT}/profile',
          '--hide-scrollbars', '--virtual-time-budget=8000']
subprocess.run(chrome + [f'--window-size=1920,{1080 * n}', f'--screenshot={full}', url], check=True, timeout=120)
# PDF: 한 쪽 = 슬라이드 한 장 (1920x1080)
open(os.path.join(OUT, 'print.html'), 'w', encoding='utf-8').write(page.replace(
    '</style>', '\n@page { size: 1920px 1080px; margin: 0 } body > section { break-after: page }\n'
    'html { -webkit-print-color-adjust: exact; print-color-adjust: exact }</style>', 1))
subprocess.run(chrome + ['--no-pdf-header-footer', f'--print-to-pdf={PDF}', url.replace('deck.html', 'print.html')],
               check=True, timeout=120)
print('pdf', PDF)
img = Image.open(full)
print('full', img.size)
slides = [img.crop((0, i * 1080, 1920, (i + 1) * 1080)) for i in range(n)]
for g in range(0, n, 4):
    sheet = Image.new('RGB', (1920, 1080), 'white')
    for k, s in enumerate(slides[g:g + 4]):
        sheet.paste(s.resize((960, 540)), ((k % 2) * 960, (k // 2) * 540))
    sheet.save(os.path.join(OUT, f'sheet{g // 4 + 1}.png'))
print('preview sheets', (n + 3) // 4, 'in', OUT)
