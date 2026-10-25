package vn.csd.compression.dataset;
import java.util.Random;
/**
 * Công cụ sắp xếp/xáo trộn byte cho RQ2.
 * Phụ trách: TV4 (Cặp 2)
 */
public class ByteOrdering {
    // TODO (TV4): Cài đặt counting sort và Fisher-Yates
    // 1. Giữ nguyên (Original)
    public static byte[] original(byte[] input) {
        return input.clone();
    }

    // 2. Gom nhóm (Grouped) dùng Counting Sort để giữ nguyên hoàn toàn histogram
    public static byte[] grouped(byte[] input) {
        int[] freq = new int[256];
        for (byte b : input) {
            freq[b & 0xFF]++;
        }
        byte[] output = new byte[input.length];
        int idx = 0;
        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < freq[i]; j++) {
                output[idx++] = (byte) i;
            }
        }
        return output;
    }

    // 3. Xáo trộn (Shuffled) dùng thuật toán Fisher-Yates với seed cố định
    public static byte[] shuffled(byte[] input, long seed) {
        byte[] output = input.clone();
        Random rnd = new Random(seed);
        for (int i = output.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            byte a = output[index];
            output[index] = output[i];
            output[i] = a;
        }
        return output;
    }
}

