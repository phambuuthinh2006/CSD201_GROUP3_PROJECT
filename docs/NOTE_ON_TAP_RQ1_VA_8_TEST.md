# TONG HOP KIEN THUC ON TAP: RQ1 & 8 TEST LZW
**De tai:** So sanh cac thuat toan nen du lieu (CSD201 - Group 3)  
**Phu trach LZW:**  Bui Phat

---KỊCH BẢN 2: KHI DEMO ĐO THÍ NGHIỆM RQ1
1. Lời giới thiệu trước khi gõ lệnh:

"Tiếp theo, em xin phép demo chương trình chạy thực nghiệm tự động để đo đạc số liệu cho câu hỏi RQ1.
Để đảm bảo tính khoa học và công bằng giữa các lần đo, nhóm em thiết kế theo mô hình Runner - Worker gồm 2 class chính:

Class RQ1Runner.java: Đóng vai trò là bộ điều phối trung tâm.
Class RQ1Worker.java: Đóng vai trò là tiến trình thực thi từng phép đo riêng biệt."
2. Giới thiệu cách các class và hàm phối hợp chạy:

(Bạn mở file RQ1Runner.java):

"Cơ chế hoạt động của chương trình diễn ra như sau:

RQ1Runner sẽ chạy vòng lặp qua các cấu hình: 3 thuật toán (RLE, Huffman, LZW), 3 bộ dữ liệu (C1, C2, C3), các kích thước từ 1 đến 64 MiB và 3 seeds.
Với mỗi cấu hình, thay vì chạy chung trong một tiến trình làm rác bộ nhớ, RQ1Runner dùng ProcessBuilder để khởi chạy một tiến trình JVM hoàn toàn mới cho RQ1Worker.
Trong RQ1Worker:
Đầu tiên gọi DataGenerator sinh dữ liệu.
Thực hiện làm ấm JIT 3 lần (warm-up) để CPU đạt hiệu năng ổn định.
Sau đó gọi BenchmarkRunner.run() để đo: bấm giờ nano-giây trước/sau hàm compress và decompress, đo lượng RAM phụ trợ bằng heapAfter - heapBefore, và kiểm tra SHA-256.
RQ1Runner đồng thời áp dụng cơ chế Timeout 60 giây: nếu một lượt chạy quá 60s thì tự động ngắt để tránh treo hệ thống.
Toàn bộ dữ liệu đo được gom tự động vào file CSV chuẩn: results/raw/rq1.csv.

Bây giờ em xin phép chạy chế độ kiểm tra nhanh --quick trên Terminal để Thầy/Cô quan sát tiến trình đo thực tế."
## PHAN I: BAN CHAT 3 THUAT TOAN NEN
1. **RLE (Run-Length Encoding):** Meo dem so lan lap.
   - *Nguyen ly:* Gom chuoi lap `AAAAAA` -> `6A`.
   - *Uu diem:* Cuc nhanh, nen cuc tot chuoi lap dai (C1).
   - *Nhuoc diem:* Phinh to gap doi tren van ban (C2) va ngau nhien (C3) vi `ABC` -> `1A1B1C`.
2. **Huffman Canonical:** Meo chu hay gap dung it bit, chu hiem dung nhieu bit.
   - *Nguyen ly:* Dung cay nhi phan gan ma bit bien thien theo tan suat ky tu. Chay 2 luot (Two-pass).
   - *Uu diem:* Nen rat on dinh tren van ban tieng Anh (C2, tiet kiem ~43%).
   - *Nhuoc diem:* Cham nhat vi phai xu ly ghep tung bit nhi phan (Bit I/O). Bat luc tren C1 va C3.
3. **LZW 12-bit (Cua Phat):** Meo tu hoc cac cum tu lap vao tu dien 4.096 muc.
   - *Nguyen ly:* Khoi tao 256 byte don le. Gap cum tu moi thi cap ma moi tu 256 den 4095 (dung ma 12-bit). Cu 2 ma 12-bit (24 bit) ghep vua khit vao 3 byte.
   - *Dac ta de bai:* Khi tu dien du 4.096 muc thi **DONG BANG (Freeze)**, khong them muc moi de khong che bo nho an toan.

