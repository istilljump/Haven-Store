#!/usr/bin/env python3
"""
启动后端服务的Python脚本
模拟Spring Boot API服务用于测试
"""

from flask import Flask, request, jsonify, Response
import json
import time
import uuid
from datetime import datetime, timedelta
import bcrypt
import random
import math

app = Flask(__name__)

# 模拟数据库数据
mock_db = {
    'users': [],
    'products': [],
    'categories': [
        {'id': 1, 'name': '手机数码', 'sort': 1, 'status': 1},
        {'id': 2, 'name': '电脑办公', 'sort': 2, 'status': 1},
        {'id': 3, 'name': '图书教材', 'sort': 3, 'status': 1},
        {'id': 4, 'name': '家用电器', 'sort': 4, 'status': 1},
        {'id': 5, 'name': '服饰鞋包', 'sort': 5, 'status': 1},
        {'id': 6, 'name': '运动户外', 'sort': 6, 'status': 1},
        {'id': 7, 'name': '美妆个护', 'sort': 7, 'status': 1},
        {'id': 8, 'name': '其他闲置', 'sort': 8, 'status': 1}
    ],
    'admin_users': [
        {'id': 1, 'username': 'admin', 'password': bcrypt.hashpw('123456'.encode('utf-8'), bcrypt.gensalt()).decode('utf-8'), 
         'phone': '13800138000', 'nickname': '管理员', 'is_admin': True, 'status': 1, 'create_time': datetime.now().isoformat()}
    ],
    'system_messages': [],
    'next_user_id': 1,
    'next_product_id': 1,
    'next_message_id': 1
}

# 模拟JWT token生成
def generate_token(user_id, username, is_admin=False):
    return f"mock_token_{user_id}_{int(time.time())}_{'admin' if is_admin else 'user'}"

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
        'status': 1,
        'is_admin': False
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
    
    # 管理员用户单独检查
    if not user:
        for u in mock_db['admin_users']:
            if u['username'] == username:
                user = u
                break
    
    if not user:
        return error_response(401, '用户名或密码错误')
    
    # 验证密码
    if not verify_password(password, user['password']):
        return error_response(401, '用户名或密码错误')
    
    # 生成token
    token = generate_token(user['id'], user['username'], user.get('is_admin', False))
    
    # 返回用户信息（脱敏）
    user_info = {
        'userId': user['id'],
        'username': user['username'],
        'nickname': user['nickname'],
        'phone': user['phone'],
        'avatar': user['avatar'],
        'createTime': user['create_time'],
        'isAdmin': user.get('is_admin', False)
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
    
    # 检查管理员用户
    if not user:
        for u in mock_db['admin_users']:
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
        'status': user['status'],
        'isAdmin': user.get('is_admin', False)
    }
    
    return success_response(user_info)

# 管理员登录接口
@app.route('/api/admin/login', methods=['POST'])
def admin_login():
    data = request.get_json()
    
    if not data or not all(k in data for k in ['username', 'password']):
        return error_response(400, '参数不完整')
    
    username = data['username']
    password = data['password']
    
    # 查找管理员用户
    admin = None
    for a in mock_db['admin_users']:
        if a['username'] == username:
            admin = a
            break
    
    if not admin:
        return error_response(401, '管理员账号或密码错误')
    
    # 验证密码
    if not verify_password(password, admin['password']):
        return error_response(401, '管理员账号或密码错误')
    
    # 生成token
    token = generate_token(admin['id'], admin['username'], True)
    
    # 返回管理员信息（脱敏）
    admin_info = {
        'adminId': admin['id'],
        'username': admin['username'],
        'nickname': admin['nickname'],
        'phone': admin['phone'],
        'createTime': admin['create_time'],
        'isAdmin': True
    }
    
    return success_response({
        'token': token,
        'adminInfo': admin_info
    }, '管理员登录成功')

