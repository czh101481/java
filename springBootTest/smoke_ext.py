# -*- coding: utf-8 -*-
"""鬼市 / 限时折扣 / 富甲榜 / 后台物品管理 端到端冒烟

覆盖：
  1) 后台物品管理：列表、无令牌拒绝、改价、折扣设置与清空、上下架、删除保护、新增与校验
  2) 限时折扣：商店折后价、折扣过期回落原价、购买按折后价扣款
  3) 鬼市：寄售预扣、在售列表、买自己的单被拒、他人买入（5% 抽成）、撤单退物、超量寄售被拒、价格钳制
  4) 富甲榜：匿名可看、登录带我的名次、排序口径正确
"""
import json
import subprocess
import urllib.error
import urllib.request
from datetime import datetime, timedelta

BASE = "http://127.0.0.1:8081"
DB = "C:/Users/czh10/mysql84/server/bin/mysql.exe"
A_NAME, B_NAME, PWD = "trade_a", "trade_b", "smoke123"

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


def login(name, pwd):
    d = call("POST", "/api/auth/login", body={"name": name, "password": pwd})
    return (d.get("data") or {}).get("token"), (d.get("data") or {}).get("user")


def register(name, email):
    call("POST", "/api/auth/register",
         body={"name": name, "email": email, "realm": "血煞", "password": PWD})


def minutes(dt):
    return dt.strftime("%Y-%m-%dT%H:%M")


print("=" * 68)
print("魔族修行 · 折扣 / 鬼市 / 富甲榜 冒烟")
print("=" * 68)

# ---------- 0. 管理员令牌 ----------
adm = None
for u, p in (("魔祖", "123456"), ("czh101481", "czh101481")):
    d = call("POST", "/api/admin/login", body={"username": u, "password": p})
    if d.get("code") == 200:
        adm = d["data"]["token"]
        print("  管理员：", u)
        break
check("管理员登录", bool(adm))

# ---------- 1. 后台物品管理 ----------
d = call("GET", "/api/admin/item/list")
check("后台物品列表：无令牌被拒", d.get("code") == 401, d.get("msg"))

d = call("GET", "/api/admin/item/list", adm)
items = d.get("data") or []
check("后台物品列表可读", d.get("code") == 200 and len(items) >= 8, "共 %d 件" % len(items))
gather = next((x for x in items if x["code"] == "gather_pill"), None)
check("种子物品齐全", gather is not None)
if not gather:
    raise SystemExit("缺少聚煞丹，无法继续")
gid = gather["id"]

# 恢复初始状态，保证可重复跑
call("PUT", "/api/admin/item/%d" % gid, adm,
     {"discountPercent": 0, "discountStart": "", "discountEnd": "", "status": 1,
      "priceCoin": 60, "priceJade": 0})

d = call("PUT", "/api/admin/item/%d" % gid, adm, {"priceCoin": 80})
check("改价生效", d.get("code") == 200 and d["data"]["priceCoin"] == 80, d["data"]["priceCoin"])
call("PUT", "/api/admin/item/%d" % gid, adm, {"priceCoin": 60})

d = call("PUT", "/api/admin/item/%d" % gid, adm, {"priceCoin": -5})
check("负价被拒", d.get("code") == 400, d.get("msg"))

d = call("POST", "/api/admin/item", adm,
         {"code": "bad_effect", "name": "邪物", "priceCoin": 10, "effectType": "hack"})
check("非法效果类型被拒", d.get("code") == 400, d.get("msg"))

d = call("POST", "/api/admin/item", adm, {"code": "free_item", "name": "白送之物", "priceCoin": 0, "priceJade": 0})
check("双币皆 0 被拒", d.get("code") == 400, d.get("msg"))

tmp_code = "smoke_item_%s" % datetime.now().strftime("%H%M%S")
d = call("POST", "/api/admin/item", adm,
         {"code": tmp_code, "name": "试炼符", "icon": "符", "category": "符箓", "priceCoin": 15,
          "effectType": "exp", "effectValue": 10, "dailyLimit": 1, "status": 1, "sort": 99})
