# BÁO CÁO REVIEW TOÀN DIỆN MÃ NGUỒN VÀ TIẾN ĐỘ ĐỒ ÁN CSD201
**Đề tài:** Nén Dữ Liệu Không Mất Mát (Lossless Compression) — Group 3
**Thời điểm đánh giá:** 26/10/2026
**Commit kiểm tra:** `3554bb8` (nhánh `main`)

---

## I. TỔNG QUAN TÌNH TRẠNG DỰ ÁN

| Phân hệ / Module | Phụ trách | Tình trạng | Đánh giá |
| :--- | :--- | :---: | :--- |
| **1. LZ4 Block Codec** | TV5 (Phạm Bửu Thịnh) | **ĐẠT CHUẨN** ✅ | Hoàn chỉnh 100%, đúng đặc tả, 18/18 unit test JUnit 5 pass. |
| **2. LZW 12-bit Codec** | TV2 (Phát Bùi) | **ĐẠT CHUẨN (CẦN TINH CHỈNH TEST)** ⚠️ | Thuật toán chuẩn, tra mảng phẳng, xử lý KwKwK chuẩn. Cần chuyển test sang JUnit. |
| **3. Canonical Huffman & Bit I/O** | TV3 (Q. Vương) | **ĐẠT CHUẨN (CẦN TINH CHỈNH TEST)** ⚠️ | Thuật toán chuẩn, BitReader/Writer chuẩn MSB-first. Cần chuyển test sang JUnit. |
| **4. Metrics, Entropy & Benchmark** | TV4 (Võ Thảo Nguyên) | **CẦN SỬA ĐỔI & BỔ SUNG** ⚠️ | Đã có khung H0, Fisher-Yates, Counting Sort. Runner thiếu warm-up, thiếu cột đặc thù LZW/LZ4. |
| **5. RLE Codec** | TV1 | **CHƯA HOÀN THÀNH** ❌ | Mới có file khung ném `UnsupportedOperationException`. |
| **6. Dataset Generator & Corpus** | TV6 | **CHƯA HOÀN THÀNH** ❌ | `DataGenerator.java` còn rỗng; thiếu tập dữ liệu thực nghiệm (Silesia, Canterbury, C1-C3). |
| **7. Tích hợp hệ thống & CLI** | Nhóm chung | **CẦN BỔ SUNG** ⚠️ | `Main.java` và `RoundTripTest.java` còn là file mẫu. |

---

## II. CHI TIẾT CÁC PHẦN ĐÃ ĐẠT CHUẨN (GIỮ NGUYÊN)

### 1. `LZ4Codec.java` & `LZ4Rq3Runner.java` (TV5) — **Ổn định, không cần sửa**
* **Ưu điểm:**
  * Cài đặt đúng chuẩn LZ4 block format với container `LZ4B` (16-byte header).
  * Hỗ trợ hai chế độ `HASH_LOG = 12` (4.096 ô) và `16` (65.536 ô).
  * Phân tách rõ ràng giữa lượt nén đo tốc độ và hàm thống kê `analyze()` (`lz4_sequence_count`, `lz4_avg_match_length`).
  * Decoder có 2 pass: quét kiểm tra biên an toàn trước khi cấp phát bộ nhớ, chống buffer overflow.
  * Đầy đủ 18 test JUnit 5 tích hợp mượt mà với Maven Surefire (`mvn test`).

### 2. `LZWCodec.java` (TV2 - Phát) — **Thuật toán cốt lõi đã đạt chuẩn**
* **Ưu điểm:**
  * Tra cứu từ điển bằng mảng phẳng `int[4096 * 256]`, đúng yêu cầu không dùng `HashMap`.
  * Cơ chế đóng băng (freeze) chuẩn khi từ điển đạt 4.096 mục.
  * Header 12 byte: `LZW1` (4 byte) + 8 byte độ dài gốc big-endian.
  * Xử lý chính xác ca biên kinh điển `KwKwK`.
  * Có getter `getDictionaryFullAt()` để phục vụ ghi dữ liệu vào CSV.

### 3. `HuffmanCodec.java`, `BitReader.java`, `BitWriter.java` (TV3) — **Thuật toán cốt lõi đã đạt chuẩn**
* **Ưu điểm:**
  * Canonical Huffman chuẩn: sinh độ dài mã, sắp xếp theo `(length, symbol)` và gán mã không tiền tố.
  * Header 268 byte (`HUF1` 4 byte + 8 byte độ dài gốc + 256 byte bảng code lengths).
  * Đóng gói bit MSB-first qua `BitWriter`/`BitReader` sử dụng bộ đệm `long`.
  * Xử lý đúng các ca biên: input rỗng, input chỉ có 1 ký hiệu (gán mã 1 bit).
  * Hàm `calculateExpectedPayloadBits()` tính toán độc lập $\sum (freq \times codeLength)$ để đối chiếu lý thuyết.

