package vn.csd.compression;

import vn.csd.compression.codec.HuffmanCodec;
import vn.csd.compression.io.BitReader;
import vn.csd.compression.io.BitWriter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

/**
 * Bộ kiểm thử nghiệm thu toàn diện cho HuffmanCodec, BitWriter, BitReader.
 * Đáp ứng đầy đủ các ca biên theo đề bài Đề tài 4 (CSD201):
 * - T01: Input rỗng
 * - T02: 1 byte (Huffman mã 1 bit)
 * - T03: 1 MiB cùng một byteþ
 * - T04: 256 giá trị byte, mỗi giá trị đúng 1 lần
 * - T09: Magic sai bị từ chối
 * - T10: Huffman payload bits = Σ (frequency × codeLength)
 * - T12: Dữ liệu ngẫu nhiên với kiểm chứng SHA-256
 * 
 * Hỗ trợ chạy trực tiếp qua hàm main() bằng bất kỳ môi trường JDK 8/11/17+ mà không cần phụ thuộc thư viện ngoài.
 * Phụ trách: TV3 (Cặp 2)
 */
public class HuffmanCodecTest {

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            System.err.println("❌ FAILED: " + message);
            throw new AssertionError(message);
        } else {
            System.out.println("✅ PASSED: " + message);
        }
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            System.err.println("❌ FAILED: " + message + " (Expected: " + expected + ", Actual: " + actual + ")");
            throw new AssertionError(message + " (Expected: " + expected + ", Actual: " + actual + ")");
        } else {
            System.out.println("✅ PASSED: " + message);
        }
    }

    private static void assertArrayEquals(byte[] expected, byte[] actual, String message) {
        if (!Arrays.equals(expected, actual)) {
            System.err.println("❌ FAILED: " + message);
            throw new AssertionError(message);
        } else {
            System.out.println("✅ PASSED: " + message);
        }
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  BẮT ĐẦU CHẠY BỘ TEST NGHIỆM THU CHO HUFFMAN (TV3)");
        System.out.println("=================================================");

        HuffmanCodecTest testSuite = new HuffmanCodecTest();

        // T01: Input rỗng
        testSuite.testT01_EmptyInput();

        // T02: 1 byte (Huffman mã 1 bit)
        testSuite.testT02_SingleByte();

        // T03: 1 MiB cùng một byte
        testSuite.testT03_RepeatedByte();

        // T04: 256 giá trị byte, mỗi giá trị đúng 1 lần
        testSuite.testT04_All256Bytes();

        // T09: Magic sai bị từ chối
        testSuite.testT09_InvalidMagic();

        // T10: Huffman payload bits = Σ (frequency × codeLength)
        testSuite.testT10_PayloadBitsFormula();

        // T12: Dữ liệu ngẫu nhiên với kiểm chứng SHA-256
        testSuite.testT12_RandomDataRoundTrip();

        // Kiểm thử chuỗi văn bản UTF-8
        testSuite.testShortTextStrings();

        // Kiểm thử riêng rẽ BitWriter và BitReader
        testSuite.testBitWriterAndReader();

        // Kiểm thử xử lý dữ liệu lỗi và biên
        testSuite.testInvalidInputsAndBoundaries();

        System.out.println("=================================================");
        System.out.println("🎉 CHÚC MỪNG: TẤT CẢ CÁC BÀI TEST HUFFMAN ĐÃ PASS!");
        System.out.println("=================================================");
    }

    public void testT01_EmptyInput() {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] input = new byte[0];
        byte[] compressed = codec.compress(input);
        byte[] decompressed = codec.decompress(compressed);

        assertTrue(compressed.length == HuffmanCodec.HEADER_SIZE,
                "T01 - Header kích thước đúng chuẩn 268 byte (" + compressed.length + " bytes)");
        assertTrue(decompressed.length == 0,
                "T01 - Input rỗng round-trip thành công, trả về mảng 0 byte");
    }

    public void testT02_SingleByte() {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] input = new byte[]{(byte) 'Z'};
        byte[] compressed = codec.compress(input);
        byte[] decompressed = codec.decompress(compressed);

        assertArrayEquals(input, decompressed,
                "T02 - Input 1 byte round-trip chính xác 100%");
        // 268 byte header + 1 byte payload (1 bit '0' pad thành 1 byte) = 269 bytes
        assertTrue(compressed.length == 269,
                "T02 - File nén 1 byte có kích thước đúng 269 byte (gồm 1 bit padding thành 1 byte payload)");

        int[] codeLengths = codec.computeCodeLengths(input);
        assertTrue(codeLengths['Z'] == 1,
                "T02 - Ký hiệu đơn lẻ có độ dài mã chính xác là 1 bit");
    }

    public void testT03_RepeatedByte() {
        HuffmanCodec codec = new HuffmanCodec();
        int size = 1024 * 1024; // 1 MiB
        byte[] input = new byte[size];
        Arrays.fill(input, (byte) 'A');

        byte[] compressed = codec.compress(input);
        byte[] decompressed = codec.decompress(compressed);

        double ratio = (double) compressed.length / input.length;
        System.out.printf("   [T03 Info] Gốc: %d bytes -> Nén: %d bytes (Tỷ lệ: %.4f)\n",
                input.length, compressed.length, ratio);

        assertArrayEquals(input, decompressed,
                "T03 - 1 MiB cùng 1 byte giải nén khớp 100%");
        // Mỗi byte tốn 1 bit -> 1048576 bit = 131072 byte payload + 268 byte header = 131340 bytes
        assertTrue(compressed.length == 268 + (size / 8),
                "T03 - Kích thước nén đúng chuẩn lý thuyết: 268 header + 131072 payload");
        assertTrue(ratio < 0.13,
                "T03 - Dữ liệu 1 byte lặp nén đạt tỷ lệ ~12.5% (đúng 1 bit/byte)");
    }

    public void testT04_All256Bytes() {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] input = new byte[256];
        for (int i = 0; i < 256; i++) {
            input[i] = (byte) i;
        }

        byte[] compressed = codec.compress(input);
        byte[] decompressed = codec.decompress(compressed);

        assertArrayEquals(input, decompressed,
                "T04 - Đủ 256 byte khác nhau round-trip chính xác");

        int[] lengths = codec.computeCodeLengths(input);
        boolean all8Bits = true;
        for (int len : lengths) {
            if (len != 8) {
                all8Bits = false;
                break;
            }
        }
        assertTrue(all8Bits,
                "T04 - 256 ký hiệu có tần suất bằng nhau (1) đều được gán mã 8 bit");
    }

    public void testT09_InvalidMagic() {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] fakeData = new byte[HuffmanCodec.HEADER_SIZE];
        fakeData[0] = 'B';
        fakeData[1] = 'A';
        fakeData[2] = 'D';
        fakeData[3] = '!';

        boolean rejected = false;
        try {
            codec.decompress(fakeData);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }

        assertTrue(rejected, "T09 - Decoder từ chối magic sai hợp lệ");
    }

    public void testT10_PayloadBitsFormula() {
        HuffmanCodec codec = new HuffmanCodec();

        // Kiểm thử công thức T10 trên nhiều chuỗi văn bản mẫu
        String[] samples = new String[]{
                "ABRACADABRA",
                "TO BE OR NOT TO BE THAT IS THE QUESTION",
                "She sells seashells on the seashore",
                "CSD201 Lossless Compression Project Canonical Huffman by TV3"
        };

        for (String sample : samples) {
            byte[] input = sample.getBytes(StandardCharsets.UTF_8);

            long expectedBits = codec.calculateExpectedPayloadBits(input);

            // Đếm thủ công tần suất và code lengths độc lập
            long[] freq = new long[256];
            for (byte b : input) {
                freq[b & 0xFF]++;
            }
            int[] codeLengths = codec.computeCodeLengths(input);
            long manualSum = 0;
            for (int i = 0; i < 256; i++) {
                manualSum += freq[i] * codeLengths[i];
            }

            assertEquals(expectedBits, manualSum,
                    "T10 - Tính độc lập: expectedBits (" + expectedBits + ") == Σ(freq * codeLength) (" + manualSum + ")");

            // Kiểm tra số byte payload thực tế trong file nén
            byte[] compressed = codec.compress(input);
            int actualPayloadBytes = compressed.length - HuffmanCodec.HEADER_SIZE;
            int expectedPayloadBytes = (int) ((expectedBits + 7) / 8);

            assertEquals(expectedPayloadBytes, actualPayloadBytes,
                    "T10 - Kích thước payload byte (" + actualPayloadBytes + ") khớp chính xác trần số bit / 8 (" + expectedPayloadBytes + ")");
        }
    }

    public void testT12_RandomDataRoundTrip() {
        HuffmanCodec codec = new HuffmanCodec();
        Random rnd = new Random(20261021);

        // Test 1: 100.000 byte ngẫu nhiên
        byte[] input1 = new byte[100000];
        rnd.nextBytes(input1);
        String sha1Before = sha256Hex(input1);
        byte[] comp1 = codec.compress(input1);
        byte[] decomp1 = codec.decompress(comp1);
        String sha1After = sha256Hex(decomp1);

        assertTrue(sha1Before.equals(sha1After),
                "T12 - 100.000 byte random SHA-256 trước và sau giải nén trùng khớp tuyệt đối");

        // Test 2: 300.000 byte với phân bố không đều (tần suất khác nhau để Huffman nén hiệu quả)
        byte[] input2 = new byte[300000];
        for (int i = 0; i < input2.length; i++) {
            // 70% là chữ cái thường tiếng Anh (a-z), 30% là byte ngẫu nhiên
            if (rnd.nextInt(100) < 70) {
                input2[i] = (byte) ('a' + rnd.nextInt(26));
            } else {
                input2[i] = (byte) rnd.nextInt(256);
            }
        }
        String sha2Before = sha256Hex(input2);
        byte[] comp2 = codec.compress(input2);
        byte[] decomp2 = codec.decompress(comp2);
        String sha2After = sha256Hex(decomp2);

        assertTrue(sha2Before.equals(sha2After),
                "T12 - 300.000 byte văn bản mô phỏng SHA-256 trước và sau giải nén trùng khớp tuyệt đối");
        double ratio2 = (double) comp2.length / input2.length;
        System.out.printf("   [T12 Info] 300 KB văn bản mô phỏng: nén đạt tỷ lệ %.4f (tiết kiệm %.1f%%)\n",
                ratio2, (1.0 - ratio2) * 100);
        assertTrue(ratio2 < 0.85, "T12 - Văn bản mô phỏng nén tốt (< 85%)");
    }

    public void testShortTextStrings() {
        HuffmanCodec codec = new HuffmanCodec();
        String text = "Xin chao! Day la thuat toan nen Canonical Huffman do TV3 (Nhom 3 - CSD201) cai dat.";
        byte[] input = text.getBytes(StandardCharsets.UTF_8);

        byte[] compressed = codec.compress(input);
        byte[] restored = codec.decompress(compressed);

        String restoredText = new String(restored, StandardCharsets.UTF_8);
        assertTrue(text.equals(restoredText), "Chuỗi văn bản tiếng Việt UTF-8 round-trip chính xác 100%");
    }

    public void testBitWriterAndReader() {
        BitWriter writer = new BitWriter();
        writer.writeBit(1);                   // 1 bit: 1
        writer.writeBits(0b101, 3);           // 3 bit: 101
        writer.writeBits(0b110011, 6);        // 6 bit: 110011
        writer.writeBits(0xABCD, 16);         // 16 bit: 1010101111001101
        writer.writeBit(0);                   // 1 bit: 0
        // Tổng cộng: 1 + 3 + 6 + 16 + 1 = 27 bit
        assertTrue(writer.getTotalBitsWritten() == 27, "BitWriter ghi đúng 27 bit");

        byte[] bytes = writer.toByteArray();
        // 27 bit pad thành (27 + 7) / 8 = 4 byte
        assertTrue(bytes.length == 4, "27 bit được xả bù 0 thành 4 byte");

        BitReader reader = new BitReader(bytes);
        int bit1 = reader.readBit();
        int val3 = reader.readBits(3);
        int val6 = reader.readBits(6);
        int val16 = reader.readBits(16);
        int bitEnd = reader.readBit();

        assertTrue(bit1 == 1, "BitReader đọc bit 1 đúng");
        assertTrue(val3 == 0b101, "BitReader đọc 3 bit đúng");
        assertTrue(val6 == 0b110011, "BitReader đọc 6 bit đúng");
        assertTrue(val16 == 0xABCD, "BitReader đọc 16 bit đúng");
        assertTrue(bitEnd == 0, "BitReader đọc bit cuối đúng");
        assertTrue(reader.getTotalBitsRead() == 27, "BitReader đã đọc đúng 27 bit");
    }

    public void testInvalidInputsAndBoundaries() {
        HuffmanCodec codec = new HuffmanCodec();

        // Test null input
        boolean nullChecked = false;
        try {
            codec.compress(null);
        } catch (NullPointerException e) {
            nullChecked = true;
        }
        assertTrue(nullChecked, "Từ chối compress(null) bằng NullPointerException");

        // Test truncated compressed data
        boolean shortChecked = false;
        try {
            codec.decompress(new byte[100]); // nhỏ hơn 268 byte
        } catch (IllegalArgumentException e) {
            shortChecked = true;
        }
        assertTrue(shortChecked, "Từ chối compressed data < 268 bytes");
    }
}
