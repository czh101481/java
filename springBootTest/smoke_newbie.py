# -*- coding: utf-8 -*-
"""入魔道（新手期）冒烟（可重复跑）：
   开局礼包 → 七日礼逐日领取/防重复 → 一次性资源总额 → 新兵期倍率（签到/洞府）
   → 模块门禁放宽 → 入门试炼仅新兵期可见且不耗煞 → 存量账号懒补发 → 幂等。
运行：后端 8081 与 MySQL 3306 已启动时 `python smoke_newbie.py`
"""
import json
import math
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
NAME = "newbie_smoke"
PWD = "smoke123"
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
        for t in ("demon_newbie", "demon_user_achievement", "demon_wallet", "demon_coin_log",
                  "demon_bag", "demon_purchase", "demon_den", "demon_quiz_record",
                  "demon_read_record", "demon_explore_log", "demon_realm_log",
                  "demon_friend", "demon_message"):
            sql("DELETE FROM %s WHERE user_id=%s" % (t, old))
        sql("DELETE FROM demon_user WHERE id=%s" % old)


def set_boost(uid, active):
    sql("UPDATE demon_newbie SET started_at=%s WHERE user_id=%s"
        % ("NOW()" if active else "DATE_SUB(NOW(), INTERVAL 20 DAY)", uid))


def yesterday(uid):
    sql("UPDATE demon_newbie SET last_claim_date=DATE_SUB(CURDATE(), INTERVAL 1 DAY) "
        "WHERE user_id=%s" % uid)


def wallet(uid, col):
    return int(sql1("SELECT %s FROM demon_wallet WHERE user_id=%s" % (col, uid)) or 0)


def exp_of(uid):
    return int(sql1("SELECT exp FROM demon_user WHERE id=%s" % uid) or 0)


# ---------- 0. 准备 ----------
cleanup()
call("POST", "/api/auth/register", body={
    "name": NAME, "email": "newbie_smoke@demon.com", "realm": "血煞", "password": PWD})
r = call("POST", "/api/auth/login", body={"name": NAME, "password": PWD})
tok = (r.get("data") or {}).get("token")
check("注册 / 登录测试修者", bool(tok), r.get("code"))
if not tok:
    print("无法继续。")
    sys.exit(1)
uid = call("GET", "/api/auth/me", tok)["data"]["id"]

# ---------- 1. 开局礼包 ----------
check("注册即发煞气 120", exp_of(uid) == 120, exp_of(uid))
check("注册即发魔晶 200", wallet(uid, "coin") == 200, wallet(uid, "coin"))
check("注册即发血玉 3", wallet(uid, "jade") == 3, wallet(uid, "jade"))
check("新手行已建立（starter_done=1 / login_day=1）",
      sql1("SELECT CONCAT(starter_done,'/',login_day) FROM demon_newbie WHERE user_id=%s" % uid) == "1/1")

# ---------- 2. state ----------
st = (call("GET", "/api/newbie/state", tok).get("data") or {})
check("state：新兵期生效", st.get("boostActive") is True)
check("state：首日为第 1 日且今日已领",
      st.get("loginDay") == 1 and st.get("claimedToday") is True)
check("state：七日奖励清单完整", len(st.get("rewards") or []) == 7)
check("state：下一日为第 2 日", st.get("nextDay") == 2)
check("state：新兵期剩余约 14 天",
      isinstance(st.get("boostDaysLeft"), int) and 13 <= st["boostDaysLeft"] <= 14,
      st.get("boostDaysLeft"))
check("state：倍率下发正确",
      (st.get("boost") or {}).get("expMult") == 1.5 and (st.get("boost") or {}).get("denMult") == 2.0)

# ---------- 3. 同日不可重复领 ----------
r = call("POST", "/api/newbie/claim", tok)
check("同日重复领取被拒", r.get("code") == 400, r.get("msg"))

# ---------- 4. 七日礼逐日推进 ----------
for d in range(2, 8):
    yesterday(uid)
    r = call("POST", "/api/newbie/claim", tok)
    check("领取第 %d 日之礼" % d,
          r.get("code") == 200 and r.get("data", {}).get("day") == d,
          r.get("msg") or r.get("code"))

# 一次性资源总额（尚未触发任何玩法产出）
check("七日礼合计煞气 430", exp_of(uid) == 430, exp_of(uid))
check("七日礼合计魔晶 950", wallet(uid, "coin") == 950, wallet(uid, "coin"))
check("七日礼合计血玉 9", wallet(uid, "jade") == 9, wallet(uid, "jade"))
check("七日礼走 earn 写流水（reason=newbie，共 9 笔）",
      sql1("SELECT COUNT(*) FROM demon_coin_log WHERE user_id=%s AND reason='newbie'" % uid) == "9",
      sql1("SELECT COUNT(*) FROM demon_coin_log WHERE user_id=%s AND reason='newbie'" % uid))

