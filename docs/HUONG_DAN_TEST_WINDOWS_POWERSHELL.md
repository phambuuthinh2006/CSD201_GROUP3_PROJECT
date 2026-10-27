# Test và đo tốc độ trên Windows PowerShell

Dùng trực tiếp PowerShell trên Windows; không cần WSL, Git Bash hoặc các lệnh Linux. Các script này gọi cùng codec Java và cùng harness benchmark như bản Linux.

## 1. Chuẩn bị

Cài sẵn **JDK 17**, **Maven 3.6.3+**, **Python 3.9+**. Mở một tab PowerShell trong Windows Terminal, vào thư mục repo đã clone:

```powershell
Set-Location "C:\Users\TEN_BAN\Downloads\CSD201_GROUP3_PROJECT"
java -version
javac -version
mvn -version
py -3 --version
```

Nếu không có launcher `py`, thử `python --version`. Script tự tìm `py`, rồi `python`, rồi `python3`. Máy cần Python3 thật, không chỉ shortcut mở Microsoft Store. JDK phải có cả `java` và `javac`; JRE riêng không đủ cho phần biên dịch harness.

Đường dẫn ví dụ phải đổi theo máy bạn. Đường dẫn có dấu cách được truyền dưới dạng đối số riêng, không cần đổi tên thư mục.

Nếu Maven chưa ở PATH nhưng đã giải nén:

```powershell
$env:CSD_MAVEN_BIN = "C:\Tools\apache-maven-3.9.9\bin\mvn.cmd"
```

Không cần chạy lệnh này nếu `mvn -version` đã được. Lần đầu Maven có thể cần mạng để tải dependency.

## 2. Một lệnh dùng cho cả nhóm

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\demo-terminal.ps1" -Mode all
```

Nếu dùng PowerShell7, có thể thay `powershell.exe` bằng `pwsh`.

`-ExecutionPolicy Bypass` ở lệnh trên chỉ áp dụng cho tiến trình đang chạy script, không đổi chính sách toàn máy. Không cần chạy Set-ExecutionPolicy cho máy.

Script thực hiện:

1. In phiên bản Java/Maven và thư mục kết quả.
2. Chạy toàn bộ56 test JUnit hiện có, đọc XML để phát hiện failure/error kể cả POM đang ignore test failure.
3. Chạy test generator C1/C2/C3; lưu đầy đủ generator.log và hiện phần tóm tắt.
4. Đo mới tốc độ **RLE, Huffman, LZW, LZ4-12, LZ4-16** trên cùng dữ liệu và điều kiện.
5. Hiện bảng ratio, tốc độ nén và giải nén cho1MiB, rồi4MiB.
6. Lưu transcript, báo cáo, CSV, SHA input/source và thông tin môi trường.

Bốn thuật toán tạo thành năm biến thể vì LZ4 có hai hashlog. Thứ tự codec được xáo trộn có seed, ba JVM chạy tuần tự; mọi lượt kiểm đầy đủ SHA-256.

## 3. Đọc số liệu cho thầy

Bảng unit test:

| Suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| RLECodecTest | 7 | 0 | 0 | 0 |
| HuffmanCodecTest | 23 | 0 | 0 | 0 |
| LZWCodecTest | 8 | 0 | 0 | 0 |
| LZ4CodecTest | 16 | 0 | 0 | 0 |
| LZ4Rq3RunnerTest | 2 | 0 | 0 | 0 |
| Tổng | 56 | 0 | 0 | 0 |

Đây là số lượt kiểm tính đúng của phiên bản hiện tại. Không dùng `Time elapsed` của JUnit để xếp hạng tốc độ codec. Test generator dùng main, không nằm trong số56.

Bảng tốc độ:

```text
Dataset | Codec | Ratio | Compress MiB/s | Decompress MiB/s
```

- Ratio = toàn bộ container nén/input gốc, gồm header; **nhỏ hơn tốt hơn**.
- Compress MiB/s = tốc độ nén; **lớn hơn tốt hơn**.
- Decompress MiB/s = tốc độ giải nén, đo riêng; **lớn hơn tốt hơn**.
- Hai tốc độ đều tính theo số byte gốc; không tính đọc file, tạo input, SHA hoặc ghi CSV.
- Ratio>1 có thể hợp lệ: input nhỏ hoặc khó nén bị phình ra.
- Mỗi ô tốc độ là median của3median JVM, mỗi JVM lấy median7lượt sau5warm-up.

Tổng40cấu hình×3JVM×7lượt=840dòngđo; thêm600warm-up. Bốn kiểu input là SINGLE_BYTE, LONG_RUNS, WORD_TEXT, UNIFORM_RANDOM, mỗi kiểu1/4MiB, seed20261027.

Không lấy số MiB/s đo trên Linux rồi nói đó là số của Windows. Mỗi máy sẽ có số thời gian riêng. Mục tiêu là so các codec **trên cùng máy**, cùng lượt chạy; chênh vài phần trăm có thể là nhiễu.

Đây là benchmark khảo sát của implementation Java nhóm. Chưa thay RQ3 chính thức với corpus Silesia/Canterbury,64MiB, nhiều seed và peak heap. Không chạy hai benchmark song song.

## 4. Riêng phần TV5

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\demo-terminal.ps1" -Mode tv5
```