new_id = (d.get("data") or {}).get("id")
check("新增物品", d.get("code") == 200 and new_id, d.get("msg"))

d = call("POST", "/api/admin/item", adm, {"code": tmp_code, "name": "重名符", "priceCoin": 10})
check("物品代号唯一", d.get("code") == 400, d.get("msg"))

d = call("DELETE", "/api/admin/item/%s" % new_id, adm)
check("无引用物品可删除", d.get("code") == 200, d.get("msg"))

# ---------- 2. 限时折扣 ----------
start = minutes(datetime.now() - timedelta(minutes=5))
end = minutes(datetime.now() + timedelta(hours=1))
d = call("PUT", "/api/admin/item/%d" % gid, adm,
         {"discountPercent": 50, "discountStart": start, "discountEnd": end})
check("设置 5 折", d.get("code") == 200 and d["data"]["discountPercent"] == 50
      and d["data"]["discountEnd"], d.get("msg"))

d = call("GET", "/api/coin/shop")
row = next((x for x in (d.get("data") or {}).get("list", []) if x["id"] == gid), None)
check("商店显示折扣", row and row["discounting"] and row["discountPercent"] == 50,
      row and (row["price"], row["originalPrice"]))
check("折后单价 = 30", row and row["price"] == 30 and row["originalPrice"] == 60,
      row and (row["price"], row["originalPrice"]))

# 折扣过期 → 回落原价
past_end = minutes(datetime.now() - timedelta(minutes=1))
call("PUT", "/api/admin/item/%d" % gid, adm, {"discountEnd": past_end})
d = call("GET", "/api/coin/shop")
row = next((x for x in (d.get("data") or {}).get("list", []) if x["id"] == gid), None)
check("折扣过期回落原价", row and not row["discounting"] and row["price"] == 60,
      row and (row["discounting"], row["price"]))

# 下架 → 商店不可见；上架 → 复现
call("PUT", "/api/admin/item/%d" % gid, adm, {"status": 0})
d = call("GET", "/api/coin/shop")
check("下架后商店不可见",
      all(x["id"] != gid for x in (d.get("data") or {}).get("list", [])))
call("PUT", "/api/admin/item/%d" % gid, adm, {"status": 1})
d = call("GET", "/api/coin/shop")
check("上架后商店复现",
      any(x["id"] == gid for x in (d.get("data") or {}).get("list", [])))

# ---------- 3. 玩家注册 / 备币 ----------
for n, e in ((A_NAME, "trade_a@demon.com"), (B_NAME, "trade_b@demon.com")):
    register(n, e)
tok_a, user_a = login(A_NAME, PWD)
tok_b, user_b = login(B_NAME, PWD)
check("测试玩家登录", bool(tok_a) and bool(tok_b))
uid_a, uid_b = user_a["id"], user_b["id"]
print("  玩家 A=%d  B=%d" % (uid_a, uid_b))

call("GET", "/api/coin/wallet", tok_a)
call("GET", "/api/coin/wallet", tok_b)
# 清空两个测试号的历史数据并各发 5000 魔晶
sql("DELETE FROM demon_trade WHERE seller_id IN (%d,%d);"
    "DELETE FROM demon_bag WHERE user_id IN (%d,%d);"
    "DELETE FROM demon_coin_log WHERE user_id IN (%d,%d);"
    "UPDATE demon_wallet SET coin=5000, jade=5, earned_coin=0, spent_coin=0 WHERE user_id IN (%d,%d);"
    % (uid_a, uid_b, uid_a, uid_b, uid_a, uid_b, uid_a, uid_b))

# ---------- 4. 按折后价购买 ----------
call("PUT", "/api/admin/item/%d" % gid, adm,
     {"discountPercent": 50, "discountStart": start, "discountEnd": end})