yesterday(uid)
r = call("POST", "/api/newbie/claim", tok)
check("第 8 次领取被拒（七日圆满）", r.get("code") == 400, r.get("msg"))

# ---------- 5. state 幂等 ----------
before = (exp_of(uid), wallet(uid, "coin"), wallet(uid, "jade"))
for _ in range(3):
    call("GET", "/api/newbie/state", tok)
check("state 连调 3 次不重复发奖",
      before == (exp_of(uid), wallet(uid, "coin"), wallet(uid, "jade")))

# ---------- 6. 新兵期倍率 ----------
set_boost(uid, False)
normal = (call("GET", "/api/home/sign", tok).get("data") or {}).get("reward", 0)
set_boost(uid, True)
boosted = (call("GET", "/api/home/sign", tok).get("data") or {}).get("reward", 0)
check("签到煞气 ×1.5（预览与结算同源）",
      normal > 0 and boosted == math.floor(normal * 1.5 + 0.5),
      "normal=%s boosted=%s" % (normal, boosted))

sr = call("POST", "/api/home/sign", tok)
check("实际签到所得等于预览值",
      sr.get("code") == 200 and (sr.get("data") or {}).get("gain") == boosted,
      (sr.get("data") or {}).get("gain"))

exp_before_explore = exp_of(uid)
er = call("POST", "/api/home/explore/random", tok)
check("新兵期参悟可正常结算（煞气实际入账）",
      er.get("code") == 200 and exp_of(uid) > exp_before_explore,
      er.get("msg") or (exp_of(uid) - exp_before_explore))

den_on = (call("GET", "/api/den", tok).get("data") or {})
check("洞府每级产量 ×2（2→4 / 100→200‰）",
      den_on.get("shaPerLevel") == 4 and den_on.get("coinPerLevel") == 4
      and den_on.get("jadeMilliPerLevel") == 200,
      (den_on.get("shaPerLevel"), den_on.get("coinPerLevel"), den_on.get("jadeMilliPerLevel")))
set_boost(uid, False)
den_off = (call("GET", "/api/den", tok).get("data") or {})
check("新兵期结束洞府产量回落基准",
      den_off.get("shaPerLevel") == 2 and den_off.get("coinPerLevel") == 2
      and den_off.get("jadeMilliPerLevel") == 100)
set_boost(uid, True)

# ---------- 7. 模块门禁放宽 ----------
check("引煞可登论道台（原魔卒封禁）",
      call("GET", "/api/home/list?type=debate", tok).get("code") == 200)
check("引煞可进秘境列表页",
      call("GET", "/api/home/list?type=trial", tok).get("code") == 200)
check("凝煞门槛仍生效（codex）",
      call("GET", "/api/home/list?type=codex", tok).get("code") == 403)
check("魔卒门槛仍生效（archive）",
      call("GET", "/api/home/list?type=archive", tok).get("code") == 403)

# ---------- 8. 入门试炼 ----------
sl = (call("GET", "/api/home/secret/list", tok).get("data") or {}).get("list") or []
nb = [x for x in sl if x.get("newbieOnly")]
check("新兵期列表含 3 处入门试炼", len(nb) == 3, len(nb))
check("入门试炼引煞可入且不耗煞",
      all(x.get("unlocked") and x.get("costExp") == 0 for x in nb),
      [(x.get("realmReq"), x.get("costExp")) for x in nb])
check("新兵期正境启门亦不耗煞",
      all(x.get("costExp") == 0 for x in sl if not x.get("newbieOnly")))

en = call("POST", "/api/home/secret/enter/10", tok)
ed = en.get("data") or {}
check("可结算入门试炼",
      en.get("code") == 200 and ed.get("cost") == 0 and ed.get("net") == ed.get("gain"),
      en.get("msg") or (ed.get("gain"), ed.get("cost")))

set_boost(uid, False)
sl2 = (call("GET", "/api/home/secret/list", tok).get("data") or {}).get("list") or []
check("新兵期结束后入门试炼自隐", len(sl2) == 9 and not any(x.get("newbieOnly") for x in sl2), len(sl2))
check("新兵期外不可再入试炼",
      call("POST", "/api/home/secret/enter/10", tok).get("code") == 400)
set_boost(uid, True)

# ---------- 9. 存量账号懒补发 ----------
sql("DELETE FROM demon_newbie WHERE user_id=%s" % uid)
st2 = (call("GET", "/api/newbie/state", tok).get("data") or {})
check("无记录账号访问 state 自动补发",
      st2.get("starterDone") is True and st2.get("loginDay") == 1 and st2.get("boostActive") is True,
      (st2.get("starterDone"), st2.get("loginDay")))

# ---------- 收尾 ----------
cleanup()
print("\n===== 入魔道冒烟：%d PASS / %d FAIL =====" % (len(passed), len(failed)))
if failed:
    for f in failed:
        print("  - " + f)
    sys.exit(1)
