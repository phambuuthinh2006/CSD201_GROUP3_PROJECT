import vn.csd.compression.codec.*;
import vn.csd.compression.dataset.*;
import vn.csd.compression.metrics.*;
import java.util.*;
import java.nio.file.*;
import java.security.MessageDigest;

public class IntegrationCheck {
    public static void main(String[] args) throws Exception {
        new IntegrationCheck().run(args);
    }

    private void run(String[] args) throws Exception {
        int checks = 0;
        for (int size : new int[]{0, 1, 13, 4096, 1048576}) {
            byte[][] datasets = {DataGenerator.generateC1(size, 20261027),
                    DataGenerator.generateC2(size, 20261027), DataGenerator.generateC3(size, 20261027)};
            for (int ds = 0; ds < datasets.length; ds++) {
                byte[] input = datasets[ds];
                byte[] expected = MessageDigest.getInstance("SHA-256").digest(input);
                for (Codec codec : new Codec[]{new RLECodec(), new HuffmanCodec(), new LZWCodec(), new LZ4Codec(12), new LZ4Codec(16)}) {
                    byte[] output = codec.decompress(codec.compress(input));
                    if (!Arrays.equals(input, output) || !Arrays.equals(expected, MessageDigest.getInstance("SHA-256").digest(output))) {
                        throw new AssertionError(codec.getName() + " C" + (ds + 1) + " size=" + size);
                    }
                    checks++;
                    if (size == 4096) CsvWriter.writeResult(args[0], BenchmarkRunner.run(codec, input,
                            "CHECK", "C" + (ds + 1), "generated", "original", 20261027));
                }
                for (byte[] variant : new byte[][]{ByteOrdering.original(input), ByteOrdering.grouped(input), ByteOrdering.shuffled(input, 42)}) {
                    int[] before = new int[256], after = new int[256];
                    for (byte b : input) before[b & 255]++;
                    for (byte b : variant) after[b & 255]++;
                    if (!Arrays.equals(before, after)) throw new AssertionError("Histogram mismatch");
                    if (Math.abs(Entropy.calculateH0(input) - Entropy.calculateH0(variant)) > 1e-12) throw new AssertionError("H0 mismatch");
                }
                for (String type : new String[]{"C" + (ds + 1)}) {
                    Path file = Paths.get(args[1], type + "-" + size + ".bin");
                    DataGenerator.generateToFile(type, size, 20261027, file.toFile());
                    if (!Arrays.equals(input, Files.readAllBytes(file))) throw new AssertionError("Array/stream mismatch " + type);
                }
            }
        }
        System.out.println("PASS " + checks + " generator/codec round-trips with full SHA-256");
        System.out.println("PASS histograms/H0 of three orderings, 15 generator array/file equivalence cases");
        System.out.println("PASS common BenchmarkRunner/CsvWriter exercised on 15 cases; inspect CSV separately");
    }
}
