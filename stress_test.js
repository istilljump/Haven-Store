/**
 * API压力测试脚本
 * 使用方法: node stress_test.js [options]
 * 
 * 选项:
 *   -t, --threads 并发线程数
 *   -r, --requests 每个线程请求数
 *   -u, --url 测试URL
 *   -m, --method HTTP方法 (GET/POST)
 *   -p, --payload JSON格式的请求体
 *   -h, --help 显示帮助信息
 */

const https = require('https');
const http = require('http');
const { argv } = require('process');
const fs = require('fs');

// 解析命令行参数
const parseArgs = () => {
  const args = {
    threads: 10,
    requests: 100,
    url: 'http://localhost:8080/api/health',
    method: 'GET',
    payload: null,
    headers: {
      'Content-Type': 'application/json'
    }
  };

  for (let i = 2; i < argv.length; i++) {
    const arg = argv[i];
    
    switch (arg) {
      case '-t':
      case '--threads':
        args.threads = parseInt(argv[++i]);
        break;
      case '-r':
      case '--requests':
        args.requests = parseInt(argv[++i]);
        break;
      case '-u':
      case '--url':
        args.url = argv[++i];
        break;
      case '-m':
      case '--method':
        args.method = argv[++i].toUpperCase();
        break;
      case '-p':
      case '--payload':
        try {
          args.payload = JSON.parse(argv[++i]);
        } catch (e) {
          console.error('Error parsing JSON payload:', e.message);
          process.exit(1);
        }
        break;
      case '-h':
      case '--help':
        printHelp();
        process.exit(0);
    }
  }

  return args;
};

// 打印帮助信息
const printHelp = () => {
  console.log(`
API压力测试工具
使用方法: node stress_test.js [options]

选项:
  -t, --threads    并发线程数 (默认: 10)
  -r, --requests   每个线程请求数 (默认: 100)
  -u, --url        测试URL (默认: http://localhost:8080/api/health)
  -m, --method     HTTP方法 (默认: GET)
  -p, --payload    JSON格式的请求体 (默认: null)
  -h, --help       显示帮助信息

示例:
  node stress_test.js -t 20 -r 50 -u http://localhost:8080/api/products -m GET
  node stress_test.js -t 5 -r 100 -u http://localhost:8080/api/auth/login -m POST -p '{"username":"admin","password":"123456"}'
`);
};

// 创建HTTP请求
const makeRequest = (url, method, payload, headers, results, index) => {
  const startTime = Date.now();
  const lib = url.startsWith('https') ? https : http;

  const options = {
    hostname: new URL(url).hostname,
    port: new URL(url).port || (url.startsWith('https') ? 443 : 80),
    path: new URL(url).pathname,
    method: method,
    headers: headers
  };

  const req = lib.request(options, (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
      const duration = Date.now() - startTime;
      results[index] = {
        status: res.statusCode,
        duration: duration,
        success: res.statusCode >= 200 && res.statusCode < 400
      };
    });
  });

  req.on('error', (error) => {
    const duration = Date.now() - startTime;
    results[index] = {
      status: 0,
      duration: duration,
      success: false,
      error: error.message
    };
  });

  if (payload && method !== 'GET' && method !== 'HEAD') {
    req.write(JSON.stringify(payload));
  }

  req.end();
};

// 运行压力测试
const runStressTest = (args) => {
  console.log('开始API压力测试...');
  console.log(`配置: ${args.threads} 线程 × ${args.requests} 请求 = ${args.threads * args.requests} 总请求`);
  console.log(`目标: ${args.method} ${args.url}`);
  
  const totalRequests = args.threads * args.requests;
  const results = new Array(totalRequests);
  let completedRequests = 0;
  let startTime = Date.now();

  // 记录响应时间统计
  const responseTimes = [];
  const statusCounts = {};
  const errorCounts = {};

  // 创建工作线程
  const workers = [];
  for (let i = 0; i < args.threads; i++) {
    workers.push({
      id: i,
      requests: args.requests,
      start: i * args.requests,
      end: (i + 1) * args.requests
    });
  }

  // 执行测试
  workers.forEach(worker => {
    for (let j = worker.start; j < worker.end; j++) {
      setTimeout(() => {
        makeRequest(args.url, args.method, args.payload, args.headers, results, j);
      }, 0);
    }
  });

  // 监控进度
  const progressInterval = setInterval(() => {
    const completed = results.filter(r => r !== undefined).length;
    const progress = (completed / totalRequests * 100).toFixed(1);
    process.stdout.write(`\r进度: ${progress}% (${completed}/${totalRequests})`);
    
    if (completed === totalRequests) {
      clearInterval(progressInterval);
      process.stdout.write('\n');
      analyzeResults(results);
    }
  }, 100);
};

