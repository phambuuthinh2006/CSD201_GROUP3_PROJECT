# So sánh RLE, Huffman, LZW và LZ4 — đo sơ bộ ngày 27/10/2026

> Kết luận: LZ4 không nhanh nhất trên mọi dữ liệu trong bản cài đặt hiện tại. Cần xét riêng tốc độ nén, tốc độ giải nén và tỷ lệ nén. Đây là thí nghiệm sơ bộ, chưa thay RQ3 chính thức.

## 1. Đã đo như thế nào?

- Máy: AMD Ryzen 5 6600H, 6 nhân/12 luồng, RAM khoảng 15 GiB; Linux x86_64, OpenJDK 17.0.20.1. Chi tiết kernel, bộ nhớ và tải máy trong environment.txt.
- Mỗi lượt chạy JVM có `-Xms512m -Xmx1g -XX:+UseG1GC`. Ba JVM mới chạy tuần tự, không chạy benchmark codec song song.
- Năm biến thể: RLE, Huffman, LZW, LZ4 HASH_LOG=12 và 16. Mỗi biến thể dùng đúng mã nguồn project tại lúc đo; danh sách SHA-256 nguồn đi kèm.
- Hai kích thước: 1 MiB và 4 MiB; bốn loại dữ liệu; seed 20261027. Tám input có cùng SHA-256 giữa ba JVM.
- Mỗi cấu hình/input trong mỗi JVM: 5 warm-up, sau đó 7 lượt đo. Thứ tự codec được xáo trộn có seed qua từng vòng để giảm thiên lệch vị trí.
- Tổng 40 cấu hình × 3 JVM × 7 = **840 lượt đo**; thêm **600 warm-up**. Cả **1.440 lượt** đều so độ dài và toàn bộ SHA-256 output với input, không có mismatch.
- Tạo input, tính SHA-256 và ghi CSV nằm ngoài thời gian đo. Nén và giải nén đo riêng bằng nanoTime, gồm cấp phát bộ đệm bên trong codec; không gọi System.gc giữa các ca. GC phát sinh bên trong vùng đo vẫn nằm trong kết quả.
- Tốc độ MiB/s tính theo kích thước gốc, 1 MiB = 1.048.576 byte. Ratio = toàn bộ container nén / số byte gốc, gồm header.
- Bảng dùng median của 7 lượt trong từng JVM, rồi median của ba median JVM. Mỗi ô đại diện cho 21 lần đo qua ba tiến trình; không coi 21 lượt là 21 JVM độc lập.
- summary.csv còn có min/max của ba median JVM để thấy dao động; khoảng đó không phải khoảng tin cậy thống kê. Chênh lệch vài phần trăm chỉ nên coi là gần nhau.

## 2. Dữ liệu dùng để đo

| Nhãn | Cách tạo |
|---|---|
| SINGLE_BYTE | Toàn byte ASCII A; tình huống rất thuận lợi cho RLE |
| LONG_RUNS | Độ dài run hình học kỳ vọng 32 byte, giá trị run chọn đều 0–255; run liền kề có thể trùng giá trị |
| WORD_TEXT | Từ vựng 1.000 từ sinh bằng chữ thường, dài 3–10; lấy mẫu theo trọng số Zipf 1/rank, cách nhau dấu cách |
| UNIFORM_RANDOM | Random.nextBytes với seed cố định; dữ liệu giả ngẫu nhiên gần phân bố đều |

Các input được sinh riêng trong harness vì generator chung chưa có. Không gắn nhãn đây là corpus R2/R3 hoặc bộ C1–C3 chính thức; chỉ dùng để khảo sát sơ bộ. Có một seed, chưa kiểm độ ổn định qua nhiều seed.

## 3. Tốc độ và tỷ lệ nén

Tốc độ lớn hơn tốt hơn; ratio nhỏ hơn tốt hơn. Ratio 0,42 nghĩa là container còn khoảng 42% kích thước gốc; ratio lớn hơn 1 nghĩa là file bị phình ra.

### Input 4 MiB

