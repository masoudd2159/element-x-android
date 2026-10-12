'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const sharp = require('sharp');
const { generate, readSvg, parseXml, validateMonochrome, launcherMonochrome } = require('./generate-icons.cjs');
const { verify } = require('./verify-source.cjs');
const { createHash } = require('node:crypto');
const { brandCall } = require('./brand-call.cjs');
const vm = require('node:vm');
const { vectorAlpha } = require('./test-fixtures/vector-preview.cjs');
const baseline = require('./test-fixtures/hoopoe-baseline.json').current;
const vector = '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FFFFFF" android:pathData="M4,4H20V20H4Z"/></vector>';
const fixture = (t, svg) => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'brand-icons-'));
    t.after(() => fs.rmSync(dir, { recursive: true, force: true }));
    const sourceDir = path.join(dir, 'brand');
    fs.mkdirSync(sourceDir);
    if (svg) fs.writeFileSync(path.join(sourceDir, 'logo.svg'), svg);
    for (const file of ['monochrome.xml', 'notification.xml']) {
        fs.writeFileSync(path.join(sourceDir, file), vector);
    }
    return { sourceDir, outputDir: path.join(dir, 'res') };
};
const svg = (w, h) => `<?xml version="1.0"?><!-- legal notice --><svg xmlns="http://www.w3.org/2000/svg" viewBox="10 20 ${w} ${h}"><rect x="10" y="20" width="${w}" height="${h}" fill="#E33454"/></svg>`;
for (const [w, h] of [[100, 200], [200, 100], [100, 100]]) {
    test(`Preserves ${w}:${h} aspect ratio, origin, colour and centring`, async t => {
        const options = fixture(t, svg(w, h));
        await generate(options);
        const { data, info } = await sharp(path.join(options.outputDir, 'drawable-xxxhdpi/brand_foreground.png')).raw().toBuffer({ resolveWithObject: true });
        const pixels = [];
        for (let y = 0; y < info.height; y++) for (let x = 0; x < info.width; x++) {
            if (data[(y * info.width + x) * 4 + 3] > 200) pixels.push([x, y]);
        }
        const xs = pixels.map(p => p[0]), ys = pixels.map(p => p[1]);
        const left = Math.min(...xs), right = Math.max(...xs), top = Math.min(...ys), bottom = Math.max(...ys);
        assert.ok(Math.abs((right - left + 1) / (bottom - top + 1) - w / h) < 0.02);
        assert.ok(Math.abs(left + right - 431) <= 1 && Math.abs(top + bottom - 431) <= 1);
        assert.ok(pixels.every(([x, y]) => Math.hypot(x + 0.5 - 216, y + 0.5 - 216) <= 132), 'Paint must fit the adaptive safe circle');
        const offset = (216 * info.width + 216) * 4;
        assert.deepEqual([...data.subarray(offset, offset + 4)], [227, 52, 84, 255]);

    });
}
test('Idempotent generation preserves bytes and timestamps; stale outputs are removed', async t => {
    const options = fixture(t, svg(100, 200));
    await generate(options);
    const files = fs.readdirSync(options.outputDir, { recursive: true }).filter(p => fs.statSync(path.join(options.outputDir, p)).isFile());
    const before = files.map(p => [p, fs.readFileSync(path.join(options.outputDir, p)), fs.statSync(path.join(options.outputDir, p)).mtimeMs]);
    fs.writeFileSync(path.join(options.outputDir, 'stale.png'), 'stale');
    await generate(options);
    for (const [file, content, time] of before) {
        assert.deepEqual(fs.readFileSync(path.join(options.outputDir, file)), content);
        assert.equal(fs.statSync(path.join(options.outputDir, file)).mtimeMs, time);
    }
    assert.ok(!fs.existsSync(path.join(options.outputDir, 'stale.png')));
    assert.equal(files.length, 10);
});
test('Missing official asset fails and does not touch existing output', async t => {
    const options = fixture(t);
    fs.mkdirSync(options.outputDir);
    fs.writeFileSync(path.join(options.outputDir, 'sentinel'), 'unchanged');
    await assert.rejects(generate(options), /Missing approved brand artwork/);
    assert.equal(fs.readFileSync(path.join(options.outputDir, 'sentinel'), 'utf8'), 'unchanged');
});
test('Refuses to use the authoritative artwork directory as generated output', async t => {
    const options = fixture(t, svg(100, 100));
    await assert.rejects(generate({ ...options, outputDir: options.sourceDir }), /must be separate/);
    assert.equal(fs.readFileSync(path.join(options.sourceDir, 'logo.svg'), 'utf8'), svg(100, 100));
});
test('Valid XML without painted artwork fails explicitly', async t => {
    const options = fixture(t, '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"/>');
    await assert.rejects(generate(options), /no visible artwork/);
});
test('Rejects invalid XML, external content and unsupported structures', t => {
    for (const input of ['<svg>', '<svg/><svg/>', '<!DOCTYPE svg><svg/>',
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10"><image href="https://example.com/x"/></svg>',
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 0 10"/>']) {
        const options = fixture(t, input);
        assert.throws(() => readSvg(path.join(options.sourceDir, 'logo.svg')));
    }
});
test('Different brand outputs remain isolated', async t => {
    const first = fixture(t, svg(100, 200)), second = fixture(t, svg(200, 100));
    await generate(first);
    const image = fs.readFileSync(path.join(first.outputDir, 'drawable-xxxhdpi/brand_foreground.png'));
    await generate(second);
    assert.deepEqual(fs.readFileSync(path.join(first.outputDir, 'drawable-xxxhdpi/brand_foreground.png')), image);
    assert.notDeepEqual(fs.readFileSync(path.join(second.outputDir, 'drawable-xxxhdpi/brand_foreground.png')), image);
});
test('Round launcher is an alias of the adaptive launcher', async t => {
    const options = fixture(t, svg(100, 100));
    await generate(options);
    const doc = parseXml(fs.readFileSync(path.join(options.outputDir, 'values/brand_icons.xml'), 'utf8'));
    assert.equal(Array.from(doc.getElementsByTagName('item')).find(n => n.getAttribute('name') === 'brand_launcher_round').textContent, '@mipmap/brand_launcher');
    assert.equal(fs.readdirSync(path.join(options.outputDir, 'mipmap-anydpi-v26')).length, 1);
});
test('Required monochrome accepts the reviewed Hoopoe silhouette and rejects coloured paths', () => {
    const mono = fs.readFileSync(path.join(__dirname, '../../branding/apHoopoe/monochrome.xml'), 'utf8');
    assert.ok(validateMonochrome(mono).endsWith(mono));
    assert.throws(() => validateMonochrome(mono.replace('#000000', '#FF0000')), /opaque black\/white/);
});
test('The authoritative Hoopoe artwork is unchanged from the supplied canonical SVG', () => {
    function canonical(node) {
        if (node.nodeType === 1) return [node.nodeName, Array.from(node.attributes).map(a => [a.name, a.value]).sort(),
            Array.from(node.childNodes).map(canonical).filter(Boolean)];
        if (node.nodeType === 3 && node.data.trim()) return node.data.trim();
    }
    const logo = parseXml(fs.readFileSync(path.join(__dirname, '../../branding/apHoopoe/logo.svg'), 'utf8')).documentElement;
    assert.equal(createHash('sha256').update(JSON.stringify(canonical(logo))).digest('hex'), baseline.canonicalSvgSha256);
});
test('Visual regression guard catches renamed known assets, permits technical Element names', t => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'brand-guard-'));
    t.after(() => fs.rmSync(dir, { recursive: true, force: true }));
    fs.mkdirSync(path.join(dir, 'app/src/main/res/drawable'), { recursive: true });
    fs.writeFileSync(path.join(dir, 'app/build.gradle.kts'), 'implementation(projects.libraries.matrix.api) // io.element');
    const file = path.join(dir, 'app/src/main/res/drawable/element_logo.xml');
    fs.writeFileSync(file, '<vector/>');
    assert.throws(() => verify(dir), /Element visual assets/);
    fs.unlinkSync(file);
    assert.doesNotThrow(() => verify(dir));
    fs.appendFileSync(path.join(dir, 'app/build.gradle.kts'), '\n// https://element.io/mobile-icon.png');
    assert.throws(() => verify(dir), /Element visual/);
    fs.writeFileSync(path.join(dir, 'app/build.gradle.kts'), '// io.element is a technical identifier');
    const root = path.join(__dirname, '../..');
    const asset = fs.readdirSync(path.join(root, 'appicon/element/src/main/res/mipmap-mdpi')).find(p => p.endsWith('.webp'));
    fs.copyFileSync(path.join(root, 'appicon/element/src/main/res/mipmap-mdpi', asset), path.join(path.dirname(file), 'renamed.webp'));
    assert.throws(() => verify(dir), /renamed.webp/);
});
const callFixture = 'const unrelated="unchanged"; const Logo=(e,t)=>R.jsxs(`svg`,{width:260,height:30,viewBox:"0 0 260 30",ref:t,...e,children:[R.jsx("desc",{children:`Element Call (Beta)`}),R.jsx("path",{d:"M1 2L3 4"})]});';
test('Embedded call transformation preserves frame, properties, ref and unrelated code', () => {
    const branded = brandCall(callFixture);
    assert.ok(branded.startsWith('const unrelated="unchanged";'));
    assert.ok(!branded.includes('M1 2L3 4') && !branded.includes('Element Call (Beta)'));
    const result = vm.runInNewContext(branded + ';Logo({className:"logo",style:{opacity:0.8}},"reference")', { R: { jsxs: (tag, props) => ({ tag, ...props }) } });
    assert.equal(result.tag, 'img');
    assert.equal(result.width, 260);
    assert.equal(result.height, 30);
    assert.equal(result.className, 'logo');
    assert.equal(result.ref, 'reference');
    assert.equal(result.style.objectFit, 'contain');
    assert.equal(result.style.opacity, 0.8);
    assert.equal(result.src, '/element-call/brand-logo.svg');
    assert.equal(result.children, undefined);
});
test('Unknown or ambiguous embedded call artwork fails explicitly', () => {
    assert.throws(() => brandCall('const NoLogo=1;'), /expected one reviewed logo, found 0/);
    assert.throws(() => brandCall(callFixture + callFixture.replaceAll('unrelated', 'other').replaceAll('Logo', 'OtherLogo')), /found 2/);
});
test('Embedded call overlays are deterministic, remove stale outputs and retain authoritative SVG', async t => {
    const options = fixture(t, svg(100, 200));
    options.assetsDir = path.join(path.dirname(options.outputDir), 'assets');
    options.callScript = path.join(path.dirname(options.outputDir), 'vendor.js');
    options.callScriptPath = 'element-call/assets/index-reviewed.js';
    fs.writeFileSync(options.callScript, callFixture);
    await generate(options);
    const files = fs.readdirSync(options.assetsDir, { recursive: true }).filter(f => fs.statSync(path.join(options.assetsDir, f)).isFile());
    const before = files.map(f => [f, fs.readFileSync(path.join(options.assetsDir, f)), fs.statSync(path.join(options.assetsDir, f)).mtimeMs]);
    assert.equal(files.length, 3);
    assert.deepEqual(fs.readFileSync(path.join(options.assetsDir, 'element-call/brand-logo.svg')), fs.readFileSync(path.join(options.sourceDir, 'logo.svg')));
    fs.writeFileSync(path.join(options.assetsDir, 'stale.js'), 'old');
    await generate(options);
    for (const [file, bytes, time] of before) {
        assert.deepEqual(fs.readFileSync(path.join(options.assetsDir, file)), bytes);
        assert.equal(fs.statSync(path.join(options.assetsDir, file)).mtimeMs, time);
    }
    assert.ok(!fs.existsSync(path.join(options.assetsDir, 'stale.js')));
    fs.writeFileSync(options.callScript, 'const vendorUpgrade=true;');
    await assert.rejects(generate(options), /expected one reviewed logo/);
    assert.deepEqual(fs.readFileSync(path.join(options.assetsDir, before[0][0])), before[0][1]);
    await assert.rejects(generate({ ...options, assetsDir: options.sourceDir }), /must be separate/);
});

