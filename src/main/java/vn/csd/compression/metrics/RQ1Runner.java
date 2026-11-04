package vn.csd.compression.metrics;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Runner điều phối toàn bộ thí nghiệm RQ1.
 * Mở từng lượt đo qua ProcessBuilder với JVM sạch, áp dụng Timeout 60s và gom kết quả vào CSV.
 */
public class RQ1Runner {

    private static final String CSV_HEADER = "rq,dataset,file,variant,seed,input_bytes,codec,params,compressed_bytes,ratio,compress_ns,decompress_ns,compress_mib_s,decompress_mib_s,compress_heap_aux_bytes,decompress_heap_aux_bytes,entropy_h0,lzw_dictionary_full_at,lz4_sequence_count,lz4_average_match_length,output_check,status\n";

    public static void main(String[] args) throws Exception {
        boolean quick = false;
        String xmx = "1g";
        List<String> codecs = Arrays.asList("rle", "huffman", "lzw");

        for (int i = 0; i < args.length; i++) {
            if ("--quick".equalsIgnoreCase(args[i])) {
                quick = true;
            } else if ("--xmx".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                xmx = args[++i];
            } else if ("--codecs".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                codecs = Arrays.asList(args[++i].split(","));
            }
        }

        int[] sizes = quick ? new int[]{1024 * 1024, 4 * 1024 * 1024} : new int[]{
            1 * 1024 * 1024,
            4 * 1024 * 1024,
            16 * 1024 * 1024,
            32 * 1024 * 1024,
            64 * 1024 * 1024
        };

        String[] datasets = {"C1", "C2", "C3"};
        long[] seeds = quick ? new long[]{20261020L} : new long[]{20261020L, 20261021L, 20261022L};

        Path outDir = Paths.get("results/raw");
        Files.createDirectories(outDir);
        Path csvPath = outDir.resolve("rq1.csv");

        System.out.println("===================================================================");
        System.out.println("  BAT DAU THI NGHIEM RQ1 (ProcessBuilder - Clean JVM - Timeout 60s)");
        System.out.println("  Che do: " + (quick ? "QUICK (Thu nghiem nhanh)" : "FULL MA TRAN"));
        System.out.println("  Codecs: " + codecs);
        System.out.println("  File ket qua: " + csvPath.toAbsolutePath());
        System.out.println("===================================================================");

        try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(CSV_HEADER);
            writer.flush();

            int totalRuns = codecs.size() * datasets.length * sizes.length * seeds.length;
            int runCount = 0;

            String javaBin = Paths.get(System.getProperty("java.home"), "bin", "java").toString();

            for (int size : sizes) {
                for (String dataset : datasets) {
                    for (long seed : seeds) {
                        for (String codec : codecs) {
                            runCount++;
                            String label = String.format("[%d/%d] Codec=%-7s Dataset=%-2s Size=%2d MiB Seed=%d",
                                    runCount, totalRuns, codec, dataset, size / (1024 * 1024), seed);
                            System.out.print(label + " -> ");

                            ProcessBuilder pb = new ProcessBuilder(
                                    javaBin,
                                    "-Xms256m",
                                    "-Xmx" + xmx,
                                    "-XX:+UseG1GC",
                                    "-Dfile.encoding=UTF-8",
                                    "-cp",
                                    "bin",
                                    "vn.csd.compression.metrics.RQ1Worker",
                                    codec,
                                    dataset,
                                    String.valueOf(size),
                                    String.valueOf(seed)
                            );

                            pb.redirectErrorStream(true);
                            Process proc = pb.start();

                            boolean finished = proc.waitFor(60, TimeUnit.SECONDS);

                            if (!finished) {
                                proc.destroyForcibly();
                                System.out.println("TIMEOUT (> 60s)");
                                String timeoutRow = String.format("RQ1,%s,%s_%d_%d,base,%d,%d,%s,,0,,0,0,0,0,0,0,0,,,TIMEOUT:Exceeded 60s,TIMEOUT\n",
                                        dataset, dataset, size, seed, seed, size, codec);
                                writer.write(timeoutRow);
                                writer.flush();
                                continue;
                            }

                            String outputRow = null;
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (line.startsWith("RESULT:")) {
                                        outputRow = line.substring(7);
                                    }
                                }
                            }

                            if (proc.exitValue() == 0 && outputRow != null) {
                                writer.write(outputRow + "\n");
                                writer.flush();
                                System.out.println("PASS");
                            } else {
                                System.out.println("CRASH (Exit code: " + proc.exitValue() + ")");
                                String crashRow = String.format("RQ1,%s,%s_%d_%d,base,%d,%d,%s,,0,,0,0,0,0,0,0,0,,,CRASH,CRASH\n",
                                        dataset, dataset, size, seed, seed, size, codec);
                                writer.write(crashRow);
                                writer.flush();
                            }
                        }
                    }
                }
            }
        }

        System.out.println("\n===================================================================");
        System.out.println("🎉 HOAN TAT THI NGHIEM RQ1! KET QUA DA LUU TAI: " + csvPath);
        System.out.println("===================================================================");
    }
}
