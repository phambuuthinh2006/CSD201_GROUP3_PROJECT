---
tags: [CSD201, TV5, LZ4, terminal, video, test]
created: 2026-10-27
---

# Kịch bản báo cáo test trên terminal Linux — đầy đủ các ca hiện có

> Dành cho TV5 trình bày với thầy: **lệnh chạy → số liệu nhìn thấy → ý nghĩa → lời giải thích**. Đối chiếu source local main `91d5663` ngày 27/10/2026. “Đầy đủ” ở đây là đầy đủ các test và tình huống đang được kiểm chứng trong phiên bản này, không có nghĩa đã chứng minh mọi input có thể có đều không lỗi.

## Chọn lệnh theo hệ điều hành

| Hệ điều hành | Lệnh chạy test và đo tốc độ cả nhóm |
|---|---|
| Windows PowerShell | `powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\demo-terminal.ps1" -Mode all` |
| Linux Bash | `bash scripts/demo-terminal.sh all` |

Bản Windows gọi cùng Java harness và các helper Python, không cần WSL/Git Bash. Cả hai luồng đều chạy56JUnit, generator và benchmark5biến thể. Hướng dẫn Windows đầy đủ: [Hướng dẫn Windows](HUONG_DAN_TEST_WINDOWS_POWERSHELL.md). Các lệnh shell trong phần dưới là bản Linux; không chép nguyên export/grep/classpath dấu hai chấm sang PowerShell.

## 0. Cách chạy mới: một lệnh đo tốc độ tất cả thuật toán

**Đã cập nhật script:** chế độ `all` hiện vừa kiểm thử cả nhóm, vừa đo mới tốc độ **RLE, Huffman, LZW và LZ4**. Không còn giới hạn phần đo cuối ở riêng LZ4.

```bash
cd /home/phambuuthinh/Downloads/CSD201_GROUP3_PROJECT
bash scripts/demo-terminal.sh all
```

| Chế độ | Kiểm thử | Đo tốc độ tự động |
|---|---|---|
| `all` | 56 JUnit + generator main | RLE, Huffman, LZW, LZ4-12, LZ4-16; 3 JVM; input 1/4 MiB |
| `tv5` | 18 JUnit TV5 | Demo LZ4-12/16 trên README, 4 dòng đo |

Chế độ `all` lần lượt in: môi trường → JUnit → generator → tiến độ đo mới → bảng 1 MiB → bảng 4 MiB. Khi trình bày, chỉ vào các cột **Ratio**, **Compress MiB/s**, **Decompress MiB/s**. Bảng cuối lấy từ phép đo vừa chạy, không phải bảng của lần cũ.

Mỗi bảng có bốn kiểu dữ liệu và năm biến thể. Một codec tên LZ4 được đo thành hai cấu hình12/16, nên có năm dòng biến thể dù chỉ có bốn thuật toán.

Khi script kết thúc, nó in thư mục `results/video-.../`. Kết quả benchmark nằm trong `comparison/summary.csv` cùng các CSV thô và môi trường. Có thể cuộn lại terminal để thuyết trình.

Script đang có cục bộ, chưa push GitHub; không có nghĩa file Java thuật toán chưa được commit. Máy khác cần có các script/helper này trước khi dùng các lệnh mới.

## 1. Phân biệt ba loại con số trước khi báo cáo

| Kết quả | Nó chứng minh điều gì? | Không dùng để kết luận |
|---|---|---|
| 56 test JUnit PASS | Các assertion của năm suite hiện có đều đúng | Codec nào nhanh nhất |
| Generator test main PASS | Các ca biên, seed và đặc tính mẫu C1/C2/C3 được kiểm tra đúng | Đã chạy đủ corpus RQ3 |
| CSV nén/giải nén | Kích thước, thời gian và SHA của chính input được đo | Mọi dataset khác đều có cùng hiệu năng |

Phần TV5 có 18 lượt JUnit (16 codec +2 runner). Tham số12/16 khiến một số phương thức được chạy hai lượt. Một phương thức lại kiểm nhiều input bên trong, nên số input thực tế lớn hơn số test hiển thị.

## 2. Chuẩn bị một lần trước khi quay

Mở terminal, phóng to chữ. Tất cả lệnh dưới đây chạy từ root project, trên cùng terminal để giữ biến CSD_REPORT_DIR.

```bash
cd /home/phambuuthinh/Downloads/CSD201_GROUP3_PROJECT
export PATH="$HOME/.cache/csd-tools/apache-maven-3.9.9/bin:$PATH"
java -version
mvn -version
python3 --version
git log -1 --oneline
git status --short
mkdir -p results
CSD_REPORT_DIR=$(mktemp -d "$PWD/results/video-report-$(date +%Y%m%d-%H%M%S)-XXXXXX")
printf '%s\n' "$CSD_REPORT_DIR"
```

Maven cache trên là bản có trên máy hiện tại; máy khác có thể dùng Maven trên PATH. Cần JDK17 và Python3 như môi trường đã kiểm chứng. Lần đầu Maven có thể cần mạng tải dependency.

`git status` thể hiện file sửa/chưa commit. File đó vẫn có thể được build; không nên chỉ chỉ commit rồi nói mọi source đang chạy đều nằm trong commit.

Các helper dùng trong hướng dẫn:

