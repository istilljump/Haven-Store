#!/usr/bin/env node
/**
 * 前端API测试脚本
 * 测试前端与API接口的交互功能
 */

const https = require('https');
const http = require('http');
const fs = require('fs');

class FrontendAPITester {
  constructor() {
    this.results = [];
    this.baseUrl = 'http://localhost:3000';
    this.apiUrl = 'http://localhost:8080/api';
    this.session = {};
  }

  log(testName, passed, details = {}) {
    const result = {
      name: testName,
      passed,
      timestamp: new Date().toISOString(),
      details
    };

    this.results.push(result);

    const status = passed ? '✅' : '❌';
    console.log(`${status} ${testName}`);
    
    if (!passed) {
      console.log(`   错误详情:`, JSON.stringify(details, null, 2));
    }
  }

  async testHealth() {
    console.log('\n🔍 测试健康检查...');
    
    return new Promise((resolve) => {
      const url = `${this.apiUrl}/health`;
      const req = http.get(url, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          try {
            const result = JSON.parse(data);
            if (res.statusCode === 200 && result.code === 200) {
              this.log('健康检查API响应正常', true, {
                status: res.statusCode,
                data: result
              });
            } else {
              this.log('健康检查API响应异常', false, {
                status: res.statusCode,
                data: result
              });
            }
          } catch (error) {
            this.log('健康检查API解析失败', false, {
              status: res.statusCode,
              error: error.message,
              data: data
            });
          }
          resolve();
        });
      });
      
      req.on('error', (error) => {
        this.log('健康检查API连接失败', false, { error: error.message });
        resolve();
      });
      
