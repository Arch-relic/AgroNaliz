"""JCS is supplied by the official, externally installed rfc8785==0.1.4."""
import hashlib
import json
from importlib.metadata import version

import rfc8785

if version('rfc8785') != '0.1.4':
    raise RuntimeError('rfc8785==0.1.4 required')


class Refusal(ValueError):
    def __init__(self, code):
        self.code = code
        super().__init__(code)


def require(condition, code='INVALID_SCHEMA'):
    if not condition:
        raise Refusal(code)


def domain_json(value):
    if value is None or type(value) is bool:
        return
    if type(value) is int:
        require(0 <= value <= 9007199254740991)
    elif type(value) is str:
        require(not any(0xD800 <= ord(c) <= 0xDFFF for c in value))
    elif type(value) is list:
        for item in value:
            domain_json(item)
    elif type(value) is dict:
        for key, item in value.items():
            require(type(key) is str)
            domain_json(key)
            domain_json(item)
    else:
        raise Refusal('INVALID_SCHEMA')


def loads(raw):
    def pairs(items):
        obj = {}
        for key, value in items:
            require(key not in obj, 'INVALID_CANONICAL_JSON')
            obj[key] = value
        return obj
    def invalid(_):
        raise Refusal('INVALID_CANONICAL_JSON')
    try:
        require(type(raw) is bytes, 'INVALID_CANONICAL_JSON')
        obj = json.loads(raw.decode('utf-8'), object_pairs_hook=pairs,
                         parse_float=invalid, parse_constant=invalid)
        domain_json(obj)
        return obj
    except Refusal:
        raise
    except (UnicodeError, ValueError, RecursionError):
        raise Refusal('INVALID_CANONICAL_JSON') from None


def dumps(obj):
    domain_json(obj)
    return rfc8785.dumps(obj)


def digest(name, obj):
    return hashlib.sha256(('agronaliz.mock-b/1/' + name + '\0').encode() + dumps(obj)).hexdigest()


def sha(raw):
    return hashlib.sha256(raw).hexdigest()