Chạy18JUnit TV5 rồi đo LZ4-12/16 trên README: một warm-up, hai lượt đo mỗi hashlog, tạo4dòngCSV. Terminal hiện cả kích thước/ratio/thống kê/hash và median tốc độ của hai lượt.

Để chạy một ca khi thầy hỏi:

```powershell
mvn '-Dtest=LZ4CodecTest#overlappingMatchesAndMatchExtensions' test
py -3 -X utf8 .\scripts\report-terminal.py tests --suite LZ4CodecTest --details
```

Ca này có2lượt (hash12/16). Muốn trở về tổng56test mới, chạy `mvn clean verify` rồi đọc lại XML. Các ca khác xem bảng method đầy đủ trong `docs/KICH_BAN_BAO_CAO_TEST_TERMINAL_DAY_DU.md`; lệnh Maven dùng được trên Windows nếu giữ nguyên dấu nháy quanh `-Dtest=...`.

## 5. Chỉ đo tốc độ tất cả codec, bỏ qua unit test

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\benchmark\run-comparison.ps1"
```

Script này tự compile, đo3JVM, tổng hợp và in bảng1/4MiB. Các round-trip benchmark vẫn kiểm SHA-256, nhưng không thay bước full JUnit khi nghiệm thu.

Có thể chỉ định thư mục mới, kể cả có dấu cách:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\benchmark\run-comparison.ps1" -OutputDirectory "results\comparison new"
```

OutputDirectory phải chưa tồn tại. Khi để trống, script tạo tên có thời gian và suffix riêng.

## 6. Kết quả nằm ở đâu?

```text
results/video-windows-YYYYMMDD-HHMMSS-xxxxxxxx/
├── terminal.log
├── generator.log            # mode all
├── TEST-*.xml
├── comparison/              # mode all
│   ├── raw-fork-1.csv
│   ├── raw-fork-2.csv
│   ├── raw-fork-3.csv
│   ├── inputs-fork-*.csv
│   ├── environment.txt
│   ├── source-sha256.txt
│   ├── summary.csv
│   ├── fork-medians.csv
│   └── SO_SANH_CODEC.md
└── lz4-demo.csv              # chỉ mode tv5
```

Chạy benchmark riêng tạo `results/codec-comparison-.../`. `environment.txt` ghi OS/Java, processor count, JVM flags; trên Windows còn thử lấy CPU/RAM qua CIM. Nếu CIM không có, file ghi rõ để bổ sung thủ công. Không gắn cứng CPU/hệ điều hành vào báo cáo của máy khác.

Các thư mục video là dữ liệu cục bộ được gitignore. Muốn nộp kết quả, chọn CSV/log từ đúng lần chạy. `mvn clean` xóa target nhưng không xóa các kết quả này.

Đọc lại bảng của lần **all** gần nhất, không đo lại:

```powershell
$CsdRun = Get-ChildItem .\results -Directory -Filter 'video-windows-*' |
    Where-Object { Test-Path (Join-Path $_.FullName 'comparison/summary.csv') } |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $CsdRun) { throw 'Chưa có kết quả mode all.' }
$CsdSummary = Join-Path $CsdRun.FullName 'comparison/summary.csv'
py -3 -X utf8 .\scripts\report-terminal.py benchmark "$CsdSummary" --mib 4
```

Nếu không dùng launcher py, thay `py -3` bằng `python`. File transcript/log không phải video: dùng công cụ quay màn hình của bạn để ghi terminal.

## 7. Chạy tay generator và classpath Windows

```powershell
mvn test-compile
java -Xmx1g -cp "target\classes;target\test-classes" vn.csd.compression.DataGeneratorTest
```

Classpath Windows phân cách bằng **dấu chấm phẩy `;`**, và phải có dấu nháy bao quanh. Các script tự chọn dấu đúng hệ điều hành, nên không cần tự sửa classpath trong script.

Không dùng nguyên dòng `target/classes:target/test-classes` của Linux trên Windows. Không dùng `export`, `mkdir -p`, `sha256sum`, `grep`, hoặc `bash` trong PowerShell; bản ps1 không phụ thuộc những lệnh đó.

Xem SHA file bằng PowerShell:

```powershell
Get-FileHash .\README.md -Algorithm SHA256
```

Nếu tạo10input theo hướng dẫn đầy đủ, script Python dùng được trên cả hai hệ điều hành. Dùng `py -3 -X utf8` thay `python3`, đường dẫn trong dấu nháy; riêng lệnh glob truyền cho Java cần liệt kê file bằng Get-ChildItem (xem đoạn dưới), không dựa vào wildcard của Bash:

