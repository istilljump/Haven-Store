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


def section(title):
    print("\n===== " + title + " =====")


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

    new_name = "smoke_%d" % int(time.time())
    _, _, r = call("POST", "/admin/users",
                   {"username": new_name, "password": "123456",
                    "nickname": "冒烟测试用户", "phone": "", "isAdmin": False},
                   token=admin_token)
    check("POST /admin/users 新增用户", r, '"code":200')

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
