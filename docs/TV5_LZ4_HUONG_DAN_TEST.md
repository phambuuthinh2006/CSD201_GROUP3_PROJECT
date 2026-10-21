---
tags:
  - CSD201
  - LZ4
  - TV5
  - unit-test
created: 2026-10-21
---

# CSD201 — TV5: Hướng dẫn test và giải thích code LZ4

> Mục đích: giúp thành viên 5 tự chạy kiểm thử, hiểu từng phần của `LZ4Codec.java`, giải thích khi demo và bàn giao số liệu LZ4 cho thành viên 6.
>
> Mã nguồn nằm tại `/home/phambuuthinh/Downloads/CSD201_GROUP3_PROJECT`.

## 1. Các file cần biết

| File trong project | Vai trò |
|---|---|
| `pom.xml` | Cấu hình build, JUnit, thư viện oracle chỉ dùng khi test |
| `src/main/java/vn/csd/compression/codec/Codec.java` | Interface chung của các codec |
| `src/main/java/vn/csd/compression/codec/LZ4Codec.java` | Thuật toán nén, giải nén và thống kê LZ4 |
| `src/test/java/vn/csd/compression/LZ4CodecTest.java` | Kiểm thử thuật toán, dữ liệu hỏng, tương thích |
| `src/main/java/vn/csd/compression/metrics/LZ4Rq3Runner.java` | Chạy đo LZ4 với hash log 12 và 16 |
| `src/test/java/vn/csd/compression/LZ4Rq3RunnerTest.java` | Kiểm thử CSV và hoạt động runner |
| `docs/TV5_LZ4_RQ3.md` | Chi tiết bàn giao TV6 và các đầu vào còn thiếu |
| `results/lz4-smoke.csv` | Kết quả chạy thử trên file source Java |

Mở nhanh [thư mục project](../README.md) hoặc [LZ4Codec.java](../src/main/java/vn/csd/compression/codec/LZ4Codec.java). Bản trong repository dùng liên kết tương đối để xem trên GitHub.

## 2. Chạy test từ terminal

### 2.1. Chuẩn bị

Dùng JDK 17 như môi trường đã kiểm chứng và Maven 3.6.3 trở lên. Cấu hình chung dùng `source=1.8`, `target=1.8` để tạo bytecode Java 8; mã TV5 đã từng biên dịch riêng với `release=8` trước khi tích hợp; chưa kiểm chứng bằng JVM 8 thực tế.

```bash
cd /home/phambuuthinh/Downloads/CSD201_GROUP3_PROJECT
java -version
mvn -version
```

Nếu báo `mvn: command not found`, máy chưa có Maven trên PATH. Trong phiên triển khai đã tải Maven 3.9.9 tại `/tmp/apache-maven-3.9.9`. Nếu thư mục này vẫn còn, chạy:

```bash
/tmp/apache-maven-3.9.9/bin/mvn -version
```

Có thể thay `mvn` trong các lệnh bên dưới bằng đường dẫn đó. Thư mục `/tmp` có thể bị dọn sau khi khởi động lại; khi đó cài Maven hoặc cấu hình Maven của IDE. Lần chạy đầu cần mạng để tải dependency.

### 2.2. Chạy toàn bộ test

```bash
mvn test
```

Nếu dùng Maven tạm trên máy hiện tại:

```bash
/tmp/apache-maven-3.9.9/bin/mvn test
```

Kết quả mong đợi theo bộ test hiện tại:

```text
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Có 16 lượt kiểm thử codec và 2 kiểm thử runner. Các test tham số được tính thành nhiều lượt vì chạy cho cả hash log 12 và 16. Một số test còn lặp nhiều input bên trong nên số input thực tế lớn hơn 18.

### 2.3. Chạy riêng phần cần xem

```bash
# Chỉ kiểm thử codec
mvn -Dtest=LZ4CodecTest test

# Chỉ kiểm thử runner
mvn -Dtest=LZ4Rq3RunnerTest test

# Chỉ ca dữ liệu 64 MiB, chạy cho cả hai hash log
mvn '-Dtest=LZ4CodecTest#full64MiBBoundary' test

