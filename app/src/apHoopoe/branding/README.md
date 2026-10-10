# Hoopoe launcher icon

`hoopeo_usa_refactored.svg` is an unchanged copy of the supplied logo. SHA-256:
`a7c2d0119f23767d313b4a5fec1b5d92ceb5b434cc4d0138a570f94056ce032a`.

Only `app/src/apHoopoe/res` contains the launcher overrides. Android merges this
flavor's resources ahead of the `appicon` library resources for both debug and
release. The existing manifest names, flavor configuration, shared icon layers,
splash resources, and other flavors are unchanged.

## Generation

Run `node app/src/apHoopoe/branding/generate-icons.cjs` with `sharp` available to
Node (for example, set `NODE_PATH` to a tooling installation's `node_modules`).
This is an offline asset-generation tool; it adds no Gradle or app dependency.
These assets were generated with sharp 0.35.5, libvips 8.18.7, and librsvg 2.63.2.

- Render the original SVG with librsvg at four times each output resolution,
  then downsample using Lanczos3 into lossless RGBA PNGs. No gradient, color,
  path geometry, or aspect-ratio changes are made to the color artwork.
- Adaptive foreground: 108 dp square, with the complete logo centered at
  60 dp high and approximately 51.37 dp wide. Every painted pixel fits inside
  the central 66 dp diameter safe circle. The background is solid white.
- Legacy icons: 48 dp square with a centered 40 dp high logo. The normal icon
  has an opaque white square background; the round icon has a white circular
  background with antialiased transparent corners. The logo is never cropped.
- Foreground PNG sizes (mdpi through xxxhdpi): 108, 162, 216, 324, and 432 px.
  Both legacy icon sizes: 48, 72, 96, 144, and 192 px.
- Android 13 monochrome: a separate 108 dp vector using the original paths and
  adaptive placement. The red ring and red/white bird areas form one opaque
  color; the navy field and eye become transparent cutouts. There are no color
  gradients in this layer; Android supplies the theme tint.
- Both adaptive XML resources reference the same Hoopoe foreground, background,
  and monochrome layers. The layer names include `hoopoe` so existing shared
  layers remain available to other consumers.

Android references: [adaptive icon requirements](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)
and [source-set resource precedence](https://developer.android.com/build/build-variants#sourcesets).
