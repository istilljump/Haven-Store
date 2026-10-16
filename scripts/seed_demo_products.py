#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Haven-Store 演示商品数据生成器

用途：为项目演示一次性灌入一批「贴近真实业务场景」的商品数据，
      让前台商品列表在品类、价格、成色、交易方式等维度上都足够丰富。

做什么：
  1. 用 Pillow 合成 1000x1000 的商品封面卡（版式对齐仓库自带的两张种子图：
     白底商品照 + 柔和投影 + 淡色水印）；
  2. 走真实接口上传图片（POST /product/upload）拿到访问地址；
  3. 走真实接口发布商品（POST /product/add），不直接写库，与前端同一条链路。

为什么图片是合成的：
  商品图不能引用第三方版权图，本机也没有可用的图床；因此用系统自带的彩色 emoji
  字体（seguiemj.ttf）绘制扁平化商品图标，观感与仓库自带种子图保持一致。
  合成图不含任何真实商品照片，仅用于演示商品列表的完整度。

用法：
  python scripts/seed_demo_products.py                       # 默认给 testuser1 灌 40 件商品
  python scripts/seed_demo_products.py --dry-run             # 只合成图片，不调接口
  python scripts/seed_demo_products.py --user testuser2 --password 123456
  python scripts/seed_demo_products.py --keep-images         # 保留本地合成的图片目录