# Chỉ kiểm tra tương thích với thư viện độc lập
mvn '-Dtest=LZ4CodecTest#compatibleWithIndependentJavaOracleInBothDirections' test
```

Trong IntelliJ, mở project qua `pom.xml`, chọn JDK 17, chờ Maven tải dependency rồi bấm nút chạy cạnh tên class test. Chạy bằng Maven là cách đối chiếu chuẩn vì `pom.xml` đặt heap test thành `-Xmx1g`.

### 2.4. Xem báo cáo và đóng gói

Báo cáo test nằm trong:

```text
target/surefire-reports/
```

Đọc file `.txt` để xem tổng số lượt và lỗi; file `TEST-*.xml` dùng cho công cụ tổng hợp kết quả.

```bash
mvn package
```

Lệnh này chạy test rồi tạo `target/compression-project-1.0-SNAPSHOT.jar`. Nếu chỉ cần biên dịch trước khi chạy runner:

```bash
mvn compile
```

JAR hiện không cấu hình `Main-Class`; chạy runner bằng `java -cp target/classes ...` như phần 8.

## 3. Bộ test đang kiểm tra những gì?

| Nhóm test | Ý nghĩa |
|---|---|
| Rỗng, 1 byte, mọi độ dài dưới 13 | Không tìm match khi dữ liệu quá ngắn; input rỗng vẫn có token cuối |
| Đủ 256 giá trị byte | Không xử lý nhầm byte âm của Java |
| Literal length 15, 270 và lân cận | Extension length đúng, đặc biệt byte extension cuối bằng 0 |
| Match length 19, 274 và lân cận | Nibble match tràn sang extension đúng |
| 1 MiB cùng một byte | Copy match chồng lấn, đặc biệt offset bằng 1 |
| Random và dữ liệu có chu kỳ với seed cố định | Round-trip trên nhiều kiểu dữ liệu có thể tái tạo |
| Giải nén bằng hash log khác phía nén | Decoder không phụ thuộc kích thước bảng hash encoder |
| Offset 8 và 65.535 tạo thủ công | Đọc offset little-endian và hỗ trợ khoảng cách tối đa |
| 64 MiB random và 64 MiB cùng một byte | Đúng ở giới hạn input, với cả hash log 12 và 16 |
| Input lớn hơn 64 MiB | Từ chối vượt giới hạn |
| Magic, header, offset, extension hoặc tail hỏng | Decoder từ chối bằng lỗi có kiểm soát |
| Oracle `lz4-java` | Payload tự nén được thư viện giải; decoder tự viết giải được payload fast/high của thư viện |
| Runner | Hai hash log, số dòng đo không tính warm-up, SHA-256, ratio file rỗng và không ghi đè CSV |

Hàm trợ giúp `roundTrip()` còn kiểm tra input và dữ liệu nén không bị sửa sau khi gọi API.

> `lz4-java` có scope `test`. Mã thuật toán trong `src/main` tự cài đặt, không gọi thư viện đó.

## 4. Ý tưởng thuật toán và định dạng dữ liệu

LZ4 tìm chuỗi byte lặp lại trong dữ liệu trước đó. Byte chưa tìm được chuỗi thay thế được ghi nguyên dạng, gọi là **literal**. Chuỗi lặp được mô tả bằng **offset** và **match length**.

Ví dụ output đã có `AB`, gặp match offset 2, length 6 thì decoder nối thêm `ABABAB`. Chỉ cần chỉ dẫn sao chép thay cho lưu lại cả sáu byte.

Một sequence thường có dạng:

```text
token → literal-length extension nếu cần → literals
      → offset 2 byte → match-length extension nếu cần