d = call("POST", "/api/coin/buy", tok_a, {"itemId": gid, "qty": 3})
check("折扣价成交（3×30=90）", d.get("code") == 200 and d["data"]["payAmount"] == 90
      and d["data"]["discountPercent"] == 50, d.get("data") and d["data"].get("payAmount"))
check("折扣购得 3 件", d.get("code") == 200 and d["data"]["bagQty"] == 3,
      d.get("data") and d["data"].get("bagQty"))
check("扣款后余额 4910", call("GET", "/api/coin/wallet", tok_a)["data"]["coin"] == 4910)

# 已有修者持有 / 购买记录 → 拒绝删除（防止脏数据）
d = call("DELETE", "/api/admin/item/%d" % gid, adm)
check("有持有或购买记录的物品拒绝删除",
      d.get("code") == 400 and "不可删除" in (d.get("msg") or ""), d.get("msg"))

# 折扣还原，后续按原价计价
call("PUT", "/api/admin/item/%d" % gid, adm,
     {"discountPercent": 0, "discountStart": "", "discountEnd": ""})
d = call("GET", "/api/coin/shop")
row = next((x for x in (d.get("data") or {}).get("list", []) if x["id"] == gid), None)
check("折扣字段可清空", row and not row["discounting"] and row["price"] == 60,
      row and (row["discountPercent"], row["price"]))

# ---------- 5. 鬼市 ----------
d = call("POST", "/api/trade/sell", tok_a, {"itemId": gid, "qty": 1, "price": 100, "currency": "coin"})
trade1 = (d.get("data") or {}).get("tradeId")
check("寄售成功", d.get("code") == 200 and trade1, d.get("msg"))
check("寄售即预扣（袋余 2）", d.get("code") == 200 and d["data"]["bagQty"] == 2,
      d.get("data") and d["data"].get("bagQty"))

d = call("GET", "/api/trade/list")
lst = (d.get("data") or {}).get("list", [])
mine_row = next((x for x in lst if x["id"] == trade1), None)
check("鬼市可见挂单", mine_row is not None)
check("挂单信息完整", mine_row and mine_row["itemName"] == "聚煞丹" and mine_row["total"] == 100
      and mine_row["sellerName"] == A_NAME and mine_row["currencyName"] == "魔晶",
      mine_row and (mine_row["itemName"], mine_row["total"], mine_row["sellerName"]))

d = call("POST", "/api/trade/buy", tok_a, {"tradeId": trade1})
check("不能买自家挂单", d.get("code") == 400 and "自家" in (d.get("msg") or ""), d.get("msg"))

d = call("POST", "/api/trade/buy", tok_b, {"tradeId": trade1})
check("他人买入成功", d.get("code") == 200, d.get("msg"))
check("买家扣 100", call("GET", "/api/coin/wallet", tok_b)["data"]["coin"] == 4900)
check("卖家净得 95（抽 5%）", call("GET", "/api/coin/wallet", tok_a)["data"]["coin"] == 5005,
      call("GET", "/api/coin/wallet", tok_a)["data"]["coin"])

bag_b = (call("GET", "/api/coin/bag", tok_b).get("data") or {}).get("list", [])
check("物品入买家袋", any(x["id"] == gid and x["qty"] == 1 for x in bag_b), bag_b)

d = call("POST", "/api/trade/buy", tok_b, {"tradeId": trade1})
check("已售单不可重复购买", d.get("code") == 400, d.get("msg"))

# 撤单退物
d = call("POST", "/api/trade/sell", tok_a, {"itemId": gid, "qty": 2, "price": 50, "currency": "coin"})
trade2 = (d.get("data") or {}).get("tradeId")
check("再挂一单（袋余 0）", d.get("code") == 200 and d["data"]["bagQty"] == 0,
      d.get("data") and d["data"].get("bagQty"))