// 分析测试结果
const analyzeResults = (results) => {
  const completed = results.filter(r => r !== undefined);
  const successCount = completed.filter(r => r.success).length;
  const failureCount = completed.length - successCount;
  
  const totalTime = completed.reduce((sum, r) => sum + r.duration, 0);
  const avgTime = totalTime / completed.length;
  
  const times = completed.map(r => r.duration).sort((a, b) => a - b);
  const medianTime = times[Math.floor(times.length / 2)];
  
  const minTime = times[0];
  const maxTime = times[times.length - 1];
  
  // 统计响应时间
  const timeRanges = {
    '0-100ms': 0,
    '100-500ms': 0,
    '500-1000ms': 0,
    '1000ms+': 0
  };
  
  completed.forEach(r => {
    if (r.duration < 100) timeRanges['0-100ms']++;
    else if (r.duration < 500) timeRanges['100-500ms']++;
    else if (r.duration < 1000) timeRanges['500-1000ms']++;
    else timeRanges['1000ms+']++;
  });
  
  // 统计状态码
  const statusCounts = {};
  completed.forEach(r => {
    if (statusCounts[r.status]) {
      statusCounts[r.status]++;
    } else {
      statusCounts[r.status] = 1;
    }
    
    if (!r.success && r.error) {
      if (!errorCounts[r.error]) {
        errorCounts[r.error] = 0;
      }
      errorCounts[r.error]++;
    }
  });
  
  // 打印结果
  console.log('\n=== 测试结果报告 ===');
  console.log(`总请求数: ${completed.length}`);
  console.log(`成功请求数: ${successCount}`);
  console.log(`失败请求数: ${failureCount}`);
  console.log(`成功率: ${((successCount / completed.length) * 100).toFixed(2)}%`);
  
  console.log('\n=== 响应时间统计 ===');
  console.log(`平均响应时间: ${avgTime.toFixed(2)} ms`);
  console.log(`最小响应时间: ${minTime} ms`);
  console.log(`最大响应时间: ${maxTime} ms`);
  console.log(`中位响应时间: ${medianTime} ms`);
  
  console.log('\n=== 响应时间分布 ===');
  Object.entries(timeRanges).forEach(([range, count]) => {
    const percentage = (count / completed.length * 100).toFixed(2);
    console.log(`${range}: ${count} 个请求 (${percentage}%)`);
  });
  
  console.log('\n=== 状态码统计 ===');
  Object.entries(statusCounts).forEach(([status, count]) => {
    const percentage = (count / completed.length * 100).toFixed(2);
    console.log(`HTTP ${status}: ${count} 个请求 (${percentage}%)`);
  });
  
  if (Object.keys(errorCounts).length > 0) {
    console.log('\n=== 错误统计 ===');
    Object.entries(errorCounts).forEach(([error, count]) => {
      console.log(`${error}: ${count} 次`);
    });
  }
  
  const testDuration = Date.now() - startTime;
  const requestsPerSecond = (completed.length / (testDuration / 1000)).toFixed(2);
  console.log(`\n测试总耗时: ${(testDuration / 1000).toFixed(2)} 秒`);
  console.log(`每秒请求数: ${requestsPerSecond} RPS`);
  
  console.log('\n=== 测试完成 ===');
  
  // 保存结果到文件
  const report = {
    timestamp: new Date().toISOString(),
    config: {
      threads: args.threads,
      requests: args.requests,
      url: args.url,
      method: args.method
    },
    results: {
      totalRequests: completed.length,
      successCount: successCount,
      failureCount: failureCount,
      successRate: (successCount / completed.length * 100).toFixed(2),
      avgResponseTime: avgTime.toFixed(2),
      minResponseTime: minTime,
      maxResponseTime: maxTime,
      medianResponseTime: medianTime,
      requestsPerSecond: requestsPerSecond,
      testDuration: (testDuration / 1000).toFixed(2)
    },
    responseTimeRanges: timeRanges,
    statusCodes: statusCounts,
    errors: errorCounts
  };
  
  fs.writeFileSync(`stress_test_report_${Date.now()}.json`, JSON.stringify(report, null, 2));
  console.log(`报告已保存到: stress_test_report_${Date.now()}.json`);
};

// 主程序
const main = () => {
  try {
    const args = parseArgs();
    runStressTest(args);
  } catch (error) {
    console.error('运行压力测试时发生错误:', error.message);
    process.exit(1);
  }
};

if (require.main === module) {
  main();
}

module.exports = { parseArgs, runStressTest, analyzeResults };