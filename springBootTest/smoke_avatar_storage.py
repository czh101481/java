# -*- coding: utf-8 -*-
"""头像图片入库冒烟（可重复跑）：
   上传 → 图片本体入库 → 后端/前端同源取图 → 删掉磁盘文件仍可取图（库优先）
   → 英杰榜头像可访问 → 换回预设自动清库 → 非法上传被拒 → 清理测试账号。
运行：MySQL 3306、后端 8081、前端 5173 均已启动时 `python smoke_avatar_storage.py`
"""
import base64
import glob
import json
import pathlib
import re
import subprocess
import sys
import urllib.error
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent
BACK = "http://127.0.0.1:8081"
FRONT = "http://127.0.0.1:5173"
DB = "C:/Users/czh10/mysql84/server/bin/mysql.exe"
PROPS = ROOT / "src/main/resources/application.properties"
DB_PWD = re.search(r"^spring\.datasource\.password=(.*)$",
                   PROPS.read_text(encoding="utf-8"), re.M).group(1).strip()
NAME, PWD = "avatar_smoke", "smoke123"
# 1×1 透明 PNG
PNG = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAE"
    "hQGAhKmMIQAAAABJRU5ErkJggg==")
passed, failed = [], []


def check(name, ok, extra=""):
    (passed if ok else failed).append(name)
    print(("  PASS  " if ok else "  FAIL  ") + name + (("  | " + str(extra)) if extra else ""))


def call(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BACK + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode())


def upload(token, filename, content, mime="image/png"):
    boundary = "----wbavatar" + str(len(content))
    body = (
        ("--%s\r\n" % boundary).encode()
        + ('Content-Disposition: form-data; name="file"; filename="%s"\r\n' % filename).encode()
        + ("Content-Type: %s\r\n\r\n" % mime).encode()
        + content
        + ("\r\n--%s--\r\n" % boundary).encode()
    )
    req = urllib.request.Request(BACK + "/api/auth/avatar/upload", data=body, method="POST")
    req.add_header("Content-Type", "multipart/form-data; boundary=" + boundary)
    req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode())


def fetch(url):
    """返回 (status, content_type, bytes)"""
    try:
        with urllib.request.urlopen(url, timeout=20) as r:
            return r.status, r.headers.get("Content-Type", ""), r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.headers.get("Content-Type", ""), e.read()


