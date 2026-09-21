# 提交记录#005（Prompt#INIT004）

对应提交哈希：`f13a894`（fix: 修复项目启动异常 - Prompt#INIT004）

## 本轮目标

按项目规划第二阶段步骤4，定位并修复用户 IDEA 中的项目编译报错，完成启动验证收尾。

## 用户提供的报错现象（IDEA 构建日志）

```
java: 错误: 不支持发行版本 5
编译模块 'second-hand-market' 时发生错误
javac 22 用于编译 java 源
```

## 定位过程

1. **矛盾点识别**：命令行 Maven 构建（便携 Maven 3.9.16 + JDK 22.0.2，本会话多轮验证）始终
   BUILD SUCCESS，而 IDEA 报「不支持发行版本 5」→ 差异只可能出在 IDE 对编译级别的识别上
2. **配置核实**（.idea/misc.xml、.idea/compiler.xml、本地仓库父工程 pom）：
   - IDEA 项目级 SDK 为 JDK 22、项目语言级别 JDK_22，注解处理（Lombok）与 -parameters 正常
   - spring-boot-starter-parent 2.7.12 的编译级别是**通过属性**传递的
     （`maven.compiler.source/target = ${java.version}` = 1.8），maven-compiler-plugin 配置节中
     仅有 `<parameters>true</parameters>`，**无显式 source/target**
3. **根因**：Maven 命令行读取属性正常编译为 1.8；IDEA 的 Maven 导入器未能将父工程属性映射到
   模块语言级别，模块语言级别回落为 Maven 默认值 5 → javac 22 以 release 5 编译 →
   「不支持发行版本 5」（JDK 9+ 的 javac 已移除对发行版本 5 的支持）

## 修复方案

在 backend/pom.xml 的 build/plugins 中**显式声明** maven-compiler-plugin 的 source/target
（取值仍为 `${java.version}` 即 1.8）：
- 显式插件配置可被 IDEA 导入器稳定识别，导入后模块语言级别即正确设为 8
- 与父工程 pluginManagement 合并后 `-parameters` 等既有配置保持不变
- Maven 侧行为零变化（命令行本来就是 1.8）

## 验证情况

1. `mvn clean compile`：BUILD SUCCESS
2. 字节码核验：target/classes 产物 class 文件主版本号 = 52（Java 8），编译目标显式生效
3. 运行时兼容性旁证：本会话全部启动/冒烟测试均在 JDK 22.0.2 上执行通过
   （Boot 2.7.12 + target 1.8 + Lombok 1.18.36 组合在该 JDK 下编译、运行均正常）

## 用户侧补充操作（IDEA 使修复生效）

1. 更新代码后在 IDEA 的 Maven 面板点击「重新加载所有 Maven 项目」（Reload All Maven Projects），
   模块语言级别将被正确导入为 8
2. 若仍报错：File → Project Structure → Modules → second-hand-market → 语言级别改为 8；
   Settings → Build → Compiler → Java Compiler → 目标字节码版本改为 8

## 留痕说明

- 本记录为 过程记录第 005 篇，衔接 #004（47f648f，Prompt#INIT003）
- 至此项目骨架阶段（第二阶段步骤1~4）全部完成，后续业务功能在此基线上迭代
