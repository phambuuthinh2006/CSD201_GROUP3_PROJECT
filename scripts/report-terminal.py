#!/usr/bin/env python3
"""Display actual test XML or benchmark CSV in a readable terminal table."""
import argparse
import collections
import csv
import statistics
import xml.etree.ElementTree as ET
from pathlib import Path


def table(headers, rows):
    rows = [[str(value) for value in row] for row in rows]
    widths = [max(len(str(h)), *(len(row[i]) for row in rows)) for i, h in enumerate(headers)]
    print(' | '.join(str(h).ljust(widths[i]) for i, h in enumerate(headers)))
    print('-+-'.join('-' * width for width in widths))
    for row in rows:
        print(' | '.join(value.ljust(widths[i]) for i, value in enumerate(row)))


def tests(args):
    files = sorted(Path('target/surefire-reports').glob('TEST-*.xml'))
    suites = [ET.parse(file).getroot() for file in files]
    if args.suite:
        suites = [suite for suite in suites if suite.attrib['name'].rsplit('.', 1)[-1] == args.suite]
    if not suites:
        raise SystemExit('Không tìm thấy báo cáo: chạy Maven test trước, kiểm tra thư mục hiện tại.')
    rows, total = [], [0, 0, 0, 0]
    for suite in suites:
        counts = [int(suite.attrib.get(key, 0)) for key in ('tests', 'failures', 'errors', 'skipped')]
        rows.append([suite.attrib['name'].rsplit('.', 1)[-1], *counts])
        total = [a + b for a, b in zip(total, counts)]
    table(['Suite', 'Tests', 'Failures', 'Errors', 'Skipped'], rows + [['TOTAL', *total]])
    if args.details:
        for suite in suites:
            print('\n' + suite.attrib['name'])
            for case in suite.findall('testcase'):
                status = ('FAIL' if case.find('failure') is not None or case.find('error') is not None
                          else 'SKIP' if case.find('skipped') is not None else 'PASS')
                print('  ' + status + '  ' + case.attrib['name'])
    print('\nXML có thể thuộc các lần chạy khác nhau; dùng clean verify để tạo bộ báo cáo mới đầy đủ.')
    if total[1] or total[2]:
        raise SystemExit('Có failure/error, không kết luận PASS từ BUILD SUCCESS.')


def demo_csv(args):
    with args.file.open(encoding="utf-8-sig", newline='') as stream:
        rows = list(csv.DictReader(stream))
    if not rows or any(row['status'] != 'PASS' for row in rows):
        raise SystemExit('CSV rỗng hoặc có status khác PASS.')
    groups = collections.defaultdict(list)
    for row in rows:
        groups[(row['file'], row['hash_log'])].append(row)
    output = []
    for (filename, hash_log), group in sorted(groups.items()):
        for field in ('input_bytes', 'compressed_bytes', 'ratio', 'output_check', 'lz4_sequence_count', 'lz4_avg_match_length'):
            if len({row[field] for row in group}) != 1:
                raise SystemExit('Kết quả không nhất quán: ' + filename + ', field=' + field)
        row = group[0]
        if args.speed:
            comp = [float(item['compress_mib_s']) for item in group]
            decomp = [float(item['decompress_mib_s']) for item in group]
            output.append([Path(filename).name, hash_log, len(group), f'{statistics.median(comp):.2f}',
                           f'{statistics.median(decomp):.2f}'])
        else:
            output.append([Path(filename).name, hash_log, row['input_bytes'], row['compressed_bytes'],
                           row['ratio'] or 'N/A', row['lz4_sequence_count'], row['lz4_avg_match_length'],
                           row['output_check'], 'PASS'])
    headers = (['Input', 'Log', 'Runs', 'Compress MiB/s', 'Decompress MiB/s'] if args.speed
               else ['Input', 'Log', 'Bytes', 'Compressed', 'Ratio', 'Seq', 'AvgMatch', 'SHA-prefix', 'Status'])
    table(headers, output)
    print(f'\n{len(rows)} dòng đo PASS; {len(groups)} cấu hình input/hash log. Warm-up không nằm trong CSV.')
    if args.speed:
        print('Tốc độ là median các lượt trong CSV; input rất nhỏ không phù hợp để xếp hạng hiệu năng.')


def benchmark(args):
    with args.file.open(encoding="utf-8-sig", newline='') as stream:
        rows = [row for row in csv.DictReader(stream) if int(row['input_bytes']) == args.mib * 1048576]
    if not rows:
        raise SystemExit('Không có kích thước đã chọn trong summary CSV.')
    table(['Dataset', 'Codec', 'Ratio', 'Compress MiB/s', 'Decompress MiB/s'], [
        [row['dataset'], row['codec'], f"{float(row['ratio']):.5f}",
         f"{float(row['compress_median_mib_s']):.1f}", f"{float(row['decompress_median_mib_s']):.1f}"]
        for row in rows])
    print(f'\nInput {args.mib} MiB. Đang đọc summary.csv đã có, không đo lại trong lệnh này.')
    print('Bảng do harness so sánh sinh ra: median 3 JVM, mỗi JVM lấy median 7 lượt; 5 warm-up mỗi cấu hình/JVM.')


parser = argparse.ArgumentParser(description=__doc__)
sub = parser.add_subparsers(dest='mode', required=True)
p = sub.add_parser('tests'); p.add_argument('--suite'); p.add_argument('--details', action='store_true'); p.set_defaults(run=tests)
p = sub.add_parser('csv'); p.add_argument('file', type=Path); p.add_argument('--speed', action='store_true'); p.set_defaults(run=demo_csv)
p = sub.add_parser('benchmark'); p.add_argument('file', type=Path); p.add_argument('--mib', type=int, default=4); p.set_defaults(run=benchmark)
args = parser.parse_args()
args.run(args)
