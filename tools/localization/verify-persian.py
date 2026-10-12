#!/usr/bin/env python3
"""Guard the file-scoped lint exception for Persian overlays of project resources.

Copyright (c) 2026 Element Creations Ltd.
SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
"""
import argparse
from collections import defaultdict
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

FORMAT = re.compile(r'%(?:(?P<index>\d+)\$)?(?P<flags>[-#+ 0,(<]*)\d*(?:\.\d+)?(?P<conversion>[tT][a-zA-Z]|[a-zA-Z%])')


def arguments(element):
    result = {}
    implicit = 0
    previous = None
    for match in FORMAT.finditer(''.join(element.itertext())):
        conversion = match['conversion']
        if conversion in ('%', 'n'):
            continue
        if '<' in match['flags']:
            assert previous is not None, 'Relative format argument has no predecessor'
            index = previous
        elif match['index']:
            index = int(match['index'])
        else:
            implicit += 1
            index = implicit
        previous = index
        result.setdefault(index, set()).add(conversion)
    return result


def verify(root, overrides, files, projects):
    defaults = defaultdict(list)
    for file in sorted(files):
        relative = file.relative_to(root).as_posix()
        owner, separator, resource = relative.partition('/src/main/res/values/')
        if not separator or '/' in resource or ':' + owner.replace('/', ':') not in projects:
            continue
        catalogue = ET.parse(file).getroot()
        for element in catalogue:
            if element.tag in ('string', 'plurals') and element.get('name'):
                translatable = catalogue.get('translatable') != 'false' and element.get('translatable') != 'false'
                defaults[element.tag, element.get('name')].append((relative, element, translatable))
    audit = []
    seen = set()
    for translated in ET.parse(overrides).getroot():
        key = translated.tag, translated.get('name')
        assert translated.tag in ('string', 'plurals') and key[1], f'Unsupported Persian override: {key}'
        assert key not in seen, f'Duplicate Persian override: {key}'
        seen.add(key)
        candidates = defaults.get(key, [])
        assert candidates, f'No unqualified default in active project dependencies: {key}'
        for file, original, translatable in candidates:
            assert translatable, f'Persian override of a non-translatable default: {key} ({file})'
            if translated.tag == 'plurals':
                source_items = {item.get('quantity'): item for item in original}
                target_items = {item.get('quantity'): item for item in translated}
                assert len(source_items) == len(original) and len(target_items) == len(translated), f'Duplicate plural quantity: {key}'
                assert 'other' in source_items and 'other' in target_items, f'Missing other plural fallback: {key}'
                for quantity, item in target_items.items():
                    default = source_items.get(quantity, source_items['other'])
                    assert arguments(item) == arguments(default), f'Format arguments differ: {key}/{quantity} ({file})'
            else:
                assert arguments(translated) == arguments(original), f'Format arguments differ: {key} ({file})'
        audit.append({'type': key[0], 'name': key[1], 'defaults': [file for file, _, _ in candidates]})
    assert audit, 'No Persian overlays were verified'
    return audit


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('root', type=Path)
    parser.add_argument('overrides', type=Path)
    parser.add_argument('--file-list', required=True, type=Path)
    parser.add_argument('--projects', required=True, type=Path)
    parser.add_argument('--receipt', required=True, type=Path)
    args = parser.parse_args()
    audit = verify(args.root.resolve(), args.overrides, [Path(p) for p in args.file_list.read_text().splitlines()],
                   set(args.projects.read_text().splitlines()))
    args.receipt.parent.mkdir(parents=True, exist_ok=True)
    args.receipt.write_text(json.dumps(audit, ensure_ascii=False, indent=2) + '\n')
    print(f'PASS: {len(audit)} Persian overlays have active project defaults and compatible format arguments')


if __name__ == '__main__':
    main()
