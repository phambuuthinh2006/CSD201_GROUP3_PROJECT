package vn.csd.compression.codec;

import java.util.Arrays;

/**
 * Cài đặt thuật toán nén LZW (Lempel-Ziv-Welch) 12-bit theo đặc tả Đề tài 4 (CSD201).
 * 
 * Quy định kỹ thuật:
 * - Magic: ASCII "LZW1" (4 byte)
 * - Độ dài gốc: 8 byte big-endian
 * - Mã 12 bit, đóng gói bit cao trước (MSB-first)
 * - Từ điển 4.096 mục (0-255 là byte đơn, 256-4095 là chuỗi con ghép thêm)
 * - Khi từ điển đầy (4096 mục): ĐÓNG BĂNG, không thêm mục mới
 * - Encoder tra cứu bằng mảng int[4096 * 256], KHÔNG dùng HashMap
 * - Decoder xử lý trường hợp mã chưa có trong từ điển (chuỗi dạng KwKwK, ví dụ ABABABA)
 * - Ghi nhận vị trí byte input tại lúc từ điển đầy (lzw_dictionary_full_at)
 */
public class LZWCodec implements Codec {

    public static final byte[] MAGIC = new byte[]{'L', 'Z', 'W', '1'};
    public static final int DICTIONARY_SIZE = 4096;
    public static final int CLEAR_BYTE_COUNT = 256;

    // Lưu vị trí byte input tại thời điểm từ điển bị đầy ở lượt nén gần nhất (-1 nếu chưa đầy)
    private int dictionaryFullAt = -1;

    @Override
    public String getName() {
        return "lzw";
    }

    /**
     * Lấy vị trí byte input tại thời điểm từ điển đầy ở lượt nén gần nhất.
     * Dùng để ghi vào cột CSV lzw_dictionary_full_at.
     */
    public int getDictionaryFullAt() {
        return dictionaryFullAt;
    }

    @Override
    public byte[] compress(byte[] input) {
        dictionaryFullAt = -1;

        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }

        // Trường hợp input rỗng: chỉ ghi header 12 byte
        if (input.length == 0) {
            byte[] headerOnly = new byte[12];
            System.arraycopy(MAGIC, 0, headerOnly, 0, 4);
            // 8 byte tiếp theo là 0 (độ dài gốc = 0)
            return headerOnly;
        }

        // Bảng tra cứu phẳng theo yêu cầu đề bài: int[4096 * 256]
        // table[prefix * 256 + (nextByte & 0xFF)] = code
        int[] table = new int[DICTIONARY_SIZE * 256];
        Arrays.fill(table, -1);

        int nextCode = CLEAR_BYTE_COUNT; // Bắt đầu thêm từ 256

        // Bộ đệm lưu các mã 12-bit sinh ra
        int initialCap = Math.max(16, Math.min(input.length, 65536));
        int[] codes = new int[initialCap];
        int codeCount = 0;

        int prefix = input[0] & 0xFF;

        for (int i = 1; i < input.length; i++) {
            int nextByte = input[i] & 0xFF;
            int code = table[prefix * 256 + nextByte];

            if (code != -1) {
                // Đã có trong từ điển -> mở rộng chuỗi prefix
                prefix = code;
            } else {
                // Chưa có -> xuất mã của prefix hiện tại
                if (codeCount == codes.length) {
                    int newCap = codes.length < 1048576 ? codes.length * 2 : codes.length + (codes.length >> 1);
                    codes = Arrays.copyOf(codes, newCap);
                }
                codes[codeCount++] = prefix;

                // Thêm vào từ điển nếu chưa đầy
                if (nextCode < DICTIONARY_SIZE) {
                    table[prefix * 256 + nextByte] = nextCode;
                    nextCode++;
                    if (nextCode == DICTIONARY_SIZE && dictionaryFullAt == -1) {
                        dictionaryFullAt = i; // Vị trí byte input khi từ điển đạt 4096
                    }
                }

                prefix = nextByte;
            }
        }

        // Xuất mã prefix cuối cùng
        if (codeCount == codes.length) {
            codes = Arrays.copyOf(codes, codes.length + 1);
        }
        codes[codeCount++] = prefix;

        // Đóng gói các mã 12-bit thành byte stream (MSB-first)
        // 2 mã 12-bit (24 bit) = 3 byte
        // 1 mã lẻ cuối (12 bit) = 2 byte
        int numPairs = codeCount / 2;
        boolean hasOdd = (codeCount % 2 != 0);
        int payloadLen = numPairs * 3 + (hasOdd ? 2 : 0);

        byte[] output = new byte[12 + payloadLen];

        // 1. Ghi Magic (4 byte)
        System.arraycopy(MAGIC, 0, output, 0, 4);

        // 2. Ghi độ dài gốc 8 byte (Big-endian)
        long origLen = input.length;
        for (int i = 7; i >= 0; i--) {
            output[4 + i] = (byte) (origLen & 0xFF);
            origLen >>= 8;
        }

        // 3. Đóng gói mã 12-bit MSB-first vào payload
        int outPos = 12;
        for (int i = 0; i < codeCount; i += 2) {
            if (i + 1 < codeCount) {
                int c1 = codes[i];
                int c2 = codes[i + 1];
                output[outPos++] = (byte) ((c1 >> 4) & 0xFF);
                output[outPos++] = (byte) (((c1 & 0x0F) << 4) | ((c2 >> 8) & 0x0F));
                output[outPos++] = (byte) (c2 & 0xFF);
            } else {
                int c1 = codes[i];
                output[outPos++] = (byte) ((c1 >> 4) & 0xFF);
                output[outPos++] = (byte) ((c1 & 0x0F) << 4);
            }
        }

