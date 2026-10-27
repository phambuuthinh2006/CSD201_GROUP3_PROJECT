# Quay video kiểm thử và đo tốc độ cả nhóm trên terminal Linux

## Chọn lệnh theo hệ điều hành

| Hệ điều hành | Lệnh chạy test và đo tốc độ cả nhóm |
|---|---|
| Windows PowerShell | `powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\demo-terminal.ps1" -Mode all` |
| Linux Bash | `bash scripts/demo-terminal.sh all` |

Bản Windows gọi cùng Java harness và các helper Python, không cần WSL/Git Bash. Cả hai luồng đều chạy56JUnit, generator và benchmark5biến thể. Hướng dẫn Windows đầy đủ: [Hướng dẫn Windows](HUONG_DAN_TEST_WINDOWS_POWERSHELL.md). Các lệnh shell trong phần dưới là bản Linux; không chép nguyên export/grep/classpath dấu hai chấm sang PowerShell.

## 1. Một lệnh dùng cho cả nhóm

```bash
cd /home/phambuuthinh/Downloads/CSD201_GROUP3_PROJECT
bash scripts/demo-terminal.sh all
```

**Chế độ all đã được cập nhật để đo tốc độ tất cả thuật toán**, không chỉ riêng LZ4. Script tự chạy theo thứ tự:

1. In môi trường Java/Maven.
2. Chạy56 test JUnit: RLE7, Huffman23, LZW8, LZ4 codec16, runnerLZ4 2; đọc XML kiểm failure/error.
3. Chạy test generator C1/C2/C3 riêng. Log đầy đủ vào generator.log, terminal chỉ in tóm tắt.
4. Đo mới RLE/Huffman/LZW/LZ4-12/LZ4-16 trên cùng input và điều kiện.
5. In bảng ratio và tốc độ nén/giải nén cho input1MiB, rồi4MiB.
6. In đường dẫn lưu log/CSV/môi trường để kiểm tra lại.

Bốn thuật toán nhưng năm biến thể vì LZ4 có hai hash log12/16.

## 2. Bảng cuối có gì?

```text
Dataset | Codec | Ratio | Compress MiB/s | Decompress MiB/s
```

- Dataset: SINGLE_BYTE, LONG_RUNS, WORD_TEXT, UNIFORM_RANDOM.
- Ratio = container nén / input gốc, gồm header; nhỏ hơn tốt hơn, >1 là file phình ra.
- Compress MiB/s: tốc độ nén; lớn hơn tốt hơn.
- Decompress MiB/s: tốc độ giải nén; lớn hơn tốt hơn.
- Hai tốc độ tính theo số byte gốc, đo riêng, không tính I/O, SHA-256 và ghi CSV.

Lời nói gợi ý: “Em đo cả bốn thuật toán trên cùng dữ liệu. Với mỗi kiểu dữ liệu, em so tốc độ nén, giải nén và tỷ lệ nén riêng. LZ4 có hai cấu hình bảng hash; em không giả định một cấu hình luôn tốt hơn.”

Không lấy thời gian chạy suite JUnit để nói tốc độ codec. PASS chứng minh tính đúng của các ca đã chọn; bảng benchmark mới dùng để so hiệu năng.

## 3. Điều kiện đo

- Bốn kiểu input, mỗi kiểu1/4MiB, seed20261027; cùng bytes cho tất cả codec.
- Mỗi cấu hình trong mỗi JVM:5warm-up và7lượt đo.
- Ba JVM mới chạy tuần tự; thứ tự codec được xáo trộn có seed qua mỗi vòng.
- 40cấu hình×3JVM×7lượt=840dòngđo; thêm600warm-up.
- Mọi lượt đều so độ dài và toàn bộ SHA-256 với input gốc.
- Bảng tổng hợp lấy median của ba median JVM, mỗi JVM lấy median bảy lượt.
- Flags JVM: -Xms512m -Xmx1g -XX:+UseG1GC.

