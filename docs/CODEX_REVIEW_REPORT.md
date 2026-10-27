# CODEX — Báo cáo review mã nguồn CSD201 Group 3

Ngày review: 26/10/2026. Phạm vi: toàn bộ **20 file Java trong `src/`**, README, hai tài liệu TV5 và cấu hình Maven. Chỉ review và tạo báo cáo; không sửa source.

## 1. Căn cứ và kết luận

Đã đọc `README.md`, `docs/TV5_LZ4_RQ3.md`, `docs/TV5_LZ4_HUONG_DAN_TEST.md`. Do README chủ yếu mô tả cấu trúc/API và tài liệu TV5 dẫn tới plan gốc, đã đọc thêm `/home/phambuuthinh/PLAN_14_NGAY_NEN_DU_LIEU_NHOM_6.md` để đối chiếu format RLE, generator, ma trận RQ1–RQ3, phép đo và T01–T13.

**Dự án chưa đủ điều kiện nghiệm thu toàn bộ.** LZ4 và runner riêng TV5 đã ổn trong phạm vi đã công bố. RLE, generator và test tích hợp chưa cài đặt. LZW/Huffman có thuật toán hoạt động với input hợp lệ, nhưng cần sửa xử lý container hỏng. Runner chung chưa đáp ứng đầy đủ phép đo và metadata của plan.

Chưa có văn bản quy định chung/rubric gốc của giảng viên trong các tài liệu được cung cấp. Vì vậy đánh giá dưới đây đối chiếu các đặc tả đã ghi trong repository và plan, không khẳng định schema CSV đề xuất hay endian block length BE là quy định chính thức. Quy định bỏ static được tài liệu TV5 mô tả cho phần TV5; chưa có căn cứ áp dụng bắt buộc cho mọi module.

Mức ưu tiên: **P1** chặn nghiệm thu/tính đúng hoặc làm sai kết luận benchmark; **P2** cần hoàn thiện độ tin cậy và tích hợp; **P3** tài liệu/quy ước.

## 2. Kiểm chứng thực tế

Môi trường: Linux amd64, OpenJDK 17.0.20.1+1, Maven 3.9.9 tải vào `/tmp/csd-codex-review/`, test JVM `-Xmx1g`. Maven chưa có trên PATH. Các lệnh đã chạy:

```bash
/tmp/csd-codex-review/apache-maven-3.9.9/bin/mvn clean verify
java -Xmx1g -cp target/classes:target/test-classes vn.csd.compression.LZWCodecTest
java -Xmx1g -cp target/classes:target/test-classes vn.csd.compression.HuffmanCodecTest
```

- `clean verify`: BUILD SUCCESS, **18 lượt JUnit, 0 failure/error/skip**: 16 LZ4CodecTest + 2 LZ4Rq3RunnerTest. Có test 64 MiB cho cả hai hash log, random và dữ liệu lặp, cùng oracle hai chiều.
- Hai chương trình test Huffman/LZW chạy riêng đều kết thúc PASS. Chúng **không được Surefire tự thực thi**, vì không có annotation JUnit; 18 PASS không đại diện test toàn bộ dự án.
- Biên dịch/chạy probe bổ sung ngoài repository bằng `javac --release 8`, ghi nhận các phản ví dụ dưới đây. Probe/log ở `/tmp/csd-codex-review/` chỉ là vật liệu kiểm chứng tạm, không phải bộ test đã tích hợp vào bản nộp.
- Chưa chạy JVM 8 thực tế, chưa chạy benchmark đầy đủ corpus hoặc xác minh mọi codec ở 64 MiB. Không có `data/` trong checkout; `results/` hiện chỉ có smoke LZ4 và log/README.

