#!/usr/bin/env python3
"""
Mock API Server for Frontend Testing
模拟后端API服务，用于前端功能测试和开发
"""

from flask import Flask, request, jsonify, Response
import json
import time
import uuid
from datetime import datetime, timedelta
import bcrypt
import random

app = Flask(__name__)

# 模拟数据库数据
mock_db = {
    'users': [],
    'products': [],
    'categories': [
        {'id': 1, 'name': '手机数码', 'sort': 1},
        {'id': 2, 'name': '电脑办公', 'sort': 2},
        {'id': 3, 'name': '图书教材', 'sort': 3},
        {'id': 4, 'name': '家用电器', 'sort': 4},
        {'id': 5, 'name': '服饰鞋包', 'sort': 5},
        {'id': 6, 'name': '运动户外', 'sort': 6},
        {'id': 7, 'name': '美妆个护', 'sort': 7},
        {'id': 8, 'name': '其他闲置', 'sort': 8}
    ],
    'next_user_id': 1,
    'next_product_id': 1
}

# 模拟JWT token生成
def generate_token(user_id, username):
    # 实际项目中应该使用JWT库
    return f"mock_token_{user_id}_{int(time.time())}"

# 密码加密
def hash_password(password):
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')

# 密码验证
def verify_password(plain_password, hashed_password):
    return bcrypt.checkpw(plain_password.encode('utf-8'), hashed_password.encode('utf-8'))

# 统一响应格式
def success_response(data=None, message="成功"):
    return jsonify({
        'code': 200,
        'msg': message,
        'data': data
    })

def error_response(code, message):
    return jsonify({
        'code': code,
        'msg': message,
        'data': None
    })

# 健康检查接口
@app.route('/api/health', methods=['GET'])
def health_check():
    return success_response({
        'status': 'ok',
        'timestamp': datetime.now().isoformat(),
        'message': 'Mock API服务运行正常'
    })

# 用户注册接口
@app.route('/api/user/register', methods=['POST'])
def register():
    data = request.get_json()
    
    # 验证参数
    if not data or not all(k in data for k in ['username', 'password', 'confirmPassword', 'phone', 'nickname']):
        return error_response(400, '参数不完整')
    
    username = data['username']
    password = data['password']
    phone = data['phone']
    nickname = data['nickname']
    
    # 检查用户名是否已存在
    for user in mock_db['users']:
        if user['username'] == username:
            return error_response(400, '用户名已存在')
    
    # 检查手机号是否已存在
    for user in mock_db['users']:
        if user['phone'] == phone:
            return error_response(400, '手机号已存在')
    
    # 创建用户
    user_id = mock_db['next_user_id']
    hashed_password = hash_password(password)
    
    new_user = {
        'id': user_id,
        'username': username,
        'password': hashed_password,
        'phone': phone,
        'nickname': nickname,
        'avatar': None,
        'create_time': datetime.now().isoformat(),
        'update_time': datetime.now().isoformat(),
        'status': 1
    }
    
    mock_db['users'].append(new_user)
    mock_db['next_user_id'] += 1
    
    return success_response(None, '注册成功')

# 用户登录接口
@app.route('/api/user/login', methods=['POST'])
def login():
    data = request.get_json()
    
    if not data or not all(k in data for k in ['username', 'password']):
        return error_response(400, '参数不完整')
    
    username = data['username']
    password = data['password']
    
    # 查找用户
    user = None
    for u in mock_db['users']:
        if u['username'] == username:
            user = u
            break
    
    if not user:
        return error_response(401, '用户名或密码错误')
    
    # 验证密码
    if not verify_password(password, user['password']):
        return error_response(401, '用户名或密码错误')
    
    # 生成token
    token = generate_token(user['id'], user['username'])
    
    # 返回用户信息（脱敏）
    user_info = {
        'userId': user['id'],
        'username': user['username'],
        'nickname': user['nickname'],
        'phone': user['phone'],
        'avatar': user['avatar'],
        'createTime': user['create_time']
    }
    
    return success_response({
        'token': token,
        'userInfo': user_info
    }, '登录成功')

# 获取当前用户信息接口
@app.route('/api/user/info', methods=['GET'])
def get_user_info():
    # 从Authorization头获取token
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    token = auth_header[7:]  # 去掉 'Bearer ' 前缀
    user_id = token.split('_')[1]  # 从mock token中提取用户ID
    
    # 查找用户
    user = None
    for u in mock_db['users']:
        if str(u['id']) == user_id:
            user = u
            break
    
    if not user:
        return error_response(404, '用户不存在')
    
    # 脱敏返回用户信息
    user_info = {
        'id': user['id'],
        'username': user['username'],
        'nickname': user['nickname'],
        'phone': user['phone'],
        'avatar': user['avatar'],
        'createTime': user['create_time'],
        'updateTime': user['update_time'],
        'status': user['status']
    }
    
    return success_response(user_info)