test('Hoopoe source scope excludes inactive brands and catches relevant Kotlin changes', t => {
    const { sourceFiles } = require('./verify-source.cjs');
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'brand-scope-'));
    t.after(() => fs.rmSync(dir, { recursive: true, force: true }));
    for (const relative of ['app/src/apPoopak/res/drawable/element_logo.xml', 'app/src/legacyBranding/res/drawable/element_logo.xml',
        'enterprise/features/private/impl/src/main/res/drawable/element_logo.xml']) {
        const file = path.join(dir, relative);
        fs.mkdirSync(path.dirname(file), { recursive: true });
        fs.writeFileSync(file, '<vector/>');
    }
    const kotlin = path.join(dir, 'libraries/matrix/impl/src/main/kotlin/Logo.kt');
    fs.mkdirSync(path.dirname(kotlin), { recursive: true });
    fs.writeFileSync(kotlin, '// io.element is a technical namespace');
    assert.deepEqual(sourceFiles(dir), [kotlin]);
    assert.doesNotThrow(() => verify(dir));
    fs.appendFileSync(kotlin, '\nval uri = "https://element.io/mobile-icon.png"');
    assert.throws(() => verify(dir), /Logo.kt/);
    const active = path.join(dir, 'app/src/apHoopoe/res/drawable/renamed.webp');
    fs.mkdirSync(path.dirname(active), { recursive: true });
    const root = path.join(__dirname, '../..');
    const asset = fs.readdirSync(path.join(root, 'appicon/element/src/main/res/mipmap-mdpi')).find(p => p.endsWith('.webp'));
    fs.copyFileSync(path.join(root, 'appicon/element/src/main/res/mipmap-mdpi', asset), active);
    assert.throws(() => verify(dir, [active]), /renamed.webp/);
    const foss = path.join(dir, 'features/enterprise/impl-foss/src/main/res/drawable/element_logo.xml');
    fs.mkdirSync(path.dirname(foss), { recursive: true });
    fs.writeFileSync(foss, '<vector/>');
    assert.ok(sourceFiles(dir).includes(foss));
    assert.throws(() => verify(dir, [foss]), /impl-foss/);
});