### 4. `Entropy.java` & `ByteOrdering.java` (TV4) — **Đã đạt chuẩn**
* `Entropy.calculateH0()`: Tính entropy bậc 0 chính xác theo công thức $H_0 = -\sum p \log_2 p$.
* `ByteOrdering`:
  * `grouped()` dùng Counting Sort để giữ nguyên histogram tần suất byte.
  * `shuffled()` dùng Fisher-Yates shuffle với `seed` cố định, đảm bảo tính tái lập (reproducibility).

---

## III. DANH SÁCH CHI TIẾT CÁC PHẦN CẦN SỬA VÀ BỔ SUNG

### 1. Phân hệ Benchmark & Metrics (TV4) — ⚠️ **CẦN SỬA CODE**

#### 🔴 Vấn đề 1: `BenchmarkRunner.java` bỏ trống các cột đặc thù của LZW và LZ4
* **Hiện trạng (dòng 188 & 198-200):**
  Trong `BenchmarkResult.toCsvRow()`, các trường `params`, `lzw_dictionary_full_at`, `lz4_sequence_count`, `lz4_avg_match_length` đang bị hardcode thành chuỗi rỗng `""`.
* **Cần sửa:**
  1. Khi `codec` là instance của `LZWCodec`, cần lấy `((LZWCodec) codec).getDictionaryFullAt()` điền vào cột `lzw_dictionary_full_at`.
  2. Khi `codec` là instance của `LZ4Codec`:
     - Cột `params`: ghi rõ `hashLog=12` hoặc `hashLog=16`.
     - Sau khi nén xong (ngoài khoảng đo thời gian), gọi `LZ4Codec.BlockStatistics stats = ((LZ4Codec) codec).analyze(compressed);` để lấy `stats.getSequenceCount()` và `stats.getAverageMatchLength()` điền vào 2 cột tương ứng.

#### 🔴 Vấn đề 2: Phương pháp đo bộ nhớ Heap (`BenchmarkRunner.java`)
* **Hiện trạng (dòng 21-31, 33-43):**
  Đang tính:
  ```java
  long compHeapAux = Math.max(0L, heapAfterComp - heapBeforeComp);
  ```
* **Rủi ro:**
  `Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()` là bộ nhớ tức thời tại 2 thời điểm. Khi nén dữ liệu lớn, Garbage Collector có thể chạy ngầm giữa chừng khiến `heapAfter` nhỏ hơn hoặc bằng `heapBefore`, dẫn đến `compHeapAux = 0` (không phản ánh peak bộ nhớ phụ trợ auxiliary memory).
* **Cần sửa:**
  Tách riêng việc đo heap hoặc sử dụng `ThreadMXBean` / ước lượng dung lượng cấp phát cố định của cấu trúc dữ liệu (ví dụ: bảng hash LZ4 16 KiB/256 KiB; từ điển LZW $4096 \times 256 \times 4$ bytes = 4 MiB).

#### 🔴 Vấn đề 3: Thiếu Warm-up và Lặp lại nhiều lượt đo (Repeats)
* **Hiện trạng:**
  `BenchmarkRunner.run()` chỉ thực thi đúng 1 lần cho mỗi phép đo. Lần chạy đầu tiên JVM chưa JIT compile sẽ cho thời gian rất chậm và không ổn định.
* **Cần sửa:**
  Bổ sung vòng lặp warm-up (ví dụ: 3 lần) và đo lấy trung bình/trung vị của $N$ lần lặp (ví dụ: 5 lần) tương tự như thiết kế trong `LZ4Rq3Runner`.

---

### 2. Chuẩn hóa Unit Test cho Maven Surefire (TV2 & TV3) — ⚠️ **CẦN SỬA**

#### 🔴 Vấn đề: `LZWCodecTest.java` và `HuffmanCodecTest.java` chưa chạy tự động khi `mvn test`
* **Hiện trạng:**
  Hai class kiểm thử này đang dùng `public static void main(String[] args)` và in kết quả ra màn hình. Khi gõ lệnh `mvn test`, Maven Surefire chỉ quét các class có annotation `@Test` (JUnit 4/5), dẫn đến chỉ có 18 test của LZ4 được chạy, còn LZW và Huffman bị bỏ qua.
