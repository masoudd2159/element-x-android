const fs = require('node:fs');
const path = require('node:path');
const sharp = require('sharp');
const root = path.resolve(__dirname, '..');
const svg = fs.readFileSync(path.join(root, 'branding/hoopeo_usa_refactored.svg'), 'utf8');
const content = svg.replace(/^<svg[^>]*>/, '').replace(/<\/svg>\s*$/, '');
const background = '#FFFFFF';
function imageSvg(canvas, logoHeight, round = false, transparent = false) {
    const logoWidth = logoHeight * 1185 / 1384;
    const x = (canvas - logoWidth) / 2;
    const y = (canvas - logoHeight) / 2;
    const bg = transparent ? '' : round
        ? `<circle cx="${canvas / 2}" cy="${canvas / 2}" r="${canvas / 2}" fill="${background}"/>`
        : `<rect width="${canvas}" height="${canvas}" fill="${background}"/>`;
    return `<svg xmlns="http://www.w3.org/2000/svg" width="${canvas}" height="${canvas}" viewBox="0 0 ${canvas} ${canvas}">${bg}<svg x="${x}" y="${y}" width="${logoWidth}" height="${logoHeight}" viewBox="0 0 1185 1384">${content}</svg></svg>`;
}
async function render(svgText, size, output) {
    // Render at 4x the final density with librsvg, then downsample with Lanczos3.
    await sharp(Buffer.from(svgText), { density: 72 * size / Number(svgText.match(/width="([\d.]+)"/)[1]) * 4 })
        .resize(size, size, { kernel: 'lanczos3' }).png({ compressionLevel: 9 }).toFile(output);
}
(async () => {
    for (const [density, scale] of [['mdpi', 1], ['hdpi', 1.5], ['xhdpi', 2], ['xxhdpi', 3], ['xxxhdpi', 4]]) {
        const dir = path.join(root, 'res', `mipmap-${density}`);
        fs.mkdirSync(dir, { recursive: true });
        await render(imageSvg(108, 60, false, true), 108 * scale, path.join(dir, 'ic_launcher_hoopoe_foreground.png'));
        await render(imageSvg(48, 40), 48 * scale, path.join(dir, 'ic_launcher.png'));
        await render(imageSvg(48, 40, true), 48 * scale, path.join(dir, 'ic_launcher_round.png'));
    }
    writeXmlResources();
    console.log('Generated 15 density-specific PNG resources using sharp', sharp.versions.sharp, '/ librsvg', sharp.versions.rsvg);
})().catch(error => {
    console.error(error);
    process.exitCode = 1;
});

function writeXmlResources() {
    const res = path.join(root, 'res');
    for (const dir of ['mipmap-anydpi-v26', 'drawable', 'values']) {
        fs.mkdirSync(path.join(res, dir), { recursive: true });
    }
    const adaptive = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_hoopoe_background" />
    <foreground android:drawable="@mipmap/ic_launcher_hoopoe_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_hoopoe_monochrome" />
</adaptive-icon>
`;
    for (const name of ['ic_launcher', 'ic_launcher_round']) {
        fs.writeFileSync(path.join(res, 'mipmap-anydpi-v26', `${name}.xml`), adaptive);
    }
    fs.writeFileSync(path.join(res, 'values/ic_launcher_hoopoe_background.xml'), `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_hoopoe_background">#FFFFFF</color>
</resources>
`);
    // Extract unchanged path geometry from this source SVG; no path approximation.
    const paths = Object.fromEntries([...svg.matchAll(/<path d="([^"]+)"[^>]* id="([^"]+)"/g)].map(m => [m[2], m[1]]));
    const monoPaths = [paths['outer-silhouette'] + ' ' + paths['inner-circle'],
        ...['beak-and-body', 'lower-body', 'back-feathers', 'middle-feathers', 'front-feathers'].map(id => paths[id])];
    if (Object.keys(paths).length !== 8 || monoPaths.some(d => !d) || !paths.eye) {
        throw new Error('The source SVG path structure has changed; review the monochrome conversion.');
    }
    const scale = 60 / 1384;
    const x = (108 - 1185 * scale) / 2;
    const vector = `<?xml version="1.0" encoding="utf-8"?>
<!-- Source geometry: red/white areas are opaque; the navy field and eye are cutouts. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="${scale.toFixed(12)}"
        android:scaleY="${scale.toFixed(12)}"
        android:translateX="${x.toFixed(12)}"
        android:translateY="24">
        <clip-path
            android:fillType="evenOdd"
            android:pathData="M0 0H1185V1384H0Z ${paths.eye}" />
${monoPaths.map((d, i) => `        <path
            android:fillColor="#000000"${i === 0 ? '\n            android:fillType="evenOdd"' : ''}
            android:pathData="${d}" />`).join('\n')}
    </group>
</vector>
`;
    fs.writeFileSync(path.join(res, 'drawable/ic_launcher_hoopoe_monochrome.xml'), vector);
}