| Ca kiểm chứng bổ sung | Kết quả thực tế | Đánh giá |
|---|---|---|
| LZW: nén `[65]`, sửa long BE ở offset 4 từ 1 thành 3 | Trả `[65,0,0]` | Sai: thiếu dữ liệu nhưng tự để zero và trả thành công |
| LZW: nén `[65,66]`, sửa original length thành 1 | Trả `[65]` | Bỏ qua mã dư |
| LZW: nén `ABABABA`, sửa original length thành 4 | Trả `[65,66,65,66]` | Cắt một chuỗi mã để vừa header thay vì báo lỗi |
| LZW: nén `[65]`, đặt nibble padding cuối thành 15 | Vẫn trả `[65]` | Không kiểm padding 0 |
| Huffman: nén `[65]`, bật bit padding thấp cuối cùng | Vẫn trả `[65]` | Không kiểm padding 0 |
| Huffman: nối byte `99` sau container của `[65]` | Vẫn trả `[65]` | Không kiểm payload thừa |
| HUF1 thủ công: original length 1, lengths của symbol 0/1/2 lần lượt 1/1/2, payload 0 | Trả `[0]` | Chấp nhận bảng oversubscribed, tổng Kraft = 1.25 |
| `new BitReader(new byte[1], Integer.MAX_VALUE, 1)` | Constructor chấp nhận | Cộng offset + length bị tràn int |
| Runner chung với input rỗng | ratio `0.0`, status PASS | Trái yêu cầu không tính ratio cho file rỗng |

Các ca padding/trailing phản ánh validation format, không có nghĩa decoder phải phát hiện mọi thay đổi dữ liệu hợp lệ về cấu trúc. Container không có checksum; việc xác minh nội dung vẫn thuộc SHA-256 của runner.

## 3. Đánh giá từng file production

### 3.1. `src/main/java/vn/csd/compression/codec/Codec.java`

**Đã ổn — giữ nguyên API.** Có đủ `compress(byte[])`, `decompress(byte[])`, `getName()` theo plan, ghi rõ header thuộc codec và decoder kiểm magic. Không cần thay interface để thêm metadata; có thể dùng đối tượng kết quả/adapter của runner. Javadoc tên codec viết thường trong khi LZ4 trả `LZ4`: nên thống nhất quy ước ở khâu xuất dữ liệu (P3), không phải lỗi thuật toán.

### 3.2. `src/main/java/vn/csd/compression/codec/RLECodec.java`

**Chưa hoàn thành — P1.** Cả hai hàm chính ném `UnsupportedOperationException` tại dòng 17/23. Chưa đạt format hay bất kỳ ca round-trip nào; chặn baseline RQ1/RQ2/RQ3.

Hướng sửa: viết container `RLE1` + long BE độ dài gốc + cặp `(count,value)`; count unsigned 1–255, tách run 256 thành 255+1. Empty chỉ có header. Decoder kiểm magic, chiều dài header/payload chẵn, count khác 0, output không vượt/thiếu so với header, từ chối payload dư và length ngoài 0–64 MiB trước cấp phát. Encoder cũng giới hạn 64 MiB. Thêm test T01–T04, T07, T09, random và biên lớn.

### 3.3. `src/main/java/vn/csd/compression/codec/LZWCodec.java`

**Đạt lõi thuật toán, cần sửa trước nghiệm thu — P1/P2.** Đúng header 12 byte, MSB-first 12-bit, mảng tra `int[4096*256]`, freeze ở 4096, KwKwK và thống kê dictionary-full. Các test hợp lệ hiện có chạy PASS; giữ cấu trúc thuật toán này.

Các vấn đề cụ thể:

