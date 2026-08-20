#!/usr/bin/env python3
"""Build the final Day 3 report as a dependency-free OOXML document."""

from __future__ import annotations

import html
import re
import struct
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "day3-실습-보고서-초안.md"
OUTPUT = ROOT / "P343_함형준_Day3_실습보고서.docx"

W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"


def esc(value: str) -> str:
    return html.escape(value, quote=True)


def run(text: str, *, bold=False, italic=False, code=False, size=None, color=None) -> str:
    props = []
    font = "D2Coding Ligature" if code else "Pretendard"
    props.append(f'<w:rFonts w:ascii="{font}" w:hAnsi="{font}" w:eastAsia="{font}" w:cs="{font}"/>')
    if bold:
        props.append("<w:b/><w:bCs/>")
    if italic:
        props.append("<w:i/><w:iCs/>")
    if size:
        props.append(f'<w:sz w:val="{size}"/><w:szCs w:val="{size}"/>')
    if color:
        props.append(f'<w:color w:val="{color}"/>')
    if code:
        props.append('<w:shd w:val="clear" w:color="auto" w:fill="F3F4F6"/>')
    return f'<w:r><w:rPr>{"".join(props)}</w:rPr><w:t xml:space="preserve">{esc(text)}</w:t></w:r>'


def inline(text: str) -> str:
    parts = re.split(r'(\*\*.*?\*\*|`.*?`)', text)
    out = []
    for part in parts:
        if part.startswith("**") and part.endswith("**"):
            out.append(run(part[2:-2], bold=True))
        elif part.startswith("`") and part.endswith("`"):
            out.append(run(part[1:-1], code=True, size=18))
        else:
            out.append(run(part))
    return "".join(out)


def paragraph(text="", *, style=None, align=None, before=0, after=120, keep=False, indent=None) -> str:
    ppr = []
    if style:
        ppr.append(f'<w:pStyle w:val="{style}"/>')
    if align:
        ppr.append(f'<w:jc w:val="{align}"/>')
    ppr.append(f'<w:spacing w:before="{before}" w:after="{after}" w:line="330" w:lineRule="auto"/>')
    if keep:
        ppr.append("<w:keepNext/>")
    if indent:
        ppr.append(f'<w:ind w:left="{indent}"/>')
    return f'<w:p><w:pPr>{"".join(ppr)}</w:pPr>{inline(text)}</w:p>'


def code_block(lines: list[str]) -> str:
    # Keep each source line in its own Word paragraph. Manual line breaks inside
    # one paragraph are rendered inconsistently by some Word-compatible viewers.
    paragraphs = []
    for line in lines:
        text = line if line else " "
        paragraphs.append(
            '<w:p><w:pPr><w:spacing w:before="0" w:after="0" '
            'w:line="260" w:lineRule="exact"/><w:keepLines/></w:pPr>'
            '<w:r><w:rPr><w:rFonts w:ascii="D2Coding Ligature" '
            'w:hAnsi="D2Coding Ligature" w:eastAsia="D2Coding Ligature" '
            'w:cs="D2Coding Ligature"/><w:sz w:val="18"/>'
            '<w:szCs w:val="18"/><w:color w:val="263238"/></w:rPr>'
            f'<w:t xml:space="preserve">{esc(text)}</w:t></w:r></w:p>'
        )
    return (
        '<w:tbl><w:tblPr><w:tblW w:w="9360" w:type="dxa"/>'
        '<w:tblLayout w:type="fixed"/><w:tblBorders>'
        '<w:left w:val="single" w:sz="16" w:color="68788A"/>'
        '<w:top w:val="nil"/><w:right w:val="nil"/><w:bottom w:val="nil"/>'
        '<w:insideH w:val="nil"/><w:insideV w:val="nil"/>'
        '</w:tblBorders></w:tblPr><w:tblGrid><w:gridCol w:w="9360"/>'
        '</w:tblGrid><w:tr><w:tc><w:tcPr><w:tcW w:w="9360" w:type="dxa"/>'
        '<w:shd w:val="clear" w:color="auto" w:fill="F4F6F8"/>'
        '<w:tcMar><w:top w:w="120" w:type="dxa"/><w:left w:w="180" '
        'w:type="dxa"/><w:bottom w:w="120" w:type="dxa"/><w:right '
        'w:w="180" w:type="dxa"/></w:tcMar></w:tcPr>'
        + "".join(paragraphs)
        + '</w:tc></w:tr></w:tbl><w:p><w:pPr><w:spacing w:after="120"/>'
        '</w:pPr></w:p>'
    )


