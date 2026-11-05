# -*- coding: utf-8 -*-
"""境界分重 / 功法分重与购买 / 修为数值面板 端到端冒烟

覆盖：
  1) 境界分重：九重推算、进度字段、九重圆满方可冲境、冲境后回落一重
  2) 功法阁：黄/玄级分重、品阶重数上限、同系列前置重、已习重复、境界门槛、血玉不足
  3) 货币购得：魔晶扣款、余额变化、已习列表
  4) 数值面板：五维、来源拆解、战力公式、功法加成入账
  5) 后台：重数超限拒绝、上下架生效、已习功法删除保护、新增/删除
"""
import json
import subprocess
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8081"
DB = "C:/Users/czh10/mysql84/server/bin/mysql.exe"
NAME, PWD = "art_smoke", "smoke123"

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
    return (d.get("data") or {}).get("token")


def register(name, pwd):
    return call("POST", "/api/auth/register",
                body={"name": name, "email": name + "@smoke.local", "realm": "血煞", "password": pwd})


def arts(token):
    d = call("GET", "/api/home/list?type=art", token)
    return d.get("data") or []


def art_id(token, name):
    for a in arts(token):
        if a.get("title") == name:
            return a.get("id")
    return None


def wallet(token):
    return (call("GET", "/api/coin/wallet", token).get("data") or {})


# ==================== 准备 ====================
print("== 准备测试修者 ==")
tok = login(NAME, PWD)
if not tok:
    register(NAME, PWD)
    tok = login(NAME, PWD)
check("测试修者登录", bool(tok))

sql("DELETE ua FROM demon_user_art ua JOIN demon_user u ON u.id = ua.user_id WHERE u.name = '%s';" % NAME)
sql("UPDATE demon_user SET cultivation='引煞', layer=1, exp=0, break_fail=2 WHERE name='%s';" % NAME)
sql("INSERT INTO demon_wallet (user_id, coin, jade, earned_coin, spent_coin, earned_jade, spent_jade) "
    "SELECT id, 5000, 0, 0, 0, 0, 0 FROM demon_user WHERE name='%s' "
    "ON DUPLICATE KEY UPDATE coin=5000, jade=0;" % NAME)

adm = (call("POST", "/api/admin/login", body={"username": "魔祖", "password": "123456"}).get("data") or {}).get("token")
check("后台登录", bool(adm))

# ==================== 1. 境界分重 ====================
print("== 1. 境界分重 ==")
st = call("GET", "/api/home/realm", tok).get("data") or {}
check("境界状态含重数", st.get("layer") == 1 and st.get("layerName") == "一", st.get("layer"))
check("每境重数上限 9", st.get("layerMax") == 9, st.get("layerMax"))
check("本重进度字段齐全", st.get("layerStartExp") == 0 and st.get("layerNextExp", 0) > 0,
      (st.get("layerStartExp"), st.get("layerNextExp")))
check("引煞一重非九重圆满", st.get("layerFull") is False)

d = call("POST", "/api/home/breakthrough", tok)
check("未满九重不可冲境", d.get("code") == 400 and "九重圆满" in (d.get("msg") or ""), d.get("msg"))

sql("UPDATE demon_user SET exp=400 WHERE name='%s';" % NAME)
st = call("GET", "/api/home/realm", tok).get("data") or {}
check("煞气满 400 自动九重", st.get("layer") == 9, st.get("layer"))
check("九重圆满可冲境", st.get("layerFull") is True and st.get("canBreakthrough") is True,
      (st.get("layerFull"), st.get("canBreakthrough")))
d = call("POST", "/api/home/breakthrough", tok)
check("冲境成功", d.get("code") == 200, d.get("msg"))
d2 = call("GET", "/api/auth/me", tok).get("data") or {}
check("冲境后境界=凝煞且回落一重", d2.get("cultivation") == "凝煞" and d2.get("layer") == 1,
      (d2.get("cultivation"), d2.get("layer")))

# ==================== 2. 功法阁分重与购买 ====================
print("== 2. 功法阁分重与购买 ==")
sql("UPDATE demon_user SET cultivation='引煞', layer=1, exp=0 WHERE name='%s';" % NAME)
lst = arts(tok)
names = [a.get("title") for a in lst]
check("功法阁含黄级分重功法", "凝血爪·一重" in names and "凝血爪·四重" in names)
check("功法阁含玄级分重功法", "玄阴指·一重" in names and "玄阴指·六重" in names)
check("列表展示品阶·重与售价",
      any("黄·一重" in (a.get("extra") or "") and "魔晶" in (a.get("extra") or "") for a in lst))
check("功法总数 >= 26", len(lst) >= 26, len(lst))

g1 = art_id(tok, "凝血爪·一重")
g4 = art_id(tok, "凝血爪·四重")
x1 = art_id(tok, "玄阴指·一重")
xian = art_id(tok, "血煞戮天诀")

d = call("POST", "/api/art/buy/%s" % g1)
check("无令牌不可购", d.get("code") == 401, d.get("msg"))

d = call("POST", "/api/art/buy/%s" % art_id(tok, "铁骨煞体·二重"), tok)
check("越级买同系列第二重被拒（须先习一重）",
      d.get("code") == 400 and "递进" in (d.get("msg") or ""), d.get("msg"))

d = call("POST", "/api/art/buy/%s" % x1, tok)
check("境界不足不可购玄级", d.get("code") == 400 and "修为不足" in (d.get("msg") or ""), d.get("msg"))

