"""Electron 을 마우스 없이 조작: CDP 로 페이지/메인 프로세스에서 JS 실행 · 스크린샷.
사용법:
  cdp.py targets
  cdp.py eval <url부분|main> <js>        (js 는 await 가능, 결과 JSON 출력)
  cdp.py shot <url부분> <파일.png>
"""
import asyncio
import base64
import json
import sys
import urllib.request

import websockets


def targets(port):
    return json.load(urllib.request.urlopen(f"http://127.0.0.1:{port}/json/list"))


def find(part):
    if part == "main":
        return targets(9229)[0]["webSocketDebuggerUrl"]
    for t in targets(9223):
        # "=주소" 는 정확히 일치, 아니면 경로 시작 일치 (http://localhost 뒤)
        url = t["url"]
        path = url.split("//", 1)[-1].split("/", 1)[-1]
        ok = url == part[1:] if part.startswith("=") else ("/" + path).startswith(part)
        if t.get("type") == "page" and ok:
            return t["webSocketDebuggerUrl"]
    raise SystemExit(f"no target matching {part}: {[t['url'] for t in targets(9223)]}")


async def call(ws_url, method, params):
    async with websockets.connect(ws_url, max_size=50_000_000) as ws:
        await ws.send(json.dumps({"id": 1, "method": method, "params": params}))
        while True:
            msg = json.loads(await ws.recv())
            if msg.get("id") == 1:
                return msg


async def main():
    cmd = sys.argv[1]
    if cmd == "targets":
        for t in targets(9223):
            print(t["type"], t["url"])
        return
    if cmd == "eval":
        expr = f"(async () => {{ return await ({sys.argv[3]}) }})()"
        res = await call(find(sys.argv[2]), "Runtime.evaluate",
                         {"expression": expr, "awaitPromise": True, "returnByValue": True, "includeCommandLineAPI": True})
        r = res.get("result", {})
        if "exceptionDetails" in r:
            print("EXCEPTION:", json.dumps(r["exceptionDetails"], ensure_ascii=False)[:800])
        else:
            print(json.dumps(r.get("result", {}).get("value"), ensure_ascii=False, indent=1)[:4000])
        return
    if cmd == "shot":
        res = await call(find(sys.argv[2]), "Page.captureScreenshot", {"format": "png"})
        open(sys.argv[3], "wb").write(base64.b64decode(res["result"]["data"]))
        print("saved", sys.argv[3])


asyncio.run(main())
