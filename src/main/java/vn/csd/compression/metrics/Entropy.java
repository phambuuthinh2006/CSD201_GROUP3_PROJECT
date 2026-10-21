package vn.csd.compression.metrics;

/**
 * Tính Entropy bậc 0 (H0).
 * Phụ trách: TV4 (Cặp 2)
 */
public class Entropy {

    public static double calculateH0(byte[] data) {
        if (data == null || data.length == 0) {
            return 0.0;
        }

        long[] freq = new long[256];
        for (byte b : data) {
            freq[b & 0xFF]++;
        }

        double h0 = 0.0;
        double total = data.length;
        double log2 = Math.log(2);

        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                double p = freq[i] / total;
                h0 -= p * (Math.log(p) / log2);
            }
        }

        return h0;
    }
}