d = call("POST", "/api/art/buy/%s" % art_id(tok, "玄阴养魂诀"), tok)
check("血玉不足不可购地级", d.get("code") == 400 and "血玉不足" in (d.get("msg") or ""), d.get("msg"))

before = wallet(tok).get("coin")
d = call("POST", "/api/art/buy/%s" % g1, tok)
check("购得黄级一重成功", d.get("code") == 200, d.get("msg"))
after = wallet(tok).get("coin")
check("扣款 60 魔晶", before - after == 60, (before, after))
check("购买返回数值面板", ((d.get("data") or {}).get("stats", {}) or {}).get("power", 0) > 0)

d = call("POST", "/api/art/buy/%s" % g1, tok)
check("重复购得被拒", d.get("code") == 400 and "早已" in (d.get("msg") or ""), d.get("msg"))

before = wallet(tok).get("coin")
d = call("POST", "/api/art/buy/%s" % art_id(tok, "凝血爪·二重"), tok)
check("购得同系列二重成功", d.get("code") == 200, d.get("msg"))
check("二重扣款 140 魔晶", before - wallet(tok).get("coin") == 140, before - wallet(tok).get("coin"))

mine = call("GET", "/api/art/mine", tok).get("data") or {}
mlist = mine.get("list") or []
check("已习列表 2 部", len(mlist) == 2, len(mlist))
check("已习列表含重与数值",
      all(("layerText" in a and "bonusAtk" in a) for a in mlist))

# ==================== 3. 数值面板 ====================
print("== 3. 修为数值面板 ==")
p = call("GET", "/api/cultivate/panel", tok).get("data") or {}
check("面板含五维", len(p.get("attrs") or []) == 5, len(p.get("attrs") or []))
check("面板含已习功法", len(p.get("arts") or []) == 2)
atk = next((a["value"] for a in p.get("attrs", []) if a["key"] == "atk"), 0)
check("攻伐 = 基础+功法(4+8)", atk == 10 + 12, atk)
defn = next((a["value"] for a in p.get("attrs", []) if a["key"] == "def"), 0)
check("护体 = 基础+功法(3+6)", defn == 8 + 9, defn)
calc = round((p.get("powerRealm", 0) + p.get("powerExp", 0) + p.get("powerArt", 0))
             * (1 + (p.get("originPowerMod") or 0)))
check("战力公式自洽", abs(calc - (p.get("power") or 0)) <= 1, (calc, p.get("power")))
check("功法战力 > 0", (p.get("powerArt") or 0) > 0, p.get("powerArt"))
check("面板计重数", p.get("layer") == 1 and p.get("layerName") == "一")

d = call("GET", "/api/cultivate/rules")
check("数值规则可公开读取", d.get("code") == 200 and (d.get("data") or {}).get("layersPerRealm") == 9)

# ==================== 4. 后台管理 ====================
print("== 4. 后台功法管理 ==")
d = call("POST", "/api/admin/art", adm, body={
    "name": "冒烟·越限功", "grade": "黄", "layer": 5, "category": "攻伐", "realmReq": "引煞"})
check("重数超品阶上限被拒", d.get("code") == 400 and "至多 4 重" in (d.get("msg") or ""), d.get("msg"))

d = call("POST", "/api/admin/art", adm, body={
    "name": "冒烟·合法功", "grade": "玄", "layer": 6, "category": "护体", "realmReq": "引煞",
    "priceCoin": 100, "bonusAtk": 1})
check("合法新增功法", d.get("code") == 200, d.get("msg"))
new_id = ((d.get("data") or {}).get("id"))
d = call("DELETE", "/api/admin/art/%s" % new_id, adm)
check("无修者习得可删除", d.get("code") == 200, d.get("msg"))

d = call("DELETE", "/api/admin/art/%s" % g1, adm)
check("已习功法禁止删除", d.get("code") == 400 and "下架" in (d.get("msg") or ""), d.get("msg"))

# 下架黄级三重 → 列表不可见、不可购买
g3 = art_id(tok, "凝血爪·三重")
call("PUT", "/api/admin/art/%s" % g3, adm, body={
    "name": "凝血爪·三重", "grade": "黄", "layer": 3, "category": "攻伐", "realmReq": "凝煞",
    "priceCoin": 260, "status": 0})
names2 = [a.get("title") for a in arts(tok)]
check("下架功法不在列表", "凝血爪·三重" not in names2)
d = call("POST", "/api/art/buy/%s" % g3, tok)
check("下架功法不可购", d.get("code") == 400 and "撤下" in (d.get("msg") or ""), d.get("msg"))
call("PUT", "/api/admin/art/%s" % g3, adm, body={
    "name": "凝血爪·三重", "grade": "黄", "layer": 3, "category": "攻伐", "realmReq": "凝煞",
    "priceCoin": 260, "status": 1})
check("恢复上架", "凝血爪·三重" in [a.get("title") for a in arts(tok)])

# ==================== 5. 清理 ====================
print("== 5. 清理测试数据 ==")
sql("DELETE ua FROM demon_user_art ua JOIN demon_user u ON u.id = ua.user_id WHERE u.name = '%s';" % NAME)
sql("UPDATE demon_wallet w JOIN demon_user u ON u.id = w.user_id SET w.coin=0, w.jade=0 "
    "WHERE u.name='%s';" % NAME)
sql("UPDATE demon_user SET cultivation='引煞', layer=1, exp=0 WHERE name='%s';" % NAME)

print("\n== 结果：%d 项通过，%d 项失败 ==" % (len(passed), len(failed)))
if failed:
    print("失败项：")
    for f in failed:
        print("  - " + f)
    raise SystemExit(1)
