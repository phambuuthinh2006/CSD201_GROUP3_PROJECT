# BÁO CÁO CÁ NHÂN: BỘ SINH DỮ LIỆU THỰC NGHIỆM C1, C2, C3
**Môn học:** CSD201 — Cấu trúc dữ liệu và Giải thuật  
**Đề tài 4:** Nén dữ liệu không mất mát (Lossless Data Compression): RLE, Huffman, LZW và LZ4  
**Sinh viên thực hiện:** Thái Quang Huy  
**Mã thành viên:** TV6 (Cặp 3 — Nhóm 3)  
**Phạm vi phụ trách báo cáo này:** Cài đặt, tối ưu hóa và kiểm chứng bộ sinh dữ liệu tổng hợp C1, C2, C3 (`DataGenerator.java` & `DataGeneratorTest.java`)

---

## 1. MỤC TIÊU VÀ Ý NGHĨA KỸ THUẬT

Để đánh giá khách quan và toàn diện hiệu năng của 4 thuật toán nén (RLE, Canonical Huffman, LZW 12-bit và LZ4 Block), nhóm không thể chỉ dùng các file ngẫu nhiên mà bắt buộc phải có các tập dữ liệu tổng hợp có đặc tính thống kê chuẩn xác theo quy định của đề bài:

1. **Tập C1 (Run-Length Dominated):** Mô phỏng dữ liệu chứa các chuỗi byte lặp dài liên tiếp (thường gặp trong ảnh bitmap thô, file nhị phân đệm). Đây là tập dữ liệu đo đòn bẩy tối đa của thuật toán **RLE** và khả năng match lặp của **LZ4**.
2. **Tập C2 (Natural Language / Zipf Distribution):** Mô phỏng văn bản ngôn ngữ tự nhiên với quy luật tần suất từ ngữ thực tế. Đây là tập dữ liệu then chốt để đánh giá khả năng xây dựng cây mã tối ưu của **Huffman** và cơ chế xây dựng từ điển chuỗi con của **LZW / LZ4**.
3. **Tập C3 (Uniform Random / Maximum Entropy):** Dữ liệu giả ngẫu nhiên có phân phối đều hoàn toàn trên 256 giá trị byte ($0 \dots 255$). Dữ liệu này có độ hỗn loạn (Entropy $H_0 \approx 8$ bit/byte) cực đại, dùng để kiểm tra giới hạn nén và đo độ trễ (overhead) của cấu trúc header/container ở từng codec.

---

## 2. PHƯƠNG PHÁP CÀI ĐẶT THUẬT TOÁN (TRONG `DataGenerator.java`)

### 2.1. Cài đặt tập C1 (Phân phối hình học — Geometric Distribution)
* **Đặc tả:** Chuỗi byte lặp (run); độ dài mỗi đoạn lặp $k$ ($k \ge 1$) tuân theo phân phối hình học với kỳ vọng độ dài trung bình $\mu = 32$ bytes; giá trị byte của mỗi đoạn lặp phân phối đều ngẫu nhiên trong khoảng $[0, 255]$.
* **Công thức toán học áp dụng:**  
  Xác suất dừng một run là $p = \frac{1}{\mu} = \frac{1}{32} = 0.03125$.  
  Sử dụng phương pháp biến đổi nghịch đảo (Inverse Transform Sampling), với biến ngẫu nhiên đều $U \in (0, 1)$:
  $$k = \left\lfloor \frac{\ln(1 - U)}{\ln(1 - p)} \right\rfloor + 1$$
* **Đoạn mã cốt lõi:**
  ```java
  final double p = 1.0 / 32.0;
  final double log1MinusP = Math.log(1.0 - p);
  double u = rnd.nextDouble();
  if (u == 0.0) u = 0.0000001;
  int runLength = (int) (Math.log(1.0 - u) / log1MinusP) + 1;
  byte runValue = (byte) rnd.nextInt(256);
  ```

