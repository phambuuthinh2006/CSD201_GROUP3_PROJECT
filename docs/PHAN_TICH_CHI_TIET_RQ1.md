# BAO CAO PHAN TICH CHI TIET KET QUA THUC NGHIEM RQ1
**De tai:** So sanh 4 thuat toan nen du lieu (CSD201 - Group 3)  
**Cau hoi nghien cuu RQ1:** RLE, Huffman va LZW khac nhau the nao ve Ty le nen, Thoi gian nen, Thoi gian giai nen va Bo nho khi kich thuoc du lieu tang tu 1 MiB den 64 MiB?

---

## I. TONG QUAN MA TRAN KET QUA
- **Tong so luot chay:** 135 luot (3 Codecs x 3 Datasets C1, C2, C3 x 5 Kich thuoc 1, 4, 16, 32, 64 MiB x 3 Seeds).
- **Tinh toan ven (Lossless):** 100% cac luot do deu dat **PASS**, chuoi bam SHA-256 truoc va sau giai nen trung khop hoan toan.

---

## II. PHAN TICH CHI TIET THEO 4 TIEU CHI

### 1. TIEU CHI 1: TY LE NEN (Compression Ratio = compressed_bytes / input_bytes)
*Ty le cang nho thi nen cang tot; ty le > 1.0 tuc la bi no dung luong.*

#### A. Tren tap du lieu C1 (Chuoi byte lap lien tuc - Run-Length):
- **RLE nen tot nhat:** Ty le nen dat **0.0618 den 0.0624 (khoang 6.2%)**.
  - *So lieu thuc te:* O 1 MiB (1.048.576 bytes), RLE nen xuong con **64.886 bytes**; o 4 MiB nen con **260.852 bytes**.
  - *Giai thich:* C1 duoc sinh voi do dai lap trung binh 32 bytes. Cu moi 32 bytes giong nhau, RLE chi can dung 2 bytes (1 byte dem + 1 byte gia tri) de luu. Ty le ly thuyet la 2 / 32 = 0.0625. So lieu thuc te khop hoan toan voi ly thuyet.
- **LZW dung thu hai:** Ty le nen dat **0.200 den 0.202 (khoang 20.1%)**.
  - *So lieu thuc te:* O 1 MiB con **211.943 bytes**; o 4 MiB con **846.381 bytes**.
  - *Giai thich:* LZW cung gom cac chuoi lap nhung dung ma 12-bit (1.5 byte). Do tu dien gioi han 4096 muc nen tren chuoi lap dai thuan tuy, LZW xep sau RLE.
- **Huffman khong nen duoc:** Ty le nen la **1.0002 (100.02%)**.
  - *So lieu thuc te:* O 1 MiB sau khi nen nang **1.048.844 bytes** (to hon file goc dung 268 bytes Header).
  - *Giai thich:* Xet tren toan bo file, tan suat xuat hien cua 256 ky tu la dong deu. Huffman bat buoc gan toi thieu 1 bit/ky tu va phai ganh them 268 bytes Header nen file bi no ra nhe.

#### B. Tren tap du lieu C2 (Van ban tieng Anh mo phong luat Zipf):
- **LZW nen tot nhat:** Ty le nen dat **0.400 (40.0%)**.
  - *So lieu thuc te:* O 1 MiB nen con **420.473 bytes**; o 4 MiB nen con **1.676.984 bytes**.
  - *Giai thich:* Van ban tieng Anh chua nhieu tu va cum tu lap lai (nhu "the", "ing", "tion"...). LZW nhanh chong hoc va dua cac cum tu nay vao tu dien 4096 muc va thay the ca cum bang 1 ma 12-bit.