| File | Công dụng |
|---|---|
| scripts/demo-terminal.sh | `all`: test cả nhóm và đo tốc độ tất cả thuật toán; `tv5`: riêng LZ4 |
| scripts/report-terminal.py | Đọc XML/CSV thật và hiển thị bảng gọn |
| scripts/create-demo-inputs.py | Tạo10 input demo có seed |
| scripts/benchmark/IntegrationCheck.java | Kiểm chéo generator→codec, histogram/H0 và CSV |
| scripts/benchmark/run-comparison.sh | Đo so sánh5 biến thể trên nhiều JVM |

Các helper mới hiện có cục bộ trên máy này; chưa được tự commit/push trong tác vụ viết tài liệu. Máy khác phải có các file này trước khi dùng lệnh tương ứng.

## 3. Bước đầu: chạy cả nhóm, chỉ rõ56 test ở đâu

```bash
set -o pipefail
mvn -B -Dstyle.color=never clean verify 2>&1 | tee "$CSD_REPORT_DIR/junit.log"
python3 scripts/report-terminal.py tests
```

Bảng cần chỉ trên terminal:

```text
Suite            Tests  Failures  Errors  Skipped
HuffmanCodecTest   23      0        0        0
LZ4CodecTest       16      0        0        0
LZ4Rq3RunnerTest    2      0        0        0
LZWCodecTest        8      0        0        0
RLECodecTest        7      0        0        0
TOTAL             56      0        0        0
```

Ý nghĩa từng cột:

- **Tests:** số lượt JUnit đã chạy, không phải số file input.
- **Failures:** assertion không đạt.
- **Errors:** lỗi ngoài assertion khi chạy test.
- **Skipped:** test không được chạy.
- **Time elapsed** trong output Maven là thời gian chạy suite, gồm tạo input/assertion/hash; không phải tốc độ codec.

Lời nói: “Em chạy clean verify để tạo báo cáo mới. Cả56 lượt đều chạy, không failure/error/skip. Phần cá nhân TV5 có18 lượt.”

**Lưu ý quan trọng:** POM hiện có `testFailureIgnore=true`, nên không dùng riêng chữ BUILD SUCCESS làm bằng chứng. Helper đọc XML và báo lỗi nếu có failure/error. Nó không thay kết quả test mà chỉ trình bày lại. Không dùng báo cáo cũ trong target để nói rằng vừa chạy test mới.

Muốn hiện tên từng ca TV5:

```bash
python3 scripts/report-terminal.py tests --suite LZ4CodecTest --details
python3 scripts/report-terminal.py tests --suite LZ4Rq3RunnerTest --details
```

## 4. Chạy từng ca LZ4 khi thầy yêu cầu

Các lệnh mẫu dùng tên đúng trong source. Sau mỗi lệnh, in bảng riêng suite và tên từng ca:

```bash
mvn '-Dtest=LZ4CodecTest#overlappingMatchesAndMatchExtensions' test
python3 scripts/report-terminal.py tests --suite LZ4CodecTest --details
```

Ca trên có2 lượt vì chạy hashlog12 và16. Dòng `[1]` và `[2]` trong tên report tương ứng thứ tự `@ValueSource(ints={12,16})` trong source.

Những ca đáng trình bày riêng:

| Muốn chứng minh | Lệnh |
|---|---|
| Empty và input<13byte chỉ literal | `mvn '-Dtest=LZ4CodecTest#emptyAndShortInputsAreLiteralOnly' test` |
| Offset1 chồng lấn, extension match | `mvn '-Dtest=LZ4CodecTest#overlappingMatchesAndMatchExtensions' test` |
| Literal15/270 và extension0 | `mvn '-Dtest=LZ4CodecTest#allBytesAndLiteralExtensionBoundaries' test` |
| Offset little-endian và tối đa65535 | `mvn '-Dtest=LZ4CodecTest#handCraftedOffsetsAndMaximumDistance' test` |
| Header/magic/truncation sai | `mvn '-Dtest=LZ4CodecTest#rejectsCorruptHeadersAndTruncations' test` |
| Block hỏng, offset0, thiếu tail/extension | `mvn '-Dtest=LZ4CodecTest#rejectsMalformedBlocksWithConsistentHeader' test` |
| Random và chuỗi có chu kỳ | `mvn '-Dtest=LZ4CodecTest#seededRandomAndRepetitiveInputs' test` |
| Oracle độc lập hai chiều | `mvn '-Dtest=LZ4CodecTest#compatibleWithIndependentJavaOracleInBothDirections' test` |
| Null và hashlog không được hỗ trợ | `mvn '-Dtest=LZ4CodecTest#parametersAndNulls' test` |
| 64MiB và từ chối64MiB+1 | `mvn '-Dtest=LZ4CodecTest#full64MiBBoundary' test` |
| Runner có đủ12/16, loại warm-up, bảo vệ output | `mvn '-Dtest=LZ4Rq3RunnerTest#csvHasBothHashLogsAndOnlyMeasuredIterations' test` |
| Runner file rỗng không tính ratio | `mvn '-Dtest=LZ4Rq3RunnerTest#emptyFileHasNoRatioAndKnownSha256' test` |

Sau khi chọn một method, report của suite có thể chỉ còn1/2 lượt. Đó không phải mất test. Nếu muốn trở lại tổng56, chạy `mvn clean verify` rồi đọc report mới; tránh cộng lẫn XML mới/cũ từ nhiều lệnh khác nhau.