### 2.2. Cài đặt tập C2 (Kho từ vựng 1.000 từ & Phân phối Zipf $s = 1$)
* **Đặc tả:** Tạo một kho từ vựng cố định gồm đúng **1.000 từ**, mỗi từ có độ dài từ 3 đến 10 ký tự chữ cái thường (`a`–`z`). Dữ liệu văn bản được sinh ra bằng cách lấy mẫu các từ theo định luật Zipf với số mũ $s = 1$, các từ phân tách nhau bằng đúng một ký tự dấu cách (`' '`).
* **Công thức toán học áp dụng:**  
  Với từ có hạng (rank) $k \in [1, 1000]$, xác suất xuất hiện của từ là:
  $$P(k) = \frac{\frac{1}{k^s}}{\sum_{i=1}^{1000} \frac{1}{i^s}} = \frac{\frac{1}{k}}{H_{1000}} \quad (s = 1)$$
  Trong đó $H_{1000} = \sum_{i=1}^{1000} \frac{1}{i} \approx 7.48547$ là số Harmonic bậc 1000.
* **Tối ưu hiệu năng:** 
  * Xây dựng trước bảng hàm phân phối tích lũy **CDF (Cumulative Distribution Function)** có kích thước 1.000 phần tử.
  * Khi sinh dữ liệu, sử dụng thuật toán **Tìm kiếm nhị phân (Binary Search)** trên mảng CDF với độ phức tạp $O(\log_2 1000) \approx 10$ phép so sánh cho mỗi từ, đảm bảo sinh hàng chục Megabyte dữ liệu cực nhanh.

### 2.3. Cài đặt tập C3 (Byte ngẫu nhiên đều — Uniform Random)
* **Đặc tả:** Sinh chuỗi byte có phân phối xác suất đồng đều trên toàn bộ không gian giá trị $0 \dots 255$, tức xác suất xuất hiện của mỗi byte xấp xỉ $\frac{1}{256}$.
* **Cài đặt:** Khởi tạo `Random(seed)` và gọi `rnd.nextBytes(data)` để lấp đầy mảng byte với tốc độ xử lý nhanh nhất.

### 2.4. Kỹ thuật chống tràn bộ nhớ Heap (Out-Of-Memory Protection)
* Đề bài yêu cầu đo đạc dữ liệu lên tới mức **64 MiB**. Nếu cấp phát nhiều mảng `byte[67108864]` liên tục trên JVM sẽ rất dễ gây nghẽn Garbage Collector hoặc lỗi `java.lang.OutOfMemoryError: Java heap space`.
* Em đã thiết kế hàm `generateToFile(type, sizeBytes, seed, destFile)` sử dụng cơ chế **BufferedOutputStream (bộ đệm 64 KiB)** để ghi trực tiếp dòng byte ra ổ cứng dạng Stream, giúp sinh file 64 MiB mượt mà với lượng RAM tiêu thụ chỉ dưới vài Megabytes.

---

## 3. BẢNG DẪN CHỨNG VÀ KẾT QUẢ KIỂM THỬ THỰC TẾ

Toàn bộ các hàm sinh dữ liệu đã được kiểm thử độc lập qua file `DataGeneratorTest.java` với 5 nhóm kiểm thử nghiệm thu:

| Nhóm kiểm thử | Mục đích kiểm tra | Kết quả đo thực tế trên máy | Trạng thái |
|---|---|---|---|
| **1. Trường hợp biên** | Kiểm tra `size = 0` và `size = 1` | `size = 0` trả về mảng rỗng; `size = 1` trả về đúng 1 byte, không văng lỗi ngoại lệ. | **[ĐẠT]** |
| **2. Tính tái lập (Seed)** | Xác minh cùng seed phải ra cùng dữ liệu 100% | Cùng seed ra cùng mảng byte, mã băm SHA-256 trùng khớp 100%; khác seed ra hash khác nhau. | **[ĐẠT]** |
| **3. Đặc tính tập C1** | Kiểm tra độ dài đoạn lặp trung bình $\mu \approx 32$ | Trên mẫu 200 KB, đo được **6.225 đoạn lặp**, độ dài run trung bình thực tế đạt **`32.13 bytes`** (khớp lý thuyết ~32). | **[ĐẠT]** |
| **4. Đặc tính tập C2** | Kiểm tra kho từ vựng và phân phối Zipf | Sinh đúng **1.000 từ** vựng (độ dài 3–10 ký tự `a-z`); văn bản nối bằng dấu cách chuẩn xác. | **[ĐẠT]** |
| **5. Đặc tính tập C3** | Kiểm tra tính ngẫu nhiên phân phối đều | Tần suất xuất hiện của 256 giá trị byte dao động quanh mức kỳ vọng 1.000 lần (Min: 904, Max: 1087). | **[ĐẠT]** |

