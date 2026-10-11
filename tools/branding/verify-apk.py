#!/usr/bin/env python3
# Copyright (c) 2026 Element Creations Ltd.
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
# Please see LICENSE files in the repository root for full details.
"""Verify actual compiled resources and manifest; requires the SDK's aapt2, no pip dependencies."""
import hashlib
import json
import re
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path


def verify_archive(archive, forbidden, prefix=''):
    for original in archive.namelist():
        file = original.removeprefix(prefix)
        if file.endswith('.dex'):
            assert all(url.encode() not in archive.read(original) for url in forbidden['remoteLogoUrls']), 'Packaged Element OAuth logo fallback detected'
        if file.startswith('assets/element-call/') and file.endswith('.js'):
            assert b'Element Call (Beta)' not in archive.read(original), 'Embedded Element Call logo remains in JavaScript'
            if re.fullmatch(r'assets/element-call/assets/index-[\w-]+\.js', file):
                assert b'/element-call/brand-logo.svg' in archive.read(original), 'Embedded call logo does not use the approved brand'
                assert prefix + 'assets/element-call/brand-logo.svg' in archive.namelist(), 'Missing embedded call brand artwork'
        if re.fullmatch(r'assets/element-call/assets/index-[\w-]+\.js\.map', file):
            assert b'Element Call (Beta)' not in archive.read(original), 'Embedded Element artwork remains in source map'
        if file.startswith('res/'):
            assert hashlib.sha256(archive.read(original)).hexdigest() not in forbidden['sha256'], f'Packaged Element artwork: {file}'


def resource_records(aapt, apk):
    resources = subprocess.check_output([str(aapt), 'dump', 'resources', str(apk)], text=True)
    records = {}
    for entry in re.split(r'(?=\s*resource 0x[0-9a-f]+)', resources):
        match = re.search(r'resource (0x[0-9a-f]+) (?:[^: ]+:)?(\w+/[\w.]+)', entry)
        if match:
            records[match[2]] = (match[1], entry)
    return records


def resource_files(record):
    return re.findall(r'^\s*\(([^)]*)\) \(file\) (res/[^\s)]+)', record, re.M)


def normalized_xml(aapt, apk, file):
    tree = subprocess.check_output([str(aapt), 'dump', 'xmltree', str(apk), '--file', file], text=True)
    return re.sub(r' \(line=\d+\)', '', tree).strip()


def verify(aapt, apk, expected, crunched, assets):
    forbidden = json.loads(Path(__file__).with_name('forbidden-element-assets.json').read_text())
    records = resource_records(aapt, apk)
    expected_records = resource_records(aapt, expected)
    names = {name.split('/')[-1] for name in records}
    assert not names.intersection(forbidden['resourceNames']), 'Packaged Element visual resource names detected'
    with zipfile.ZipFile(apk) as archive:
        verify_archive(archive, forbidden)
        with zipfile.ZipFile(expected) as baseline, zipfile.ZipFile(crunched) as compressed:
            for name in ['drawable/brand_foreground', 'drawable/brand_logo_artwork', 'drawable/brand_monochrome', 'drawable/ic_notification']:
                actual_files = dict(resource_files(records[name][1]))
                expected_files = dict(resource_files(expected_records[name][1]))
                assert actual_files.keys() == expected_files.keys(), f'Unexpected configurations for {name}: {actual_files.keys()}'
                for config, expected_file in expected_files.items():
                    actual_file = actual_files[config]
                    if expected_file.endswith('.xml'):
                        assert normalized_xml(aapt, apk, actual_file) == normalized_xml(aapt, expected, expected_file), f'Compiled {name} differs from canonical artwork ({config})'
                    else:
                        assert archive.read(actual_file) in (baseline.read(expected_file), compressed.read(expected_file)), f'Packaged {name} differs from generated SVG artwork ({config})'
        for file in (assets.rglob('*') if assets else []):
            if file.is_file():
                assert archive.read('assets/' + file.relative_to(assets).as_posix()) == file.read_bytes(), f'Embedded call overlay differs: {file.name}'
    def xml(file):
        return subprocess.check_output([str(aapt), 'dump', 'xmltree', str(apk), '--file', file], text=True)
    manifest = xml('AndroidManifest.xml')
    for attr, name in [('icon', 'mipmap/brand_launcher'), ('roundIcon', 'mipmap/brand_launcher_round')]:
        resource_id, _ = records[name]
        assert re.search(rf'android:{attr}\([^\n]+{resource_id}', manifest), f'Manifest {attr} is not the approved brand resource'
    launcher_id, launcher = records['mipmap/brand_launcher']
    assert launcher_id in records['mipmap/brand_launcher_round'][1] or '@mipmap/brand_launcher' in records['mipmap/brand_launcher_round'][1], 'Round icon must alias the adaptive icon'
    icon_file = re.search(r'(res/[^\s)]+\.xml)', launcher)[1]
    adaptive = xml(icon_file)
    for layer, name in [('foreground', 'drawable/brand_foreground'), ('background', 'color/brand_icon_background'), ('monochrome', 'drawable/brand_monochrome')]:
        resource_id, _ = records[name]
        assert layer in adaptive and resource_id in adaptive, f'Missing adaptive {layer}'
    logo_id, _ = records['drawable/brand_logo_artwork']
    assert logo_id in records['drawable/brand_logo'][1] or '@drawable/brand_logo_artwork' in records['drawable/brand_logo'][1], 'ElementLogoAtom would resolve to preview/fallback artwork'
    brand_logo_id, _ = records['drawable/brand_logo']
    assert brand_logo_id in records['drawable/brand_login_logo'][1] or '@drawable/brand_logo' in records['drawable/brand_login_logo'][1], 'Classic login must resolve to the approved brand logo'
    for name in ['drawable/brand_logo_artwork', 'drawable/brand_foreground', 'drawable/brand_monochrome']:
        assert 'night' not in records[name][1], f'Unexpected night override for {name}'
    notification = re.search(r'(res/[^\s)]+\.xml)', records['drawable/ic_notification'][1])[1]
    small_icon = xml(notification)
    assert 'gradient' not in small_icon and re.search(r'#ff(?:ffffff|000000)', small_icon, re.I), 'Notification icon must be a tintable silhouette'
    print(f'PASS {apk.name}: manifest, adaptive/round/themed layers, light/dark logo, notification, Element asset exclusion')


