# Android brand artwork

`BrandConfig` in `plugins/src/main/kotlin/poopak.android-brands.gradle.kts` is the
single registry. Only `apHoopoe` opts in with `centralizedBranding = true`.
The other 18 flavors retain their legacy launcher/UI resources, notification icon,
application IDs, Firebase files, hosts and signing configuration.

## Three canonical files

```
branding/<flavor>/
├── logo.svg
├── monochrome.xml
└── notification.xml
```

All three files are mandatory for enrolled flavors, for every build type. Replace
these files with reviewed artwork; do not maintain generated PNGs or Android XML
versions of the colour logo. To migrate another flavor, add its three files and
set `centralizedBranding = true` in its registry entry. No per-flavor generator or
Kotlin screen is needed. Registry rendering options control background and extent.

- `logo.svg` remains SVG to preserve gradients, colours, geometry and transparency.
  Supply self-contained artwork with a positive `viewBox`; external references,
  embedded images, fonts, scripts and unsupported structures fail explicitly.
- `monochrome.xml` is the supplied Android VectorDrawable for the adaptive icon's
  monochrome layer. Internal transparent cutouts remain intact. The generated layer centers and scales
  the supplied viewport inside the 65dp adaptive safe circle, without editing its paths. No inferred alpha
  silhouette or generic fallback is substituted. Android applies themed-icon tint.
- `notification.xml` is an independent, tintable VectorDrawable for notification
  small icons. Use black/white paths on a transparent background, with readable
  geometry at 24dp. Gradients, colour references and opaque background tiles are
  unsuitable. Android controls tint; launcher artwork cannot serve this purpose.

Hoopoe's initial notification artwork is derived from the supplied monochrome
silhouette, with unchanged paths, square viewport and 24dp intrinsic bounds. It
is **not independently approved notification artwork** and remains replaceable.
The supplied `logo.svg` and `monochrome.xml` are preserved. The current supplied SVG
has different gradient definitions from the older `e2edb6306c` rendering baseline;
`tools/branding/test-fixtures/hoopoe-baseline.json` records both versions explicitly.
Future artwork changes require reviewing and updating the current canonical XML
hash and decoded RGBA pixel baselines together; never refresh them automatically
just to make a failing regression test pass.

## Automatic generation

Gradle's Node plugin 7.1.0 manages Node **24.12.0** and bundled npm **11.6.2** under
`.gradle/branding-node`. Locked Sharp/xmldom/Acorn dependencies install under
`build/brandingTools`; no global packages or runtime dependencies are added.
Sync does not download tooling. First generation needs network access; subsequent
unchanged builds reuse outputs.

`generate<Flavor>BrandIcons` owns `app/build/generated/brandIcons/<flavor>`:

- Five 108dp adaptive foreground PNGs: 108/162/216/324/432px for mdpi through xxxhdpi.
  The SVG renderer preserves aspect ratio and fits paint inside the 65dp safe circle.
- One 440px in-app logo, shared by light/dark onboarding and classic login through
  existing resource aliases.
- Adaptive launcher XML, background/round aliases and the XML monochrome layer.
  Round launcher icons use the same adaptive artwork with the launcher's mask.
- `drawable/ic_notification.xml`, copied from the dedicated notification source.

The generated application resource overlay takes precedence over the shared
library's `ic_notification`, so message, incoming-call, call foreground and push
fetch foreground notifications keep their existing Kotlin resource references.
Specialized actions and live-location status icons keep their semantic artwork.
There is no Firebase default-notification-icon metadata in the current manifests;
application-managed notifications use the shared resource reference.

The same SVG supplies embedded Element Call branding under
`app/build/generated/brandAssets/<flavor>`. Acorn replaces exactly the reviewed
logo component in the pinned embedded-call AAR, retaining its frame, properties
and ref. An unexpected AAR structure fails explicitly. The generated bundle,
SVG and valid empty source map replace the obsolete map containing Element artwork.
Call signalling/media code is unchanged.

Generated resources/assets are registered with AGP `addGeneratedSourceDirectory`.
Debug, release and nightly share the selected brand's outputs. Tasks declare all
three artwork inputs, tooling, renderer platform, registry options and AAR inputs;
configuration/build caches remain supported. Source verification and artwork
validation are separate from rendering. One shared generator keeps ownership
simple: changing notification XML reruns generation, but byte comparisons preserve
all unchanged launcher/UI/call output files and timestamps. Failed validation
leaves previous outputs intact. Do not commit anything under `build/generated`.
Adaptive launcher resources require minSdk >= 26 (current FOSS minSdk is 28).

## Verification

```
npm ci --prefix tools/branding --ignore-scripts
node --test tools/branding/generate-icons.test.cjs
node tools/branding/verify-source.cjs .
./gradlew :app:installBrandingTools :app:validateAllBrandArtwork
python3 tools/branding/test-gradle-integration.py
./gradlew :app:assembleApHoopoeDebug --stacktrace
./gradlew :app:verifyApHoopoeDebugBrandApk
./gradlew :app:assembleApPoopakDebug :app:assembleApMscDebug --continue --stacktrace
```

Assemble/bundle tasks verify every ABI APK or the AAB base module using AGP AAPT2.
The verifier independently compiles generated resources, compares compiled themed
and notification vectors, checks all foreground/logo density configurations and
bytes, follows launcher/round/login aliases, rejects night overrides and known
Element assets, and compares embedded-call assets. This verifies actual resource
resolution rather than assuming overlay priority. The shared upstream notification
fallback is allowed only at its original source path; it must never resolve in an
enrolled package. Renamed/copied Element artwork is still rejected.

Integration tests exercise missing/malformed XML, notification-only edits, unchanged
outputs, build/configuration caches, FOSS dependencies and all 19 registrations.
The CI branding workflow validates sources, runs these tests, builds Hoopoe Debug,
checks compiled packages and uploads APKs only after successful verification.
Runtime launcher masks, themed-icon settings and small-icon legibility still require
an isolated emulator/device review. A local notification does not test push delivery.

## Upstream compatibility and FOSS

Brand-specific changes belong in `branding/`, tooling and the convention plugin.
The shared notification resource retains its upstream implementation; changing a
brand requires no notification business-logic edits. Existing UI customization is
limited to one resource reference in the upstream `ElementLogoAtom` and classic
login resource aliases; onboarding keeps the original upstream API and source. The app manifest
needs launcher placeholders, and `app/build.gradle.kts` applies the convention and
FOSS dependencies. No Matrix SDK, networking, authentication or signing changes are
required to replace artwork.

All 19 flavors use `features.enterprise.impl-foss`. Private `:enterprise:` projects
are excluded from settings; dependency verification rejects them. The public
`appicon.enterprise` resource module remains only for the 18 legacy flavors.
Hoopoe depends on neither upstream appicon module. No private deployment key or
submodule checkout is required. Existing release lint checks remain enabled;
well-known discovery, placeholder Firebase configuration and Persian translation
lint problems are separate from branding.
