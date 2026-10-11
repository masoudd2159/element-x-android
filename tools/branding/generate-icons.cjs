/* Copyright (c) 2026 Element Creations Ltd.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const sharp = require('sharp');
const { DOMParser, XMLSerializer } = require('@xmldom/xmldom');
const { brandCall } = require('./brand-call.cjs');
const header = `<?xml version="1.0" encoding="utf-8"?>
<!-- Copyright (c) 2026 Element Creations Ltd.
SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
Please see LICENSE files in the repository root for full details. -->
`;
const android = 'http://schemas.android.com/apk/res/android';
const densities = [['mdpi', 1], ['hdpi', 1.5], ['xhdpi', 2], ['xxhdpi', 3], ['xxxhdpi', 4]];

function parseXml(text) {
    if (/<!DOCTYPE|<!ENTITY/i.test(text)) throw Error('DTD/entities are unsupported');
    const errors = [];
    let doc;
    try { doc = new DOMParser({ onError: (level, message) => errors.push(`${level}: ${message}`) }).parseFromString(text, 'application/xml'); }
    catch (error) { throw Error(`Invalid XML: ${error.message}`); }
    if (errors.length || !doc.documentElement) throw Error(`Invalid XML: ${errors.join('; ')}`);
    if ([...Array(doc.childNodes.length).keys()].filter(i => doc.childNodes[i].nodeType === 1).length !== 1) {
        throw Error('XML must contain exactly one root');
    }
    return doc;
}

function readSvg(file) {
    if (!fs.existsSync(file)) throw Error(`Missing approved brand artwork: ${file}. Add the official logo.svg; no fallback is allowed.`);
    const doc = parseXml(fs.readFileSync(file, 'utf8'));
    const svg = doc.documentElement;
    if (svg.localName !== 'svg' || svg.namespaceURI !== 'http://www.w3.org/2000/svg') throw Error('Expected an SVG root');
    const bounds = (svg.getAttribute('viewBox') || '').trim().split(/[\s,]+/).map(Number);
    if (bounds.length !== 4 || !bounds.every(Number.isFinite) || bounds[2] <= 0 || bounds[3] <= 0) {
        throw Error('SVG requires a finite, positive viewBox');
    }
    // Accept self-contained vector artwork only. Never silently rasterize unsupported/external content.
    const allowed = new Set(['svg', 'g', 'defs', 'title', 'desc', 'metadata', 'path', 'rect', 'circle', 'ellipse',
        'line', 'polyline', 'polygon', 'linearGradient', 'radialGradient', 'stop', 'clipPath', 'mask']);
    for (const node of Array.from(doc.getElementsByTagName('*'))) {
        if (!allowed.has(node.localName) || node.namespaceURI !== svg.namespaceURI) throw Error(`Unsupported SVG structure: ${node.nodeName}`);
        for (const attr of Array.from(node.attributes)) {
            if (/^on|href$/i.test(attr.name) || /url\(\s*['"]?(?!#)/i.test(attr.value)) throw Error(`Unsupported SVG reference: ${attr.name}`);
        }
    }
    return { doc, svg, bounds };
}

function placedSvg(source, canvas, extent, background = '') {
    const svg = source.svg.cloneNode(true);
    const [, , width, height] = source.bounds;
    const scale = extent / Math.max(width, height);
    svg.setAttribute('x', (canvas - width * scale) / 2);
    svg.setAttribute('y', (canvas - height * scale) / 2);
    svg.setAttribute('width', width * scale);
    svg.setAttribute('height', height * scale);
    svg.setAttribute('preserveAspectRatio', 'xMidYMid meet');
    // DOM serialization handles comments/XML declarations before the original root correctly.
    const notices = Array.from(source.doc.childNodes).filter(n => n.nodeType === 8).map(n => new XMLSerializer().serializeToString(n)).join('');
    return `<svg xmlns="http://www.w3.org/2000/svg" width="${canvas}" height="${canvas}" viewBox="0 0 ${canvas} ${canvas}">${notices}${background}${new XMLSerializer().serializeToString(svg)}</svg>`;
}

function writeIfChanged(file, content) {
    const buffer = Buffer.isBuffer(content) ? content : Buffer.from(content);
    if (fs.existsSync(file) && fs.readFileSync(file).equals(buffer)) return;
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, buffer);
}

async function render(svg, size) {
    return sharp(Buffer.from(svg), { density: 288 }).resize(size, size, { kernel: 'lanczos3' }).png({ compressionLevel: 9 }).toBuffer();
}

// Deliberately limited to self-contained, tintable VectorDrawables. Android owns the tint.
function validateMonochrome(text) {
    const doc = parseXml(text);
    const vector = doc.documentElement;
    if (vector.localName !== 'vector') throw Error('Expected a VectorDrawable root');
    for (const attribute of ['width', 'height']) {
        if (!/^\d+(?:\.\d+)?dp$/.test(vector.getAttributeNS(android, attribute)) ||
            Number.parseFloat(vector.getAttributeNS(android, attribute)) <= 0) throw Error(`Vector requires positive ${attribute} in dp`);
    }
    for (const attribute of ['viewportWidth', 'viewportHeight']) {
        const value = Number(vector.getAttributeNS(android, attribute));
        if (!Number.isFinite(value) || value <= 0) throw Error(`Vector requires positive ${attribute}`);
    }
    const attributes = {
        vector: ['width', 'height', 'viewportWidth', 'viewportHeight'],
        group: ['rotation', 'pivotX', 'pivotY', 'scaleX', 'scaleY', 'translateX', 'translateY'],
        path: ['pathData', 'fillColor', 'fillType', 'fillAlpha', 'strokeAlpha'],
        'clip-path': ['pathData', 'fillType'],
    };
    let painted = false;
    for (const node of Array.from(doc.getElementsByTagName('*'))) {
        if (!attributes[node.localName] || node.namespaceURI) throw Error(`Unsupported vector structure: ${node.nodeName}`);
        for (const attr of Array.from(node.attributes)) {
            if (attr.namespaceURI === 'http://www.w3.org/2000/xmlns/' ||
                (attr.namespaceURI === 'http://schemas.android.com/tools' && attr.localName === 'ignore')) continue;
            if (attr.namespaceURI !== android || !attributes[node.localName].includes(attr.localName) || attr.value.includes('@')) {
                throw Error(`Unsupported vector attribute: ${attr.name}`);
            }
            if (['fillAlpha', 'strokeAlpha'].includes(attr.localName) &&
                (!Number.isFinite(Number(attr.value)) || Number(attr.value) < 0 || Number(attr.value) > 1)) throw Error('Invalid vector alpha');
            if (node.localName === 'group' && !Number.isFinite(Number(attr.value))) throw Error('Invalid vector transform');
            if (attr.localName === 'fillType' && !['evenOdd', 'nonZero'].includes(attr.value)) throw Error('Unsupported vector fillType');
        }
        if (['path', 'clip-path'].includes(node.localName)) {
            const data = node.getAttributeNS(android, 'pathData');
            const arity = { M: 2, L: 2, H: 1, V: 1, C: 6, S: 4, Q: 4, T: 2, A: 7, Z: 0 };
            const segments = data.match(/[a-df-z][^a-df-z]*/gi) || [];
            if (!/^[Mm]/.test(data) || segments.join('') !== data) throw Error('Invalid vector pathData');
            for (const segment of segments) {
                const count = arity[segment[0].toUpperCase()];
                const values = segment.slice(1).match(/[-+]?(?:\d*\.\d+|\d+\.?\d*)(?:e[-+]?\d+)?/gi) || [];
                const remainder = segment.slice(1).replace(/[-+]?(?:\d*\.\d+|\d+\.?\d*)(?:e[-+]?\d+)?/gi, '').replace(/[\s,]/g, '');
                if (count === undefined || remainder || !values.every(v => Number.isFinite(Number(v))) ||
                    (count ? !values.length || values.length % count : values.length)) throw Error('Invalid vector pathData');
            }
        }
        if (node.localName === 'path') {
            if (!/^#(?:000000|FFFFFF)$/i.test(node.getAttributeNS(android, 'fillColor'))) {
                throw Error('Monochrome paths must have opaque black/white fills, without gradients');
            }
            painted ||= !node.hasAttributeNS(android, 'fillAlpha') || Number(node.getAttributeNS(android, 'fillAlpha')) > 0;
        }
    }
    if (!painted) throw Error('Vector has no visible paths');
    return text.startsWith('<?xml') ? text : header + text;
}