* **Cần sửa:**
  Chuyển các phương thức kiểm tra trong `LZWCodecTest` và `HuffmanCodecTest` thành các phương thức có gắn `@Test` (JUnit 5 `org.junit.jupiter.api.Test` hoặc JUnit 4 `org.junit.Test`), ví dụ:
  ```java
  @Test
  public void testT01HeaderAndEmptyInput() { ... }
  ```

---

### 3. Phân hệ RLE Codec (TV1) — ❌ **CHƯA CÀI ĐẶT**

* **Hiện trạng:**
  `src/main/java/vn/csd/compression/codec/RLECodec.java` chỉ ném `UnsupportedOperationException`.
* **Cần làm:**
  1. Cài đặt thuật toán RLE (Run-Length Encoding) hỗ trợ dữ liệu nhị phân tùy ý (byte array 0–255).
  2. Quy ước Header cho RLE (ví dụ magic `RLE1`, độ dài gốc).
  3. Xử lý trường hợp chuỗi không lặp và chuỗi lặp dài > 255.
  4. Viết các ca kiểm thử trong `RLECodecTest.java`.

---

### 4. Phân hệ Dataset & Thực nghiệm (TV6) — ❌ **CHƯA CÀI ĐẶT**

* **Hiện trạng:**
  `DataGenerator.java` còn rỗng; thư mục `data/` chưa có dữ liệu.
* **Cần làm:**
  1. **Cài đặt `DataGenerator.java`** sinh 3 tập dữ liệu tổng hợp (kích thước chuẩn 64 MiB):
     - **C1 (Đồng nhất):** Dữ liệu một ký tự duy nhất lặp lại.
     - **C2 (Đều/Ngẫu nhiên hoàn toàn):** Dữ liệu phân phối đều byte 0–255 (Entropy $H_0 \approx 8.0$).
     - **C3 (Phân phối Zipf/Văn bản thực tế):** Phân phối lũy thừa mô phỏng văn bản tự nhiên.
  2. **Chuẩn bị Corpus thực nghiệm:**
     - **R2 (Silesia corpus):** 12 file chuẩn.
     - **R3 (Canterbury corpus):** 11 file chuẩn.

---

### 5. Tích hợp Hệ thống (Integration) — ⚠️ **CẦN BỔ SUNG**

#### 🔴 `RoundTripTest.java`
* Cần viết kiểm thử tự động nén $\to$ giải nén $\to$ so sánh SHA-256 cho **cả 4 codec** trên cùng một tập test vector:
  - Input rỗng (0 byte).
  - Input 1 byte.
  - Input lặp lại đồng nhất.
  - Input ngẫu nhiên 256 byte.
  - Input văn bản thực tế.

#### 🔴 `Main.java`
* Cần xây dựng CLI hoàn chỉnh hỗ trợ:
  - Lệnh nén/giải nén file: `java -jar compression.jar compress <codec> <input-file> <output-file>`
  - Lệnh chạy toàn bộ benchmark: `java -jar compression.jar benchmark [output.csv]`

---

## IV. BẢNG TỔNG KẾT HÀNH ĐỘNG (ACTION ITEMS)

| Ưu tiên | Nhiệm vụ | Người thực hiện | Tác động |
| :---: | :--- | :---: | :--- |
| **P0** | Cài đặt `RLECodec.java` và bộ test | **TV1** | Mở khóa việc tích hợp `RoundTripTest` và nghiệm thu RQ1. |
| **P0** | Sửa `BenchmarkRunner.java` lấy đúng `lzw_dictionary_full_at`, `lz4_sequence_count`, `lz4_avg_match_length` | **TV4** | Đảm bảo file CSV không bị khuyết dữ liệu quan trọng của LZW/LZ4. |
| **P1** | Cài đặt `DataGenerator.java` (C1, C2, C3) và chuẩn bị corpus | **TV6** | Mở khóa việc chạy benchmark RQ1, RQ2, RQ3. |
| **P1** | Chuyển test `LZWCodecTest`, `HuffmanCodecTest` sang JUnit (`@Test`) | **TV2, TV3** | Cho phép `mvn test` tự động kiểm thử toàn bộ dự án trên CI/CD hoặc máy chấm điểm. |
| **P2** | Viết `RoundTripTest.java` và giao diện CLI trong `Main.java` | **Cả nhóm** | Hoàn thiện sản phẩm để demo và chấm điểm. |
