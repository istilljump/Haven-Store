#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
作品介绍文档 PDF 生成器

背景：项目交付要求提交 PDF 文档，但本机没有 LaTeX / wkhtmltopdf / weasyprint，
      因此用 reportlab 直接排版，并把 Markdown 当作文档的唯一内容源。

做什么：
  1. 读取 reports/Haven-Store作品介绍文档.md；
  2. 按 Markdown 子集（标题、段落、列表、引用、表格、代码块、图片）渲染为 PDF；
  3. 生成封面页、页脚页码，图片按正文宽度等比缩放并配图注。

用法：
  python scripts/build_intro_pdf.py
  python scripts/build_intro_pdf.py --src reports/xxx.md --out reports/xxx.pdf
"""

import argparse
import os
import re
import sys

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import cm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (BaseDocTemplate, Frame, Image, KeepTogether, PageBreak,
                                PageTemplate, Paragraph, Preformatted, Spacer, Table, TableStyle)

# 中文字体（Windows 自带）；正文用微软雅黑，等宽用 Consolas
FONT_REGULAR = "C:/Windows/Fonts/msyh.ttc"
FONT_BOLD = "C:/Windows/Fonts/msyhbd.ttc"
FONT_MONO = "C:/Windows/Fonts/consola.ttf"

INK = colors.HexColor("#1f2329")
GREY = colors.HexColor("#6b7280")
ACCENT = colors.HexColor("#2a5c8a")
RULE = colors.HexColor("#d7dbe2")
CODE_BG = colors.HexColor("#f5f6f8")

PAGE_W, PAGE_H = A4
MARGIN_X = 2.0 * cm
MARGIN_TOP = 2.0 * cm
MARGIN_BOTTOM = 1.9 * cm
CONTENT_W = PAGE_W - 2 * MARGIN_X


def register_fonts():
    for path in (FONT_REGULAR, FONT_BOLD, FONT_MONO):
        if not os.path.exists(path):
            print("缺少字体文件：%s" % path)
            sys.exit(2)
    pdfmetrics.registerFont(TTFont("CJK", FONT_REGULAR))
    pdfmetrics.registerFont(TTFont("CJK-Bold", FONT_BOLD))
    pdfmetrics.registerFont(TTFont("Mono", FONT_MONO))
    pdfmetrics.registerFontFamily("CJK", normal="CJK", bold="CJK-Bold",
                                  italic="CJK", boldItalic="CJK-Bold")


def make_styles():
    s = {}
    s["body"] = ParagraphStyle("body", fontName="CJK", fontSize=10.5, leading=17.5,
                               textColor=INK, alignment=TA_LEFT, spaceAfter=7, wordWrap="CJK")
    s["h1"] = ParagraphStyle("h1", fontName="CJK-Bold", fontSize=15.5, leading=22,
                             textColor=ACCENT, spaceBefore=6, spaceAfter=4,
                             wordWrap="CJK", keepWithNext=1)
    s["h2"] = ParagraphStyle("h2", fontName="CJK-Bold", fontSize=12.5, leading=19,
                             textColor=INK, spaceBefore=13, spaceAfter=6,
                             wordWrap="CJK", keepWithNext=1)
    s["h3"] = ParagraphStyle("h3", fontName="CJK-Bold", fontSize=11, leading=17,
                             textColor=INK, spaceBefore=10, spaceAfter=4,
                             wordWrap="CJK", keepWithNext=1)
    s["quote"] = ParagraphStyle("quote", parent=s["body"], fontSize=10, leading=17,
                                textColor=GREY, leftIndent=12, spaceAfter=8)
    s["bullet"] = ParagraphStyle("bullet", parent=s["body"], leftIndent=18, bulletIndent=4,
                                 spaceAfter=4, wordWrap="CJK")
    s["code"] = ParagraphStyle("code", fontName="Mono", fontSize=8.5, leading=13.5,
                               textColor=INK, leftIndent=10, spaceBefore=2, spaceAfter=2)
    s["caption"] = ParagraphStyle("caption", fontName="CJK", fontSize=9, leading=14,
                                  textColor=GREY, alignment=TA_CENTER, spaceBefore=3, spaceAfter=10)
    s["cell"] = ParagraphStyle("cell", fontName="CJK", fontSize=9, leading=14, textColor=INK,
                               wordWrap="CJK")
    s["cellh"] = ParagraphStyle("cellh", fontName="CJK-Bold", fontSize=9, leading=14, textColor=INK)
    return s


S = make_styles()


def _code_span(match):
    """行内代码：Consolas 没有中文字形，含中文的片段改用中文字体渲染，否则会显示为空白。"""
    text = match.group(1)
    if re.search(r"[\u3000-\u9fff\uff00-\uffef]", text):
        return '<font name="CJK" color="#2a5c8a">%s</font>' % text
    return '<font name="Mono" size="9.5">%s</font>' % text


def inline(text):
    """Markdown 行内标记 -> reportlab 迷你 HTML。"""
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", text)
    text = re.sub(r"(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)", r"<i>\1</i>", text)
    text = re.sub(r"`([^`\n]+)`", _code_span, text)
    return text


def split_row(line):
    return [c.strip() for c in line.strip().strip("|").split("|")]


def is_table_sep(line):
    return bool(re.match(r"^\|[\s:\-|]+\|$", line.strip()))


def build_table(rows):
    """把 Markdown 表格转成 reportlab Table，列宽按各列最长内容加权分配。"""
    ncol = max(len(r) for r in rows)
    rows = [r + [""] * (ncol - len(r)) for r in rows]
    weights = []
    for c in range(ncol):
        longest = max(len(r[c]) for r in rows)
        weights.append(max(6, min(longest, 34)))
    total = float(sum(weights))
    widths = [CONTENT_W * w / total for w in weights]
    data = []
    for i, row in enumerate(rows):
        style = S["cellh"] if i == 0 else S["cell"]
        data.append([Paragraph(inline(c), style) for c in row])
    table = Table(data, colWidths=widths, repeatRows=1, hAlign="LEFT")
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#eef1f5")),
        ("LINEBELOW", (0, 0), (-1, -1), 0.4, RULE),
        ("BOX", (0, 0), (-1, -1), 0.5, RULE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    return [Spacer(1, 2), table, Spacer(1, 10)]


def build_image(src, caption, base_dir):
    path = src if os.path.isabs(src) else os.path.join(base_dir, src)
    if not os.path.exists(path):
        return [Paragraph("[缺图：%s]" % src, S["body"])]
    from PIL import Image as PILImage
    w, h = PILImage.open(path).size
    # 界面截图统一压到「每页两张」的高度：既省页，也保证在 PDF 里放大后仍可辨认
    max_h = 10.4 * cm
    draw_w = CONTENT_W
    draw_h = draw_w * h / float(w)
    if draw_h > max_h:
        draw_h = max_h
        draw_w = draw_h * w / float(h)
    parts = [Spacer(1, 4), Image(path, width=draw_w, height=draw_h)]
    if caption:
        parts.append(Paragraph(inline(caption), S["caption"]))
    else:
        parts.append(Spacer(1, 8))
    return [KeepTogether(parts)]


def build_code(lines):
    body = Preformatted("\n".join(lines), S["code"])
    box = Table([[body]], colWidths=[CONTENT_W], hAlign="LEFT")
    box.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), CODE_BG),
        ("BOX", (0, 0), (-1, -1), 0.4, RULE),
        ("LEFTPADDING", (0, 0), (-1, -1), 8),
        ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    return [box, Spacer(1, 9)]


def render_markdown(md_text, base_dir):
    """按行扫描 Markdown，产出 platypus flowable 列表。"""
    flow = []
    lines = md_text.replace("\r\n", "\n").split("\n")
    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        if not stripped:
            i += 1
            continue

        # 一级标题：交给封面，正文里跳过
        if stripped.startswith("# ") and not stripped.startswith("## "):
            i += 1
            continue

        if stripped.startswith("```"):
            i += 1
            block = []
            while i < len(lines) and not lines[i].strip().startswith("```"):
                block.append(lines[i])
                i += 1
            i += 1
            flow += build_code(block)
            continue

        m = re.match(r"^!\[(.*?)\]\((.*?)\)\s*$", stripped)
        if m:
            flow += build_image(m.group(2), m.group(1), base_dir)
            i += 1
            continue

        if stripped.startswith("|") and is_table_sep(lines[i + 1] if i + 1 < len(lines) else ""):
            rows = [split_row(stripped)]
            i += 2
            while i < len(lines) and lines[i].strip().startswith("|"):
                rows.append(split_row(lines[i]))
                i += 1
            flow += build_table(rows)
            continue

        if stripped.startswith("#### "):
            flow.append(Paragraph(inline(stripped[5:]), S["h3"]))
            i += 1
            continue
        if stripped.startswith("### "):
            flow.append(Paragraph(inline(stripped[4:]), S["h2"]))
            i += 1
            continue
        if stripped.startswith("## "):
            flow.append(Spacer(1, 6))
            flow.append(Paragraph(inline(stripped[3:]), S["h1"]))
            flow.append(Table([[""]], colWidths=[CONTENT_W], rowHeights=[0.6],
                              style=TableStyle([("LINEABOVE", (0, 0), (-1, 0), 1.1, ACCENT)])))
            flow.append(Spacer(1, 8))
            i += 1
            continue

        if stripped.startswith("> "):
            flow.append(Paragraph(inline(stripped[2:]), S["quote"]))
            i += 1
            continue

        m = re.match(r"^[-*]\s+(.*)$", stripped)
        if m:
            flow.append(Paragraph(inline(m.group(1)), S["bullet"], bulletText="•"))
            i += 1
            continue

        m = re.match(r"^(\d+)\.\s+(.*)$", stripped)
        if m:
            flow.append(Paragraph(inline(m.group(2)), S["bullet"], bulletText=m.group(1) + "."))
            i += 1
            continue

        if stripped in ("---", "***", "___"):
            i += 1
            continue

        flow.append(Paragraph(inline(stripped), S["body"]))
        i += 1
    return flow


def cover_page(md_text):
    """从 Markdown 里提取标题与信息表，排成封面。"""
    lines = md_text.split("\n")
    title = "作品介绍文档"
    for line in lines:
        if line.startswith("# "):
            title = line[2:].strip()
            break
    info = []
    for idx, line in enumerate(lines):
        if line.strip().startswith("|"):
            j = idx
            while j < len(lines) and lines[j].strip().startswith("|"):
                if not is_table_sep(lines[j]):
                    cells = split_row(lines[j])
                    if len(cells) >= 2:
                        info.append(cells[:2])
                j += 1
            break
    flow = [Spacer(1, 4.6 * cm),
            Paragraph(title, ParagraphStyle("cover", fontName="CJK-Bold", fontSize=25,
                                            leading=36, textColor=INK, alignment=TA_CENTER)),
            Spacer(1, 0.45 * cm),
            Paragraph("Haven-Store 二手商品交易平台",
                      ParagraphStyle("cover2", fontName="CJK", fontSize=13, leading=22,
                                     textColor=ACCENT, alignment=TA_CENTER)),
            Spacer(1, 1.1 * cm)]
    for k, v in info[:4]:
        flow.append(Paragraph("<b>%s</b>　%s" % (inline(k), inline(v)),
                              ParagraphStyle("coverinfo", fontName="CJK", fontSize=10, leading=18,
                                             textColor=GREY, alignment=TA_CENTER, spaceAfter=2)))
    flow.append(Spacer(1, 0.4 * cm))
    flow.append(Paragraph("本文档由项目方编写，所有截图与数据均来自本机真实运行环境。",
                          ParagraphStyle("covernote", fontName="CJK", fontSize=9.5, leading=16,
                                         textColor=GREY, alignment=TA_CENTER)))
    flow.append(PageBreak())
    return flow


def decorate(canvas, doc):
    """页脚：左侧文档名，右侧页码。封面不加页脚。"""
    if doc.page == 1:
        return
    canvas.saveState()
    canvas.setStrokeColor(RULE)
    canvas.setLineWidth(0.5)
    canvas.line(MARGIN_X, MARGIN_BOTTOM - 0.35 * cm, PAGE_W - MARGIN_X, MARGIN_BOTTOM - 0.35 * cm)
    canvas.setFont("CJK", 8.5)
    canvas.setFillColor(GREY)
    canvas.drawString(MARGIN_X, MARGIN_BOTTOM - 0.75 * cm, "Haven-Store 二手商品交易平台 · 作品介绍文档 v1.0")
    canvas.drawRightString(PAGE_W - MARGIN_X, MARGIN_BOTTOM - 0.75 * cm, "第 %d 页" % (doc.page - 1))
    canvas.restoreState()


def main():
    parser = argparse.ArgumentParser(description="由 Markdown 生成作品介绍文档 PDF")
    parser.add_argument("--src", default="reports/Haven-Store作品介绍文档.md")
    parser.add_argument("--out", default="reports/Haven-Store作品介绍文档.pdf")
    args = parser.parse_args()

    if not os.path.exists(args.src):
        print("找不到源文件：%s" % args.src)
        sys.exit(2)

    with open(args.src, encoding="utf-8") as f:
        md_text = f.read()

    register_fonts()
    base_dir = os.path.dirname(os.path.abspath(args.src))

    doc = BaseDocTemplate(args.out, pagesize=A4,
                          leftMargin=MARGIN_X, rightMargin=MARGIN_X,
                          topMargin=MARGIN_TOP, bottomMargin=MARGIN_BOTTOM,
                          title="Haven-Store 作品介绍文档", author="Haven-Store")
    frame = Frame(MARGIN_X, MARGIN_BOTTOM, CONTENT_W, PAGE_H - MARGIN_TOP - MARGIN_BOTTOM, id="body")
    doc.addPageTemplates([PageTemplate(id="all", frames=[frame], onPage=decorate)])

    story = cover_page(md_text) + render_markdown(md_text, base_dir)
    doc.build(story)

    from pypdf import PdfReader
    pages = len(PdfReader(args.out).pages)
    print("已生成：%s（共 %d 页，含封面）" % (args.out, pages))


if __name__ == "__main__":
    main()
