# ĐỒ ÁN CSD201 — NÉN DỮ LIỆU KHÔNG MẤT MÁT (GROUP 3)

Dự án so sánh 4 thuật toán nén: **RLE, Huffman Canonical, LZW 12-bit và LZ4 Block**, dùng cấu trúc Maven và package chung `vn.csd.compression`. Mã nguồn hướng đến Java 8, có thể mở trực tiếp trong NetBeans 17 hoặc chạy bằng OpenJDK 8/17.

---

## 1. Cấu trúc thư mục dự án

Cấu trúc chuẩn của dự án được quy định thống nhất cho toàn bộ 6 thành viên:

```text
src/
├── main/java/vn/csd/compression/
│   ├── codec/
│   │   ├── Codec.java             # Interface chung bắt buộc cho 4 codec
│   │   ├── RLECodec.java          # Thuật toán RLE (TV1)
│   │   ├── HuffmanCodec.java      # Thuật toán Huffman Canonical (TV3)
│   │   ├── LZWCodec.java          # Thuật toán LZW 12-bit (TV2 - Phát)
│   │   └── LZ4Codec.java          # Thuật toán LZ4 Block Format (TV5 - Nhóm trưởng)
│   ├── io/
│   │   ├── BitReader.java         # Đọc luồng bit MSB-first (TV3)
│   │   └── BitWriter.java         # Ghi luồng bit MSB-first (TV3)
│   ├── dataset/
│   │   ├── DataGenerator.java     # Bộ sinh dữ liệu C1, C2, C3 (TV6)
│   │   └── ByteOrdering.java      # Biến đổi thứ tự byte cho RQ2 (TV4)
│   ├── metrics/
│   │   ├── Entropy.java           # Tính Entropy bậc 0 (H0) cho RQ2 (TV4)
│   │   ├── BenchmarkRunner.java   # Bộ điều phối đo hiệu năng chung (TV4)
│   │   ├── CsvWriter.java         # Ghi kết quả benchmark ra CSV chuẩn (TV4)
│   │   └── LZ4Rq3Runner.java      # Runner riêng cho thí nghiệm RQ3 LZ4 (TV5)
│   └── Main.java                  # Điểm khởi chạy chính của ứng dụng
└── test/java/vn/csd/compression/
    ├── RLECodecTest.java          # Test suite nghiệm thu RLE (TV1)
    ├── HuffmanCodecTest.java      # Test suite nghiệm thu Huffman (TV3)
    ├── LZWCodecTest.java          # Test suite nghiệm thu LZW (TV2 - Phát)
    ├── LZ4CodecTest.java          # Test suite nghiệm thu LZ4 (TV5 - Nhóm trưởng)
    ├── LZ4Rq3RunnerTest.java      # Test suite cho runner RQ3 LZ4 (TV5)
    └── RoundTripTest.java         # Kiểm thử tích hợp nén -> giải nén cả 4 codec
```

---

## 2. Hướng dẫn Build và Test

Yêu cầu môi trường: JDK 8 hoặc JDK 17, Apache Maven 3.6.3+.

### Biên dịch dự án bằng Maven:
```sh
mvn clean compile
```

### Chạy toàn bộ kiểm thử bằng Maven:
```sh
mvn test
```

---

## 3. Phần TV2 — LZW (Lempel-Ziv-Welch 12-bit)

Phụ trách: **TV2 (Phát)** — Chi nhánh: `lzw-Phat`

```java
import vn.csd.compression.codec.LZWCodec;

LZWCodec codec = new LZWCodec();
byte[] compressed = codec.compress(input);
byte[] restored = codec.decompress(compressed);
int fullAt = codec.getDictionaryFullAt(); // Vị trí byte input khi từ điển 4096 mục bị đầy
```

### Đặc tả kỹ thuật LZW:
- **Header:** Magic `LZW1` (4 byte), theo sau là 8 byte big-endian lưu độ dài gốc của dữ liệu.
- **Từ điển:** 4.096 mục ($2^{12}$). Khởi tạo 0–255 cho byte đơn, các chuỗi ghép mới bắt đầu từ 256.
- **Cơ chế đóng băng:** Khi từ điển đạt 4.096 mục thì **đóng băng (freeze)**, không thêm mục mới và tiếp tục nén đến hết file.
- **Tra cứu siêu tốc:** Encoder tra từ điển bằng mảng phẳng `int[4096 * 256]`, không dùng `HashMap`.
- **Đóng gói bit:** Các mã 12-bit được đóng gói theo thứ tự bit cao trước (**MSB-first**): cứ 2 mã 12-bit ghép thành 3 byte.
- **Xử lý ngoại lệ:** Decoder xử lý chuẩn xác ca biên kinh điển `KwKwK` (chuỗi dạng `ABABABA`).

### Chạy riêng bộ kiểm thử LZW nghiệm thu (T01 – T09):
- **Bằng Maven:**
  ```sh
  mvn test -Dtest=LZWCodecTest
  ```
- **Hoặc chạy trực tiếp bằng dòng lệnh Java (không cần Maven):**
  ```powershell
  # Trên Windows PowerShell:
  javac -encoding UTF-8 -d bin src/main/java/vn/csd/compression/codec/*.java src/test/java/vn/csd/compression/LZWCodecTest.java
  java -cp bin vn.csd.compression.LZWCodecTest
  ```

---

## 4. Phần TV5 — LZ4 (LZ4 Block Format)

Phụ trách: **TV5 (Nhóm trưởng)** — Chi nhánh: `feature/lz4-tv5`

```java
import vn.csd.compression.codec.LZ4Codec;

LZ4Codec codec = new LZ4Codec(12); // HASH_LOG: 12 hoặc 16; mặc định 12
byte[] compressed = codec.compress(input);
byte[] restored = codec.decompress(compressed);
LZ4Codec.BlockStatistics stats = codec.analyze(compressed);
```

### Đặc tả kỹ thuật LZ4:
- `compress(byte[])`, `decompress(byte[])` theo interface `Codec` chung.
- Giới hạn input tối đa: 64 MiB (`codec.getMaxInputSize()`). Dữ liệu hỏng ném `IllegalArgumentException`, null ném `NullPointerException`.
- **Container:** Magic `LZ4B` (4 byte), độ dài gốc 8 byte big-endian, độ dài block 4 byte big-endian, sau đó là payload LZ4 block.
- **Hash Table:** Hỗ trợ `HASH_LOG = 12` (4.096 ô) và `HASH_LOG = 16` (65.536 ô).
- Thư viện `lz4-java` chỉ dùng làm Oracle kiểm tra đối chiếu trong test, không dùng thay thế thuật toán tự viết.

### Chạy riêng kiểm thử và runner LZ4:
```sh
# Chạy unit test LZ4
mvn -Dtest=LZ4CodecTest,LZ4Rq3RunnerTest test

# Chạy runner RQ3 LZ4
mkdir -p results
java -Xmx1g -cp target/classes vn.csd.compression.metrics.LZ4Rq3Runner \
  results/rq3-lz4.csv 3 5 data/raw/dickens data/raw/alice29.txt \
  2> results/rq3-lz4-environment.log
```

- [Hướng dẫn test và giải thích code TV5](docs/TV5_LZ4_HUONG_DAN_TEST.md)
- [Bàn giao TV6 và tích hợp RQ3](docs/TV5_LZ4_RQ3.md)
- [Kết quả smoke test](results/README.md)
