"""Regression tests for the guard on cross-module Persian resource overlays."""
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

    def check(self, source, translated, projects=(':libraries:strings',)):
        self.default.write_text(f'<resources>{source}</resources>')
        self.overrides.write_text(f'<resources>{translated}</resources>')
        return validator.verify(self.root, self.overrides, [self.default], set(projects))

    def test_library_default_and_reordered_arguments_are_valid(self):
        result = self.check('<string name="label">%1$s: %2$d</string>', '<string name="label">%2$d: %1$s</string>')
        self.assertEqual(result[0]['defaults'], ['libraries/strings/src/main/res/values/default.xml'])

    def test_missing_default_is_rejected(self):
        with self.assertRaisesRegex(AssertionError, 'No unqualified default'):
            self.check('<string name="other">value</string>', '<string name="label">value</string>')

    def test_default_in_inactive_dependency_is_rejected(self):
        with self.assertRaisesRegex(AssertionError, 'active project dependencies'):
            self.check('<string name="label">value</string>', '<string name="label">value</string>', projects=(':app',))

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
        with self.assertRaisesRegex(AssertionError, 'Duplicate Persian override'):
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


if __name__ == '__main__':
    unittest.main()