```powershell
$CsdCaseDir = Join-Path $PWD ('results/video-windows-cases-' + [Guid]::NewGuid().ToString('N').Substring(0, 8))
$null = New-Item -ItemType Directory -Path $CsdCaseDir
py -3 -X utf8 .\scripts\create-demo-inputs.py (Join-Path $CsdCaseDir 'inputs')
$CsdInputs = @(Get-ChildItem (Join-Path $CsdCaseDir 'inputs') -File | Sort-Object Name | ForEach-Object { $_.FullName })
$CsdCsv = Join-Path $CsdCaseDir 'lz4-cases.csv'
java -Xmx1g -cp target/classes vn.csd.compression.metrics.LZ4Rq3Runner "$CsdCsv" 3 5 @CsdInputs
if ($LASTEXITCODE -ne 0) { throw 'LZ4 runner failed.' }
py -3 -X utf8 .\scripts\report-terminal.py csv "$CsdCsv"
```

## 8. Lỗi thường gặp

| Dấu hiệu | Cách xử lý |
|---|---|
| java/javac không tìm thấy | Kiểm JDK17/bin trên PATH, mở terminal mới sau cài đặt |
| mvn.cmd không tìm thấy | Kiểm Maven/bin hoặc đặt CSD_MAVEN_BIN |
| Python mở Microsoft Store | Cài Python3 thật/launcher py và kiểm py -3 --version |
| Script không tìm thấy | Clone/pull đúng repo và phải có scripts/common.ps1, demo-terminal.ps1, benchmark và các helper Python/Java |
| Execution policy chặn file | Dùng lệnh powershell.exe với -ExecutionPolicy Bypass cho riêng lần chạy như mục2; không đổi chính sách toàn máy |
| Test có failure nhưng Maven BUILD SUCCESS | Script đọc XML và sẽ dừng; xử lý test trước khi báo cáo |
| Đường dẫn có dấu cách | Dùng dấu nháy; không tự ghép một chuỗi lệnh rồi Invoke-Expression |
| ClassNotFoundException | Chạy compile/test-compile trước; classpath Windows dùng dấu ; trong dấu nháy |
| Output directory đã tồn tại | Đổi tên hoặc để script tạo tên mới; không ghi đè kết quả cũ |
| Unicode bị lỗi khi chạy tay | Dùng py -3 -X utf8; script đặt UTF-8 console trong phiên chạy |
| Máy khác ra tốc độ khác | Bình thường; ghi môi trường, so cùng máy, nhiều lượt và median |

## 9. Những file cần đưa lên GitHub

Đưa lên toàn bộ thư mục `scripts/` để giữ đủ dependency:3file ps1, Java harness, summarizer và report helper. Thêm hướng dẫn này và các hướng dẫn liên quan trong docs, README, thay đổi gitignore dành cho kết quả video. Không chỉ gửi riêng một file ps1.

Không cần đưa target/, kết quả quay video cục bộ hoặc toàn bộ thư mục Obsidian lên GitHub. Hai báo cáo review đang untracked là tài liệu khác; tự chọn nếu muốn đưa vào commit riêng.

Có thể chọn đúng các file liên quan bằng lệnh sau ở root repo (đây chỉ là hướng dẫn, trợ lý chưa chạy git add):

```powershell
git add scripts README.md .gitignore docs/HUONG_DAN_TEST_WINDOWS_POWERSHELL.md docs/HUONG_DAN_QUAY_VIDEO_TEST_TERMINAL.md docs/KICH_BAN_BAO_CAO_TEST_TERMINAL_DAY_DU.md
git diff --cached --stat
```

Xem danh sách staged trước khi tự commit/push; không dùng git add toàn bộ nếu bạn chưa muốn đưa hai báo cáo review vào cùng commit.

Sau khi bạn commit/push, người khác pull/clone sẽ có script và có thể dùng mục2. Trong tác vụ này trợ lý không commit, stage hoặc push thay bạn.

## 10. Phạm vi đã kiểm chứng

- PowerShell7.4.6 **trên Linux**: parse cả3ps1 PASS; mode all chạy56JUnitPASS, generatorPASS,840dòngbenchmarkPASS và in đủ bảng1/4MiB.
- Mode tv5 chạy qua đường dẫn có dấu cách:18JUnitPASS,4dòngCSV PASS.
- Native command trả exit7 được wrapper nhận và báo lỗi, không tiếp tục giả thành công. XML test giả có failure cũng làm script dừng; output directory đã tồn tại bị từ chối trước khi chạy benchmark.
- Đã bỏ thông tin CPU/Linux cố định khỏi summarizer và chỉ đọc môi trường của phép đo thực tế; file CSV/Markdown dùng UTF-8 rõ ràng.
- **Chưa chạy trên Windows thật**, chưa xác minh trực tiếp Windows PowerShell5.1, mvn.cmd hoặc nhánh thu thập phần cứng Windows/CIM. Mã dùng cú pháp tương thích5.1/7 và classpath theo OS; thành viên Windows cần chạy lần đầu để xác nhận môi trường máy họ.

Tham khảo thêm: `docs/KICH_BAN_BAO_CAO_TEST_TERMINAL_DAY_DU.md` có50method/56lượt và ý nghĩa từng ca; `docs/HUONG_DAN_QUAY_VIDEO_TEST_TERMINAL.md` là luồng Linux.
