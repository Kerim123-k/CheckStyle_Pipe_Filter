#!/usr/bin/env python3
"""Render report.md to docs/SENG326-Task2-Report-Team13.docx via htmldocx."""
from pathlib import Path

import markdown
from docx import Document
from docx.shared import Pt, RGBColor, Cm, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH
from htmldocx import HtmlToDocx

ROOT = Path(__file__).parent
SRC = ROOT / "report.md"
OUT = ROOT / "docs" / "SENG326-Task2-Report-Team13.docx"

md_text = SRC.read_text(encoding="utf-8")
html = markdown.markdown(
    md_text,
    extensions=["fenced_code", "tables", "toc", "sane_lists"],
)

doc = Document()
for section in doc.sections:
    section.top_margin = Cm(2.5)
    section.bottom_margin = Cm(2.5)
    section.left_margin = Cm(2.5)
    section.right_margin = Cm(2.5)

styles = doc.styles
normal = styles["Normal"]
normal.font.name = "Calibri"
normal.font.size = Pt(11)

for level, size in [(1, 22), (2, 16), (3, 13), (4, 12)]:
    s = styles[f"Heading {level}"]
    s.font.name = "Calibri"
    s.font.size = Pt(size)
    s.font.bold = True
    s.font.color.rgb = RGBColor(0x0F, 0x47, 0x61)

converter = HtmlToDocx()
converter.table_style = "Light Grid Accent 1"
converter.add_html_to_document(html, doc)

doc.save(str(OUT))
print(f"wrote {OUT}")