test('Hoopoe adaptive foregrounds preserve the supplied approved pixels at every density', async t => {
    const options = fixture(t);
    for (const file of ['logo.svg', 'monochrome.xml', 'notification.xml']) {
        fs.copyFileSync(path.join(__dirname, '../../branding/apHoopoe', file), path.join(options.sourceDir, file));
    }
    await generate(options);
    // Explicitly reviewed baseline data stays separate from production rendering.
    const approved = baseline.foregroundRgbaSha256;
    for (const [density, hash] of Object.entries(approved)) {
        const pixels = await sharp(path.join(options.outputDir, `drawable-${density}/brand_foreground.png`)).raw().toBuffer();
        assert.equal(createHash('sha256').update(pixels).digest('hex'), hash, density);
    }
});

for (const name of ['monochrome.xml', 'notification.xml']) {
    test(`${name} is mandatory and invalid artwork leaves all outputs intact`, async t => {
        const options = fixture(t, svg(100, 100));
        await generate(options);
        const original = fs.readFileSync(path.join(options.outputDir, 'drawable/ic_notification.xml'));
        fs.unlinkSync(path.join(options.sourceDir, name));
        await assert.rejects(generate(options), new RegExp(`Missing required brand artwork:.*${name}`));
        for (const invalid of ['<vector>', vector.replace('M4,4H20V20H4Z', 'M1'), vector.replace('24dp', '0dp'),
            vector.replace('#FFFFFF', '@color/white'), vector.replace('#FFFFFF', '#FF0000'),
            vector.replace('<path ', '<path android:fillAlpha="0" ')]) {
            fs.writeFileSync(path.join(options.sourceDir, name), invalid);
            await assert.rejects(generate(options), new RegExp(name));
            assert.deepEqual(fs.readFileSync(path.join(options.outputDir, 'drawable/ic_notification.xml')), original);
        }
    });
}
test('Replacing notification alone changes its dedicated output and preserves every other output timestamp', async t => {
    const options = fixture(t, svg(100, 100));
    await generate(options);
    const files = fs.readdirSync(options.outputDir, { recursive: true }).filter(p => fs.statSync(path.join(options.outputDir, p)).isFile());
    const before = files.map(p => [p, fs.readFileSync(path.join(options.outputDir, p)), fs.statSync(path.join(options.outputDir, p)).mtimeMs]);
    fs.writeFileSync(path.join(options.sourceDir, 'notification.xml'), vector.replace('M4,4H20V20H4Z', 'M6,4H20V20H6Z'));
    await generate(options);
    for (const [file, bytes, time] of before) {
        if (file === 'drawable/ic_notification.xml') assert.notDeepEqual(fs.readFileSync(path.join(options.outputDir, file)), bytes);
        else {
            assert.deepEqual(fs.readFileSync(path.join(options.outputDir, file)), bytes);
            assert.equal(fs.statSync(path.join(options.outputDir, file)).mtimeMs, time);
        }
    }
});
function assertNotificationAlpha(alpha, size) {
    const scale = size / 24;
    assert.ok(alpha.filter(a => a > 128).length > 70 * scale ** 2, 'Silhouette must remain visible at notification size');
    assert.ok(alpha.filter(a => a === 0).length > 200 * scale ** 2, 'A full opaque tile is not a notification icon');
    for (const [x, y] of [[0, 0], [23, 0], [0, 23], [23, 23], [5, 11], [18, 11]]) {
        assert.equal(alpha[Math.floor((y + 0.5) * scale) * size + Math.floor((x + 0.5) * scale)], 0,
            `Background/interior cutout at (${x}, ${y}) must remain transparent`);
    }
}