**Với input hỏng:** PASS có nghĩa chương trình đã ném đúng lỗi mà test mong đợi. Không có nghĩa decoder chấp nhận block hỏng. Ví dụ magic sai phải bị từ chối có kiểm soát.

**Với oracle:** thư viện chỉ ở scope test. Mục đích là tránh trường hợp encoder và decoder cùng sai một kiểu nên tự round-trip vẫn tưởng đúng.

## 5. Demo bằng10 file thật để nhìn thấy số liệu

Sau khi đã compile ở bước3, tạo input mới:

```bash
python3 scripts/create-demo-inputs.py "$CSD_REPORT_DIR/inputs"
```

Script in tên file, số byte và16 ký tự đầu SHA-256. Nó từ chối thư mục đã tồn tại, tránh ghi đè input của lần quay trước. Muốn làm lại toàn bộ, tạo CSD_REPORT_DIR mới ở bước2.

Chạy nén/giải nén và ghi CSV:

```bash
java -Xmx1g -cp target/classes vn.csd.compression.metrics.LZ4Rq3Runner \
  "$CSD_REPORT_DIR/lz4-cases.csv" 3 5 "$CSD_REPORT_DIR"/inputs/* \
  2> "$CSD_REPORT_DIR/lz4-environment.log"
python3 scripts/report-terminal.py csv "$CSD_REPORT_DIR/lz4-cases.csv"
```

Đối số3 là warm-up,5 là số lượt đo. Có10input×2hashlog×5 = **100 dòng CSV đo**, chưa tính60warm-up. Runner kiểm độ dài và toàn bộ SHA-256 trong cả160 lượt.

Bảng gọn gom5 lượt cùng input/hashlog thành một dòng vì kích thước, thống kê và hash không đổi; như vậy bảng có20 dòng, còn CSV thật có100 dòng. Khi xem tốc độ ở phần6, helper lấy median5 lượt, không lấy ngẫu nhiên một dòng.

### 5.1. Các trường hợp và số liệu đã kiểm chứng

| Input | Bytes gốc | Bytes nén log12 / log16 | Sequence log12 / log16 | Match trung bình log12 / log16 |
|---|---:|---:|---:|---:|
| 01-empty.bin | 0 | 17 / 17 | 1 / 1 | 0 / 0 |
| 02-one-byte.bin | 1 | 18 / 18 | 1 / 1 | 0 / 0 |
| 03-short.bin | 7 | 24 / 24 | 1 / 1 | 0 / 0 |
| 04-overlap-20A.bin | 20 | 26 / 26 | 2 / 2 | 14 / 14 |
| 05-pattern.bin | 17 | 29 / 29 | 2 / 2 | 8 / 8 |
| 06-literal15.bin | 15 | 33 / 33 | 1 / 1 | 0 / 0 |
| 07-all256.bin | 256 | 274 / 274 | 1 / 1 | 0 / 0 |
| 08-repeat-1MiB.bin | 1048576 | 4138 / 4138 | 2 / 2 | 1.04857e+06 / 1.04857e+06 |
| 09-random-1MiB.bin | 1048576 | 1052705 / 1052701 | 2 / 10 | 4 / 4 |
| 10-vietnamese.txt | 68 | 86 / 86 | 1 / 1 | 0 / 0 |

Các số kích thước ở đây đã chạy thật trên source hiện tại; số tốc độ thay đổi theo máy và lần chạy. Input random dùng Python Random seed20261027, khác thuật toán sinh byte Java C3 nên không tự so với hash của Java C3.

### 5.2. Chỉ tay vào bảng và giải thích các trường hợp

**Empty:** Bytes0, Compressed17 =16header+1token0; Ratio hiển thị N/A vì không chia cho0; Seq1, AvgMatch0. Tốc độ0MiB/s của input rỗng chỉ do số byte xử lý bằng0, không dùng so hiệu năng.

**Một byte:** 1→18byte; short7byte→24byte. Input dưới13byte không có match, vẫn cần header/token. Nén phình ra là hợp lệ.

**20byte A:** 20→26byte, Seq2, MatchCount1, AvgMatch14. Encoder ghi1byte A đầu, match offset1 dài14, rồi5byte A cuối làm literal. Payload10byte+header16byte=26. Đây là ví dụ dễ giải thích offset/length: lùi1byte, sao chép14byte bằng forward copy.

**ABCDABCDABCD12345:** 17→29byte; literalABCD, match offset4 dài8, tail12345; Seq2, AvgMatch8. Match chồng lấn có thể dùng những byte vừa copy để tiếp tục mở rộng output.

**Literal15:** 15→33byte:16header+1token+1extension0+15literal. Nibble literal bằng15 phải có extension kể cả phần dư0.

**Đủ256byte:** 256→274byte; kiểm byte0–255 được xử lý đúng, kể cả byte âm trong Java. Case này không có đoạn4byte lặp nên không có match.

**1MiB A:** 1048576→4138byte; ratio≈0,003946 tức còn≈0,3946% kích thước gốc. Seq2, match dài1048570. Dữ liệu lặp dài có lợi cho LZ4, nhưng không từ đó kết luận mọi input nén tốt như vậy.

**Random1MiB:** ratio≈1,00394, tức tăng khoảng0,394%. Rất ít match; hai hashlog có thể tìm số match khác nhau. Không khẳng định random luôn tuyệt đối không có đoạn trùng4byte.