Đây là benchmark khảo sát trên implementation Java của nhóm. Chưa phải RQ3 đầy đủ với Silesia/Canterbury,64MiB, nhiều seed hay peak heap. Không chạy hai benchmark song song khi quay.

## 4. Lưu kết quả

```text
results/video-.../
├── terminal.log
├── generator.log
├── TEST-*.xml
└── comparison/
    ├── raw-fork-1.csv
    ├── raw-fork-2.csv
    ├── raw-fork-3.csv
    ├── inputs-fork-*.csv
    ├── environment.txt
    ├── source-sha256.txt
    ├── summary.csv
    ├── fork-medians.csv
    └── SO_SANH_CODEC.md
```

Mỗi lần chạy tạo thư mục mới, không ghi đè số liệu cũ. Log ở ngoài target nên mvn clean không xóa mất. Thư mục video được gitignore; muốn nộp báo cáo thì lấy đúng CSV/log cần bàn giao.

Mở lại bảng của lần vừa chạy mà không đo lại:

```bash
CSD_COMPARE_DIR=$(cat /tmp/csd-latest-comparison-path)
python3 scripts/report-terminal.py benchmark "$CSD_COMPARE_DIR/summary.csv" --mib 4
```

Bảng đọc CSV chỉ là trình bày kết quả, không tạo một phép đo thứ hai.

## 5. Nếu chỉ muốn đo tốc độ, bỏ qua phần unit test

```bash
bash scripts/benchmark/run-comparison.sh
CSD_COMPARE_DIR=$(cat /tmp/csd-latest-comparison-path)
python3 scripts/benchmark/summarize-comparison.py "$CSD_COMPARE_DIR"
python3 scripts/report-terminal.py benchmark "$CSD_COMPARE_DIR/summary.csv" --mib 1
python3 scripts/report-terminal.py benchmark "$CSD_COMPARE_DIR/summary.csv" --mib 4
```

Tất cả codec vẫn được đo và kiểm SHA-256 trong benchmark. Các lệnh này không thay thế bước chạy full JUnit khi nghiệm thu.

## 6. Nếu chỉ quay phần TV5

```bash
bash scripts/demo-terminal.sh tv5
```

Chế độ tv5 giữ nguyên:18JUnit và demo LZ4-12/16 trên README (1warm-up+2lượt đo mỗi cấu hình,4dòngCSV). Nó không chạy benchmark các codec khác. File đầu ra riêng là lz4-demo.csv.

## 7. Môi trường và chia sẻ với nhóm

Cần Bash, JDK17, Python3, Maven. Script tìm Maven trên PATH hoặc ~/.cache/csd-tools/apache-maven-3.9.9/bin/mvn. Có thể đặt đường dẫn riêng:

```bash
CSD_MAVEN_BIN=/duong/dan/bin/mvn bash scripts/demo-terminal.sh all
```

Các script mới và helper hiện nằm trên máy này, chưa tự commit/push lên GitHub. Điều đó tách biệt với việc các file Java thuật toán đã nằm trong lịch sử project. Thành viên khác cần được cung cấp scripts/ cùng helper trước khi chạy lệnh này.

Script chỉ chạy test/đo và lưu log, không tự quay video hoặc thay đổi thuật toán. Dùng công cụ quay màn hình của bạn để ghi terminal; phóng to chữ và cuộn lại bảng sau khi script kết thúc.

## 8. Kết quả kiểm chứng lần cập nhật này

Đã chạy thật `bash scripts/demo-terminal.sh all`, exit0:56JUnitPASS, generatorPASS,840dòngđoPASS, bảng1/4MiB đều xuất đủ5biến thể. Kết quả tại `/home/phambuuthinh/Downloads/CSD201_GROUP3_PROJECT/results/video-20261027-221026-1GADFm`.

Xem [[CSD201_KICH_BAN_BAO_CAO_TEST_TERMINAL_DAY_DU]] để có toàn bộ ca test, lệnh riêng từng method và giải thích từng cột CSV.
