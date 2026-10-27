package vn.csd.compression;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vn.csd.compression.codec.LZWCodec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

/**
 * Bộ kiểm thử nghiệm thu cho LZWCodec theo yêu cầu Đề tài 4 (CSD201).
 * Chứa các ca biên bắt buộc: T01, T02, T03, T04, T05, T08, T09 và kiểm tra SHA-256.
 * Mỗi hàm đều có annotation @Test để hiển thị nút mũi tên xanh chạy riêng trong IDE.
 */
public class LZWCodecTest {

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
            Assertions.fail(message);
        } else {
            System.out.println("✅ PASSED: " + message);
        }
    }

    // T01: Input rỗng
    @Test
    public void testT01_EmptyInput() {
        LZWCodec codec = new LZWCodec();
        byte[] input = new byte[0];
        byte[] compressed = codec.compress(input);
        byte[] decompressed = codec.decompress(compressed);

        assertTrue(compressed.length == 12, "T01 - Header kích thước đúng 12 byte");
        assertTrue(decompressed.length == 0, "T01 - Input rỗng round-trip thành công");
    }

    // T02: 1 byte
    @Test
    public void testT02_SingleByte() {
        LZWCodec codec = new LZWCodec();
        byte[] input = new byte[]{(byte) 'X'};
        byte[] compressed = codec.compress(input);
        byte[] decompressed = codec.decompress(compressed);

        assertTrue(Arrays.equals(input, decompressed), "T02 - Input 1 byte round-trip chính xác");
    }

    // T03: 1 MiB cùng một byte
    @Test
    public void testT03_RepeatedByte() {
        LZWCodec codec = new LZWCodec();
        int size = 1024 * 1024; // 1 MiB
        byte[] input = new byte[size];
        Arrays.fill(input, (byte) 'A');

        byte[] compressed = codec.compress(input);
        byte[] decompressed = codec.decompress(compressed);

        double ratio = (double) compressed.length / input.length;
        System.out.printf("   [T03 Info] Gốc: %d bytes -> Nén: %d bytes (Tỷ lệ: %.4f)\n", input.length, compressed.length, ratio);

        assertTrue(Arrays.equals(input, decompressed), "T03 - 1 MiB cùng 1 byte giải nén khớp 100%");
        assertTrue(ratio < 0.05, "T03 - Dữ liệu lặp nén cực tốt (tỷ lệ < 5%)");
    }

    // T04: 256 giá trị byte, mỗi giá trị 1 lần
    @Test
    public void testT04_All256Bytes() {
        LZWCodec codec = new LZWCodec();
        byte[] input = new byte[256];
        for (int i = 0; i < 256; i++) {
            input[i] = (byte) i;
        }

        byte[] compressed = codec.compress(input);
        byte[] decompressed = codec.decompress(compressed);

        assertTrue(Arrays.equals(input, decompressed), "T04 - Đủ 256 byte khác nhau round-trip chính xác");
    }

    // T05: Chuỗi kinh điển ABABABA (Kiểm tra KwKwK)
    @Test
    public void testT05_KwKwK_ABABABA() {
        LZWCodec codec = new LZWCodec();
        byte[] input = "ABABABA".getBytes(StandardCharsets.US_ASCII);
        byte[] compressed = codec.compress(input);
        byte[] decompressed = codec.decompress(compressed);

        String resultStr = new String(decompressed, StandardCharsets.US_ASCII);
        assertTrue("ABABABA".equals(resultStr), "T05 - Ca biên KwKwK (ABABABA) giải nén ra đúng 'ABABABA'");
    }

    // T08: Input lớn làm đầy từ điển 4096 mục
    @Test
    public void testT08_DictionaryFull() {
        LZWCodec codec = new LZWCodec();
        Random rnd = new Random(42);
        byte[] input = new byte[100000];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte) ('a' + rnd.nextInt(26));
        }

        byte[] compressed = codec.compress(input);
        int fullAt = codec.getDictionaryFullAt();
        System.out.printf("   [T08 Info] Từ điển đầy tại byte thứ: %d / %d\n", fullAt, input.length);

        assertTrue(fullAt > 0 && fullAt < input.length, "T08 - Từ điển 4096 mục đã đạt trạng thái đầy và ghi nhận vị trí");

        byte[] decompressed = codec.decompress(compressed);
        assertTrue(Arrays.equals(input, decompressed), "T08 - Sau khi từ điển đóng băng, dữ liệu vẫn giải nén đúng 100%");
    }

    // T09: Magic sai -> Decoder từ chối
    @Test
    public void testT09_InvalidMagic() {
        LZWCodec codec = new LZWCodec();
        byte[] fakeData = new byte[16];
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

    // Test mở rộng: Dữ liệu ngẫu nhiên với SHA-256
    @Test
    public void testRandomData() {
        LZWCodec codec = new LZWCodec();
        Random rnd = new Random(12345);
        byte[] input = new byte[200000];
        rnd.nextBytes(input);

        String inputSha = sha256Hex(input);
        byte[] compressed = codec.compress(input);
        byte[] decompressed = codec.decompress(compressed);
        String decompressedSha = sha256Hex(decompressed);

        assertTrue(inputSha.equals(decompressedSha), "Random Test - SHA-256 trước và sau giải nén trùng khớp tuyệt đối!");
    }

    // Hàm main để có thể chạy trực tiếp bằng dòng lệnh hoặc file .bat
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  BẮT ĐẦU CHẠY BỘ TEST NGHIỆM THU CHO LZW CODEC");
        System.out.println("=================================================");

        LZWCodecTest test = new LZWCodecTest();
        test.testT01_EmptyInput();
        test.testT02_SingleByte();
        test.testT03_RepeatedByte();
        test.testT04_All256Bytes();
        test.testT05_KwKwK_ABABABA();
        test.testT08_DictionaryFull();
        test.testT09_InvalidMagic();
        test.testRandomData();

        System.out.println("=================================================");
        System.out.println("🎉 CHÚC MỪNG: TẤT CẢ CÁC CA KIỂM THỬ LZW ĐÃ PASS!");
        System.out.println("=================================================");
    }
}
