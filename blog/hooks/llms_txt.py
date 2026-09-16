"""빌드 때 llms.txt와 llms-full.txt를 사이트 루트에 만든다.

llms.txt는 https://llmstxt.org 형식의 사이트 색인(제목, 요약, 글 목록)이고,
llms-full.txt는 모든 글의 본문을 순서대로 이은 텍스트다. 생성형 검색과 LLM 크롤러가
사이트를 통째로 읽을 때 쓰라고 두는 파일이며, 사람이 볼 페이지에는 영향이 없다.
"""

from __future__ import annotations

import os
import re
from html.parser import HTMLParser

_layout: list[tuple[str | None, list[str]]] = []   # (nav 절 제목, 그 절의 페이지 src_uri 목록)
_pages: dict[str, dict] = {}                        # src_uri -> title, url, description, text


class _TextExtractor(HTMLParser):
    """렌더링된 본문 HTML에서 읽을 수 있는 텍스트만 뽑는다. 코드 블록은 줄 바꿈을 지킨다."""

    SKIP_TAGS = {"script", "style", "svg", "button", "form", "nav"}
    BLOCK_TAGS = {"p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "pre", "tr", "table",
                  "blockquote", "section", "figure", "figcaption", "ul", "ol", "details", "summary", "br", "hr"}

    def __init__(self) -> None:
        super().__init__()
        self.parts: list[str] = []
        self.skip = 0
        self.in_pre = False

    def handle_starttag(self, tag, attrs):
        classes = dict(attrs).get("class", "") or ""
        if tag in self.SKIP_TAGS or (tag == "a" and "headerlink" in classes):
            self.skip += 1
            return
        if tag == "pre":
            self.in_pre = True
        if tag in self.BLOCK_TAGS:
            self.parts.append("\n")
        if tag in ("h1", "h2", "h3", "h4"):
            self.parts.append("#" * int(tag[1]) + " ")
        if tag == "td" or tag == "th":
            self.parts.append(" | ")

    def handle_endtag(self, tag):
        if tag in self.SKIP_TAGS or tag == "a":
            if self.skip:
                self.skip -= 1
            return
        if tag == "pre":
            self.in_pre = False
        if tag in self.BLOCK_TAGS:
            self.parts.append("\n")

    def handle_data(self, data):
        if self.skip:
            return
        self.parts.append(data if self.in_pre else re.sub(r"\s+", " ", data))

    def text(self) -> str:
        joined = "".join(self.parts)
        joined = re.sub(r"[ \t]+\n", "\n", joined)
        return re.sub(r"\n{3,}", "\n\n", joined).strip()


def on_nav(nav, config, files):
    _layout.clear()
    for item in nav.items:
        if item.is_page:
            _layout.append((None, [item.file.src_uri]))
        elif item.is_section:
            _layout.append((item.title, [p.file.src_uri for p in item.children if p.is_page]))


def on_page_content(html, page, config, files):
    parser = _TextExtractor()
    parser.feed(html)
    _pages[page.file.src_uri] = {
        "title": page.title,
        "url": page.canonical_url or (config.site_url + page.url),
        "description": (page.meta or {}).get("description", ""),
        "text": parser.text(),
    }
    return html


def on_post_build(config):
    site_name = config.site_name
    lines = [f"# {site_name}", "", f"> {config.site_description}", "",
             "책 「스프링 AI 2.0으로 배우는 AI 에이전트 개발 완벽 가이드」(허제민 지음, 위키북스, 2026)의 핵심을 정리한 온라인 가이드입니다. "
             "글은 책의 장 순서를 따르고, 코드 블록은 예제 저장소 https://github.com/JM-Lab/spring-ai-agent-book 의 파일에서 그대로 가져옵니다. "
             f"모든 글의 본문을 이은 파일은 {config.site_url}llms-full.txt 입니다.", ""]
    full = [f"# {site_name}", "", config.site_description, ""]
    for section, uris in _layout:
        entries = [_pages[u] for u in uris if u in _pages]
        if not entries:
            continue
        lines.append(f"## {section}" if section else "## 사이트")
        for e in entries:
            desc = f": {e['description']}" if e["description"] else ""
            lines.append(f"- [{e['title']}]({e['url']}){desc}")
            full += ["", "-" * 72, f"# {e['title']}", f"URL: {e['url']}", "", e["text"], ""]
        lines.append("")
    with open(os.path.join(config.site_dir, "llms.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines).rstrip() + "\n")
    with open(os.path.join(config.site_dir, "llms-full.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(full).rstrip() + "\n")
