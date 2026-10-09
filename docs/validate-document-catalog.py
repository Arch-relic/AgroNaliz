#!/usr/bin/env python3
"""Validate the real catalog and legacy references using only Python's stdlib."""
import hashlib
import json
import re
from pathlib import Path


def require(condition, message):
    if not condition:
        raise ValueError(message)


def validate():
    root = Path(__file__).resolve().parents[1]
    main = root / 'app/src/main'
    assets = main / 'assets'
    java = main / 'java/ru/fo6osik/workjournal'
    catalog = json.loads((assets / 'documents/catalog.json').read_text(encoding='utf-8'))
    require(catalog['schemaVersion'] == 1, 'Unsupported schema')
    documents = catalog['documents']
    require(len(documents) == 20, 'Expected all 20 legacy documents')
    vehicle = (java / 'VehicleDetailsActivity.kt').read_text(encoding='utf-8')
    bulletins = (java / 'Nova340BulletinsActivity.kt').read_text(encoding='utf-8')
    legacy = dict(re.findall(
        r'"assetPath",\s*"(manuals/[^"\n]+\.pdf)"\s*\)\s*intent.putExtra\(\s*"documentTitle",\s*"([^"\n]+)"', vehicle
    ))
    legacy.update({pdf: title for title, pdf in re.findall(
        r'setupBulletinButton\(R.id.\w+, "([^"\n]+)", "(manuals/[^"\n]+\.pdf)"\)', bulletins
    )})
    require(len(legacy) == 20, 'Legacy reference/title extraction failed')
    all_references = set()
    for source in java.rglob('*.kt'):
        all_references.update(re.findall(r'"(manuals/[^"\n]+\.pdf)"', source.read_text(encoding='utf-8')))
    require(set(legacy) == all_references, 'Unmapped PDF reference in source')
    ids, pdfs, indexes = set(), set(), set()
    for document in documents:
        doc_id = document['documentId']
        require(re.fullmatch(r'[a-z][a-z0-9_]*', doc_id) and doc_id not in ids, f'Invalid/duplicate ID: {doc_id}')
        ids.add(doc_id)
        require(document['title'].strip(), f'Empty title: {doc_id}')
        require(len(document['sources']) == 1, f'Expected one existing asset source: {doc_id}')
        source = document['sources'][0]
        require(source['type'] == 'assets', f'Unexpected source: {doc_id}')
        pdf, index = source['pdfPath'], source['searchIndexPath']
        for path in (pdf, index):
            require(path.startswith('manuals/') and '\\' not in path and ':' not in path
                    and all(part not in ('', '.', '..') for part in path.split('/')), f'Unsafe path: {path}')
        require(pdf.endswith('.pdf') and index == pdf[:-4] + '_search.json', f'Index path mismatch: {doc_id}')
        require(pdf not in pdfs and index not in indexes, f'Duplicate asset path: {doc_id}')
        pdfs.add(pdf)
        indexes.add(index)
        require(legacy.get(pdf) == document['title'], f'Legacy title/path mismatch: {doc_id}')
        for field in ('pdfSha256', 'searchIndexSha256'):
            value = document[field]
            require(value is None or isinstance(value, str) and re.fullmatch(r'[0-9a-f]{64}', value), f'Invalid {field}: {doc_id}')
        revision = document['revision']
        require(revision is None or isinstance(revision, str) and revision.strip(), f'Invalid revision: {doc_id}')
        if not (assets / pdf).exists():
            require(document['pdfSha256'] is None and revision is None, f'Unknown PDF must not have invented metadata: {doc_id}')
        elif document['pdfSha256'] is not None:
            require(hashlib.sha256((assets / pdf).read_bytes()).hexdigest() == document['pdfSha256'], f'PDF hash mismatch: {doc_id}')
        data = (assets / index).read_bytes()
        require(hashlib.sha256(data).hexdigest() == document['searchIndexSha256'], f'Index hash mismatch: {doc_id}')
        parsed = json.loads(data)
        require(parsed['version'] == 4 and parsed['coordinateSystem'] == 'normalized_page', f'Unexpected index format: {doc_id}')
        require(isinstance(parsed['pages'], list) and parsed['pages'], f'Empty index: {doc_id}')
    require(pdfs == all_references, 'Catalog does not cover every PDF reference')
    existing_indexes = {p.relative_to(assets).as_posix() for p in (assets / 'manuals').rglob('*_search.json')}
    require(indexes == existing_indexes, 'Catalog does not cover every existing index')
    print(f'OK: {len(documents)} documents, legacy titles/paths, 20 index SHA-256 values and index formats verified')


if __name__ == '__main__':
    validate()
