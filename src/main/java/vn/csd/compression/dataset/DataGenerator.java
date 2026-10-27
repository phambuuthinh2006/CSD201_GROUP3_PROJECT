package vn.csd.compression.dataset;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Bộ sinh dữ liệu C1, C2, C3 phục vụ benchmark và nghiên cứu RQ1, RQ3.
 * Phụ trách: TV6 (Cặp 3 / Điều phối)
 *
 * Đặc tả theo đề bài:
 * - C1: Run dài; run length phân phối hình học, trung bình 32 byte, giá trị run đều 0–255.
 * - C2: 1.000 từ, mỗi từ 3–10 chữ thường; lấy mẫu theo Zipf s = 1, cách nhau dấu cách.
 * - C3: Byte gần ngẫu nhiên, phân phối đều 0–255.
 *
 * Mọi tập dữ liệu đều tái lập được hoàn toàn bằng seed cố định.
 */
public class DataGenerator {

    /**
     * Sinh tập dữ liệu C1 dạng byte array:
     * Run length phân phối hình học với kỳ vọng mean = 32.
     * Xác suất thành công p = 1.0 / 32.0.
     * Run length k >= 1 được tính bằng: floor(ln(1 - U) / ln(1 - p)) + 1 với U in [0, 1).
     * Giá trị mỗi run phân phối đều trong [0, 255].
     *
     * @param sizeBytes kích thước dữ liệu cần sinh (bytes)
     * @param seed      hạt giống ngẫu nhiên để tái lập
     * @return byte[] dữ liệu C1 đúng kích thước
     */
    public static byte[] generateC1(int sizeBytes, long seed) {
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("Kích thước không được âm: " + sizeBytes);
        }
        byte[] data = new byte[sizeBytes];
        if (sizeBytes == 0) {
            return data;
        }

        Random rnd = new Random(seed);
        final double p = 1.0 / 32.0;
        final double log1MinusP = Math.log(1.0 - p);

