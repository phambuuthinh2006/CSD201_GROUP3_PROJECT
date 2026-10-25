package vn.csd.compression.metrics;

import vn.csd.compression.codec.Codec;
import java.security.MessageDigest;

public class BenchmarkRunner {

    public static BenchmarkResult run(Codec codec, byte[] input, String rqName, String datasetName, String fileName, String variant, long seed) {
        // Đảm bảo không đo I/O hay khởi tạo trong lúc đo nén/giải nén
        System.gc(); 
        long heapBeforeComp = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long startTimeComp = System.nanoTime();
        byte[] compressed = codec.compress(input);
        long endTimeComp = System.nanoTime();

        long heapAfterComp = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long compHeapAux = Math.max(0L, heapAfterComp - heapBeforeComp);
        // Đo giải nén
        long heapBeforeDecomp = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long startTimeDecomp = System.nanoTime();
        byte[] decompressed = codec.decompress(compressed);
        long endTimeDecomp = System.nanoTime();

        long heapAfterDecomp = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long decompHeapAux = Math.max(0L, heapAfterDecomp - heapBeforeDecomp);
        // Kiểm tra tính đúng đắn qua SHA-256
        boolean isMatch = java.util.Arrays.equals(input, decompressed);
        String outputCheck = "";
        if (isMatch) {
            outputCheck = getSha256Hex(decompressed).substring(0, 16);
        } else {
            outputCheck = "FAIL_MISMATCH";
        }

        double ratio = (double) compressed.length / input.length;
        double compTimeSec = (endTimeComp - startTimeComp) / 1_000_000_000.0;
        double decompTimeSec = (endTimeDecomp - startTimeDecomp) / 1_000_000_000.0;

        double compMibS = (input.length / (1024.0 * 1024.0)) / compTimeSec;
        double decompMibS = (input.length / (1024.0 * 1024.0)) / decompTimeSec;
        double h0 = Entropy.calculateH0(input);

        return new BenchmarkResult(
            rqName, datasetName, fileName, variant, seed, input.length,
            codec.getName(), compressed.length, ratio,
            endTimeComp - startTimeComp, endTimeDecomp - startTimeDecomp,
            compMibS, decompMibS, compHeapAux, decompHeapAux, h0, outputCheck, isMatch ? "PASS" : "FAIL"
        );
    }

    private static String getSha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }
}