        return output;
    }

    @Override
    public byte[] decompress(byte[] compressed) {
        if (compressed == null || compressed.length < 12) {
            throw new IllegalArgumentException("Dữ liệu nén không hợp lệ: độ dài tối thiểu phải là 12 byte");
        }

        // 1. Kiểm tra Magic LZW1
        for (int i = 0; i < 4; i++) {
            if (compressed[i] != MAGIC[i]) {
                throw new IllegalArgumentException(String.format(
                        "Magic không hợp lệ: mong đợi LZW1, nhận được 0x%02X 0x%02X 0x%02X 0x%02X",
                        compressed[0], compressed[1], compressed[2], compressed[3]));
            }
        }

        // 2. Đọc độ dài gốc 8 byte (Big-endian)
        long origLenLong = 0;
        for (int i = 0; i < 8; i++) {
            origLenLong = (origLenLong << 8) | (compressed[4 + i] & 0xFFL);
        }

        if (origLenLong < 0 || origLenLong > 64L * 1024 * 1024) {
            throw new IllegalArgumentException("Độ dài gốc vượt quá giới hạn hoặc không hợp lệ: " + origLenLong);
        }

        int origLen = (int) origLenLong;
        if (origLen == 0) {
            return new byte[0];
        }

        // 3. Giải gói các mã 12-bit từ payload
        int payloadLen = compressed.length - 12;
        int numPairs = payloadLen / 3;
        int remainder = payloadLen % 3;

        if (remainder == 1) {
            throw new IllegalArgumentException("Payload LZW 12-bit bị hỏng (độ dài byte dư 1 không hợp lệ)");
        }

        int totalCodes = numPairs * 2 + (remainder == 2 ? 1 : 0);
        int[] codes = new int[totalCodes];
        int codeIdx = 0;
        int inPos = 12;

        for (int i = 0; i < numPairs; i++) {
            int b0 = compressed[inPos++] & 0xFF;
            int b1 = compressed[inPos++] & 0xFF;
            int b2 = compressed[inPos++] & 0xFF;
            codes[codeIdx++] = (b0 << 4) | (b1 >> 4);
            codes[codeIdx++] = ((b1 & 0x0F) << 8) | b2;
        }

        if (remainder == 2) {
            int b0 = compressed[inPos++] & 0xFF;
            int b1 = compressed[inPos++] & 0xFF;
            codes[codeIdx++] = (b0 << 4) | (b1 >> 4);
        }

        if (totalCodes == 0) {
            throw new IllegalStateException("Dữ liệu nén không chứa mã nào nhưng độ dài gốc > 0");
        }

        // 4. Giải nén bằng từ điển 4096 mục
        byte[] output = new byte[origLen];
        int outOffset = 0;

        int[] prefix = new int[DICTIONARY_SIZE];
        byte[] character = new byte[DICTIONARY_SIZE];
        int[] length = new int[DICTIONARY_SIZE];
        byte[] firstChar = new byte[DICTIONARY_SIZE];

        // Khởi tạo 256 byte đơn
        for (int i = 0; i < CLEAR_BYTE_COUNT; i++) {
            prefix[i] = -1;
            character[i] = (byte) i;
            length[i] = 1;
            firstChar[i] = (byte) i;
        }

        int nextEntry = CLEAR_BYTE_COUNT;

        // Mã đầu tiên
        int oldCode = codes[0];
        if (oldCode < 0 || oldCode >= CLEAR_BYTE_COUNT) {
            throw new IllegalStateException("Mã đầu tiên vượt ngoài phạm vi khởi tạo: " + oldCode);
        }

        output[outOffset++] = character[oldCode];

        for (int i = 1; i < totalCodes; i++) {
            if (outOffset >= origLen) {
                break;
            }

            int newCode = codes[i];
            byte c;
            boolean isKwKwK = false;

            if (newCode < nextEntry) {
                // Đã có trong từ điển
                c = firstChar[newCode];
            } else if (newCode == nextEntry) {
                // Ca biên KwKwK: chuỗi là chuỗi của oldCode + ký tự đầu của oldCode
                c = firstChar[oldCode];
                isKwKwK = true;
            } else {
                throw new IllegalStateException(String.format(
                        "Mã LZW không hợp lệ: %d (nextEntry hiện tại là %d)", newCode, nextEntry));
            }

            // Thêm mục mới vào từ điển nếu chưa đầy
            if (nextEntry < DICTIONARY_SIZE) {
                prefix[nextEntry] = oldCode;
                character[nextEntry] = c;
                length[nextEntry] = length[oldCode] + 1;
                firstChar[nextEntry] = firstChar[oldCode];
                nextEntry++;
            }

            // Xuất chuỗi tương ứng với newCode ra output
            int len = isKwKwK ? (length[oldCode] + 1) : length[newCode];
            if (outOffset + len > origLen) {
                len = origLen - outOffset; // Bảo vệ biên nếu có padding bit
            }

            int pos = outOffset + len - 1;
            if (isKwKwK) {
                if (pos >= outOffset) {
                    output[pos--] = c;
                }
                int curr = oldCode;
                while (curr >= 0 && pos >= outOffset) {
                    output[pos--] = character[curr];
                    curr = prefix[curr];
                }
            } else {
                int curr = newCode;
                while (curr >= 0 && pos >= outOffset) {
                    output[pos--] = character[curr];
                    curr = prefix[curr];
                }
            }

            outOffset += len;
            oldCode = newCode;
        }

        return output;
    }
}