        int written = 0;
        while (written < sizeBytes) {
            // Lấy mẫu Geometric(p) với support k = 1, 2, 3, ...
            double u = rnd.nextDouble();
            if (u == 0.0) {
                u = 0.0000001;
            }
            int runLength = (int) (Math.log(1.0 - u) / log1MinusP) + 1;
            if (runLength <= 0) {
                runLength = 1;
            }

            int toWrite = Math.min(runLength, sizeBytes - written);
            byte runValue = (byte) rnd.nextInt(256);
            for (int i = 0; i < toWrite; i++) {
                data[written++] = runValue;
            }
        }
        return data;
    }

    /**
     * Sinh tập từ vựng cho C2 gồm exactly 1.000 từ, mỗi từ từ 3 đến 10 chữ cái thường [a-z].
     * Dùng seed phụ thuộc seed gốc để đảm bảo tái lập hoàn toàn.
     */
    public static byte[][] generateVocabulary(long vocabSeed) {
        final int VOCAB_SIZE = 1000;
        byte[][] vocab = new byte[VOCAB_SIZE][];
        Random rnd = new Random(vocabSeed);

        for (int i = 0; i < VOCAB_SIZE; i++) {
            int len = 3 + rnd.nextInt(8); // 3 đến 10 ký tự
            byte[] word = new byte[len];
            for (int j = 0; j < len; j++) {
                word[j] = (byte) ('a' + rnd.nextInt(26));
            }
            vocab[i] = word;
        }
        return vocab;
    }

    /**
     * Tính bảng CDF cho phân phối Zipf s = 1 trên 1.000 phần tử (rank 1 đến 1.000).
     * P(k) = (1 / k) / H_1000, với H_1000 = sum_{i=1..1000} (1 / i).
     */
    public static double[] computeZipfCdf(int n) {
        double[] cdf = new double[n];
        double harmonicSum = 0.0;
        for (int i = 1; i <= n; i++) {
            harmonicSum += 1.0 / i;
        }

        double accum = 0.0;
        for (int i = 0; i < n; i++) {
            accum += (1.0 / (i + 1)) / harmonicSum;
            cdf[i] = accum;
        }
        cdf[n - 1] = 1.0; // làm tròn chính xác mức 1.0
        return cdf;
    }

    /**
     * Lấy mẫu chỉ số từ (0 đến n - 1) theo phân phối Zipf bằng tìm kiếm nhị phân trên CDF.
     */
    private static int sampleZipf(double[] cdf, double u) {
        int low = 0;
        int high = cdf.length - 1;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (u <= cdf[mid]) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    /**
     * Sinh tập dữ liệu C2 dạng byte array:
     * 1.000 từ ngữ vựng (3–10 ký tự thường a-z), lấy mẫu Zipf s = 1,
     * các từ cách nhau bởi một dấu cách (byte ' ').
     *
     * @param sizeBytes kích thước dữ liệu cần sinh (bytes)
     * @param seed      hạt giống ngẫu nhiên để tái lập
     * @return byte[] dữ liệu C2 đúng kích thước
     */
    public static byte[] generateC2(int sizeBytes, long seed) {
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("Kích thước không được âm: " + sizeBytes);
        }
        byte[] data = new byte[sizeBytes];
        if (sizeBytes == 0) {
            return data;
        }

        Random rnd = new Random(seed);
        byte[][] vocab = generateVocabulary(seed ^ 0x5DEECE66DL);
        double[] zipfCdf = computeZipfCdf(vocab.length);

        int written = 0;
        boolean firstWord = true;

        while (written < sizeBytes) {
            if (!firstWord) {
                data[written++] = (byte) ' ';
                if (written >= sizeBytes) {
                    break;
                }
            } else {
                firstWord = false;
            }

            int wordIdx = sampleZipf(zipfCdf, rnd.nextDouble());
            byte[] word = vocab[wordIdx];

            int toWrite = Math.min(word.length, sizeBytes - written);
            System.arraycopy(word, 0, data, written, toWrite);
            written += toWrite;
        }

        return data;
    }

    /**
     * Sinh tập dữ liệu C3 dạng byte array:
     * Byte gần ngẫu nhiên, phân phối đều trong [0, 255].
     *
     * @param sizeBytes kích thước dữ liệu cần sinh (bytes)
     * @param seed      hạt giống ngẫu nhiên để tái lập
     * @return byte[] dữ liệu C3 đúng kích thước
     */
    public static byte[] generateC3(int sizeBytes, long seed) {
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("Kích thước không được âm: " + sizeBytes);
        }
        byte[] data = new byte[sizeBytes];
        if (sizeBytes == 0) {
            return data;
        }

        Random rnd = new Random(seed);
        rnd.nextBytes(data);
        return data;
    }

    /**
     * Sinh dữ liệu ra File theo cơ chế Stream/Buffer để tránh tốn Heap RAM
     * khi sinh kích thước lớn (ví dụ 64 MiB hoặc nhiều GB).
     *
     * @param type      "C1", "C2", hoặc "C3"
     * @param sizeBytes kích thước cần ghi
     * @param seed      seed ngẫu nhiên
     * @param destFile  file đích
     * @throws IOException nếu lỗi ghi file
     */
    public static void generateToFile(String type, long sizeBytes, long seed, File destFile) throws IOException {
        if (destFile.getParentFile() != null) {
            destFile.getParentFile().mkdirs();
        }

        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile), 64 * 1024)) {
            Random rnd = new Random(seed);
            long written = 0;

            if ("C1".equalsIgnoreCase(type)) {
                final double p = 1.0 / 32.0;
                final double log1MinusP = Math.log(1.0 - p);

                while (written < sizeBytes) {
                    double u = rnd.nextDouble();
                    if (u == 0.0) u = 0.0000001;
                    int runLength = (int) (Math.log(1.0 - u) / log1MinusP) + 1;
                    if (runLength <= 0) runLength = 1;

                    int toWrite = (int) Math.min((long) runLength, sizeBytes - written);
                    byte runValue = (byte) rnd.nextInt(256);
                    for (int i = 0; i < toWrite; i++) {
                        out.write(runValue);
                    }
                    written += toWrite;
                }
            } else if ("C2".equalsIgnoreCase(type)) {
                byte[][] vocab = generateVocabulary(seed ^ 0x5DEECE66DL);
                double[] zipfCdf = computeZipfCdf(vocab.length);
                boolean first = true;

                while (written < sizeBytes) {
                    if (!first) {
                        out.write((byte) ' ');
                        written++;
                        if (written >= sizeBytes) break;
                    } else {
                        first = false;
                    }

                    int wordIdx = sampleZipf(zipfCdf, rnd.nextDouble());
                    byte[] word = vocab[wordIdx];
                    int toWrite = (int) Math.min((long) word.length, sizeBytes - written);
                    out.write(word, 0, toWrite);
                    written += toWrite;
                }
            } else if ("C3".equalsIgnoreCase(type)) {
                byte[] buffer = new byte[64 * 1024];
                while (written < sizeBytes) {
                    int toWrite = (int) Math.min((long) buffer.length, sizeBytes - written);
                    rnd.nextBytes(buffer);
                    out.write(buffer, 0, toWrite);
                    written += toWrite;
                }
            } else {
                throw new IllegalArgumentException("Loại dữ liệu không hỗ trợ: " + type);
            }
        }
    }
}