"""

import argparse
import json
import os
import random
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request
import uuid

try:
    from PIL import Image, ImageDraw, ImageFilter, ImageFont
except ImportError:
    print("缺少依赖：请先 pip install pillow")
    sys.exit(2)

BASE = "http://localhost:8080/api"
EMOJI_FONT = "C:/Windows/Fonts/seguiemj.ttf"
TEXT_FONT = "C:/Windows/Fonts/msyh.ttc"

CARD_SIZE = 1000

# 本脚本的请求目标只有「本机自启的 Haven-Store 服务」，因此把主机限制为环回地址。
# 需要指向其它地址时，用环境变量 HAVEN_ALLOW_HOST 显式列白名单（逗号分隔）。
LOOPBACK_HOSTS = {"localhost", "127.0.0.1", "::1"}


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    """禁止跟随重定向：白名单校验的是原始主机，跟随跳转等于绕开这道校验。"""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise urllib.error.HTTPError(newurl, code, "拒绝重定向", headers, fp)


_OPENER = urllib.request.build_opener(_NoRedirect)


def safe_url(url):
    """请求目标白名单校验（SSRF 防护）：只允许 http/https + 已放行主机。"""
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme not in ("http", "https"):
        raise ValueError("仅允许 http/https 请求：" + url)
    host = parsed.hostname or ""
    extra = {h.strip() for h in os.environ.get("HAVEN_ALLOW_HOST", "").split(",") if h.strip()}
    if host not in LOOPBACK_HOSTS | extra:
        raise ValueError("拒绝访问白名单外的主机：%s（如需放行请设置 HAVEN_ALLOW_HOST）" % host)
    return url


# ============================================================
# 演示商品目录
# 字段：title 标题 / desc 描述 / icon 图标注 / price 售价 / original 原价 / cond 成色
#      trade 交易方式 / brand 品牌 / model 型号 / buy 购买时间 / features 特色
#      remark 备注 / place 线下交易地点(可选) / lng,lat 坐标(可选) / extra 附加图片数
# ============================================================
CATALOG = {
    "手机数码": [
        dict(icon="📱", title="iPhone 14 Pro 256G 深空黑", price=6488, original=8999, cond="九成新",
             brand="Apple", model="iPhone 14 Pro 256G", buy="2023-10-08",
             desc="自用一年，全程贴膜带壳，无拆修无进水，电池健康度 95%。原装数据线、包装盒齐全，支持当面验机。",
             features="国行,支持验机,无拆修记录", remark="可小刀，仅接受当面交易", extra=1),
        dict(icon="📱", title="华为 Mate 60 Pro 12+512G 雅川青", price=5799, original=6999, cond="九成新",
             brand="Huawei", model="Mate 60 Pro", buy="2023-12-20",
             desc="商务自用，屏幕无划痕，边框有极轻微使用痕迹。附赠原装 88W 充电器与两个原厂保护壳。",
             features="国行,碎屏险未过期,卫星消息", remark="充电器、包装全套", trade="线下",
             place="北京市海淀区中关村大街 1 号", lng=116.316833, lat=39.981013, extra=2),
        dict(icon="📱", title="iPad Air 5 64G WiFi 蓝色", price=2680, original=4399, cond="八成新",
             brand="Apple", model="iPad Air 5", buy="2022-08-11",
             desc="上网课用了一学期，屏幕无划痕，背面有轻微氧化点。可配二代 Apple Pencil 一起出。",
             features="国行,含包装盒", remark="学生党首选，可到手刀", extra=0),
        dict(icon="🎧", title="AirPods Pro 2 代 国行正品", price=1099, original=1899, cond="九成新",
             brand="Apple", model="AirPods Pro 2 (USB-C)", buy="2024-03-02",
             desc="换新耳机闲置，降噪正常，充电盒无磕碰，已做消毒清洁。序列号可查，非拆机件。",
             features="国行,可查序列号,已消毒", remark="当面验货", extra=1),
        dict(icon="⌚", title="Apple Watch SE 44mm 蜂窝版", price=1090, original=2199, cond="八成新",
             brand="Apple", model="Watch SE 44mm", buy="2022-11-25",
             desc="手表功能全部正常，表带换过一条新的，屏幕边缘有一道浅划痕（不影响使用）。",
             features="可插卡,含原装充电线", remark="划痕已拍照说明，介意勿拍", extra=0),
        dict(icon="📷", title="佳能 EOS 200D II 单反套机", price=3150, original=4699, cond="八成新",
             brand="Canon", model="EOS 200D II 18-55 STM", buy="2021-07-18",
             desc="入门单反，快门数约 8000 次，镜片干净无霉无雾。含原装电池两块、64G 卡与相机包。",
             features="含镜头,快门数低,配件齐全", remark="相机包、存储卡一并赠送", extra=2),
        dict(icon="🎧", title="索尼 WH-1000XM4 头戴降噪耳机", price=1150, original=2699, cond="七成新及以下",
             brand="Sony", model="WH-1000XM4", buy="2021-05-30",
             desc="头梁有轻微压痕，耳罩海绵是一年前更换的原厂件。降噪与续航正常，约 25 小时。",
             features="降噪正常,已换原厂耳罩", remark="成色一般，价格对应", extra=0),
        dict(icon="📱", title="小米 13 Ultra 16+512G 橄榄绿", price=3999, original=6499, cond="九成新",
             brand="Xiaomi", model="13 Ultra", buy="2023-09-14",
             desc="徕卡四摄，拍照主力机，无拆修。随机附赠原装 90W 充电器与官方皮质保护壳。",
             features="徕卡四摄,国行,无拆修", remark="支持当天自取", extra=1),
    ],
    "电脑办公": [
        dict(icon="💻", title="MacBook Air M2 13 寸 8+256G", price=5899, original=9499, cond="九成新",
             brand="Apple", model="MacBook Air M2 2022", buy="2023-04-16",
             desc="轻度办公使用，电池循环 78 次，键盘无油光，屏幕无亮点。附原装 30W 充电器。",
             features="电池循环低,国行,含原装电源", remark="可当面跑分验机", extra=1),
        dict(icon="💻", title="ThinkPad X1 Carbon 2021 i7 16+512", price=3580, original=12999, cond="八成新",
             brand="Lenovo", model="X1 Carbon Gen9", buy="2022-01-09",
             desc="工程机退役，C 面有贴纸残留痕迹，已清灰换硅脂。指纹、摄像头、背光键盘均正常。",
             features="i7 处理器,已清灰,商务办公", remark="原价过万，性价比之选", trade="线下",
             place="深圳市南山区科技园", lng=113.943588, lat=22.533267, extra=0),
        dict(icon="⌨", title="罗技 MX Keys 无线键盘 + 鼠标套装", price=520, original=1099, cond="九成新",
             brand="Logitech", model="MX Keys + M720", buy="2023-06-01",
             desc="多设备切换用了一段时间，按键手感依旧，键帽无磨损。含原装 Type-C 充电线。",
             features="多设备切换,可充电", remark="打包出，不单卖", extra=1),
        dict(icon="🖱", title="雷蛇 毒蝰 V2 Pro 无线鼠标", price=560, original=999, cond="八成新",
             brand="Razer", model="Viper V2 Pro", buy="2023-03-11",
             desc="59g 轻量化，适合 FPS。底部脚贴有使用痕迹，微动与滚轮一切正常。",
             features="轻量化,低延迟,可充电", remark="附赠一套原厂脚贴", extra=0),
        dict(icon="📺", title="AOC 27 寸 4K 显示器 U27N3C", price=980, original=1899, cond="八成新",
             brand="AOC", model="U27N3C", buy="2022-10-05",
             desc="设计用的 4K IPS 屏，无坏点无漏光，带全功能 Type-C 65W 反向供电。含原装支架。",
             features="4K IPS,Type-C 供电,无坏点", remark="同城可上门看屏", trade="线下",
             place="成都市武侯区天府三街", lng=104.066541, lat=30.572815, extra=1),
        dict(icon="🖨", title="惠普 136w 无线激光打印机", price=430, original=899, cond="八成新",
             brand="HP", model="LaserJet 136w", buy="2022-06-22",
             desc="在家办公打印用，打印张数约 1200 页，刚换过一支新硒鼓，支持手机直连打印。",
             features="无线打印,含新硒鼓", remark="硒鼓是上月新换的", extra=0),
    ],
    "图书教材": [
        dict(icon="📖", title="《JavaScript 高级程序设计》第 4 版", price=68, original=129, cond="九成新",
             brand="人民邮电出版社", model="红宝书 第4版", buy="2023-02-18",
             desc="前端入门经典，仅前两章有少量铅笔笔记，其余书页干净无折角。",
             features="无划线,少量笔记", remark="同系列其他书可一起看", extra=0),
        dict(icon="📚", title="考研数学复习全书套装（数一）", price=95, original=298, cond="八成新",
             brand="李永乐", model="基础篇+强化篇", buy="2024-01-05",
             desc="去年考研用书，笔记做在便利贴上，可整本保留也可以撕掉，知识点标注很全。",
             features="含笔记,成套不拆", remark="上岸后低价转让，祝学弟学妹一战成硕", extra=2),
        dict(icon="📚", title="《明朝那些事儿》全 9 册", price=120, original=358, cond="九成新",
             brand="磨铁图书", model="全 9 册", buy="2022-05-20",
             desc="通读一遍，无笔记无划线，书脊无折痕，书页干燥无黄斑。整套出不单卖。",
             features="整套 9 册,保存完好", remark="搬运不便，仅限同城自提", trade="线下",
             place="北京市朝阳区国贸 CBD", lng=116.466240, lat=39.920800, extra=1),
        dict(icon="📖", title="注册会计师 6 科教材 + 真题集", price=180, original=680, cond="八成新",
             brand="中国财政经济出版社", model="2024 版全套", buy="2024-03-12",
             desc="备考一年用书，真题册有大量笔记（这恰恰是价值所在），教材本体干净。",
             features="含真题,笔记完整", remark="适合今年备考的同学", extra=0),
        dict(icon="📖", title="考研英语真题全套（2010-2024）", price=55, original=198, cond="八成新",
             brand="考研真相", model="英语一 真题册", buy="2024-02-28",
             desc="真题册已做完一遍，答案页有批注；另附赠一本自己整理的作文模板手写本。",
             features="含批注,赠作文模板", remark="赠品为本子，介意勿拍", extra=0),
    ],
    "家用电器": [
        dict(icon="🧹", title="戴森 V10 无线吸尘器", price=1180, original=3299, cond="八成新",
             brand="Dyson", model="V10 Fluffy", buy="2021-11-11",
             desc="换房后闲置，电池健康，标准档续航约 30 分钟。滤网已清洗，吸头齐全。",
             features="原装全套吸头,续航正常", remark="可当场试机", trade="线下",
             place="北京市朝阳区望京 SOHO", lng=116.480471, lat=39.998122, extra=2),
        dict(icon="💡", title="小米空气净化器 4 Pro", price=520, original=1099, cond="九成新",
             brand="Xiaomi", model="空气净化器 4 Pro", buy="2023-01-08",
             desc="搬家后不用了，滤芯三个月前刚换新（有购买记录），App 联网与空气质量检测正常。",
             features="滤芯新换,支持米家,含购买记录", remark="滤芯是上月新换的", extra=1),
        dict(icon="🍳", title="九阳破壁机 L18-P521", price=320, original=899, cond="八成新",
             brand="Joyoung", model="L18-P521", buy="2022-09-19",
             desc="早餐打豆浆、做辅食用过一段时间，刀头无锈，杯体无裂缝，功能全部正常。",
             features="功能正常,已清洗", remark="附赠原装食谱册", extra=0),
        dict(icon="🍚", title="美的 4L 智能电饭煲", price=140, original=429, cond="八成新",
             brand="Midea", model="MB-FB40", buy="2022-04-02",
             desc="宿舍升级闲置，内胆涂层完好无脱落，预约、保温功能正常。含原装蒸格与量杯。",
             features="涂层完好,含蒸格", remark="宿舍党友好", trade="线下",
             place="上海市浦东新区张江高科", lng=121.599281, lat=31.208259, extra=0),
        dict(icon="🚿", title="海尔 60L 速热电热水器", price=380, original=1099, cond="七成新及以下",
             brand="Haier", model="EC6002-JT3", buy="2020-10-16",
             desc="租房自用后拆下来的，加热正常，外壳有使用痕迹，镁棒去年更换过。仅同城自提。",
             features="加热正常,已换镁棒", remark="大件仅限自提，可帮搬下楼", trade="线下",
             place="广州市天河区体育西路", lng=113.322151, lat=23.135593, extra=0),
    ],
    "服饰鞋包": [
        dict(icon="👟", title="Nike Air Force 1 白色 42 码", price=380, original=899, cond="八成新",
             brand="Nike", model="Air Force 1 Low", buy="2023-04-09",
             desc="上脚不到十次，鞋底磨损极轻微，鞋面已专业清洗，鞋盒略有压痕。",
             features="已清洗,鞋盒在,正品", remark="鞋码 42，脚瘦可选 41.5", extra=1),
        dict(icon="👖", title="Lululemon Align 瑜伽裤 6 码", price=280, original=850, cond="九成新",
             brand="Lululemon", model="Align 25 英寸", buy="2023-08-15",
             desc="买大了一码，只试穿过两次，无起球无勾丝，吊牌已剪但保留。",
             features="仅试穿,无起球", remark="尺码不合才转，非质量问题", extra=0),
        dict(icon="👜", title="Coach 单肩托特包 深棕", price=680, original=2380, cond="八成新",
             brand="Coach", model="Willow Tote", buy="2022-03-18",
             desc="通勤用过一季，边角有轻微磨白，五金无氧化，内衬干净。含原装防尘袋。",
             features="正品,含防尘袋,五金完好", remark="可提供专柜购买小票照片", extra=2),
        dict(icon="🧥", title="优衣库 轻型羽绒服 L 码 藏青", price=180, original=599, cond="九成新",
             brand="Uniqlo", model="高级轻型羽绒 2023", buy="2023-11-20",
             desc="去年冬天买的，穿了不到十次，无破损无跑绒，已干洗收纳。",
             features="已干洗,无跑绒", remark="含收纳袋", extra=0),
        dict(icon="🥾", title="全新马丁靴 42 码 黑色", price=420, original=1299, cond="全新",
             brand="Dr. Martens", model="1460 八孔", buy="2024-09-01",
             desc="生日礼物买错尺码，仅试穿一次，鞋底无磨损，原盒、吊牌、防尘袋齐全。",
             features="全新未穿,吊牌齐全", remark="可当面试穿", trade="线下",
             place="杭州市西湖区文三路", lng=120.130710, lat=30.279765, extra=1),
    ],
    "运动户外": [
        dict(icon="🚲", title="捷安特 ATX 660 山地车 27.5 寸", price=1180, original=2399, cond="八成新",
             brand="Giant", model="ATX 660", buy="2022-07-30",
             desc="通勤骑了两年，上周刚做保养（换刹车皮、调变速），车架无裂无缝，车锁一并送。",
             features="刚做保养,含车锁,变速顺畅", remark="仅同城自提，可试骑", trade="线下",
             place="北京市海淀区五道口", lng=116.338100, lat=39.992900, extra=2),
        dict(icon="⛺", title="迪卡侬 3-4 人自动帐篷", price=260, original=699, cond="八成新",
             brand="Decathlon", model="QUECHUA 2 秒速开", buy="2023-05-02",
             desc="露营用过去年一季，防水涂层完好，骨架无变形，含原装地钉与收纳袋。",
             features="防雨,速开,含地钉", remark="已清洗晾干收纳", extra=0),
        dict(icon="🏸", title="尤尼克斯 羽毛球拍 双拍套", price=420, original=1080, cond="九成新",
             brand="YONEX", model="NF-700 + 弓箭 8", buy="2023-09-24",
             desc="双打搭档换拍，两只一起出，已重新缠手胶，拍框无磕碰。含拍包。",
             features="双拍套装,已换手胶", remark="含拍包，不单卖", extra=1),
        dict(icon="💪", title="家用可调哑铃套装 20kg", price=260, original=699, cond="八成新",
             brand="英尔健", model="20kg 可调", buy="2023-02-14",
             desc="包胶片，无掉渣无锈，配连接杆可变杠铃。搬家不带走了，仅限同城自提。",
             features="可调重量,可做杠铃", remark="重量大，仅自提", trade="线下",
             place="上海市徐汇区漕河泾", lng=121.402800, lat=31.170100, extra=1),
        dict(icon="⌚", title="佳明 Forerunner 255 运动手表", price=1290, original=2380, cond="九成新",
             brand="Garmin", model="Forerunner 255", buy="2023-10-11",
             desc="跑步记录 800 公里，表盘无划痕，心率与 GPS 精准，附原装充电线。",
             features="双频 GPS,血氧监测,含充电线", remark="表带另送一条织物款", extra=0),
    ],
    "美妆个护": [
        dict(icon="🧴", title="雅诗兰黛小棕瓶 50ml 未拆封", price=480, original=1050, cond="全新",
             brand="Estée Lauder", model="特润修护精华 50ml", buy="2024-09-20",
             desc="专柜活动多买了一套，全新未拆封，批号可查，保质期到 2027 年。",
             features="全新未拆,正品批号可查", remark="不退不换，可当面验封口", extra=1),
        dict(icon="🧴", title="SK-II 神仙水 230ml", price=980, original=1540, cond="全新",
             brand="SK-II", model="护肤精华露 230ml", buy="2024-08-15",
             desc="免税店购入，包装完好未开封，附小票照片。可到专柜验货后交易。",
             features="全新,含小票,可专柜验货", remark="贵重物品，仅限当面交易", trade="线下",
             place="南京市鼓楼区新街口", lng=118.778600, lat=32.042400, extra=0),
        dict(icon="💄", title="兰蔻菁纯口红 196 号", price=180, original=320, cond="九成新",
             brand="Lancôme", model="菁纯唇膏 196", buy="2024-05-06",
             desc="色号不合适，只涂过两次，已用酒精棉片擦拭消毒，原盒在。",
             features="仅用过 2 次,已消毒,原盒在", remark="色号 196 正红", extra=0),
        dict(icon="🪥", title="飞利浦电动牙刷 HX6730 双支装", price=260, original=699, cond="八成新",
             brand="Philips", model="HX6730", buy="2023-01-30",
             desc="情侣款两支一起出，机身无进水，刷头均为全新未拆封，含原装旅行盒。",
             features="双支装,刷头全新", remark="刷头未拆封，机身已消毒", extra=1),
        dict(icon="🧼", title="海蓝之谜精华面霜 60ml 正装", price=520, original=1280, cond="全新",
             brand="La Mer", model="精华面霜 60ml", buy="2024-07-22",
             desc="套装拆出的正装，未开封，附赠一瓶 15ml 旅行装小样，塑封完整。",
             features="全新未开,赠 15ml 小样", remark="包装塑封完整，可拍照核对", extra=0),
    ],
    "其他闲置": [
        dict(icon="🐱", title="实木猫爬架 1.6 米高", price=180, original=559, cond="八成新",
             brand="未卡", model="1.6m 多层猫爬架", buy="2022-12-08",
             desc="猫主子长胖后爬不动了，剑麻柱无散绳，木板已擦拭消毒，仅同城自提。",
             features="已消毒,剑麻未散,可拆卸", remark="体积较大，需自提", trade="线下",
             place="成都市锦江区春熙路", lng=104.081400, lat=30.657400, extra=1),
        dict(icon="🛹", title="小米电动滑板车 3 代", price=880, original=2499, cond="八成新",
             brand="Xiaomi", model="Electric Scooter 3", buy="2023-03-19",
             desc="通勤最后两公里用，电池健康 92%，轮胎刚换新，折叠机构正常。含原装充电器。",
             features="电池健康 92%,轮胎新换,可折叠", remark="仅限同城，可试骑", trade="线下",
             place="深圳市福田区车公庙", lng=114.028100, lat=22.539400, extra=1),
        dict(icon="🎹", title="雅马哈电子琴 PSR-E373 61 键", price=780, original=1580, cond="九成新",
             brand="Yamaha", model="PSR-E373", buy="2023-06-25",
             desc="孩子的第一台琴，考级后换了钢琴。琴键无卡顿，配原装琴架与延音踏板。",
             features="含琴架,延音踏板,61 键", remark="琴架、踏板一起出", extra=1),
        dict(icon="🧩", title="乐高 星球大战 千年隼 75257", price=560, original=1299, cond="九成新",
             brand="LEGO", model="75257 千年隼", buy="2023-01-15",
             desc="拼装完成后陈列过，零件完整无缺失（说明书与备用件都在），已按编号拆回分袋。",
             features="零件齐全,含说明书,已分袋", remark="可视频核对零件袋", extra=2),
        dict(icon="☕", title="星巴克联名保温杯 355ml 全新", price=95, original=269, cond="全新",
             brand="Starbucks", model="圣诞限定保温杯", buy="2024-10-05",
             desc="年会抽奖得到的，全新未使用，杯盖密封圈完好，附原装礼盒。",
             features="全新未用,含礼盒", remark="礼盒有轻微压痕", extra=0),
    ],
}


def load_fonts():
    """加载绘图所需字体，缺任何一个都直接退出并说明缺哪个。"""
    for path in (EMOJI_FONT, TEXT_FONT):
        if not os.path.exists(path):
            print("找不到字体文件：%s" % path)
            sys.exit(2)
    return {
        "emoji": ImageFont.truetype(EMOJI_FONT, 380),
        "sub": ImageFont.truetype(TEXT_FONT, 36),
    }


_EMOJI_CACHE = {}


def emoji_font(size):
    if size not in _EMOJI_CACHE:
        _EMOJI_CACHE[size] = ImageFont.truetype(EMOJI_FONT, size)
    return _EMOJI_CACHE[size]


def draw_emoji_centered(draw, icon, size, center_y):
    """把 emoji 图标按其真实包围盒居中绘制（不同 emoji 的留白不同，必须实测）。"""
    font = emoji_font(size)
    bbox = draw.textbbox((0, 0), icon, font=font, embedded_color=True)
    w, h = bbox[2] - bbox[0], bbox[3] - bbox[1]
    draw.text((CARD_SIZE // 2 - w // 2 - bbox[0], center_y - h // 2 - bbox[1]),
              icon, font=font, embedded_color=True)


def render_cover(path, item, variant=0):
    """合成一张 1000x1000 的商品图。

    刻意做成「白底商品照」而不是海报：商品列表卡片本身已经会渲染标题、成色、
    价格，图上再写一遍会让列表出现重复文案；所以图里只留商品本体、柔和投影与
    一枚淡色水印，观感与真实卖家上传的白底实拍图一致。
    variant > 0 时生成「细节图/配件图/包装图」，用于演示商品多图相册。
    """
    fonts = FONTS
    img = Image.new("RGB", (CARD_SIZE, CARD_SIZE), (255, 255, 255))

    # 底部极淡的影棚灰，避免纯白死板
    vignette = Image.new("L", (1, CARD_SIZE))
    for y in range(CARD_SIZE):
        vignette.putpixel((0, y), int(7 * (y / CARD_SIZE) ** 2))
    img = Image.composite(Image.new("RGB", (CARD_SIZE, CARD_SIZE), (233, 235, 240)),
                          img, vignette.resize((CARD_SIZE, CARD_SIZE)))

    # 图标下方的柔和投影
    shadow = Image.new("L", (CARD_SIZE, CARD_SIZE), 0)
    ImageDraw.Draw(shadow).ellipse((CARD_SIZE // 2 - 200, 700, CARD_SIZE // 2 + 200, 756), fill=64)
    shadow = shadow.filter(ImageFilter.GaussianBlur(26))
    img = Image.composite(Image.new("RGB", (CARD_SIZE, CARD_SIZE), (152, 155, 162)), img, shadow)

    layer = Image.new("RGBA", (CARD_SIZE, CARD_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    if variant == 0:
        draw_emoji_centered(draw, item["icon"], 420, 430)
    else:
        draw_emoji_centered(draw, item["icon"], 300, 400)
        tag = ["细节图", "配件图", "包装图"][(variant - 1) % 3]
        tw = draw.textlength(tag, font=fonts["sub"])
        draw.rounded_rectangle((CARD_SIZE // 2 - tw // 2 - 26, 620, CARD_SIZE // 2 + tw // 2 + 26, 682),
                               radius=31, fill=(235, 238, 243, 255))
        draw.text((CARD_SIZE // 2 - tw // 2, 626), tag, font=fonts["sub"], fill=(120, 126, 136, 255))

    # 右下角淡色水印：标明是演示数据而非真实商品照片
    mark = "Haven-Store 演示图"
    mw = draw.textlength(mark, font=fonts["sub"])
    draw.text((CARD_SIZE - 54 - mw, CARD_SIZE - 84), mark, font=fonts["sub"], fill=(203, 206, 213, 255))

    img = Image.alpha_composite(img.convert("RGBA"), layer).convert("RGB")
    img.save(path, "PNG", optimize=True)
    return path


# ============================================================
# HTTP 部分
# ============================================================
def request(method, path, body=None, token=None, raw_body=None, content_type=None):
    url = safe_url(BASE + urllib.parse.quote(path, safe="/?&=%"))
    data = raw_body
    headers = {}
    if raw_body is None and body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json; charset=utf-8"
    elif content_type:
        headers["Content-Type"] = content_type
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with _OPENER.open(req, timeout=30) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        text = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(text)
        except json.JSONDecodeError:
            return e.code, {"msg": text[:200]}


def login(username, password):
    status, payload = request("POST", "/user/login", {"username": username, "password": password})
    if payload.get("code") != 200:
        print("登录失败：%s" % json.dumps(payload, ensure_ascii=False))
        sys.exit(1)
    data = payload["data"]
    print("已登录：%s（%s）" % (username, data.get("nickname")))
    return data["token"], data.get("nickname") or username


def upload_image(path, token):
    """multipart 请求体手工拼装（urllib 没有内置的 multipart 编码器）。"""
    with open(path, "rb") as f:
        content = f.read()
    boundary = "----havenboundary" + uuid.uuid4().hex
    filename = os.path.basename(path)
    body = (
        ("--" + boundary + "\r\n"
         'Content-Disposition: form-data; name="file"; filename="%s"\r\n'
         "Content-Type: image/png\r\n\r\n" % filename).encode("utf-8")
        + content
        + ("\r\n--" + boundary + "--\r\n").encode("utf-8")
    )
    status, payload = request("POST", "/product/upload", token=token, raw_body=body,
                              content_type="multipart/form-data; boundary=" + boundary)
    if payload.get("code") != 200:
        raise RuntimeError("上传失败 %s：%s" % (filename, json.dumps(payload, ensure_ascii=False)))
    return payload["data"]


def publish(item, image_urls, seller_name, token):
    body = {
        "title": item["title"],
        "description": item["desc"],
        "categoryId": item["category_id"],
        "price": item["price"],
        "originalPrice": item["original"],
        "productCondition": item["cond"],
        "tradeType": item.get("trade", "线上"),
        "brand": item.get("brand"),
        "model": item.get("model"),
        "purchaseTime": item.get("buy"),
        "features": item.get("features"),
        "remark": item.get("remark"),
        "contactName": seller_name,
        "images": image_urls,
    }
    if body["tradeType"] == "线下":
        body["address"] = item["place"]
        body["longitude"] = item["lng"]
        body["latitude"] = item["lat"]
    status, payload = request("POST", "/product/add", body, token)
    if payload.get("code") != 200:
        raise RuntimeError("发布失败《%s》：%s" % (item["title"], json.dumps(payload, ensure_ascii=False)))
    return payload["data"].get("productId")


def main():
    global BASE, FONTS

    parser = argparse.ArgumentParser(description="生成 Haven-Store 演示商品数据")
    parser.add_argument("--base", default=os.environ.get("HAVEN_BASE", BASE))
    parser.add_argument("--user", default="testuser1")
    parser.add_argument("--password", default="123456")
    parser.add_argument("--dry-run", action="store_true", help="只合成图片，不调接口")
    parser.add_argument("--keep-images", action="store_true", help="保留合成图片目录")
    parser.add_argument("--seed", type=int, default=20261016, help="随机种子，保证可复现")
    args = parser.parse_args()

    BASE = args.base
    random.seed(args.seed)
    FONTS = load_fonts()

    total = sum(len(v) for v in CATALOG.values())

    if args.dry_run:
        tmp_dir = os.path.join(os.getcwd(), "demo-images")
        os.makedirs(tmp_dir, exist_ok=True)
        n = 0
        for items in CATALOG.values():
            for item in items:
                n += 1
                render_cover(os.path.join(tmp_dir, "sample-%02d.png" % n), item)
        print("已合成 %d 张示例图：%s" % (total, tmp_dir))
        return

    tmp_dir = tempfile.mkdtemp(prefix="haven_demo_")
    print("合成图片临时目录：%s" % tmp_dir)

    _, cats = request("GET", "/product/categories")
    name_to_id = {c["name"]: c["id"] for c in (cats.get("data") or [])}
    missing = [n for n in CATALOG if n not in name_to_id]
    if missing:
        print("以下分类在当前库中不存在，无法发布：%s" % "、".join(missing))
        print("请先在管理后台补齐分类后重试。")
        sys.exit(1)

    token, nickname = login(args.user, args.password)
    ok, failed = 0, []
    for cat_name, items in CATALOG.items():
        print("\n【%s】" % cat_name)
        for item in items:
            item["category_id"] = name_to_id[cat_name]
            paths = []
            for v in range(1 + item.get("extra", 0)):
                p = os.path.join(tmp_dir, "%s_%d.png" % (uuid.uuid4().hex[:8], v))
                render_cover(p, item, variant=v)
                paths.append(p)
            try:
                urls = [upload_image(p, token) for p in paths]
                pid = publish(item, urls, nickname, token)
                ok += 1
                print("  [OK]   #%s %s（%d 张图）" % (pid, item["title"], len(urls)))
            except Exception as exc:  # 单件失败不阻断整批，最后统一汇报
                failed.append((item["title"], str(exc)))
                print("  [失败] %s -> %s" % (item["title"], exc))
            finally:
                if not args.keep_images:
                    for p in paths:
                        if os.path.exists(p):
                            os.remove(p)

    print("\n完成：成功 %d / %d 件" % (ok, total))
    if failed:
        print("失败明细：")
        for title, err in failed:
            print("  - %s：%s" % (title, err))
    if args.keep_images:
        print("图片目录保留在：%s" % tmp_dir)
    else:
        try:
            os.rmdir(tmp_dir)
        except OSError:
            pass


FONTS = None

if __name__ == "__main__":
    main()
