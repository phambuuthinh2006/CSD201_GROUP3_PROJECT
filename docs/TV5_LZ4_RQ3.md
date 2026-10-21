# Bàn giao TV5 → TV6: LZ4 và RQ3

## Mã nguồn và định dạng

- `src/main/java/vn/csd/compression/codec/LZ4Codec.java`: encoder greedy một candidate gần nhất trên mỗi ô hash, decoder có kiểm tra biên, API theo `Codec`.
- Hash: đọc 4 byte little-endian, nhân `0x9e3779b1` với overflow int, lấy HASH_LOG bit cao. Bảng `int[1 << hashLog]` khởi tạo -1; bộ nhớ bảng 16 KiB hoặc 256 KiB (chưa kể object header).
- Match tối thiểu 4 byte, offset 1–65535, offset hai byte little-endian. Token nibble cao literal length, nibble thấp match length trừ 4. Nibble 15 luôn có extension, kể cả extension cuối bằng 0.
- Encoder giữ 5 literal cuối, match cuối bắt đầu cách cuối dữ liệu gốc ít nhất 12 byte. Input <13 byte chỉ literal. Empty block chứa token 0.
- Container: byte 0–3 `LZ4B`; byte 4–11 original length unsigned trong phạm vi 0–64 MiB, big-endian; byte 12–15 compressed block length big-endian; từ byte 16 là raw LZ4 block. Độ dài block không gồm header 16 byte. Không có hash log trong header vì decoder không cần tham số này.
- **Độ dài block BE là quy ước triển khai**, chưa phải kết luận về yêu cầu giảng viên. TV6 cần xác nhận trước khi khóa format.
- Decoder kiểm tra header, độ dài, extension, offset, output bounds và điều kiện cuối block trước khi cấp phát output. Sau đó parse lần hai để copy; chi phí cả hai lượt được tính vào thời gian giải nén.
- Instance chỉ giữ hash log bất biến; bảng hash và bộ đệm đều cục bộ mỗi lời gọi. Không giữ input hoặc thống kê của lần chạy trước.