**Tiếng Việt:** input68byte UTF-8 được khôi phục nguyên byte và hash. Thuật toán xử lý byte, không cần hiểu ký tự hay dấu tiếng Việt.

### 5.3. Xem đủ15 cột của một dòng dễ trình bày

```bash
python3 - "$CSD_REPORT_DIR/lz4-cases.csv" <<'PYROW'
import csv, sys
from pathlib import Path
with open(sys.argv[1]) as f:
    row = next(r for r in csv.DictReader(f)
               if Path(r['file']).name == '04-overlap-20A.bin'
               and r['hash_log'] == '12' and r['iteration'] == '1')
for key, value in row.items():
    print(f'{key:24} = {value or "N/A"}')
PYROW
```

| Trường CSV | Ý nghĩa cần nói |
|---|---|
| file | Input thật vừa đọc |
| hash_log | 12 hoặc16, tham số encoder |
| iteration | Lượt đo, không gồm warm-up |
| input_bytes | Kích thước gốc |
| compressed_bytes | Cả container, gồm16byte header |
| ratio | compressed_bytes/input_bytes; nhỏ hơn tốt hơn; rỗng không tính |
| compress_ns | Thời gian compress, đơn vị nanosecond |
| decompress_ns | Thời gian decompress riêng, không phải tổng cả hai |
| compress_mib_s | Tốc độ nén theo số byte gốc |
| decompress_mib_s | Tốc độ giải nén theo số byte gốc |
| lz4_sequence_count | Gồm sequence literal cuối |
| lz4_match_count | Số match, không gồm sequence literal cuối |
| lz4_avg_match_length | Tổng độ dài match/số match; không có match thì0 |
| output_check |16 ký tự hex đầu SHA-256 output; runner đã so đủ32byte hash |
| status | PASS nếu độ dài và SHA đúng; runner dừng khi có mismatch |

Đối chiếu hash input:

```bash
sha256sum "$CSD_REPORT_DIR/inputs/04-overlap-20A.bin"
```

Prefix phải là `edfcaac579024f57`, trùng output_check của input đó. Không nói chỉ kiểm16ký tự: đó chỉ là phần hiển thị.

## 6. Chỉ số tốc độ trên terminal

```bash
python3 scripts/report-terminal.py csv "$CSD_REPORT_DIR/lz4-cases.csv" --speed
```

Bảng có Compress MiB/s và Decompress MiB/s, lấy median5 lượt trên một JVM demo. Đây là số đo minh họa; không dùng tốc độ của file1byte/20byte để xếp hạng vì chi phí gọi hàm, cấp phát, JIT/timer chiếm phần lớn.

Công thức:

```text
MiB/s = (input_bytes / 1048576) / (time_ns / 1000000000)
ratio = compressed_bytes / input_bytes
```

Ví dụ input1MiB, nén mất4.000.000ns=0,004s thì tốc độ250MiB/s. Đây là ví dụ giải thích công thức, không phải một dòng đo được dựng lên để báo cáo.

Cả hai tốc độ tính theo bytes gốc. Nén/giải nén đo riêng; I/O, tạo input, SHA-256, thống kê và ghi CSV nằm ngoài thời gian codec. Cấp phát bên trong codec và hai lượt parse của decoder LZ4 hiện có vẫn nằm trong thời gian đo.

## 7. Chạy generator TV6 để trình bày C1/C2/C3

```bash
if java -Xmx1g -cp target/classes:target/test-classes \
    vn.csd.compression.DataGeneratorTest > "$CSD_REPORT_DIR/generator.log" 2>&1; then
  grep -E -- '---|Info|size 0|size 1|cung seed|khac seed|Tu vung dung|C2 sinh|C3 sinh|CHUC MUNG|\[FAIL\]' \
    "$CSD_REPORT_DIR/generator.log"
else
  cat "$CSD_REPORT_DIR/generator.log"
  false
fi
```

Log đầy đủ còn nguyên; lọc chỉ để terminal không bị hàng nghìn dòng kiểm từng chữ của vocabulary cuộn qua quá nhanh. Chỉ lọc sau khi Java đã chạy thành công; lỗi được in đầy đủ.

| Nhóm generator | Số liệu/điều kiện nên chỉ |
|---|---|
| Boundaries | C1/C2/C3 đều trả đúng kích thước0 và1 |
| Reproducibility | Cùng seed ra cùng bytes và SHA; khác seed ra khác bytes trên ca được chọn |
| C1 | Mẫu200000byte, in số run và độ dài trung bình thực tế; test chấp nhận20–45, kỳ vọng≈32 |
| C2 | Vocabulary1000 phần tử, mỗi từ3–10chữ thường; output100000byte chỉ chứa a–z và space |
| C3 | Mẫu256000byte, kỳ vọng mỗi byte xuất hiện1000lần; in Min/Max; test yêu cầu Min>700, Max<1300 |

C1 trung bình thực tế không phải đúng32,00 mọi lần. C3 không phải mỗi giá trị phải xuất hiện đúng1000lần. Đây là kiểm tra đặc tính thống kê trên mẫu hữu hạn.

Ví dụ output generator đã chạy thực tế:

```text
[C1 Info] Tong run: 6225, Do dai run trung binh thuc te: 32.13 bytes (Ly thuyet: ~32)
[C3 Info] Tan suat ky vong: 1000 | Min: 904, Max: 1087
```

