# TV1 — RLE và phân tích RQ1

## Định dạng RLE1

Container có header cố định 12 byte: magic `RLE1` (4 byte ASCII), độ dài dữ liệu gốc (8 byte big-endian), tiếp theo là payload các cặp `(count, value)`. `count` là một byte unsigned từ 1 đến 255. Run vượt 255 byte được chia thành những cặp liên tiếp có cùng `value`; ví dụ run 256 byte thành `(255, value), (1, value)`. Input rỗng chỉ có header với độ dài gốc bằng 0.

Decoder xác thực magic, kích thước header, payload có số byte chẵn, count khác 0, không giải mã vượt độ dài đã khai báo và tổng độ dài run phải khớp chính xác. Độ dài âm hoặc lớn hơn giới hạn mảng Java bị từ chối trước khi cấp phát output.

## Phân tích RLE cho RQ1

Với `N` byte input và `R` run sau khi tách mọi run thành đoạn tối đa 255 byte, kích thước container là `12 + 2R` byte. Do đó:

`ratio = (12 + 2R) / N` (với `N > 0`); file rỗng không có ratio.

Encoder và decoder đều tuyến tính theo kích thước dữ liệu (`O(N)`). Decoder thêm thao tác xử lý từng cặp run. Encoder ghi hai byte cho mỗi đoạn run và header, nên dữ liệu không có run dài có thể nở gần gấp đôi.

Theo đặc tả synthetic đã cung cấp, C1 có run dài trung bình 32 byte với giá trị byte phân bố đều. Đây là trường hợp thuận lợi cho RLE: xấp xỉ một cặp trên 32 byte trước hiệu ứng biên và các run cần chia tại 255. C3 có byte độc lập, đồng xác suất 0–255; hai byte kề nhau trùng nhau với xác suất `1/256`, nên số run xấp xỉ `255N/256` và ratio kỳ vọng tiến gần `2 × 255/256` khi kích thước tăng. C2 là văn bản từ vựng Zipf; việc từ lặp lại không giúp RLE nếu byte giống nhau không đứng liền nhau, vì vậy cần dùng số liệu đo thay vì suy ra tỷ lệ từ Zipf.

Các giá trị trên là phân tích cấu trúc/ước lượng, không phải kết quả thực nghiệm. Chưa có kết luận RQ1 hay `rq1.csv` trong checkout này. Hiện chỉ có `RLECodec` và LZ4; chưa có `HuffmanCodec`, `LZWCodec` hoặc runner RQ1 chung. Phép đo `compress_heap_aux_bytes`/`decompress_heap_aux_bytes` cũng chưa có định nghĩa thực thi. Theo thống nhất, không xuất CSV RLE-only thay cho ma trận đầy đủ.

Trước khi chạy ma trận RQ1 cần tích hợp Huffman và LZW, thống nhất quy tắc sinh dữ liệu tái lập (PRNG, seed cụ thể, cách tạo từ vựng và phân phối độ dài từ, cách cắt đúng kích thước byte), cũng như warm-up, số lần lặp, phương pháp đo heap phụ trợ và quy ước cho các cột không áp dụng như `lz4_sequence_count`. Ma trận đã chốt gồm C1–C3, kích thước 1, 4, 16, 32, 64 MiB và 3 seed; mỗi codec/lượt chạy phải kiểm tra độ dài và SHA-256 đầy đủ trước khi ghi `status=PASS`.

## Kiểm thử

`RLECodecTest` bao gồm T01 (input rỗng), T02 (1 byte), T03 (1 MiB cùng byte), T07 (run 255/256), T09 (magic sai), T12 (nhiều kích thước ngẫu nhiên với SHA-256), cùng kiểm tra payload malformed.
