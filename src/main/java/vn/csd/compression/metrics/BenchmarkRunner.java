
package vn.csd.compression.metrics;

import vn.csd.compression.codec.Codec;
import java.security.MessageDigest;
import java.util.Arrays;

public class BenchmarkRunner {

    public static BenchmarkResult run(
            Codec codec,
            byte[] input,
            String rqName,
            String datasetName,
            String fileName,
            String variant,
            long seed) {

        System.gc();

        long heapBeforeComp = Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory();

        long startTimeComp = System.nanoTime();
        byte[] compressed = codec.compress(input);
        long endTimeComp = System.nanoTime();

        long heapAfterComp = Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory();

        long compHeapAux = Math.max(0L, heapAfterComp - heapBeforeComp);

        long heapBeforeDecomp = Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory();

        long startTimeDecomp = System.nanoTime();
        byte[] decompressed = codec.decompress(compressed);
        long endTimeDecomp = System.nanoTime();

        long heapAfterDecomp = Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory();

        long decompHeapAux = Math.max(0L, heapAfterDecomp - heapBeforeDecomp);

        boolean isMatch = Arrays.equals(input, decompressed);

        String outputCheck = isMatch
                ? getSha256Hex(decompressed).substring(0, 16)
                : "FAIL_MISMATCH";

        double ratio = input.length == 0
                ? 0.0
                : (double) compressed.length / input.length;

        double compTimeSec =
                (endTimeComp - startTimeComp) / 1_000_000_000.0;

        double decompTimeSec =
                (endTimeDecomp - startTimeDecomp) / 1_000_000_000.0;

        double compMibS = compTimeSec == 0
                ? 0.0
                : (input.length / (1024.0 * 1024.0)) / compTimeSec;

        double decompMibS = decompTimeSec == 0
                ? 0.0
                : (input.length / (1024.0 * 1024.0)) / decompTimeSec;

        double h0 = Entropy.calculateH0(input);

        return new BenchmarkResult(
                rqName,
                datasetName,
                fileName,
                variant,
                seed,
                input.length,
                codec.getName(),
                compressed.length,
                ratio,
                endTimeComp - startTimeComp,
                endTimeDecomp - startTimeDecomp,
                compMibS,
                decompMibS,
                compHeapAux,
                decompHeapAux,
                h0,
                outputCheck,
                isMatch ? "PASS" : "FAIL"
        );
    }

    private static String getSha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            return "";
        }
    }

    // Class BenchmarkResult được đặt bên trong BenchmarkRunner
    public static class BenchmarkResult {

        private final String rq;
        private final String dataset;
        private final String file;
        private final String variant;
        private final long seed;
        private final long inputBytes;
        private final String codec;
        private final long compressedBytes;
        private final double ratio;
        private final long compressNs;
        private final long decompressNs;
        private final double compressMibS;
        private final double decompressMibS;
        private final long compressHeapAuxBytes;
        private final long decompressHeapAuxBytes;
        private final double entropyH0;
        private final String outputCheck;
        private final String status;

        public BenchmarkResult(
                String rq,
                String dataset,
                String file,
                String variant,
                long seed,
                long inputBytes,
                String codec,
                long compressedBytes,
                double ratio,
                long compressNs,
                long decompressNs,
                double compressMibS,
                double decompressMibS,
                long compressHeapAuxBytes,
                long decompressHeapAuxBytes,
                double entropyH0,
                String outputCheck,
                String status) {

            this.rq = rq;
            this.dataset = dataset;
            this.file = file;
            this.variant = variant;
            this.seed = seed;
            this.inputBytes = inputBytes;
            this.codec = codec;
            this.compressedBytes = compressedBytes;
            this.ratio = ratio;
            this.compressNs = compressNs;
            this.decompressNs = decompressNs;
            this.compressMibS = compressMibS;
            this.decompressMibS = decompressMibS;
            this.compressHeapAuxBytes = compressHeapAuxBytes;
            this.decompressHeapAuxBytes = decompressHeapAuxBytes;
            this.entropyH0 = entropyH0;
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
                    "",
                    String.valueOf(compressedBytes),
                    String.valueOf(ratio),
                    String.valueOf(compressNs),
                    String.valueOf(decompressNs),
                    String.valueOf(compressMibS),
                    String.valueOf(decompressMibS),
                    String.valueOf(compressHeapAuxBytes),
                    String.valueOf(decompressHeapAuxBytes),
                    String.valueOf(entropyH0),
                    "",
                    "",
                    "",
                    csv(outputCheck),
                    csv(status)
            );
        }

        private String csv(String value) {
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
