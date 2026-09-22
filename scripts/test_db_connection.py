#!/usr/bin/env python3
"""
数据库连接测试脚本
"""

import pymysql
import sys

def test_mysql_connection():
    """测试MySQL连接"""
    try:
        connection = pymysql.connect(
            host='localhost',
            user='root',
            password='1234',
            database='second_hand_market',
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )
        
        print("✅ MySQL连接成功")
        
        # 检查表是否存在
        with connection.cursor() as cursor:
            # 检查用户表
            cursor.execute("SHOW TABLES LIKE 'user'")
            user_table_exists = cursor.fetchone() is not None
            
            cursor.execute("SHOW TABLES LIKE 'category'")
            category_table_exists = cursor.fetchone() is not None
            
            cursor.execute("SHOW TABLES LIKE 'product'")
            product_table_exists = cursor.fetchone() is not None
            
            print(f"📋 user表: {'✅ 存在' if user_table_exists else '❌ 不存在'}")
            print(f"📋 category表: {'✅ 存在' if category_table_exists else '❌ 不存在'}")
            print(f"📋 product表: {'✅ 存在' if product_table_exists else '❌ 不存在'}")
            
            if not all([user_table_exists, category_table_exists, product_table_exists]):
                print("\n⚠️ 数据库表不存在，需要初始化数据库")
                print("请执行以下SQL语句初始化数据库:")
                print("mysql -u root -p < init_database.sql")
                
        connection.close()
        return True
        
    except pymysql.MySQLError as e:
        if "Unknown database" in str(e):
            print("❌ 数据库 'second_hand_market' 不存在")
            print("请先创建数据库：")
            print("mysql -u root -p -e 'CREATE DATABASE second_hand_market CHARACTER SET utf8mb4;'")
        else:
            print(f"❌ MySQL连接失败: {e}")
        return False
    except Exception as e:
        print(f"❌ 连接测试失败: {e}")
        return False

def main():
    print("🔍 测试数据库连接...")
    success = test_mysql_connection()
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()