      req.setTimeout(5000, () => {
        req.destroy();
        this.log('健康检查API超时', false);
        resolve();
      });
    });
  }

  async testFrontendPages() {
    console.log('\n📄 测试前端页面...');
    
    const pages = [
      { path: '/', name: '首页' },
      { path: '/login', name: '登录页' },
      { path: '/register', name: '注册页' },
      { path: '/products', name: '商品列表' },
      { path: '/publish', name: '发布商品' }
    ];

    for (const page of pages) {
      await new Promise((resolve) => {
        const url = `${this.baseUrl}${page.path}`;
        const req = http.get(url, (res) => {
          let data = '';
          res.on('data', chunk => data += chunk);
          res.on('end', () => {
            if (res.statusCode === 200 && data.includes('二手商品交易市场')) {
              this.log(`${page.name}页面加载正常`, true, {
                status: res.statusCode,
                url,
                length: data.length
              });
            } else {
              this.log(`${page.name}页面加载异常`, false, {
                status: res.statusCode,
                url,
                preview: data.substring(0, 200)
              });
            }
            resolve();
          });
        });
        
        req.on('error', (error) => {
          this.log(`${page.name}页面连接失败`, false, {
            url,
            error: error.message
          });
          resolve();
        });
        
        req.setTimeout(3000, () => {
          req.destroy();
          this.log(`${page.name}页面超时`, false, { url });
          resolve();
        });
      });
    }
  }

  async testUserRegistration() {
    console.log('\n👤 测试用户注册API...');
    
    return new Promise((resolve) => {
      const testData = {
        username: `testuser_${Date.now()}`,
        password: '123456',
        confirmPassword: '123456',
        phone: '13800138000',
        nickname: `测试用户_${Date.now()}`
      };

      const postData = JSON.stringify(testData);
      
      const options = {
        hostname: 'localhost',
        port: 8080,
        path: '/api/user/register',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData)
        }
      };

      const req = http.request(options, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          try {
            const result = JSON.parse(data);
            if (res.statusCode === 200 && result.code === 200) {
              this.log('用户注册API调用成功', true, {
                username: testData.username,
                result
              });
              // 保存测试用户信息
              this.testUser = testData;
            } else if (res.statusCode === 200 && result.code === 400 && result.msg.includes('已存在')) {
              this.log('用户注册API - 用户名已存在', true, {
                username: testData.username,
                result
              });
            } else {
              this.log('用户注册API调用失败', false, {
                status: res.statusCode,
                result
              });
            }
          } catch (error) {
            this.log('用户注册API响应解析失败', false, {
              status: res.statusCode,
              error: error.message,
              data: data
            });
          }
          resolve();
        });
      });
      
      req.on('error', (error) => {
        this.log('用户注册API连接失败', false, {
          error: error.message
        });
        resolve();
      });
      
      req.write(postData);
      req.end();
      
      req.setTimeout(10000, () => {
        req.destroy();
        this.log('用户注册API超时', false);
        resolve();
      });
    });
  }

  async testUserLogin() {
    console.log('\n🔐 测试用户登录API...');
    
    if (!this.testUser) {
      this.log('跳过登录测试 - 没有测试用户', false, { reason: '需要先注册用户' });
      return Promise.resolve();
    }

    return new Promise((resolve) => {
      const loginData = {
        username: this.testUser.username,
        password: this.testUser.password
      };

      const postData = JSON.stringify(loginData);
      
      const options = {
        hostname: 'localhost',
        port: 8080,
        path: '/api/user/login',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData)
        }
      };

      const req = http.request(options, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          try {
            const result = JSON.parse(data);
            if (res.statusCode === 200 && result.code === 200) {
              const token = result.data.token;
              this.session.token = token;
              this.session.userId = result.data.userId;
              this.log('用户登录API调用成功', true, {
                username: this.testUser.username,
                token: token.substring(0, 20) + '...',
                userId: this.session.userId
              });
            } else {
              this.log('用户登录API调用失败', false, {
                status: res.statusCode,
                result
              });
            }
          } catch (error) {
            this.log('用户登录API响应解析失败', false, {
              status: res.statusCode,
              error: error.message,
              data: data
            });
          }
          resolve();
        });
      });
      
      req.on('error', (error) => {
        this.log('用户登录API连接失败', false, {
          error: error.message
        });
        resolve();
      });
      
      req.write(postData);
      req.end();
      
      req.setTimeout(10000, () => {
        req.destroy();
        this.log('用户登录API超时', false);
        resolve();
      });
    });
  }

  async testProductOperations() {
    console.log('\n📦 测试商品相关API...');
    
    if (!this.session.token) {
      this.log('跳过商品API测试 - 需要登录', false, { reason: '需要先登录' });
      return Promise.resolve();
    }

    // 测试商品分类获取
    await new Promise((resolve) => {
      const options = {
        hostname: 'localhost',
        port: 8080,
        path: '/api/product/categories',
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.session.token}`
        }
      };

      const req = http.get(options, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          try {
            const result = JSON.parse(data);
            if (res.statusCode === 200 && result.code === 200) {
              this.log('获取商品分类API调用成功', true, {
                categoryCount: result.data.length
              });
            } else {
              this.log('获取商品分类API调用失败', false, {
                status: res.statusCode,
                result
              });
            }
          } catch (error) {
            this.log('获取商品分类API响应解析失败', false, {
              status: res.statusCode,
              error: error.message
            });
          }
          resolve();
        });
      });
      
      req.on('error', (error) => {
        this.log('获取商品分类API连接失败', false, { error: error.message });
        resolve();
      });
    });

    // 测试发布商品
    await new Promise((resolve) => {
      const productData = {
        title: `测试商品_${Date.now()}`,
        categoryId: 1,
        description: '这是一个测试商品',
        price: 999.99,
        productCondition: '九成新',
        tradeType: 'offline',
        address: '测试地址',
        longitude: 116.316833,
        latitude: 39.981013
      };

      const postData = JSON.stringify(productData);
      
      const options = {
        hostname: 'localhost',
        port: 8080,
        path: '/api/product/add',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData),
          'Authorization': `Bearer ${this.session.token}`
        }
      };

      const req = http.request(options, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          try {
            const result = JSON.parse(data);
            if (res.statusCode === 200 && result.code === 200) {
              this.log('发布商品API调用成功', true, {
                title: productData.title,
                productId: result.data.productId
              });
            } else {
              this.log('发布商品API调用失败', false, {
                status: res.statusCode,
                result
              });
            }
          } catch (error) {
            this.log('发布商品API响应解析失败', false, {
              status: res.statusCode,
              error: error.message,
              data: data
            });
          }
          resolve();
        });
      });
      
      req.on('error', (error) => {
        this.log('发布商品API连接失败', false, { error: error.message });
        resolve();
      });
      
      req.write(postData);
      req.end();
      
      req.setTimeout(10000, () => {
        req.destroy();
        this.log('发布商品API超时', false);
        resolve();
      });
    });
  }

  async generateReport() {
    console.log('\n📊 生成测试报告...');
    
    const passedCount = this.results.filter(r => r.passed).length;
    const failedCount = this.results.filter(r => !r.passed).length;
    const totalCount = this.results.length;

    const report = {
      testDate: new Date().toISOString(),
      environment: {
        frontend: this.baseUrl,
        backend: this.apiUrl
      },
      summary: {
        total: totalCount,
        passed: passedCount,
        failed: failedCount,
        successRate: totalCount > 0 ? ((passedCount / totalCount) * 100).toFixed(2) + '%' : '0%'
      },
      details: this.results
    };

    // 保存测试报告
    const reportPath = './frontend_api_test_report.json';
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    console.log(`📄 JSON测试报告已保存到: ${reportPath}`);

    // 生成简单的HTML报告
    const htmlReport = `
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>前端API测试报告</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f5f5f5; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
        .summary { display: flex; gap: 20px; margin-bottom: 20px; }
        .stat { background: #e8f5e8; padding: 15px; border-radius: 8px; flex: 1; }
        .stat.failed { background: #ffe8e8; }
        .details { margin-top: 20px; }
        .test-item { border: 1px solid #ddd; padding: 15px; margin-bottom: 10px; border-radius: 8px; }
        .test-item.passed { background: #e8f5e8; }
        .test-item.failed { background: #ffe8e8; }
        .test-name { font-weight: bold; margin-bottom: 10px; }
        .test-details { font-size: 12px; color: #666; }
    </style>
</head>
<body>
    <div class="header">
        <h1>前端API测试报告</h1>
        <p>测试时间: ${report.testDate}</p>
        <p>前端地址: ${this.baseUrl}</p>
        <p>后端地址: ${this.apiUrl}</p>
        
        <div class="summary">
            <div class="stat">
                <h3>总测试数</h3>
                <p>${report.summary.total}</p>
            </div>
            <div class="stat passed">
                <h3>通过</h3>
                <p>${report.summary.passed}</p>
            </div>
            <div class="stat failed">
                <h3>失败</h3>
                <p>${report.summary.failed}</p>
            </div>
            <div class="stat">
                <h3>成功率</h3>
                <p>${report.summary.successRate}</p>
            </div>
        </div>
    </div>

    <div class="details">
        <h2>测试详情</h2>
        ${this.results.map(result => `
            <div class="test-item ${result.passed ? 'passed' : 'failed'}">
                <div class="test-name">
                    ${result.passed ? '✅' : '❌'} ${result.name}
                </div>
                <div class="test-details">
                    时间: ${result.timestamp}<br>
                    ${result.details && Object.keys(result.details).length > 0 ? 
                      '详情: ' + JSON.stringify(result.details, null, 2) : 
                      '无详细信息'}
                </div>
            </div>
        `).join('')}
    </div>
</body>
</html>
    `;

    const htmlPath = './frontend_api_test_report.html';
    fs.writeFileSync(htmlPath, htmlReport);
    console.log(`📄 HTML测试报告已保存到: ${htmlPath}`);

    return report;
  }

  async runAllTests() {
    console.log('🚀 开始前端API测试...');
    console.log('📅 测试时间:', new Date().toISOString());

    try {
      // 按顺序执行测试
      await this.testHealth();
      await this.testFrontendPages();
      await this.testUserRegistration();
      await this.testUserLogin();
      await this.testProductOperations();

      // 生成报告
      const report = await this.generateReport();
      
      console.log('\n🎉 测试完成！');
      console.log(`📊 测试总结:`);
      console.log(`   总计: ${report.summary.total}`);
      console.log(`   通过: ${report.summary.passed}`);
      console.log(`   失败: ${report.summary.failed}`);
      console.log(`   成功率: ${report.summary.successRate}`);

      return report;

    } catch (error) {
      console.error('❌ 测试过程中发生错误:', error);
      throw error;
    }
  }
}

// 如果直接运行此脚本
if (require.main === module) {
  const tester = new FrontendAPITester();
  tester.runAllTests()
    .then(report => {
      const success = report.summary.failed === 0;
      console.log(success ? '\n🎉 所有测试通过！' : '\n⚠️ 部分测试失败');
      process.exit(success ? 0 : 1);
    })
    .catch(error => {
      console.error('测试失败:', error);
      process.exit(1);
    });
}

module.exports = FrontendAPITester;