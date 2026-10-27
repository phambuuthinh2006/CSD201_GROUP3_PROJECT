#!/usr/bin/env python3
"""Create small, reproducible inputs for a LZ4 correctness video (not RQ3 corpus)."""
import hashlib
import random
import sys
from pathlib import Path

folder = Path(sys.argv[1])
folder.mkdir(parents=True, exist_ok=False)
rng = random.Random(20261027)
fixtures = {
    '01-empty.bin': b'',
    '02-one-byte.bin': b'A',
    '03-short.bin': b'ABABABA',
    '04-overlap-20A.bin': b'A' * 20,
    '05-pattern.bin': b'ABCDABCDABCD12345',
    '06-literal15.bin': bytes(range(15)),
    '07-all256.bin': bytes(range(256)),
    '08-repeat-1MiB.bin': b'A' * 1048576,
    '09-random-1MiB.bin': bytes(rng.randrange(256) for _ in range(1048576)),
    '10-vietnamese.txt': 'Nén dữ liệu không mất mát: kiểm tra LZ4, tiếng Việt.\n'.encode('utf-8'),
}
for filename, data in fixtures.items():
    (folder / filename).write_bytes(data)
    print(f'{filename:24} bytes={len(data):8} SHA-prefix={hashlib.sha256(data).hexdigest()[:16]}')
print(f'Created {len(fixtures)} inputs. Seed for Python Random: 20261027; not the Java C1/C2/C3 generator.')