Bạn chỉ vào32,13 để nói đó là trung bình run trên mẫu, và904/1087 là tần suất nhỏ nhất/lớn nhất trong256giá trị byte. Đây là số liệu seed cố định của bộ test, không phải điều kiện bắt buộc mọi seed phải cho đúng các số đó.

Test C2 hiện kiểm cấu trúc từ và ký tự; không từ đó nói đã kiểm định thống kê đầy đủ phân phối Zipf hay đảm bảo1000 từ đều khác nhau. Generator test này dùng main, không nằm trong con số56 JUnit.

## 8. Kiểm chéo cả nhóm: generator → năm biến thể codec

Phần này dành khi thầy muốn thấy dữ liệu TV6 thực sự đi qua các codec và runner chung, không chỉ test riêng từng module.

```bash
mkdir -p "$CSD_REPORT_DIR/integration-classes" "$CSD_REPORT_DIR/generated"
javac --release 8 -encoding UTF-8 -cp target/classes \
  -d "$CSD_REPORT_DIR/integration-classes" scripts/benchmark/IntegrationCheck.java
java -Xmx1g -cp "target/classes:$CSD_REPORT_DIR/integration-classes" \
  IntegrationCheck "$CSD_REPORT_DIR/integration.csv" "$CSD_REPORT_DIR/generated"
```

Kết quả mong đợi:

```text
PASS 75 generator/codec round-trips with full SHA-256
PASS histograms/H0 of three orderings, 15 generator array/file equivalence cases
PASS common BenchmarkRunner/CsvWriter exercised on 15 cases; inspect CSV separately
```

75 =3dataset C1/C2/C3×5kích thước0/1/13/4096/1048576byte×5biến thể RLE/Huffman/LZW/LZ4-12/LZ4-16.

Ngoài round-trip, probe kiểm histogram/H0 sau original/grouped/shuffled và sinh byte[]/file có cùng kết quả ở15cặp dataset/kích thước. Nó gọi runner chung và CSV cho15case input4096byte.

Đọc các trường quan trọng trong CSV chung:

```bash
python3 - "$CSD_REPORT_DIR/integration.csv" <<'PYCHECK'
import csv, sys
with open(sys.argv[1]) as f:
    rows = list(csv.DictReader(f))
print('Rows:', len(rows))
for r in rows:
    print(r['dataset'], r['codec'], r['params'],
          'ratio=' + r['ratio'], 'seq=' + r['lz4_sequence_count'],
          'avg=' + r['lz4_avg_match_length'],
          'check=' + r['output_check'], r['status'])
assert len(rows) == 15 and all(r['status'] == 'PASS' for r in rows)
PYCHECK
```

Đây là15case kiểm pipeline, không có warm-up/repeat để xếp hạng tốc độ. File IntegrationCheck nằm ngoài src/test, được chạy riêng; không cộng75 vào số56 test JUnit. RoundTripTest.java chung hiện vẫn là khung in thông báo; probe này không tự biến file đó thành suite hoàn chỉnh.

Runner chung đang ghi output_check dạng PASS:prefix và có heap delta; runner LZ4 riêng ghi prefix16 và không có heap. Khi báo cáo phải nhận diện đúng file/schema. Heap trước/sau của runner chung **không phải peak heap**.

## 9. Đo/hiện bảng so sánh tốc độ cả nhóm

Nếu đã dùng chế độ `all` ở mục0, các bước đo/tổng hợp/hiện bảng đã được thực hiện tự động. Các lệnh dưới đây là tùy chọn khi muốn đọc lại bảng hoặc chỉ chạy benchmark.

Lệnh sau chỉ đọc số liệu cũ đã lưu, không tạo một phép đo mới:

```bash
python3 scripts/report-terminal.py benchmark \
  results/codec-comparison-20261027-125308-xyiK9z/summary.csv --mib 4
```

Muốn xem input1MiB, đổi thành `--mib 1`.

Để đo lại thực sự, chạy tuần tự trên máy ít tải:

```bash
bash scripts/benchmark/run-comparison.sh
CSD_COMPARE_DIR=$(cat /tmp/csd-latest-comparison-path)
python3 scripts/benchmark/summarize-comparison.py "$CSD_COMPARE_DIR"
python3 scripts/report-terminal.py benchmark "$CSD_COMPARE_DIR/summary.csv" --mib 4
```

Không chạy các benchmark song song. Khi bắt đầu quá trình quay, có thể dùng bảng đã đo và nói rõ đó là kết quả lưu trước; không nói lệnh đọc CSV vừa thực hiện840lượt đo.

Thiết kế phép đo so sánh:4kiểu dữ liệu×2size1/4MiB×5biến thể=40cấu hình;3JVM mới chạy tuần tự; mỗi JVM/cấu hình5warm-up+7lượt đo. Tổng840lượt đo+600warm-up, SHA đúng trong1440lượt. Mỗi ô tốc độ là median của3median JVM, mỗi JVM lấy median7lượt.

Bốn kiểu là SINGLE_BYTE, LONG_RUNS, WORD_TEXT và UNIFORM_RANDOM của harness, dùng seed20261027. Chúng không tự trở thành corpus R2/R3 hoặc input generator chính thức của nhóm.

Cách đọc bảng4MiB đã đo:

