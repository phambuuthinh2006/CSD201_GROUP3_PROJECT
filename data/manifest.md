# DATASET MANIFEST (CSD201 - DE TAI 4)
**Phu trach:** TV6 (Cap 3 / Dieu phoi)

Manifest nay quan ly toan bo tap du lieu phuc vu benchmark va nghien cuu cac cau hoi RQ1, RQ2, RQ3.

---

## 1. DU LIEU THUC TE (REAL DATASETS)

### R1 (RQ2): 3 tap tin tu Silesia Corpus
Dung de phan tich anh huong cua thu tu byte (Original, Grouped, Shuffled) len Entropy $H_0$ va ty le nen.

| File Name | Source URL | Mo ta | Kich thuoc goc | SHA-256 Checksum |
|---|---|---|---|---|
| `dickens` | Silesia Corpus | Tac pham van hoc Dickens (Plain Text) | 10,192,446 bytes | *(Dien sau khi fetch)* |
| `xml` | Silesia Corpus | Tai lieu dinh dang XML lon | 5,345,280 bytes | *(Dien sau khi fetch)* |
| `mozilla` | Silesia Corpus | File thuc thi tar archive / binary | 51,220,480 bytes | *(Dien sau khi fetch)* |

### R2 (RQ3): Toan bo 12 tap tin Silesia Corpus
Dung de so sanh toan dien LZ4 vs LZW tren tap corpus chuan.

1. `dickens` (10,192,446 B)
2. `mozilla` (51,220,480 B)
3. `mr` (9,970,564 B)
4. `nci` (33,553,445 B)
5. `ooffice` (6,152,192 B)
6. `osdb` (10,085,684 B)
7. `reymont` (6,627,202 B)
8. `samba` (21,606,400 B)
9. `sao` (7,251,944 B)
10. `webster` (41,458,703 B)
11. `xml` (5,345,280 B)
12. `x-ray` (8,474,240 B)

### R3 (RQ3): 11 tap tin Canterbury Corpus
Tap du lieu gom cac file nho de quan sat header/codebook overhead.

1. `alice29.txt` (152,089 B)
2. `asyoulik.txt` (125,179 B)
3. `cp.html` (24,603 B)
4. `fields.c` (11,150 B)
5. `grammar.lsp` (3,721 B)
6. `kennedy.xls` (1,029,744 B)
7. `lcet10.txt` (426,754 B)
8. `plrabn12.txt` (481,861 B)
9. `ptt5` (513,216 B)
10. `sum` (38,240 B)
11. `xargs.1` (4,227 B)

---

## 2. DU LIEU SINH (GENERATED DATASETS C1, C2, C3)
Duoc sinh boi `vn.csd.compression.dataset.DataGenerator`.

* **C1 (Run-length):** Phân phối hình học trung bình run 32 bytes, byte phân phối đều 0–255.
* **C2 (Zipf Text):** 1.000 từ vựng tiếng Anh thường (3–10 ký tự), lấy mẫu Zipf $s = 1$, cách nhau dấu cách.
* **C3 (Uniform Random):** Byte phân phối đều hoàn toàn 0–255.

### Kich thuoc dung cho RQ1:
* Kích thước: **1 MiB, 4 MiB, 16 MiB, 32 MiB, 64 MiB**.
* Hạt giống (Seed) mặc định: `20261020`, `20261021`, `20261022` (3 seeds).
