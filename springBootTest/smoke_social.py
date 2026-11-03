import json, urllib.request

BASE = "http://localhost:8081"

def call(method, path, token=None, body=None):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode())

def reg(name, email):
    return call("POST", "/api/auth/register", body={"name": name, "email": email, "realm": "血煞", "password": "smoke123"})

def login(name):
    s, d = call("POST", "/api/auth/login", body={"name": name, "password": "smoke123"})
    return s, d

ok = True
def check(label, cond, extra=""):
    global ok
    print(("PASS " if cond else "FAIL ") + label + ("  " + extra if extra else ""))
    if not cond:
        ok = False

# 1) 注册两个测试账号
s, d = reg("smoke_a", "smoke_a@test.com")
check("注册 A", s == 200 and d.get("code") == 200, str(d))
s, d = reg("smoke_b", "smoke_b@test.com")
check("注册 B", s == 200 and d.get("code") == 200, str(d))

# 2) 登录拿到令牌与 id
s, da = login("smoke_a"); ta = da["data"]["token"]; ida = da["data"]["user"]["id"]
s, db = login("smoke_b"); tb = db["data"]["token"]; idb = db["data"]["user"]["id"]
check("登录 A/B", ta and tb)

# 3) A 向 B 递契约
s, d = call("POST", "/api/social/friend/apply", ta, {"toName": "smoke_b", "message": "共参魔道"})
check("A 申请加 B", d.get("code") == 200, str(d))

# 3b) 反向：B 再向 A 申请应被拦截提示
s, d = call("POST", "/api/social/friend/apply", tb, {"toName": "smoke_a"})
check("B 申请已被提示(对方已递来)", d.get("code") == 400, str(d))

# 4) B 查看待处理请求，取 applyId
s, d = call("GET", "/api/social/friend/requests", tb)
reqs = d.get("data") or []
applyId = reqs[0]["applyId"] if reqs else None
check("B 收到请求", len(reqs) == 1 and applyId is not None, str(reqs))

# 5) B 同意
s, d = call("POST", "/api/social/friend/handle", tb, {"applyId": applyId, "action": "accept"})
check("B 同意申请", d.get("code") == 200, str(d))

# 6) A 好友列表含 B
s, d = call("GET", "/api/social/friend/list", ta)
friends = [f["name"] for f in (d.get("data") or [])]
check("A 好友列表含 B", "smoke_b" in friends, str(friends))

# 7) A 向 B 发文字
s, d = call("POST", "/api/social/message/send", ta, {"toId": idb, "content": "道友安好，共参魔道。", "msgType": 0})
check("A 发文字给 B", d.get("code") == 200, str(d))

# 8) A 向 B 发修炼心得卡片（art id=1）
s, d = call("POST", "/api/social/message/send", ta, {"toId": idb, "msgType": 1, "refType": "art", "refId": 1, "refTitle": "血河大法", "refSummary": "血煞一脉根本功法。"})
check("A 发心得卡片给 B", d.get("code") == 200 and d.get("data", {}).get("msgType") == 1, str(d))

# 8b) 向非好友发送应被拒
s, d = call("POST", "/api/social/message/send", ta, {"toId": 999999, "content": "x", "msgType": 0})
check("向非好友发消息被拒(403)", d.get("code") == 403, str(d))

# 9) B 查未读（应为 2：一条文字 + 一条心得）
s, d = call("GET", "/api/social/message/unread", tb)
total_before = (d.get("data") or {}).get("total", 0)
check("B 未读=2", total_before == 2, str(d.get("data")))

# 10) B 拉历史（since=0）-> 应得 2 条，并标记已读
s, d = call("GET", "/api/social/message/history", tb, ) 
# GET with body not allowed; use query params instead
s, d = call("GET", "/api/social/message/history?friendId=%d&since=0" % ida, tb)
hist = d.get("data") or []
check("B 历史含 2 条", len(hist) == 2, "count=" + str(len(hist)))

# 11) B 再查未读（已读后应为 0）
s, d = call("GET", "/api/social/message/unread", tb)
total_after = (d.get("data") or {}).get("total", 0)
check("B 已读后未读=0", total_after == 0, str(d.get("data")))

# 12) B 回 A 一条
s, d = call("POST", "/api/social/message/send", tb, {"toId": ida, "content": "善，他日魔渊再会。", "msgType": 0})
check("B 回 A", d.get("code") == 200, str(d))

# 13) A 拉历史（增量 since=0）-> 应得 3 条
s, d = call("GET", "/api/social/message/history?friendId=%d&since=0" % idb, ta)
histA = d.get("data") or []
last_id = max(m["id"] for m in histA) if histA else 0
check("A 历史含 3 条", len(histA) == 3, "count=" + str(len(histA)))

# 14) 增量轮询：B 发第 4 条后，A 用 since=last_id 只拉到 1 条
s, d = call("POST", "/api/social/message/send", tb, {"toId": ida, "content": "再赠你一语。", "msgType": 0})
call("GET", "/api/social/message/history?friendId=%d&since=%d" % (idb, last_id), ta)  # 先让 A 已读
s, d = call("POST", "/api/social/message/send", tb, {"toId": ida, "content": "第4条", "msgType": 0})
s, d = call("GET", "/api/social/message/history?friendId=%d&since=%d" % (idb, last_id), ta)
inc = d.get("data") or []
check("A 增量只拉到新消息", len(inc) == 1 and inc[0]["content"] == "第4条", "count=" + str(len(inc)))

print("\n==== 结果:", "全部通过 ✅" if ok else "存在失败 ❌", "====")