test('Notification independently preserves the existing VectorDrawable, transparent cutouts and a legible small silhouette', async () => {
    const text = fs.readFileSync(path.join(__dirname, '../../branding/apHoopoe/notification.xml'), 'utf8');
    const notification = parseXml(validateMonochrome(text));
    // Pin the existing notification independently of themed launcher artwork. Ignore
    // XML comments/formatting, but retain every Android attribute and node's order.
    function canonical(node) {
        return [node.localName, Array.from(node.attributes).filter(a => a.namespaceURI === 'http://schemas.android.com/apk/res/android')
            .map(a => [a.localName, a.value]).sort(), Array.from(node.childNodes).filter(n => n.nodeType === 1).map(canonical)];
    }
    const hash = createHash('sha256').update(JSON.stringify(canonical(notification.documentElement))).digest('hex');
    assert.equal(hash, baseline.notification.canonicalVectorSha256);
    // 24dp at all five Android densities; no launcher viewport or placement constants.
    for (const size of [24, 36, 48, 72, 96]) assertNotificationAlpha(await vectorAlpha(text, size), size);
});

test('Notification visual checks reject invisible, tiny, opaque and filled-cutout artwork', async () => {
    const text = fs.readFileSync(path.join(__dirname, '../../branding/apHoopoe/notification.xml'), 'utf8');
    const tile = '<path android:fillColor="#ffffff" android:pathData="M0,0H24V24H0Z"/>';
    await assert.rejects(vectorAlpha(text.replace('<path', '<path android:fillAlpha="0"'), 24), /no visible paths/);
    for (const bad of [text.replace('<path', '<group android:translateX="1000"><path').replace('</vector>', '</group></vector>'),
        text.replace('<path', '<group android:scaleX="0.01" android:scaleY="0.01"><path').replace('</vector>', '</group></vector>'),
        text.replace('</vector>', `${tile}</vector>`),
        text.replace('</vector>', `${tile.replace('M0,0H24V24H0Z', 'M4,10H7V13H4Z')}</vector>`)]) {
        const alpha = await vectorAlpha(bad, 24);
        assert.throws(() => assertNotificationAlpha(alpha, 24), /Silhouette|opaque tile|cutout/);
    }
});

