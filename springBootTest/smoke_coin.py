# -*- coding: utf-8 -*-
"""货币体系端到端冒烟：签到/参悟产出 → 钱包流水 → 商店购买 → 背包使用 → 限购/余额校验"""
import json
import subprocess
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8081"
DB = "C:/Users/czh10/mysql84/server/bin/mysql.exe"
NAME = "coin_smoke"
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
    subprocess.run([DB, "-uroot", "-p123456", "-h127.0.0.1", "-P3306", "demon_db",
                    "-e", stmt], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


# ---------- 0. 注册/登录 ----------
call("POST", "/api/auth/register", body={
    "name": NAME, "email": "coin_smoke@demon.com", "realm": "血煞", "password": PWD})
r = call("POST", "/api/auth/login", body={"name": NAME, "password": PWD})
tok = (r.get("data") or {}).get("token")
check("注册/登录测试修者", bool(tok), r.get("code"))
uid = call("GET", "/api/auth/me", tok)["data"]["id"]
print("  测试修者 id =", uid)

# 清零余额与背包，保证可重复
sql("DELETE FROM demon_wallet WHERE user_id=%d; "
    "DELETE FROM demon_coin_log WHERE user_id=%d; "
    "DELETE FROM demon_bag WHERE user_id=%d; "
    "DELETE FROM demon_purchase WHERE user_id=%d;" % (uid, uid, uid, uid))

# ---------- 1. 钱包自动开户 ----------
d = call("GET", "/api/coin/wallet", tok)
w = d.get("data") or {}
check("钱包自动开户", d.get("code") == 200 and w.get("coin") == 0 and w.get("jade") == 0, w)

# ---------- 2. 签到产出魔晶 ----------
d = call("POST", "/api/home/sign", tok)
sd = d.get("data") or {}
check("签到发魔晶", d.get("code") == 200 and sd.get("coinGain", 0) > 0,
      "coinGain=%s coin=%s" % (sd.get("coinGain"), sd.get("coin")))
sign_coin = sd.get("coinGain", 0)

d = call("GET", "/api/coin/wallet", tok)
check("签到后余额一致", d["data"]["coin"] == sign_coin, d["data"]["coin"])

# ---------- 3. 参悟产出魔晶 ----------
d = call("POST", "/api/home/explore/random", tok)
if d.get("code") == 200:
    check("参悟发魔晶", d["data"].get("coinGain") == 3, d["data"].get("coinGain"))
else:
    check("参悟发魔晶", False, d.get("msg"))

d = call("GET", "/api/coin/wallet", tok)
coin_after_explore = d["data"]["coin"]
check("参悟后余额 = 签到 + 3", coin_after_explore == sign_coin + 3, coin_after_explore)

# ---------- 4. 商店列表 ----------
d = call("GET", "/api/coin/shop", tok)
shop = (d.get("data") or {}).get("list") or []
check("商店返回在售物品", d.get("code") == 200 and len(shop) >= 8, len(shop))
pill = next((x for x in shop if x["code"] == "gather_pill"), None)
pouch = next((x for x in shop if x["code"] == "treasure_pouch"), None)
orb = next((x for x in shop if x["code"] == "abyss_orb"), None)
check("商品字段完整（价格/限购/是否买得起）",
      all(k in pill for k in ("price", "payCurrency", "remainToday", "affordable", "effectText")))
check("魔晶不足时 affordable=false", pill["affordable"] is False, pill["affordable"])

# ---------- 5. 余额不足拒绝购买 ----------
d = call("POST", "/api/coin/buy", tok, {"itemId": pill["id"], "qty": 1})
check("余额不足拒绝购买", d.get("code") == 400 and "不足" in (d.get("msg") or ""), d.get("msg"))

# ---------- 6. 注入资金后购买 ----------
sql("UPDATE demon_wallet SET coin = coin + 500, jade = jade + 5 WHERE user_id=%d;" % uid)
d = call("GET", "/api/coin/wallet", tok)
check("注资后余额 500+", d["data"]["coin"] >= 500 and d["data"]["jade"] == 5, d["data"])

d = call("POST", "/api/coin/buy", tok, {"itemId": pill["id"], "qty": 2})
check("购买聚煞丹 ×2", d.get("code") == 200 and d["data"]["payAmount"] == 120, d.get("msg"))
check("购买后背包有 2 件", d["data"]["bagQty"] == 2, d["data"]["bagQty"])

d = call("GET", "/api/coin/wallet", tok)
check("购买扣款正确", d["data"]["coin"] == 500 + sign_coin + 3 - 120, d["data"]["coin"])

# ---------- 7. 背包使用 ----------
d = call("GET", "/api/coin/bag", tok)
bag = (d.get("data") or {}).get("list") or []
check("乾坤袋含聚煞丹", any(b["code"] == "gather_pill" and b["qty"] == 2 for b in bag), len(bag))

exp_before = call("GET", "/api/auth/me", tok)["data"]["exp"]
d = call("POST", "/api/coin/use", tok, {"itemId": pill["id"], "qty": 1})
check("使用聚煞丹", d.get("code") == 200 and d["data"]["effectType"] == "exp", d.get("msg"))
exp_after = call("GET", "/api/auth/me", tok)["data"]["exp"]
check("使用后煞气 +60", exp_after == exp_before + 60, "%s → %s" % (exp_before, exp_after))

d = call("GET", "/api/coin/bag", tok)
bag = (d.get("data") or {}).get("list") or []
check("使用后背包剩 1", any(b["code"] == "gather_pill" and b["qty"] == 1 for b in bag), bag)

# ---------- 8. 每日限购 ----------
ok3 = 0
for i in range(3):
    r = call("POST", "/api/coin/buy", tok, {"itemId": pouch["id"], "qty": 1})
    if r.get("code") == 200:
        ok3 += 1
r4 = call("POST", "/api/coin/buy", tok, {"itemId": pouch["id"], "qty": 1})
check("限购 3 件：前 3 次成功", ok3 == 3, ok3)
check("限购 3 件：第 4 次拒绝", r4.get("code") == 400 and "限购" in (r4.get("msg") or ""), r4.get("msg"))

# ---------- 9. 血玉商品 ----------
d = call("POST", "/api/coin/buy", tok, {"itemId": orb["id"], "qty": 1})
check("血玉购买深渊魔珠", d.get("code") == 200 and d["data"]["payCurrency"] == "jade", d.get("msg"))
d = call("GET", "/api/coin/wallet", tok)
check("血玉扣除 3", d["data"]["jade"] == 2, d["data"]["jade"])

coin_before_orb = d["data"]["coin"]
d = call("POST", "/api/coin/use", tok, {"itemId": orb["id"], "qty": 1})
check("使用魔珠得 300 魔晶", d.get("code") == 200 and d["data"]["effectValue"] == 300, d.get("msg"))
d = call("GET", "/api/coin/wallet", tok)
check("魔珠入账 300", d["data"]["coin"] == coin_before_orb + 300, d["data"]["coin"])

# ---------- 10. 流水 ----------
d = call("GET", "/api/coin/logs?limit=50", tok)
logs = (d.get("data") or {}).get("list") or []
reasons = {l["reason"] for l in logs}
check("流水含产出与消费", {"sign", "explore", "buy", "item"} <= reasons, sorted(reasons))
check("流水金额正负正确",
      any(l["amount"] > 0 for l in logs) and any(l["amount"] < 0 for l in logs))
check("流水含中文事由", all(l.get("reasonText") for l in logs))

# ---------- 11. 匿名与鉴权 ----------
d = call("GET", "/api/coin/shop")
check("匿名可浏览商店", d.get("code") == 200 and (d["data"]["logged"] is False), d.get("code"))
d = call("GET", "/api/coin/wallet")
check("钱包无令牌 401", d.get("code") == 401, d.get("code"))
d = call("POST", "/api/coin/buy", None, {"itemId": pill["id"], "qty": 1})
check("购买无令牌 401", d.get("code") == 401, d.get("code"))

print("\n===== 结果：%d PASS / %d FAIL =====" % (len(passed), len(failed)))
if failed:
    print("失败项：", "；".join(failed))
