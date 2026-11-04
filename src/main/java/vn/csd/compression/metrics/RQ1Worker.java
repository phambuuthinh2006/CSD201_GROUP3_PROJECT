package vn.csd.compression.metrics;

import vn.csd.compression.codec.*;
import vn.csd.compression.dataset.DataGenerator;

/**
 * Worker chạy trong một tiến trình JVM độc lập cho từng lượt đo RQ1.
 * Đảm bảo cô lập bộ nhớ và loại trừ ảnh hưởng của JIT giữa các lượt đo.
 */
public class RQ1Worker {

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: RQ1Worker <codecName> <datasetName> <sizeBytes> <seed>");
            System.exit(1);
        }

        String codecName = args[0];
        String datasetName = args[1];
        int size = Integer.parseInt(args[2]);
        long seed = Long.parseLong(args[3]);

        // 1. Khởi tạo Codec
        Codec codec;
        if ("rle".equalsIgnoreCase(codecName)) {
            codec = new RLECodec();
        } else if ("huffman".equalsIgnoreCase(codecName)) {
            codec = new HuffmanCodec();
        } else if ("lzw".equalsIgnoreCase(codecName)) {
            codec = new LZWCodec();
        } else if ("lz4".equalsIgnoreCase(codecName) || "lz4-12".equalsIgnoreCase(codecName)) {
            codec = new LZ4Codec(12);
        } else if ("lz4-16".equalsIgnoreCase(codecName)) {
            codec = new LZ4Codec(16);
        } else {
            System.err.println("Unknown codec: " + codecName);
            System.exit(1);
            return;
        }

        // 2. Sinh dữ liệu theo Dataset C1, C2, C3
        DataGenerator gen = new DataGenerator();
        byte[] input;
        if ("C1".equalsIgnoreCase(datasetName)) {
            input = gen.generateC1(size, seed);
        } else if ("C2".equalsIgnoreCase(datasetName)) {
            input = gen.generateC2(size, seed);
        } else if ("C3".equalsIgnoreCase(datasetName)) {
            input = gen.generateC3(size, seed);
        } else {
            System.err.println("Unknown dataset: " + datasetName);
            System.exit(1);
            return;
        }

        // 3. Làm ấm JIT (Warm-up 3 lần trên dữ liệu nhỏ <= 1 MiB)
        int warmupSize = Math.min(size, 1024 * 1024);
        byte[] warmupData = new byte[warmupSize];
        System.arraycopy(input, 0, warmupData, 0, warmupSize);
        for (int i = 0; i < 3; i++) {
            byte[] c = codec.compress(warmupData);
            codec.decompress(c);
        }

        // 4. Đo chính thức thông qua BenchmarkRunner
        BenchmarkRunner.BenchmarkResult res = BenchmarkRunner.run(
                codec, input, "RQ1", datasetName, datasetName + "_" + size + "_" + seed, "base", seed
        );

        // In ra dòng kết quả CSV để Runner đón nhận
        System.out.println("RESULT:" + res.toCsvRow());
    }
}