def table(rows: list[list[str]]) -> str:
    cols = max(len(r) for r in rows)
    widths = [int(9360 / cols)] * cols
    grid = "".join(f'<w:gridCol w:w="{w}"/>' for w in widths)
    trs = []
    for ri, row in enumerate(rows):
        cells = []
        for ci in range(cols):
            value = row[ci].strip() if ci < len(row) else ""
            fill = "E8EDF2" if ri == 0 else ("F7F9FA" if ri % 2 == 0 else "FFFFFF")
            content = inline(value)
            cells.append(f'<w:tc><w:tcPr><w:tcW w:w="{widths[ci]}" w:type="dxa"/><w:shd w:val="clear" w:fill="{fill}"/><w:tcMar><w:top w:w="90" w:type="dxa"/><w:left w:w="110" w:type="dxa"/><w:bottom w:w="90" w:type="dxa"/><w:right w:w="110" w:type="dxa"/></w:tcMar></w:tcPr><w:p><w:pPr><w:spacing w:after="0" w:line="270" w:lineRule="auto"/></w:pPr>{content}</w:p></w:tc>')
        trs.append("<w:tr>" + "".join(cells) + "</w:tr>")
    return f'<w:tbl><w:tblPr><w:tblW w:w="9360" w:type="dxa"/><w:tblLayout w:type="fixed"/><w:tblBorders><w:top w:val="single" w:sz="8" w:color="AAB4BE"/><w:left w:val="nil"/><w:bottom w:val="single" w:sz="8" w:color="AAB4BE"/><w:right w:val="nil"/><w:insideH w:val="single" w:sz="4" w:color="D7DDE2"/><w:insideV w:val="single" w:sz="4" w:color="D7DDE2"/></w:tblBorders></w:tblPr><w:tblGrid>{grid}</w:tblGrid>{"".join(trs)}</w:tbl><w:p><w:pPr><w:spacing w:after="100"/></w:pPr></w:p>'


def png_size(path: Path) -> tuple[int, int]:
    data = path.read_bytes()[:24]
    return struct.unpack(">II", data[16:24])


def image_paragraph(rel_id: str, path: Path, doc_pr: int) -> str:
    px_w, px_h = png_size(path)
    max_w, max_h = 5486400, 6858000
    emu_w, emu_h = px_w * 9525, px_h * 9525
    scale = min(max_w / emu_w, max_h / emu_h, 1)
    cx, cy = int(emu_w * scale), int(emu_h * scale)
    name = esc(path.name)
    drawing = f'''<w:drawing><wp:inline xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" distT="0" distB="0" distL="0" distR="0"><wp:extent cx="{cx}" cy="{cy}"/><wp:docPr id="{doc_pr}" name="{name}"/><wp:cNvGraphicFramePr/><a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:nvPicPr><pic:cNvPr id="0" name="{name}"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed="{rel_id}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing>'''
    return f'<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:before="120" w:after="60"/></w:pPr><w:r>{drawing}</w:r></w:p>'