---

## PHAN II: 4 TIEU CHI DO CUA RQ1 TRONG FILE `rq1.csv`

### 1. Ty le nen (`ratio = compressed_bytes / input_bytes`)
- **Tap C1 (Chuoi lap):**
  - **RLE vo dich:** Ratio ~ 0.062 (giam 93.8%). Dung cong thuc ly thuyet 2 / 32 = 0.0625.
  - **LZW dung nhi:** Ratio ~ 0.200 (giam 80%).
  - **Huffman khong nen duoc:** Ratio ~ 1.000 (do tan suat cac byte trong C1 phan bo deu).
- **Tap C2 (Van ban Zipf):**
  - **LZW tot nhat:** Ratio ~ 0.400 (gom duoc cac cum tu tieng Anh lap lai).
  - **Huffman tot nhi:** Ratio ~ 0.570 (gan it bit cho cac chu cai pho bien nhu khoang trang, 'e', 't').
  - **RLE that bai:** Ratio ~ 1.940 (file no to gan gap doi).
- **Tap C3 (Ngau nhien Uniform):**
  - Ca 3 deu khong nen duoc (Ratio >= 1.0). 
  - *Ly thuyet Shannon:* Entropy H0 ~ 8.0 bit/byte (cuc dai), khong co tinh du thua nen khong the nen khong mat mat.

### 2. Thoi gian nen (`compress_ns` / `compress_mib_s`)
- Tang tuyen tinh O(N) theo kich thuoc du lieu.
- **RLE nhanh nhat:** > 1.000 MiB/s (chi duyet 1 vong lap dem).
- **LZW dung nhi:** 180 - 240 MiB/s (nho toi uu bang tra cuu mang phang `int[4096 * 256]` dat do phuc tap O(1), khong dung `HashMap`).
- **Huffman cham nhat:** 45 - 52 MiB/s (phai chay 2 luot va xu ly tung bit don le).

### 3. Thoi gian giai nen (`decompress_ns` / `decompress_mib_s`)
- Luon nhanh gap 2 den 3 lan thoi gian nen o ca 3 thuat toan.
- **RLE giai nen sieu toc:** > 1.200 MiB/s (chi doc so dem va ghi de mang byte).
- **LZW giai nen rat nhanh:** ~ 380 MiB/s (chi doc ma 12-bit va tra nguoc bang prefix, khong phai do tim nhu luc nen).
- **Huffman giai nen trung binh:** ~ 80 - 100 MiB/s.

### 4. Bo nho phu tro (`compress_heap_aux_bytes`)
- **Dinh nghia:** La luong RAM tang them de chua "hop do nghe" (cau truc du lieu phu tro) phuc vu tinh toan, khong tinh mang du lieu goc.
- **RLE:** Ton ~ 0 MB (chi dung vai bien dem nguyen thuy).
- **Huffman:** Ton co dinh ~ 5 - 6 MB (cay nhi phan 511 nut va bang tan suat 256 phan tu).
- **LZW:** Ton co dinh ~ 4 MB cho mang phang `int[4096 * 256]` (1.048.576 x 4 bytes).
- **Y nghia dong bang:** Khi dat 4.096 muc (tai byte thu 27.211 o C1, 11.107 o C2, 3.958 o C3), tu dien dong bang hoan toan. Nho do len toi 64 MiB bo nho van kiem soat o O(1), khong bao gio bi loi tran RAM (`OutOfMemoryError`).

