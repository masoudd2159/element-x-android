/* Copyright (c) 2026 Element Creations Ltd.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
'use strict';
const acorn = require('acorn');
function walk(node, visit) {
    if (!node || typeof node !== 'object') return;
    if (node.type) visit(node);
    for (const value of Object.values(node)) {
        if (Array.isArray(value)) value.forEach(n => walk(n, visit));
        else if (value && typeof value === 'object') walk(value, visit);
    }
}
const value = node => node?.type === 'Literal' ? node.value : node?.type === 'TemplateLiteral' && !node.expressions.length ? node.quasis[0].value.cooked : null;
function brandCall(source) {
    const replacements = [];
    const ast = acorn.parse(source, { ecmaVersion: 'latest', sourceType: 'module' });
    walk(ast, node => {
        if (node.type !== 'CallExpression' || value(node.arguments[0]) !== 'svg' || node.arguments[1]?.type !== 'ObjectExpression') return;
        let logo = false;
        walk(node.arguments[1], child => {
            if (child.type === 'CallExpression' && value(child.arguments[0]) === 'desc') {
                logo ||= child.arguments[1]?.properties?.some(p => (p.key.name || value(p.key)) === 'children' && value(p.value) === 'Element Call (Beta)');
            }
        });
        if (!logo) return;
        const properties = node.arguments[1].properties.filter(p => p.type === 'SpreadElement' || !['xmlns', 'viewBox', 'fill', 'children'].includes(p.key.name || value(p.key)));
        const retained = properties.map(p => source.slice(p.start, p.end));
        const props = `((p)=>({...p,src:"/element-call/brand-logo.svg",alt:"",children:undefined,style:{...p.style,objectFit:"contain"}}))({${retained.join(',')}})`;
        replacements.push([node.arguments[0].start, node.arguments[1].end, `"img",${props}`]);
    });
    if (replacements.length !== 1) throw Error(`Embedded call branding changed: expected one reviewed logo, found ${replacements.length}. Review the AAR upgrade; no Element fallback is allowed.`);
    const [start, end, replacement] = replacements[0];
    const output = source.slice(0, start) + replacement + source.slice(end);
    acorn.parse(output, { ecmaVersion: 'latest', sourceType: 'module' });
    if (output.includes('Element Call (Beta)')) throw Error('Embedded Element logo remains after transformation');
    return output;
}
module.exports = { brandCall };