d = call("POST", "/api/trade/cancel", tok_b, {"tradeId": trade2})
check("他人不可撤我的单", d.get("code") == 400, d.get("msg"))
d = call("POST", "/api/trade/cancel", tok_a, {"tradeId": trade2})
check("本人撤单成功", d.get("code") == 200 and d["data"]["bagQty"] == 2,
      d.get("data") and d["data"].get("bagQty"))

d = call("GET", "/api/trade/mine", tok_a)
rows = (d.get("data") or {}).get("list", [])
check("我的挂单含已售与已撤",
      len(rows) >= 2 and any(x["status"] == 1 for x in rows) and any(x["status"] == 2 for x in rows),
      [(x["id"], x["statusText"]) for x in rows])

d = call("POST", "/api/trade/sell", tok_a, {"itemId": gid, "qty": 99, "price": 10, "currency": "coin"})
check("超量寄售被拒", d.get("code") == 400 and "不足" in (d.get("msg") or ""), d.get("msg"))

d = call("POST", "/api/trade/sell", tok_a, {"itemId": gid, "qty": 1, "price": 99999999, "currency": "coin"})
trade3 = (d.get("data") or {}).get("tradeId")
check("单价上限钳制 99999", d.get("code") == 200 and d["data"]["price"] == 99999,
      d.get("data") and d["data"].get("price"))
call("POST", "/api/trade/cancel", tok_a, {"tradeId": trade3})

d = call("GET", "/api/trade/mine")
check("未持魔契读我的挂单被拒", d.get("code") == 401, d.get("msg"))

# ---------- 6. 富甲榜 ----------
d = call("GET", "/api/coin/rank?type=rich&limit=20")
check("匿名可看富甲榜", d.get("code") == 200 and isinstance(d["data"]["list"], list),
      len((d.get("data") or {}).get("list") or []))

d = call("GET", "/api/coin/rank?type=coin&limit=20", tok_b)
rows = (d.get("data") or {}).get("list", [])
coins = [x["coin"] for x in rows]
check("魔晶榜按余额倒序", coins == sorted(coins, reverse=True), coins[:5])
check("登录后附带我的名次", d["data"].get("myRank", 0) >= 1 and d["data"].get("myScore") == 4900,
      (d["data"].get("myRank"), d["data"].get("myScore")))
check("榜中含本人标记", any(x.get("me") for x in rows))

d = call("GET", "/api/coin/rank?type=jade&limit=20", tok_b)
rows = (d.get("data") or {}).get("list", [])
jades = [x["jade"] for x in rows]
check("血玉榜按血玉倒序", jades == sorted(jades, reverse=True), jades[:5])
check("总资产 = 魔晶 + 血玉×100", all(x["score"] == x["coin"] + x["jade"] * 100 for x in rows))

d = call("GET", "/api/coin/rank?type=rich&limit=20", tok_b)
check("总资产榜首名不低", d["data"]["list"][0]["score"] >= d["data"]["list"][-1]["score"])

# ---------- 7. 收尾：清掉测试交易与背包 ----------
sql("DELETE FROM demon_trade WHERE seller_id IN (%d,%d);"
    "DELETE FROM demon_bag WHERE user_id IN (%d,%d);"
    "DELETE FROM demon_purchase WHERE user_id IN (%d,%d);"
    "UPDATE demon_wallet SET coin=0, jade=0 WHERE user_id IN (%d,%d);"
    % (uid_a, uid_b, uid_a, uid_b, uid_a, uid_b, uid_a, uid_b))
call("PUT", "/api/admin/item/%d" % gid, adm,
     {"discountPercent": 0, "discountStart": "", "discountEnd": "", "status": 1,
      "priceCoin": 60, "priceJade": 0})
print("\n已清理测试交易 / 背包 / 购买记录，聚煞丹已复原。")

print("=" * 68)
print("通过 %d 项，失败 %d 项" % (len(passed), len(failed)))
if failed:
    print("失败项：")
    for f in failed:
        print("  - " + f)
    raise SystemExit(1)
print("全部通过 ✅")
