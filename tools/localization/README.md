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
for plurals. Every overlay must have an unqualified default in an active project
and compatible format arguments. A shared Localazy key provided by multiple
upstream modules needs just one app overlay.

Full release lint additionally treats these app overlays as unused because their
call sites use the owning libraries' R classes. The upstream lint configuration
already permits unused Localazy catalogue entries. The `UnusedResources` exception
here applies only to this mirror of library translations, not generated brand
metadata or other app resources. It also preserves translations whose upstream
catalogue entries are currently unused. It does not assert that every key has a call site.

Only these three checks are suppressed **on this XML file**. All other checks,
Release lint, `abortOnError`, and checks on other files retain their existing behavior.

## Variant coverage and overlay guard

The custom `poopak.android-localization` convention registers
`verify<Variant>PersianTranslations` before variant lint, vital lint, assemble and
bundle tasks. The existing branding workflow therefore verifies coverage for
both brands on the same jobs that assemble their APKs.

- The app uses the configured Android source set directories for the selected
  flavor, build type and variant. Libraries use the selected Android variants
  from Gradle's runtime dependency graph, including build-type fallbacks, and
  their conventional `src/<sourceSet>/res` directories.
- Each translatable `string` and `plurals` entry in unqualified `values` needs a
  translation in `values-fa` or its equivalent `values-b+fa`. Persian resources
  in the app take precedence over library values. Higher-priority source sets
  within each project take precedence over its `main` resources.
- The guard reports missing translations with the resource type, key, owning
  module and default XML file. It checks formatted argument indices and
  conversions, rejects blank replacements of nonempty text and checks plural
  quantities and `other` fallbacks. Duplicate definitions at the same source set
  priority fail validation. Existing shared Localazy keys can have multiple
  owners and one merged Persian value.
- `translatable="false"` excludes technical values and brand names. The
  [exact-key exceptions](persian-exclusions.json) document locale-neutral text
  whose spelling is intentionally shared with the default. Keep exceptions
  specific to the owning module and include a reason.

Inactive dependencies, test source sets, generated resources, external AARs
and configuration-qualified resources beyond the base Persian locale are
outside the source guard. If a library configures custom resource directories,
extend the source directory collection when introducing them. Hardcoded UI copy
still requires source review and Android Lint.

Audit receipts under `app/build/localizationVerification/<variant>.json` list
checked keys, their types and their active default XML files. Android's resource
merger and AAPT2 can independently verify that the expected Persian value is
packaged for each brand.

```
python3 -m unittest discover -s tools/localization -p 'test_*.py'
./gradlew :app:verifyApPoopakDebugPersianTranslations :app:verifyApHoopoeDebugPersianTranslations
./gradlew :app:verifyApHoopoeReleasePersianTranslations --configuration-cache
./gradlew :app:lintApHoopoeRelease :app:assembleApHoopoeRelease
```

The lint implementation is documented in the upstream
[TranslationDetector source](https://android.googlesource.com/platform/tools/base/+/mirror-goog-studio-main/lint/libs/lint-checks/src/main/java/com/android/tools/lint/checks/TranslationDetector.kt).
The audit additionally needs actual merged/packaged resource validation when changing
the overlay mechanism; a source-level match alone does not establish overlay precedence.