- LONG_RUNS: RLE nén≈1394MiB/s, giải nén≈1475MiB/s, ratio≈0,06263. LZ4-12≈206/346MiB/s, ratio≈0,17858. RLE tốt hơn trong ca này.
- WORD_TEXT: RLE nén≈181MiB/s nhưng ratio≈1,94088, nghĩa là file lớn gần gấp đôi. LZ4-16≈146/249MiB/s, ratio≈0,41995; LZW≈141/209MiB/s, ratio≈0,42094.
- UNIFORM_RANDOM: LZ4-12 nén≈187,7MiB/s, RLE≈184,6MiB/s, chênh rất nhỏ; không khẳng định thắng chắc. LZ4 giải nén≈12489–12836MiB/s nhờ dữ liệu gần như literal và bộ đệm RAM/cache nóng, không phải tốc độ ổ đĩa.
- SINGLE_BYTE: RLE nhanh; LZW có ratio thấp hơn. Thuật toán nhanh nhất có thể khác thuật toán nén nhỏ nhất.

Đây là số đã đo trên máy Ryzen5 6600H/JDK17 hiện tại; không cần cố làm lần quay mới ra đúng mọi chữ số. Chưa có kiểm định thống kê; chênh vài phần trăm coi là gần nhau. Chưa chạy đầy đủ corpus chuẩn/64MiB/đa seed/peak heap nên chưa thay báo cáo RQ3 chính thức.

## 10. Kịch bản nói ngắn khi quay

| Chặng | Hành động | Lời nói gợi ý |
|---|---|---|
| Mở đầu | Hiện Java/Maven và source | Em là TV5, phụ trách LZ4 hai hashlog12/16 và phối hợp đo RQ3. |
| Đúng thuật toán | Run full56, đọc bảng XML | Đây là test tính đúng.56lượt không failure/error/skip;18lượt thuộc TV5. |
| Ca khó | Chọn overlap hoặc block hỏng | Offset1 tái dùng byte vừa copy. Với input hỏng, test PASS khi decoder từ chối đúng lỗi. |
| Số liệu cụ thể | Tạo10input, chạy runner, mở bảng | Với20byte A, container26byte gồm header;2sequence,1match dài14. |
| File lớn/lặp | Chỉ input1MiB A | Container4138byte, ratio≈0,003946; khôi phục đúng SHA. |
| Hashlog | Chỉ hai dòng cùng input | Cùng dữ liệu được đo với12/16; không tự giả định16 luôn tốt hơn. |
| Phối hợp nhóm | Generator/integration nếu cần | C1/C2/C3 đã đi qua cả5biến thể và so SHA, histogram/H0 giữ nguyên sau đổi thứ tự. |
| Hiệu năng | Mở summary4MiB | Phải xét tốc độ nén, giải nén và ratio riêng; LZ4 không thắng mọi input. |
| Kết | Hiện đường dẫn kết quả | Log/XML/CSV lưu được để kiểm tra lại; kết quả sơ bộ chưa thay RQ3 đầy đủ. |

Nếu video ngắn, ưu tiên chặng full56 → overlap →10input/CSV → bảng so sánh. Các bảng phụ lục là phần chuẩn bị để trả lời khi thầy yêu cầu sâu hơn, không cần đọc hết56 tên test trong video.

## 11. Bản đồ đầy đủ các method test JUnit

Mỗi dòng bên dưới tương ứng một method, cột “Lượt” là số testcase trong XML hiện tại. Để chạy bất kỳ dòng nào:

```text
mvn '-Dtest=TÊN_SUITE#TÊN_METHOD' test
python3 scripts/report-terminal.py tests --suite TÊN_SUITE --details
```

### LZ4CodecTest — 16 lượt

| Method | Lượt | Kiểm tra/tiêu chí đạt |
|---|---:|---|
| `emptyAndShortInputsAreLiteralOnly` | 2 | Rỗng và mọi độ dài 1–12 byte: chỉ literal, container n+17 byte, không có match; rỗng có 1 sequence. |
| `allBytesAndLiteralExtensionBoundaries` | 2 | Đủ 256 giá trị byte; literal 13/14/15/16/254/255/256/269/270/271/525/4096; literal 15 có extension0, 270 có255,0. |
| `overlappingMatchesAndMatchExtensions` | 2 | Đoạn toàn A ở 13/14/23/24/25/278/279/280 byte và1MiB; offset1, match n−6, 2sequence/1match, tail5byte. |
| `seededRandomAndRepetitiveInputs` | 2 | 120 input mỗi hash log, seed20261021, độ dài dưới100000; xen kẽ random và chu kỳ; round-trip không sửa input/container. |
| `compatibleWithIndependentJavaOracleInBothDirections` | 2 | Hai chiều với lz4-java: thư viện giải payload tự viết; decoder tự viết giải fast/high payload thư viện; nhiều kích thước đến1MiB. |
| `handCraftedOffsetsAndMaximumDistance` | 1 | Block tạo thủ công offset8 và65535; đọc little-endian và khôi phục match đúng. |
| `rejectsCorruptHeadersAndTruncations` | 1 | Cắt container ở mọi vị trí; magic sai; original length âm/sai/vượt64MiB; byte dư bị từ chối. |
| `rejectsMalformedBlocksWithConsistentHeader` | 1 | Extension/literal/offset thiếu, offset0/lùi trước output, thiếu tail, tail không hợp lệ và extension cực dài bị từ chối. |
| `parametersAndNulls` | 1 | Mặc định hash12, tênLZ4; từ chối hashlog0/11/13/15/17/32; null nén/giải nén gây đúng exception. |
| `full64MiBBoundary` | 2 | 64MiB random và64MiB cùng byte khôi phục đúng ở cả hai hashlog; từ chối64MiB+1. |

