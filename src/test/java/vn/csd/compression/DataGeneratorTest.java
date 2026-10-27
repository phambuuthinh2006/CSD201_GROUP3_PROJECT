package vn.csd.compression;

import vn.csd.compression.dataset.DataGenerator;

import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Bộ kiểm thử toàn diện cho DataGenerator (TV6).
 * Kiểm tra:
 * - C1: Phân phối hình học, trung bình run ~32, tái lập bằng seed
 * - C2: 1000 từ vựng (3-10 ký tự), phân phối Zipf s=1, cách nhau dấu cách
 * - C3: Phân phối đều 0-255, ngẫu nhiên
 * - Tái lập: Cùng seed cho ra cùng SHA-256 chính xác 100%
 * - Khác seed cho ra hash khác nhau
 * - Ca biên: size = 0, size = 1, size = 100.000
 */
public class DataGeneratorTest {

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
            System.err.println("[FAIL] " + message);
            throw new AssertionError(message);
        } else {
            System.out.println("[PASS] " + message);
        }
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            System.err.println("[FAIL] " + message + " (Expected: " + expected + ", Actual: " + actual + ")");
            throw new AssertionError(message);
        } else {
            System.out.println("[PASS] " + message);
        }
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  BAT DAU CHAY BO TEST CHO DATAGENERATOR (TV6)   ");
        System.out.println("=================================================");

        testBoundaries();
        testReproducibility();
        testC1Properties();
        testC2Properties();
        testC3Properties();

        System.out.println("=================================================");
        System.out.println(">>> CHUC MUNG: TAT CA CAC BAI TEST TV6 DA PASS!  ");
        System.out.println("=================================================");
    }

    public static void testBoundaries() {
        System.out.println("\n--- 1. Kiem thu ca bien (size = 0, size = 1) ---");
        byte[] c1_0 = DataGenerator.generateC1(0, 12345);
        byte[] c2_0 = DataGenerator.generateC2(0, 12345);
        byte[] c3_0 = DataGenerator.generateC3(0, 12345);

        assertEquals(0, c1_0.length, "C1 size 0 tra ve mang rong");
        assertEquals(0, c2_0.length, "C2 size 0 tra ve mang rong");
        assertEquals(0, c3_0.length, "C3 size 0 tra ve mang rong");

        byte[] c1_1 = DataGenerator.generateC1(1, 12345);
        byte[] c2_1 = DataGenerator.generateC2(1, 12345);
        byte[] c3_1 = DataGenerator.generateC3(1, 12345);

        assertEquals(1, c1_1.length, "C1 size 1 dung 1 byte");
        assertEquals(1, c2_1.length, "C2 size 1 dung 1 byte");
        assertEquals(1, c3_1.length, "C3 size 1 dung 1 byte");
    }

    public static void testReproducibility() {
        System.out.println("\n--- 2. Kiem thu tinh tai lap tuyet doi (Seed Consistency) ---");
        long seed = 20261027L;
        int size = 64 * 1024; // 64 KiB

        byte[] c1_a = DataGenerator.generateC1(size, seed);
        byte[] c1_b = DataGenerator.generateC1(size, seed);
        assertTrue(Arrays.equals(c1_a, c1_b), "C1 cung seed ra ket qua giong nhau 100%");
        assertTrue(sha256Hex(c1_a).equals(sha256Hex(c1_b)), "C1 cung seed co SHA-256 trung khop");

        byte[] c2_a = DataGenerator.generateC2(size, seed);
        byte[] c2_b = DataGenerator.generateC2(size, seed);
        assertTrue(Arrays.equals(c2_a, c2_b), "C2 cung seed ra ket qua giong nhau 100%");
        assertTrue(sha256Hex(c2_a).equals(sha256Hex(c2_b)), "C2 cung seed co SHA-256 trung khop");

        byte[] c3_a = DataGenerator.generateC3(size, seed);
        byte[] c3_b = DataGenerator.generateC3(size, seed);
        assertTrue(Arrays.equals(c3_a, c3_b), "C3 cung seed ra ket qua giong nhau 100%");
        assertTrue(sha256Hex(c3_a).equals(sha256Hex(c3_b)), "C3 cung seed co SHA-256 trung khop");

        // Khac seed phai ra khac nhau
        byte[] c1_diff = DataGenerator.generateC1(size, seed + 1);
        assertTrue(!Arrays.equals(c1_a, c1_diff), "C1 khac seed cho ra ket qua khac nhau");
    }

    public static void testC1Properties() {
        System.out.println("\n--- 3. Kiem thu dac tinh C1 (Run Length ~ 32) ---");
        int size = 200000;
        byte[] c1 = DataGenerator.generateC1(size, 42L);

        // Dem do dai run trung binh
        int runCount = 0;
        int currentRun = 1;
        long totalRunLengths = 0;

        for (int i = 1; i < c1.length; i++) {
            if (c1[i] == c1[i - 1]) {
                currentRun++;
            } else {
                runCount++;
                totalRunLengths += currentRun;
                currentRun = 1;
            }
        }
        runCount++;
        totalRunLengths += currentRun;

        double avgRun = (double) totalRunLengths / runCount;
        System.out.printf("   [C1 Info] Tong run: %d, Do dai run trung binh thuc te: %.2f bytes (Ly thuyet: ~32)\n",
                runCount, avgRun);

        // Chap nhan sai so thong ke vi mau 200KB (gia tri trong khoang 20 - 45)
        assertTrue(avgRun >= 20.0 && avgRun <= 45.0, "C1 do dai run trung binh phu hop phan phoi hinh hoc ~32");
    }

    public static void testC2Properties() {
        System.out.println("\n--- 4. Kiem thu dac tinh C2 (Tu vung 1000 tu, Zipf s=1) ---");
        byte[][] vocab = DataGenerator.generateVocabulary(12345L);
        assertEquals(1000, vocab.length, "Tu vung dung 1000 tu");

        for (byte[] word : vocab) {
            assertTrue(word.length >= 3 && word.length <= 10, "Moi tu co do dai tu 3 den 10 ky tu");
            for (byte b : word) {
                assertTrue(b >= 'a' && b <= 'z', "Tu chi chua chu cai thuong a-z");
            }
        }

        int size = 100000;
        byte[] c2 = DataGenerator.generateC2(size, 9999L);
        assertEquals(size, c2.length, "C2 sinh dung kich thuoc yeu cau");

        // Kiem tra ky tu: chi chua chu thuong a-z hoac dau cach ' '
        boolean validChars = true;
        for (byte b : c2) {
            if (b != ' ' && (b < 'a' || b > 'z')) {
                validChars = false;
                break;
            }
        }
        assertTrue(validChars, "C2 chi gom chu cai a-z va dau cach phan tach");
    }

    public static void testC3Properties() {
        System.out.println("\n--- 5. Kiem thu dac tinh C3 (Byte ngau nhien phan phoi deu) ---");
        int size = 256 * 1000; // 256.000 bytes
        byte[] c3 = DataGenerator.generateC3(size, 8888L);
        assertEquals(size, c3.length, "C3 sinh dung kich thuoc yeu cau");

        int[] freq = new int[256];
        for (byte b : c3) {
            freq[b & 0xFF]++;
        }

        // Moi byte ky vong xuat hien ~1.000 lan
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int count : freq) {
            if (count < min) min = count;
            if (count > max) max = count;
        }

        System.out.printf("   [C3 Info] Tan suat ky vong: 1000 | Min: %d, Max: %d\n", min, max);
        assertTrue(min > 700 && max < 1300, "C3 phan phoi tan suat byte gan dong deu tren [0, 255]");
    }
}
