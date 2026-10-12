# Persian cross-module overlays

`app/src/main/res/values-fa/persian_overrides.xml` overrides existing library and
feature resources at the application layer, for every brand. It is not a second
default catalogue. Keep the translations, placeholders and their owners intact;
do not copy English defaults into the app or move translations into upstream modules.

## Why this one file has lint exceptions

The application uses the upstream `checkDependencies=false` configuration.
Android Lint's TranslationDetector excludes non-reporting library resources in
`ignoreFile`; it therefore cannot see library defaults when checking app overlays.
Its reporting code uses `ExtraTranslation` for strings and `MissingDefaultResource`
for plurals. Investigating each category independently found **1,870 strings and
45 plurals, all with unqualified defaults in the actual Hoopoe Release merger**.
None required an invented fallback. Their format argument indices and conversions
match the defaults. Duplicate names already provided by multiple upstream modules
are retained; no new duplicate definition is introduced.

Full release lint additionally treats these app overlays as unused because their
call sites use the owning libraries' R classes. The upstream lint configuration
already permits unused Localazy catalogue entries. The `UnusedResources` exception
here applies only to this mirror of library translations, not generated brand
metadata or other app resources. It also preserves translations whose upstream
catalogue entries are currently unused. It does not assert that every key has a call site.

Only these three checks are suppressed **on this XML file**. All other checks,
Release lint, `abortOnError`, and checks on other files retain their existing behavior.

## The replacement guard

The custom `poopak.android-localization` convention registers
`verify<Variant>PersianTranslations` before variant lint, vital lint, assemble and
bundle tasks. It uses the public AGP runtime configuration and Gradle component
graph to restrict default owners to that variant's active project dependencies.
It requires a matching type/name in `src/main/res/values`, checks every format
argument, requires `other` plural fallbacks, and rejects duplicate Persian keys
and overrides of non-translatable defaults (which `ExtraTranslation` suppression
can also mask in TranslationDetector).
An English-only qualified resource or a default in an inactive project is insufficient.
Changing/removing a default invalidates the task; missing defaults still fail the build.
No APK resources or source sets are rewritten by this check.

Audit receipts under `app/build/localizationVerification/<variant>.json` list every
affected resource's type, exact name and default source files. These are generated
evidence, not source catalogues to maintain or commit. New default resource owners
outside project `src/main/res/values` require explicitly extending the validator;
failure is preferable to silently granting another lint exception.

```
python3 -m unittest discover -s tools/localization -p 'test_*.py'
./gradlew :app:verifyApHoopoeReleasePersianTranslations --configuration-cache
./gradlew :app:lintApHoopoeRelease :app:assembleApHoopoeRelease
```

The lint implementation is documented in the upstream
[TranslationDetector source](https://android.googlesource.com/platform/tools/base/+/mirror-goog-studio-main/lint/libs/lint-checks/src/main/java/com/android/tools/lint/checks/TranslationDetector.kt).
The audit additionally needs actual merged/packaged resource validation when changing
the overlay mechanism; a source-level match alone does not establish overlay precedence.