### LZ4Rq3RunnerTest — 2 lượt

| Method | Lượt | Kiểm tra/tiêu chí đạt |
|---|---:|---|
| `csvHasBothHashLogsAndOnlyMeasuredIterations` | 1 | Input mẫu, warmup1/repeat2: CSV4 dòng đo, đủ12/16, thời gian>0, hash trùng, statusPASS; không ghi đè CSV có sẵn. |
| `emptyFileHasNoRatioAndKnownSha256` | 1 | File rỗng: ratio để trống, SHA-prefix e3b0c44298fc1c14, 1sequence/0match. |

### RLECodecTest — 7 lượt

| Method | Lượt | Kiểm tra/tiêu chí đạt |
|---|---:|---|
| `T01_emptyInputRoundTripsWithHeaderOnly` | 1 | Input rỗng: headerRLE1 12byte, original length0, round-trip và SHA đúng. |
| `T02_oneByteRoundTrips` | 1 | Một byte0xA5: cặp count1/value0xA5, khôi phục và SHA đúng. |
| `T03_oneMiBRepeatedByteUsesOnlyValidRunCounts` | 1 | 1MiB cùng byte: mọi count nằm1–255, payload thành cặp, khôi phục và SHA đúng. |
| `T07_runLengths255And256AreEncodedAsExpected` | 1 | Run255 thành một cặp; run256 tách255+1, cùng giá trị. |
| `T09_wrongMagicIsRejected` | 1 | Magic bị sửa phải ném IllegalArgumentException với thông báo Bad RLE1 magic. |
| `T12_seededRandomInputsAtMultipleSizesMatchSha256` | 1 | Seed20261021; input0/1/2/254/255/256/4096/1MiB; bytes/SHA trùng. |
| `malformedContainersAndNullArgumentsAreRejected` | 1 | Null, header ngắn, original length âm/quá lớn, cặp thiếu/count0/tổng count sai đều bị từ chối. |

### LZWCodecTest — 8 lượt

| Method | Lượt | Kiểm tra/tiêu chí đạt |
|---|---:|---|
| `testT01_EmptyInput` | 1 | Rỗng: header12byte và round-trip. |
| `testT02_SingleByte` | 1 | Một byte khôi phục đúng. |
| `testT03_RepeatedByte` | 1 | 1MiB cùng byte: khôi phục đúng và ratio nhỏ theo assertion của test. |
| `testT04_All256Bytes` | 1 | Đủ256 giá trị byte khôi phục đúng. |
| `testT05_KwKwK_ABABABA` | 1 | Chuỗi ABABABA đi qua nhánh KwKwK và khôi phục đúng. |
| `testT08_DictionaryFull` | 1 | Từ điển4096 mục đầy có ghi nhận vị trí; đóng băng rồi vẫn khôi phục đúng. |
| `testT09_InvalidMagic` | 1 | Magic sai phải bị từ chối. |
| `testRandomData` | 1 | Random theo test: SHA-256 trước/sau trùng. |

### HuffmanCodecTest — 23 lượt

| Method | Lượt | Kiểm tra/tiêu chí đạt |
|---|---:|---|
| `testT01_EmptyInput_HeaderSize` | 1 | Rỗng có header268byte. |
| `testT01_EmptyInput_RoundTrip` | 1 | Rỗng khôi phục đúng. |
| `testT02_SingleByte_RoundTrip` | 1 | Một byte khôi phục đúng. |
| `testT02_SingleByte_CompressedSize269` | 1 | Một byte thành container269byte:268header+1payload. |
| `testT02_SingleByte_CodeLength1Bit` | 1 | Input một ký hiệu dùng mã dài1bit. |
| `testT03_RepeatedByte_RoundTrip` | 1 | 1MiB cùng byte khôi phục đúng. |
| `testT03_RepeatedByte_TheoreticalSize` | 1 | Kích thước đúng268header+131072payload cho1MiB một ký hiệu. |
| `testT03_RepeatedByte_CompressionRatio` | 1 | Ratio của dữ liệu một ký hiệu đáp ứng assertion test. |
| `testT04_All256Bytes_RoundTrip` | 1 | Đủ256 giá trị byte khôi phục đúng. |
| `testT04_All256Bytes_All8Bits` | 1 | 256 ký hiệu đồng tần suất có mã8bit. |
| `testT09_InvalidMagic_Rejected` | 1 | Magic sai bị từ chối. |
| `testT10_PayloadBitsFormula_ABRACADABRA` | 1 | Đối chiếu tổng bit payload Σfreq×codeLength trên ABRACADABRA. |
| `testT10_PayloadBitsFormula_Shakespeare` | 1 | Đối chiếu tổng bit payload trên mẫu Shakespeare trong test. |
| `testT10_PayloadBitsFormula_Seashore` | 1 | Đối chiếu tổng bit payload trên mẫu Seashore. |
| `testT10_PayloadBitsFormula_CSD201Text` | 1 | Đối chiếu tổng bit payload trên mẫu CSD201. |
| `testT12_RandomData_100KB_SHA256` | 1 | Random100KB theo test: SHA khớp. |
| `testT12_SimulatedText_300KB_SHA256` | 1 | Văn bản mô phỏng300KB theo test: SHA khớp. |
| `testT12_SimulatedText_CompressionRatio` | 1 | Văn bản mô phỏng có ratio đáp ứng assertion test. |
| `testShortText_VietnameseUTF8` | 1 | Chuỗi tiếng Việt UTF-8 khôi phục đúng. |
| `testBitWriter_WriteAndPadding` | 1 | Ghi bit MSB-first, flush/padding đúng chuỗi test. |
| `testBitReader_ReadBitAndBits` | 1 | Đọc lại từng bit và nhóm bit theo chuỗi test. |
| `testBoundary_NullInputRejected` | 1 | Null bị từ chối. |
| `testBoundary_TruncatedDataRejected` | 1 | Container quá ngắn bị từ chối. |


