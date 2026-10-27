package vn.csd.compression;

import vn.csd.compression.codec.HuffmanCodec;
import vn.csd.compression.io.BitReader;
import vn.csd.compression.io.BitWriter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

/**
 * Bo kiem thu nghiem thu toan dien cho HuffmanCodec, BitWriter, BitReader.
 * Dap ung day du cac ca bien theo de bai De tai 4 (CSD201):
 * - T01: Input rong
 * - T02: 1 byte (Huffman ma 1 bit)
 * - T03: 1 MiB cung mot byte
 * - T04: 256 gia tri byte, moi gia tri dung 1 lan
 * - T09: Magic sai bi tu choi
 * - T10: Huffman payload bits = SUM (frequency x codeLength)
 * - T12: Du lieu ngau nhien voi kiem chung SHA-256
 * 
 * Ho tro chay truc tiep qua ham main() tren moi IDE (NetBeans, VS Code, IntelliJ)
 * khong bi loi font chu tieng Viet.
 * Phuc trach: TV3 (Cap 2)
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
            System.err.println("[FAIL] FAILED: " + message);
            throw new AssertionError(message);
        } else {
            System.out.println("[PASS] PASSED: " + message);
        }
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            System.err.println("[FAIL] FAILED: " + message + " (Expected: " + expected + ", Actual: " + actual + ")");
            throw new AssertionError(message + " (Expected: " + expected + ", Actual: " + actual + ")");
        } else {
            System.out.println("[PASS] PASSED: " + message);
        }
    }

    private static void assertArrayEquals(byte[] expected, byte[] actual, String message) {
        if (!Arrays.equals(expected, actual)) {
            System.err.println("[FAIL] FAILED: " + message);
            throw new AssertionError(message);
        } else {
            System.out.println("[PASS] PASSED: " + message);
        }
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  BAT DAU CHAY BO TEST NGHIEM THU CHO HUFFMAN (TV3)");
        System.out.println("=================================================");

        HuffmanCodecTest testSuite = new HuffmanCodecTest();

        // T01: Input rong
        testSuite.testT01_EmptyInput();

        // T02: 1 byte (Huffman ma 1 bit)
        testSuite.testT02_SingleByte();

        // T03: 1 MiB cung mot byte
        testSuite.testT03_RepeatedByte();

        // T04: 256 gia tri byte, moi gia tri dung 1 lan
        testSuite.testT04_All256Bytes();

        // T09: Magic sai bi tu choi
        testSuite.testT09_InvalidMagic();

        // T10: Huffman payload bits = SUM (frequency x codeLength)
        testSuite.testT10_PayloadBitsFormula();

        // T12: Du lieu ngau nhien voi kiem chung SHA-256
        testSuite.testT12_RandomDataRoundTrip();

        // Kiem thu chuoi van ban UTF-8
        testSuite.testShortTextStrings();

        // Kiem thu rieng re BitWriter va BitReader
        testSuite.testBitWriterAndReader();

        // Kiem thu xu ly du lieu loi va bien
        testSuite.testInvalidInputsAndBoundaries();

        System.out.println("=================================================");
        System.out.println(">>> CHUC MUNG: TAT CA CAC BAI TEST HUFFMAN DA PASS!");
        System.out.println("=================================================");
    }

    public void testT01_EmptyInput() {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] input = new byte[0];
        byte[] compressed = codec.compress(input);
        byte[] decompressed = codec.decompress(compressed);

        assertTrue(compressed.length == HuffmanCodec.HEADER_SIZE,
                "T01 - Header kich thuoc dung chuan 268 byte (" + compressed.length + " bytes)");
        assertTrue(decompressed.length == 0,
                "T01 - Input rong round-trip thanh cong, tra ve mang 0 byte");
    }

    public void testT02_SingleByte() {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] input = new byte[]{(byte) 'Z'};
        byte[] compressed = codec.compress(input);
        byte[] decompressed = codec.decompress(compressed);

        assertArrayEquals(input, decompressed,
                "T02 - Input 1 byte round-trip chinh xac 100%");
        // 268 byte header + 1 byte payload (1 bit '0' pad thanh 1 byte) = 269 bytes
        assertTrue(compressed.length == 269,
                "T02 - File nen 1 byte co kich thuoc dung 269 byte (gom 1 bit padding thanh 1 byte payload)");

        int[] codeLengths = codec.computeCodeLengths(input);
        assertTrue(codeLengths['Z'] == 1,
                "T02 - Ky hieu don le co do dai ma chinh xac la 1 bit");
    }

    public void testT03_RepeatedByte() {
        HuffmanCodec codec = new HuffmanCodec();
        int size = 1024 * 1024; // 1 MiB
        byte[] input = new byte[size];
        Arrays.fill(input, (byte) 'A');

        byte[] compressed = codec.compress(input);
        byte[] decompressed = codec.decompress(compressed);

        double ratio = (double) compressed.length / input.length;
        System.out.printf("   [T03 Info] Goc: %d bytes -> Nen: %d bytes (Ty le: %.4f)\n",
                input.length, compressed.length, ratio);

        assertArrayEquals(input, decompressed,
                "T03 - 1 MiB cung 1 byte giai nen khop 100%");
        // Moi byte ton 1 bit -> 1048576 bit = 131072 byte payload + 268 byte header = 131340 bytes
        assertTrue(compressed.length == 268 + (size / 8),
                "T03 - Kich thuoc nen dung chuan ly thuyet: 268 header + 131072 payload");
        assertTrue(ratio < 0.13,
                "T03 - Du lieu 1 byte lap nen dat ty le ~12.5% (dung 1 bit/byte)");
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
                "T04 - Du 256 byte khac nhau round-trip chinh xac");

        int[] lengths = codec.computeCodeLengths(input);
        boolean all8Bits = true;
        for (int len : lengths) {
            if (len != 8) {
                all8Bits = false;
                break;
            }
        }
        assertTrue(all8Bits,
                "T04 - 256 ky hieu co tan suat bang nhau (1) deu duoc gan ma 8 bit");
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

        assertTrue(rejected, "T09 - Decoder tu choi magic sai hop le");
    }

    public void testT10_PayloadBitsFormula() {
        HuffmanCodec codec = new HuffmanCodec();

        // Kiem thu cong thuc T10 tren nhieu chuoi van ban mau
        String[] samples = new String[]{
                "ABRACADABRA",
                "TO BE OR NOT TO BE THAT IS THE QUESTION",
                "She sells seashells on the seashore",
                "CSD201 Lossless Compression Project Canonical Huffman by TV3"
        };

        for (String sample : samples) {
            byte[] input = sample.getBytes(StandardCharsets.UTF_8);

            long expectedBits = codec.calculateExpectedPayloadBits(input);

            // Dem thu cong tan suat va code lengths doc lap
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
                    "T10 - Tinh doc lap: expectedBits (" + expectedBits + ") == SUM(freq * codeLength) (" + manualSum + ")");

            // Kiem tra so byte payload thuc te trong file nen
            byte[] compressed = codec.compress(input);
            int actualPayloadBytes = compressed.length - HuffmanCodec.HEADER_SIZE;
            int expectedPayloadBytes = (int) ((expectedBits + 7) / 8);

            assertEquals(expectedPayloadBytes, actualPayloadBytes,
                    "T10 - Kich thuoc payload byte (" + actualPayloadBytes + ") khop chinh xac tran so bit / 8 (" + expectedPayloadBytes + ")");
        }
    }

    public void testT12_RandomDataRoundTrip() {
        HuffmanCodec codec = new HuffmanCodec();
        Random rnd = new Random(20261021);

        // Test 1: 100.000 byte ngau nhien
        byte[] input1 = new byte[100000];
        rnd.nextBytes(input1);
        String sha1Before = sha256Hex(input1);
        byte[] comp1 = codec.compress(input1);
        byte[] decomp1 = codec.decompress(comp1);
        String sha1After = sha256Hex(decomp1);

        assertTrue(sha1Before.equals(sha1After),
                "T12 - 100.000 byte random SHA-256 truoc va sau giai nen trung khop tuyet doi");

        // Test 2: 300.000 byte voi phan bo khong deu (tan suat khac nhau de Huffman nen hieu qua)
        byte[] input2 = new byte[300000];
        for (int i = 0; i < input2.length; i++) {
            // 70% la chu cai thuong tieng Anh (a-z), 30% la byte ngau nhien
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
                "T12 - 300.000 byte van ban mo phong SHA-256 truoc va sau giai nen trung khop tuyet doi");
        double ratio2 = (double) comp2.length / input2.length;
        System.out.printf("   [T12 Info] 300 KB van ban mo phong: nen dat ty le %.4f (tiet kiem %.1f%%)\n",
                ratio2, (1.0 - ratio2) * 100);
        assertTrue(ratio2 < 0.85, "T12 - Van ban mo phong nen tot (< 85%)");
    }

    public void testShortTextStrings() {
        HuffmanCodec codec = new HuffmanCodec();
        String text = "Xin chao! Day la thuat toan nen Canonical Huffman do TV3 (Nhom 3 - CSD201) cai dat.";
        byte[] input = text.getBytes(StandardCharsets.UTF_8);

        byte[] compressed = codec.compress(input);
        byte[] restored = codec.decompress(compressed);

        String restoredText = new String(restored, StandardCharsets.UTF_8);
        assertTrue(text.equals(restoredText), "Chuoi van ban UTF-8 round-trip chinh xac 100%");
    }

    public void testBitWriterAndReader() {
        BitWriter writer = new BitWriter();
        writer.writeBit(1);                   // 1 bit: 1
        writer.writeBits(0b101, 3);           // 3 bit: 101
        writer.writeBits(0b110011, 6);        // 6 bit: 110011
        writer.writeBits(0xABCD, 16);         // 16 bit: 1010101111001101
        writer.writeBit(0);                   // 1 bit: 0
        // Tong cong: 1 + 3 + 6 + 16 + 1 = 27 bit
        assertTrue(writer.getTotalBitsWritten() == 27, "BitWriter ghi dung 27 bit");

        byte[] bytes = writer.toByteArray();
        // 27 bit pad thanh (27 + 7) / 8 = 4 byte
        assertTrue(bytes.length == 4, "27 bit duoc xa bu 0 thanh 4 byte");

        BitReader reader = new BitReader(bytes);
        int bit1 = reader.readBit();
        int val3 = reader.readBits(3);
        int val6 = reader.readBits(6);
        int val16 = reader.readBits(16);
        int bitEnd = reader.readBit();

        assertTrue(bit1 == 1, "BitReader doc bit 1 dung");
        assertTrue(val3 == 0b101, "BitReader doc 3 bit dung");
        assertTrue(val6 == 0b110011, "BitReader doc 6 bit dung");
        assertTrue(val16 == 0xABCD, "BitReader doc 16 bit dung");
        assertTrue(bitEnd == 0, "BitReader doc bit cuoi dung");
        assertTrue(reader.getTotalBitsRead() == 27, "BitReader da doc dung 27 bit");
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
        assertTrue(nullChecked, "Tu choi compress(null) bang NullPointerException");

        // Test truncated compressed data
        boolean shortChecked = false;
        try {
            codec.decompress(new byte[100]); // nho hon 268 byte
        } catch (IllegalArgumentException e) {
            shortChecked = true;
        }
        assertTrue(shortChecked, "Tu choi compressed data < 268 bytes");
    }
}
