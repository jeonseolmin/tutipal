"""ERD 다시 만들기: 돌고 있는 DB(docker compose 의 db)에서 스키마를 읽어 물리 ERD, 손으로 배치한 논리 ERD 를 PNG · PDF 로.
사용 (저장소 루트에서, docker compose up 상태): python docs/erd/source/render.py
필요: Pillow, Chrome(CHROME 환경변수로 경로 변경), 인터넷(Mermaid · 글꼴 CDN).
- 물리 ERD: physical.py 가 스키마 덤프 → ERD.md(Mermaid) + 출력용 HTML. 컬럼 설명은 physical.py 의 COL_KO
- 논리 ERD: logical.py 의 엔터티 · 관계 목록 (DB 를 바꾸면 여기도 손으로 고친다)
"""
import os
import subprocess
import sys
import tempfile

from PIL import Image, ImageChops

HERE = os.path.dirname(os.path.abspath(__file__))
ERD = os.path.dirname(HERE)
CHROME = os.environ.get('CHROME', r'C:\Program Files\Google\Chrome\Application\chrome.exe')
TMP = tempfile.mkdtemp(prefix='erd-')

schema = os.path.join(TMP, 'schema.txt')
sql = open(os.path.join(HERE, 'schema.sql'), encoding='utf-8').read()
out = subprocess.run(['docker', 'compose', 'exec', '-T', 'db', 'psql', '-U', 'tutipal', '-d', 'tutipal', '-At', '-F', '|', '-c', sql],
                     check=True, capture_output=True, text=True, encoding='utf-8').stdout
open(schema, 'w', encoding='utf-8').write(out)

phys_html, logi_html = os.path.join(TMP, 'physical.html'), os.path.join(TMP, 'logical.html')
subprocess.run([sys.executable, os.path.join(HERE, 'physical.py'), schema, ERD, phys_html], check=True)
subprocess.run([sys.executable, os.path.join(HERE, 'logical.py'), logi_html], check=True)


def shot(html, png, w, h):
    subprocess.run([CHROME, '--headless=new', '--disable-gpu', f'--user-data-dir={TMP}/profile', '--hide-scrollbars',
                    '--force-device-scale-factor=1.5', f'--window-size={w},{h}', '--virtual-time-budget=10000',
                    f'--screenshot={png}', 'file:///' + html.replace('\\', '/')], check=True, timeout=120)
    return Image.open(png).convert('RGB')


def save(img, name):
    img.save(os.path.join(ERD, name + '.png'))
    img.save(os.path.join(ERD, name + '.pdf'), resolution=150)
    print(name, img.size)


# 물리: 넓은 창에 그린 뒤 빈 여백만 잘라냄
im = shot(phys_html, os.path.join(TMP, 'p.png'), 4400, 3000)
box = ImageChops.difference(im, Image.new('RGB', im.size, im.getpixel((5, 5)))).getbbox()
save(im.crop((max(0, box[0] - 60), max(0, box[1] - 60), min(im.width, box[2] + 60), min(im.height, box[3] + 60))), 'ERD-physical')
save(shot(logi_html, os.path.join(TMP, 'l.png'), 2780, 1820), 'ERD-logical')
