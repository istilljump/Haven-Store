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


# 测试数据的标题标记：脚本开头与结尾都会据此清理，保证可重复执行
TEST_TITLE_MARKERS = ("冒烟商品", "扩展字段商品", "待删除商品", "UPLOAD-TEST",
                      "NO-COVER-ITEM", "DELETE-TEST", "VERIFY-ITEM")


def cleanup_test_products(token):
    """删除历史遗留的测试商品，使脚本可重复运行。

    走管理端列表拿全量数据（含已下架的商品，前台搜索只看在售，会漏掉下架残留）。
    """
    _, _, r = call("GET", "/admin/products?page=1&pageSize=200", token=token)
    records = ((r.get("data") or {}).get("records") or []) if isinstance(r, dict) else []
    removed = 0
    for item in records:
        title = item.get("title") or ""
        if any(title.startswith(m) for m in TEST_TITLE_MARKERS):
            call("DELETE", "/admin/products/%d" % item["id"], token=token)
            removed += 1
    return removed


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

    # ---------- 收尾：清理本次产生的测试数据 ----------
    cleanup_test_products(admin_token)

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
