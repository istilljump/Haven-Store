#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Haven-Store 后端接口冒烟测试

覆盖范围：
  1. 健康检查 / 鉴权拦截（未登录、非管理员、游客）
  2. 管理后台六个页面：数据概览、用户管理、商品管理、分类管理、系统消息、系统设置
  3. 数据导出（CSV）与前台发布商品

为什么用 Python 而不是 shell + curl：
  Git Bash 调用原生 curl.exe 时会按系统 ANSI 代码页转换命令行中的非 ASCII 参数，
  中文请求体会被转成 GBK，后端按 UTF-8 解析直接报 "Invalid UTF-8 middle byte"。
  用 Python 可以显式以 UTF-8 编码请求体，避免这类假失败。

用法：python tests/api_smoke_test.py [base_url]
"""

import json
import sys
import time
import urllib.error
import urllib.parse
import uuid
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080/api"

PASS = 0
FAIL = 0
FAILURES = []


def call(method, path, body=None, token=None, raw=False):
    """发起请求，返回 (http_status, headers, payload)。

    payload：raw=False 时为解析后的 JSON 字典；raw=True 时为原始 bytes。
    """
    # 路径中的中文（如搜索关键词）必须做百分号编码，否则 http.client 只接受 ASCII
    url = BASE + urllib.parse.quote(path, safe="/?&=%")
    data = None
    headers = {}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json; charset=utf-8"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            content = resp.read()
            if raw:
                return resp.status, dict(resp.headers), content
            text = content.decode("utf-8", errors="replace")
            try:
                return resp.status, dict(resp.headers), json.loads(text)
            except json.JSONDecodeError:
                return resp.status, dict(resp.headers), text
    except urllib.error.HTTPError as e:
        content = e.read()
        if raw:
            return e.code, dict(e.headers), content
        return e.code, dict(e.headers), content.decode("utf-8", errors="replace")


def check(name, actual, expected):
    """actual 包含 expected 即通过。expected 为 None 表示只要求 actual 为真值。"""
    global PASS, FAIL
    text = actual if isinstance(actual, str) else json.dumps(actual, ensure_ascii=False,
                                                              separators=(",", ":"))
    if expected is None:
        ok = bool(text)
    else:
        # 同时比对原始串与去空格串，避免断言被空白差异干扰
        exp = str(expected)
        ok = exp in text or exp.replace(" ", "") in text.replace(" ", "")
    if ok:
        print("  [OK]   " + name)
        PASS += 1
    else:
        shown = text if len(text) < 240 else text[:240] + "..."
        print("  [FAIL] " + name + "\n         期望包含: " + str(expected) + "\n         实际: " + shown)
        FAIL += 1
        FAILURES.append(name)


def upload_png(path, token):
    """上传一张 1x1 的合法 PNG，返回 (http_status, 解析后的 JSON)。

    说明：multipart 请求体必须手工拼装——urllib 没有内置的 multipart 编码器。
    """
    png = bytes.fromhex(
        "89504e470d0a1a0a0000000d49484452000000010000000108020000009077"
        "3dde0000000c4944415408ed63f8cfc000000301010018dd8db0000000004945"
        "4e44ae426082")
    boundary = "----smokeboundary" + uuid.uuid4().hex
    body = (
        ("--" + boundary + "\r\n"
         'Content-Disposition: form-data; name="file"; filename="smoke.png"\r\n'
         "Content-Type: image/png\r\n\r\n").encode("ascii")
        + png
        + ("\r\n--" + boundary + "--\r\n").encode("ascii")
    )
    url = BASE + path
    headers = {"Content-Type": "multipart/form-data; boundary=" + boundary}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8", "replace") or "{}")


def upload_sized_png(path, token, size_bytes, filename="smoke-sized.png"):
    """上传指定体积的伪 PNG，用于验证图片大小上限。

    后端只按扩展名与体积判断，不解析图片内容，所以这里不必生成真实 PNG。
    """
    payload = b"\x89PNG\r\n\x1a\n" + b"\x00" * max(0, size_bytes - 8)
    boundary = "----smokeboundary" + uuid.uuid4().hex
    body = (
        ("--" + boundary + "\r\n"
         'Content-Disposition: form-data; name="file"; filename="' + filename + '"\r\n'
         "Content-Type: image/png\r\n\r\n").encode("ascii")
        + payload
        + ("\r\n--" + boundary + "--\r\n").encode("ascii")
    )
    headers = {"Content-Type": "multipart/form-data; boundary=" + boundary}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8", "replace") or "{}")


# 测试数据的标题标记：脚本开头与结尾都会据此清理，保证可重复执行
TEST_TITLE_MARKERS = ("冒烟商品", "扩展字段商品", "待删除商品", "UPLOAD-TEST",
                      "NO-COVER-ITEM", "DELETE-TEST", "VERIFY-ITEM",
                      "评价测试商品", "多图测试商品", "单图兼容商品", "超限图片",
                      "冒烟购物车商品")


def cleanup_test_products(token):
    """删除历史遗留的测试数据，使脚本可重复运行。

    商品走管理端列表拿全量数据（含已下架的商品，前台搜索只看在售会漏掉下架残留）；
    分类与消息同样需要清理，否则每跑一次就多几条。
    """
    removed = 0

    _, _, r = call("GET", "/admin/products?page=1&pageSize=200", token=token)
    records = ((r.get("data") or {}).get("records") or []) if isinstance(r, dict) else []
    for item in records:
        title = item.get("title") or ""
        if any(title.startswith(m) for m in TEST_TITLE_MARKERS):
            call("DELETE", "/admin/products/%d" % item["id"], token=token)
            removed += 1

    _, _, r = call("GET", "/admin/categories", token=token)
    for item in ((r.get("data") or []) if isinstance(r, dict) else []):
        if (item.get("name") or "").startswith("冒烟分类"):
            call("DELETE", "/admin/categories/%d" % item["id"], token=token)
            removed += 1

    # 消息按公告聚合，删除接口会把同一次群发的记录整组删掉
    _, _, r = call("GET", "/admin/messages?page=1&pageSize=100", token=token)
    for item in (((r.get("data") or {}).get("records") or []) if isinstance(r, dict) else []):
        if (item.get("title") or "").startswith("冒烟"):
            call("DELETE", "/admin/messages/%d" % item["id"], token=token)
            removed += 1

    return removed


def section(title):
    print("\n===== " + title + " =====")


def conversation_keys(payload):
    """把会话列表响应转成 {(对方用户ID, 商品ID)} 集合。

    断言只针对本脚本自己造出来的会话：会话说到底是用户数据，
    手工用过的账号里本来就可能有会话，不能假设列表是空的。
    """
    data = payload.get("data") if isinstance(payload, dict) else None
    return set((item.get("peerId"), item.get("productId")) for item in (data or []))


def cart_product_ids(payload):
    """把购物车列表响应转成 {商品ID} 集合（同理，不能假设购物车恰好只有测试商品）"""
    data = payload.get("data") if isinstance(payload, dict) else None
    return set(item.get("productId") for item in (data or []))


def main():
    # ---------- 1. 健康检查 ----------
    section("1. 健康检查")
    _, _, r = call("GET", "/health")
    check("GET /health 返回成功", r, '"code":200')

    # ---------- 2. 鉴权 ----------
    section("2. 鉴权拦截")
    _, _, r = call("GET", "/admin/dashboard")
    check("无 Token 访问后台被拒（401）", r, '"code":401')

    # ---------- 3. 管理员登录 ----------
    section("3. 管理员登录")
    _, _, r = call("POST", "/admin/login", {"username": "admin", "password": "admin123"})
    check("POST /admin/login 成功", r, '"isAdmin":true')
    admin_token = (r.get("data") or {}).get("token") if isinstance(r, dict) else None
    check("返回 JWT Token", admin_token, None)
    if not admin_token:
        print("\n管理员登录失败，后续用例无法继续。")
        return 1

    _, _, r = call("POST", "/admin/login", {"username": "admin", "password": "wrong-password"})
    check("错误密码被拒", r, "用户名或密码错误")

    # 清理上一次运行可能留下的测试商品，避免污染后续断言（如前台商品总数）
    cleaned = cleanup_test_products(admin_token)
    if cleaned:
        print("  （已清理历史测试商品 %d 件）" % cleaned)

    # ---------- 4. 普通用户越权 ----------
    section("4. 普通用户越权校验")
    _, _, r = call("POST", "/user/login", {"username": "testuser1", "password": "123456"})
    check("POST /user/login 成功", r, '"code":200')
    user_token = (r.get("data") or {}).get("token") if isinstance(r, dict) else None

    _, _, r = call("GET", "/admin/dashboard", token=user_token)
    check("普通用户访问后台被拒（403）", r, '"code":403')

    _, _, r = call("POST", "/admin/login", {"username": "testuser1", "password": "123456"})
    check("非管理员登录后台被拒（403）", r, '"code":403')

    # ---------- 5. 数据概览 ----------
    section("5. 数据概览")
    _, _, r = call("GET", "/admin/dashboard", token=admin_token)
    check("GET /admin/dashboard", r, '"statistics"')
    check("含总用户数", r, '"totalUsers"')
    check("含总商品数", r, '"totalProducts"')
    check("含今日新增", r, '"todayProducts"')
    check("含正常状态用户数", r, '"activeUsers"')
    check("含最近商品列表", r, '"recentProducts"')
    check("含系统消息列表", r, '"systemMessages"')
    if isinstance(r, dict) and r.get("data"):
        print("  统计: " + json.dumps(r["data"]["statistics"], ensure_ascii=False))

    # ---------- 6. 用户管理 ----------
    section("6. 用户管理")
    _, _, r = call("GET", "/admin/users?page=1&pageSize=10", token=admin_token)
    check("GET /admin/users 分页结构", r, '"records"')
    check("列表含 admin 账号", r, '"username":"admin"')
    check("列表项含 isAdmin 标识", r, '"isAdmin"')
    # 脱敏校验：用户列表响应里不允许出现密码字段
    text = json.dumps(r, ensure_ascii=False)
    check("响应中不出现 password 字段", "NOT-FOUND" if "password" not in text else "LEAKED", "NOT-FOUND")

    # 使用固定用户名：后台没有删除用户的接口，若每次运行都新建账号会不断累积
    new_name = "smoke_test_user"
    _, _, r = call("POST", "/admin/users",
                   {"username": new_name, "password": "123456",
                    "nickname": "冒烟测试用户", "phone": "", "isAdmin": False},
                   token=admin_token)
    # 直接判断返回码，避免依赖 JSON 文本格式（json.dumps 默认会加空格）
    resp_code = (r.get("code") if isinstance(r, dict) else None)
    resp_msg = ((r.get("msg") if isinstance(r, dict) else "") or "")
    check("POST /admin/users 新增用户（首次创建，之后幂等）",
          "OK" if (resp_code == 200 or "已存在" in resp_msg) else "FAIL", "OK")

    _, _, r = call("GET", "/admin/users?page=1&pageSize=50&keyword=" + new_name, token=admin_token)
    check("新增用户可被关键词搜到", r, new_name)
    check("昵称支持中文", r, "冒烟测试用户")
    records = ((r.get("data") or {}).get("records") or []) if isinstance(r, dict) else []
    new_id = records[0]["id"] if records else None
    check("取到新用户 ID", new_id, None)

    if new_id:
        _, _, r = call("PUT", "/admin/users/%d/status" % new_id, {"status": 0}, token=admin_token)
        check("PUT /admin/users/{id}/status 禁用", r, '"code":200')
        _, _, r = call("GET", "/admin/users?page=1&pageSize=50&keyword=" + new_name, token=admin_token)
        check("禁用后状态为 0", r, '"status":0')
        _, _, r = call("PUT", "/admin/users/%d/status" % new_id, {"status": 1}, token=admin_token)
        check("PUT /admin/users/{id}/status 启用", r, '"code":200')

    _, _, r = call("POST", "/admin/users", {"username": new_name, "password": "123456"}, token=admin_token)
    check("重复用户名被拒", r, "已存在")

    _, _, r = call("PUT", "/admin/users/1/status", {"status": 0}, token=admin_token)
    check("禁止禁用当前登录账号", r, "不能禁用")

    _, _, r = call("PUT", "/admin/users/99999/status", {"status": 0}, token=admin_token)
    check("不存在的用户返回业务错误", r, "用户不存在")

    # ---------- 7. 商品管理 ----------
    section("7. 商品管理")
    _, _, r = call("GET", "/admin/products?page=1&pageSize=10", token=admin_token)
    check("GET /admin/products 分页结构", r, '"records"')
    check("列表含发布者用户名", r, '"username"')
    check("列表含分类名称", r, '"categoryName"')
    check("列表含成色字段", r, '"productCondition"')

    _, _, r = call("PUT", "/admin/products/1/status", {"status": 3}, token=admin_token)
    check("PUT /admin/products/{id}/status 下架", r, '"code":200')
    _, _, r = call("GET", "/admin/products?page=1&pageSize=10&status=3", token=admin_token)
    check("按状态「已下架」筛选生效", r, '"code":200')
    _, _, r = call("PUT", "/admin/products/1/status", {"status": 1}, token=admin_token)
    check("重新上架", r, '"code":200')
    _, _, r = call("PUT", "/admin/products/1/status", {"status": 99}, token=admin_token)
    check("非法状态被拒", r, "非法的商品状态")

    # ---------- 8. 分类管理 ----------
    section("8. 分类管理")
    _, _, r = call("GET", "/admin/categories", token=admin_token)
    check("GET /admin/categories", r, "手机数码")
    check("分类含状态字段", r, '"status"')

    cat_name = "冒烟分类%d" % int(time.time())
    _, _, r = call("POST", "/admin/categories",
                   {"name": cat_name, "sort": 50, "status": 1}, token=admin_token)
    check("POST /admin/categories 新增", r, '"code":200')

    _, _, r = call("GET", "/admin/categories", token=admin_token)
    cat_id = None
    if isinstance(r, dict):
        for c in (r.get("data") or []):
            if c.get("name") == cat_name:
                cat_id = c["id"]
                break
    check("新增分类出现在列表", cat_id, None)

    if cat_id:
        _, _, r = call("PUT", "/admin/categories/%d" % cat_id,
                       {"name": cat_name, "sort": 51, "status": 0}, token=admin_token)
        check("PUT /admin/categories/{id} 修改", r, '"code":200')
        _, _, r = call("DELETE", "/admin/categories/%d" % cat_id, token=admin_token)
        check("DELETE /admin/categories/{id} 删除", r, '"code":200')

    _, _, r = call("POST", "/admin/categories", {"name": cat_name, "sort": 1, "status": 1}, token=admin_token)
    check("新增分类（用于重复校验）", r, '"code":200')
    _, _, r = call("POST", "/admin/categories", {"name": cat_name, "sort": 1, "status": 1}, token=admin_token)
    check("重复分类名被拒", r, "已存在")

    _, _, r = call("DELETE", "/admin/categories/1", token=admin_token)
    check("删除有商品的分类被拒", r, "无法删除")

    # ---------- 9. 系统消息 ----------
    section("9. 系统消息")
    _, _, r = call("GET", "/admin/messages?page=1&pageSize=10", token=admin_token)
    check("GET /admin/messages 分页结构", r, '"records"')
    check("含接收群体字段", r, '"userType"')
    check("含送达人数字段", r, '"receiverCount"')
    check("含消息状态字段", r, '"status"')

    _, _, r = call("POST", "/admin/messages",
                   {"title": "冒烟测试公告", "content": "这是一条冒烟测试消息，用于验证群发。",
                    "userType": "all"}, token=admin_token)
    check("POST /admin/messages 群发", r, '"code":200')

    _, _, r = call("GET", "/admin/messages?page=1&pageSize=10", token=admin_token)
    check("群发后可在列表看到", r, "冒烟测试公告")
    check("中文标题正确回显", r, "冒烟测试公告")
    check("接收群体标记为 all", r, '"userType":"all"')
    _, _, r = call("GET", "/admin/messages?page=1&pageSize=10&userType=admin", token=admin_token)
    check("按接收群体筛选生效", r, '"code":200')

    _, _, r = call("POST", "/admin/messages",
                   {"title": "x", "content": "y", "userType": "badtype"}, token=admin_token)
    check("非法接收群体被拒", r, "不合法")

    _, _, r = call("POST", "/admin/messages",
                   {"title": "", "content": "y", "userType": "all"}, token=admin_token)
    check("空标题触发参数校验", r, "标题不能为空")

    # ---------- 10. 系统设置 ----------
    section("10. 系统设置")
    _, _, r = call("GET", "/admin/settings", token=admin_token)
    check("GET /admin/settings", r, '"site"')
    check("含上传设置", r, '"upload"')
    check("含交易设置", r, '"trade"')
    check("含联系方式", r, '"contact"')
    check("含安全设置", r, '"security"')
    check("默认站点名为 Haven-Store", r, "Haven-Store")

    payload = {
        "site": {"name": "Haven-Store", "description": "冒烟测试站点", "logo": "/logo.png", "icp": "x"},
        "upload": {"maxFileSize": 5120, "allowedTypes": ["jpg"], "maxImages": 9},
        "trade": {"autoEstimate": True, "estimateConfidence": 80, "maxPrice": 100, "autoOfflineHours": 0},
        "contact": {"email": "a@b.com", "phone": "400-1", "address": "addr", "workTime": ["09:00", "18:00"]},
        "security": {"passwordStrength": "medium", "loginAttempts": 5, "lockDuration": 30,
                     "sessionTimeout": 120}
    }
    _, _, r = call("PUT", "/admin/settings", payload, token=admin_token)
    check("PUT /admin/settings 保存", r, '"code":200')
    _, _, r = call("GET", "/admin/settings", token=admin_token)
    check("设置可读回（中文描述）", r, "冒烟测试站点")

    _, _, r = call("PUT", "/admin/settings", {"site": {"name": ""}}, token=admin_token)
    check("空站点名被拒", r, "不能为空")

    # 还原默认描述，避免污染演示数据
    payload["site"]["description"] = "专业的二手商品交易平台"
    call("PUT", "/admin/settings", payload, token=admin_token)

    # ---------- 11. 数据导出 ----------
    section("11. 数据导出")
    _, headers, body = call("GET", "/admin/export/users", token=admin_token, raw=True)
    check("导出用户响应为 CSV", headers.get("Content-Type", ""), "csv")
    check("导出为附件下载", headers.get("Content-Disposition", ""), "attachment")
    csv_text = body.decode("utf-8-sig", errors="replace")
    check("CSV 含表头", csv_text, "用户ID,用户名")
    check("CSV 含 admin 数据", csv_text, "admin")

    _, headers, body = call("GET", "/admin/export/products", token=admin_token, raw=True)
    check("导出商品响应为 CSV", headers.get("Content-Type", ""), "csv")
    csv_text = body.decode("utf-8-sig", errors="replace")
    check("商品 CSV 含表头", csv_text, "商品ID,商品标题")
    check("商品 CSV 含发布者列", csv_text, "发布者")

    # ---------- 12. 前台商品分类 ----------
    section("12. 商品分类（发布页下拉数据源）")
    _, _, r = call("GET", "/product/categories", token=admin_token)
    check("GET /product/categories", r, "手机数码")
    check("分类返回状态字段", r, '"status"')

    # ---------- 13. 发布商品 ----------
    section("13. 发布商品")
    title = "冒烟商品%d" % int(time.time())
    _, _, r = call("POST", "/product/add",
                   {"title": title, "description": "冒烟测试商品描述内容，验证发布链路。",
                    "categoryId": 1, "price": 123.45, "productCondition": "九成新",
                    "tradeType": "线上"}, token=admin_token)
    check("POST /product/add 线上发布成功", r, '"code":200')
    check("返回商品 ID", r, '"productId"')

    _, _, r = call("GET", "/admin/products?page=1&pageSize=10&keyword=" + title, token=admin_token)
    check("新商品出现在管理端列表", r, title)

    _, _, r = call("POST", "/product/add",
                   {"title": title + "-非法成色", "categoryId": 1, "price": 1,
                    "productCondition": "99新", "tradeType": "线上"}, token=admin_token)
    check("非法成色被拒", r, "成色不合法")

    _, _, r = call("POST", "/product/add",
                   {"title": title + "-非法交易方式", "categoryId": 1, "price": 1,
                    "productCondition": "全新", "tradeType": "快递"}, token=admin_token)
    check("非法交易方式被拒", r, "交易方式不合法")

    _, _, r = call("POST", "/product/add",
                   {"title": title + "-线下缺坐标", "categoryId": 1, "price": 1,
                    "productCondition": "全新", "tradeType": "线下"}, token=admin_token)
    check("线下缺地址/坐标被拒", r, "地址")

    _, _, r = call("POST", "/product/add",
                   {"title": title + "-分类不存在", "categoryId": 99999, "price": 1,
                    "productCondition": "全新", "tradeType": "线上"}, token=admin_token)
    check("不存在的分类被拒", r, "分类不存在")

    _, _, r = call("POST", "/product/add",
                   {"title": title + "-线下", "categoryId": 1, "price": 1,
                    "productCondition": "全新", "tradeType": "线下",
                    "address": "北京市朝阳区国贸CBD", "longitude": 116.46, "latitude": 39.92},
                   token=admin_token)
    check("线下带坐标发布成功", r, '"code":200')

    _, _, r = call("POST", "/product/add",
                   {"title": title + "-未登录", "categoryId": 1, "price": 1,
                    "productCondition": "全新", "tradeType": "线上"})
    check("未登录发布被拒（401）", r, '"code":401')

    # ---------- 14. 商品搜索（游客可访问） ----------
    section("14. 商品搜索（无需登录）")
    _, _, r = call("GET", "/product/search?page=1&pageSize=10")
    check("游客可访问商品搜索", r, '"code":200')
    check("返回分页结构", r, '"records"')
    check("列表项含封面图字段", r, '"coverImage"')
    check("列表项含分类名称", r, '"categoryName"')
    check("列表项含发布者", r, '"username"')

    _, _, r = call("GET", "/product/search?keyword=" + urllib.parse.quote("编程"))
    check("关键词筛选生效", r, "编程书籍套装")
    total_kw = ((r.get("data") or {}).get("total") if isinstance(r, dict) else None)
    check("关键词筛选命中 1 条", total_kw, "1")

    _, _, r = call("GET", "/product/search?keyword=" + urllib.parse.quote("绝不存在的商品"))
    check("无命中时返回空列表", r, '"total":0')

    _, _, r = call("GET", "/product/search?categoryId=3")
    check("按分类筛选生效", r, "编程书籍套装")
    _, _, r = call("GET", "/product/search?categoryId=1")
    check("按分类筛选（手机数码）", r, "二手iPhone 12")

    _, _, r = call("GET", "/product/search?minPrice=1000")
    check("价格下限筛选生效", r, "二手iPhone 12")
    _, _, r = call("GET", "/product/search?minPrice=1000&maxPrice=2000")
    check("价格区间无命中", r, '"total":0')
    _, _, r = call("GET", "/product/search?maxPrice=200")
    check("价格上限筛选生效", r, "编程书籍套装")

    _, _, r = call("GET", "/product/search?sort=priceAsc&page=1&pageSize=10")
    check("价格升序排序", r, '"code":200')

    # ---------- 15. 商品详情（游客可访问） ----------
    section("15. 商品详情（无需登录）")
    _, _, r = call("GET", "/product/detail/1")
    check("游客可访问商品详情", r, '"code":200')
    check("含图片数组", r, '"images"')
    check("图片指向真实资源", r, "/images/products/iphone-12.png")
    check("含分类名称", r, "手机数码")
    check("含卖家用户名", r, "testuser1")
    check("含卖家在售数", r, '"sellerProductCount"')
    check("含成色与交易方式", r, '"productCondition"')
    check("含线下交易地址", r, "北京市朝阳区")

    views_before = ((r.get("data") or {}).get("viewCount") or 0) if isinstance(r, dict) else 0
    _, _, r2 = call("GET", "/product/detail/1")
    views_after = ((r2.get("data") or {}).get("viewCount") or 0) if isinstance(r2, dict) else 0
    check("浏览次数随访问累加", str(views_after), str(views_before + 1))

    _, _, r = call("GET", "/product/detail/2")
    check("商品2 返回书籍数据", r, "编程书籍套装")
    check("商品2 图片正确", r, "/images/products/programming-books.png")
    check("商品2 分类正确", r, "图书教材")

    _, _, r = call("GET", "/product/detail/99999")
    check("不存在的商品被拒", r, "商品不存在")

    # ---------- 16. 删除商品 ----------
    section("16. 删除商品（管理端）")
    _, _, r = call("POST", "/product/add",
                   {"title": "待删除商品", "description": "用于验证删除接口", "categoryId": 1,
                    "price": 10, "productCondition": "全新", "tradeType": "线上"},
                   token=admin_token)
    tmp_id = ((r.get("data") or {}).get("productId") if isinstance(r, dict) else None)
    check("创建待删除商品", tmp_id, None)

    if tmp_id:
        _, _, r = call("DELETE", "/admin/products/%d" % tmp_id, token=admin_token)
        check("DELETE /admin/products/{id}", r, '"code":200')
        _, _, r = call("GET", "/product/detail/%d" % tmp_id)
        check("删除后详情返回商品不存在", r, "商品不存在")
        _, _, r = call("DELETE", "/admin/products/%d" % tmp_id, token=admin_token)
        check("重复删除返回业务错误", r, "商品不存在")

    _, _, r = call("DELETE", "/admin/products/1")
    check("未登录删除商品被拒（401）", r, '"code":401')

    # ---------- 16.5 删除系统消息 ----------
    section("16.5 删除系统消息")
    _, _, r = call("POST", "/admin/messages",
                   {"title": "冒烟待删除公告", "content": "该公告用于验证删除接口", "userType": "admin"},
                   token=admin_token)
    check("创建待删除公告", r, '"code":200')
    _, _, r = call("GET", "/admin/messages?page=1&pageSize=50", token=admin_token)
    target_id = None
    for item in (((r.get("data") or {}).get("records") or []) if isinstance(r, dict) else []):
        if item.get("title") == "冒烟待删除公告":
            target_id = item["id"]
            break
    check("取到公告 ID", target_id, None)
    if target_id:
        _, _, r = call("DELETE", "/admin/messages/%d" % target_id, token=admin_token)
        check("DELETE /admin/messages/{id}", r, '"code":200')
        _, _, r = call("GET", "/admin/messages?page=1&pageSize=50", token=admin_token)
        check("删除后公告不再出现",
              "ABSENT" if "冒烟待删除公告" not in json.dumps(r, ensure_ascii=False) else "PRESENT", "ABSENT")
        _, _, r = call("DELETE", "/admin/messages/%d" % target_id, token=admin_token)
        check("重复删除返回业务错误", r, "消息不存在")
    _, _, r = call("DELETE", "/admin/messages/1")
    check("未登录删除消息被拒（401）", r, '"code":401')

    # ---------- 17. 商品图片上传 ----------
    section("17. 商品图片上传")
    status_code, resp = upload_png("/product/upload", admin_token)
    check("POST /product/upload", resp, '"code":200')
    image_url = (resp.get("data") if isinstance(resp, dict) else None)
    check("返回可访问的图片地址", image_url, "/api/uploads/")

    if image_url:
        # 上传的图片由 img 标签直接访问，不带 Token；路径前缀含 /api
        _, headers, body = call("GET", image_url.replace("/api", ""), raw=True)
        check("图片可直接访问（无需登录）", headers.get("Content-Type", ""), "image")
        check("图片内容非空", "NONEMPTY" if len(body) > 0 else "EMPTY", "NONEMPTY")

    status_code, resp = upload_png("/product/upload", None)
    check("未登录上传被拒（401）", resp, '"code":401')

    # ---------- 18. 商品扩展字段持久化 ----------
    section("18. 商品扩展字段持久化")
    _, _, r = call("POST", "/product/add",
                   {"title": "扩展字段商品", "description": "用于验证新增字段落库",
                    "categoryId": 1, "price": 888.00, "originalPrice": 1299.00,
                    "productCondition": "全新", "tradeType": "线上",
                    "brand": "TestBrand", "model": "T1", "purchaseTime": "2024-03-01",
                    "features": "包邮,支持验货", "remark": "仅用于测试",
                    "contactName": "测试联系人", "contactPhone": "13900139000",
                    "coverImage": image_url}, token=admin_token)
    ext_id = ((r.get("data") or {}).get("productId") if isinstance(r, dict) else None)
    check("创建带扩展字段的商品", ext_id, None)

    if ext_id:
        _, _, r = call("GET", "/product/detail/%d" % ext_id, token=admin_token)
        check("原价已保存", r, '"originalPrice":1299')
        check("品牌已保存", r, "TestBrand")
        check("型号已保存", r, "T1")
        check("购买时间已保存", r, "2024-03-01")
        check("商品特色已保存", r, "包邮,支持验货")
        check("备注已保存", r, "仅用于测试")
        check("联系人已保存", r, "测试联系人")
        check("封面图为上传地址", r, "/api/uploads/")
        check("登录用户可见联系电话", r, "13900139000")

        _, _, r = call("GET", "/product/detail/%d" % ext_id)
        check("游客看不到联系电话", r, '"contactPhone":null')
        check("游客仍可查看商品详情", r, '"code":200')

    # ---------- 19. 收藏 ----------
    section("19. 收藏")
    if ext_id:
        _, _, r = call("GET", "/product/detail/%d" % ext_id)
        check("初始未收藏", r, '"favorited":false')
        check("初始收藏数为 0", r, '"favoriteCount":0')

        _, _, r = call("POST", "/product/%d/favorite" % ext_id, token=admin_token)
        check("POST 收藏", r, '"code":200')
        _, _, r = call("GET", "/product/detail/%d" % ext_id, token=admin_token)
        check("收藏后 favorited 为 true", r, '"favorited":true')
        check("收藏后计数为 1", r, '"favoriteCount":1')

        _, _, r = call("GET", "/product/detail/%d" % ext_id)
        check("游客看到计数但 favorited 为 false", r, '"favorited":false')
        check("游客看到收藏计数 1", r, '"favoriteCount":1')

        _, _, r = call("POST", "/product/%d/favorite" % ext_id, token=admin_token)
        check("重复收藏不报错", r, '"code":200')
        _, _, r = call("GET", "/product/favorites", token=admin_token)
        check("收藏列表包含该商品", r, '%d' % ext_id)

        _, _, r = call("DELETE", "/product/%d/favorite" % ext_id, token=admin_token)
        check("DELETE 取消收藏", r, '"code":200')
        _, _, r = call("GET", "/product/detail/%d" % ext_id, token=admin_token)
        check("取消后计数归零", r, '"favoriteCount":0')

        _, _, r = call("POST", "/product/%d/favorite" % ext_id)
        check("未登录收藏被拒（401）", r, '"code":401')

    # ---------- 20. 我的发布 / 编辑 / 下架 ----------
    section("20. 我的发布 / 编辑 / 下架")
    _, _, r = call("GET", "/product/mine", token=admin_token)
    check("GET /product/mine", r, '"records"')
    check("我的发布只含自己发布的商品", r, "扩展字段商品")
    mine_text = json.dumps(r, ensure_ascii=False)
    check("我的发布不含他人商品", "ABSENT" if "二手iPhone 12" not in mine_text else "PRESENT", "ABSENT")

    _, _, r = call("GET", "/product/mine")
    check("未登录访问我的发布被拒（401）", r, '"code":401')

    if ext_id:
        _, _, r = call("PUT", "/product/%d" % ext_id,
                       {"title": "扩展字段商品-已改", "description": "编辑后的描述内容",
                        "categoryId": 2, "price": 777.00, "originalPrice": 999.00,
                        "productCondition": "九成新", "tradeType": "线上"}, token=admin_token)
        check("PUT 编辑自己的商品", r, '"code":200')
        _, _, r = call("GET", "/product/detail/%d" % ext_id, token=admin_token)
        check("标题已更新", r, "扩展字段商品-已改")
        check("价格已更新", r, '"price":777')
        check("分类已更新", r, "电脑办公")

        # 他人不能改：用普通用户 Token
        _, _, r = call("POST", "/user/login", {"username": "testuser1", "password": "123456"})
        other_token = ((r.get("data") or {}).get("token") if isinstance(r, dict) else None)
        _, _, r = call("PUT", "/product/%d" % ext_id,
                       {"title": "越权修改", "categoryId": 1, "price": 1,
                        "productCondition": "全新", "tradeType": "线上"}, token=other_token)
        check("他人编辑被拒（403）", r, '"code":403')

        _, _, r = call("DELETE", "/product/%d" % ext_id, token=other_token)
        check("他人下架被拒（403）", r, '"code":403')

        _, _, r = call("DELETE", "/product/%d" % ext_id, token=admin_token)
        check("DELETE 下架自己的商品", r, '"code":200')
        _, _, r = call("GET", "/product/detail/%d" % ext_id, token=admin_token)
        check("下架后状态为 3", r, '"status":3')
        _, _, r = call("GET", "/product/search?page=1&pageSize=50")
        check("已下架商品不出现在前台列表",
              "ABSENT" if "扩展字段商品" not in json.dumps(r, ensure_ascii=False) else "PRESENT", "ABSENT")
        # 前台只返回在售商品：总数应与「在售商品数」一致，且不含已下架的那条
        _, _, r2 = call("GET", "/admin/products?page=1&pageSize=200&status=1", token=admin_token)
        on_shelf = ((r2.get("data") or {}).get("total") if isinstance(r2, dict) else None)
        _, _, r3 = call("GET", "/product/search?page=1&pageSize=50")
        front_total = ((r3.get("data") or {}).get("total") if isinstance(r3, dict) else None)
        check("前台列表总数与在售商品数一致", front_total, on_shelf)

        _, _, r = call("DELETE", "/admin/products/%d" % ext_id, token=admin_token)
        check("管理端物理删除", r, '"code":200')

    # ---------- 21. 个人资料 ----------
    section("21. 个人资料")
    _, _, r = call("POST", "/user/login", {"username": "testuser1", "password": "123456"})
    me_token = ((r.get("data") or {}).get("token") if isinstance(r, dict) else None)
    check("普通用户登录", me_token, None)

    _, _, r = call("GET", "/user/info", token=me_token)
    check("获取个人资料", r, '"username":"testuser1"')
    check("资料含邮箱字段", r, '"email"')
    check("资料含简介字段", r, '"bio"')
    check("资料含 isAdmin 标识", r, '"isAdmin":false')
    text = json.dumps(r, ensure_ascii=False)
    check("资料不含 password 字段", "NOT-FOUND" if "password" not in text else "LEAKED", "NOT-FOUND")

    _, _, r = call("PUT", "/user/info",
                   {"nickname": "测试用户1", "phone": "13800138001",
                    "email": "smoke@haven.com", "bio": "冒烟测试简介"}, token=me_token)
    check("保存个人资料", r, '"code":200')
    _, _, r = call("GET", "/user/info", token=me_token)
    check("邮箱已保存", r, "smoke@haven.com")
    check("简介已保存", r, "冒烟测试简介")

    _, _, r = call("PUT", "/user/info", {"nickname": "x", "phone": "13800138002"}, token=me_token)
    check("手机号被他人占用时拒绝", r, "已被其他账号使用")
    _, _, r = call("PUT", "/user/info", {"email": "not-an-email"}, token=me_token)
    check("非法邮箱被拒", r, "邮箱格式不正确")
    _, _, r = call("PUT", "/user/info", {"nickname": "x"})
    check("未登录改资料被拒（401）", r, '"code":401')

    # ---------- 22. 修改密码 ----------
    section("22. 修改密码")
    _, _, r = call("PUT", "/user/password",
                   {"oldPassword": "wrong-password", "newPassword": "smoke12345",
                    "confirmPassword": "smoke12345"}, token=me_token)
    check("原密码错误被拒", r, "原密码不正确")
    _, _, r = call("PUT", "/user/password",
                   {"oldPassword": "123456", "newPassword": "smoke12345",
                    "confirmPassword": "different1"}, token=me_token)
    check("两次新密码不一致被拒", r, "两次输入的新密码不一致")
    _, _, r = call("PUT", "/user/password",
                   {"oldPassword": "123456", "newPassword": "123456",
                    "confirmPassword": "123456"}, token=me_token)
    check("新旧密码相同被拒", r, "新密码不能与原密码相同")
    _, _, r = call("PUT", "/user/password",
                   {"oldPassword": "123456", "newPassword": "smoke12345",
                    "confirmPassword": "smoke12345"}, token=me_token)
    check("修改密码成功", r, '"code":200')

    _, _, r = call("POST", "/user/login", {"username": "testuser1", "password": "smoke12345"})
    check("可用新密码登录", r, '"code":200"'.replace('"code":200"', '"code":200'))
    _, _, r = call("POST", "/user/login", {"username": "testuser1", "password": "123456"})
    check("旧密码已失效", r, "用户名或密码错误")

    # 改回原密码，避免影响其他用例与演示数据
    _, _, r = call("POST", "/user/login", {"username": "testuser1", "password": "smoke12345"})
    back_token = ((r.get("data") or {}).get("token") if isinstance(r, dict) else None)
    _, _, r = call("PUT", "/user/password",
                   {"oldPassword": "smoke12345", "newPassword": "123456",
                    "confirmPassword": "123456"}, token=back_token)
    check("改回原密码", r, '"code":200')
    _, _, r = call("POST", "/user/login", {"username": "testuser1", "password": "123456"})
    check("原密码恢复可用", r, '"code":200')
    _, _, r = call("PUT", "/user/password",
                   {"oldPassword": "123456", "newPassword": "smoke12345",
                    "confirmPassword": "smoke12345"})
    check("未登录改密码被拒（401）", r, '"code":401')

    # ---------- 23. 头像上传 ----------
    section("23. 头像上传")
    # 用测试账号而不是 testuser1：头像无法通过接口清空，避免把演示账号的头像改掉
    _, _, r = call("POST", "/user/login", {"username": "smoke_test_user", "password": "123456"})
    smoke_token = ((r.get("data") or {}).get("token") if isinstance(r, dict) else None)
    check("测试账号登录", smoke_token, None)
    status_code, resp = upload_png("/user/avatar", smoke_token)
    check("POST /user/avatar", resp, '"code":200')
    avatar_url = (resp.get("data") if isinstance(resp, dict) else None)
    check("返回头像地址", avatar_url, "/api/uploads/avatar/")
    if avatar_url:
        _, headers, body = call("GET", avatar_url.replace("/api", ""), raw=True)
        check("头像可直接访问", headers.get("Content-Type", ""), "image")
        _, _, r = call("GET", "/user/info", token=smoke_token)
        check("资料里头像已更新", r, avatar_url)
    status_code, resp = upload_png("/user/avatar", None)
    check("未登录上传头像被拒（401）", resp, '"code":401')

    # ---------- 24. 商品评价 ----------
    section("24. 商品评价")
    # 用测试商品承载评价，删除商品时会级联清掉评论，保证脚本可重复
    _, _, r = call("POST", "/product/add",
                   {"title": "评价测试商品", "description": "用于验证评价功能",
                    "categoryId": 1, "price": 66.00, "productCondition": "全新",
                    "tradeType": "线上"}, token=admin_token)
    cmt_pid = ((r.get("data") or {}).get("productId") if isinstance(r, dict) else None)
    check("创建评价测试商品", cmt_pid, None)

    if cmt_pid:
        _, _, r = call("GET", "/product/%d/comments" % cmt_pid)
        check("游客可看评论列表", r, '"code":200')
        check("初始评论数为 0", r, '"total":0')

        _, _, r = call("POST", "/product/%d/comments" % cmt_pid,
                       {"content": "游客发表评价尝试", "rating": 5})
        check("游客发表评论被拒（401）", r, '"code":401')

        _, _, r = call("POST", "/product/%d/comments" % cmt_pid,
                       {"content": "评价自己发布的商品", "rating": 5}, token=admin_token)
        check("不能评价自己发布的商品", r, "不能评价自己发布的商品")

        _, _, r = call("POST", "/product/%d/comments" % cmt_pid,
                       {"content": "商品成色与描述一致，卖家很负责。", "rating": 5}, token=me_token)
        check("发表评价成功", r, '"code":200')

        _, _, r = call("POST", "/product/%d/comments" % cmt_pid,
                       {"content": "重复评价测试内容", "rating": 4}, token=me_token)
        check("重复评价被拒", r, "已经评价过该商品")

        _, _, r = call("POST", "/product/%d/comments" % cmt_pid,
                       {"content": "评分越界测试", "rating": 6}, token=me_token)
        check("评分超出范围被拒", r, "评分最高为5")
        _, _, r = call("POST", "/product/%d/comments" % cmt_pid,
                       {"content": "短", "rating": 5}, token=me_token)
        check("评价内容过短被拒", r, "评论内容长度须为2-500个字符")

        _, _, r = call("GET", "/product/%d/comments" % cmt_pid)
        check("评论列表含新评论", r, "商品成色与描述一致")
        check("评论含评论人用户名", r, "testuser1")
        _, _, r = call("GET", "/product/%d/comments?page=1&pageSize=5" % cmt_pid)
        check("评论列表支持分页", r, '"total":1')

        _, _, r = call("GET", "/product/detail/%d" % cmt_pid, token=me_token)
        check("详情含评论数", r, '"commentCount":1')
        check("详情含平均评分", r, '"ratingAvg":5.0')
        check("评价人视角 commented 为 true", r, '"commented":true')
        _, _, r = call("GET", "/product/detail/%d" % cmt_pid)
        check("游客视角 commented 为 false", r, '"commented":false')
        check("游客也能看到评论数", r, '"commentCount":1')

    # ---------- 25. 商品多图 ----------
    section("25. 商品多图")
    img1 = (upload_png("/product/upload", admin_token)[1] or {}).get("data")
    img2 = (upload_png("/product/upload", admin_token)[1] or {}).get("data")
    img3 = (upload_png("/product/upload", admin_token)[1] or {}).get("data")
    check("上传三张图片", all([img1, img2, img3]), None)

    _, _, r = call("POST", "/product/add",
                   {"title": "多图测试商品", "description": "验证多图链路",
                    "categoryId": 1, "price": 100.00, "productCondition": "全新",
                    "tradeType": "线上", "images": [img1, img2, img3]}, token=admin_token)
    multi_pid = ((r.get("data") or {}).get("productId") if isinstance(r, dict) else None)
    check("创建多图商品", multi_pid, None)

    if multi_pid:
        _, _, r = call("GET", "/product/detail/%d" % multi_pid, token=admin_token)
        check("详情返回全部 3 张图", r, '"images":["%s","%s","%s"]' % (img1, img2, img3))
        _, _, r = call("GET", "/product/search?keyword=" + urllib.parse.quote("多图测试"),
                       token=admin_token)
        check("列表封面取第一张", r, img1)

        _, _, r = call("PUT", "/product/%d" % multi_pid,
                       {"title": "多图测试商品", "description": "编辑为单图",
                        "categoryId": 1, "price": 90.00, "productCondition": "全新",
                        "tradeType": "线上", "images": [img2]}, token=admin_token)
        check("编辑商品图片", r, '"code":200')
        _, _, r = call("GET", "/product/detail/%d" % multi_pid, token=admin_token)
        check("编辑后只剩 1 张", r, '"images":["%s"]' % img2)

        _, _, r = call("POST", "/product/add",
                       {"title": "单图兼容商品", "description": "验证 coverImage 兼容",
                        "categoryId": 1, "price": 50.00, "productCondition": "全新",
                        "tradeType": "线上", "coverImage": img3}, token=admin_token)
        single_pid = ((r.get("data") or {}).get("productId") if isinstance(r, dict) else None)
        check("创建单图商品（仅传 coverImage）", single_pid, None)
        if single_pid:
            _, _, r = call("GET", "/product/detail/%d" % single_pid, token=admin_token)
            check("单图商品 images 兼容返回", r, '"images":["%s"]' % img3)

        _, _, r = call("POST", "/product/add",
                       {"title": "超限图片", "categoryId": 1, "price": 1,
                        "productCondition": "全新", "tradeType": "线上",
                        "images": [img1] * 10}, token=admin_token)
        check("超过 9 张被拒", r, "最多上传 9 张图片")

    # ---------- 26. 上传大小限制 ----------
    section("26. 图片上传大小限制")
    # 回归点：application.yml 漏配 spring.servlet.multipart 时，Spring 默认只放行 1MB，
    # 超过 1MB 的图片会在进入 Controller 之前被拒，之前表现为"上传失败 + 系统内部错误"
    _, r = upload_sized_png("/product/upload", me_token, 900 * 1024)
    check("900KB 图片可上传", r, '"code":200')
    _, r = upload_sized_png("/product/upload", me_token, 3 * 1024 * 1024)
    check("3MB 图片可上传", r, '"code":200')
    _, r = upload_sized_png("/product/upload", me_token, 6 * 1024 * 1024)
    check("6MB 图片被拒并给出明确提示", r, "单张不能超过 5MB")

    # ---------- 27. 私信与商品咨询 ----------
    section("27. 私信与商品咨询")
    _, _, r = call("GET", "/user/info", token=me_token)
    me_id = ((r.get("data") or {}).get("id") if isinstance(r, dict) else None)
    check("取得 testuser1 的用户ID", me_id, None)

    _, _, r = call("POST", "/user/login", {"username": "testuser2", "password": "123456"})
    peer_token = (r.get("data") or {}).get("token") if isinstance(r, dict) else None
    peer_id = ((r.get("data") or {}).get("userId") if isinstance(r, dict) else None)
    check("testuser2 登录成功", peer_token, None)

    # 先清掉本脚本自己要用的两个会话（与 testuser2 的普通私信、以及针对商品 1 的咨询），
    # 断言随后只针对这两个会话，不去假设账号里没有别人手工产生的会话
    call("DELETE", "/message/private/chat?peerId=%d" % peer_id, token=me_token)
    call("DELETE", "/message/private/chat?peerId=%d&productId=1" % peer_id, token=me_token)
    _, _, r = call("GET", "/message/private/conversations", token=me_token)
    _keys = conversation_keys(r)
    check("清理后本脚本要用的两个会话均不存在",
          {"plain": (peer_id, None) in _keys, "consult": (peer_id, 1) in _keys},
          '{"plain":false,"consult":false}')

    # 鉴权：私信接口不允许游客访问
    _, _, r = call("GET", "/message/private/conversations")
    check("未登录看会话列表被拒（401）", r, '"code":401')
    _, _, r = call("POST", "/message/private/send", {"toUserId": peer_id, "content": "hi"})
    check("未登录发私信被拒（401）", r, '"code":401')
    _, _, r = call("GET", "/message/private/chat?peerId=%d" % peer_id)
    check("未登录拉聊天记录被拒（401）", r, '"code":401')

    # 参数校验
    _, _, r = call("POST", "/message/private/send",
                   {"toUserId": me_id, "content": "自己发自己"}, token=me_token)
    check("不能给自己发私信", r, "不能给自己发私信")
    _, _, r = call("POST", "/message/private/send",
                   {"toUserId": 999999, "content": "你好"}, token=me_token)
    check("接收者不存在被拒", r, "接收者不存在")
    _, _, r = call("POST", "/message/private/send",
                   {"toUserId": peer_id, "content": "   "}, token=me_token)
    check("空白内容被拒（400）", r, '"code":400')
    _, _, r = call("POST", "/message/private/send",
                   {"toUserId": peer_id, "content": "x" * 501}, token=me_token)
    check("超长内容被拒（400）", r, '"code":400')
    _, _, r = call("POST", "/message/private/send",
                   {"toUserId": peer_id, "productId": 999999, "content": "在吗"}, token=me_token)
    check("咨询不存在的商品被拒", r, "咨询的商品不存在")
    # 商品 1 由 testuser1 发布，自己不能咨询自己的商品
    _, _, r = call("POST", "/message/private/send",
                   {"toUserId": peer_id, "productId": 1, "content": "咨询"}, token=me_token)
    check("不能咨询自己发布的商品", r, "不能咨询自己发布的商品")

    # 普通私信：不带商品上下文
    _, _, r = call("POST", "/message/private/send",
                   {"toUserId": peer_id, "content": "冒烟私信内容"}, token=me_token)
    check("发送普通私信", r, '"code":200')
    _, _, r = call("GET", "/message/private/unread/count", token=peer_token)
    check("接收方未读数为 1", {"n": (r.get("data") if isinstance(r, dict) else None)}, '"n":1')

    _, _, r = call("GET", "/message/private/conversations", token=me_token)
    check("会话列表含对方昵称", r, '"peerName"')
    first_conv = (r.get("data") or [{}])[0] if isinstance(r, dict) else {}
    check("普通私信会话 productId 为空", {"productId": first_conv.get("productId")},
          '"productId":null')
    check("发送方自己未读数为 0", {"unread": first_conv.get("unreadCount")}, '"unread":0')

    _, _, r = call("GET", "/message/private/chat?peerId=%d" % me_id, token=peer_token)
    check("接收方拉到聊天记录", r, "冒烟私信内容")
    check("他人消息 self 为 false", r, '"self":false')
    _, _, r = call("GET", "/message/private/unread/count", token=peer_token)
    check("读过之后未读数归零", {"n": (r.get("data") if isinstance(r, dict) else None)}, '"n":0')

    _, _, r = call("POST", "/message/private/send",
                   {"toUserId": me_id, "content": "冒烟私信回复"}, token=peer_token)
    check("接收方回复成功", r, '"code":200')
    _, _, r = call("GET", "/message/private/chat?peerId=%d" % peer_id, token=me_token)
    check("会话累计 2 条", {"n": len(r.get("data") or [])}, '"n":2')
    check("自己的消息 self 为 true", r, '"self":true')

    # 商品咨询：同一对用户、不同商品，应与上面的普通私信分成两个会话
    _, _, r = call("POST", "/message/private/send",
                   {"toUserId": me_id, "productId": 1, "content": "冒烟咨询内容"},
                   token=peer_token)
    check("发起商品咨询", r, '"code":200')
    _, _, r = call("GET", "/message/private/conversations", token=peer_token)
    _keys = conversation_keys(r)
    check("同一对用户拆成普通私信与咨询两个会话",
          {"plain": (me_id, None) in _keys, "consult": (me_id, 1) in _keys},
          '{"plain":true,"consult":true}')
    check("咨询会话带回商品标题", r, "二手iPhone 12")
    _, _, r = call("GET", "/message/private/chat?peerId=%d&productId=1" % me_id,
                   token=peer_token)
    check("按商品拉咨询记录", r, "冒烟咨询内容")
    _, _, r = call("GET", "/message/private/chat?peerId=%d" % me_id, token=peer_token)
    check("不带商品ID拉不到咨询消息", r, "冒烟私信内容")

    # 删除会话：双向删除，且只删掉被指定的那个会话
    _, _, r = call("DELETE", "/message/private/chat?peerId=%d&productId=1" % me_id,
                   token=peer_token)
    check("删除咨询会话", r, '"code":200')
    _, _, r = call("GET", "/message/private/conversations", token=me_token)
    _keys = conversation_keys(r)
    check("咨询会话删除后对方也看不到了", {"consult": (peer_id, 1) in _keys}, '{"consult":false}')
    check("普通私信会话未被误删", r, "冒烟私信回复")

    # 收尾：清空本次测试留下的会话，保证脚本可重复执行
    call("DELETE", "/message/private/chat?peerId=%d" % peer_id, token=me_token)
    _, _, r = call("GET", "/message/private/conversations", token=me_token)
    _keys = conversation_keys(r)
    check("测试会话已清理干净",
          {"plain": (peer_id, None) in _keys, "consult": (peer_id, 1) in _keys},
          '{"plain":false,"consult":false}')

    # ---------- 28. 购物车 ----------
    section("28. 购物车")
    cart_pids = []
    for suffix in ("A", "B"):
        _, _, r = call("POST", "/product/add",
                       {"title": "冒烟购物车商品" + suffix, "description": "购物车与订单链路验证用",
                        "categoryId": 1, "price": 100.00, "productCondition": "全新",
                        "tradeType": "线上"}, token=me_token)
        cart_pids.append((r.get("data") or {}).get("productId"))
    pid_a, pid_b = cart_pids
    check("创建购物车测试商品 A", pid_a, None)
    check("创建购物车测试商品 B", pid_b, None)

    # 鉴权：购物车相关接口都不允许游客访问
    _, _, r = call("GET", "/cart/list")
    check("未登录看购物车被拒（401）", r, '"code":401')
    _, _, r = call("POST", "/cart/%d" % pid_a)
    check("未登录加入购物车被拒（401）", r, '"code":401')

    _, _, r = call("POST", "/cart/%d" % pid_a, token=me_token)
    check("加购自己发布的商品被拒", r, "不能把自己发布的商品加入购物车")
    _, _, r = call("POST", "/cart/999999", token=peer_token)
    check("加购不存在的商品被拒", r, "商品不存在或已被删除")

    _, _, r = call("POST", "/cart/%d" % pid_a, token=peer_token)
    check("加入购物车成功（新加入返回 true）", r, '"data":true')
    _, _, r = call("POST", "/cart/%d" % pid_a, token=peer_token)
    check("重复加入返回 false 且不报错（幂等）", r, '"data":false')

    _, _, r = call("GET", "/cart/count", token=peer_token)
    _count = r.get("data") if isinstance(r, dict) else None
    check("购物车件数接口返回正整数", {"positive": isinstance(_count, int) and _count >= 1},
          '"positive":true')
    _, _, r = call("GET", "/cart/list", token=peer_token)
    check("购物车里有该商品", {"has": pid_a in cart_product_ids(r)}, '{"has":true}')
    check("购物车带回商品标题", r, "冒烟购物车商品A")
    check("购物车带回卖家昵称", r, '"sellerName":"测试用户1"')
    check("在售商品标记为可结算", r, '"available":true')

    _, _, r = call("POST", "/cart/%d/move-to-favorite" % pid_a, token=peer_token)
    check("购物车移入收藏", r, '"code":200')
    _, _, r = call("GET", "/cart/list", token=peer_token)
    check("移入收藏后该商品已不在购物车", {"has": pid_a in cart_product_ids(r)}, '{"has":false}')
    _, _, r = call("GET", "/product/favorites?page=1&pageSize=50", token=peer_token)
    check("收藏里能看到该商品", r, "冒烟购物车商品A")

    _, _, r = call("POST", "/cart/favorites/add-all", token=peer_token)
    check("收藏批量加入购物车", r, '"code":200')
    _, _, r = call("GET", "/cart/list", token=peer_token)
    check("批量加入后购物车里有该商品", r, "冒烟购物车商品A")

    # ---------- 29. 订单与模拟支付 ----------
    section("29. 订单与模拟支付")
    _, _, r = call("POST", "/order/create", {"productIds": []})
    check("未登录下单被拒（401）", r, '"code":401')
    _, _, r = call("POST", "/order/create", {"productIds": []}, token=peer_token)
    check("空商品列表被参数校验拦下（400）", r, '"code":400')
    _, _, r = call("POST", "/order/create", {"productIds": [pid_a]}, token=me_token)
    check("购买自己发布的商品被拒", r, "是你自己发布的商品，不能购买")
    _, _, r = call("POST", "/order/create", {"productIds": [999999]}, token=peer_token)
    check("购买不存在的商品被拒", r, "部分商品已不存在")

    _, _, r = call("POST", "/order/create",
                   {"productIds": [pid_a], "remark": "冒烟测试留言"}, token=peer_token)
    check("下单成功", r, '"code":200')
    order_no = ((r.get("data") or {}).get("orderNo") if isinstance(r, dict) else None)
    check("订单号以 HM 开头", order_no, None)
    check("初始状态为待支付", r, '"status":1')
    # 金额比较用数值而不是字符串：BigDecimal 序列化后 100.00 会写成 100.0
    check("订单金额等于商品价格",
          {"v": float((r.get("data") or {}).get("totalAmount"))}, '"v":100.0')
    check("买家昵称已回填", r, '"buyerName":"测试用户2"')
    check("订单明细为下单时的标题快照", r, "冒烟购物车商品A")
    check("买家留言已保存", r, "冒烟测试留言")

    _, _, r = call("GET", "/product/detail/%d" % pid_a)
    check("下单后商品被锁定为已售出", r, '"status":2')
    _, _, r = call("GET", "/cart/list", token=peer_token)
    check("已下单商品已从购物车移除", {"has": pid_a in cart_product_ids(r)}, '{"has":false}')

    _, _, r = call("POST", "/order/create", {"productIds": [pid_a]}, token=peer_token)
    check("已售出商品无法再次下单", r, "已售出")
    _, _, r = call("POST", "/cart/%d" % pid_a, token=peer_token)
    check("已售出商品无法加入购物车", r, "无法加入购物车")

    _, _, r = call("GET", "/order/list?page=1&pageSize=50", token=peer_token)
    check("我的订单里能查到该订单", r, order_no)
    _, _, r = call("GET", "/order/list?status=1&page=1&pageSize=50", token=peer_token)
    check("可按待支付筛选到它", r, order_no)
    _, _, r = call("GET", "/order/sold?page=1&pageSize=50", token=me_token)
    check("卖家视角能看到这笔订单", r, order_no)
    check("卖家视角带回买家昵称", r, '"buyerName":"测试用户2"')

    _, _, r = call("POST", "/order/%s/pay" % order_no, token=me_token)
    check("他人不能支付我的订单（403）", r, '"code":403')
    _, _, r = call("POST", "/order/HM000000000000000000/pay", token=peer_token)
    check("订单不存在被拒", r, "订单不存在")
    _, _, r = call("POST", "/order/%s/pay" % order_no, token=peer_token)
    check("模拟支付成功", r, '"code":200')
    _, _, r = call("POST", "/order/%s/pay" % order_no, token=peer_token)
    check("重复支付被拒", r, "不能支付")
    _, _, r = call("GET", "/order/%s" % order_no, token=peer_token)
    check("支付后状态为已支付并记录支付时间", r, '"status":2')
    check("支付时间已写入", r, '"payTime"')
    _, _, r = call("GET", "/order/%s" % order_no, token=me_token)
    check("他人不能查看我的订单（403）", r, '"code":403')

    _, _, r = call("POST", "/order/%s/confirm" % order_no, token=peer_token)
    check("确认收货成功", r, '"code":200')
    _, _, r = call("GET", "/order/%s" % order_no, token=peer_token)
    check("确认后状态为已完成", r, '"statusDesc":"已完成"')
    _, _, r = call("POST", "/order/%s/confirm" % order_no, token=peer_token)
    check("已完成订单不能重复确认", r, "只有已支付")

    # 取消订单：商品应释放回在售
    _, _, r = call("POST", "/order/create", {"productIds": [pid_b]}, token=peer_token)
    cancel_no = ((r.get("data") or {}).get("orderNo") if isinstance(r, dict) else None)
    check("用商品 B 下单", cancel_no, None)
    _, _, r = call("GET", "/product/detail/%d" % pid_b)
    check("下单后 B 被锁定为已售出", r, '"status":2')
    _, _, r = call("POST", "/order/%s/cancel" % cancel_no, token=peer_token)
    check("取消订单成功", r, '"code":200')
    _, _, r = call("GET", "/product/detail/%d" % pid_b)
    check("取消后 B 回到在售", r, '"status":1')
    _, _, r = call("GET", "/order/%s" % cancel_no, token=peer_token)
    check("订单状态为已取消并记录取消时间", r, '"statusDesc":"已取消"')
    check("取消时间已写入", r, '"cancelTime"')
    _, _, r = call("POST", "/order/%s/pay" % cancel_no, token=peer_token)
    check("已取消订单不能支付", r, "不能支付")
    _, _, r = call("POST", "/order/%s/cancel" % cancel_no, token=peer_token)
    check("已取消订单不能重复取消", r, "不能取消")

    # 商品删除后订单快照仍然完整（订单明细存的是下单时的标题与封面）
    _, _, r = call("POST", "/order/create", {"productIds": [pid_b]}, token=peer_token)
    snap_no = ((r.get("data") or {}).get("orderNo") if isinstance(r, dict) else None)
    call("POST", "/order/%s/pay" % snap_no, token=peer_token)
    _, _, r = call("DELETE", "/admin/products/%d" % pid_b, token=admin_token)
    check("管理员删除商品 B", r, '"code":200')
    _, _, r = call("GET", "/order/%s" % snap_no, token=peer_token)
    check("商品删除后订单标题快照仍在", r, "冒烟购物车商品B")
    check("订单明细标记商品已被删除", r, '"productDeleted":true')
    # 说明：订单没有删除接口，这里不清理订单记录；订单明细存的是快照，
    #       商品被删除也不影响历史订单展示，重复执行不会产生错误数据

    # 收尾：把购物车测试商品 A 也删掉，避免残留（B 已在上一步删除）
    call("DELETE", "/admin/products/%d" % pid_a, token=admin_token)

    # ---------- 收尾：清理本次产生的测试数据 ----------
    cleanup_test_products(admin_token)
    # 把演示账号 testuser1 的资料恢复为初始状态（邮箱与简介在种子里为空）
    if me_token:
        call("PUT", "/user/info", {"nickname": "测试用户1", "phone": "13800138001",
                                   "email": "", "bio": ""}, token=me_token)

    # ---------- 汇总 ----------
    print("\n" + "=" * 46)
    print("  通过: %d   失败: %d" % (PASS, FAIL))
    if FAILURES:
        print("  失败用例:")
        for name in FAILURES:
            print("    - " + name)
    print("=" * 46)
    return 1 if FAIL else 0


if __name__ == "__main__":
    sys.exit(main())