```

Sequence cuối chỉ chứa token, extension literal nếu cần và literals; không có offset/match.

### Container bên ngoài

| Byte offset | Kích thước | Ý nghĩa |
|---|---|---|
| 0–3 | 4 byte | Magic `LZ4B` |
| 4–11 | 8 byte | Độ dài input gốc, big-endian |
| 12–15 | 4 byte | Độ dài raw block, big-endian, không tính header |
| Từ 16 | Theo block length | Raw LZ4 block |

`ByteBuffer` mặc định big-endian nên các lệnh `putInt`, `putLong`, `getInt`, `getLong` dùng đúng quy ước header này.

**Trường block length BE là lựa chọn triển khai cần TV6 xác nhận với đề gốc.** Offset trong payload vẫn dùng little-endian theo LZ4. Container `LZ4B` của đồ án khác LZ4 Frame.

### Token và extension length

Token có hai nửa, mỗi nửa 4 bit:

```text
bit 7..4: min(literal length, 15)
bit 3..0: min(match length - 4, 15)
```

Match tối thiểu dài 4 byte nên chỉ cần lưu `length - 4`.

Khi một nửa token bằng 15, phải đọc/ghi extension: mỗi byte 255 yêu cầu thêm một byte kế tiếp; byte nhỏ hơn 255 kết thúc dãy.

| Độ dài cần biểu diễn | Token nibble | Extension |
|---|---|---|
| Literal 14 | 14 | Không có |
| Literal 15 | 15 | `0` |
| Literal 270 | 15 | `255, 0` |
| Match 18 | 14 | Không có |
| Match 19 | 15 | `0` |
| Match 274 | 15 | `255, 0` |

## 5. Giải thích `compress(byte[] input)`

### 5.1. Chuẩn bị bộ đệm và bảng hash

Hàm từ chối `null` và input vượt 64 MiB. Sau đó tạo:

```java
int[] table = new int[1 << hashLog];
Arrays.fill(table, -1);
```

Mỗi ô lưu một vị trí đã gặp trong input; `-1` nghĩa là chưa có ứng viên. Khi trùng ô hash, vị trí mới thay vị trí cũ.

`ByteArrayOutputStream` chứa container và payload. Hàm ghi trước 16 byte 0 để dành chỗ cho header; sau khi nén mới điền magic và độ dài chính xác.

### 5.2. Hai biến quan trọng

| Biến | Ý nghĩa |
|---|---|
| `pos` | Vị trí đang tìm match |
| `anchor` | Vị trí đầu tiên của đoạn literal chưa ghi |
| `candidate` | Vị trí cũ do bảng hash đề xuất |

Nếu chưa tìm thấy match, tăng `pos`; `anchor` giữ nguyên để gom literal thành một đoạn.

### 5.3. Tìm ứng viên bằng hash

```java
return (readIntLE(input, pos) * 0x9e3779b1) >>> (32 - hashLog);
```

- `readIntLE` ghép 4 byte thành một `int`, dùng `& 255` để bỏ sign extension của byte Java.
- Nhân hằng số để trộn các bit; overflow `int` ở đây là chủ ý.
- `>>>` dịch phải không dấu để lấy 12 hoặc 16 bit cao làm chỉ số bảng.

Có cùng hash chưa đủ kết luận giống nhau. Encoder luôn so lại 4 byte thực tế. Ứng viên bị bỏ nếu chưa có, cách xa hơn 65.535 byte hoặc 4 byte không bằng nhau.

### 5.4. Mở rộng match và ghi sequence

Khi 4 byte đầu bằng nhau, bắt đầu `length = 4`, rồi so từng byte tiếp theo để kéo dài match. Điều kiện dừng giữ lại 5 byte cuối làm literal.

Các giá trị được tính:

```java
int literals = pos - anchor;
int matchCode = length - 4;
int offset = pos - candidate;
```

Hàm ghi token, extension literal nếu cần, literal, offset và extension match nếu cần. Offset được tách byte thấp trước:

```java
out.write(offset & 255);
out.write(offset >>> 8);
```

Sau đó cập nhật bảng hash cho các vị trí trong match, chuyển `pos` và `anchor` đến cuối match. Đây là cách tìm greedy với một candidate mỗi ô hash; không tìm hết mọi chuỗi trùng trước đó.

### 5.5. Kết thúc block

Vòng tìm match chỉ chạy khi `pos <= n - 12`. Vì vậy match cuối bắt đầu cách cuối input ít nhất 12 byte. Phần mở rộng match dừng trước 5 byte cuối.

Sau vòng lặp, ghi toàn bộ đoạn từ `anchor` đến cuối thành sequence literal cuối. Với input dưới 13 byte, không có match hợp lệ, chỉ có literal.

Cuối cùng:

```java
ByteBuffer.wrap(result)
    .putInt(magic)
    .putLong(n)
    .putInt(result.length - headerSize);