# 获取管理员统计信息
@app.route('/api/admin/dashboard', methods=['GET'])
def get_admin_dashboard():
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    # 检查是否为管理员token
    if '_admin' not in auth_header:
        return error_response(403, '权限不足')
    
    # 统计数据
    total_users = len(mock_db['users']) + len(mock_db['admin_users'])
    total_products = len([p for p in mock_db['products'] if p['status'] == 1])
    total_categories = len(mock_db['categories'])
    today_products = len([p for p in mock_db['products'] 
                         if datetime.fromisoformat(p['create_time']).date() == datetime.now().date()])
    
    dashboard_data = {
        'statistics': {
            'totalUsers': total_users,
            'totalProducts': total_products,
            'totalCategories': total_categories,
            'todayProducts': today_products,
            'activeUsers': total_users * 0.3,  # 模拟活跃用户数
            'pendingProducts': 0  # 模拟待审核商品数
        },
        'recentProducts': [
            {
                'id': p['id'],
                'title': p['title'],
                'price': p['price'],
                'status': p['status'],
                'createTime': p['createTime']
            } for p in mock_db['products'][-5:]  # 最近5个商品
        ],
        'systemMessages': mock_db['system_messages'][-3:]  # 最近3条系统消息
    }
    
    return success_response(dashboard_data)

# 获取用户列表（管理员功能）
@app.route('/api/admin/users', methods=['GET'])
def get_admin_users():
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    if '_admin' not in auth_header:
        return error_response(403, '权限不足')
    
    page = int(request.args.get('page', 1))
    page_size = int(request.args.get('pageSize', 10))
    status = request.args.get('status', '')
    keyword = request.args.get('keyword', '')
    
    # 获取所有用户
    all_users = []
    for u in mock_db['users']:
        user_data = {
            'id': u['id'],
            'username': u['username'],
            'nickname': u['nickname'],
            'phone': u['phone'],
            'avatar': u['avatar'],
            'status': u['status'],
            'createTime': u['create_time'],
            'isAdmin': u.get('is_admin', False)
        }
        all_users.append(user_data)
    
    # 添加管理员用户到列表
    for u in mock_db['admin_users']:
        admin_data = {
            'id': u['id'],
            'username': u['username'],
            'nickname': u['nickname'],
            'phone': u['phone'],
            'avatar': u['avatar'],
            'status': u['status'],
            'createTime': u['create_time'],
            'isAdmin': True
        }
        all_users.append(admin_data)
    
    # 过滤
    filtered_users = all_users
    if status:
        filtered_users = [u for u in filtered_users if str(u['status']) == status]
    if keyword:
        filtered_users = [u for u in filtered_users 
                         if keyword in u['username'] or keyword in u['nickname'] or keyword in u['phone']]
    
    # 分页
    start = (page - 1) * page_size
    end = start + page_size
    paginated_users = filtered_users[start:end]
    
    return success_response({
        'records': paginated_users,
        'total': len(filtered_users),
        'page': page,
        'pageSize': page_size,
        'pages': (len(filtered_users) + page_size - 1) // page_size
    })

# 更新用户状态（管理员功能）
@app.route('/api/admin/users/<int:user_id>/status', methods=['PUT'])
def update_user_status(user_id):
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    if '_admin' not in auth_header:
        return error_response(403, '权限不足')
    
    data = request.get_json()
    if not data or 'status' not in data:
        return error_response(400, '参数不完整')
    
    new_status = int(data['status'])
    if new_status not in [1, 2]:  # 1-正常，2-禁用
        return error_response(400, '无效的状态值')
    
    # 查找并更新用户
    found = False
    for user_list in [mock_db['users'], mock_db['admin_users']]:
        for user in user_list:
            if user['id'] == user_id:
                user['status'] = new_status
                user['update_time'] = datetime.now().isoformat()
                found = True
                break
        if found:
            break
    
    if not found:
        return error_response(404, '用户不存在')
    
    return success_response(None, '用户状态更新成功')

