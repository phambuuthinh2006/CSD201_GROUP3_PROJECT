package vn.csd.compression.metrics;

import vn.csd.compression.codec.*;
import vn.csd.compression.dataset.ByteOrdering;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class RQ2Runner {

    private static final String CSV_PATH = "results/rq2.csv";
    private static final long SHUFFLE_SEED = 20261020L;

    public static void main(String[] args) {
        System.out.println("========================================================================");
        System.out.println("   KHOI CHAY BENCHMARK TU DONG CHO RQ2 (BYTE ORDERING & ENTROPY)");
        System.out.println("========================================================================");

        // 1. Kiểm tra và định vị các file R1
        String[] r1Files = {"dickens", "xml", "mozilla"};
        Path dataDir = Paths.get("data");

        for (String file : r1Files) {
            Path p = dataDir.resolve(file);
            if (!Files.exists(p)) {
                System.err.println("[LOI] Khong tim thay file: " + p.toAbsolutePath());
                System.err.println("Vui long dam bao ca 3 file dickens, xml, mozilla da co trong thu muc data/");
                return;
            }
        }

       
        Path csvFile = Paths.get(CSV_PATH);
        if (Files.exists(csvFile)) {
            try {
                Files.delete(csvFile);
                System.out.println("[INFO] Da xoa file ket qua cu: " + CSV_PATH);
            } catch (IOException e) {
                System.err.println("[CANH BAO] Khong the xoa file cu: " + e.getMessage());
            }
        }

        
        List<Codec> codecs = new ArrayList<>();
        codecs.add(new RLECodec());
        codecs.add(new HuffmanCodec());
        codecs.add(new LZWCodec());
        codecs.add(new LZ4Codec(12)); // HashLog mặc định theo đề tài

        int totalRuns = r1Files.length * 3 * codecs.size();
        int currentRun = 0;
        int passCount = 0;

        System.out.println("Tong so ca chay du kien: " + totalRuns + " (3 files * 3 variants * 4 codecs)");
        System.out.println("------------------------------------------------------------------------");

        for (String fileName : r1Files) {
            Path filePath = dataDir.resolve(fileName);
            System.out.println("\n>>> [DATASET] Dang xu ly tap tin: " + fileName + " (" + getFileSizeDescription(filePath) + ")");

            byte[] originalBytes;
            try {
                originalBytes = Files.readAllBytes(filePath);
            } catch (IOException e) {
                System.err.println("[LOI] Khong doc duoc file " + fileName + ": " + e.getMessage());
                continue;
            }

            
            System.out.println("    -> Dang tao 3 bien the: original, grouped, shuffled (seed=" + SHUFFLE_SEED + ")...");
            byte[] groupedBytes = ByteOrdering.grouped(originalBytes);
            byte[] shuffledBytes = ByteOrdering.shuffled(originalBytes, SHUFFLE_SEED);

          
            double h0Orig = Entropy.calculateH0(originalBytes);
            double h0Group = Entropy.calculateH0(groupedBytes);
            double h0Shuff = Entropy.calculateH0(shuffledBytes);

            System.out.printf("    -> [Test T11 Verification] H0 original: %.6f | grouped: %.6f | shuffled: %.6f%n",
                    h0Orig, h0Group, h0Shuff);
            if (Math.abs(h0Orig - h0Group) > 1e-9 || Math.abs(h0Orig - h0Shuff) > 1e-9) {
                System.err.println("[CANH BAO] Entropy H0 giua cac bien the khong khop! Kiem tra lai ByteOrdering!");
            } else {
                System.out.println("    -> [PASS T11] Entropy H0 va Histogram cua 3 bien the trung khop 100%!");
            }

            
            String[] variantNames = {"original", "grouped", "shuffled"};
            byte[][] variantData = {originalBytes, groupedBytes, shuffledBytes};

            for (int v = 0; v < variantNames.length; v++) {
                String variant = variantNames[v];
                byte[] input = variantData[v];
                long seed = variant.equals("shuffled") ? SHUFFLE_SEED : 0L;

                for (Codec codec : codecs) {
                    currentRun++;
                    System.out.printf("[%2d/%2d] Chay %-8s | File: %-7s | Variant: %-8s ... ",
                            currentRun, totalRuns, codec.getName(), fileName, variant);

                    try {
                        BenchmarkRunner.BenchmarkResult result = BenchmarkRunner.run(
                                codec,
                                input,
                                "RQ2",
                                "R1",
                                fileName,
                                variant,
                                seed
                        );

            
                        CsvWriter.writeResult(CSV_PATH, result);

                        if ("PASS".equals(result.toCsvRow().substring(result.toCsvRow().lastIndexOf(',') + 1))) {
                            passCount++;
                            System.out.printf("[PASS] Ratio: %s | Comp: %.2f MiB/s | Decomp: %.2f MiB/s%n",
                                    result.toCsvRow().split(",")[9],
                                    Double.parseDouble(result.toCsvRow().split(",")[12]),
                                    Double.parseDouble(result.toCsvRow().split(",")[13]));
                        } else {
                            System.out.println("[FAIL]");
                        }
                    } catch (Exception e) {
                        System.out.println("[ERROR: " + e.getMessage() + "]");
                        e.printStackTrace();
                    }
                }
            }
        }

        System.out.println("\n========================================================================");
        System.out.printf("HOAN THANH BENCHMARK RQ2! Ket qua: %d/%d ca PASS 100%%%n", passCount, totalRuns);
        System.out.println("Du lieu da duoc xuat ra tep: " + Paths.get(CSV_PATH).toAbsolutePath());
        System.out.println("========================================================================");
    }

    private static String getFileSizeDescription(Path path) {
        try {
            long bytes = Files.size(path);
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } catch (IOException e) {
            return "unknown size";
        }
    }
}
