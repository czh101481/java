# -*- coding: utf-8 -*-
"""破关之钥冒烟（可重复跑）：
   正文/商店文案一致 → 乾坤袋不可开囊（不吞道具、不加煞、不占额度） → 修为页破关仍可正常耗用。
运行：后端 8081 已启动、MySQL 3306 已启动时 `python smoke_key_item.py`
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
NAME = "key_item_smoke"
PWD = "smoke123"
passed, failed = [], []


def call(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
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


def rows_of(d):
    """匿名 / 登录后两种返回形态：list 或 {list: [...]}"""
    data = d.get("data") or []
    if isinstance(data, dict):
        data = data.get("list") or data.get("items") or []
    return data


def cleanup():
    uid = sql1("SELECT id FROM demon_user WHERE name='%s'" % NAME)
    if uid:
        for t in ("demon_newbie", "demon_user_art", "demon_bag", "demon_item_use", "demon_wallet",
                  "demon_coin_log", "demon_purchase", "demon_explore_log",
                  "demon_quiz_record", "demon_read_record"):
            sql("DELETE FROM %s WHERE user_id=%s" % (t, uid))
        sql("DELETE FROM demon_user WHERE id=%s" % uid)


cleanup()
call("POST", "/api/auth/register",
     body={"name": NAME, "email": NAME + "@smoke.local", "realm": "血煞", "password": PWD})
r = call("POST", "/api/auth/login", body={"name": NAME, "password": PWD})
TOK = (r.get("data") or {}).get("token")
UID = sql1("SELECT id FROM demon_user WHERE name='%s'" % NAME)
check("注册并登录测试修者", bool(TOK) and bool(UID), r.get("code"))

INS = sql1("SELECT id FROM demon_item WHERE code='insight_fate'")
TRA = sql1("SELECT id FROM demon_item WHERE code='death_battle'")
ART = sql1("SELECT id FROM demon_art ORDER BY id LIMIT 1")
check("道具与功法已录入", all((INS, TRA, ART)), (INS, TRA, ART))

# 构造：完整卷已至圆满 + 袋中各有 1 枚破关钥
sql("INSERT INTO demon_user_art (user_id,art_id,form,stage,prof,train_count) "
    "VALUES (%s,%s,0,4,0,0)" % (UID, ART))
sql("INSERT INTO demon_bag (user_id,item_id,qty) VALUES (%s,%s,1),(%s,%s,1) "
    "ON DUPLICATE KEY UPDATE qty=qty+1" % (UID, INS, UID, TRA))
sql("DELETE FROM demon_item_use WHERE user_id=%s" % UID)
EXP0 = sql1("SELECT exp FROM demon_user WHERE id=%s" % UID)

print("== 1. 文案与实得一致 ==")
d = call("GET", "/api/coin/shop", TOK)
shop = {x["code"]: x for x in rows_of(d)}
check("商店：悟道机缘显示破关之钥",
      "破关之钥" in shop["insight_fate"]["effectText"]
      and "煞气" not in shop["insight_fate"]["effectText"], shop["insight_fate"]["effectText"])
check("商店：死战悟道显示破关之钥",
      "破关之钥" in shop["death_battle"]["effectText"]
      and "煞气" not in shop["death_battle"]["effectText"], shop["death_battle"]["effectText"])
check("丹符说明与 effect_value 一致",
      shop["abyss_orb"]["desc"].find(str(shop["abyss_orb"]["effectValue"])) >= 0
      and shop["supreme_fruit"]["desc"].find(str(shop["supreme_fruit"]["effectValue"])) >= 0,
      (shop["abyss_orb"]["desc"], shop["supreme_fruit"]["desc"]))

d = call("GET", "/api/coin/bag", TOK)
bag = {x["code"]: x for x in rows_of(d)}
check("乾坤袋：两件秘宝文案同为破关之钥",
      "破关之钥" in bag["insight_fate"]["effectText"]
      and "破关之钥" in bag["death_battle"]["effectText"])

print("== 2. 不可开囊，且不吞道具 ==")
for code, iid in (("悟道机缘", INS), ("死战悟道", TRA)):
    r = call("POST", "/api/coin/use", TOK, {"itemId": int(iid), "qty": 1})
    check("%s 取用被拒" % code, r.get("code") == 400 and "不可开囊" in (r.get("msg") or ""),
          r.get("msg"))
qty_ins = sql1("SELECT qty FROM demon_bag WHERE user_id=%s AND item_id=%s" % (UID, INS))
qty_tra = sql1("SELECT qty FROM demon_bag WHERE user_id=%s AND item_id=%s" % (UID, TRA))
exp1 = sql1("SELECT exp FROM demon_user WHERE id=%s" % UID)
used = sql1("SELECT COUNT(*) FROM demon_item_use WHERE user_id=%s" % UID)
check("被拒后道具仍在袋中", qty_ins == "1" and qty_tra == "1", (qty_ins, qty_tra))
check("被拒后煞气未变", exp1 == EXP0, (EXP0, exp1))
check("被拒后未占每日限用额度", used == "0", used)

print("== 3. 修为页破关仍可耗用 ==")
r = call("POST", "/api/art/insight/%s" % ART, TOK)
check("顿悟入道成功", r.get("code") == 200
      and (r.get("data") or {}).get("art", {}).get("stageName") == "入道", r.get("msg"))
check("入道消耗一枚悟道机缘",
      sql1("SELECT qty FROM demon_bag WHERE user_id=%s AND item_id=%s" % (UID, INS)) == "0")
check("破关计入每日限用账",
      sql1("SELECT qty FROM demon_item_use WHERE user_id=%s AND item_id=%s" % (UID, INS)) == "1")

sql("UPDATE demon_user_art SET stage=5, prof=0 WHERE user_id=%s AND art_id=%s" % (UID, ART))
r = call("POST", "/api/art/transcend/%s" % ART, TOK)
check("死战破境至无上", r.get("code") == 200
      and (r.get("data") or {}).get("art", {}).get("stageName") == "无上", r.get("msg"))
check("无上消耗一枚死战悟道",
      sql1("SELECT qty FROM demon_bag WHERE user_id=%s AND item_id=%s" % (UID, TRA)) == "0")

cleanup()
print("\n合计：%d PASS / %d FAIL" % (len(passed), len(failed)))
if failed:
    print("失败项：" + "、".join(failed))
    sys.exit(1)