function readArtwork(sourceDir) {
    const source = readSvg(path.join(sourceDir, 'logo.svg'));
    const vectors = {};
    for (const name of ['monochrome', 'notification']) {
        const file = path.join(sourceDir, `${name}.xml`);
        if (!fs.existsSync(file)) throw Error(`Missing required brand artwork: ${file}. All three canonical files are mandatory.`);
        try { vectors[name] = validateMonochrome(fs.readFileSync(file, 'utf8')); }
        catch (error) { throw Error(`${file}: ${error.message}`); }
    }
    return { source, ...vectors };
}

function launcherMonochrome(text, extent) {
    const doc = parseXml(text);
    const vector = doc.documentElement;
    const width = Number(vector.getAttributeNS(android, 'viewportWidth'));
    const height = Number(vector.getAttributeNS(android, 'viewportHeight'));
    // Keep the whole supplied viewport in the central 65dp safe circle, even for rectangular silhouettes.
    const scale = Math.min(extent / Math.max(width, height), 65 / Math.hypot(width, height));
    const group = doc.createElement('group');
    for (const [name, value] of Object.entries({ scaleX: scale, scaleY: scale,
        translateX: (108 - width * scale) / 2, translateY: (108 - height * scale) / 2 })) {
        group.setAttributeNS(android, `android:${name}`, String(value));
    }
    while (vector.firstChild) group.appendChild(vector.firstChild);
    vector.appendChild(group);
    for (const name of ['width', 'height']) vector.setAttributeNS(android, `android:${name}`, '108dp');
    for (const name of ['viewportWidth', 'viewportHeight']) vector.setAttributeNS(android, `android:${name}`, '108');
    return header + new XMLSerializer().serializeToString(vector) + '\n';
}