```

Input rỗng tạo container 17 byte: header 16 byte và token 0. File ngắn hoặc khó nén có thể lớn hơn input do overhead; điều đó vẫn hợp lệ.

## 6. Giải thích `decompress(byte[] compressed)`

### 6.1. Kiểm tra header bằng `originalSize()`

Hàm kiểm tra có đủ header và ít nhất một byte block, magic đúng, original length nằm trong 0–64 MiB, block length bằng số byte payload thực tế. Dữ liệu thừa hoặc bị cắt khiến độ dài không khớp sẽ bị từ chối.

### 6.2. Vì sao gọi `parse()` hai lần?

```java
parse(compressed, size, null);
byte[] result = new byte[size];
parse(compressed, size, result);
```

Lần đầu kiểm tra cấu trúc và kích thước mà chưa cấp phát output theo độ dài khai báo. Lần sau mới ghi dữ liệu giải nén. Cách này thêm chi phí parse nhưng giúp phát hiện block hỏng trước khi tạo mảng output lớn. Thời gian giải nén trong benchmark bao gồm cả hai lần gọi.

### 6.3. Một vòng giải mã sequence

`cursor.pos` theo dõi vị trí đọc trong compressed; `written` là số byte output đã tái tạo.

1. Đọc token.
2. Đọc literal length, cộng extension nếu nibble cao bằng 15.
3. Kiểm tra còn đủ input/output rồi chép literal.
4. Nếu đã hết block: kiểm tra output đúng độ dài và điều kiện sequence cuối.
5. Nếu còn byte: đọc offset, yêu cầu `offset > 0` và `offset <= written`.
6. Đọc match length bằng nibble thấp cộng extension và cộng 4.
7. Chép match, cập nhật `written`, tiếp tục sequence kế tiếp.

`readLength()` kiểm tra phần cộng thêm với `limit - length` trước khi cộng, tránh vượt giới hạn output và tránh cộng dồn thành số quá lớn.

### 6.4. Vì sao match phải copy từ trái sang phải?

```java
for (int i = 0; i < length; i++) {
    output[written + i] = output[written + i - offset];
}
```

Ví dụ output có một byte `A`, offset 1 và match length 7. Mỗi byte vừa ghi trở thành nguồn cho byte kế tiếp, tạo thêm bảy byte `A`.

`System.arraycopy` có ngữ nghĩa xử lý vùng chồng lấn như sao chép qua vùng tạm; nó không tạo được chuỗi lặp theo cách này khi match dài hơn offset. Vì vậy literal dùng `arraycopy`, match dùng vòng lặp tiến.

### 6.5. Dữ liệu hỏng bị xử lý thế nào?

Decoder ném `IllegalArgumentException` cho header/block hỏng. Truyền `null` gây `NullPointerException`. Những kiểm tra này phát hiện cấu trúc không hợp lệ; container không có checksum nên thay đổi byte mà vẫn tạo cấu trúc hợp lệ có thể không bị decoder phát hiện. Runner đối chiếu SHA-256 với input gốc để xác minh round-trip.

## 7. HASH_LOG 12/16 và thống kê

| Tham số | Số ô hash | Dung lượng phần tử `int[]` |
|---|---:|---:|
| 12 | 4.096 | 16 KiB |
| 16 | 65.536 | 256 KiB |

Bảng 16 lớn gấp 16 lần, thường giảm va chạm hash nhưng hiệu năng còn phụ thuộc dữ liệu và cache. Cần đo thực tế để kết luận. Các số bộ nhớ trên chỉ tính phần tử bảng, chưa gồm header object, buffer nén, output và chi phí JVM.

Decoder không dùng bảng hash; có thể nén bằng instance 12 rồi giải bằng instance 16 và ngược lại. `getName()` luôn trả `LZ4`, tham số phải được runner ghi riêng.

```java
LZ4Codec codec = new LZ4Codec(12);
byte[] compressed = codec.compress(input);
byte[] restored = codec.decompress(compressed);
LZ4Codec.BlockStatistics stats = codec.analyze(compressed);
```

`analyze()` parse và kiểm tra block mà không tạo output giải nén. Thống kê gồm:

- `getSequenceCount()`: mọi sequence, kể cả literal cuối; input rỗng có 1.
- `getMatchCount()`: số sequence chứa match.
- `getTotalMatchLength()`: tổng số byte do match tái tạo.
- `getAverageMatchLength()`: tổng độ dài match chia số match; trả 0 nếu không có match.

Ví dụ 1.024 byte `A`: encoder phát literal `A`, match offset 1 dài 1.018, rồi 5 literal cuối. Thống kê là 2 sequence, 1 match và average match length 1.018 byte.

## 8. Chạy thử runner và đọc CSV

Dùng file source trong repo để thử ngay, không cần tải corpus:

```bash
cd /home/phambuuthinh/Downloads/CSD201_GROUP3_PROJECT
mvn compile
mkdir -p results
java -Xmx1g -cp target/classes vn.csd.compression.metrics.LZ4Rq3Runner \
  results/lz4-demo.csv 1 2 \
  src/main/java/vn/csd/compression/codec/LZ4Codec.java \
  2> results/lz4-demo-environment.log