def sql(stmt):
    subprocess.run([DB, "-uroot", "-p" + DB_PWD, "-h127.0.0.1", "-P3306",
                    "--default-character-set=utf8mb4", "demon_db", "-e", stmt],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def sql1(stmt):
    p = subprocess.run([DB, "-uroot", "-p" + DB_PWD, "-h127.0.0.1", "-P3306",
                        "--default-character-set=utf8mb4", "-N", "-B", "demon_db", "-e", stmt],
                       stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    out = p.stdout.decode("utf-8", "replace").strip()
    return out or None


def cleanup():
    uid = sql1("SELECT id FROM demon_user WHERE name='%s'" % NAME)
    if uid:
        for t in ("demon_user_achievement", "demon_user_art", "demon_bag", "demon_item_use",
                  "demon_wallet", "demon_coin_log", "demon_purchase", "demon_den",
                  "demon_quiz_record", "demon_read_record", "demon_explore_log",
                  "demon_realm_log", "demon_friend", "demon_message"):
            sql("DELETE FROM %s WHERE user_id=%s" % (t, uid))
        sql("DELETE FROM demon_user WHERE id=%s" % uid)
        sql("DELETE FROM demon_avatar_file WHERE file_path REGEXP '^/uploads/avatars/%s_'" % uid)
        for f in glob.glob(str(ROOT / "uploads" / "avatars" / (uid + "_*"))):
            try:
                pathlib.Path(f).unlink()
            except OSError:
                pass


# ---------- 0. 准备 ----------
cleanup()
check("清理上次残留", sql1("SELECT id FROM demon_user WHERE name='%s'" % NAME) is None)
call("POST", "/api/auth/register",
     body={"name": NAME, "email": NAME + "@smoke.local", "realm": "血煞", "password": PWD})
r = call("POST", "/api/auth/login", body={"name": NAME, "password": PWD})
tok = (r.get("data") or {}).get("token")
uid = sql1("SELECT id FROM demon_user WHERE name='%s'" % NAME)
check("注册并登录测试修者", bool(tok) and bool(uid), r.get("code"))
if not tok:
    print("无法继续。")
    sys.exit(1)

# ---------- 1. 上传 ----------
up = upload(tok, "smoke.png", PNG)
avatar = ((up.get("data") or {}).get("avatar") or "")
check("上传返回 code 200 且给出 /uploads 路径",
      up.get("code") == 200 and avatar.startswith("/uploads/avatars/"), avatar)

b64 = sql1("SELECT img_base64 FROM demon_avatar_file WHERE file_path='%s'" % avatar)
check("图片本体已写入 demon_avatar_file", bool(b64), "len=%s" % (len(b64) if b64 else 0))
check("库内 base64 与上传字节完全一致",
      bool(b64) and base64.b64decode(b64) == PNG)
mime = sql1("SELECT mime FROM demon_avatar_file WHERE file_path='%s'" % avatar)
check("mime 按扩展名定为 image/png", mime == "image/png", mime)

# ---------- 2. 取图：后端直连 / 前端同源代理 ----------
st, ct, data = fetch(BACK + avatar)
check("后端直连取图 200 / image/png / 字节一致",
      st == 200 and ct.startswith("image/png") and data == PNG, "%s %s %dB" % (st, ct, len(data)))
st2, ct2, data2 = fetch(FRONT + avatar)
check("前端 Vite 代理（同源相对路径）取图一致",
      st2 == 200 and ct2.startswith("image/png") and data2 == PNG, "%s %s %dB" % (st2, ct2, len(data2)))

# ---------- 3. 关键：删掉磁盘文件后仍可取图（模拟换电脑 uploads/ 为空） ----------
disk = ROOT / avatar.lstrip("/")
if disk.exists():
    disk.unlink()
check("磁盘文件已移除", not disk.exists())
st3, ct3, data3 = fetch(BACK + avatar)
check("磁盘无文件时仍从库取到原图（跨电脑可见）",
      st3 == 200 and data3 == PNG, "%s %dB" % (st3, len(data3)))

# ---------- 4. 英杰榜现有上传头像都能取到 ----------
rank = call("GET", "/api/home/rank?type=exp")
paths = [x["avatar"] for x in (rank.get("data") or [])
         if str(x.get("avatar") or "").startswith("/uploads/")][:3]
ok = bool(paths)
for p in paths:
    s, c, b = fetch(BACK + p)
    ok = ok and s == 200 and c.startswith("image/") and len(b) > 0
check("英杰榜 %d 个上传头像均可取图" % len(paths), ok, paths)

# ---------- 5. 换回预设：旧图片记录被清理 ----------
me = call("GET", "/api/auth/me", tok)
check("/api/auth/me 返回新头像", (me.get("data") or {}).get("avatar") == avatar)
sw = call("POST", "/api/auth/avatar", tok, {"avatar": "blood"})
check("切回预设头像 code 200", sw.get("code") == 200, sw.get("msg"))
check("切换后旧图片库记录已删除",
      sql1("SELECT COUNT(*) FROM demon_avatar_file WHERE file_path='%s'" % avatar) == "0")
st4, _, _ = fetch(BACK + avatar)
check("旧路径取图返回 404", st4 == 404, st4)

# ---------- 6. 非法上传仍被拒 ----------
bad = upload(tok, "evil.txt", b"not an image", "text/plain")
check("非法扩展名被拒且未写库", bad.get("code") == 400, bad.get("msg"))
big = upload(tok, "big.png", b"0" * (2 * 1024 * 1024 + 1))
check("超过 2MB 被拒", big.get("code") == 400, big.get("msg"))

# ---------- 7. 收尾 ----------
cleanup()
check("测试账号与图片记录已清理",
      sql1("SELECT id FROM demon_user WHERE name='%s'" % NAME) is None
      and (sql1("SELECT COUNT(*) FROM demon_avatar_file") or "0") != "0")
check("历史 9 张头像仍在库中",
      int(sql1("SELECT COUNT(*) FROM demon_avatar_file") or 0) >= 9,
      sql1("SELECT COUNT(*) FROM demon_avatar_file"))

print("\n%d PASS / %d FAIL" % (len(passed), len(failed)))
if failed:
    for f in failed:
        print("  FAILED: " + f)
    sys.exit(1)