def verify_bundle(aapt, bundle, expected, crunched, assets):
    # AAB uses protobuf resources. Convert the base module's resources to a binary APK for the same checks.
    with tempfile.TemporaryDirectory(prefix='brand-bundle-') as directory:
        proto = Path(directory) / 'base-proto.apk'
        binary = Path(directory) / 'base-binary.apk'
        with zipfile.ZipFile(bundle) as source, zipfile.ZipFile(proto, 'w') as target:
            for file in assets.rglob('*'):
                if file.is_file():
                    assert source.read('base/assets/' + file.relative_to(assets).as_posix()) == file.read_bytes(), f'Embedded call overlay differs: {file.name}'
            verify_archive(source, json.loads(Path(__file__).with_name('forbidden-element-assets.json').read_text()), 'base/')
            for file in source.namelist():
                if file.startswith('base/res/') or file == 'base/resources.pb':
                    target.writestr(file.removeprefix('base/'), source.read(file))
                elif file == 'base/manifest/AndroidManifest.xml':
                    target.writestr('AndroidManifest.xml', source.read(file))
        subprocess.run([str(aapt), 'convert', '--output-format', 'binary', '-o', str(binary), str(proto)], check=True)
        verify(aapt, binary, expected, crunched, None)
    print(f'PASS {bundle.name}: AAB base module branding')


if __name__ == '__main__':
    try:
        aapt, target, resources, assets, android_jar = map(Path, sys.argv[1:])
        apks = sorted(target.glob('*.apk')) if target.is_dir() else [target]
        assert apks, f'No APK outputs at {target}'
        # Compile the current canonical outputs independently. This tests resource resolution,
        # all density configurations and every vector attribute, without permanent artwork hashes.
        with tempfile.TemporaryDirectory(prefix='brand-expected-') as directory:
            directory = Path(directory)
            compiled = directory / 'resources.zip'
            expected = directory / 'expected.apk'
            crunched = directory / 'crunched.apk'
            manifest = directory / 'AndroidManifest.xml'
            manifest.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="branding.expected"/>')
            subprocess.run([str(aapt), 'compile', '--no-crunch', '--dir', str(resources), '-o', str(compiled)], check=True)
            subprocess.run([str(aapt), 'link', '--min-sdk-version', '26', '-I', str(android_jar), '--manifest', str(manifest), '-o', str(expected), str(compiled)], check=True)
            # Release builds may losslessly crunch PNGs; accept AAPT2's encoding as well as original SVG renders.
            subprocess.run([str(aapt), 'compile', '--dir', str(resources), '-o', str(compiled)], check=True)
            subprocess.run([str(aapt), 'link', '--min-sdk-version', '26', '-I', str(android_jar), '--manifest', str(manifest), '-o', str(crunched), str(compiled)], check=True)
            for apk in apks:
                (verify_bundle if apk.suffix == '.aab' else verify)(aapt, apk, expected, crunched, assets)
    except (AssertionError, KeyError, TypeError, ValueError, subprocess.CalledProcessError) as error:
        sys.exit(f'Brand APK verification failed: {error}')