| Dữ liệu | Codec | Ratio | Nén (MiB/s) | Giải nén (MiB/s) |
|---|---|---:|---:|---:|
| Một byte lặp | RLE | 0.00785 | 3190.1 | 6231.0 |
| Một byte lặp | Huffman | 0.12506 | 365.2 | 1127.3 |
| Một byte lặp | LZW | 0.00104 | 463.3 | 478.0 |
| Một byte lặp | LZ4-12 | 0.00393 | 264.5 | 506.8 |
| Một byte lặp | LZ4-16 | 0.00393 | 264.0 | 506.7 |
| Run dài | RLE | 0.06263 | 1394.2 | 1475.5 |
| Run dài | Huffman | 1.00006 | 159.2 | 84.2 |
| Run dài | LZW | 0.23407 | 289.6 | 367.4 |
| Run dài | LZ4-12 | 0.17858 | 206.1 | 346.2 |
| Run dài | LZ4-16 | 0.18586 | 205.6 | 343.0 |
| Văn bản theo từ | RLE | 1.94088 | 180.9 | 297.6 |
| Văn bản theo từ | Huffman | 0.57600 | 174.0 | 101.5 |
| Văn bản theo từ | LZW | 0.42094 | 140.6 | 209.2 |
| Văn bản theo từ | LZ4-12 | 0.47860 | 139.4 | 243.3 |
| Văn bản theo từ | LZ4-16 | 0.41995 | 146.2 | 249.1 |
| Byte ngẫu nhiên | RLE | 1.99227 | 184.6 | 306.8 |
| Byte ngẫu nhiên | Huffman | 1.00006 | 178.7 | 83.7 |
| Byte ngẫu nhiên | LZW | 1.41932 | 139.8 | 201.3 |
| Byte ngẫu nhiên | LZ4-12 | 1.00393 | 187.7 | 12489.3 |
| Byte ngẫu nhiên | LZ4-16 | 1.00392 | 105.5 | 12835.8 |

### Input 1 MiB

| Dữ liệu | Codec | Ratio | Nén (MiB/s) | Giải nén (MiB/s) |
|---|---|---:|---:|---:|
| Một byte lặp | RLE | 0.00786 | 2376.6 | 3534.4 |
| Một byte lặp | Huffman | 0.12526 | 343.8 | 516.1 |
| Một byte lặp | LZW | 0.00208 | 308.3 | 457.5 |
| Một byte lặp | LZ4-12 | 0.00395 | 332.8 | 469.6 |
| Một byte lặp | LZ4-16 | 0.00395 | 331.5 | 476.1 |
| Run dài | RLE | 0.06275 | 1355.7 | 1413.4 |
| Run dài | Huffman | 1.00024 | 149.7 | 82.6 |
| Run dài | LZW | 0.23196 | 273.1 | 367.3 |
| Run dài | LZ4-12 | 0.17863 | 194.5 | 344.7 |
| Run dài | LZ4-16 | 0.18578 | 192.7 | 340.5 |
| Văn bản theo từ | RLE | 1.94139 | 185.8 | 295.1 |
| Văn bản theo từ | Huffman | 0.57611 | 164.5 | 95.2 |
| Văn bản theo từ | LZW | 0.42135 | 134.0 | 209.1 |
| Văn bản theo từ | LZ4-12 | 0.48019 | 138.8 | 244.0 |
| Văn bản theo từ | LZ4-16 | 0.42229 | 145.4 | 247.9 |
| Byte ngẫu nhiên | RLE | 1.99215 | 188.2 | 303.5 |
| Byte ngẫu nhiên | Huffman | 1.00026 | 175.5 | 83.5 |
| Byte ngẫu nhiên | LZW | 1.41918 | 162.7 | 195.3 |
| Byte ngẫu nhiên | LZ4-12 | 1.00394 | 187.3 | 11455.0 |
| Byte ngẫu nhiên | LZ4-16 | 1.00394 | 106.8 | 10980.0 |

## 4. Biến thể có median tốc độ cao nhất

Bảng này chỉ xếp median trong mẫu đo, không khẳng định ưu thế có ý nghĩa thống kê. Nếu hai vị trí đầu lệch dưới 5%, ghi thêm tên runner-up để tránh kết luận mạnh.

| Input | Dữ liệu | Nén | Giải nén | Ratio thấp nhất |
|---|---|---|---|---|
| 1 MiB | Một byte lặp | RLE | RLE | LZW (0.00208) |
| 1 MiB | Run dài | RLE | RLE | RLE (0.06275) |
| 1 MiB | Văn bản theo từ | RLE | RLE | LZW (0.42135) |
| 1 MiB | Byte ngẫu nhiên | RLE ≈ LZ4-12 (lệch 0.5%) | LZ4-12 ≈ LZ4-16 (lệch 4.3%) | Huffman (1.00026) |
| 4 MiB | Một byte lặp | RLE | RLE | LZW (0.00104) |
| 4 MiB | Run dài | RLE | RLE | RLE (0.06263) |
| 4 MiB | Văn bản theo từ | RLE ≈ Huffman (lệch 3.9%) | RLE | LZ4-16 (0.41995) |
| 4 MiB | Byte ngẫu nhiên | LZ4-12 ≈ RLE (lệch 1.7%) | LZ4-16 ≈ LZ4-12 (lệch 2.8%) | Huffman (1.00006) |

## 5. Kết luận có thể trình bày với thầy

