import json, urllib.request
BASE = "http://localhost:8081"
def call(method, path, token=None, body=None):
    req = urllib.request.Request(BASE+path, data=(json.dumps(body).encode() if body is not None else None), method=method)
    req.add_header("Content-Type","application/json")
    if token: req.add_header("Authorization","Bearer "+token)
    try:
        with urllib.request.urlopen(req, timeout=10) as r: return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e: return e.code, json.loads(e.read().decode())

# 复用 smoke_a / smoke_b
_, d = call("POST","/api/auth/login",body={"name":"smoke_a","password":"smoke123"}); sa = d["data"]["token"]
_, d = call("POST","/api/auth/login",body={"name":"smoke_b","password":"smoke123"}); sb = d["data"]["token"]
_, d = call("GET","/api/auth/me",sa); ida = d["data"]["id"]
_, d = call("GET","/api/auth/me",sb); idb = d["data"]["id"]

# A -> B 发一条，记录其 id
_, d = call("POST","/api/social/message/send",sa,{"toId":idb,"content":"基线消息","msgType":0})
base_id = d["data"]["id"]
print("基线消息 id =", base_id)

# B 取历史 since=base_id 应为 0 条（无新消息）
_, d = call("GET","/api/social/message/history?friendId=%d&since=%d"%(ida,base_id), sb)
print("since=base 时 B 拉到条数:", len(d["data"]), "(期望 0)")

# B -> A 发一条新消息（新 id）
_, d = call("POST","/api/social/message/send",sb,{"toId":ida,"content":"增量新消息","msgType":0})
new_id = d["data"]["id"]
print("新消息 id =", new_id)

# A 取历史 since=base_id 应只拉到 1 条（即这条新消息）
_, d = call("GET","/api/social/message/history?friendId=%d&since=%d"%(idb,base_id), sa)
inc = d["data"]
print("since=base 时 A 拉到条数:", len(inc), "内容:", [m["content"] for m in inc], "(期望 1 且为 增量新消息)")
print("RESULT:", "PASS" if (len(inc)==1 and inc[0]["content"]=="增量新消息") else "FAIL")
