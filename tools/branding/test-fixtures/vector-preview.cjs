'use strict';
const sharp = require('sharp');
const { parseXml, validateMonochrome } = require('../generate-icons.cjs');
const { XMLSerializer } = require('@xmldom/xmldom');
const android = 'http://schemas.android.com/apk/res/android';
const svgNamespace = 'http://www.w3.org/2000/svg';

// Test-only preview of the supported tintable VectorDrawable subset. Android group
// order is scale, rotate, then translate, around the pivot. Clip paths affect only
// subsequent siblings in their group, including nested groups.
function vectorPreview(text, size) {
    const vector = parseXml(validateMonochrome(text)).documentElement;
    const doc = parseXml(`<svg xmlns="${svgNamespace}" width="${size}" height="${size}"/>`);
    const svg = doc.documentElement;
    const attr = (node, name, fallback) => node.hasAttributeNS(android, name) ? node.getAttributeNS(android, name) : fallback;
    svg.setAttribute('viewBox', `0 0 ${attr(vector, 'viewportWidth')} ${attr(vector, 'viewportHeight')}`);
    // VectorDrawable stretches the viewport to its bounds rather than applying SVG's aspect-ratio padding.
    svg.setAttribute('preserveAspectRatio', 'none');
    let clipId = 0;
    const element = name => doc.createElementNS(svgNamespace, name);
    function children(source, target) {
        let current = target;
        for (const node of Array.from(source.childNodes).filter(n => n.nodeType === 1)) {
            if (node.localName === 'group') {
                const group = element('g');
                const x = Number(attr(node, 'pivotX', 0)), y = Number(attr(node, 'pivotY', 0));
                group.setAttribute('transform', `translate(${Number(attr(node, 'translateX', 0)) + x} ${Number(attr(node, 'translateY', 0)) + y}) ` +
                    `rotate(${attr(node, 'rotation', 0)}) scale(${attr(node, 'scaleX', 1)} ${attr(node, 'scaleY', 1)}) translate(${-x} ${-y})`);
                current.appendChild(group);
                children(node, group);
            } else {
                const path = element('path');
                path.setAttribute('d', attr(node, 'pathData'));
                const rule = attr(node, 'fillType', 'nonZero') === 'evenOdd' ? 'evenodd' : 'nonzero';
                if (node.localName === 'clip-path') {
                    const clip = element('clipPath');
                    clip.setAttribute('id', `clip${clipId++}`);
                    clip.setAttribute('clipPathUnits', 'userSpaceOnUse');
                    path.setAttribute('clip-rule', rule);
                    clip.appendChild(path);
                    current.appendChild(clip);
                    const clipped = element('g');
                    clipped.setAttribute('clip-path', `url(#${clip.getAttribute('id')})`);
                    current.appendChild(clipped);
                    current = clipped;
                } else {
                    path.setAttribute('fill', attr(node, 'fillColor'));
                    path.setAttribute('fill-opacity', attr(node, 'fillAlpha', 1));
                    path.setAttribute('fill-rule', rule);
                    current.appendChild(path);
                }
            }
        }
    }
    children(vector, svg);
    return new XMLSerializer().serializeToString(svg);
}

async function vectorAlpha(text, size) {
    return sharp(Buffer.from(vectorPreview(text, size))).ensureAlpha().extractChannel('alpha').raw().toBuffer();
}

module.exports = { vectorPreview, vectorAlpha };
