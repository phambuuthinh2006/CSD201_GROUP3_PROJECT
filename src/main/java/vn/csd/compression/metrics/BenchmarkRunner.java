
package vn.csd.compression.metrics;

import vn.csd.compression.codec.Codec;
import vn.csd.compression.codec.LZWCodec;
import vn.csd.compression.codec.LZ4Codec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;

public class BenchmarkRunner {

    public static BenchmarkResult run(
            Codec codec,
            byte[] input,
            String rqName,
            String datasetName,
            String fileName,
            String variant,
            long seed) {

        if (codec == null || input == null) {
            throw new IllegalArgumentException("Codec và input không được null");
        }

        // Đo bộ nhớ phụ trợ theo heap đang sử dụng trước/sau.
        // Đây KHÔNG phải heap peak thực tế.
        System.gc();

        Runtime runtime = Runtime.getRuntime();
        long heapBeforeComp = runtime.totalMemory() - runtime.freeMemory();

        long startComp = System.nanoTime();
        byte[] compressed = codec.compress(input);
        long endComp = System.nanoTime();

        long heapAfterComp = runtime.totalMemory() - runtime.freeMemory();
        long compHeapAux = Math.max(0L, heapAfterComp - heapBeforeComp);

        long heapBeforeDecomp = runtime.totalMemory() - runtime.freeMemory();

        long startDecomp = System.nanoTime();
        byte[] decompressed = codec.decompress(compressed);
        long endDecomp = System.nanoTime();

        long heapAfterDecomp = runtime.totalMemory() - runtime.freeMemory();
        long decompHeapAux = Math.max(0L, heapAfterDecomp - heapBeforeDecomp);

        // Tính hash và kiểm tra kết quả ngoài thời gian đo codec.
        String inputHash = getSha256Hex(input);
        String outputHash = getSha256Hex(decompressed);
        boolean isMatch = Arrays.equals(input, decompressed)
                && inputHash.equals(outputHash);

        String outputCheck = isMatch
                ? "PASS:" + inputHash.substring(0, 16)
                : "FAIL:in=" + inputHash.substring(0, 16)
                    + ";out=" + outputHash.substring(0, 16);

        long compNs = endComp - startComp;
        long decompNs = endDecomp - startDecomp;

        // Input rỗng: ratio để trống, tránh chia cho 0.
        String ratio = input.length == 0
                ? ""
                : String.format(
                    Locale.ROOT,
                    "%.6f",
                    (double) compressed.length / input.length
                );

        double inputMiB = input.length / (1024.0 * 1024.0);

        double compMiBPerSec = compNs > 0
                ? inputMiB / (compNs / 1_000_000_000.0)
                : 0.0;

        double decompMiBPerSec = decompNs > 0
                ? inputMiB / (decompNs / 1_000_000_000.0)
                : 0.0;

        double entropy = Entropy.calculateH0(input);

        // Metadata theo loại codec.
        String params = "";
        String lzwDictionaryFullAt = "";
        String lz4SequenceCount = "";
        String lz4AverageMatchLength = "";

        if (codec instanceof LZWCodec) {
            LZWCodec lzw = (LZWCodec) codec;
            int fullAt = lzw.getDictionaryFullAt();

            if (fullAt >= 0) {
                lzwDictionaryFullAt = String.valueOf(fullAt);
            }

            params = "dictionary_size=4096;code_bits=12";
        }

        if (codec instanceof LZ4Codec) {
            LZ4Codec lz4 = (LZ4Codec) codec;

            params = "hashLog=" + lz4.getHashLog();

            // Phân tích block sau khi đã dừng đồng hồ đo.
            LZ4Codec.BlockStatistics stats = lz4.analyze(compressed);

            lz4SequenceCount = String.valueOf(stats.getSequenceCount());

            lz4AverageMatchLength = String.format(
                    Locale.ROOT,
                    "%.6f",
                    stats.getAverageMatchLength()
            );
        }

        return new BenchmarkResult(
                rqName,
                datasetName,
                fileName,
                variant,
                seed,
                input.length,
                codec.getName(),
                params,
                compressed.length,
                ratio,
                compNs,
                decompNs,
                compMiBPerSec,
                decompMiBPerSec,
                compHeapAux,
                decompHeapAux,
                entropy,
                lzwDictionaryFullAt,
                lz4SequenceCount,
                lz4AverageMatchLength,
                outputCheck,
                isMatch ? "PASS" : "FAIL"
        );
    }