---

## 4. HƯỚNG DẪN THAO TÁC LẤY DẪN CHỨNG KHI BÁO CÁO / VẤN ĐÁP

### 4.1. Lệnh thực thi trên Terminal PowerShell
Để hiển thị bảng minh chứng kiểm nghiệm trực tiếp trước mặt giảng viên, mở Terminal tại thư mục gốc của dự án và chạy câu lệnh sau:

```powershell
& 'C:\Program Files\Java\jdk1.8.0_202\bin\java.exe' -cp "target\classes;target\test-classes" vn.csd.compression.DataGeneratorTest | Select-String -Pattern '---|Thong tin|Tu vung|sinh dung|CHUC MUNG'
```

### 4.2. Màn hình kết quả xuất ra (Dùng để chụp ảnh chèn vào báo cáo)
```text
--- 1. Kiem thu cac truong hop bien (Kich thuoc 0 byte va 1 byte) ---
--- 2. Kiem thu tinh tai lap bang Seed (Cung seed phai ra cung du lieu) ---
--- 3. Kiem thu dac tinh tap C1 (Do dai doan lap trung binh ~32 bytes) ---
   [Thong tin C1] Tong so doan lap: 6225, Do dai doan lap trung binh: 32.13 bytes (Ly thuyet: ~32 bytes)
--- 4. Kiem thu dac tinh tap C2 (Kho tu vung 1000 tu, phan phoi Zipf s=1) ---
[DAT] Tu vung dung 1000 tu
[DAT] C2 sinh dung kich thuoc yeu cau
--- 5. Kiem thu dac tinh tap C3 (Byte ngau nhien phan phoi dong deu tu 0 den 255) ---
[DAT] C3 sinh dung kich thuoc yeu cau
   [Thong tin C3] Tan suat ky vong moi byte: 1000 lan | Thap nhat: 904 lan, Cao nhat: 1087 lan
>>> CHUC MUNG: TAT CA CAC BAI TEST TV6 DA PASS!
```

---

## 5. KỊCH BẢN NÓI KHI THUYẾT TRÌNH VỀ PHẦN C1, C2, C3 (1–2 PHÚT)

Khi đến phần trình bày của mình, bạn nói tự tin theo 3 ý sau:

1. **Giới thiệu:**  
   *"Thưa thầy/cô, em là Thái Quang Huy, phụ trách cài đặt bộ sinh dữ liệu tổng hợp C1, C2, C3 phục vụ đo đạc hiệu năng cho 4 thuật toán nén."*
2. **Thuyết minh kỹ thuật theo màn hình minh chứng:**  
   * *"Ở tập C1, em mô phỏng dữ liệu có chuỗi lặp dài theo phân phối hình học $p = 1/32$. Như thầy cô thấy ở mục 3 trên màn hình, trên mẫu thử 200 KB, thuật toán sinh ra 6.225 đoạn lặp với độ dài trung bình thực tế đạt **32.13 bytes**, khớp chuẩn xác với kỳ vọng 32 bytes của đề bài."*  
   * *"Ở tập C2, em tạo sẵn kho từ vựng gồm đúng **1.000 từ** (từ 3 đến 10 ký tự `a-z`) và lấy mẫu theo phân phối Zipf $s=1$ bằng mảng tích lũy CDF kết hợp Binary Search, các từ cách nhau bởi dấu cách để mô phỏng văn bản thực tế."*  
   * *"Ở tập C3, dữ liệu được sinh ngẫu nhiên đều, tần suất xuất hiện của 256 byte dao động rất đều quanh mức kỳ vọng 1.000 lần (từ 904 đến 1087 lần)."*  
   * *"Đặc biệt, tất cả các tập dữ liệu đều đảm bảo **tính tái lập tuyệt đối bằng Seed** (cùng seed luôn cho ra cùng dữ liệu và mã băm SHA-256) và hỗ trợ ghi dạng Buffered Stream trực tiếp ra đĩa để sinh file 64 MiB mà không gây tràn bộ nhớ Heap của máy."*
3. **Kết luận:**  
   *"Bộ sinh dữ liệu đã vượt qua 100% các ca kiểm thử biên và kiểm thử thống kê, đáp ứng đầy đủ và chuẩn xác các yêu cầu kỹ thuật của đề tài. Em xin cảm ơn thầy cô!"*
