# Generated flavor metadata and lint

`poopak.android-brands.gradle.kts` remains the single registry for all 19 flavors.
It generates four string resources from each brand's existing host configuration:

| Resource | Consumer or purpose |
| --- | --- |
| `brand_firebase_push_gateway` | `BrandFirebaseGatewayProvider` reads the library's `R.string` with the application context. Firebase registration, unregistration, push configuration and installation ID rotation use this provider. The app override replaces the library's empty default with `https://<backendHost>/_matrix/push/v1/notify`. |
| `brand_backend_host` | Intentional generated host metadata, with no current resource consumer. `PoopakDefaultHomeserverProvider` uses the equivalent `BuildConfig.BACKEND_HOST`. |
| `brand_services_host` | Intentional generated host metadata, with no current resource consumer. The registry also generates `BuildConfig.SERVICES_HOST`. |
| `brand_web_host` | Intentional generated host metadata, with no current resource consumer. The registry also generates `BuildConfig.WEB_HOST`. |

The gateway's `UnusedResources` diagnostic is a cross-module false positive with
the existing `checkDependencies = false` lint configuration. The other three
diagnostics correctly identify strings without current runtime consumers; these
metadata declarations are deliberately retained rather than changing the flavor
registry or its exported resources during a lint fix. Release resource shrinking
already removes the three unused host strings and retains the gateway. A lint
exception does not affect resource shrinking or add resource keep rules.

The shared lint configuration is explicitly selected by `defaultLintOptions`.
Four anchored message expressions in its existing `UnusedResources` rule match
only these exact string names. Severity remains `error`; other resources, similar
names, and other issue IDs are unaffected. Matching the diagnostic rather than
`app/build.gradle.kts` is necessary because lint attributes every generated
`resValue` to that file, including unrelated resources. A changed diagnostic
format fails closed and requires review instead of hiding additional findings.

Validation of this exception must include full `:app:lintApHoopoeRelease`, not
just `lintVital`, and a temporary unused string in an ordinary app `values` file
(for example `brand_backend_host_lint_probe`). It must still produce an
`UnusedResources` error; remove the fixture and rerun full lint afterward.
Inspect actual APK resource tables with `aapt2 dump resources` to confirm the
nonempty gateway and the unchanged presence/absence and values of all four
resources. Compare all 19 linked flavor manifests/configurations with the
registry; successful icon generation alone cannot validate push configuration.

The custom branding workflow runs full Hoopoe Release lint and the Persian
overlay verifier tests in addition to its existing branding, FOSS and style
checks. No application logic, host values, Firebase configuration, translations,
artwork, signing configuration or dependency versions are changed by this policy.