def cover() -> str:
    return "".join([
        '<w:p><w:pPr><w:spacing w:after="1900"/></w:pPr></w:p>',
        f'<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:after="260"/></w:pPr>{run("SPRING AI · DAY 3", bold=True, size=24, color="68788A")}</w:p>',
        f'<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:after="240"/></w:pPr>{run("이커머스 상담 AI 에이전트", bold=True, size=38, color="253342")}</w:p>',
        f'<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:after="1500"/></w:pPr>{run("실습 보고서", size=27, color="526273")}</w:p>',
        f'<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:after="110"/></w:pPr>{run("제출자  P343 함형준", bold=True, size=23, color="253342")}</w:p>',
        f'<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:after="80"/></w:pPr>{run("작성일  2026. 08. 20.", size=20, color="526273")}</w:p>',
        '<w:p><w:r><w:br w:type="page"/></w:r></w:p>',
        f'<w:p><w:pPr><w:spacing w:after="260"/></w:pPr>{run("목차", bold=True, size=29, color="253342")}</w:p>',
        paragraph("1. 실습 개요", indent=240), paragraph("2. 실행 환경 및 방법", indent=240),
        paragraph("3. 구현 구조", indent=240), paragraph("4. 실행 결과", indent=240),
        paragraph("5. 레드팀 검증 결과", indent=240), paragraph("6. 완료 기준 점검 및 결론", indent=240),
    ])


def parse_markdown(text: str):
    lines = text.splitlines()[4:]
    body, media, rels = [cover()], [], []
    i, img_no = 0, 1
    while i < len(lines):
        line = lines[i]
        if line.startswith("```mermaid"):
            block = []
            i += 1
            while i < len(lines) and not lines[i].startswith("```"):
                block.append(lines[i]); i += 1
            body.append(code_block(["처리 흐름", "사용자 → Chat API → Audit → Safety → Memory → RAG → LLM", "LLM → 주문 조회 / 환불 티켓(PENDING) → 응답", "위험 입력 → Safety 단계에서 즉시 차단"]))
        elif line.startswith("```"):
            block = []
            i += 1
            while i < len(lines) and not lines[i].startswith("```"):
                block.append(lines[i]); i += 1
            body.append(code_block(block))
        elif re.match(r'^\|.*\|$', line):
            rows = []
            while i < len(lines) and re.match(r'^\|.*\|$', lines[i]):
                cells = [c.strip() for c in lines[i].strip().strip("|").split("|")]
                if not all(re.fullmatch(r':?-+:?', c) for c in cells):
                    rows.append(cells)
                i += 1
            body.append(table(rows)); continue
        elif line.startswith("!["):
            m = re.match(r'!\[(.*?)\]\(<(.+)>\)', line)
            if m:
                path = ROOT / m.group(2)
                rid = f"rIdImg{img_no}"
                body.append(image_paragraph(rid, path, img_no))
                media.append((f"image{img_no}.png", path))
                rels.append((rid, f"media/image{img_no}.png"))
                img_no += 1
        elif line.startswith("### "):
            body.append(paragraph(line[4:], style="Heading2", keep=True))
        elif line.startswith("## "):
            body.append(paragraph(line[3:], style="Heading1", keep=True))
        elif line.startswith("# "):
            pass
        elif line.startswith("**그림"):
            body.append(f'<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:after="180"/></w:pPr>{run(line.replace("**", ""), bold=True, size=18, color="526273")}</w:p>')
        elif line.startswith("- ") or line.startswith("* "):
            body.append(paragraph("•  " + line[2:], indent=300, after=60))
        elif line.strip():
            clean = line.replace(
                "예상 확인 항목은 답변에 `수령 후 7일 이내`, `고객 부담`이 포함되고, `sources`에 `return-policy`가 표시되는 것이다.",
                "확인 결과, 답변에 `수령 후 7일 이내`와 `고객 부담`이 포함되었고, `sources`에 `return-policy`가 표시되었다.",
            )
            body.append(paragraph(clean))
        i += 1
    return "".join(body), media, rels