# 发布商品接口
@app.route('/api/product/add', methods=['POST'])
def add_product():
    # 验证登录
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    token = auth_header[7:]
    user_id = token.split('_')[1]
    
    data = request.get_json()
    
    # 验证参数
    if not data or not all(k in data for k in ['title', 'categoryId', 'price', 'productCondition', 'tradeType']):
        return error_response(400, '参数不完整')
    
    title = data['title']
    category_id = data['categoryId']
    price = float(data['price'])
    condition = data['productCondition']
    trade_type = data['tradeType']
    
    # 验证分类是否存在
    category_exists = any(c['id'] == category_id for c in mock_db['categories'])
    if not category_exists:
        return error_response(400, '商品分类不存在')
    
    # 验证价格
    if price <= 0:
        return error_response(400, '商品价格必须大于0')
    
    # 验证交易方式和地址
    if trade_type == 'offline':
        if not data.get('address'):
            return error_response(400, '线下交易必须填写地址')
        if not data.get('longitude') or not data.get('latitude'):
            return error_response(400, '线下交易必须填写经纬度')
    
    # 创建商品
    product_id = mock_db['next_product_id']
    
    new_product = {
        'id': product_id,
        'userId': int(user_id),
        'title': title,
        'description': data.get('description', ''),
        'coverImage': data.get('coverImage'),
        'categoryId': category_id,
        'price': price,
        'productCondition': condition,
        'tradeType': trade_type,
        'address': data.get('address'),
        'longitude': data.get('longitude'),
        'latitude': data.get('latitude'),
        'createTime': datetime.now().isoformat(),
        'updateTime': datetime.now().isoformat(),
        'status': 1
    }
    
    mock_db['products'].append(new_product)
    mock_db['next_product_id'] += 1
    
    return success_response({
        'productId': product_id,
        'estimatedPrice': None  # 模拟接口不提供估价
    }, '发布成功')

# 查询附近商品接口
@app.route('/api/product/nearby', methods=['POST'])
def find_nearby_products():
    # 验证登录
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    data = request.get_json()
    
    if not data or not all(k in data for k in ['longitude', 'latitude', 'radius', 'page', 'pageSize']):
        return error_response(400, '参数不完整')
    
    longitude = float(data['longitude'])
    latitude = float(data['latitude'])
    radius = float(data['radius'])
    page = int(data['page'])
    page_size = int(data['pageSize'])
    
    # 模拟附近商品查询（返回所有商品）
    all_products = []
    for product in mock_db['products']:
        if product['longitude'] and product['latitude']:
            # 简单距离计算（实际应该使用Haversine公式）
            distance = random.uniform(100, 5000)  # 模拟距离
            
            if distance <= radius:
                product_data = {
                    'id': product['id'],
                    'userId': product['userId'],
                    'title': product['title'],
                    'description': product['description'],
                    'coverImage': product['coverImage'],
                    'categoryId': product['categoryId'],
                    'price': product['price'],
                    'productCondition': product['productCondition'],
                    'tradeType': product['tradeType'],
                    'address': product['address'],
                    'longitude': product['longitude'],
                    'latitude': product['latitude'],
                    'distance': round(distance, 2),
                    'createTime': product['createTime'],
                    'updateTime': product['updateTime']
                }
                all_products.append(product_data)
    
    # 分页
    start = (page - 1) * page_size
    end = start + page_size
    paginated_products = all_products[start:end]
    
    # 按距离排序
    paginated_products.sort(key=lambda x: x['distance'])
    
    return success_response({
        'records': paginated_products,
        'total': len(all_products),
        'page': page,
        'pageSize': page_size,
        'pages': (len(all_products) + page_size - 1) // page_size
    })

