#!/usr/bin/env python3
"""Validate the real catalog and legacy references using only Python's stdlib."""
import hashlib
import json
import math
import re
from pathlib import Path


def require(condition, message):
    if not condition:
        raise ValueError(message)


ASSET_PREFIX = 'app/src/main/assets/'
SHA256 = r'[0-9a-f]{64}'


def read_pdf_hashes(text):
    """Read the supplied sha256sum manifest; never match by basename or order."""
    hashes, digests = {}, set()
    for line in text.splitlines():
        match = re.fullmatch(r'([0-9a-f]{64})  (app/src/main/assets/manuals/[^\\\r\n]+\.pdf)', line)
        require(match is not None, 'Invalid SHA-256 manifest line')
        digest, path = match.groups()
        require(all(part not in ('', '.', '..') for part in path.split('/'))
                and ':' not in path and not any(ord(c) < 32 for c in path), f'Unsafe manifest path: {path}')
        require(path not in hashes, f'Duplicate manifest path: {path}')
        require(digest not in digests, f'Duplicate PDF checksum in this 20-document batch: {path}')
        hashes[path] = digest
        digests.add(digest)
    require(len(hashes) == 20, 'Expected exactly 20 PDF checksums')
    return hashes


def verify_pdf_hash_mapping(documents, hashes):
    paths = []
    for document in documents:
        sources = document['sources']
        require(len(sources) == 1 and sources[0]['type'] == 'assets', 'Expected one asset source')
        path = ASSET_PREFIX + sources[0]['pdfPath']
        require(path in hashes, f'No exact manifest path: {path}')
        require(isinstance(document['pdfSha256'], str)
                and re.fullmatch(SHA256, document['pdfSha256']), f'Missing/invalid PDF checksum: {path}')
        require(document['pdfSha256'] == hashes[path], f'PDF checksum/path mismatch: {path}')
        paths.append(path)
    require(len(paths) == 20 and len(set(paths)) == 20, 'Expected 20 unique catalog PDF paths')
    require(set(paths) == set(hashes), 'Manifest/catalog path sets differ')


def file_sha256(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(8192), b''):
            digest.update(block)
    return digest.hexdigest()


def verify_index_structure(parsed, doc_id):
    """Checks independent of PDF bytes. Text offsets follow Java UTF-16 units."""
    require(type(parsed['version']) is int and parsed['version'] == 4
            and parsed['coordinateSystem'] == 'normalized_page', f'Unexpected index format: {doc_id}')
    pages = parsed['pages']
    require(isinstance(pages, list) and pages, f'Empty index: {doc_id}')
    seen, words_count = set(), 0
    for page in pages:
        number = page['page']
        require(type(number) is int and number > 0 and number not in seen, f'Invalid/duplicate page: {doc_id}')
        seen.add(number)
        require(isinstance(page['text'], str), f'Invalid text: {doc_id}')
        length = len(page['text'].encode('utf-16-le')) // 2
        words = page.get('words', [])
        require(isinstance(words, list), f'Invalid words: {doc_id}')
        for word in words:
            start, end = word['start'], word['end']
            require(type(start) is int and type(end) is int and 0 <= start < end <= length,
                    f'Invalid word offsets: {doc_id}, page {number}')
            coordinates = [word[key] for key in ('x0', 'x1', 'y0', 'y1')]
            require(all(type(v) in (int, float) and math.isfinite(v) for v in coordinates),
                    f'Invalid word coordinates: {doc_id}, page {number}')
            x0, x1, y0, y1 = coordinates
            require(0 <= x0 <= x1 <= 1 and 0 <= y0 <= y1 <= 1,
                    f'Invalid word coordinates: {doc_id}, page {number}')
        words_count += len(words)
    return len(pages), words_count, max(seen)


def validate():
    root = Path(__file__).resolve().parents[1]
    main = root / 'app/src/main'
    assets = main / 'assets'
    java = main / 'java/ru/fo6osik/workjournal'
    catalog = json.loads((assets / 'documents/catalog.json').read_text(encoding='utf-8'))
    require(catalog['schemaVersion'] == 1, 'Unsupported schema')
    documents = catalog['documents']
    hashes = read_pdf_hashes((root / 'docs/pdf-sha256-local.txt').read_text(encoding='utf-8'))
    verify_pdf_hash_mapping(documents, hashes)
    page_count, word_count, available_pdfs = 0, 0, 0
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
            require(isinstance(value, str) and re.fullmatch(SHA256, value), f'Invalid {field}: {doc_id}')
        revision = document['revision']
        require(revision is None or isinstance(revision, str) and revision.strip(), f'Invalid revision: {doc_id}')
        if (assets / pdf).exists():
            require(file_sha256(assets / pdf) == document['pdfSha256'], f'PDF hash mismatch: {doc_id}')
            available_pdfs += 1
        data = (assets / index).read_bytes()
        require(hashlib.sha256(data).hexdigest() == document['searchIndexSha256'], f'Index hash mismatch: {doc_id}')
        parsed = json.loads(data)
        pages, words, maximum = verify_index_structure(parsed, doc_id)
        page_count += pages
        word_count += words
        print(f'INDEX {doc_id}: pages={pages}, maxPage={maximum}, words={words}')
    require(pdfs == all_references, 'Catalog does not cover every PDF reference')
    existing_indexes = {p.relative_to(assets).as_posix() for p in (assets / 'manuals').rglob('*_search.json')}
    require(indexes == existing_indexes, 'Catalog does not cover every existing index')
    print(f'OK: {len(documents)} exact PDF checksum/path mappings, legacy titles/paths, 20 index hashes; {page_count} index pages, {word_count} words')
    print(f'PDF bytes checked: {available_pdfs}/20. PDF/index content compatibility is NOT established by this check.')


if __name__ == '__main__':
    validate()