- **Huffman nen tot thu nhi:** Ty le nen dat **0.572 den 0.577 (khoang 57.5%)**.
  - *So lieu thuc te:* O 1 MiB nen con **605.055 bytes**; o 4 MiB nen con **2.419.281 bytes**.
  - *Giai thich:* Quy luat Zipf lam cho cac ky tu nhu dau cach, 'e', 't', 'a' xuat hien ap dao. Huffman gan cho cac ky tu nay ma bit rat ngan (3 den 4 bit thay vi 8 bit) nen tiet kiem duoc hon 42% dung luong.
- **RLE that bai hoan toan:** Ty le nen la **1.941 (194.1%) - File nen bi no gan gap doi!**
  - *So lieu thuc te:* 1 MiB sau khi nen phinh len **2.035.622 bytes**.
  - *Giai thich:* Trong van ban, cac chu cai dung canh nhau hau nhu la khac nhau. Cu moi 1 ky tu don le, RLE phai ghi 1 byte dem + 1 byte gia tri, bien 1 byte goc thanh 2 byte nen.

#### C. Tren tap du lieu C3 (Du lieu phan bo deu ngau nhien - Uniform Random):
- **Ca 3 thuat toan deu khong nen duoc:**
  - Huffman: Ty le = 1.000256 (1.048.844 bytes)
  - LZW: Ty le = 1.419 (1.488.351 bytes)
  - RLE: Ty le = 1.992 (2.088.970 bytes)
- *Giai thich:* C3 co do hon loan Entropy do duoc la **7.9998 bit/byte** (Entropy cuc dai xap xi 8). Theo dinh ly thong tin cua Shannon, du lieu khong co tinh du thua thi khong the nen khong mat mat. LZW bi no khoang 42% vi moi byte don le bi bien thanh ma 12-bit (1.5 byte).

---

### 2. TIEU CHI 2: THOI GIAN NEN VA THONG LUONG NEN (compress_ns / compress_mib_s)
*Thoi gian nen cua ca 3 thuat toan deu tang tuyen tinh O(N) theo kich thuoc.*

1. **RLE nhanh nhat tuyet doi:**
   - Tren C1 (1 MiB): nen chi mat **0.6 den 0.9 mili-giay** (615.000 - 919.200 ns).
   - Thong luong dat toi **1.087 den 1.626 MiB/s**.
   - *Ly do:* RLE chi can 1 vong lap don gian quet tu dau den cuoi mang de dem so lan lap.
2. **LZW dung thu hai (Toc do rat on dinh):**
   - Tren C1 (1 MiB): nen mat **5.0 den 7.5 mili-giay**, thong luong **131 den 198 MiB/s**.
   - Tren C2 (1 MiB): nen mat **8.8 den 8.9 mili-giay**, thong luong **111 den 112 MiB/s**.
   - *Diem sang ky thuat:* Nhom da toi uu tra cuu tu dien bang mang phang `int[4096 * 256]` (do phuc tap O(1)) thay vi dung `HashMap`, nho do toc do nen cua LZW duy tri rat cao.
3. **Huffman cham nhat:**
   - Tren C1 (1 MiB): nen mat **18.9 den 21.8 mili-giay**, thong luong **45 den 52 MiB/s**.
   - *Ly do:* Huffman phai chay 2 luot (Luot 1 dem tan suat dung cay, Luot 2 quet lai du lieu ghi tung bit). Xu ly tung bit (Bit-level I/O) lam ton nhieu chu ky CPU nhat.

---

### 3. TIEU CHI 3: THOI GIAN GIAI NEN (decompress_ns / decompress_mib_s)
*Toc do giai nen luon nhanh gap 2 den 3 lan so voi toc do nen:*

- **RLE giai nen sieu toc:** Tren C1 (1 MiB), thoi gian giai nen chi mat **0.7 den 1.2 mili-giay**, thong luong dat **772 den 1.412 MiB/s**. Thao tac giai nen chi la doc so dem va ghi de byte.
- **LZW giai nen rat nhanh:** Chi mat **2.3 den 2.8 mili-giay** (nhanh gap doi luc nen), thong luong dat **351 den 430 MiB/s**. Khi giai nen, LZW chi can doc ma 12-bit roi tra nguoc mang prefix co san.
- **Huffman giai nen trung binh:** Mat **9.7 den 16.2 mili-giay** (thong luong **61 den 102 MiB/s**), nhanh hon luc nen vi bang ma da co san trong Header 268 bytes.

