#!/usr/bin/env python3
"""Validate and summarize the fixed exploratory comparison, no external dependencies."""
import collections
import csv
import statistics
import sys
from pathlib import Path

root = Path(sys.argv[1]).resolve()
rows = []
for fork in (1, 2, 3):
    with (root / f"raw-fork-{fork}.csv").open() as stream:
        part = list(csv.DictReader(stream))
    assert len(part) == 280, (fork, len(part))
    assert {int(row["fork"]) for row in part} == {fork}
    rows.extend(part)
assert len(rows) == 840 and all(row["status"] == "PASS" for row in rows)
manifest = [(root / f"inputs-fork-{fork}.csv").read_text() for fork in (1, 2, 3)]
assert len(set(manifest)) == 1, "Inputs differ between forks"

groups = collections.defaultdict(list)
for row in rows:
    groups[(row["dataset"], int(row["input_bytes"]), row["codec"])].append(row)
assert len(groups) == 40
order = ["RLE", "Huffman", "LZW", "LZ4-12", "LZ4-16"]
datasets = ["SINGLE_BYTE", "LONG_RUNS", "WORD_TEXT", "UNIFORM_RANDOM"]
names = {"SINGLE_BYTE": "Một byte lặp", "LONG_RUNS": "Run dài", "WORD_TEXT": "Văn bản theo từ", "UNIFORM_RANDOM": "Byte ngẫu nhiên"}
summaries = {}
fork_rows = []
for key, group in groups.items():
    assert len(group) == 21
    assert len({(r["fork"], r["iteration"]) for r in group}) == 21
    assert len({r["compressed_bytes"] for r in group}) == 1
    assert len({r["output_check"] for r in group}) == 1
    item = {"dataset": key[0], "input_bytes": key[1], "codec": key[2],
            "compressed_bytes": int(group[0]["compressed_bytes"]), "ratio": float(group[0]["ratio"])}
    for metric in ("compress", "decompress"):
        medians = []
        for fork in (1, 2, 3):
            runs = [r for r in group if int(r["fork"]) == fork]
            assert len(runs) == 7
            value = statistics.median(float(r[metric + "_mib_s"]) for r in runs)
            medians.append(value)
            fork_rows.append({"dataset": key[0], "input_bytes": key[1], "codec": key[2], "fork": fork,
                              "operation": metric, "median_mib_s": value})
        item[metric + "_median_mib_s"] = statistics.median(medians)
        item[metric + "_fork_min_mib_s"] = min(medians)
        item[metric + "_fork_max_mib_s"] = max(medians)
    summaries[key] = item

ordered = [summaries[(ds, size, codec)] for size in (1048576, 4194304) for ds in datasets for codec in order]
with (root / "summary.csv").open("w") as stream:
    writer = csv.DictWriter(stream, fieldnames=list(ordered[0]))
    writer.writeheader()
    writer.writerows(ordered)
with (root / "fork-medians.csv").open("w") as stream:
    writer = csv.DictWriter(stream, fieldnames=list(fork_rows[0]))
    writer.writeheader()
    writer.writerows(fork_rows)

