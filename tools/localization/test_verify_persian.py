"""Regression tests for variant-aware Persian resource coverage."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('verify_persian', Path(__file__).with_name('verify-persian.py'))
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)


class PersianOverridesTest(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        self.default = self.root / 'libraries/strings/src/main/res/values/default.xml'
        self.default.parent.mkdir(parents=True)
        self.overrides = self.root / 'app/src/main/res/values-fa/persian_overrides.xml'
        self.overrides.parent.mkdir(parents=True)
        self.layers = {
            'app/src/main/res': (':app', 0),
            'libraries/strings/src/main/res': (':libraries:strings', 0),
        }

    def check(self, source, translated, extra=None, exclusions=None):
        self.default.write_text(f'<resources>{source}</resources>')
        self.overrides.write_text(f'<resources>{translated}</resources>')
        files = [self.default, self.overrides]
        for relative, elements in (extra or {}).items():
            file = self.root / relative
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text(f'<resources>{elements}</resources>')
            files.append(file)
        return validator.verify(self.root, self.overrides, files, self.layers, exclusions or {})

    def test_library_default_and_reordered_arguments_are_valid(self):
        result = self.check('<string name="label">%1$s: %2$d</string>', '<string name="label">%2$d: %1$s</string>')
        self.assertEqual(result[0]['defaults'], ['libraries/strings/src/main/res/values/default.xml'])

    def test_missing_default_is_rejected(self):
        with self.assertRaisesRegex(AssertionError, 'No unqualified default'):
            self.check('<string name="other">value</string>', '<string name="label">value</string>')

    def test_default_in_inactive_dependency_is_rejected(self):
        self.layers.pop('libraries/strings/src/main/res')
        with self.assertRaisesRegex(AssertionError, 'active project dependencies'):
            self.check('<string name="label">value</string>', '<string name="label">مقدار</string>')

    def test_qualified_default_does_not_count_as_fallback(self):
        self.default = self.root / 'libraries/strings/src/main/res/values-en/default.xml'
        self.default.parent.mkdir(parents=True)
        with self.assertRaisesRegex(AssertionError, 'No unqualified default'):
            self.check('<string name="label">value</string>', '<string name="label">value</string>')

    def test_missing_or_incompatible_arguments_are_rejected(self):
        for text in ('value', '%1$d', '%2$s'):
            with self.subTest(text=text), self.assertRaisesRegex(AssertionError, 'Format arguments differ'):
                self.check('<string name="label">%1$s</string>', f'<string name="label">{text}</string>')

    def test_formatter_indices_reuse_dates_and_literal_percent(self):
        self.check('<string name="label">%1$s %s %&lt;s %%</string>', '<string name="label">%1$s %%</string>')
        self.check('<string name="label">%1$tY %1$tm</string>', '<string name="label">%1$tm %1$tY</string>')
        with self.assertRaisesRegex(AssertionError, 'Format arguments differ'):
            self.check('<string name="label">%1$s %s</string>', '<string name="label">%1$s %2$s</string>')

    def test_duplicate_override_is_rejected(self):
        with self.assertRaisesRegex(AssertionError, 'Duplicate resource :app/string/label'):
            self.check('<string name="label">value</string>', '<string name="label">value</string>' * 2)

    def test_non_translatable_default_is_rejected(self):
        with self.assertRaisesRegex(AssertionError, 'non-translatable default'):
            self.check('<string name="label" translatable="false">value</string>', '<string name="label">value</string>')

    def test_plural_fallback_and_all_quantities_are_checked(self):
        source = '<plurals name="label"><item quantity="other">%1$d items</item></plurals>'
        self.check(source, '<plurals name="label"><item quantity="one">%1$d item</item><item quantity="other">%1$d items</item></plurals>')
        with self.assertRaisesRegex(AssertionError, 'Missing other plural fallback'):
            self.check(source, '<plurals name="label"><item quantity="one">%1$d item</item></plurals>')
        with self.assertRaisesRegex(AssertionError, 'Format arguments differ'):
            self.check(source, '<plurals name="label"><item quantity="one">%1$s item</item><item quantity="other">%1$d items</item></plurals>')

    def test_missing_string_and_plural_report_owner_and_file(self):
        defaults = (
            '<string name="label">Label</string>'
            '<string name="missing_string">Missing string</string>'
            '<plurals name="missing_plural"><item quantity="other">%1$d items</item></plurals>'
        )
        with self.assertRaises(AssertionError) as error:
            self.check(defaults, '<string name="label">عنوان</string>')
        message = str(error.exception)
        for key in ('string/missing_string', 'plurals/missing_plural'):
            self.assertIn(f':libraries:strings/{key} (libraries/strings/src/main/res/values/default.xml)', message)

    def test_module_translation_and_item_alias_syntax_count(self):
        extra = {'libraries/strings/src/main/res/values-b+fa/translations.xml': '<string name="hint">راهنما</string>'}
        result = self.check(
            '<string name="label">Label</string><item type="string" name="hint">Hint</item>',
            '<string name="label">عنوان</string>',
            extra,
        )
        self.assertEqual({entry['name'] for entry in result}, {'label', 'hint'})

    def test_module_translation_must_have_compatible_formats(self):
        extra = {'libraries/strings/src/main/res/values-fa/translations.xml': '<string name="hint">راهنمای %2$s</string>'}
        with self.assertRaisesRegex(AssertionError, 'Format arguments differ: :libraries:strings/string/hint'):
            self.check('<string name="label">Label</string><string name="hint">Hint %1$s</string>', '<string name="label">عنوان</string>', extra)

    def test_app_translation_takes_precedence_over_library_translation(self):
        extra = {'libraries/strings/src/main/res/values-fa/translations.xml': '<string name="label">عنوان %2$s</string>'}
        self.check('<string name="label">Label %1$s</string>', '<string name="label">عنوان %1$s</string>', extra)

    def test_duplicate_persian_definitions_in_multiple_files_at_same_priority_fail(self):
        extra = {'app/src/main/res/values-fa/other.xml': '<string name="label">عنوان</string>'}
        with self.assertRaisesRegex(AssertionError, r'Duplicate resource :app/string/label.*other.xml.*persian_overrides.xml'):
            self.check('<string name="label">Label</string>', '<string name="label">عنوان</string>', extra)

    def test_active_source_sets_can_override_main_and_inactive_sources_are_ignored(self):
        self.layers['app/src/brandDebug/res'] = ':app', 1
        extra = {
            'app/src/brandDebug/res/values/strings.xml': '<string name="title">Brand title</string>',
            'app/src/main/res/values/strings.xml': '<string name="title">App title</string>',
            'app/src/brandDebug/res/values-fa/strings.xml': '<string name="title">عنوان برند</string>',
            'app/src/otherBrand/res/values/strings.xml': '<string name="other_brand">Other brand</string>',
            'app/src/test/res/values/strings.xml': '<string name="test_only">Test only</string>',
        }
        result = self.check('<string name="label">Label</string>', '<string name="label">عنوان</string>', extra)
        self.assertIn({'type': 'string', 'name': 'title', 'defaults': ['app/src/brandDebug/res/values/strings.xml']}, result)

    def test_inactive_persian_value_cannot_hide_missing_active_translation(self):
        extra = {'app/src/otherBrand/res/values-fa/strings.xml': '<string name="title">عنوان</string>'}
        with self.assertRaisesRegex(AssertionError, 'Missing Persian translation: :libraries:strings/string/title'):
            self.check('<string name="label">Label</string><string name="title">Title</string>', '<string name="label">برچسب</string>', extra)

    def test_non_translatable_and_documented_exclusion_do_not_require_persian(self):
        self.check(
            '<string name="label">Label</string><string name="brand">Brand</string>'
            '<string name="technical" translatable="false">example.net</string>',
            '<string name="label">عنوان</string>',
            exclusions={':libraries:strings/string/brand': 'Product name'},
        )

    def test_blank_and_english_values_do_not_count_as_translated(self):
        for value, message in (('""', 'Empty Persian value'), ('Label', 'Persian value repeats the default')):
            with self.subTest(value=value), self.assertRaisesRegex(AssertionError, message):
                self.check('<string name="label">Label</string>', f'<string name="label">{value}</string>')

    def test_malformed_percent_is_rejected_in_formatted_translation(self):
        with self.assertRaisesRegex(AssertionError, 'Unescaped percent'):
            self.check('<string name="label">Loading %1$d%%</string>', '<string name="label">بارگیری %1$d%</string>')


if __name__ == '__main__':
    unittest.main()