- **LZ4 không nhanh nhất trên tất cả dữ liệu.** Trong dữ liệu một byte lặp và run dài, RLE nhanh hơn rõ về cả nén và giải nén trong bản cài đặt đang đo.
- Với byte ngẫu nhiên 4 MiB, tốc độ nén LZ4-12: 187.7 MiB/s; RLE: 184.6 MiB/s; LZ4-16: 105.5 MiB/s. Đọc cùng bảng xếp median và lưu ý các chênh lệch nhỏ.
- Với byte ngẫu nhiên 4 MiB, LZ4-12 giải nén 12489.3 MiB/s, LZ4-16 giải nén 12835.8 MiB/s. Dữ liệu gần như toàn literal nên decoder chủ yếu sao chép mảng. Đây là tốc độ trong RAM với buffer vừa được tạo, cache nóng; không phải tốc độ đọc file/ổ đĩa.
- Với văn bản 4 MiB, RLE có ratio 1.941: đầu ra lớn gần gấp đôi. LZ4-16 có ratio 0.420, giải nén 249.1 MiB/s; LZW có ratio 0.421, giải nén 209.2 MiB/s. Cần đọc tốc độ cùng kích thước đầu ra.
- Hash log không cho cùng kết quả trên mọi input: văn bản 4 MiB có ratio LZ4-12/16 là 0.479/0.420; run dài là 0.179/0.186. Tốc độ cũng phụ thuộc input.
- Với một byte lặp 4 MiB, LZW có ratio 0.00104, RLE nén 3190.1 MiB/s. Thuật toán nhanh nhất và thuật toán nén nhỏ nhất có thể khác nhau.

> Cách nói ngắn: Em đo năm biến thể trên cùng dữ liệu và kiểm SHA-256 mọi lượt. Kết quả cho thấy LZ4 nổi bật ở giải nén dữ liệu gần ngẫu nhiên; RLE nhanh ở run dài; còn dữ liệu văn bản phải cân bằng tốc độ với tỷ lệ nén. Em chưa kết luận LZ4 nhanh nhất trong mọi trường hợp.

## 6. Giới hạn và bước tiếp theo

- Đây là so sánh các **bản cài đặt Java của nhóm**, không phải kết luận phổ quát về mọi implementation RLE/Huffman/LZW/LZ4.
- Chỉ 1/4 MiB, một seed và dữ liệu tổng hợp. Chưa đo Silesia/Canterbury, 64 MiB, nhiều seed hay ma trận RQ3 chính thức.
- Không đo peak heap; không xếp hạng bộ nhớ bằng chênh lệch heap tức thời.
- Chạy trên Linux desktop, không khóa tần số CPU, pin affinity hoặc loại bỏ toàn bộ tiến trình nền. Đã lưu môi trường/tải máy; chênh lệch nhỏ có thể do nhiễu.
- Năm warm-up mỗi cấu hình và ba JVM giảm thiên lệch nhưng không chứng minh JIT hội tụ hoàn toàn. Chưa dùng JMH hay kiểm định thống kê.
- Chỉ kiểm round-trip các input đo, không thay unit test dữ liệu hỏng. Các lỗi decoder với input hỏng đã nêu trong review vẫn cần xử lý riêng.
- Bước tiếp theo: TV4/TV6 đưa cùng chính sách đo vào runner chuẩn, bổ sung corpus chuẩn, 64 MiB, nhiều seed và peak heap trước khi viết kết luận báo cáo chính thức.

## 7. Dữ liệu gốc và cách tái tạo

Thư mục kết quả: `/home/phambuuthinh/Downloads/CSD201_GROUP3_PROJECT/results/codec-comparison-20261027-125308-xyiK9z`.

- `raw-fork-1.csv` đến `raw-fork-3.csv`: 840 dòng đo; `inputs-fork-*.csv`: SHA-256 đầy đủ của input.
- `summary.csv`: 40 dòng tổng hợp, ratio và tốc độ cùng khoảng dao động median qua JVM.
- `fork-medians.csv`: 240 median riêng (40 cấu hình × 3 JVM × 2 thao tác).
- `environment.txt`, `source-sha256.txt`, `fork-*.log`: môi trường, nhận diện source và log tiến trình.
- Bản harness Java và shell đã chép vào thư mục kết quả để lưu lại cách đo.

```bash
cd /home/phambuuthinh/Downloads/CSD201_GROUP3_PROJECT
bash scripts/benchmark/run-comparison.sh
# Script in đường dẫn thư mục mới; truyền đường dẫn đó vào lệnh dưới:
python3 scripts/benchmark/summarize-comparison.py /duong/dan/thu/muc/ket/qua
```

Các lệnh chỉ tạo kết quả mới, không sửa thuật toán và không push GitHub. Nếu chưa có Maven trên PATH, runner tìm bản ở ~/.cache/csd-tools hoặc dùng biến CSD_MAVEN_BIN.