test('Vector preview honours viewport, nested pivot transforms, fill rules, alpha and scoped clips', async () => {
    const wrap = body => vector.replace('<path android:fillColor="#FFFFFF" android:pathData="M4,4H20V20H4Z"/>', body);
    const square = (data, attrs = '') => `<path android:fillColor="#FFFFFF" android:pathData="${data}" ${attrs}/>`;
    // Scale around (4,4), rotate 90 degrees, translate (10,2), then apply a nested translation.
    const transformed = wrap('<group android:pivotX="4" android:pivotY="4" android:scaleX="2" ' +
        'android:rotation="90" android:translateX="10" android:translateY="2">' +
        `<group android:translateX="1">${square('M4,4H6V7H4Z')}</group></group>`);
    assert.deepEqual(await vectorAlpha(transformed, 24), await vectorAlpha(wrap(square('M11,8H14V12H11Z')), 24));
    const rectangle = wrap(square('M4,4H20V20H4Z')).replace('android:viewportWidth="24"', 'android:viewportWidth="48"');
    assert.deepEqual(await vectorAlpha(rectangle, 24), await vectorAlpha(wrap(square('M2,4H10V20H2Z')), 24));
    const clipped = wrap(square('M0,0H2V2H0Z') + '<group>' + square('M4,4H6V6H4Z') + '<clip-path android:pathData="M8,8H16V16H8Z"/>' +
        square('M4,4H20V20H4Z') + '</group>' + square('M20,20H24V24H20Z'));
    assert.deepEqual(await vectorAlpha(clipped, 24), await vectorAlpha(wrap(square('M0,0H2V2H0Z M4,4H6V6H4Z M8,8H16V16H8Z M20,20H24V24H20Z')), 24));
    const hole = 'M4,4H20V20H4Z M8,8H16V16H8Z';
    assert.equal((await vectorAlpha(wrap(square(hole, 'android:fillType="evenOdd"')), 24))[12 * 24 + 12], 0);
    assert.equal((await vectorAlpha(wrap(square(hole)), 24))[12 * 24 + 12], 255);
    assert.equal((await vectorAlpha(wrap(square('M4,4H20V20H4Z', 'android:fillAlpha="0.5"')), 24))[12 * 24 + 12], 128);
});

