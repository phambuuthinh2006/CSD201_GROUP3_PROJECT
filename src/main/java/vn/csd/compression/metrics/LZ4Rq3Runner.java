package vn.csd.compression.metrics;

import vn.csd.compression.codec.LZ4Codec;
import vn.csd.compression.codec.LZ4Codec.BlockStatistics;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Locale;

/** LZ4-only RQ3 measurements; TV4/TV6 merge these with the common runner. */
public final class LZ4Rq3Runner {
    public LZ4Rq3Runner() { }

    public static void main(String[] args) throws Exception {
        new LZ4Rq3Runner().run(args);
    }

    public void run(String[] args) throws Exception {
        if (args.length < 4) {
            throw new IllegalArgumentException("Usage: LZ4Rq3Runner output.csv warmups repeats input-file...");
        }
        int warmups = Integer.parseInt(args[1]);
        int repeats = Integer.parseInt(args[2]);
        if (warmups < 0 || repeats < 1) throw new IllegalArgumentException("Invalid iteration counts");
        Path destination = Paths.get(args[0]);
        // CREATE_NEW prevents an accidental overwrite of previous experiment results.
        try (BufferedWriter csv = Files.newBufferedWriter(destination, StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE)) {
            csv.write("file,hash_log,iteration,input_bytes,compressed_bytes,ratio,compress_ns,decompress_ns,compress_mib_s,decompress_mib_s,lz4_sequence_count,lz4_match_count,lz4_avg_match_length,output_check,status\n");
            System.err.println("Java=" + System.getProperty("java.runtime.version")
                    + "; VM=" + System.getProperty("java.vm.name")
                    + "; OS=" + System.getProperty("os.name") + " " + System.getProperty("os.arch")
                    + "; maxHeap=" + Runtime.getRuntime().maxMemory()
                    + "; JVM args=" + java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()
                    + "; warmups=" + warmups + "; repeats=" + repeats);
            LZ4Codec sizePolicy = new LZ4Codec();
            for (int i = 3; i < args.length; i++) {
                Path path = Paths.get(args[i]);
                if (Files.size(path) > sizePolicy.getMaxInputSize()) throw new IllegalArgumentException("File exceeds 64 MiB: " + path);
                byte[] input = Files.readAllBytes(path);
                byte[] expected = sha256(input);
                for (int hashLog : new int[]{12, 16}) {
                    LZ4Codec codec = new LZ4Codec(hashLog);
                    for (int run = -warmups; run < repeats; run++) {
                        long start = System.nanoTime();
                        byte[] compressed = codec.compress(input);
                        long compressNs = System.nanoTime() - start;
                        start = System.nanoTime();
                        byte[] decoded = codec.decompress(compressed);
                        long decompressNs = System.nanoTime() - start;
                        byte[] actual = sha256(decoded);
                        if (decoded.length != input.length || !MessageDigest.isEqual(expected, actual)) {
                            throw new IllegalStateException("SHA-256 mismatch: " + path + ", hashLog=" + hashLog);
                        }
                        if (run < 0) continue;
                        BlockStatistics stats = codec.analyze(compressed);
                        String ratio = input.length == 0 ? "" : String.format(Locale.ROOT, "%.9f", (double) compressed.length / input.length);
                        double mib = input.length / 1048576.0;
                        csv.write(String.format(Locale.ROOT,
                                "%s,%d,%d,%d,%d,%s,%d,%d,%.6f,%.6f,%d,%d,%.6f,%s,PASS%n",
                                quote(path.toString()), hashLog, run + 1, input.length, compressed.length,
                                ratio, compressNs, decompressNs, mib * 1e9 / Math.max(1, compressNs),
                                mib * 1e9 / Math.max(1, decompressNs), stats.getSequenceCount(),
                                stats.getMatchCount(), stats.getAverageMatchLength(), hexPrefix(actual)));
                        csv.flush();
                    }
                }
            }
        }
    }

    private byte[] sha256(byte[] input) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(input);
    }

    private String quote(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
    private String hexPrefix(byte[] hash) {
        StringBuilder text = new StringBuilder(16);
        for (int i = 0; i < 8; i++) text.append(String.format(Locale.ROOT, "%02x", hash[i] & 255));
        return text.toString();
    }
}
