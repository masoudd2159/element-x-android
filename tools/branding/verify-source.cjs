/* Copyright (c) 2026 Element Creations Ltd.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const { createHash } = require('node:crypto');
const forbidden = require('./forbidden-element-assets.json');
const { parseXml, readArtwork } = require('./generate-icons.cjs');
// Standalone CI scope matches enrolled Hoopoe sources. Gradle supplies its exact declared inputs.
function sourceFiles(root, brand = 'apHoopoe') {
    const files = [];
    function walk(dir) {
        if (!fs.existsSync(dir)) return;
        for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
            const file = path.join(dir, entry.name);
            if (entry.isDirectory()) {
                if (!['build', 'node_modules', '.git', '.gradle', 'test', 'testFixtures', 'androidTest'].includes(entry.name)) walk(file);
            } else files.push(file);
        }
    }
    for (const set of ['main', 'debug', 'release', 'nightly', brand]) walk(path.join(root, 'app/src', set));
    for (const dir of ['appnav', 'appconfig', 'libraries', 'features', 'services']) walk(path.join(root, dir));
    for (const file of ['app/build.gradle.kts', 'plugins/src/main/kotlin/poopak.android-brands.gradle.kts']) {
        if (fs.existsSync(path.join(root, file))) files.push(path.join(root, file));
    }
    return files.sort();
}
function verify(root, files = sourceFiles(root)) {
    const failures = [];
    // Gradle provides the active flavor's artwork; standalone CI follows the same registry opt-in.
    const brandDirs = new Set(files.filter(f => f.endsWith(`${path.sep}logo.svg`) &&
        f.startsWith(path.join(root, 'branding') + path.sep)).map(f => path.dirname(f)));
    const registry = path.join(root, 'plugins/src/main/kotlin/poopak.android-brands.gradle.kts');
    if (fs.existsSync(registry)) {
        for (const block of fs.readFileSync(registry, 'utf8').split('BrandConfig(').slice(2)) {
            if (/centralizedBranding\s*=\s*true/.test(block.split('),')[0])) {
                const name = block.match(/name\s*=\s*"(ap\w+)"/)[1];
                brandDirs.add(path.join(root, 'branding', name));
            }
        }
    }
    for (const dir of brandDirs) readArtwork(dir);
    for (const file of files) {
        const entry = path.basename(file);
        if (file.includes(`${path.sep}res${path.sep}`)) {
            const name = path.parse(file).name;
            const contents = fs.readFileSync(file);
            const paths = file.endsWith('.xml') ? Array.from(parseXml(contents.toString()).getElementsByTagName('path')) : [];
            const elementPath = paths.some(n => forbidden.vectorPaths.includes(createHash('sha256')
                .update(n.getAttribute('android:pathData').replace(/[\s,]/g, '')).digest('hex')));
            const upstreamNotification = file === path.join(root, 'libraries/designsystem/src/main/res/drawable/ic_notification.xml');
            if (!upstreamNotification && (elementPath || forbidden.resourceNames.includes(name) || forbidden.sha256.includes(createHash('sha256').update(contents).digest('hex')))) {
                failures.push(file);
            }
        } else if (entry.endsWith('.kt') || entry.endsWith('.kts')) {
            const text = fs.readFileSync(file, 'utf8');
            if (forbidden.remoteLogoUrls.some(url => text.includes(url))) failures.push(file);
            // Legacy icons may be flavor dependencies; unconditional app icons are forbidden.
            if (file === path.join(root, 'app/build.gradle.kts') && /implementation\s*\(\s*projects\.appicon\./.test(text)) {
                failures.push('unconditional appicon dependency in app/build.gradle.kts');
            }
            if (/dependencies\.add\(\s*["']implementation["']\s*,\s*project\(["']:appicon:/.test(text)) {
                failures.push('unconditional appicon dependency in branding plugin');
            }
        }
    }
    if (failures.length) throw Error(`Element visual assets/dependencies must not be distributed in enrolled branding variants:\n${failures.join('\n')}`);
}
module.exports = { verify, sourceFiles };
if (require.main === module) {
    try {
        const args = process.argv.slice(2);
        const files = args[1] === '--file-list' ? fs.readFileSync(args[2], 'utf8').split('\n').filter(Boolean) : undefined;
        verify(path.resolve(args[0]), files);
    } catch (error) { console.error(error.message); process.exitCode = 1; }
}