lines = ["# So sánh RLE, Huffman, LZW và LZ4 — đo sơ bộ ngày 27/10/2026", "",
"> Kết luận: LZ4 không nhanh nhất trên mọi dữ liệu trong bản cài đặt hiện tại. Cần xét riêng tốc độ nén, tốc độ giải nén và tỷ lệ nén. Đây là thí nghiệm sơ bộ, chưa thay RQ3 chính thức.", "",
"## 1. Đã đo như thế nào?", "",
"- Máy: AMD Ryzen 5 6600H, 6 nhân/12 luồng, RAM khoảng 15 GiB; Linux x86_64, OpenJDK 17.0.20.1. Chi tiết kernel, bộ nhớ và tải máy trong environment.txt.",
"- Mỗi lượt chạy JVM có `-Xms512m -Xmx1g -XX:+UseG1GC`. Ba JVM mới chạy tuần tự, không chạy benchmark codec song song.",
"- Năm biến thể: RLE, Huffman, LZW, LZ4 HASH_LOG=12 và 16. Mỗi biến thể dùng đúng mã nguồn project tại lúc đo; danh sách SHA-256 nguồn đi kèm.",
"- Hai kích thước: 1 MiB và 4 MiB; bốn loại dữ liệu; seed 20261027. Tám input có cùng SHA-256 giữa ba JVM.",
"- Mỗi cấu hình/input trong mỗi JVM: 5 warm-up, sau đó 7 lượt đo. Thứ tự codec được xáo trộn có seed qua từng vòng để giảm thiên lệch vị trí.",
"- Tổng 40 cấu hình × 3 JVM × 7 = **840 lượt đo**; thêm **600 warm-up**. Cả **1.440 lượt** đều so độ dài và toàn bộ SHA-256 output với input, không có mismatch.",
"- Tạo input, tính SHA-256 và ghi CSV nằm ngoài thời gian đo. Nén và giải nén đo riêng bằng nanoTime, gồm cấp phát bộ đệm bên trong codec; không gọi System.gc giữa các ca. GC phát sinh bên trong vùng đo vẫn nằm trong kết quả.",
"- Tốc độ MiB/s tính theo kích thước gốc, 1 MiB = 1.048.576 byte. Ratio = toàn bộ container nén / số byte gốc, gồm header.",
"- Bảng dùng median của 7 lượt trong từng JVM, rồi median của ba median JVM. Mỗi ô đại diện cho 21 lần đo qua ba tiến trình; không coi 21 lượt là 21 JVM độc lập.",
"- summary.csv còn có min/max của ba median JVM để thấy dao động; khoảng đó không phải khoảng tin cậy thống kê. Chênh lệch vài phần trăm chỉ nên coi là gần nhau.", "",
"## 2. Dữ liệu dùng để đo", "",
"| Nhãn | Cách tạo |", "|---|---|",
"| SINGLE_BYTE | Toàn byte ASCII A; tình huống rất thuận lợi cho RLE |",
"| LONG_RUNS | Độ dài run hình học kỳ vọng 32 byte, giá trị run chọn đều 0–255; run liền kề có thể trùng giá trị |",
"| WORD_TEXT | Từ vựng 1.000 từ sinh bằng chữ thường, dài 3–10; lấy mẫu theo trọng số Zipf 1/rank, cách nhau dấu cách |",
"| UNIFORM_RANDOM | Random.nextBytes với seed cố định; dữ liệu giả ngẫu nhiên gần phân bố đều |", "",
"Các input được sinh riêng trong harness vì generator chung chưa có. Không gắn nhãn đây là corpus R2/R3 hoặc bộ C1–C3 chính thức; chỉ dùng để khảo sát sơ bộ. Có một seed, chưa kiểm độ ổn định qua nhiều seed.", "",
"## 3. Tốc độ và tỷ lệ nén", "",
"Tốc độ lớn hơn tốt hơn; ratio nhỏ hơn tốt hơn. Ratio 0,42 nghĩa là container còn khoảng 42% kích thước gốc; ratio lớn hơn 1 nghĩa là file bị phình ra.", ""]
for size in (4194304, 1048576):
    lines.extend([f"### Input {size // 1048576} MiB", "", "| Dữ liệu | Codec | Ratio | Nén (MiB/s) | Giải nén (MiB/s) |", "|---|---|---:|---:|---:|"])
    for ds in datasets:
        for codec in order:
            s = summaries[(ds, size, codec)]
            lines.append(f"| {names[ds]} | {codec} | {s['ratio']:.5f} | {s['compress_median_mib_s']:.1f} | {s['decompress_median_mib_s']:.1f} |")
    lines.append("")
