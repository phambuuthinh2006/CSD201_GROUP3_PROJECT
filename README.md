# ĐỒ ÁN CSD201 — NÉN DỮ LIỆU KHÔNG MẤT MÁT (GROUP 3)

Dự án so sánh RLE, Huffman Canonical, LZW 12-bit và LZ4 Block, dùng cấu trúc Maven và package chung `vn.csd.compression`. Mã nguồn hướng đến Java 8, có thể mở trong NetBeans 17; phần TV5 đã kiểm chứng trên OpenJDK 17.

## Cấu trúc chung

```text
pom.xml
src/
├── main/java/vn/csd/compression/
│   ├── codec/       # Codec và RLECodec/HuffmanCodec/LZWCodec/LZ4Codec
│   ├── io/          # BitReader, BitWriter (TV3)
│   ├── dataset/     # Generator, biến đổi thứ tự byte (TV4/TV6)
│   └── metrics/     # Benchmark, entropy, CSV (TV4); runner LZ4 (TV5)
└── test/java/vn/csd/compression/
    └── ...          # Unit test từng codec và kiểm thử tích hợp
```

Đây là cấu trúc dự kiến cho toàn nhóm. Trên branch TV5 hiện có interface chung, LZ4 và test/runner LZ4; các thành viên còn lại bàn giao qua branch riêng.

## Build và test

Cần JDK và Maven 3.6.3+. Môi trường TV5 đã dùng JDK 17, Maven 3.9.9.

```sh
mvn test
mvn package
```

Giữ dependency JUnit 4 của project chung; bổ sung JUnit 5 cho TV5 và Vintage để Maven chạy được các test JUnit 4 khi tích hợp. Test dạng `main()` của thành viên khác cần chạy riêng sau khi mã đó được ghép vào:

```sh
mvn test-compile
java -cp target/classes:target/test-classes vn.csd.compression.LZWCodecTest
```

Lệnh cuối dành cho bản test LZW có `main()` trên branch `lzw-Phat`; trên Windows dùng `;` thay `:` trong classpath. Các test JUnit cần dependency nên dùng Maven thay vì biên dịch toàn bộ test bằng `javac` không có classpath.

## Phần TV5 — LZ4

```java
import vn.csd.compression.codec.LZ4Codec;

LZ4Codec codec = new LZ4Codec(12); // hoặc 16; mặc định 12
byte[] compressed = codec.compress(input);
byte[] restored = codec.decompress(compressed);
LZ4Codec.BlockStatistics stats = codec.analyze(compressed);
```

- `compress(byte[])`, `decompress(byte[])` theo interface `Codec` chung.
- Codec và helper dùng phương thức instance, không khai báo `static`. Runner có `run(args)` instance; chỉ điểm vào CLI `main` giữ `static` theo yêu cầu Java 8/17.
- Lấy giới hạn input qua `codec.getMaxInputSize()` (64 MiB).
- Container gồm magic `LZ4B`, độ dài gốc 8 byte BE, độ dài block 4 byte BE, rồi payload LZ4. BE của block length là quy ước cần TV6 xác nhận với đề gốc.
- Giới hạn input 64 MiB. Dữ liệu hỏng gây `IllegalArgumentException`, null gây `NullPointerException`.
- `getName()` trả `LZ4`; runner ghi hash log riêng. Decoder không phụ thuộc hash log.
- `lz4-java` chỉ dùng làm oracle trong test, không dùng thay thuật toán.

Chạy riêng test TV5:

```sh
mvn -Dtest=LZ4CodecTest,LZ4Rq3RunnerTest test
```

Chạy phần LZ4 của RQ3 trên input có sẵn:

```sh
mkdir -p results
java -Xmx1g -cp target/classes vn.csd.compression.metrics.LZ4Rq3Runner \
  results/rq3-lz4.csv 3 5 data/raw/dickens data/raw/alice29.txt \
  2> results/rq3-lz4-environment.log
```

Các file corpus trong ví dụ cần TV6 cung cấp. `3` warm-up và `5` lượt đo là ví dụ tạm đề xuất, cần đối chiếu quy định chung. Runner chạy tuần tự HASH_LOG=12/16, kiểm SHA-256 mọi lượt và không ghi đè CSV cũ. Input, SHA-256 và thống kê xử lý ngoài khoảng đo codec.

- [Hướng dẫn test và giải thích code TV5](docs/TV5_LZ4_HUONG_DAN_TEST.md).
- [Bàn giao TV6 và tích hợp RQ3](docs/TV5_LZ4_RQ3.md).
- [Kết quả smoke test](results/README.md), không thay thế ma trận RQ3 đầy đủ.
