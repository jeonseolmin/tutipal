"""스토리보드용: Electron 메인 창 · 위젯을 CDP 로 돌며 화면을 차례로 캡처해 screens/ 에 저장.
사용: desktop 에서 `npx electron --inspect=9229 --remote-debugging-port=9223 .` 로 앱을 켠 뒤
      uv run --no-project --python 3.12 --with websockets==13.1 python docs/storyboard/source/capture.py docs/storyboard/screens
      (끝나면 앱을 꼭 종료. 개발용 로그인 1번 계정 기준)
TEACHER_ROOM = 1번 계정이 방장인 방 id, STUDENT_ROOM = 1번 계정이 참여자인 방 id (DB 에 맞게 바꿀 것).
위젯은 투명 창이라 캡처가 투명 배경 → 덱에 넣기 전에 밝은 배경을 깔았다.
"""
import asyncio
import base64
import json
import os
import sys
import urllib.request

import websockets

OUT = sys.argv[1]
TEACHER_ROOM, STUDENT_ROOM = 13, 22
os.makedirs(OUT, exist_ok=True)


def target(widget):
    for t in json.load(urllib.request.urlopen("http://127.0.0.1:9223/json/list")):
        if t["type"] == "page" and (("/widget" in t["url"]) == widget):
            return t["webSocketDebuggerUrl"]


class Page:
    def __init__(self, ws):
        self.ws, self.i = ws, 0

    async def call(self, method, params=None):
        self.i += 1
        await self.ws.send(json.dumps({"id": self.i, "method": method, "params": params or {}}))
        while True:
            m = json.loads(await self.ws.recv())
            if m.get("id") == self.i:
                return m.get("result", {})

    async def js(self, expr):
        r = await self.call("Runtime.evaluate", {"expression": expr, "awaitPromise": True, "returnByValue": True})
        return r.get("result", {}).get("value")

    async def shot(self, name):
        r = await self.call("Page.captureScreenshot", {"format": "png"})
        open(os.path.join(OUT, name + ".png"), "wb").write(base64.b64decode(r["data"]))
        print("saved", name)

    async def go(self, path, wait=3.0):
        await self.js(f"location.href = {json.dumps(path)}")
        await asyncio.sleep(wait)


SET = """const set=(el,v)=>{Object.getOwnPropertyDescriptor(Object.getPrototypeOf(el),'value').set.call(el,v);
el.dispatchEvent(new Event(el.tagName==='SELECT'?'change':'input',{bubbles:true}))};"""


async def tab(p, label):
    ok = await p.js(f"""(()=>{{const b=[...document.querySelectorAll('[role=tab]')].find(b=>b.innerText.replace(/\\s/g,'').startsWith({json.dumps(label.replace(' ', ''))}));
        if(!b) return false; b.click(); return true}})()""")
    await asyncio.sleep(1.5)
    return ok


async def main():
    async with websockets.connect(target(False), max_size=50_000_000) as ws:
        p = Page(ws)
        await p.go("/login", 2)
        await p.js("fetch('/dev/login?n=1',{method:'POST',headers:{'X-XSRF-TOKEN':decodeURIComponent((document.cookie.match(/XSRF-TOKEN=([^;]+)/)||[])[1]||'')}}).then(r=>r.status)")
        await p.go("/")
        await p.shot("02-home")
        await p.go("/chat?topic=" + urllib.request.quote("자유 주제"))
        await p.shot("03-chat")
        await p.go("/review")
        await p.shot("04-review")
        await p.go("/screen-consent")
        await p.shot("05-screen-consent")
        await p.go("/settings")
        await p.shot("06-settings-screen")
        for i, name in [(1, "07-settings-review"), (2, "08-settings-account")]:
            await p.js(f"document.querySelectorAll('[role=tab]')[{i}].click()")
            await asyncio.sleep(1)
            await p.shot(name)
        await p.go("/sessions/new")
        await p.js(f"""(async()=>{{{SET} set(document.querySelector('.field input'),'한국사 스터디');
            set(document.querySelector('.subject-picker select'),'한국사'); return 1}})()""")
        await asyncio.sleep(0.5)
        await p.shot("09-create-room")
        await p.js(f"""(async()=>{{{SET} set(document.querySelector('.subject-picker select'),'__custom__'); await new Promise(r=>setTimeout(r,300));
            set(document.querySelector('.subject-picker input'),'바리스타 자격증'); return 1}})()""")
        await asyncio.sleep(0.5)
        await p.shot("10-create-room-custom")
        await p.go("/sessions/join")
        await p.js(f"(()=>{{{SET} set(document.querySelector('.code-input'),'A7K2Q9'); return 1}})()")
        await asyncio.sleep(0.5)
        await p.shot("11-join-room")

        await p.go(f"/sessions/{TEACHER_ROOM}")
        teacher = ["참가자", "주제 설정", "이해도 질문", "화면 공유", "도움 요청", "자료", "게시판", "챗봇 미리보기"]
        for n, label in enumerate(teacher):
            if await tab(p, label):
                await p.shot(f"{12 + n:02d}-teacher-{n + 1}")
        await p.go(f"/sessions/{STUDENT_ROOM}")
        student = ["라이브", "챗봇", "공지", "게시판", "자료"]
        for n, label in enumerate(student):
            if await tab(p, label):
                await p.shot(f"{20 + n:02d}-student-{n + 1}")

    async with websockets.connect(target(True), max_size=50_000_000) as ws:
        w = Page(ws)
        await w.js("location.reload()")
        await asyncio.sleep(4)
        await w.shot("25-widget")

    async with websockets.connect(target(False), max_size=50_000_000) as ws:
        p = Page(ws)
        await p.go("/")
        await p.js("fetch('/logout',{method:'POST',headers:{'X-XSRF-TOKEN':decodeURIComponent((document.cookie.match(/XSRF-TOKEN=([^;]+)/)||[])[1]||'')}}).then(r=>r.status)")
        await p.go("/login")
        await p.shot("01-login")
        # 다시 로그인해 두기 (사용자가 계속 쓸 수 있게)
        await p.js("fetch('/dev/login?n=1',{method:'POST',headers:{'X-XSRF-TOKEN':decodeURIComponent((document.cookie.match(/XSRF-TOKEN=([^;]+)/)||[])[1]||'')}}).then(r=>r.status)")
        await p.go("/")


asyncio.run(main())
