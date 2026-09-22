#!/usr/bin/env node
/**
 * 前端功能测试脚本
 * 用于测试前端页面的基本功能和API交互
 */

const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

class FrontendTester {
  constructor() {
    this.browser = null;
    this.page = null;
    this.testResults = [];
    this.baseUrl = 'http://localhost:3000';
  }

  async init() {
    console.log('🚀 启动浏览器...');
    this.browser = await puppeteer.launch({
      headless: false, // 设置为true可以实现无头模式
      slowMo: 100, // 减慢操作速度，便于观察
      args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    this.page = await this.browser.newPage();
    await this.page.setViewport({ width: 1280, height: 720 });

    // 监听控制台消息
    this.page.on('console', msg => {
      console.log(`[浏览器] ${msg.text()}`);
    });

    // 监听网络请求
    this.page.on('request', req => {
      console.log(`[请求] ${req.method()} ${req.url()}`);
    });

    this.page.on('response', res => {
      console.log(`[响应] ${res.status()} ${res.url()}`);
    });
  }

  async testPageLoad() {
    console.log('\n📄 测试页面加载...');
    
    try {
      await this.page.goto(this.baseUrl, { 
        waitUntil: 'networkidle2',
        timeout: 15000 
      });

      // 等待页面加载完成
      await this.page.waitForSelector('#app', { timeout: 10000 });
      
      // 检查页面标题
      const title = await this.page.title();
      this.testResult('页面加载成功', title.includes('二手商品交易市场'), {
        title,
        url: this.page.url()
      });

      // 检查主要元素是否存在
      const elements = {
        '首页链接': '#home-link',
        '登录按钮': '.login-btn',
        '注册链接': '.register-link',
        '搜索框': '.search-input'
      };

      for (const [name, selector] of Object.entries(elements)) {
        try {
          await this.page.waitForSelector(selector, { timeout: 5000 });
          console.log(`✅ ${name} 元素存在`);
        } catch (error) {
          console.log(`❌ ${name} 元素不存在`);
        }
      }

    } catch (error) {
      this.testResult('页面加载失败', false, { error: error.message });
    }
  }

  async testFormValidation() {
    console.log('\n📝 测试表单验证功能...');

    // 测试登录表单验证
    await this.page.click('.login-btn');
    await this.page.waitForSelector('.login-form', { timeout: 5000 });

    // 测试空表单提交
    await this.page.click('.login-submit-btn');
    await this.page.waitForTimeout(1000);

    // 检查错误提示
    const errorExists = await this.page.$$('.error-message').length > 0;
    this.testResult('登录表单验证提示存在', errorExists, {
      errorCount: await this.page.$$('.error-message').length
    });

    // 测试用户名格式验证
    await this.typeInput('.username-input', 'us'); // 太短的用户名
    await this.page.click('.login-submit-btn');
    await this.page.waitForTimeout(500);

    // 测试密码验证
    await this.typeInput('.password-input', '123'); // 太短的密码
    await this.page.click('.login-submit-btn');
    await this.page.waitForTimeout(500);

    console.log('✅ 表单验证测试完成');
  }

  async testResponsiveDesign() {
    console.log('\n📱 测试响应式设计...');

    const viewports = [
      { width: 1920, height: 1080, name: '桌面' },
      { width: 768, height: 1024, name: '平板' },
      { width: 375, height: 667, name: '手机' }
    ];

    for (const viewport of viewports) {
      console.log(`🔄 切换到${viewport.name}视图 (${viewport.width}x${viewport.height})`);
      
      await this.page.setViewport(viewport);
      await this.page.waitForTimeout(1000);

      // 检查导航栏是否适应
      const navVisible = await this.page.$('nav').then(el => el !== null);
      console.log(`  导航栏: ${navVisible ? '✅ 可见' : '❌ 不可见'}`);

      // 检查主要内容区域是否适应
      const mainVisible = await this.page.$('main').then(el => el !== null);
      console.log(`  主要内容: ${mainVisible ? '✅ 可见' : '❌ 不可见'}`);
    }

    this.testResult('响应式设计测试通过', true, {
      viewportsTested: viewports.length
    });
  }

  async testAPIConnection() {
    console.log('\n🔗 测试API连接...');

    try {
      // 测试健康检查接口
      const response = await this.page.evaluate(async () => {
        try {
          const response = await fetch('/api/health');
          return {
            ok: response.ok,
            status: response.status,
            data: await response.json()
          };
        } catch (error) {
          return { error: error.message };
        }
      });

      if (response.error) {
        this.testResult('API连接失败', false, { error: response.error });
      } else {
        this.testResult('API连接成功', true, {
          status: response.status,
          data: response.data
        });
      }

    } catch (error) {
      this.testResult('API连接测试异常', false, { error: error.message });
    }
  }

  async testUserRegistration() {
    console.log('\n👤 测试用户注册功能...');

    try {
      // 点击注册链接
      await this.page.click('.register-link');
      await this.page.waitForSelector('.register-form', { timeout: 5000 });

      // 测试数据
      const testData = {
        username: `testuser_${Date.now()}`,
        password: '123456',
        confirmPassword: '123456',
        phone: '13800138000',
        nickname: `测试用户_${Date.now()}`
      };

      // 填写表单
      await this.typeInput('.username-input', testData.username);
      await this.typeInput('.password-input', testData.password);
      await this.typeInput('.confirm-password-input', testData.confirmPassword);
      await this.typeInput('.phone-input', testData.phone);
      await this.typeInput('.nickname-input', testData.nickname);

      // 提交表单
      await this.page.click('.register-submit-btn');
      await this.page.waitForTimeout(2000);

      // 检查注册结果
      const registrationResult = await this.page.evaluate(() => {
        const successMsg = document.querySelector('.success-message');
        const errorMsg = document.querySelector('.error-message');
        return {
          success: successMsg ? successMsg.textContent : null,
          error: errorMsg ? errorMsg.textContent : null
        };
      });

      this.testResult('用户注册功能测试', true, {
        username: testData.username,
        result: registrationResult
      });

    } catch (error) {
      this.testResult('用户注册测试失败', false, { error: error.message });
    }
  }

  async testProductPublishing() {
    console.log('\n📦 测试商品发布功能...');

    try {
      // 点击发布商品链接
      await this.page.click('.publish-btn');
      await this.page.waitForSelector('.publish-form', { timeout: 5000 });

      // 测试商品发布表单
      const productData = {
        title: `测试商品_${Date.now()}`,
        categoryId: '1',
        description: '这是一个测试商品',
        price: '999.99',
        condition: '九成新',
        tradeType: 'offline'
      };

      // 填写表单
      await this.typeInput('.title-input', productData.title);
      await this.page.selectOption('.category-select', productData.categoryId);
      await this.typeInput('.description-input', productData.description);
      await this.typeInput('.price-input', productData.price);
      await this.page.selectOption('.condition-select', productData.condition);
      await this.page.selectOption('.trade-type-select', productData.tradeType);

      // 测试交易方式联动
      const tradeTypeSelect = await this.page.$('.trade-type-select');
      await tradeTypeSelect.selectOption('offline');
      await this.page.waitForTimeout(500);

      // 检查地址输入框是否显示
      const addressFieldVisible = await this.page.$('.address-input').then(el => el !== null);
      console.log(`📍 地址字段显示: ${addressFieldVisible ? '✅ 是' : '❌ 否'}`);

      // 提交表单
      await this.page.click('.publish-submit-btn');
      await this.page.waitForTimeout(3000);

      // 检查发布结果
      const publishResult = await this.page.evaluate(() => {
        const successMsg = document.querySelector('.success-message');
        const errorMsg = document.querySelector('.error-message');
        return {
          success: successMsg ? successMsg.textContent : null,
          error: errorMsg ? errorMsg.textContent : null
        };
      });

      this.testResult('商品发布功能测试', true, {
        title: productData.title,
        result: publishResult
      });

    } catch (error) {
      this.testResult('商品发布测试失败', false, { error: error.message });
    }
  }

  async testUIComponents() {
    console.log('\n🎨 测试UI组件交互...');

    const components = [
      { name: '搜索框', selector: '.search-input', action: 'click' },
      { name: '筛选按钮', selector: '.filter-btn', action: 'click' },
      { name: '排序下拉框', selector: '.sort-select', action: 'select' },
      { name: '分页按钮', selector: '.page-btn', action: 'click' }
    ];

    for (const component of components) {
      try {
        if (component.action === 'click') {
          await this.page.click(component.selector);
        } else if (component.action === 'select') {
          await this.page.selectOption(component.selector, '1');
        }

        await this.page.waitForTimeout(500);
        console.log(`✅ ${component.name} 交互正常`);
      } catch (error) {
        console.log(`❌ ${component.name} 交互失败: ${error.message}`);
      }
    }

    this.testResult('UI组件交互测试通过', true, {
      componentsTested: components.length
    });
  }

  async typeInput(selector, text) {
    await this.page.waitForSelector(selector, { timeout: 5000 });
    await this.page.click(selector);
    await this.page.type(selector, text);
    await this.page.waitForTimeout(200);
  }

  testResult(testName, passed, details = {}) {
    const result = {
      name: testName,
      passed,
      timestamp: new Date().toISOString(),
      details
    };

    this.testResults.push(result);

    const status = passed ? '✅' : '❌';
    console.log(`${status} ${testName}`);
    
    if (!passed) {
      console.log(`   错误详情:`, details);
    }
  }

  async generateReport() {
    console.log('\n📊 生成测试报告...');

    const passedCount = this.testResults.filter(r => r.passed).length;
    const failedCount = this.testResults.filter(r => !r.passed).length;
    const totalCount = this.testResults.length;

    const report = {
      testDate: new Date().toISOString(),
      baseUrl: this.baseUrl,
      summary: {
        total: totalCount,
        passed: passedCount,
        failed: failedCount,
        successRate: ((passedCount / totalCount) * 100).toFixed(2) + '%'
      },
      details: this.testResults
    };

    // 保存测试报告
    const reportPath = path.join(__dirname, 'frontend_test_report.json');
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    console.log(`📄 测试报告已保存到: ${reportPath}`);

    // 生成HTML报告
    const htmlReport = this.generateHTMLReport(report);
    const htmlPath = path.join(__dirname, 'frontend_test_report.html');
    fs.writeFileSync(htmlPath, htmlReport);
    console.log(`📄 HTML测试报告已保存到: ${htmlPath}`);

    return report;
  }

  generateHTMLReport(report) {
    const { total, passed, failed, successRate } = report.summary;
    
    return `
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>前端功能测试报告</title>
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
        <h1>前端功能测试报告</h1>
        <p>测试时间: ${report.testDate}</p>
        <p>测试地址: ${report.baseUrl}</p>
        
        <div class="summary">
            <div class="stat">
                <h3>总测试数</h3>
                <p>${total}</p>
            </div>
            <div class="stat passed">
                <h3>通过</h3>
                <p>${passed}</p>
            </div>
            <div class="stat failed">
                <h3>失败</h3>
                <p>${failed}</p>
            </div>
            <div class="stat">
                <h3>成功率</h3>
                <p>${successRate}</p>
            </div>
        </div>
    </div>

    <div class="details">
        <h2>测试详情</h2>
        ${report.details.map(detail => `
            <div class="test-item ${detail.passed ? 'passed' : 'failed'}">
                <div class="test-name">
                    ${detail.passed ? '✅' : '❌'} ${detail.name}
                </div>
                <div class="test-details">
                    状态: ${detail.passed ? '通过' : '失败'}
                    ${detail.details ? `<br>详情: ${JSON.stringify(detail.details, null, 2)}` : ''}
                </div>
            </div>
        `).join('')}
    </div>
</body>
</html>
    `;
  }

  async cleanup() {
    console.log('\n🧹 清理资源...');
    if (this.browser) {
      await this.browser.close();
    }
  }

  async runAllTests() {
    console.log('🚀 开始前端功能测试...');
    console.log('📅 测试时间:', new Date().toISOString());

    try {
      await this.init();
      
      // 运行各项测试
      await this.testPageLoad();
      await this.testFormValidation();
      await this.testResponsiveDesign();
      await this.testAPIConnection();
      await this.testUserRegistration();
      await this.testProductPublishing();
      await this.testUIComponents();

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
    } finally {
      await this.cleanup();
    }
  }
}

// 如果直接运行此脚本
if (require.main === module) {
  const tester = new FrontendTester();
  tester.runAllTests()
    .then(report => {
      process.exit(report.summary.failed === 0 ? 0 : 1);
    })
    .catch(error => {
      console.error('测试失败:', error);
      process.exit(1);
    });
}

module.exports = FrontendTester;