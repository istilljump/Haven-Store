#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据飞轮一键演示（Haven-Store 闲置港湾）
=================================

演示脚本：走**真实业务接口**完成「发布 → 搜索 → 点击 → 下单 → 支付」一笔成交，
打印前后对比，直观展示成交行情的自动刷新与「点击 → 成交」归因：

  before：GET /market/summary（运动户外·八成新）+ /market/campus-summary（本校行情速览）
  动作  ：testuser1 发布商品 → testuser2 增强搜索（写埋点）→ 点击（信标）
          → 下单 → 模拟支付（支付成功后自动回写 deal_flag、行情样本 +1）
  after ：再查一次行情与埋点归因，打印对比

用法：
    python scripts/demo_flywheel.py            # 默认 http://127.0.0.1:8080
    python scripts/demo_flywheel.py --port 8080

安全约束（白名单 + 双重校验）：
  - 仅允许 http/https 协议
  - 主机名仅允许 localhost / 127.0.0.1 / ::1（字面白名单，且解析后 IP 必须全部为环回地址）
  - 禁止跟随重定向（防跳出白名单边界）
无第三方依赖（纯标准库）；重复执行安全（商品带随机后缀，结束后自动清理）。
"""

import argparse
import ipaddress
import json
import socket
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

# Windows 控制台默认 GBK，强制 UTF-8 输出避免 ¥ 等字符编码报错
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

# 主机白名单：演示脚本只打本机后端
ALLOWED_HOSTS = {"localhost", "127.0.0.1", "::1"}
ALLOWED_SCHEMES = {"http", "https"}

BASE = "http://127.0.0.1:8080/api"


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    """禁止重定向：跳转一律拒绝，杜绝跳出白名单边界"""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def validate_base(url):
    """协议 + 主机名 + 解析 IP 三重校验，任何一项不过即拒绝启动"""
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme not in ALLOWED_SCHEMES:
        raise SystemExit("仅允许 http/https 协议，收到：%s" % parsed.scheme)
    if parsed.hostname not in ALLOWED_HOSTS:
        raise SystemExit("主机名不在白名单（仅允许 localhost/127.0.0.1/::1）：%s" % parsed.hostname)
    try:
        infos = socket.getaddrinfo(parsed.hostname, parsed.port or 80, proto=socket.IPPROTO_TCP)
    except OSError as e:
        raise SystemExit("主机名解析失败：%s" % e)
    for info in infos:
        ip = ipaddress.ip_address(info[4][0])
        if not ip.is_loopback:
            raise SystemExit("主机名解析到非环回地址，拒绝请求：%s" % ip)
    return parsed


def call(method, path, body=None, token=None):
    """极简 HTTP 客户端：返回 (payload, http_status)"""
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    opener = urllib.request.build_opener(_NoRedirect)
    try:
        with opener.open(req, timeout=10) as resp:
            return json.loads(resp.read().decode("utf-8")), resp.status
    except urllib.error.HTTPError as e:
        try:
            return json.loads(e.read().decode("utf-8")), e.code
        except Exception:
            return {}, e.code
    except urllib.error.URLError as e:
        raise SystemExit("后端不可达（%s）：请先启动后端 jar" % e.reason)


def login(username, password):
    payload, _ = call("POST", "/user/login", {"username": username, "password": password})
    token = (payload.get("data") or {}).get("token")
    if not token:
        raise SystemExit("登录失败：%s（请确认后端已启动且种子账号存在）" % username)
    return token


def summary(category_id, condition):
    q = urllib.parse.urlencode({"categoryId": category_id, "condition": condition})
    payload, _ = call("GET", "/market/summary?" + q)
    return payload.get("data") or {}


def campus_board(campus_id):
    payload, _ = call("GET", "/market/campus-summary?" + urllib.parse.urlencode({"campusId": campus_id, "topN": 3}))
    return payload.get("data") or []


def print_board(tag, board):
    rows = ", ".join("%s(成交%d件/中位¥%s)" % (s.get("categoryName"), s.get("dealCount"), s.get("medianPrice"))
                     for s in board) or "（暂无成交行情）"
    print("  [%s] 本校行情速览 TOP3：%s" % (tag, rows))


def main():
    global BASE
    parser = argparse.ArgumentParser(description="数据飞轮演示脚本")
    parser.add_argument("--port", type=int, default=8080, help="后端端口（默认 8080）")
    args = parser.parse_args()
    BASE = "http://127.0.0.1:%d/api" % args.port
    validate_base(BASE)

    suffix = uuid.uuid4().hex[:6]
    print("=" * 60)
    print("Haven-Store 数据飞轮演示（走真实接口，不直写数据库）")
    print("=" * 60)

    seller = login("testuser1", "123456")
    buyer = login("testuser2", "123456")

    # ---------- before ----------
    print("\n[1] before：当前成交行情（运动户外·八成新）与本校行情速览")
    before = summary(6, "八成新")
    print("  [before] 运动户外·八成新：成交 %s 件，中位价 ¥%s（区间 ¥%s ~ ¥%s）" % (
        before.get("sampleSize"), before.get("medianPrice"), before.get("minPrice"), before.get("maxPrice")))
    print_board("before", campus_board(1))

    # ---------- 动作：发布 → 搜索 → 点击 → 下单 → 支付 ----------
    print("\n[2] 动作：发布「瑜伽垫」→ testuser2 搜索并点击 → 下单 → 模拟支付")
    payload, _ = call("POST", "/product/add", {
        "title": "飞轮演示瑜伽垫 " + suffix, "categoryId": 6, "price": 50.00,
        "productCondition": "八成新", "tradeType": "线下", "campusId": 1,
        "description": "数据飞轮演示：这笔成交会立即进入行情统计",
        "address": "江畔大学主校区体育馆", "longitude": 114.3548, "latitude": 30.5415,
        "images": [],
    }, token=seller)
    product_id = (payload.get("data") or {}).get("productId")
    if not product_id:
        raise SystemExit("商品发布失败：%s" % payload)
    print("  发布成功：商品 ID=%s（主校区·运动户外·八成新·¥50）" % product_id)

    payload, _ = call("GET", "/search/products?" + urllib.parse.urlencode(
        {"keyword": "瑜伽垫", "campusId": 1, "page": 1, "pageSize": 12}))
    log_id = (payload.get("data") or {}).get("logId")
    print("  搜索「瑜伽垫」成功：埋点 logId=%s（已写入搜索埋点）" % log_id)
    if log_id:
        call("POST", "/search/log/click", {"logId": log_id, "productId": product_id})
        print("  已点击该结果：点击信标回传成功")

    payload, _ = call("POST", "/order/create", {"productIds": [product_id]}, token=buyer)
    order_no = (payload.get("data") or {}).get("orderNo")
    if not order_no:
        raise SystemExit("下单失败：%s" % payload)
    print("  下单成功：订单号 %s（商品已锁定为已售出）" % order_no)

    payload, _ = call("POST", "/order/%s/pay" % order_no, token=buyer)
    if not payload.get("success"):
        raise SystemExit("支付失败：%s" % payload)
    print("  模拟支付成功：行情统计与成交归因即时刷新")

    # 给行情聚合一个短暂停顿（同一事务已提交，此处仅为人眼可读的节奏）
    time.sleep(0.5)

    # ---------- after ----------
    print("\n[3] after：成交已回流行情")
    after = summary(6, "八成新")
    grown = (after.get("sampleSize") or 0) - (before.get("sampleSize") or 0)
    print("  [after] 运动户外·八成新：成交 %s 件，中位价 ¥%s（样本 %s → %s，%s）" % (
        after.get("sampleSize"), after.get("medianPrice"),
        before.get("sampleSize"), after.get("sampleSize"),
        "+1 ✓ 数据飞轮生效" if grown == 1 else "变化 %d" % grown))
    print_board("after", campus_board(1))

    payload, _ = call("GET", "/search/log/list?" + urllib.parse.urlencode({"keyword": "瑜伽垫", "limit": 3}))
    logs = payload.get("data") or []
    hit = next((l for l in logs if l.get("dealFlag") == 1), None)
    print("  [after] 搜索埋点归因：%s" % (
        "「搜索 → 点击 → 成交」链路打通，deal_flag=1 ✓" if hit else "未观察到归因记录"))

    # ---------- 收尾 ----------
    admin = login("admin", "admin123")
    call("DELETE", "/admin/products/%s" % product_id, token=admin)
    print("\n[4] 演示商品已清理（订单快照保留）；行情样本回落，脚本可重复执行")
    print("=" * 60)
    print("结论：每一笔成交都在让估价更准 —— 这就是 Haven-Store 的数据飞轮。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