---

### 4. TIEU CHI 4: BO NHO PHU TRO (Heap Auxiliary Bytes)
*Chung minh bo nho khong bi phinh to khi kich thuoc du lieu tang len 64 MiB:*

1. **RLE:** Bo nho phu tro bang **0 bytes**. Khong can cau truc du lieu nao ngoai vai bien dem nguyen thuy. Do phuc tap bo nho la O(1).
2. **Huffman:** Bo nho phu tro co dinh khoang **5 den 6 MB** (cho mang tan suat 256 phan tu, mang cay nhi phan 511 nut va buffer BitWriter). Bo nho nay **khong doi** du nen file 1 MiB hay 64 MiB.
3. **LZW:** Bo nho phu tro chiem khoang **8 den 9 MB**.
   - Dung cho bang tra tu dien phang `int[4096 * 256]` (chiem 4 MB) va mang dem `codes[]`.
   - **Chung minh tu cot lzw_dictionary_full_at:**
     - Tren C1: Tu dien day 4096 muc tai byte thu **27.211**.
     - Tren C2: Tu dien day tai byte thu **10.567 den 11.107**.
     - Tren C3: Tu dien day rat som tai byte thu **3.950 den 3.962**.
   - *Y nghia:* Ngay khi dat 4096 muc, **tu dien LZW dong bang (freeze)**, khong cap phat them bat ky bo nho nao nua. Do do, o cac file lon 16 MiB, 32 MiB hay 64 MiB, LZW hoan toan an toan va khong bao gio bi loi tran bo nho (OutOfMemoryError).

---

## III. BANG TONG KET SO SANH NHANH

| Tieu chi | RLE (Run-Length) | LZW (12-bit freeze) | Huffman Canonical |
|---|---|---|---|
| **So truong nen** | Chuoi lap dai (C1: ratio ~ 0.06) | Van ban cum tu lap (C2: ratio ~ 0.40) | Van ban tan suat lech (C2: ratio ~ 0.57) |
| **So doan nen** | Van ban & ngau nhien (ratio ~ 1.94) | Du lieu ngau nhien (ratio ~ 1.41) | Chuoi lap dong nhat (ratio ~ 1.00) |
| **Toc do nen** | **Nhanh nhat** (> 1.000 MiB/s) | **Nhanh** (110 - 200 MiB/s) | **Cham nhat** (45 - 52 MiB/s) |
| **Toc do giai nen** | **Vo dich** (> 1.000 MiB/s) | **Rat nhanh** (350 - 430 MiB/s) | **Trung binh** (60 - 100 MiB/s) |
| **Bo nho Heap** | O(1) ~ 0 MB | O(1) co dinh ~ 4 den 8 MB | O(1) co dinh ~ 5 den 6 MB |

---

## IV. CAU CHOT DE KET LUAN BAI THUYET TRINH
> "Thua Thay/Co, qua so lieu thuc nghiem 135 cau hinh cua RQ1:
> 1. **Ve ty le nen:** LZW vo dich tren chuoi lap (C1), Huffman on dinh tren van ban (C2), ca 3 deu khong nen duoc du lieu ngau nhien (C3).
> 2. **Ve thoi gian:** RLE nhanh nhat ca nen va giai nen; Huffman cham nhat do thao tac bit I/O; thoi gian cua ca 3 deu tang tuyen tinh O(N).
> 3. **Ve bo nho:** Ca 3 thuat toan deu kiem soat bo nho phu tro o muc O(1) rat an toan, dac biet nho co che dong bang 4096 muc cua LZW ma bo nho khong he bi phinh to khi du lieu tang len 64 MiB."
