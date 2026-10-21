# ĐỒ ÁN CSD201 — NÉN DỮ LIỆU KHÔNG MẤT MÁT (GROUP 3)

## 1. Giới thiệu
Dự án so sánh 4 thuật toán nén: **RLE, Huffman Canonical, LZW 12-bit và LZ4 Block**.
Dự án được xây dựng tương thích JDK 1.8 và NetBeans 17 theo quy định của môn học CSD201 (FA26).

## 2. Cấu trúc thư mục
```text
compression-project/
├── pom.xml
├── .gitignore
├── README.md
├── src/
│   ├── main/java/vn/csd/compression/
│   │   └── codec/
│   │       ├── Codec.java          # Interface chung cho 4 codec
│   │       ├── LZWCodec.java       # LZW 12-bit codec (TV2)
│   │       ├── RLECodec.java       # RLE codec (TV1)
│   │       ├── HuffmanCodec.java   # Huffman canonical (TV3)
│   │       └── LZ4Codec.java       # LZ4 block codec (TV5)
│   └── test/java/vn/csd/compression/
│       └── LZWCodecTest.java       # Test suite nghiệm thu LZW (T01 - T09)
```

## 3. Cách biên dịch và chạy kiểm thử LZW bằng dòng lệnh
Mở Terminal / PowerShell tại thư mục `compression-project`:

```bash
# 1. Biên dịch mã nguồn (UTF-8)
javac -encoding UTF-8 -d bin src/main/java/vn/csd/compression/codec/*.java src/test/java/vn/csd/compression/*.java

# 2. Chạy bộ kiểm thử nghiệm thu LZW
java -cp bin vn.csd.compression.LZWCodecTest
```

Hoặc mở bằng NetBeans 17 và chọn **Clean and Build**.