# AI估价接口
@app.route('/api/product/estimate', methods=['POST'])
def estimate_price():
    # 验证登录
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    data = request.get_json()
    
    if not data or not all(k in data for k in ['title', 'productCondition', 'categoryId']):
        return error_response(400, '参数不完整')
    
    title = data['title']
    condition = data['productCondition']
    category_id = data['categoryId']
    
    # 模拟估价逻辑
    base_prices = {
        1: random.uniform(1000, 8000),   # 手机数码
        2: random.uniform(2000, 15000),  # 电脑办公
        3: random.uniform(50, 500),       # 图书教材
        4: random.uniform(500, 5000),    # 家用电器
        5: random.uniform(200, 2000),    # 服饰鞋包
        6: random.uniform(300, 3000),    # 运动户外
        7: random.uniform(100, 1000),    # 美妆个护
        8: random.uniform(50, 1000)      # 其他闲置
    }
    
    base_price = base_prices.get(category_id, 500)
    
    # 根据成色调整价格
    condition_multipliers = {
        '全新': 1.0,
        '九成新': 0.85,
        '八成新': 0.7,
        '七成新': 0.55,
        '七成新及以下': 0.4
    }
    
    multiplier = condition_multipliers.get(condition, 0.7)
    estimated_price = round(base_price * multiplier, 2)
    
    # 添加随机波动
    price_variation = random.uniform(0.9, 1.1)
    final_price = round(estimated_price * price_variation, 2)
    
    # 模拟置信度
    confidence = random.randint(70, 95)
    
    return success_response({
        'estimatedPrice': final_price,
        'confidence': confidence,
        'factors': [
            {'name': '商品分类', 'value': next((c['name'] for c in mock_db['categories'] if c['id'] == category_id), '未知')},
            {'name': '成色', 'value': condition},
            {'name': '市场参考价', 'value': f'¥{base_price:.2f}'}
        ]
    })

# 获取商品分类接口
@app.route('/api/product/categories', methods=['GET'])
def get_categories():
    return success_response(mock_db['categories'])

# Mock API文档接口
@app.route('/api/swagger', methods=['GET'])
def swagger_ui():
    swagger_doc = {
        "openapi": "3.0.0",
        "info": {
            "title": "二手商品交易市场 Mock API",
            "version": "1.0.0",
            "description": "Mock API用于前端开发和测试"
        },
        "servers": [
            {
                "url": "http://localhost:8080",
                "description": "Mock服务器"
            }
        ],
        "paths": {
            "/api/health": {
                "get": {
                    "summary": "健康检查",
                    "responses": {
                        "200": {"description": "服务正常"}
                    }
                }
            },
            "/api/user/register": {
                "post": {
                    "summary": "用户注册",
                    "requestBody": {
                        "content": {
                            "application/json": {
                                "schema": {
                                    "type": "object",
                                    "properties": {
                                        "username": {"type": "string"},
                                        "password": {"type": "string"},
                                        "confirmPassword": {"type": "string"},
                                        "phone": {"type": "string"},
                                        "nickname": {"type": "string"}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    return jsonify(swagger_doc)

if __name__ == '__main__':
    print("🚀 启动Mock API服务器...")
    print("📍 服务地址: http://localhost:8080")
    print("📚 API文档: http://localhost:8080/api/swagger")
    print("🔗 健康检查: http://localhost:8080/api/health")
    
    # 创建一些测试数据
    mock_db['users'].append({
        'id': 1,
        'username': 'admin',
        'password': hash_password('123456'),
        'phone': '13800138000',
        'nickname': '管理员',
        'create_time': datetime.now().isoformat(),
        'update_time': datetime.now().isoformat(),
        'status': 1
    })
    mock_db['next_user_id'] = 2
    
    # 模拟一些商品
    mock_products = [
        {
            'id': 1,
            'userId': 1,
            'title': 'iPhone 13 Pro 256G 深空灰',
            'description': '95新，无拆修，原装充电器，发票齐全',
            'price': 6999.00,
            'productCondition': '九成新',
            'tradeType': 'offline',
            'address': '北京市朝阳区国贸CBD',
            'longitude': 116.466240,
            'latitude': 39.920800,
            'createTime': (datetime.now() - timedelta(days=1)).isoformat(),
            'updateTime': (datetime.now() - timedelta(days=1)).isoformat(),
            'status': 1
        },
        {
            'id': 2,
            'userId': 1,
            'title': 'MacBook Pro 13寸 2020款',
            'description': '性能完好，轻度使用，适合办公学习',
            'price': 8999.00,
            'productCondition': '八成新',
            'tradeType': 'online',
            'address': '',
            'longitude': None,
            'latitude': None,
            'createTime': (datetime.now() - timedelta(days=3)).isoformat(),
            'updateTime': (datetime.now() - timedelta(days=3)).isoformat(),
            'status': 1
        }
    ]
    
    mock_db['products'].extend(mock_products)
    mock_db['next_product_id'] = 3
    
    app.run(debug=True, host='0.0.0.0', port=8080)