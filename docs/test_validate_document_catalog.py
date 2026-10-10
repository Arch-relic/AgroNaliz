"""Regression checks for the supplied batch and PDF-independent index validation."""
import copy
import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location('catalog_validator', ROOT / 'docs/validate-document-catalog.py')
validator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validator)


class CatalogChecksumTest(unittest.TestCase):
    def setUp(self):
        self.text = (ROOT / 'docs/pdf-sha256-local.txt').read_text(encoding='utf-8')
        self.hashes = validator.read_pdf_hashes(self.text)
        self.documents = json.loads((ROOT / 'app/src/main/assets/documents/catalog.json').read_text(encoding='utf-8'))['documents']

    def test_real_batch_maps_all_twenty_full_paths(self):
        validator.verify_pdf_hash_mapping(self.documents, self.hashes)
        self.assertEqual(20, len(self.hashes))

    def test_manifest_rejects_bad_hash_format(self):
        digest = self.text.split()[0]
        for bad in ('', digest[:-1], digest + '0', digest.upper(), 'g' * 64):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                validator.read_pdf_hashes(self.text.replace(digest, bad, 1))

    def test_manifest_rejects_duplicate_paths_and_digests(self):
        lines = self.text.splitlines()
        with self.assertRaises(ValueError):
            validator.read_pdf_hashes('\n'.join(lines[:-1] + [lines[0]]))
        last_path = lines[-1].split('  ')[1]
        with self.assertRaises(ValueError):
            validator.read_pdf_hashes('\n'.join(lines[:-1] + [lines[0].split('  ')[0] + '  ' + last_path]))

    def test_manifest_requires_exactly_twenty(self):
        with self.assertRaises(ValueError):
            validator.read_pdf_hashes('\n'.join(self.text.splitlines()[:-1]))
        with self.assertRaises(ValueError):
            validator.read_pdf_hashes(self.text + '0' * 64 + '  app/src/main/assets/manuals/extra.pdf\n')

    def test_manifest_rejects_shortened_and_unsafe_paths(self):
        path = self.text.splitlines()[0].split('  ')[1]
        for bad in ('manuals/engines/ymz_5340_536_diagnostics.pdf', path.replace('/engines/', '/../'), path.replace('/engines/', '//'), path.replace('/', '\\'), path.replace('/engines/', '/./')):
            with self.subTest(path=bad), self.assertRaises(ValueError):
                validator.read_pdf_hashes(self.text.replace(path, bad, 1))

    def test_mapping_rejects_missing_malformed_and_swapped_hashes(self):
        for bad in (None, '', 'A' * 64, '0' * 64, self.documents[1]['pdfSha256']):
            documents = copy.deepcopy(self.documents)
            documents[0]['pdfSha256'] = bad
            with self.subTest(hash=bad), self.assertRaises(ValueError):
                validator.verify_pdf_hash_mapping(documents, self.hashes)

    def test_mapping_rejects_missing_duplicate_and_unmapped_paths(self):
        with self.assertRaises(ValueError):
            validator.verify_pdf_hash_mapping(self.documents[:-1], self.hashes)
        with self.assertRaises(ValueError):
            validator.verify_pdf_hash_mapping(self.documents[:-1] + [self.documents[0]], self.hashes)
        documents = copy.deepcopy(self.documents)
        documents[0]['sources'][0]['pdfPath'] = 'manuals/other/nova_340_manual.pdf'
        with self.assertRaises(ValueError):
            validator.verify_pdf_hash_mapping(documents, self.hashes)

    def test_mapping_is_independent_of_manifest_order(self):
        validator.verify_pdf_hash_mapping(self.documents, validator.read_pdf_hashes('\n'.join(reversed(self.text.splitlines()))))


class IndexStructureTest(unittest.TestCase):
    def setUp(self):
        self.index = {'version': 4, 'coordinateSystem': 'normalized_page', 'pages': [
            {'page': 1, 'text': 'a😀', 'words': [{'start': 1, 'end': 3, 'x0': 0, 'x1': 1, 'y0': 0, 'y1': 1}]}]}

    def test_uses_java_utf16_offsets(self):
        self.assertEqual((1, 1, 1), validator.verify_index_structure(self.index, 'fixture'))

    def test_rejects_format_and_empty_pages(self):
        for field, value in [('version', 3), ('coordinateSystem', 'pixels'), ('pages', [])]:
            index = copy.deepcopy(self.index)
            index[field] = value
            with self.subTest(field=field), self.assertRaises(ValueError):
                validator.verify_index_structure(index, 'fixture')

    def test_rejects_invalid_and_duplicate_pages(self):
        for number in (0, -1, True, 1.5):
            index = copy.deepcopy(self.index)
            index['pages'][0]['page'] = number
            with self.subTest(number=number), self.assertRaises(ValueError):
                validator.verify_index_structure(index, 'fixture')
        self.index['pages'] *= 2
        with self.assertRaises(ValueError):
            validator.verify_index_structure(self.index, 'fixture')

    def test_rejects_offsets_and_non_normalized_coordinates(self):
        for field, value in [('start', -1), ('end', 4), ('end', 1), ('start', True), ('x0', -0.1), ('x0', 2), ('x1', 1.1), ('x1', -0.1), ('y0', float('nan')), ('y1', float('inf')), ('x0', True)]:
            index = copy.deepcopy(self.index)
            index['pages'][0]['words'][0][field] = value
            with self.subTest(field=field, value=value), self.assertRaises(ValueError):
                validator.verify_index_structure(index, 'fixture')

    def test_accepts_pages_without_words_without_claiming_pdf_coverage(self):
        self.index['pages'][0].pop('words')
        self.index['pages'][0]['page'] = 100
        self.assertEqual((1, 0, 100), validator.verify_index_structure(self.index, 'fixture'))


if __name__ == '__main__':
    unittest.main()