test('Themed launcher preserves supplied paths and fits its viewport inside the adaptive safe circle', () => {
    const text = fs.readFileSync(path.join(__dirname, '../../branding/apHoopoe/monochrome.xml'), 'utf8');
    const original = parseXml(text), generated = parseXml(launcherMonochrome(text, 60));
    const paths = doc => Array.from(doc.getElementsByTagName('path')).map(n => Array.from(n.attributes).map(a => [a.name, a.value]));
    assert.deepEqual(paths(generated), paths(original));
    assert.equal(generated.documentElement.getAttribute('android:viewportWidth'), '108');
    const group = generated.getElementsByTagName('group')[0];
    const scale = Number(group.getAttribute('android:scaleX'));
    assert.equal(scale, Number(group.getAttribute('android:scaleY')));
    const width = Number(original.documentElement.getAttribute('android:viewportWidth'));
    const height = Number(original.documentElement.getAttribute('android:viewportHeight'));
    assert.ok(Math.hypot(width, height) * scale <= 65 + 1e-8);
    assert.ok(Math.abs(Number(group.getAttribute('android:translateX')) + width * scale / 2 - 54) < 1e-8);
    assert.ok(Math.abs(Number(group.getAttribute('android:translateY')) + height * scale / 2 - 54) < 1e-8);
});
