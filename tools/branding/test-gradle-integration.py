#!/usr/bin/env python3
"""Exercise FOSS Gradle branding inputs and dependency graphs without private sources.

Copyright (c) 2026 Element Creations Ltd.
SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
"""
from pathlib import Path
import re
import os
import zipfile
import subprocess
import uuid

ROOT = Path(__file__).resolve().parents[2]
LOGS = ROOT / 'app/build/brandingQa/foss-integration'
LOGS.mkdir(parents=True, exist_ok=True)
GEN = ':app:generateApHoopoeBrandIcons'
VERIFY = ':app:verifyApHoopoeBrandSource'
VALIDATE = ':app:validateAllBrandArtwork'


def gradle(label, *tasks, fail=False):
    result = subprocess.run([str(ROOT / 'gradlew'), *tasks, '--configuration-cache', '--stacktrace', '--console=plain'],
                            cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    (LOGS / f'{label}.log').write_text(result.stdout)
    assert (result.returncode != 0) == fail, f'{label}: unexpected exit {result.returncode}; see {LOGS / (label + ".log")}'
    return result.stdout


def snapshot():
    return {str(p.relative_to(ROOT)): (p.read_bytes(), p.stat().st_mtime_ns)
            for base in ['brandIcons/apHoopoe', 'brandAssets/apHoopoe']
            for p in (ROOT / 'app/build/generated' / base).rglob('*') if p.is_file()}


# These temporary fixtures never overwrite existing work, even if interrupted.
fixture = ROOT / f'app/src/main/BrandVerificationFixture_{uuid.uuid4().hex}.kt'
unrelated = ROOT / f'branding/unrelated_{uuid.uuid4().hex}.kt'
foss_fixture = ROOT / f'features/enterprise/impl-foss/src/main/BrandVerificationFixture_{uuid.uuid4().hex}.kt'
notification = ROOT / 'branding/apHoopoe/notification.xml'
original_notification = notification.read_bytes()
original_stat = notification.stat()
mono = ROOT / 'branding/apHoopoe/monochrome.xml'
original_mono = mono.read_bytes()
mono_stat = mono.stat()


def packaged_artwork():
    apk = next((ROOT / 'app/build/outputs/apk/apHoopoe/debug').glob('*arm64-v8a*.apk'))
    with zipfile.ZipFile(apk) as archive:
        return {name: archive.read(name) for name in archive.namelist()
                if re.search(r'res/.*(?:brand_foreground|brand_logo_artwork|ic_notification)', name)}


try:
    gradle('first', GEN, VERIFY, VALIDATE)
    before = snapshot()
    unchanged = gradle('unchanged', GEN, VERIFY, VALIDATE)
    assert f'{GEN} UP-TO-DATE' in unchanged and f'{VERIFY} UP-TO-DATE' in unchanged
    assert 'Reusing configuration cache' in unchanged
    assert snapshot() == before
    fixture.write_text('// io.element is a harmless technical identifier\n')
    relevant = gradle('relevant-kotlin', GEN, VERIFY)
    assert f'{GEN} UP-TO-DATE' in relevant and f'{VERIFY} UP-TO-DATE' not in relevant
    assert snapshot() == before
    fixture.write_text('val forbiddenLogo = "https://element.io/mobile-icon.png"\n')
    rejected = gradle('forbidden-kotlin', VERIFY, fail=True)
    assert fixture.name in rejected and 'Element visual assets' in rejected
    fixture.unlink()
    gradle('restore', GEN, VERIFY)
    foss_fixture.write_text('val forbiddenLogo = "https://element.io/mobile-icon.png"\n')
    rejected_foss = gradle('forbidden-foss-kotlin', GEN, VERIFY, fail=True)
    assert foss_fixture.name in rejected_foss and f'{GEN} UP-TO-DATE' in rejected_foss
    assert snapshot() == before
    foss_fixture.unlink()
    gradle('restore-foss', GEN, VERIFY)
    unrelated.write_text('// not an application source\n')
    unrelated_build = gradle('unrelated-kotlin', GEN, VERIFY)
    assert f'{GEN} UP-TO-DATE' in unrelated_build and f'{VERIFY} UP-TO-DATE' in unrelated_build
    assert snapshot() == before
    # A valid independent notification edit must preserve all launcher/UI/call output bytes and mtimes.
    # Restore the user's exact sources, including their original timestamps, in finally.
    for file, original in [(notification, original_notification), (mono, original_mono)]:
        file.unlink()
        missing = gradle(f'missing-{file.stem}', VALIDATE, GEN, fail=True)
        assert file.name in missing and 'Missing required brand artwork' in missing
        assert snapshot() == before
        file.write_text('<vector>')
        malformed = gradle(f'malformed-{file.stem}', VALIDATE, GEN, fail=True)
        assert file.name in malformed and 'Invalid XML' in malformed
        assert snapshot() == before
        file.write_bytes(original)
    gradle('restore-artwork', GEN, VERIFY, VALIDATE)
    before = snapshot()
    # Actual APK regression: the dedicated icon changes, while all packaged launcher/UI PNGs stay identical.
    gradle('package-before-notification', ':app:assembleApHoopoeDebug')
    packaged_before = packaged_artwork()
    assert packaged_before and any('ic_notification' in n for n in packaged_before)
    changed_notification = original_notification.replace(b'#FFFFFF', b'#000000')
    assert changed_notification != original_notification
    notification.write_bytes(changed_notification)
    gradle('notification-only', GEN, VERIFY, VALIDATE)
    changed = snapshot()
    notification_output = 'app/build/generated/brandIcons/apHoopoe/drawable/ic_notification.xml'
    assert changed[notification_output][0] != before[notification_output][0]
    assert {k: v for k, v in changed.items() if k != notification_output} == {k: v for k, v in before.items() if k != notification_output}
    gradle('package-after-notification', ':app:assembleApHoopoeDebug')
    packaged_after = packaged_artwork()
    assert packaged_after.keys() == packaged_before.keys()
    for name in packaged_before:
        assert (packaged_after[name] != packaged_before[name]) == ('ic_notification' in name), name
    notification.write_bytes(original_notification)
    gradle('restore-notification-package', ':app:assembleApHoopoeDebug', VALIDATE)
    assert packaged_artwork() == packaged_before
    # Remove one Gradle-owned PNG to force restoration of the complete cached output.
    cache_before = snapshot()
    cached_png = ROOT / 'app/build/generated/brandIcons/apHoopoe/drawable-mdpi/brand_foreground.png'
    cached_png.unlink()
    cached = gradle('build-cache-restoration', GEN)
    assert f'{GEN} FROM-CACHE' in cached
    assert {k: v[0] for k, v in snapshot().items()} == {k: v[0] for k, v in cache_before.items()}
    for brand in ['ApPoopak', 'ApMsc']:
        plan = gradle(f'{brand}-plan', f':app:assemble{brand}Debug', '--dry-run')
        assert 'generateApHoopoeBrandIcons' not in plan and 'installBrandingTools' not in plan
        assert 'verifyEnterpriseSources' not in plan
        # Included standalone library modules are harmless; their tasks must not be in the app graph.
        assert ':features:enterprise:impl-foss:' in plan
        assert ':enterprise:features:' not in plan
    for brand in ['apHoopoe', 'apPoopak', 'apMsc']:
        graph = gradle(f'{brand}-dependencies', ':app:dependencies', '--configuration', f'{brand}DebugRuntimeClasspath')
        projects = set(re.findall(r"project ['\"]?(:[\w:-]+)", graph))
        assert ':features:enterprise:impl-foss' in projects
        assert not any(p.startswith(':enterprise:') for p in projects)
        assert 'FAILED' not in graph
        if brand == 'apHoopoe':
            assert not any(p.startswith(':appicon:') for p in projects)
        else:
            assert ':appicon:enterprise' in projects
    registry = (ROOT / 'plugins/src/main/kotlin/poopak.android-brands.gradle.kts').read_text()
    flavors = re.findall(r'name = "(ap\w+)"', registry)
    inventory = gradle('registered-tasks', ':app:tasks', '--all')
    assert len(flavors) == 19
    for flavor in flavors:
        capitalized = flavor[0].upper() + flavor[1:]
        assert f'assemble{capitalized}Debug' in inventory
        assert (f'generate{capitalized}BrandIcons' in inventory) == (flavor == 'apHoopoe')
    assert 'verifyEnterpriseSources' not in inventory
    fixtures_graph = gradle('app-test-fixtures-dependencies', ':app:dependencies', '--configuration', 'apHoopoeDebugTestFixturesCompileClasspath')
    assert 'FAILED' not in fixtures_graph and not re.search(r"project ['\"]?:enterprise:", fixtures_graph)
    for parent in ['brandIcons', 'brandAssets']:
        base = ROOT / 'app/build/generated' / parent
        assert {p.relative_to(base).parts[0] for p in base.rglob('*') if p.is_file()} == {'apHoopoe'}
    print('PASS: required/invalid XML, packaged notification-only changes, build cache, managed tooling, configuration cache, FOSS source invalidation/graphs, unchanged images and all 19 flavor registrations')
finally:
    notification.write_bytes(original_notification)
    os.utime(notification, ns=(original_stat.st_atime_ns, original_stat.st_mtime_ns))
    mono.write_bytes(original_mono)
    os.utime(mono, ns=(mono_stat.st_atime_ns, mono_stat.st_mtime_ns))
    fixture.unlink(missing_ok=True)
    unrelated.unlink(missing_ok=True)
    foss_fixture.unlink(missing_ok=True)