# 获取商品列表（管理员功能）
@app.route('/api/admin/products', methods=['GET'])
def get_admin_products():
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    if '_admin' not in auth_header:
        return error_response(403, '权限不足')
    
    page = int(request.args.get('page', 1))
    page_size = int(request.args.get('pageSize', 10))
    status = request.args.get('status', '')
    category_id = request.args.get('categoryId', '')
    keyword = request.args.get('keyword', '')
    
    # 获取所有商品
    all_products = []
    for p in mock_db['products']:
        # 查找用户名
        username = '未知用户'
        for u in mock_db['users'] + mock_db['admin_users']:
            if u['id'] == p['userId']:
                username = u['username']
                break
        
        product_data = {
            'id': p['id'],
            'userId': p['userId'],
            'username': username,
            'title': p['title'],
            'description': p['description'],
            'coverImage': p['coverImage'],
            'categoryId': p['categoryId'],
            'price': p['price'],
            'productCondition': p['productCondition'],
            'tradeType': p['tradeType'],
            'address': p['address'],
            'longitude': p['longitude'],
            'latitude': p['latitude'],
            'status': p['status'],
            'createTime': p['create_time'],
            'updateTime': p['update_time']
        }
        all_products.append(product_data)
    
    # 过滤
    filtered_products = all_products
    if status:
        filtered_products = [p for p in filtered_products if str(p['status']) == status]
    if category_id:
        filtered_products = [p for p in filtered_products if str(p['categoryId']) == category_id]
    if keyword:
        filtered_products = [p for p in filtered_products 
                           if keyword in p['title'] or keyword in p['description']]
    
    # 分页
    start = (page - 1) * page_size
    end = start + page_size
    paginated_products = filtered_products[start:end]
    
    return success_response({
        'records': paginated_products,
        'total': len(filtered_products),
        'page': page,
        'pageSize': page_size,
        'pages': (len(filtered_products) + page_size - 1) // page_size
    })

# 更新商品状态（管理员功能）
@app.route('/api/admin/products/<int:product_id>/status', methods=['PUT'])
def update_product_status(product_id):
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    if '_admin' not in auth_header:
        return error_response(403, '权限不足')
    
    data = request.get_json()
    if not data or 'status' not in data:
        return error_response(400, '参数不完整')
    
    new_status = int(data['status'])
    if new_status not in [1, 2, 3]:  # 1-在售，2-已售出，3-已下架
        return error_response(400, '无效的状态值')
    
    # 查找并更新商品
    found = False
    for product in mock_db['products']:
        if product['id'] == product_id:
            product['status'] = new_status
            product['update_time'] = datetime.now().isoformat()
            found = True
            break
    
    if not found:
        return error_response(404, '商品不存在')
    
    return success_response(None, '商品状态更新成功')

# 获取分类列表（管理员功能）
@app.route('/api/admin/categories', methods=['GET'])
def get_admin_categories():
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    if '_admin' not in auth_header:
        return error_response(403, '权限不足')
    
    return success_response(mock_db['categories'])

# 添加分类（管理员功能）
@app.route('/api/admin/categories', methods=['POST'])
def add_category():
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    if '_admin' not in auth_header:
        return error_response(403, '权限不足')
    
    data = request.get_json()
    if not data or not all(k in data for k in ['name', 'sort']):
        return error_response(400, '参数不完整')
    
    name = data['name']
    sort = int(data['sort'])
    
    # 检查分类名是否已存在
    for category in mock_db['categories']:
        if category['name'] == name:
            return error_response(400, '分类名已存在')
    
    # 创建分类
    category_id = len(mock_db['categories']) + 1
    new_category = {
        'id': category_id,
        'name': name,
        'sort': sort,
        'status': 1,
        'createTime': datetime.now().isoformat(),
        'updateTime': datetime.now().isoformat()
    }
    
    mock_db['categories'].append(new_category)
    mock_db['categories'].sort(key=lambda x: x['sort'])
    
    return success_response(new_category, '分类添加成功')

# 更新分类（管理员功能）
@app.route('/api/admin/categories/<int:category_id>', methods=['PUT'])
def update_category(category_id):
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    if '_admin' not in auth_header:
        return error_response(403, '权限不足')
    
    data = request.get_json()
    if not data:
        return error_response(400, '参数不完整')
    
    # 查找并更新分类
    found = False
    for category in mock_db['categories']:
        if category['id'] == category_id:
            category['name'] = data.get('name', category['name'])
            category['sort'] = data.get('sort', category['sort'])
            category['status'] = data.get('status', category['status'])
            category['updateTime'] = datetime.now().isoformat()
            found = True
            break
    
    if not found:
        return error_response(404, '分类不存在')
    
    return success_response(category, '分类更新成功')

