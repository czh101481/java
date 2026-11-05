# -*- coding: utf-8 -*-
"""完整/残片功法体系 · 六重修炼 · 心魔  端到端冒烟

覆盖：
  1) 阶位定价恒定：黄 100/35 魔晶、玄 800/280 魔晶、地 50/18 血玉（残片 = 完整 × 0.35）
  2) 双形态购得：残片入库、重复购拒绝并提示补全、补全付差价
  3) 六重进度：入门→小成→大成→圆满，日课限次、耗煞气、残片止步大成
  4) 顿悟 / 破境：须完整卷 + 对应秘宝，残篇拒绝
  5) 心魔：残篇每课 +2、心魔 ≥60 阻断冲境、战力折损、完整入道后每课 -1
  6) 后台：价格恒定（改价被忽略）、非法阶位拒绝
"""
import json
import subprocess
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8081"
DB = "C:/Users/czh10/mysql84/server/bin/mysql.exe"
NAME, PWD = "art2_smoke", "smoke123"

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
    subprocess.run([DB, "-uroot", "-p123456", "-h127.0.0.1", "-P3306", "demon_db", "-e", stmt],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def sql1(stmt):
    r = subprocess.run([DB, "-uroot", "-p123456", "-h127.0.0.1", "-P3306", "-N", "demon_db", "-e", stmt],
                       capture_output=True, text=True, encoding="utf-8")
    return (r.stdout or "").strip()


def login(name=NAME, pwd=PWD):
    d = call("POST", "/api/auth/login", body={"name": name, "password": pwd})
    return (d.get("data") or {}).get("token")


def arts(token):
    d = call("GET", "/api/home/list?type=art", token)
    return d.get("data") or []


def art_id(token, name):
    for a in arts(token):
        if a.get("title") == name:
            return a.get("id")
    return None


def detail(token, aid):
    return (call("GET", "/api/home/detail?type=art&id=%s" % aid, token).get("data") or {})


def wallet(token):
    return (call("GET", "/api/coin/wallet", token).get("data") or {})


def panel(token):
    return (call("GET", "/api/cultivate/panel", token).get("data") or {})


def item_id(code):
    return int(sql1("SELECT id FROM demon_item WHERE code='%s' LIMIT 1" % code) or 0)


print("== 0. 准备修者 ==")
sql("DELETE ua FROM demon_user_art ua JOIN demon_user u ON u.id=ua.user_id WHERE u.name='%s'" % NAME)
sql("DELETE w FROM demon_wallet w JOIN demon_user u ON u.id=w.user_id WHERE u.name='%s'" % NAME)
sql("DELETE b FROM demon_bag b JOIN demon_user u ON u.id=b.user_id WHERE u.name='%s'" % NAME)
sql("DELETE l FROM demon_coin_log l JOIN demon_user u ON u.id=l.user_id WHERE u.name='%s'" % NAME)
sql("DELETE FROM demon_user WHERE name='%s'" % NAME)
call("POST", "/api/auth/register",
     body={"name": NAME, "email": NAME + "@smoke.local", "realm": "血煞", "password": PWD})
sql("UPDATE demon_user SET cultivation='血煞', layer=9, exp=20000, heart_demon=0 WHERE name='%s'" % NAME)
sql("INSERT INTO demon_wallet (user_id,coin,jade,earned_coin,spent_coin,earned_jade,spent_jade) "
    "SELECT id,5000,3000,0,0,0,0 FROM demon_user WHERE name='%s' "
    "ON DUPLICATE KEY UPDATE coin=5000,jade=3000" % NAME)
TOK = login()
UID = int(sql1("SELECT id FROM demon_user WHERE name='%s'" % NAME) or 0)
check("注册并登录", bool(TOK) and UID > 0, "uid=%s" % UID)

print("== 1. 阶位与定价恒定 ==")
A = art_id(TOK, "凝血爪")
B = art_id(TOK, "玄阴指")
C = art_id(TOK, "九幽寒煞功")
X = art_id(TOK, "血煞戮天诀")
check("功法阁已合并分重（旧「·一重」不再存在）",
      all("·" not in (a.get("title") or "") for a in arts(TOK)))
check("共 15 部功法", len(arts(TOK)) == 15, len(arts(TOK)))
d = detail(TOK, A)
check("黄阶：完整 100 魔晶 / 残片 35 魔晶",
      d["art"]["priceFull"] == 100 and d["art"]["priceFrag"] == 35
      and d["art"]["currencyName"] == "魔晶", (d["art"]["priceFull"], d["art"]["priceFrag"]))
d = detail(TOK, B)
check("玄阶：完整 800 / 残片 280 魔晶",
      d["art"]["priceFull"] == 800 and d["art"]["priceFrag"] == 280)
d = detail(TOK, C)
check("地阶：完整 50 / 残片 18 血玉",
      d["art"]["priceFull"] == 50 and d["art"]["priceFrag"] == 18
      and d["art"]["currencyName"] == "血玉")
d = detail(TOK, X)
check("仙阶：完整 2000 / 残片 700 血玉",
      d["art"]["priceFull"] == 2000 and d["art"]["priceFrag"] == 700)
check("六重进度名下发", detail(TOK, A)["art"]["stageNames"] ==
      ["入门", "小成", "大成", "圆满", "入道", "无上"])

print("== 2. 双形态购得 ==")
c0 = wallet(TOK)["coin"]
r = call("POST", "/api/art/buy/%d?form=1" % A, TOK)
check("购残片成功", r.get("code") == 200, r.get("msg"))
check("残片扣 35 魔晶", wallet(TOK)["coin"] == c0 - 35, wallet(TOK)["coin"])
d = detail(TOK, A)
check("残片入库且为入门", d["owned"] and d["art"]["form"] == 1 and d["art"]["stageName"] == "入门")
check("残片数值按系数折损（0.30×0.55）",
      abs(d["art"]["factor"] - 0.165) < 1e-6, d["art"]["factor"])
r = call("POST", "/api/art/buy/%d?form=0" % A, TOK)
check("重复购买被拒且提示补全", r.get("code") == 400 and "补全" in (r.get("msg") or ""), r.get("msg"))

print("== 3. 残片修炼与心魔 ==")
r = call("POST", "/api/art/cultivate/%d" % A, TOK)
check("修炼一课成功", r.get("code") == 200, r.get("msg"))
d1 = r.get("data") or {}
check("残片每课熟练 +5", d1.get("art", {}).get("prof") == 5, d1.get("art", {}).get("prof"))
check("残片每课心魔 +2", d1.get("heartDemon") == 2, d1.get("heartDemon"))
check("每课耗煞气 10", d1.get("exp") == 19990, d1.get("exp"))
call("POST", "/api/art/cultivate/%d" % A, TOK)
d3 = call("POST", "/api/art/cultivate/%d" % A, TOK).get("data") or {}
check("每日 3 课后余 0", d3.get("trainLeft") == 0, d3.get("trainLeft"))
check("三课熟练 15、心魔 6", d3["art"]["prof"] == 15 and d3.get("heartDemon") == 6,
      (d3["art"]["prof"], d3.get("heartDemon")))
r = call("POST", "/api/art/cultivate/%d" % A, TOK)
check("第 4 课被日限拒绝", r.get("code") == 400 and "今日已修" in (r.get("msg") or ""), r.get("msg"))

# 残片封顶：直接置到大成再修炼
sql("UPDATE demon_user_art SET stage=3, prof=0, train_date=NULL, train_count=0 WHERE user_id=%d AND art_id=%d"
    % (UID, A))
r = call("POST", "/api/art/cultivate/%d" % A, TOK)
check("残片止步大成（拒绝续修）", r.get("code") == 400 and "残篇残缺" in (r.get("msg") or ""), r.get("msg"))
r = call("POST", "/api/art/insight/%d" % A, TOK)
check("残片不可顿悟入道", r.get("code") == 400 and "残篇" in (r.get("msg") or ""), r.get("msg"))

print("== 4. 补全残片 ==")
c1 = wallet(TOK)["coin"]
r = call("POST", "/api/art/complete/%d" % A, TOK)
check("补全成功", r.get("code") == 200, r.get("msg"))
check("补全付差价 65 魔晶", wallet(TOK)["coin"] == c1 - 65, wallet(TOK)["coin"])
d = detail(TOK, A)
check("已转为完整卷且保留大成进度", d["art"]["form"] == 0 and d["art"]["stageName"] == "大成")

print("== 5. 圆满 → 入道 → 无上 ==")
sql("UPDATE demon_user_art SET stage=3, prof=390, train_date=NULL, train_count=0 WHERE user_id=%d AND art_id=%d"
    % (UID, A))
r = call("POST", "/api/art/cultivate/%d" % A, TOK)
check("大成熟练满转入圆满", (r.get("data") or {}).get("art", {}).get("stageName") == "圆满", r.get("msg"))
r = call("POST", "/api/art/cultivate/%d" % A, TOK)
check("圆满后日课不再推进", r.get("code") == 400 and "悟道机缘" in (r.get("msg") or ""), r.get("msg"))
r = call("POST", "/api/art/insight/%d" % A, TOK)
check("无悟道机缘不可入道", r.get("code") == 400 and "悟道机缘" in (r.get("msg") or ""), r.get("msg"))

INS = item_id("insight_fate")
j0 = wallet(TOK)["jade"]
r = call("POST", "/api/coin/buy", TOK, {"itemId": INS, "qty": 1})
check("藏宝阁售悟道机缘 12 血玉", r.get("code") == 200 and wallet(TOK)["jade"] == j0 - 12,
      wallet(TOK)["jade"])
r = call("POST", "/api/art/insight/%d" % A, TOK)
check("顿悟入道成功", r.get("code") == 200 and (r.get("data") or {})["art"]["stageName"] == "入道", r.get("msg"))

r = call("POST", "/api/art/transcend/%d" % A, TOK)
check("无死战悟道不可破境", r.get("code") == 400 and "死战悟道" in (r.get("msg") or ""), r.get("msg"))
TR = item_id("death_battle")
call("POST", "/api/coin/buy", TOK, {"itemId": TR, "qty": 1})
p5 = panel(TOK)
r = call("POST", "/api/art/transcend/%d" % A, TOK)
check("死战破境至无上", r.get("code") == 200 and (r.get("data") or {})["art"]["stageName"] == "无上", r.get("msg"))
p6 = panel(TOK)
check("无上战力跨阶（战力上升）", p6["power"] > p5["power"], (p5["power"], p6["power"]))
a6 = [a for a in p6["arts"] if a["id"] == A][0]
check("无上系数 1.80（完整）", abs(a6["power"] / a6["basePower"] - 1.8) < 0.02,
      (a6["power"], a6["basePower"]))

print("== 6. 心魔机制 ==")
sql("UPDATE demon_user SET heart_demon=60 WHERE id=%d" % UID)
p = panel(TOK)
check("心魔 60 战力折损 0.9", abs(p["heartPenalty"] - 0.9) < 1e-6, p["heartPenalty"])
check("心魔 60 面板标记不可冲境", p["heartBlocked"] is True and p["canBreakthrough"] is False)
r = call("POST", "/api/home/breakthrough", TOK)
check("心魔缠身阻断冲境", r.get("code") == 400 and "心魔" in (r.get("msg") or ""), r.get("msg"))
sql("UPDATE demon_user SET heart_demon=85 WHERE id=%d" % UID)
check("心魔 85 战力折损 0.75", abs(panel(TOK)["heartPenalty"] - 0.75) < 1e-6)

# 完整功法入道后以煞养心：每次修炼 -1
sql("UPDATE demon_user_art SET train_date=NULL, train_count=0, stage=5 WHERE user_id=%d AND art_id=%d"
    % (UID, A))
h0 = int(sql1("SELECT heart_demon FROM demon_user WHERE id=%d" % UID))
r = call("POST", "/api/art/cultivate/%d" % A, TOK)
check("入道后以煞养心（心魔 -1）", (r.get("data") or {}).get("heartDemon") == h0 - 1,
      (h0, (r.get("data") or {}).get("heartDemon")))

print("== 7. 后台：价格恒定 / 非法阶位 ==")
adm = (call("POST", "/api/admin/login", body={"username": "魔祖", "password": "123456"}).get("data") or {}).get("token")
body = {"name": "凝血爪", "category": "攻伐", "grade": "黄", "realmReq": "引煞",
        "status": 1, "priceCoin": 1, "priceJade": 999,
        "bonusAtk": 16, "bonusDef": 12, "bonusHp": 80, "bonusInsight": 4, "bonusAgility": 8}
r = call("PUT", "/api/admin/art/%d" % A, adm, body)
check("后台改价被忽略（仍 100 魔晶）", r.get("code") == 200 and (r.get("data") or {}).get("priceCoin") == 100,
      (r.get("data") or {}).get("priceCoin"))
r = call("PUT", "/api/admin/art/%d" % A, adm, dict(body, grade="神"))
check("非法阶位被拒", r.get("code") == 400 and "阶位" in (r.get("msg") or ""), r.get("msg"))

print("== 8. 清理 ==")
sql("DELETE ua FROM demon_user_art ua WHERE ua.user_id=%d" % UID)
sql("DELETE b FROM demon_bag b WHERE b.user_id=%d" % UID)
sql("DELETE p FROM demon_purchase p WHERE p.user_id=%d" % UID)
sql("DELETE l FROM demon_coin_log l WHERE l.user_id=%d" % UID)
sql("DELETE w FROM demon_wallet w WHERE w.user_id=%d" % UID)
sql("DELETE FROM demon_user WHERE id=%d" % UID)
check("测试数据已清理", sql1("SELECT COUNT(*) FROM demon_user WHERE id=%d" % UID) == "0")

print("\n通过 %d 项，失败 %d 项" % (len(passed), len(failed)))
if failed:
    print("失败项：" + " / ".join(failed))
    raise SystemExit(1)
