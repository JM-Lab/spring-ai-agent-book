"""다국어 빌드가 모두 끝난 뒤 검색 색인을 언어별로 나눈다.

i18n 플러그인은 모든 언어의 문서를 사이트 루트의 search/search_index.json 하나에 합친다.
그대로 두면 영어판에서 검색해도 한국어 글이 함께 나오므로, 기본 언어(한국어) 문서는 루트 색인에,
다른 언어 문서는 그 언어 폴더(예: en/search/search_index.json)에 따로 쓴다. 영어판 페이지가
자기 색인을 읽도록 연결하는 부분은 overrides/main.html의 config 블록에 있다.
"""

from __future__ import annotations

import json
import os

from mkdocs.plugins import event_priority


def _write_index(site_dir: str, index: dict, lang: str, docs: list[dict]) -> None:
    config = dict(index["config"], lang=[lang])
    path = os.path.join(site_dir, "search", "search_index.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(dict(index, config=config, docs=docs), f, ensure_ascii=False, separators=(",", ":"))


@event_priority(-200)   # i18n 플러그인(-100)이 나머지 언어를 빌드하고 색인을 합친 다음에 실행한다
def on_post_build(config):
    i18n = config.plugins.get("i18n")
    # 언어별 내부 빌드 도중에는 building이 True다. 가장 바깥 빌드가 끝났을 때 한 번만 나눈다.
    if i18n is None or i18n.building:
        return
    default_language = i18n.default_language
    other_languages = [lang for lang in i18n.build_languages if lang != default_language]
    if not other_languages:
        return

    path = os.path.join(config.site_dir, "search", "search_index.json")
    with open(path, encoding="utf-8") as f:
        index = json.load(f)

    docs_by_language: dict[str, list[dict]] = {lang: [] for lang in [default_language, *other_languages]}
    for doc in index["docs"]:
        location = doc["location"]
        for lang in other_languages:
            prefix = f"{lang}/"
            if location.startswith(prefix):
                # 언어 폴더 기준 상대 위치로 바꾼다(영어판 페이지의 base가 /en/이므로)
                docs_by_language[lang].append(dict(doc, location=location[len(prefix):]))
                break
        else:
            docs_by_language[default_language].append(doc)

    _write_index(config.site_dir, index, default_language, docs_by_language[default_language])
    for lang in other_languages:
        _write_index(os.path.join(config.site_dir, lang), index, lang, docs_by_language[lang])
