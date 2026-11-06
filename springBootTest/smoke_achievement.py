# -*- coding: utf-8 -*-
"""道途成就 + 今日修行冒烟（可重复跑）：
   七课聚合 → 25 条成就 → 达成自动铭刻且不重复/不回退 → 停用生效 → 后台 CRUD 与白名单校验 → 删除保护。
运行：后端 8081 与 MySQL 3306 已启动时 `python smoke_achievement.py`
"""
import json
import pathlib
import re
import subprocess
import sys
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8081"
DB = "C:/Users/czh10/mysql84/server/bin/mysql.exe"
PROPS = pathlib.Path(__file__).parent / "src/main/resources/application.properties"
DB_PWD = re.search(r"^spring\.datasource\.password=(.*)$",
                   PROPS.read_text(encoding="utf-8"), re.M).group(1).strip()
NAME = "ach_smoke"
PWD = "smoke123"
TMP_CODE = "smoke_tmp"
passed, failed = [], []


def call(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode())


def check(name, ok, extra=""):
    (passed if ok else failed).append(name)
    print(("  PASS  " if ok else "  FAIL  ") + name + (("  | " + str(extra)) if extra else ""))


def sql(stmt):
    subprocess.run([DB, "-uroot", "-p" + DB_PWD, "-h127.0.0.1", "-P3306",
                    "--default-character-set=utf8mb4", "demon_db", "-e", stmt],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def sql1(stmt):
    p = subprocess.run([DB, "-uroot", "-p" + DB_PWD, "-h127.0.0.1", "-P3306",
                        "--default-character-set=utf8mb4", "-N", "-B", "demon_db", "-e", stmt],
                       capture_output=True, text=True)
    return (p.stdout or "").strip().split("\n")[0] if (p.stdout or "").strip() else ""


def cleanup():
    old = sql1("SELECT id FROM demon_user WHERE name='%s'" % NAME)
    if old:
        for t in ("demon_user_achievement", "demon_wallet", "demon_coin_log", "demon_bag",
                  "demon_purchase", "demon_den", "demon_quiz_record", "demon_read_record",
                  "demon_explore_log", "demon_realm_log", "demon_friend", "demon_message"):
            sql("DELETE FROM %s WHERE user_id=%s" % (t, old))
        sql("DELETE FROM demon_user WHERE id=%s" % old)
    sql("DELETE FROM demon_user_achievement WHERE achievement_id IN "
        "(SELECT id FROM (SELECT id FROM demon_achievement WHERE code='%s') t)" % TMP_CODE)
    sql("DELETE FROM demon_achievement WHERE code='%s'" % TMP_CODE)
    sql("UPDATE demon_achievement SET status=1 WHERE code='first_step'")


# ---------- 0. 准备：清理旧数据、注册测试修者、授予临时后台权限 ----------
cleanup()
call("POST", "/api/auth/register", body={
    "name": NAME, "email": "ach_smoke@demon.com", "realm": "血煞", "password": PWD})
r = call("POST", "/api/auth/login", body={"name": NAME, "password": PWD})
tok = (r.get("data") or {}).get("token")
check("注册 / 登录测试修者", bool(tok), r.get("code"))
if not tok:
    print("无法继续。"); sys.exit(1)
uid = call("GET", "/api/auth/me", tok)["data"]["id"]
sql("UPDATE demon_user SET permission=0 WHERE id=%s" % uid)
ar = call("POST", "/api/admin/login", body={"username": NAME, "password": PWD})
atok = (ar.get("data") or {}).get("token")
check("后台登录（临时授予 permission=0）", bool(atok), ar.get("code"))
if not atok:
    cleanup(); sys.exit(1)

# ---------- 1. 今日修行七课 ----------
d = call("GET", "/api/achievement/daily", tok)
dd = d.get("data") or {}
items = dd.get("items") or []
check("七课接口 200", d.get("code") == 200, d.get("msg"))
check("恰好七项且顺序正确", [x["key"] for x in items] ==
      ["sign", "explore", "quiz", "spar", "read", "secret", "den"],
      [x["key"] for x in items])
limits = {x["key"]: x["limit"] for x in items}
check("各项上限与 BALANCE.md 一致", limits ==
      {"sign": 1, "explore": 10, "quiz": 10, "spar": 5, "read": 5, "secret": 19, "den": 1}, limits)
check("新号完成度为 0", dd.get("doneCount") == 0 and dd.get("percent") == 0
      and dd.get("allDone") is False, (dd.get("doneCount"), dd.get("percent")))

# ---------- 2. 成就墙总览 ----------
a = call("GET", "/api/achievement/list", tok)
ad = a.get("data") or {}
check("成就墙接口 200", a.get("code") == 200, a.get("msg"))
check("25 条启用成就", ad.get("total") == 25, ad.get("total"))
check("六期分期汇总", len(ad.get("stages") or []) == 6,
      [s["stage"] for s in (ad.get("stages") or [])])
check("新号尚未铭刻", ad.get("unlockedCount") == 0 and ad.get("percent") == 0,
      (ad.get("unlockedCount"), ad.get("percent")))
check("下一目标为初入魔道", (ad.get("next") or {}).get("name") == "初入魔道",
      ad.get("next"))

# ---------- 3. 达成自动铭刻 ----------
sql("UPDATE demon_user SET exp=60 WHERE id=%s" % uid)
a = call("GET", "/api/achievement/list", tok)
ad = a.get("data") or {}
first = next((x for x in (ad.get("list") or []) if x["code"] == "first_step"), {})
check("达成后自动解锁「初入魔道」", first.get("unlocked") is True, first.get("cur"))
check("称号「引煞童子」入列", "引煞童子" in (ad.get("titles") or []), ad.get("titles"))
cnt1 = sql1("SELECT COUNT(*) FROM demon_user_achievement WHERE user_id=%s" % uid)
check("解锁账写入 1 条", cnt1 == "1", cnt1)
call("GET", "/api/achievement/list", tok)
cnt2 = sql1("SELECT COUNT(*) FROM demon_user_achievement WHERE user_id=%s" % uid)
check("重复读取不重复铭刻", cnt2 == "1", cnt2)

# ---------- 4. 条件回退不抹去已铭功行 ----------
sql("UPDATE demon_user SET exp=0 WHERE id=%s" % uid)
a = call("GET", "/api/achievement/list", tok)
first = next((x for x in ((a.get("data") or {}).get("list") or []) if x["code"] == "first_step"), {})
check("煞气回落后徽记仍在", first.get("unlocked") is True and first.get("cur") == 0,
      (first.get("unlocked"), first.get("cur")))

# ---------- 5. 停用后不再出现在玩家端 ----------
first_id = sql1("SELECT id FROM demon_achievement WHERE code='first_step'")
r = call("PUT", "/api/admin/achievement/%s" % first_id, atok, {"status": 0})
check("后台停用徽记 200", r.get("code") == 200, r.get("msg"))
a = call("GET", "/api/achievement/list", tok)
check("停用后玩家端剩 24 条", (a.get("data") or {}).get("total") == 24,
      (a.get("data") or {}).get("total"))
check("停用不影响已铭记录", sql1("SELECT COUNT(*) FROM demon_user_achievement WHERE user_id=%s" % uid) == "1")
r = call("PUT", "/api/admin/achievement/%s" % first_id, atok, {"status": 1})
check("后台恢复启用 200", r.get("code") == 200, r.get("msg"))

# ---------- 6. 后台 CRUD 与白名单 ----------
payload = {
    "code": TMP_CODE, "name": "冒烟徽记", "icon": "烟", "category": "日课",
    "stage": "初入", "condType": "exp", "condValue": 999999,
    "titleReward": "试印", "subtitle": "煞气累计 999999", "sortOrder": 999,
    "status": 1, "description": "冒烟用，跑完即删。"
}
r = call("POST", "/api/admin/achievement", atok, payload)
new_id = (r.get("data") or {}).get("id")
check("后台新增徽记 200", r.get("code") == 200 and bool(new_id), r.get("msg"))
r = call("POST", "/api/admin/achievement", atok, dict(payload))
check("代号重复被拒", r.get("code") == 400, r.get("msg"))
r = call("POST", "/api/admin/achievement", atok, dict(payload, code="smoke_bad", condType="not_a_cond"))
check("非法判定类型被拒", r.get("code") == 400, r.get("msg"))
r = call("POST", "/api/admin/achievement", atok, dict(payload, code="smoke_bad2", condValue=0))
check("阈值 0 被拒", r.get("code") == 400, r.get("msg"))
r = call("POST", "/api/admin/achievement", atok, dict(payload, code="smoke_bad3", stage="飞升"))
check("非法分期被拒", r.get("code") == 400, r.get("msg"))
r = call("PUT", "/api/admin/achievement/%s" % new_id, atok,
         {"name": "冒烟徽记·改", "condValue": 888888})
check("后台修改徽记 200", r.get("code") == 200, r.get("msg"))
check("修改后数值生效",
      sql1("SELECT name FROM demon_achievement WHERE id=%s" % new_id) == "冒烟徽记·改"
      and sql1("SELECT cond_value FROM demon_achievement WHERE id=%s" % new_id) == "888888")
r = call("GET", "/api/admin/achievement/list?kw=smoke_tmp", atok)
check("后台列表可按代号检索", r.get("code") == 200
      and any(x["code"] == TMP_CODE for x in (r.get("data") or [])), r.get("msg"))

# ---------- 7. 删除保护 ----------
sql("INSERT INTO demon_user_achievement(user_id, achievement_id) VALUES(%s, %s)" % (uid, new_id))
r = call("DELETE", "/api/admin/achievement/%s" % new_id, atok)
check("已有修者铭刻时禁止删除", r.get("code") == 400, r.get("msg"))
sql("DELETE FROM demon_user_achievement WHERE user_id=%s AND achievement_id=%s" % (uid, new_id))
r = call("DELETE", "/api/admin/achievement/%s" % new_id, atok)
check("清空后可删除", r.get("code") == 200, r.get("msg"))
check("成就总数恢复 25", sql1("SELECT COUNT(*) FROM demon_achievement") == "25",
      sql1("SELECT COUNT(*) FROM demon_achievement"))

# ---------- 8. 清理 ----------
cleanup()
check("测试修者已清理", sql1("SELECT COUNT(*) FROM demon_user WHERE name='%s'" % NAME) == "0")
check("临时徽记已清理", sql1("SELECT COUNT(*) FROM demon_achievement WHERE code='%s'" % TMP_CODE) == "0")
check("首枚徽记已恢复启用",
      sql1("SELECT status FROM demon_achievement WHERE code='first_step'") == "1")
check("解锁账无测试残留",
      sql1("SELECT COUNT(*) FROM demon_user_achievement ua LEFT JOIN demon_user u "
           "ON ua.user_id=u.id WHERE u.id IS NULL") == "0")

print("\n==== 道途成就冒烟：%d PASS / %d FAIL ====" % (len(passed), len(failed)))
if failed:
    for f in failed:
        print("  FAILED: " + f)
sys.exit(1 if failed else 0)