1. **P1 — Decoder trả thành công khi thiếu output**: cuối hàm dòng 289 không kiểm `outOffset == origLen`; vùng chưa giải mã giữ zero. Thêm kiểm tra chính xác tổng byte xuất.
2. **P1 — Bỏ mã dư/cắt chuỗi mã**: dòng 232–234 thoát khi đủ output, dòng 263–264 rút ngắn `len` để vừa header. Padding là bit, không phải byte thuộc chuỗi giải mã; phải từ chối nếu chuỗi vượt remaining output, và phải tiêu thụ đủ các mã hợp lệ của payload.
3. **P2 — Padding/empty không chặt**: nibble thấp cuối ở payload lẻ không được kiểm 0; `origLen == 0` trả ngay, bỏ qua payload dư. Empty phải đúng 12 byte; ca lẻ kiểm 4 bit padding bằng 0.
4. **P2 — Encoder không giới hạn 64 MiB**, trong khi decoder từ chối quá giới hạn; thêm guard đầu `compress` để API đối xứng, tránh tạo container chính decoder không nhận.
5. **P2 — Bộ nhớ lớn**: encoder giữ mọi mã trong `int[]` rồi mới pack; decoder cũng unpack toàn bộ mã ra `int[]`. Với random 64 MiB số mã có thể gần số byte, riêng mảng mã tốn hàng trăm MiB và có bản sao khi resize. Pack/unpack mã tăng dần bằng bit buffer để giảm peak; cần đo 64 MiB trước khi tuyên bố đạt M3. Đây là rủi ro theo source, chưa kết luận đã OOM thực tế.
6. Lỗi payload dùng `IllegalStateException` lẫn `IllegalArgumentException`: nên thống nhất exception input không hợp lệ cho caller dễ xử lý. `dictionaryFullAt` là trạng thái lượt nén gần nhất; runner lấy ngay sau nén, công bố đây là index zero-based `i`, không mặc định là số byte đã xử lý.

### 3.4. `src/main/java/vn/csd/compression/codec/HuffmanCodec.java`

**Đạt lõi canonical Huffman, cần sửa validation — P1/P2.** Đúng HUF1, header 268 byte, bảng 256 lengths, tie-break xác định, MSB-first, mã đơn 1 bit, empty đúng ở encoder, giới hạn 64 MiB. Test hiện hữu chạy riêng PASS. Giữ cơ chế frequency/tree/canonical và xử lý input hợp lệ.

1. **P1 — Nhận bảng mã không prefix-free**: `generateCanonicalCodes` dòng 368 và `buildDecodeTree` dòng 395 không kiểm oversubscription; dựng cây chỉ kiểm leaf đã gán, không kiểm đi xuyên leaf hoặc gán leaf lên node có con. Bảng 1/1/2 nêu trên đã bị nhận sai. Kiểm Kraft bằng số slot theo từng độ dài; khi dựng cây chặn cả hai dạng prefix collision. Chốt chính sách bảng incomplete theo format nhóm, không đánh đồng incomplete với oversubscribed.
2. **P2 — Độ dài mã từ header 0–255 nhưng mã lưu `int`**: không kiểm giới hạn trước shift; Java shift int lấy số bit modulo 32. Với bảng bất thường có thể sinh mã sai/collision. Cần chốt max length được hỗ trợ, từ chối vượt giới hạn biểu diễn trước sinh mã, hoặc dùng biểu diễn mã dài phù hợp. Không áp một giới hạn làm mất khả năng giải container hợp lệ từ encoder; chứng minh bound dựa trên input tối đa và tần suất nguyên nếu chọn int.
3. **P2 — Không kiểm phần kết thúc payload**: hai nhánh giải mã trả ngay sau đủ số symbol (dòng 180/206); cho phép bit padding khác 0 và byte dư. Dựa trên `getTotalBitsRead()`, yêu cầu số byte payload đúng `ceil(consumedBits/8)` và mọi bit padding đều 0. Empty ở dòng 146 cũng cần kiểm bảng lengths toàn 0, không payload theo format encoder đã mô tả.
4. **P3 — API phụ/magic**: `MAGIC` là mảng public mutable; có thể bị caller đổi làm lệch format toàn cục. Nên private hoặc trả bản sao. `calculateExpectedPayloadBits(null)` trả 0 khác chính sách null của compress/computeCodeLengths; chuẩn hóa nếu dùng public API này.

### 3.5. `src/main/java/vn/csd/compression/codec/LZ4Codec.java`

