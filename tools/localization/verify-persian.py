#!/usr/bin/env python3
"""Check Persian coverage and format arguments for active Android project resources.

Copyright (c) 2026 Element Creations Ltd.
SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
"""
import argparse
from collections import defaultdict
import json
from pathlib import Path
import re
from typing import NamedTuple
import xml.etree.ElementTree as ET

FORMAT = re.compile(r'%(?:(?P<index>\d+)\$)?(?P<flags>[-#+ 0,(<]*)\d*(?:\.\d+)?(?P<conversion>[tT][a-zA-Z]|[a-zA-Z%])')
CONVERSIONS = frozenset('bBhHsScCdoxXeEfgGaA')
DATE_CONVERSIONS = frozenset('HIklMSLNpzZsQBbhAaCYyjmdeRTrDFc')


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


class Resource(NamedTuple):
    owner: str
    file: str
    element: ET.Element
    translatable: bool


def resource_key(element):
    kind = element.get('type') if element.tag == 'item' else element.tag
    return kind, element.get('name')


def catalogue(root, files, layers, overlays):
    definitions = {}
    for file in sorted(files):
        directory = file.parent.parent.relative_to(root).as_posix()
        if directory not in layers:
            continue
        owner, priority = layers[directory]
        qualifier = file.parent.name
        if qualifier == 'values-b+fa':
            qualifier = 'values-fa'
        if qualifier not in ('values', 'values-fa'):
            continue
        resources = ET.parse(file).getroot()
        for element in resources:
            kind, name = resource_key(element)
            if file == overlays:
                assert kind in ('string', 'plurals') and name, f'Unsupported Persian override: {(kind, name)}'
            if kind not in ('string', 'plurals') or not name:
                continue
            relative = file.relative_to(root).as_posix()
            identity = owner, priority, qualifier, kind, name
            assert identity not in definitions, (
                f'Duplicate resource {owner}/{kind}/{name} in {qualifier}: {definitions[identity].file}, {relative}'
            )
            translatable = resources.get('translatable') != 'false' and element.get('translatable') != 'false'
            definitions[identity] = Resource(owner, relative, element, translatable)
    return definitions


def resource_text(element):
    return ''.join(element.itertext()).strip().strip('"').strip()


def plural_items(element, location):
    items = {item.get('quantity'): item for item in element}
    assert len(items) == len(element), f'Duplicate plural quantity: {location}'
    assert 'other' in items, f'Missing other plural fallback: {location}'
    assert items.keys() <= {'zero', 'one', 'two', 'few', 'many', 'other'}, f'Invalid plural quantity: {location}'
    return items


def verify_value(kind, source, target, location):
    if kind == 'plurals':
        defaults = plural_items(source, location)
        translations = plural_items(target, location)
        for quantity, item in translations.items():
            verify_value('string', defaults.get(quantity, defaults['other']), item, f'{location}/{quantity}')
    else:
        assert resource_text(target) or not resource_text(source), f'Empty Persian value: {location}'
        if source.get('formatted') != 'false':
            original_args = arguments(source)
            assert original_args == arguments(target), f'Format arguments differ: {location}'
            if original_args:
                text = resource_text(target)
                assert '%' not in FORMAT.sub('', text), f'Unescaped percent in Persian format: {location}'
                assert all(
                    index > 0 and all(
                        conversion in CONVERSIONS or len(conversion) == 2 and conversion[1] in DATE_CONVERSIONS
                        for conversion in conversions
                    )
                    for index, conversions in original_args.items()
                ), f'Invalid format arguments: {location}'


def verify(root, overrides, files, layers, exclusions):
    definitions = catalogue(root, files, layers, overrides)
    defaults = defaultdict(dict)
    persian = defaultdict(dict)
    overlay = {}
    overlay_path = overrides.relative_to(root).as_posix()
    app = layers[overrides.parent.parent.relative_to(root).as_posix()][0]
    for (owner, _, qualifier, kind, name), resource in sorted(definitions.items()):
        index = defaults if qualifier == 'values' else persian
        index[kind, name][owner] = resource
        if resource.file == overlay_path:
            overlay[kind, name] = resource
    assert overlay, 'No Persian overlays were verified'

    failures = []
    audit = []
    for kind, name in sorted(defaults.keys() | overlay.keys()):
        sources = defaults[kind, name]
        targets = persian[kind, name]
        base_overlay = overlay.get((kind, name))
        if base_overlay and not sources:
            failures.append(f'No unqualified default in active project dependencies: {kind}/{name}')
            continue
        # The app's Persian entry takes precedence over Persian library entries. With only
        # library entries, validate every candidate since they can be shared across modules.
        active_targets = [targets[app]] if app in targets else list(targets.values())
        if base_overlay and base_overlay not in active_targets:
            active_targets.append(base_overlay)
        for owner, source in sources.items():
            key = f'{owner}/{kind}/{name}'
            location = f'{key} ({source.file})'
            if not source.translatable:
                if base_overlay:
                    failures.append(f'Persian override of a non-translatable default: {location}')
                continue
            if not active_targets:
                if key not in exclusions:
                    failures.append(f'Missing Persian translation: {location}')
                continue
            for target in active_targets:
                try:
                    verify_value(kind, source.element, target.element, f'{location} -> {target.file}')
                    if kind == 'string':
                        original = resource_text(source.element)
                        if re.search(r'[a-zA-Z]{2}', original) and original == resource_text(target.element):
                            assert key in exclusions, f'Persian value repeats the default: {location} -> {target.file}'
                except AssertionError as error:
                    failures.append(str(error))
        if active_targets and sources:
            audit.append({'type': kind, 'name': name, 'defaults': [value.file for value in sources.values()]})
    assert not failures, '\n'.join(failures)
    return audit


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('root', type=Path)
    parser.add_argument('overrides', type=Path)
    parser.add_argument('--file-list', required=True, type=Path)
    parser.add_argument('--layers', required=True, type=Path)
    parser.add_argument('--exclusions', required=True, type=Path)
    parser.add_argument('--receipt', required=True, type=Path)
    args = parser.parse_args()
    layers = {}
    for line in args.layers.read_text().splitlines():
        owner, priority, directory = line.split('\t')
        layers[directory] = owner, int(priority)
    exclusions = json.loads(args.exclusions.read_text())
    assert isinstance(exclusions, dict) and all(isinstance(reason, str) and reason.strip() for reason in exclusions.values()), (
        'Each Persian coverage exclusion must have a documented reason'
    )
    try:
        audit = verify(args.root.resolve(), args.overrides, [Path(p) for p in args.file_list.read_text().splitlines()], layers, exclusions)
    except AssertionError as error:
        parser.exit(1, f'{error}\n')
    args.receipt.parent.mkdir(parents=True, exist_ok=True)
    args.receipt.write_text(json.dumps(audit, ensure_ascii=False, indent=2) + '\n')
    print(f'PASS: {len(audit)} project strings and plurals have Persian coverage and compatible format arguments')


if __name__ == '__main__':
    main()