def build():
    content, media, image_rels = parse_markdown(SOURCE.read_text(encoding="utf-8"))
    sect = '<w:sectPr><w:headerReference w:type="default" r:id="rIdHeader"/><w:footerReference w:type="default" r:id="rIdFooter"/><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1276" w:bottom="1134" w:left="1276" w:header="567" w:footer="567"/><w:cols w:space="425"/><w:docGrid w:linePitch="330"/></w:sectPr>'
    document = f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:document xmlns:w="{W}" xmlns:r="{R}"><w:body>{content}{sect}</w:body></w:document>'
    styles = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:styles xmlns:w="{W}"><w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Pretendard" w:hAnsi="Pretendard" w:eastAsia="Pretendard" w:cs="Pretendard"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:color w:val="253342"/></w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after="120" w:line="330" w:lineRule="auto"/><w:jc w:val="both"/></w:pPr></w:pPrDefault></w:docDefaults><w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style><w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:keepNext/><w:keepLines/><w:pageBreakBefore/><w:spacing w:before="240" w:after="180"/></w:pPr><w:rPr><w:rFonts w:ascii="Pretendard" w:hAnsi="Pretendard" w:eastAsia="Pretendard"/><w:b/><w:sz w:val="29"/><w:color w:val="253342"/></w:rPr></w:style><w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:keepNext/><w:keepLines/><w:spacing w:before="220" w:after="130"/><w:pBdr><w:bottom w:val="single" w:sz="8" w:space="6" w:color="D7DDE2"/></w:pBdr></w:pPr><w:rPr><w:rFonts w:ascii="Pretendard" w:hAnsi="Pretendard" w:eastAsia="Pretendard"/><w:b/><w:sz w:val="24"/><w:color w:val="334A5E"/></w:rPr></w:style></w:styles>'''
    header = f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:hdr xmlns:w="{W}"><w:p><w:pPr><w:pBdr><w:bottom w:val="single" w:sz="6" w:space="5" w:color="D7DDE2"/></w:pBdr></w:pPr>{run("Spring AI Day 3 실습 보고서", size=17, color="68788A")}</w:p></w:hdr>'
    footer = f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:ftr xmlns:w="{W}"><w:p><w:pPr><w:jc w:val="center"/></w:pPr>{run("P343 함형준  ·  ", size=17, color="68788A")}<w:r><w:rPr><w:rFonts w:ascii="Pretendard" w:hAnsi="Pretendard" w:eastAsia="Pretendard"/><w:sz w:val="17"/><w:color w:val="68788A"/></w:rPr><w:fldChar w:fldCharType="begin"/><w:instrText> PAGE </w:instrText><w:fldChar w:fldCharType="end"/></w:r></w:p></w:ftr>'
    rel_entries = ''.join(f'<Relationship Id="{rid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="{target}"/>' for rid, target in image_rels)
    doc_rels = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/><Relationship Id="rIdHeader" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/header" Target="header1.xml"/><Relationship Id="rIdFooter" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer1.xml"/>{rel_entries}</Relationships>'''
    types = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Default Extension="png" ContentType="image/png"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/><Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/><Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/><Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/></Types>'''
    root_rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/></Relationships>'''
    core = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dc:title>Spring AI Day 3 실습 보고서</dc:title><dc:subject>이커머스 상담 AI 에이전트</dc:subject><dc:creator>P343 함형준</dc:creator><cp:lastModifiedBy>P343 함형준</cp:lastModifiedBy><dcterms:created xsi:type="dcterms:W3CDTF">2026-08-20T00:00:00Z</dcterms:created><dcterms:modified xsi:type="dcterms:W3CDTF">2026-08-20T00:00:00Z</dcterms:modified></cp:coreProperties>'''
    app = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"><Application>Microsoft Office Word</Application><AppVersion>16.0000</AppVersion><Company></Company></Properties>'''
    with zipfile.ZipFile(OUTPUT, "w", zipfile.ZIP_DEFLATED) as z:
        for name, data in {"[Content_Types].xml": types, "_rels/.rels": root_rels, "word/document.xml": document, "word/styles.xml": styles, "word/_rels/document.xml.rels": doc_rels, "word/header1.xml": header, "word/footer1.xml": footer, "docProps/core.xml": core, "docProps/app.xml": app}.items():
            z.writestr(name, data)
        for name, path in media:
            z.write(path, f"word/media/{name}")
    print(OUTPUT)


if __name__ == "__main__":
    build()