lines.extend(["## 4. Biến thể có median tốc độ cao nhất", "",
"Bảng này chỉ xếp median trong mẫu đo, không khẳng định ưu thế có ý nghĩa thống kê. Nếu hai vị trí đầu lệch dưới 5%, ghi thêm tên runner-up để tránh kết luận mạnh.", "",
"| Input | Dữ liệu | Nén | Giải nén | Ratio thấp nhất |", "|---|---|---|---|---|"])
for size in (1048576, 4194304):
    for ds in datasets:
        candidates = [summaries[(ds, size, c)] for c in order]
        labels = []
        for metric in ("compress", "decompress"):
            ranking = sorted(candidates, key=lambda s: s[metric + "_median_mib_s"], reverse=True)
            first, second = ranking[:2]
            gap = 100 * (first[metric + "_median_mib_s"] / second[metric + "_median_mib_s"] - 1)
            label = first["codec"]
            if gap < 5:
                label += f" ≈ {second['codec']} (lệch {gap:.1f}%)"
            labels.append(label)
        best = min(candidates, key=lambda s: s["ratio"])
        lines.append(f"| {size // 1048576} MiB | {names[ds]} | {labels[0]} | {labels[1]} | {best['codec']} ({best['ratio']:.5f}) |")
s4 = {codec: summaries[("UNIFORM_RANDOM", 4194304, codec)] for codec in order}
w4 = {codec: summaries[("WORD_TEXT", 4194304, codec)] for codec in order}
r4 = {codec: summaries[("LONG_RUNS", 4194304, codec)] for codec in order}
a4 = {codec: summaries[("SINGLE_BYTE", 4194304, codec)] for codec in order}
lines.extend(["", "## 5. Kết luận có thể trình bày với thầy", "",
"- **LZ4 không nhanh nhất trên tất cả dữ liệu.** Trong dữ liệu một byte lặp và run dài, RLE nhanh hơn rõ về cả nén và giải nén trong bản cài đặt đang đo.",
f"- Với byte ngẫu nhiên 4 MiB, tốc độ nén LZ4-12: {s4['LZ4-12']['compress_median_mib_s']:.1f} MiB/s; RLE: {s4['RLE']['compress_median_mib_s']:.1f} MiB/s; LZ4-16: {s4['LZ4-16']['compress_median_mib_s']:.1f} MiB/s. Đọc cùng bảng xếp median và lưu ý các chênh lệch nhỏ.",
f"- Với byte ngẫu nhiên 4 MiB, LZ4-12 giải nén {s4['LZ4-12']['decompress_median_mib_s']:.1f} MiB/s, LZ4-16 giải nén {s4['LZ4-16']['decompress_median_mib_s']:.1f} MiB/s. Dữ liệu gần như toàn literal nên decoder chủ yếu sao chép mảng. Đây là tốc độ trong RAM với buffer vừa được tạo, cache nóng; không phải tốc độ đọc file/ổ đĩa.",
f"- Với văn bản 4 MiB, RLE có ratio {w4['RLE']['ratio']:.3f}: đầu ra lớn gần gấp đôi. LZ4-16 có ratio {w4['LZ4-16']['ratio']:.3f}, giải nén {w4['LZ4-16']['decompress_median_mib_s']:.1f} MiB/s; LZW có ratio {w4['LZW']['ratio']:.3f}, giải nén {w4['LZW']['decompress_median_mib_s']:.1f} MiB/s. Cần đọc tốc độ cùng kích thước đầu ra.",
f"- Hash log không cho cùng kết quả trên mọi input: văn bản 4 MiB có ratio LZ4-12/16 là {w4['LZ4-12']['ratio']:.3f}/{w4['LZ4-16']['ratio']:.3f}; run dài là {r4['LZ4-12']['ratio']:.3f}/{r4['LZ4-16']['ratio']:.3f}. Tốc độ cũng phụ thuộc input.",
f"- Với một byte lặp 4 MiB, LZW có ratio {a4['LZW']['ratio']:.5f}, RLE nén {a4['RLE']['compress_median_mib_s']:.1f} MiB/s. Thuật toán nhanh nhất và thuật toán nén nhỏ nhất có thể khác nhau.", "",
"> Cách nói ngắn: Em đo năm biến thể trên cùng dữ liệu và kiểm SHA-256 mọi lượt. Kết quả cho thấy LZ4 nổi bật ở giải nén dữ liệu gần ngẫu nhiên; RLE nhanh ở run dài; còn dữ liệu văn bản phải cân bằng tốc độ với tỷ lệ nén. Em chưa kết luận LZ4 nhanh nhất trong mọi trường hợp.", "",
"## 6. Giới hạn và bước tiếp theo", "",
"- Đây là so sánh các **bản cài đặt Java của nhóm**, không phải kết luận phổ quát về mọi implementation RLE/Huffman/LZW/LZ4.",
"- Chỉ 1/4 MiB, một seed và dữ liệu tổng hợp. Chưa đo Silesia/Canterbury, 64 MiB, nhiều seed hay ma trận RQ3 chính thức.",
"- Không đo peak heap; không xếp hạng bộ nhớ bằng chênh lệch heap tức thời.",
"- Chạy trên Linux desktop, không khóa tần số CPU, pin affinity hoặc loại bỏ toàn bộ tiến trình nền. Đã lưu môi trường/tải máy; chênh lệch nhỏ có thể do nhiễu.",
"- Năm warm-up mỗi cấu hình và ba JVM giảm thiên lệch nhưng không chứng minh JIT hội tụ hoàn toàn. Chưa dùng JMH hay kiểm định thống kê.",
"- Chỉ kiểm round-trip các input đo, không thay unit test dữ liệu hỏng. Các lỗi decoder với input hỏng đã nêu trong review vẫn cần xử lý riêng.",
"- Bước tiếp theo: TV4/TV6 đưa cùng chính sách đo vào runner chuẩn, bổ sung corpus chuẩn, 64 MiB, nhiều seed và peak heap trước khi viết kết luận báo cáo chính thức.", "",
"## 7. Dữ liệu gốc và cách tái tạo", "",
f"Thư mục kết quả: `{root}`.", "",
"- `raw-fork-1.csv` đến `raw-fork-3.csv`: 840 dòng đo; `inputs-fork-*.csv`: SHA-256 đầy đủ của input.",
"- `summary.csv`: 40 dòng tổng hợp, ratio và tốc độ cùng khoảng dao động median qua JVM.",
"- `fork-medians.csv`: 240 median riêng (40 cấu hình × 3 JVM × 2 thao tác).",
"- `environment.txt`, `source-sha256.txt`, `fork-*.log`: môi trường, nhận diện source và log tiến trình.",
"- Bản harness Java và shell đã chép vào thư mục kết quả để lưu lại cách đo.", "",
"```bash", "cd /home/phambuuthinh/Downloads/CSD201_GROUP3_PROJECT", "bash scripts/benchmark/run-comparison.sh", "# Script in đường dẫn thư mục mới; truyền đường dẫn đó vào lệnh dưới:", "python3 scripts/benchmark/summarize-comparison.py /duong/dan/thu/muc/ket/qua", "```", "",
"Các lệnh chỉ tạo kết quả mới, không sửa thuật toán và không push GitHub. Nếu chưa có Maven trên PATH, runner tìm bản ở ~/.cache/csd-tools hoặc dùng biến CSD_MAVEN_BIN.", ""])

(root / "SO_SANH_CODEC.md").write_text("\n".join(lines), encoding="utf-8")
print(f"Validated {len(rows)} PASS measurement rows, 40 summaries, {len(fork_rows)} fork-operation medians")
print(root / "SO_SANH_CODEC.md")
