# Branding regression fixtures

`hoopoe-baseline.json` pins the existing launcher SVG and decoded foreground pixels.
The notification entry separately pins the VectorDrawable already present at
`c7fdfa1ebb0949bcec7b588c51271924745f3414`. This is regression protection for the
existing artwork, not approval or replacement of that artwork, and does not require
its paths to match the themed launcher.

The notification SHA-256 hashes UTF-8 `JSON.stringify` of each element as
`[localName, sortedAndroidAttributes, orderedElementChildren]`. Attributes are
`[localName, value]` pairs in the Android namespace. XML formatting, comments and
namespace prefixes are ignored; viewport, intrinsic size, paths, fills, transforms,
alpha and clip order remain protected. It is independent of rasterizer/platform
pixel encodings. Do not refresh the hash automatically after a failed test. An
intentional notification artwork change requires an explicit review and an update
of this independent baseline and its cutout probes.

`vector-preview.cjs` is test-only. It converts the validated tintable vector subset
to SVG using the actual viewport, nested pivot/scale/rotation/translation, fill
rules, fill alpha and scoped clips. Tests compare these semantics with explicit
equivalent shapes, following the [Android VectorDrawable contract](https://developer.android.com/reference/android/graphics/drawable/VectorDrawable).
The existing notification is rendered at 24dp for all five
densities (24/36/48/72/96px). The original coverage floors remain 70 visible and
200 fully transparent pixels at mdpi, proportional to area at higher densities.
Separate probes check the exterior and two interior cutouts. Negative controls
prove that invisible, tiny, fully opaque and filled-cutout mutations are rejected.

This preview checks geometry and alpha, not Android's device-specific tint or
antialiasing. Gradle package verification separately compares every compiled vector
attribute with canonical generated resources in the actual APK/AAB.