### 5. Phuong phap do khoa hoc (Dam bao cong bang cac lan do)
- **Mo hinh Runner - Worker:** Moi lan do chay trong 1 tien trinh JVM rieng biet qua `ProcessBuilder`, do xong tat han de tranh rac RAM va JIT lam lech ket qua.
- **Warm-up 3 lan:** Cho chay nhap de lam am JIT truoc khi bam gio.
- **Timeout 60s:** Chong treo may neu gap file qua nang.

---

## PHAN III: 8 BAI TEST NGHIEM THU CUA LZW (`LZWCodecTest.java`)

1. **Test T01 - `testT01_EmptyInput` (File rong 0 byte):**
   - *Muc dich:* Tranh vang loi `IndexOutOfBoundsException` khi gap file trong.
   - *Xu ly trong LZW:* Nen chi xuat 12 byte Header (`LZW1` + 8 byte so 0); Giai nen thay do dai bang 0 thi tra ve ngay `new byte[0]`.
2. **Test T02 - `testT02_SingleByte` (File 1 byte duy nhat):**
   - *Muc dich:* Kiem tra xu ly dong goi ma bi le (Odd code count).
   - *Xu ly trong LZW:* Binh thuong 2 ma 12-bit ghep vao 3 byte. Khi chi co 1 ma le, code biet nhet 12 bit vao 2 byte (bu 4 bit 0 o duoi).
3. **Test T03 - `testT03_RepeatedByte` (1 MiB toan chu 'A'):**
   - *Muc dich:* Kiem tra hieu suat nen toi da tren du lieu lap lien tuc.
   - *Xu ly trong LZW:* Gom `A`, `AA`, `AAA`... Nen 1.048.576 byte xuong chi con **2.184 byte** (ty le 0.21%).
4. **Test T04 - `testT04_All256Bytes` (Du 256 gia tri byte tu 0 den 255):**
   - *Muc dich:* Tranh cai bay so am cua kieu `byte` trong Java (chay tu -128 den 127).
   - *Xu ly trong LZW:* Dung phep toan bitmask `& 0xFF` de bien byte am (nhu byte 255 mang gia tri -1) thanh so nguyen duong truoc khi tra mang tu dien, tranh vang loi chi so mang am.
5. **Test T05 - `testT05_KwKwK_ABABABA` (Ca bien kinh dien KwKwK):**
   - *Muc dich:* Xu ly truong hop chuoi co ky tu dau va cuoi giong nhau, ben Nen vua tao ma moi thi ben Giai nen nhan duoc ngay ma do khi tu dien chua kip cap nhat.
   - *Xu ly trong LZW:* Nhanh `else if (newCode == nextEntry)` nhan dien ca KwKwK, tu dong suy luan: `Chuoi moi = Chuoi cu + Ky tu dau cua chuoi cu`.
6. **Test T08 - `testT08_DictionaryFull` (Tu dien day 4.096 muc):**
   - *Muc dich:* Dam bao co che dong bang tu dien hoat dong chuan xac theo de tai.
   - *Xu ly trong LZW:* Khi `nextCode == 4096`, ngung them muc moi, chot vi tri `dictionaryFullAt` de ghi ra CSV, tiep tuc dung 4.096 tu co san de nen phan con lai ma khong lam tran mang.
7. **Test T09 - `testT09_InvalidMagic` (Header sai Magic):**
   - *Muc dich:* Bao ve an toan phan mem khi nguoi dung dua vao file hong hoac file la.
   - *Xu ly trong LZW:* Kiem tra 4 byte dau, neu khong phai `"LZW1"` (vi du `"BAD!"`), nem ngay ngoai le `IllegalArgumentException` de tu choi giai nen.
8. **Test Random - `testRandomData` (200.000 byte ngau nhien voi SHA-256):**
   - *Muc dich:* Bai kiem tra toan dien chung minh tinh Lossless tuyet doi 100%.
   - *Xu ly trong LZW:* Tinh ma bam SHA-256 cua 200.000 byte truoc khi nen va sau khi giai nen. Hai ma bam trung khop hoan toan -> Chung minh khong lech du chi 1 bit du lieu.