**Đã ổn — giữ nguyên thuật toán và container hiện tại.** Đáp ứng hai hash log, hash 4 byte LE, greedy nearest candidate, token/extension, offset LE 1–65535, match tối thiểu 4, 5 literal cuối và vị trí match cuối; decoder kiểm cấu trúc trước cấp phát, forward copy xử lý overlap, giới hạn 64 MiB. Thống kê sequence gồm tail và average chia số match đúng tài liệu. Source không khai báo static; API instance đúng cập nhật TV5.

18 test TV5 đã chạy lại, bao gồm oracle hai chiều và biên lớn. Quy tắc payload phù hợp [LZ4 Block Format chính thức](https://github.com/lz4/lz4/blob/dev/doc/lz4_Block_format.md) về token, offset và kết thúc block. `LZ4B` là container riêng, không phải Frame. BE của block length vẫn là quy ước cần giảng viên xác nhận; không có căn cứ sửa endian lúc này. Tên `LZ4` khác chữ hoa/thường của codec khác cần được chuẩn hóa khi tổng hợp CSV; không cần viết lại codec vì việc này.

### 3.6. `src/main/java/vn/csd/compression/io/BitReader.java`

**Cơ chế MSB-first/long buffer đã ổn; cần sửa kiểm tra slice — P2.** Refill và đọc 1–32 bit phù hợp cách Huffman sử dụng. Nhưng dòng 26 dùng `offset + length > data.length`, có thể overflow; constructor nhận slice không tồn tại như probe đã xác nhận.

Sửa thành kiểm tra không cộng tràn, ví dụ `offset < 0 || length < 0 || offset > data.length || length > data.length - offset`. Sau đó có thể giữ end hợp lệ để dùng lại. Thêm test max int, offset cuối mảng, slice rỗng và đọc thiếu bit. `readBits(numBits <= 0)` trả 0 trái Javadoc 1–32; nên chốt zero có được phép hay không, còn số âm phải báo lỗi.

### 3.7. `src/main/java/vn/csd/compression/io/BitWriter.java`

**Đã ổn cho đường dùng hiện tại — giữ nguyên cơ chế ghi.** Ghi 1–32 bit MSB-first, long buffer, flush padding zero, bộ đếm bit không gồm padding và `toByteArray()` phù hợp Huffman. Test thủ công Huffman đã kiểm round-trip 27 bit; chưa có lỗi ghi bit được chứng minh.

P3: Javadoc 1–32 nhưng `numBits <= 0` được bỏ qua; thống nhất với reader, nên reject số âm. Công bố flush là kết thúc segment vì ghi tiếp sau flush tạo byte alignment/padding giữa luồng. Đây là làm rõ hợp đồng public, không phải blocker của codec đang dùng.

### 3.8. `src/main/java/vn/csd/compression/dataset/ByteOrdering.java`

**Đã ổn về thuật toán — giữ nguyên.** `original` clone, `grouped` counting sort unsigned 0–255, `shuffled` Fisher–Yates với `Random(seed)` trên bản sao; giữ histogram và không sửa input. Không bắt buộc shuffle mọi lần phải khác original, đặc biệt input rỗng/đồng nhất.

P2 ở kiểm thử tích hợp: chưa có test tự động T11 xác nhận 256 ô histogram, unsigned order, seed tái lập và input bất biến. P3: xóa comment TODO lỗi thời vì hai thuật toán thực tế đã cài.

### 3.9. `src/main/java/vn/csd/compression/dataset/DataGenerator.java`

**Chưa hoàn thành — P1.** Chỉ có class rỗng/TODO; chưa thể tạo C1–C3 và tái lập RQ1/RQ3.

Cần API nhận loại dữ liệu, số byte và seed, validate size 0–64 MiB. C1: run hình học kỳ vọng 32, giá trị đều 0–255, cắt run cuối cho đúng size. C2: vocabulary 1.000 từ dài 3–10 chữ thường, lấy mẫu Zipf s=1, cách bằng space; chốt rõ seed vocabulary/sampling và xử lý cắt tại size. C3: byte đều 0–255. Xuất manifest gồm thuật toán/version, seed, size, SHA-256. Test cùng seed cho cùng bytes, size chính xác và đặc trưng phân phối bằng tiêu chí hợp lý, tránh assertion thống kê quá chặt gây flaky.

### 3.10. `src/main/java/vn/csd/compression/metrics/Entropy.java`

**Đã ổn — giữ nguyên công thức.** Histogram unsigned, đếm long, chỉ xét p>0 và `-Σp log2(p)` đúng đơn vị bit/byte; empty trả 0 hợp lý. Không phụ thuộc thứ tự nên phù hợp RQ2. Cần thêm test hằng=0, hai byte cân bằng=1, đủ 256 byte cân bằng=8 và bất biến theo reorder. Null trả 0 là lựa chọn hiện hữu, nên ghi rõ hợp đồng nếu nhóm muốn fail-fast.

### 3.11. `src/main/java/vn/csd/compression/metrics/BenchmarkRunner.java`

**Có khung đo đúng, chưa đủ làm runner chính thức — P1/P2.** Đã đo riêng compress/decompress với nanoTime, dùng kích thước gốc cho MiB/s, kích thước nén gồm container; entropy/so sánh/ghi row nằm ngoài vùng thời gian codec. `Arrays.equals` kiểm toàn bộ bytes mạnh về tính đúng; không phải chỉ so hash prefix.

Các phần cần sửa:

1. **P1 — Heap delta không phải heap peak** (dòng 18–43). Chỉ lấy used heap trước/sau, có thể bỏ lỡ buffer tạm đã bị GC; `System.gc()` không bảo đảm GC đã hoàn tất. Không được xuất các giá trị này như đã đáp ứng `heap_peak - heap_before`. Thống nhất cơ chế peak theo pha, ví dụ sampling có ghi chu kỳ/sai số hoặc đo trong tiến trình biệt lập có instrumentation phù hợp; ghi cách xử lý input/output và GC. Nếu chưa làm, để chỉ số chính thức trống và gắn nhãn phép đo tạm.
2. **P1 — Thiếu tham số/thống kê**: `toCsvRow` dòng 179 luôn để trống `params`, dictionary-full và hai cột LZ4. LZ4-12/16 đều tên `LZ4`, không phân biệt được trong CSV. Bổ sung metadata có cấu trúc, lấy `getDictionaryFullAt()` ngay sau nén và `analyze()` ngoài thời gian; chốt định nghĩa sequence/average theo TV5.
3. **P2 — Empty ratio sai**: dòng 51 dùng 0.0, làm hiểu nhầm nén hoàn hảo; xuất ô trống như runner TV5.
4. **P2 — Chưa có điều phối warm-up/repeats/ma trận**: `run` chỉ một lượt, chưa có iteration trong result hay environment log. Có thể giữ hàm primitive này nhưng thêm tầng điều phối tuần tự, warm-up kiểm correctness không ghi số liệu, repeats có định danh, tham số máy/JVM/seed/hash đầy đủ. Đừng khẳng định đây đã là runner đủ RQ1–RQ3.
5. **P2 — SHA theo plan**: hiện so bytes rồi chỉ hash output nếu match; chưa tính/so SHA-256 input/output mọi lượt theo thủ tục plan và không lưu hash gốc để đối chiếu manifest. Tính SHA gốc trước thời gian đo, SHA output sau đo, so đầy đủ 32 byte, giữ prefix chỉ cho hiển thị. `getSha256Hex` không được nuốt mọi exception và trả chuỗi rỗng dẫn tới lỗi substring thiếu ngữ cảnh.
6. Chưa có chính sách chung cho codec exception: có thể dừng fail-fast hoặc ghi FAIL kèm nguyên nhân rồi kết thúc không thành công; phải phân biệt file kết quả một phần với ma trận đủ. Có `status=FAIL` khi mismatch là đúng hướng, tầng tổng hợp phải loại các dòng lỗi.

### 3.12. `src/main/java/vn/csd/compression/metrics/CsvWriter.java`

**Đúng header đề xuất và có append, cần tăng độ tin cậy — P2.** Tạo thư mục cha, ghi header khi file mới/rỗng, phần row trong BenchmarkResult đã escape dấu phẩy/quote/newline/CR đúng. Giữ các phần này.

Lỗi thư mục/ghi chỉ được in stderr rồi trả về; caller không biết dữ liệu chưa lưu. `PrintWriter` còn nuốt một số lỗi ghi sau khi mở nếu không `checkError()`. Chuyển sang `BufferedWriter` UTF-8 ném IOException tới caller (hoặc trả kết quả lỗi rõ ràng). `FileWriter` dùng charset mặc định máy, dễ lệch tên file/dataset tiếng Việt; dùng UTF-8 cố định. Trước append phải xác minh header/schema file hiện có; không trộn kết quả schema khác. Chốt chính sách append/resume hay CREATE_NEW cùng runner, tránh ghi lặp cấu hình mà không có run/iteration identity. Không cần đa luồng vì plan yêu cầu benchmark tuần tự.

### 3.13. `src/main/java/vn/csd/compression/metrics/LZ4Rq3Runner.java`

**Đã ổn — giữ nguyên làm runner riêng TV5.** Đúng phạm vi LZ4-only được công bố: hash log 12/16, warm-up/repeats, size check trước đọc, SHA đầy đủ và độ dài mọi lượt, analyze/I/O ngoài giờ đo, ratio empty trống, Locale.ROOT, UTF-8, quote đường dẫn, chống ghi đè và log JVM/OS/heap. Hai test runner PASS.

Không có peak heap, baseline, H0 hay manifest không phải lỗi che giấu: tài liệu đã nói rõ đây là công cụ tạm. Việc cần làm tiếp là tích hợp API/statistics vào runner chung và cung cấp corpus/machine/CPU, không biến smoke thành kết luận RQ3. Khi lỗi giữa chừng file có thể có các dòng PASS đã flush; cần tầng điều phối/manifest đánh dấu complete trước tổng hợp. Có thể bổ sung test lỗi CLI/oversize/missing file/CSV quoting khi mở rộng runner; chưa có căn cứ viết lại logic hiện tại.

### 3.14. `src/main/java/vn/csd/compression/Main.java`

**Chưa hoàn thành vai trò entry point ứng dụng — P2.** Hiện chỉ in banner, không chọn codec, đọc file hay gọi benchmark; chạy class này không demo được nén/giải nén. Cài CLI nhỏ hoặc điều phối các runner với usage, codec/hash log, đường dẫn input/output và lỗi trả exit status phù hợp; chuẩn bị input ngoài vùng đo. Nếu nhóm chủ ý chỉ dùng các runner độc lập thì README phải chỉ rõ đó là entry point thực tế và không tuyên bố Main đã có demo. Thống nhất tên Group 3/6 trong banner/pom/README (P3).

## 4. Đánh giá từng file test

### 4.1. `src/test/java/vn/csd/compression/LZ4CodecTest.java`

**Đã ổn — giữ nguyên.** JUnit 5 thực sự được chạy, 16 invocations PASS. Có empty/short literal-only, 256 byte, extension boundaries, overlap/offset 1 và 65535, seeded random/repetitive, input bất biến, cross-hash decode, malformed container/block, 64 MiB và oracle fast/high-compression hai chiều. Oracle chỉ test scope. Thêm fuzz về sau là cải tiến tùy chọn, không phải điều kiện để viết lại test hiện tại.

### 4.2. `src/test/java/vn/csd/compression/LZ4Rq3RunnerTest.java`

**Đã ổn trong phạm vi runner hiện tại — giữ nguyên.** 2 test JUnit PASS; kiểm số dòng measured không gồm warm-up, hai hash log, SHA/empty ratio/sequence và chống ghi đè. `split(",")` chỉ dùng fixture đường dẫn đơn giản, chưa kiểm CSV quoting; khi mở rộng hãy thêm fixture có dấu phẩy/quote và dùng parser CSV đúng. Test này không chứng minh full RQ3 hay hiệu năng ổn định.

### 4.3. `src/test/java/vn/csd/compression/LZWCodecTest.java`

**Có kiểm chứng hữu ích, cần tích hợp và bổ sung — P1/P2.** Chạy `main()` PASS: empty, 1 byte, 1 MiB lặp, 256 byte, KwKwK, dictionary-full, magic, random SHA. Giữ dữ liệu/ý tưởng test.

Đổi các ca sang JUnit để `mvn test` thực sự chạy và Surefire đếm được; có thể giữ main wrapper nếu cần demo. Thêm regression cho đủ các lỗi LZW mục 3.3, payload không có mã với origLen>0, mã đầu >=256, mã >nextEntry, header length âm/quá lớn, padding, truncated, encode input >64 MiB. Test 12-bit byte pattern độc lập và vị trí dictionary-full/index chính xác; test reset thống kê khi tái sử dụng instance. Bổ sung 64 MiB tuần tự với heap đã công bố trước kết luận đáp ứng T12.

### 4.4. `src/test/java/vn/csd/compression/HuffmanCodecTest.java`

**Có kiểm chứng hữu ích, cần tích hợp và bổ sung — P1/P2.** Chạy main PASS với empty/single/repeated/all bytes/magic/payload formula/random SHA, bit I/O và input lỗi cơ bản. Chuyển JUnit như LZW.

Thêm oversubscribed/prefix-collision, length không hỗ trợ, bảng thiếu symbol, truncated payload, padding/trailing, empty header bất nhất, max size; thêm I/O mixed 1/8/12/31/32 bit qua nhiều refill, EOF và slice overflow. T10 hiện dùng lại `computeCodeLengths` và helper production để tính hai vế, rồi chỉ so `ceil(bits/8)` với byte count: có thể bỏ lọt sai khác dưới 1 byte. Nên lấy lengths trực tiếp từ header encoder, tự đếm frequency và parse đúng số bit payload đã tiêu thụ; đối chiếu tổng Σfreq×length cùng padding 0 và fixture canonical biết trước. Không gọi đây là kiểm chứng bit hoàn toàn độc lập ở trạng thái hiện tại.

### 4.5. `src/test/java/vn/csd/compression/RLECodecTest.java`

**Chưa hoàn thành — P1.** Chỉ in “Đang chờ TV1”, không gọi codec hay assert. Cài JUnit T01–T04, T07 (255/256/510/511), magic sai, count 0, payload lẻ, original length không khớp, length ngoài giới hạn và random binary/SHA. Test chính xác byte container/cặp count-value, không chỉ round-trip để tránh encoder/decoder cùng sai.

### 4.6. `src/test/java/vn/csd/compression/RoundTripTest.java`

**Chưa hoàn thành — P1.** Chỉ in thông báo; chưa test tích hợp codec nào. Dùng JUnit parameterized với RLE, Huffman, LZW, LZ4-12 và LZ4-16; kiểm length, bytes và SHA-256 cho empty, 1 byte, 256 byte, repeated/periodic/random nhiều size, input bất biến. Ca 64 MiB chạy tuần tự theo heap đủ và chính sách test nhóm. Thêm coverage cho pipeline benchmark/CSV hoặc tạo các test metrics riêng; không dùng test in console thay assertion.

## 5. Đối chiếu mục tiêu chung và phần còn thiếu

| Yêu cầu plan | Trạng thái tại review |
|---|---|
| 4 codec tự cài, round-trip và format | LZ4 ổn; Huffman/LZW cần validation; RLE chưa cài |
| Generator C1–C3 tái lập | Chưa cài |
| R1/R2/R3 + manifest nguồn/size/hash | Chưa có trong checkout, chưa có script/manifest để xác minh |
| Histogram T11, H0 RQ2 | Hàm đúng qua source review, thiếu test tự động và điều phối ma trận |
| Full unit/integration tests qua Maven | Chỉ TV5 thực sự chạy; hai suite manual ngoài Maven; hai suite khung |
| Phép đo thời gian riêng, gồm toàn header | Đã có ở cả hai runner |
| Peak heap phụ theo từng pha | Chưa đạt ở runner chung; runner riêng công bố chưa đo |
| Metadata params/statistics/environment | TV5 có phần riêng; CSV chung còn trống/thiếu |
| RQ1 đủ 135 cấu hình | Chưa có kết quả/driver đầy đủ |
| RQ2 ít nhất 36 cấu hình | Chưa có kết quả/driver đầy đủ |
| RQ3 140 cấu hình, trong đó 56 LZ4 | Chưa có corpus/ma trận đầy đủ; smoke không thay thế |
| Demo từ Main và README clone mới | Main còn banner; README thiếu CLI chung/dataset/full benchmark |

Tài liệu TV5 có các câu mô tả “chưa có ba codec còn lại/runner chung/generator tại lúc bàn giao”: đây là lịch sử ngày 21/10, không dùng làm mô tả snapshot hiện nay. Hiện đã có Huffman/LZW và khung runner chung, nhưng RLE/generator vẫn chưa có. Cập nhật thêm mục trạng thái hiện tại thay vì làm mất lịch sử kiểm chứng 18 test TV5. README nên ghi rõ test main phải chạy riêng cho đến khi chuyển sang JUnit. Pom hiện biên dịch source/target 1.8; chưa bảo đảm mọi API thuộc Java 8 như `--release 8`, và chưa thay thế việc chạy JVM 8 thực tế.

## 6. Các việc cần làm tiếp theo, theo thứ tự ưu tiên

1. **TV1:** hoàn thiện RLE + JUnit đúng container/run boundaries. **TV6:** hoàn thiện DataGenerator và manifest/script lấy đủ 12 Silesia, 11 Canterbury, C1–C3; hai đầu việc này chặn ma trận thí nghiệm.
2. **TV2:** sửa validation decoder LZW (không zero-fill thành công, không cắt chuỗi/bỏ mã), padding và limit encoder; regression test các phản ví dụ. **TV3:** kiểm bảng canonical/Kraft/giới hạn mã và kết thúc payload Huffman; sửa slice overflow BitReader.
3. **TV2/TV3/TV4/TV6:** chuyển test main sang JUnit, cài test RLE/RoundTrip và T11; thêm test metrics/CSV lỗi ghi, params LZ4, empty ratio, SHA full. Giữ nguyên hai suite TV5 đã PASS.
4. **TV4:** hoàn thiện runner chung: peak method có tài liệu sai số, params/statistics, empty ratio trống, SHA full, warm-up/repeats/iteration và environment. Sửa CsvWriter UTF-8/propagate errors/schema guard; chốt văn bản quy định chung trước khóa schema.
5. **TV4/TV6:** làm CLI điều phối/demo, thống nhất tên codec, máy/JVM/heap/seeds và chính sách dữ liệu lỗi/kết quả một phần. TV6 xác nhận endian block length với giảng viên; chưa cần sửa LZ4 đang đúng quy ước.
6. Chạy kiểm thử từ nhỏ đến 1–16 MiB rồi 64 MiB tuần tự, đặc biệt peak của LZW; ghi kết quả thực tế thay vì suy từ test nhỏ. Chạy đủ RQ1/RQ2/RQ3, kiểm đếm tổ hợp và SHA/manifest, sau đó mới tổng hợp biểu đồ và kết luận.
7. Cập nhật README/TV5 status, hướng dẫn dataset/CLI/JVM8, schema và cách tái lập; kiểm clone sạch và chuẩn bị báo cáo/slide/demo. Không dùng `BUILD SUCCESS` của 18 test TV5 để đánh dấu toàn dự án DONE.

**Các phần nên giữ nguyên:** interface Codec; thuật toán/container LZ4 và hai suite TV5; logic MSB-first của BitWriter; thuật toán ByteOrdering và công thức Entropy. Các chỉnh sửa còn lại tập trung vào phần thiếu, decoder validation, test coverage và tính hợp lệ của phép đo.
