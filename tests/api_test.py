#!/usr/bin/env python3
"""
二手商品交易市场 API 测试脚本
用于测试前后端接口联调
"""

import requests
import json
import sys
import time
from datetime import datetime

# API 基础配置
BASE_URL = "http://localhost:8080/api"
FRONTEND_URL = "http://localhost:3000"

class APITester:
    def __init__(self):
        self.session = requests.Session()
        self.token = None
        self.user_id = None
        
    def test_health(self):
        """测试后端服务健康状态"""
        print("=" * 50)
        print("测试后端服务健康状态")
        print("=" * 50)
        
        try:
            response = self.session.get(f"{BASE_URL}/health", timeout=5)
            if response.status_code == 200:
                print("✅ 后端服务运行正常")
                return True
            else:
                print(f"❌ 后端服务异常: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ 后端服务连接失败: {e}")
            print("请确保后端服务已启动在 8080 端口")
            return False
    
    def test_frontend_access(self):
        """测试前端页面访问"""
        print("\n" + "=" * 50)
        print("测试前端页面访问")
        print("=" * 50)
        
        try:
            response = self.session.get(FRONTEND_URL, timeout=5)
            if response.status_code == 200 and "二手商品交易市场" in response.text:
                print("✅ 前端页面访问正常")
                return True
            else:
                print(f"❌ 前端页面异常: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ 前端服务连接失败: {e}")
            print("请确保前端服务已启动在 3000 端口")
            return False
    
    def test_register(self, username="testuser", password="123456", phone="13800138000", nickname="测试用户"):
        """测试用户注册"""
        print("\n" + "=" * 50)
        print("测试用户注册")
        print("=" * 50)
        
        data = {
            "username": username,
            "password": password,
            "confirmPassword": password,
            "phone": phone,
            "nickname": nickname
        }
        
        try:
            response = self.session.post(f"{BASE_URL}/user/register", json=data, timeout=10)
            
            if response.status_code == 200:
                result = response.json()
                if result.get("code") == 200:
                    print(f"✅ 用户注册成功: {username}")
                    return True
                elif result.get("code") == 400 and "已存在" in result.get("message", ""):
                    print(f"ℹ️ 用户 {username} 已存在，跳过注册")
                    return True
                else:
                    print(f"❌ 用户注册失败: {result.get('message', '未知错误')}")
                    return False
            else:
                print(f"❌ 注册请求异常: {response.status_code}")
                print(f"响应内容: {response.text}")
                return False
                
        except Exception as e:
            print(f"❌ 注册请求失败: {e}")
            return False
    
    def test_login(self, username="testuser", password="123456"):
        """测试用户登录"""
        print("\n" + "=" * 50)
        print("测试用户登录")
        print("=" * 50)
        
        data = {
            "username": username,
            "password": password
        }
        
        try:
            response = self.session.post(f"{BASE_URL}/user/login", json=data, timeout=10)
            
            if response.status_code == 200:
                result = response.json()
                if result.get("code") == 200:
                    self.token = result.get("data", {}).get("token")
                    self.user_id = result.get("data", {}).get("userId")
                    print(f"✅ 用户登录成功: {username}")
                    print(f"📝 Token: {self.token[:20]}...")
                    print(f"🆔 用户ID: {self.user_id}")
                    return True
                else:
                    print(f"❌ 用户登录失败: {result.get('message', '未知错误')}")
                    return False
            else:
                print(f"❌ 登录请求异常: {response.status_code}")
                print(f"响应内容: {response.text}")
                return False
                
        except Exception as e:
            print(f"❌ 登录请求失败: {e}")
            return False
    
    def test_user_info(self):
        """测试获取用户信息"""
        print("\n" + "=" * 50)
        print("测试获取用户信息")
        print("=" * 50)
        
        if not self.token:
            print("❌ 未登录，无法获取用户信息")
            return False
            
        headers = {"Authorization": f"Bearer {self.token}"}
        
        try:
            response = self.session.get(f"{BASE_URL}/user/info", headers=headers, timeout=10)
            
            if response.status_code == 200:
                result = response.json()
                if result.get("code") == 200:
                    user_data = result.get("data", {})
                    print(f"✅ 获取用户信息成功")
                    print(f"👤 用户名: {user_data.get('username')}")
                    print(f"📱 昵称: {user_data.get('nickname')}")
                    return True
                else:
                    print(f"❌ 获取用户信息失败: {result.get('message', '未知错误')}")
                    return False
            else:
                print(f"❌ 用户信息请求异常: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"❌ 获取用户信息失败: {e}")
            return False
    
    def test_publish_product(self):
        """测试发布商品"""
        print("\n" + "=" * 50)
        print("测试发布商品")
        print("=" * 50)
        
        if not self.token:
            print("❌ 未登录，无法发布商品")
            return False
            
        headers = {"Authorization": f"Bearer {self.token}"}
        
        data = {
            "title": "测试商品 - iPhone 13",
            "categoryId": 1,
            "description": "95新 iPhone 13 白色 128G，自用一年，无拆修",
            "price": 2999.00,
            "productCondition": "九成新",
            "tradeType": "offline",
            "address": "北京市海淀区中关村大街1号",
            "longitude": 116.316833,
            "latitude": 39.981013
        }
        
        try:
            response = self.session.post(f"{BASE_URL}/product/add", json=data, headers=headers, timeout=10)
            
            if response.status_code == 200:
                result = response.json()
                if result.get("code") == 200:
                    product_data = result.get("data", {})
                    product_id = product_data.get("productId")
                    print(f"✅ 发布商品成功")
                    print(f"🆔 商品ID: {product_id}")
                    return True
                else:
                    print(f"❌ 发布商品失败: {result.get('message', '未知错误')}")
                    return False
            else:
                print(f"❌ 发布商品请求异常: {response.status_code}")
                print(f"响应内容: {response.text}")
                return False
                
        except Exception as e:
            print(f"❌ 发布商品失败: {e}")
            return False
    
    def test_nearby_products(self):
        """测试附近商品查询"""
        print("\n" + "=" * 50)
        print("测试附近商品查询")
        print("=" * 50)
        
        if not self.token:
            print("❌ 未登录，无法查询商品")
            return False
            
        headers = {"Authorization": f"Bearer {self.token}"}
        
        data = {
            "longitude": 116.316833,
            "latitude": 39.981013,
            "radius": 5000,  # 5公里
            "page": 1,
            "pageSize": 10
        }
        
        try:
            response = self.session.post(f"{BASE_URL}/product/nearby", json=data, headers=headers, timeout=10)
            
            if response.status_code == 200:
                result = response.json()
                if result.get("code") == 200:
                    page_data = result.get("data", {})
                    records = page_data.get("records", [])
                    total = page_data.get("total", 0)
                    print(f"✅ 查询附近商品成功")
                    print(f"📊 总商品数: {total}")
                    print(f"📝 返回记录数: {len(records)}")
                    
                    if records:
                        print(f"🔍 第一条商品:")
                        print(f"  标题: {records[0].get('title')}")
                        print(f"  价格: ¥{records[0].get('price')}")
                        print(f"  距离: {records[0].get('distance')}米")
                    
                    return True
                else:
                    print(f"❌ 查询附近商品失败: {result.get('message', '未知错误')}")
                    return False
            else:
                print(f"❌ 查询附近商品异常: {response.status_code}")
                print(f"响应内容: {response.text}")
                return False
                
        except Exception as e:
            print(f"❌ 查询附近商品失败: {e}")
            return False
    
    def test_ai_estimate(self):
        """测试AI估价"""
        print("\n" + "=" * 50)
        print("测试AI智能估价")
        print("=" * 50)
        
        if not self.token:
            print("❌ 未登录，无法使用AI估价")
            return False
            
        headers = {"Authorization": f"Bearer {self.token}"}
        
        data = {
            "title": "iPhone 13 白色 128G",
            "description": "95新，无拆修，自用一年",
            "productCondition": "九成新",
            "categoryId": 1
        }
        
        try:
            response = self.session.post(f"{BASE_URL}/product/estimate", json=data, headers=headers, timeout=10)
            
            if response.status_code == 200:
                result = response.json()
                if result.get("code") == 200:
                    estimate_data = result.get("data", {})
                    price = estimate_data.get("estimatedPrice")
                    confidence = estimate_data.get("confidence")
                    print(f"✅ AI估价成功")
                    print(f"💰 建议价格: ¥{price}")
                    print(f"📊 置信度: {confidence}%")
                    return True
                else:
                    print(f"❌ AI估价失败: {result.get('message', '未知错误')}")
                    return False
            else:
                print(f"❌ AI估价异常: {response.status_code}")
                print(f"响应内容: {response.text}")
                return False
                
        except Exception as e:
            print(f"❌ AI估价失败: {e}")
            return False
    
    def run_all_tests(self):
        """运行所有测试"""
        print("🚀 开始二手商品交易市场 API 测试")
        print(f"📅 测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        
        # 测试结果
        results = []
        
        # 1. 测试后端服务
        results.append(("后端服务", self.test_health()))
        
        # 2. 测试前端服务
        results.append(("前端服务", self.test_frontend_access()))
        
        # 3. 测试用户注册
        results.append(("用户注册", self.test_register()))
        
        # 4. 测试用户登录
        results.append(("用户登录", self.test_login()))
        
        # 5. 测试获取用户信息
        results.append(("获取用户信息", self.test_user_info()))
        
        # 6. 测试发布商品
        results.append(("发布商品", self.test_publish_product()))
        
        # 7. 测试附近商品查询
        results.append(("附近商品查询", self.test_nearby_products()))
        
        # 8. 测试AI估价
        results.append(("AI智能估价", self.test_ai_estimate()))
        
        # 测试结果汇总
        print("\n" + "=" * 70)
        print("📊 测试结果汇总")
        print("=" * 70)
        
        passed = 0
        failed = 0
        
        for test_name, success in results:
            status = "✅ 通过" if success else "❌ 失败"
            print(f"{test_name}: {status}")
            if success:
                passed += 1
            else:
                failed += 1
        
        print(f"\n📈 总计: {passed + failed} 项测试")
        print(f"✅ 通过: {passed} 项")
        print(f"❌ 失败: {failed} 项")
        
        if failed == 0:
            print("\n🎉 所有测试通过！前后端联调成功！")
            return True
        else:
            print(f"\n⚠️ {failed} 项测试失败，请检查配置和依赖服务")
            return False

def main():
    """主函数"""
    tester = APITester()
    success = tester.run_all_tests()
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()