async function generate({ sourceDir, outputDir, background = '#FFFFFF', extent = 60, logoFraction = 314 / 330, assetsDir, callScript, callScriptPath }) {
    if (process.version !== 'v24.12.0') throw Error('Branding tooling requires Node 24.12.0');
    const directories = [sourceDir, outputDir, assetsDir].filter(Boolean).map(p => path.resolve(p));
    for (let i = 0; i < directories.length; i++) for (let j = i + 1; j < directories.length; j++) {
        if (directories[i] === directories[j] || directories[i].startsWith(directories[j] + path.sep) || directories[j].startsWith(directories[i] + path.sep)) {
            throw Error('Artwork and generated output directories must be separate');
        }
    }
    if (!/^#[0-9a-f]{6}$/i.test(background)) throw Error('Background must be an opaque RGB colour');
    if (!(extent > 0 && extent <= 66) || !(logoFraction > 0 && logoFraction <= 1)) throw Error('Invalid logo placement');
    const { source, monochrome, notification } = readArtwork(sourceDir);
    const assets = new Map();
    if (assetsDir) {
        if (!/^element-call\/assets\/index-[\w-]+\.js$/.test(callScriptPath || '')) throw Error('Unsupported embedded call asset layout');
        assets.set(callScriptPath, brandCall(fs.readFileSync(callScript, 'utf8')));
        // The upstream map describes the unmodified bundle and embeds the removed artwork.
        // A valid empty map avoids shipping a misleading 9 MB source archive after transformation.
        assets.set(callScriptPath + '.map', JSON.stringify({ version: 3, sources: [], names: [], mappings: '' }) + '\n');
        assets.set('element-call/brand-logo.svg', fs.readFileSync(path.join(sourceDir, 'logo.svg')));
    }
    const outputs = new Map();
    let foreground = placedSvg(source, 108, extent);
    let largestForeground = await render(foreground, 432);
    const { data } = await sharp(largestForeground).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
    let radiusSquared = 0;
    for (let y = 0; y < 432; y++) for (let x = 0; x < 432; x++) {
        if (data[(y * 432 + x) * 4 + 3]) radiusSquared = Math.max(radiusSquared, (x + 0.5 - 216) ** 2 + (y + 0.5 - 216) ** 2);
    }
    if (!radiusSquared) throw Error('SVG has no visible artwork');
    // Fit painted pixels, not just the bounding rectangle, into the adaptive icon's safe circle.
    // The existing Hoopoe placement already fits and remains pixel-identical.
    const safeRadius = 32.5 * 4;
    if (radiusSquared > safeRadius ** 2) {
        foreground = placedSvg(source, 108, extent * safeRadius / Math.sqrt(radiusSquared));
        largestForeground = await render(foreground, 432);
    }
    for (const [density, scale] of densities) {
        outputs.set(`drawable-${density}/brand_foreground.png`, scale === 4 ? largestForeground : await render(foreground, 108 * scale));
    }
    outputs.set('drawable-xxxhdpi/brand_logo_artwork.png', await render(placedSvg(source, 110, 110 * logoFraction), 440));
    outputs.set('drawable/brand_monochrome.xml', launcherMonochrome(monochrome, extent));
    outputs.set('drawable/ic_notification.xml', notification);
    outputs.set('mipmap-anydpi-v26/brand_launcher.xml', header + `<adaptive-icon xmlns:android="${android}">
    <background android:drawable="@color/brand_icon_background" />
    <foreground android:drawable="@drawable/brand_foreground" />
    <monochrome android:drawable="@drawable/brand_monochrome" />
</adaptive-icon>
`);
    outputs.set('values/brand_icons.xml', header + `<resources>
    <color name="brand_icon_background">${background}</color>
    <item type="drawable" name="brand_logo">@drawable/brand_logo_artwork</item>
    <item type="mipmap" name="brand_launcher_round">@mipmap/brand_launcher</item>
</resources>
`);
    // Finish validation/rendering before touching any output. These directories are exclusively owned by this task.
    syncDirectory(outputDir, outputs);
    if (assetsDir) syncDirectory(assetsDir, assets);
}

function syncDirectory(outputDir, outputs) {
    if (fs.existsSync(outputDir) && fs.lstatSync(outputDir).isSymbolicLink()) throw Error('Generated directory must not be a symlink');
    fs.mkdirSync(outputDir, { recursive: true });
    for (const file of fs.readdirSync(outputDir, { recursive: true })) {
        const target = path.join(outputDir, file);
        if (fs.lstatSync(target).isSymbolicLink()) throw Error('Generated resources must not contain symlinks');
        if (fs.statSync(target).isFile() && !outputs.has(file)) fs.unlinkSync(target);
    }
    for (const [file, content] of outputs) writeIfChanged(path.join(outputDir, file), content);
}

module.exports = { generate, readSvg, placedSvg, parseXml, validateMonochrome, readArtwork, launcherMonochrome };
if (require.main === module) {
    if (process.argv[2] === '--validate') {
        try { readArtwork(process.argv[3]); }
        catch (error) { console.error(error.message); process.exitCode = 1; }
        return;
    }
    const [sourceDir, outputDir, background, extent, logoFraction, assetsDir, callScript, callScriptPath] = process.argv.slice(2);
    generate({ sourceDir, outputDir, background, extent: Number(extent || 60), logoFraction: Number(logoFraction || 314 / 330), assetsDir, callScript, callScriptPath })
        .catch(error => { console.error(`Brand icons: ${error.message}`); process.exitCode = 1; });
}