# 删除分类（管理员功能）
@app.route('/api/admin/categories/<int:category_id>', methods=['DELETE'])
def delete_category(category_id):
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    if '_admin' not in auth_header:
        return error_response(403, '权限不足')
    
    # 检查是否有关联商品
    for product in mock_db['products']:
        if product['categoryId'] == category_id:
            return error_response(400, '该分类下存在商品，无法删除')
    
    # 删除分类
    found = False
    for i, category in enumerate(mock_db['categories']):
        if category['id'] == category_id:
            mock_db['categories'].pop(i)
            found = True
            break
    
    if not found:
        return error_response(404, '分类不存在')
    
    return success_response(None, '分类删除成功')

# 发送系统消息（管理员功能）
@app.route('/api/admin/messages', methods=['POST'])
def send_system_message():
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    if '_admin' not in auth_header:
        return error_response(403, '权限不足')
    
    data = request.get_json()
    if not data or not all(k in data for k in ['title', 'content', 'userType']):
        return error_response(400, '参数不完整')
    
    title = data['title']
    content = data['content']
    user_type = data['userType']  # 'all' | 'admin' | 'user'
    
    # 创建系统消息
    message_id = mock_db['next_message_id']
    new_message = {
        'id': message_id,
        'title': title,
        'content': content,
        'userType': user_type,
        'createTime': datetime.now().isoformat(),
        'updateTime': datetime.now().isoformat(),
        'status': 1  # 1-发送中，2-已送达
    }
    
    mock_db['system_messages'].append(new_message)
    mock_db['next_message_id'] += 1
    
    return success_response(new_message, '系统消息发送成功')

# 获取系统消息列表（管理员功能）
@app.route('/api/admin/messages', methods=['GET'])
def get_admin_messages():
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    if '_admin' not in auth_header:
        return error_response(403, '权限不足')
    
    page = int(request.args.get('page', 1))
    page_size = int(request.args.get('pageSize', 10))
    status = request.args.get('status', '')
    user_type = request.args.get('userType', '')
    
    # 获取所有消息
    all_messages = mock_db['system_messages']
    
    # 过滤
    filtered_messages = all_messages
    if status:
        filtered_messages = [m for m in filtered_messages if str(m['status']) == status]
    if user_type:
        filtered_messages = [m for m in filtered_messages if m['userType'] == user_type]
    
    # 按创建时间倒序
    filtered_messages.sort(key=lambda x: x['id'], reverse=True)
    
    # 分页
    start = (page - 1) * page_size
    end = start + page_size
    paginated_messages = filtered_messages[start:end]
    
    return success_response({
        'records': paginated_messages,
        'total': len(filtered_messages),
        'page': page,
        'pageSize': page_size,
        'pages': (len(filtered_messages) + page_size - 1) // page_size
    })

# 获取系统设置（管理员功能）
@app.route('/api/admin/settings', methods=['GET'])
def get_admin_settings():
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    if '_admin' not in auth_header:
        return error_response(403, '权限不足')
    
    settings = {
        'site': {
            'name': '二手商品交易市场',
            'description': '专业的二手商品交易平台',
            'logo': '/logo.png',
            'icp': '京ICP备123456789号'
        },
        'upload': {
            'maxFileSize': 5242880,  # 5MB
            'allowedTypes': ['jpg', 'jpeg', 'png', 'gif'],
            'maxImages': 9
        },
        'trade': {
            'autoEstimate': True,
            'estimateConfidence': 0.8,
            'maxPrice': 1000000
        },
        'contact': {
            'email': 'admin@secondhand.com',
            'phone': '400-123-4567',
            'address': '北京市朝阳区国贸CBD'
        }
    }
    
    return success_response(settings)

# 更新系统设置（管理员功能）
@app.route('/api/admin/settings', methods=['PUT'])
def update_admin_settings():
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return error_response(401, '未授权访问')
    
    if '_admin' not in auth_header:
        return error_response(403, '权限不足')
    
    data = request.get_json()
    if not data:
        return error_response(400, '参数不完整')
    
    # 模拟设置更新
    return success_response(data, '系统设置更新成功')

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
    print("🔐 管理员登录: http://localhost:8080/api/admin/login")
    print("📊 管理后台首页: http://localhost:8080/api/admin/dashboard")
    
    # 创建一些测试数据
    mock_db['users'].append({
        'id': 1,
        'username': 'admin',
        'password': hash_password('123456'),
        'phone': '13800138000',
        'nickname': '管理员',
        'create_time': datetime.now().isoformat(),
        'update_time': datetime.now().isoformat(),
        'status': 1,
        'is_admin': True
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