```

Ý nghĩa đối số:

| Đối số | Ý nghĩa |
|---|---|
| `results/lz4-demo.csv` | File CSV mới, runner từ chối ghi đè |
| `1` | Một warm-up cho mỗi cấu hình |
| `2` | Hai lượt đo cho mỗi cấu hình |
| Đường dẫn cuối | Input; có thể thêm nhiều file, cách nhau bằng dấu cách |

Với một input, hai hash log và hai lượt đo, CSV có 4 dòng dữ liệu và một dòng header. Warm-up không ghi vào CSV nhưng vẫn được kiểm tra SHA-256.

Các bước runner:

```text
Đọc file + SHA-256 input ngoài vùng đo
    → đo compress
    → đo decompress riêng
    → SHA-256 output và kiểm tra độ dài
    → analyze ngoài vùng đo
    → ghi CSV
```

| Cột | Cách hiểu |
|---|---|
| `hash_log` | 12 hoặc 16 |
| `compressed_bytes` | Toàn bộ container, gồm 16 byte header |
| `ratio` | Compressed bytes / input bytes, nhỏ hơn tốt hơn; file rỗng để trống |
| `compress_ns`, `decompress_ns` | Nanosecond đo riêng từng thao tác |
| `compress_mib_s`, `decompress_mib_s` | `(input_bytes / 1.048.576) / (ns / 1.000.000.000)` |
| `lz4_sequence_count` | Số sequence gồm sequence cuối |
| `lz4_match_count` | Số match |
| `lz4_avg_match_length` | Tổng độ dài match / số match |
| `output_check` | 16 ký tự hex đầu của SHA-256 output |
| `status` | `PASS` khi độ dài và toàn bộ SHA-256 trùng input |

Runner so đủ 32 byte SHA-256, chỉ rút ngắn phần hiển thị trong CSV. Khi lỗi sẽ dừng bằng exception; các dòng trước đó có thể đã được ghi, nên CSV có dòng PASS không đồng nghĩa toàn bộ thí nghiệm đã chạy xong.

File environment ghi Java, VM, OS, heap tối đa, JVM flags và số lượt. Runner hiện chưa đo peak heap, H0 và chưa chạy các codec khác.

## 9. Phối hợp TV6 chạy RQ3 đầy đủ

Phần sẵn sàng: LZ4 hai cấu hình, unit test, thống kê và runner riêng LZ4. Phần TV6/TV4 cần tích hợp:

1. R2 đủ 12 file Silesia và R3 đủ 11 file Canterbury, kèm nguồn, kích thước, SHA-256.
2. C1/C2/C3 mỗi loại 64 MiB, sinh từ generator nhóm với seed được ghi rõ.
3. Đưa LZ4-12/16 cùng LZW và baseline RLE/Huffman vào runner chung.
4. Chốt warm-up, lượt đo, schema CSV và phương pháp đo peak heap theo quy định chung.
5. Chạy tuần tự trên cùng máy/JVM; kiểm SHA-256 mọi lượt và lưu môi trường.

Một seed cho mỗi synthetic tạo 28 input × 5 cấu hình = 140 tổ hợp trước khi nhân số lượt đo. Phần LZ4 chiếm 56 tổ hợp.

`results/lz4-smoke.csv` hiện có chỉ là bằng chứng lệnh chạy hoạt động trên source Java. Không dùng nó thay corpus chuẩn hay kết luận hash log nào tốt hơn trong RQ3.

## 10. Lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| `mvn: command not found` | Dùng Maven tạm nếu còn hoặc cài/cấu hình Maven |
| Không tải được dependency | Kiểm tra mạng và Maven repository/proxy |
| `ClassNotFoundException` khi chạy runner | Chạy `mvn compile`, đứng đúng thư mục root, dùng đủ tên package |
| `FileAlreadyExistsException` | Đổi tên CSV đầu ra để giữ kết quả cũ |
| `NoSuchFileException` | Kiểm tra đường dẫn input và tạo thư mục output trước |
| `OutOfMemoryError` khi chạy test trong IDE | Dùng `mvn test` với `-Xmx1g` đã cấu hình hoặc chỉnh VM options IDE |
| `Invalid LZ4B: Bad ... magic` | Input giải nén phải là container do codec đóng gói, không phải file gốc hay raw block |
| Ratio lớn hơn 1 | Có thể hợp lệ do header và dữ liệu khó nén |

## 11. Nội dung trình bày khi demo

> Em dùng bảng hash để tìm vị trí trước đó có cùng chuỗi 4 byte. Khi tìm được match, em lưu khoảng cách và độ dài, còn byte chưa khớp được lưu nguyên dạng. Decoder đọc token, literal và match để khôi phục dữ liệu, có xử lý match chồng lấn. Hai cấu hình hash log thay đổi kích thước bảng tìm kiếm của encoder; decoder dùng cùng định dạng. Em đã kiểm round-trip, dữ liệu hỏng, giới hạn 64 MiB và đối chiếu payload với thư viện độc lập. RQ3 đầy đủ cần chạy thêm corpus chuẩn cùng các codec còn lại trên runner chung.

Kết quả đã kiểm chứng ngày 21/10/2026: **18/18 lượt test PASS**, build package thành công; OpenJDK 17.0.20.1, Maven 3.9.9. Các số này là kết quả phiên triển khai, không thay cho việc chạy lại trên máy nộp bài.

## Tài liệu liên quan

- Kế hoạch gốc: `/home/phambuuthinh/PLAN_14_NGAY_NEN_DU_LIEU_NHOM_6.md`.
- [Bàn giao TV5 → TV6](TV5_LZ4_RQ3.md).
- [LZ4 Block Format — đặc tả chính thức](https://github.com/lz4/lz4/blob/dev/doc/lz4_Block_format.md).
- [[TASK_BOARD]]: CSD-LZ4-TV5-001, CSD-RQ3-TV6-002 và CSD-LZ4-DOC-003.

## Cập nhật: tổ chức theo đối tượng, bỏ static

Theo yêu cầu giảng viên, `LZ4Codec` không còn khai báo `static`: các hàm đọc header, parse, đọc/ghi length và đọc byte đều là phương thức instance; các giá trị `magic`, `headerSize`, `maxInputSize` là trường `private final` của từng đối tượng. Constructor mặc định gọi `this(12)`. `Cursor` và `BlockStatistics` là lớp bên trong không static.

`final` vẫn được giữ để các giá trị không bị gán lại; `final` không có nghĩa là `static`.

Mã dùng giới hạn input chuyển từ `LZ4Codec.MAX_INPUT_SIZE` sang `codec.getMaxInputSize()`. API chính vẫn là `new LZ4Codec(12)` hoặc `new LZ4Codec(16)` rồi gọi `compress`, `decompress`, `analyze`.

Runner có constructor public và hàm instance:

```java
LZ4Rq3Runner runner = new LZ4Rq3Runner();
runner.run(new String[]{"results/demo.csv", "1", "2", "README.md"});
```

Chỉ giữ một khai báo static trong phần source/test TV5: `public static void main(String[] args)` của runner để Java 8/17 gọi từ dòng lệnh. Hàm này chỉ tạo đối tượng và gọi `run(args)`; không chứa thuật toán hay logic benchmark. Nếu chỉ tích hợp vào runner chung của TV4, gọi `run()` trực tiếp, không cần gọi `main()` của TV5.

Helper trong test cũng là instance và không dùng static import. Các API thư viện chuẩn như `Arrays.fill`, `Math.min` hoặc `Files.readAllBytes` vẫn được gọi theo API Java; thay đổi này loại bỏ khai báo static trong mã do TV5 định nghĩa, ngoại trừ điểm vào CLI.

Sau thay đổi, `mvn clean verify` PASS 18/18 test; container và hai hash log giữ cùng định dạng.