    private static String getSha256Hex(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder hex = new StringBuilder(hash.length * 2);

            for (byte b : hash) {
                hex.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            }

            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 được Java hỗ trợ; nếu không có, báo lỗi rõ ràng.
            throw new IllegalStateException("Không thể khởi tạo SHA-256", e);
        }
    }

    /**
     * Không cần tạo BenchmarkResult.java riêng.
     */
    public static class BenchmarkResult {
        private final String rq;
        private final String dataset;
        private final String file;
        private final String variant;
        private final long seed;
        private final int inputBytes;
        private final String codec;
        private final String params;
        private final int compressedBytes;
        private final String ratio;
        private final long compressNs;
        private final long decompressNs;
        private final double compressMiBS;
        private final double decompressMiBS;
        private final long compressHeapAuxBytes;
        private final long decompressHeapAuxBytes;
        private final double entropyH0;
        private final String lzwDictionaryFullAt;
        private final String lz4SequenceCount;
        private final String lz4AverageMatchLength;
        private final String outputCheck;
        private final String status;

        public BenchmarkResult(
                String rq,
                String dataset,
                String file,
                String variant,
                long seed,
                int inputBytes,
                String codec,
                String params,
                int compressedBytes,
                String ratio,
                long compressNs,
                long decompressNs,
                double compressMiBS,
                double decompressMiBS,
                long compressHeapAuxBytes,
                long decompressHeapAuxBytes,
                double entropyH0,
                String lzwDictionaryFullAt,
                String lz4SequenceCount,
                String lz4AverageMatchLength,
                String outputCheck,
                String status) {

            this.rq = rq;
            this.dataset = dataset;
            this.file = file;
            this.variant = variant;
            this.seed = seed;
            this.inputBytes = inputBytes;
            this.codec = codec;
            this.params = params;
            this.compressedBytes = compressedBytes;
            this.ratio = ratio;
            this.compressNs = compressNs;
            this.decompressNs = decompressNs;
            this.compressMiBS = compressMiBS;
            this.decompressMiBS = decompressMiBS;
            this.compressHeapAuxBytes = compressHeapAuxBytes;
            this.decompressHeapAuxBytes = decompressHeapAuxBytes;
            this.entropyH0 = entropyH0;
            this.lzwDictionaryFullAt = lzwDictionaryFullAt;
            this.lz4SequenceCount = lz4SequenceCount;
            this.lz4AverageMatchLength = lz4AverageMatchLength;
            this.outputCheck = outputCheck;
            this.status = status;
        }

        public String toCsvRow() {
            return String.join(",",
                    csv(rq),
                    csv(dataset),
                    csv(file),
                    csv(variant),
                    String.valueOf(seed),
                    String.valueOf(inputBytes),
                    csv(codec),
                    csv(params),
                    String.valueOf(compressedBytes),
                    ratio,
                    String.valueOf(compressNs),
                    String.valueOf(decompressNs),
                    String.format(Locale.ROOT, "%.6f", compressMiBS),
                    String.format(Locale.ROOT, "%.6f", decompressMiBS),
                    String.valueOf(compressHeapAuxBytes),
                    String.valueOf(decompressHeapAuxBytes),
                    String.format(Locale.ROOT, "%.6f", entropyH0),
                    csv(lzwDictionaryFullAt),
                    csv(lz4SequenceCount),
                    csv(lz4AverageMatchLength),
                    csv(outputCheck),
                    csv(status)
            );
        }

        private static String csv(String value) {
            if (value == null) {
                return "";
            }

            if (value.contains(",") || value.contains("\"")
                    || value.contains("\n") || value.contains("\r")) {
                return "\"" + value.replace("\"", "\"\"") + "\"";
            }

            return value;
        }
    }
}