## 12. Lỗi thường gặp khi demo

| Dấu hiệu | Xử lý |
|---|---|
| mvn không tìm thấy | Kiểm đường dẫn cache hoặc Maven trên PATH ở bước2 |
| ClassNotFoundException | Đứng đúng root project, chạy compile/test trước, dùng đầy đủ package |
| FileAlreadyExistsException | Runner không ghi đè CSV. Tạo thư mục quay mới thay vì xóa kết quả cũ |
| report có ít hơn56test | Vừa chạy riêng method/suite; chạy clean verify để trở lại bộ báo cáo đầy đủ |
| BUILD SUCCESS nhưng có Failures/Errors | POM đang ignore test failure; đọc XML bằng helper và xử lý lỗi trước khi báo cáo |
| Tests có Skipped>0 | Nói rõ ca nào không chạy, không trình bày như toàn bộ đã kiểm chứng |
| Ratio>1 | Hợp lệ với file nhỏ hoặc khó nén; chỉ rõ header/overhead |
| AvgMatch=0 | Input đó không có match, phần lớn là literal |
| Tốc độ thay đổi mỗi lần | Có JIT/GC/cache/tần số CPU/tiến trình nền; dùng warm-up, nhiều lượt và median |
| Hash16khác hash input | Kiểm đúng input/dòng/log; runner đáng ra dừng khi toàn SHA khác. Không dùng CSV đang FAIL để xếp hạng |
| clean xóa target | Đó là hành vi chuẩn; log/CSV đặt trong results/video-report-* không bị xóa |
| Chỉ chạy Main và thấy tên project | Đó chưa phải chạy test; Main hiện mới in banner |

Không cố tình sửa source thành sai để quay minh họa. Các unit test dữ liệu hỏng đã tự tạo input xấu và xác nhận decoder từ chối.

## 13. Chế độ một lệnh tự chạy

```bash
bash scripts/demo-terminal.sh tv5
# hoặc toàn nhóm:
bash scripts/demo-terminal.sh all
```

`tv5` chạy18JUnit và demo LZ4 trên README với1warm-up/2lượt đo, tạo4dòngCSV.

`all` chạy56JUnit, generator main, rồi đo **năm biến thể RLE/Huffman/LZW/LZ4-12/LZ4-16**:40cấu hình×3JVM×7lượt=840dòngđo, thêm600warm-up. Hiện bảng1MiB và4MiB ngay trên terminal; chỉ một lệnh là đủ cho cả nhóm. Chế độ này không chạy lại demo README riêng LZ4.

Bộ10input/100dòng ở bước5 là luồng minh họa LZ4 thủ công khác, không cộng nhầm với840lượt benchmark chung.

Script lưu log/XML ở results/video-..., benchmark cả nhóm trong comparison/. Generator được ghi đầy đủ vào generator.log và chỉ in phần tóm tắt để quay dễ đọc. Script không quay video; dùng công cụ quay màn hình của bạn để ghi terminal.

## 14. Kết quả xác minh hướng dẫn này

- Cập nhật mới: `all` đã chạy thành công cả benchmark840lượt và in đủ bảng tốc độ1/4MiB. Kết quả lần mới xem [[CSD201_SO_SANH_TOC_DO_CODEC_2026_10_27]].

- Helper đọc đúng56test từ5suite XML và16test LZ4 khi lọc riêng.
- Đã chạy selector overlap:2lượtPASS, sau đó clean verify để phục hồi bộ56testPASS.
- Tạo10file mẫu, runner3warm-up/5đo:100dòngđoPASS, khớp các kích thước boundary trong bảng;160lượt kiểm SHA tính cảwarm-up.
- IntegrationCheck đã chạy lại:75round-tripPASS, histogram/H0 và15cặp array/filePASS, CSV chung được tạo.
- Helper benchmark đã hiển thị đúng20dòng summary ở4MiB, không chạy lại benchmark khi chỉ đọc bảng.
- Không sửa thuật toán hay POM để làm hướng dẫn; đây là bổ sung công cụ trình bày và tài liệu.

Các ghi chú liên quan: [[CSD201_KIEM_TRA_LAI_SOURCE_2026_10_27]], [[CSD201_SO_SANH_TOC_DO_CODEC_2026_10_27]], [[CSD201_TV5_LZ4_HUONG_DAN_TEST_VA_GIAI_THICH_CODE]], [[TASK_BOARD]].