Đối chiếu payload với [đặc tả chính thức LZ4 Block Format](https://github.com/lz4/lz4/blob/dev/doc/lz4_Block_format.md). Container `LZ4B` do đồ án quy định, không phải LZ4 Frame.

## Thống kê dùng trong RQ3

```java
LZ4Codec codec = new LZ4Codec(16);
byte[] compressed = codec.compress(input);
// Gọi ngoài khoảng đo tốc độ:
LZ4Codec.BlockStatistics s = codec.analyze(compressed);
int sequences = s.getSequenceCount();
int matches = s.getMatchCount();
long matchedBytes = s.getTotalMatchLength();
double average = s.getAverageMatchLength();
```

Quy ước: `lz4_sequence_count` gồm sequence literal cuối; input rỗng có 1 sequence. `lz4_avg_match_length = tổng độ dài match / số match`, bằng 0 nếu không có match. Không chia cho số sequence vì sequence cuối không chứa match. TV4/TV6 cần giữ cùng định nghĩa khi tổng hợp.

## Công cụ chạy với TV6

`LZ4Rq3Runner` nhận `output.csv warmups repeats input-file...`. Chạy cả hai hash log theo thứ tự, I/O/hash/thống kê ngoài thời gian đo, tốc độ tính bằng số byte gốc, ratio gồm cả 16 byte header. Input rỗng để trống ratio. So đủ 32 byte SHA-256; cột `output_check` lấy 16 ký tự hex đầu. Sai dữ liệu sẽ dừng runner bằng exception; CSV còn lại chỉ có các dòng đo đã PASS, không được hiểu là ma trận hoàn chỉnh. Ghi stderr kèm CSV để lưu Java, hệ điều hành, heap, JVM flags và số lượt chạy; TV6 bổ sung CPU, máy và điều kiện tải hệ thống.

CSV này là **phần LZ4 tạm thời**, chưa phải schema chung của TV4: không có số liệu peak heap, H0, LZW/RLE/Huffman hoặc manifest dataset. Không dùng chênh lệch heap tức thời thay cho peak. Khi có runner chung, tích hợp hai instance codec và `analyze()` vào đó, giữ chính sách đo peak của nhóm, không ghép các phép đo khác phương pháp thành một kết luận.

TV6 cần cung cấp/kiểm tra:

1. R2: đủ 12 file Silesia; R3: đủ 11 file Canterbury, giữ nguyên kích thước, manifest nguồn và SHA-256.
2. C1/C2/C3: mỗi file 64 MiB, sinh từ generator chung với seed được ghi; không gọi dữ liệu smoke là C1/C2/C3 chuẩn.
3. Chạy LZ4-12 và LZ4-16 trên 28 input: 56 cấu hình trước khi nhân số lượt đo. Thêm LZW + RLE/Huffman baseline để đủ 140 cấu hình nếu mỗi synthetic dùng một seed.
4. Chốt schema, warm-up, lượt đo và cách tổng hợp theo văn bản chung; ghi cùng máy/JVM và chạy tuần tự.
5. Review chéo unit test và quyết định BE của block length; so tốc độ, ratio, peak heap, sequence count và average match length giữa hai hash log. Không giả định hash log 16 luôn nén tốt hơn.

Chưa có corpus, generator TV6, ba codec còn lại hoặc runner TV4 trong repository tại lúc bàn giao. Vì vậy chưa có bảng RQ3 đầy đủ và chưa có kết luận hash log nào tốt hơn. Bàn giao qua tài liệu này và mục CSD-RQ3-TV6-002 trên bảng công việc; chưa gửi trực tiếp cho một người TV6 vì chưa xác định danh tính/tài khoản của thành viên đó.

## Kiểm chứng thực tế

Ngày 21/10/2026: OpenJDK 17.0.20.1, Maven 3.9.9, biên dịch với `--release 8`, Surefire `-Xmx1g`.

- 18 test invocations PASS: 16 codec và 2 runner, không failure/error/skip.
- Hai hash log: empty, 1 byte, mọi độ dài <13, toàn bộ 256 byte, boundary extension 15/270, match length 19/274 và lân cận, overlap offset=1, dữ liệu có chu kỳ và random theo seed.
- Container tạo thủ công kiểm tra offset little-endian 8 và 65535; dữ liệu 64 MiB random và toàn byte giống nhau cho cả hai hash log; từ chối input vượt 64 MiB.
- Header hỏng, block bị cắt, original length sai, offset 0/vượt đầu output, extension quá lớn/truncated, tail không hợp lệ đều bị từ chối.
- Oracle `lz4-java 1.8.0`: giải raw payload encoder tự viết; decoder tự viết nhận payload fast/high-compression của oracle. Oracle chỉ trong test.
- Test runner xác minh cả hai hash log, số dòng không tính warm-up, SHA-256 file rỗng chuẩn, ratio rỗng và bảo vệ CSV đã tồn tại.

Lệnh trong phiên này: `/tmp/apache-maven-3.9.9/bin/mvn -q test` vì máy chưa có Maven trên PATH. Maven tải ngoài repo; người nhận cài Maven rồi chạy `mvn test` như README. Java 8 bytecode đã biên dịch, chưa chạy bằng JVM 8 thực tế.

## Tích hợp lên khung GitHub chung

Phần TV5 trên branch `feature/lz4-tv5` được đặt trên nền `origin/main` commit `e2fda35`, sau khi remote thay lịch sử ban đầu. Giữ nguyên interface `Codec.java` và `.gitignore` chung. `pom.xml` giữ source/target Java 8 và JUnit 4, bổ sung JUnit 5 + Vintage cùng oracle LZ4 test-only và Surefire. README chung có lệnh Maven vì test JUnit cần dependency trong classpath.

Kiểm chứng sau tích hợp: `mvn clean verify` PASS, 18/18 lượt test TV5. Ngoài ra đã lấy source LZW/test dạng `main()` từ `origin/lzw-Phat` commit `867210b`, biên dịch tạm với `javac --release 8` dựa trên interface chung và chạy toàn bộ bài test LZW PASS. Đây là kiểm tra tương thích ngoài thư mục repo; mã LZW vẫn thuộc branch TV2, không được đưa vào commit TV5. Test LZW dạng `main()` cần chạy riêng, Maven không tự thực thi thân `main()`.
