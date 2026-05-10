#!/usr/bin/env python3
"""Convert report.md → docs/SENG326-Task2-Report-Team13.pdf via WeasyPrint."""
from pathlib import Path

import markdown
from weasyprint import HTML, CSS

ROOT = Path(__file__).parent
SRC = ROOT / "report.md"
OUT = ROOT / "docs" / "SENG326-Task2-Report-Team13.pdf"

CSS_TEXT = """
@page { size: A4; margin: 2.0cm; @bottom-right { content: counter(page); font-size: 9pt; color: #555; } }
body { font-family: 'DejaVu Sans', 'Liberation Sans', Arial, sans-serif; font-size: 10.5pt; line-height: 1.4; color: #222; }
h1 { color: #0F4761; page-break-before: always; border-bottom: 2px solid #0F4761; padding-bottom: 4px; font-size: 22pt; }
h1:first-of-type { page-break-before: auto; }
h2 { color: #0F4761; font-size: 15pt; margin-top: 16pt; }
h3 { color: #0F4761; font-size: 12pt; }
h4 { font-size: 11pt; }
code { font-family: 'DejaVu Sans Mono', 'Liberation Mono', monospace; background: #f2f2f2; padding: 0 3px; border-radius: 2px; font-size: 9.5pt; }
pre { background: #f2f2f2; padding: 8px; border-radius: 3px; font-size: 9pt; line-height: 1.25; overflow-x: auto; }
pre code { background: none; padding: 0; }
table { border-collapse: collapse; width: 100%; margin: 6pt 0; font-size: 9.5pt; }
th { background: #D9E1F2; }
th, td { border: 1px solid #888; padding: 4px 6px; text-align: left; vertical-align: top; }
tr:nth-child(even) td { background: #EEF3FA; }
img { max-width: 100%; }
blockquote { border-left: 3px solid #0F4761; margin-left: 0; padding-left: 10px; color: #444; }
"""

md = SRC.read_text(encoding="utf-8")
html_body = markdown.markdown(
    md,
    extensions=["fenced_code", "tables", "toc", "sane_lists"],
)
html_doc = f"""<!doctype html><html><head><meta charset='utf-8'><title>SENG326 Task 2</title></head><body>{html_body}</body></html>"""

HTML(string=html_doc, base_url=str(ROOT)).write_pdf(
    str(OUT),
    stylesheets=[CSS(string=CSS_TEXT)],
)
print(f"wrote {OUT}")
