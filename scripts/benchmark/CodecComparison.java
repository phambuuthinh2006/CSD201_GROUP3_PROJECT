import vn.csd.compression.codec.*;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.*;

/** Exploratory comparison; leaves production codecs and the common runner unchanged. */
public final class CodecComparison {
    public static void main(String[] args) throws Exception {
        new CodecComparison().run(Paths.get(args[0]), Integer.parseInt(args[1]));
    }

    private void run(Path directory, int fork) throws Exception {
        int warmups = 5;
        int repeats = 7;
        long seed = 20261027L;
        System.out.println("fork=" + fork + "; java=" + System.getProperty("java.runtime.version")
                + "; vm=" + System.getProperty("java.vm.name") + "; os=" + System.getProperty("os.name")
                + "; heap=" + Runtime.getRuntime().maxMemory()
                + "; flags=" + java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()
                + "; seed=" + seed + "; warmups=" + warmups + "; repeats=" + repeats);
        try (BufferedWriter csv = Files.newBufferedWriter(directory.resolve("raw-fork-" + fork + ".csv"),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
             BufferedWriter manifest = Files.newBufferedWriter(directory.resolve("inputs-fork-" + fork + ".csv"),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            csv.write("fork,dataset,input_bytes,seed,iteration,codec,compressed_bytes,ratio,compress_ns,decompress_ns,compress_mib_s,decompress_mib_s,output_check,status\n");
            manifest.write("dataset,input_bytes,seed,sha256\n");
            for (int size : new int[]{1048576, 4194304}) {
                for (String dataset : new String[]{"SINGLE_BYTE", "LONG_RUNS", "WORD_TEXT", "UNIFORM_RANDOM"}) {
                    byte[] input = generate(dataset, size, seed);
                    byte[] expected = digest(input);
                    manifest.write(dataset + "," + size + "," + seed + "," + hex(expected) + "\n");
                    manifest.flush();
                    Codec[] codecs = {new RLECodec(), new HuffmanCodec(), new LZWCodec(), new LZ4Codec(12), new LZ4Codec(16)};
                    String[] labels = {"RLE", "Huffman", "LZW", "LZ4-12", "LZ4-16"};
                    List<Integer> order = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4));
                    Random schedule = new Random(seed + fork * 104729L + size + dataset.hashCode());
                    for (int round = -warmups; round < repeats; round++) {
                        Collections.shuffle(order, schedule);
                        for (int index : order) {
                            Codec codec = codecs[index];
                            long start = System.nanoTime();
                            byte[] compressed = codec.compress(input);
                            long compressNs = System.nanoTime() - start;
                            start = System.nanoTime();
                            byte[] decoded = codec.decompress(compressed);
                            long decompressNs = System.nanoTime() - start;
                            byte[] actual = digest(decoded);
                            if (decoded.length != size || !MessageDigest.isEqual(expected, actual)) {
                                throw new IllegalStateException("Round-trip FAIL: " + dataset + " " + labels[index]);
                            }
                            if (round >= 0) {
                                double mib = size / 1048576.0;
                                csv.write(String.format(Locale.ROOT,
                                        "%d,%s,%d,%d,%d,%s,%d,%.9f,%d,%d,%.6f,%.6f,%s,PASS%n",
                                        fork, dataset, size, seed, round + 1, labels[index], compressed.length,
                                        (double) compressed.length / size, compressNs, decompressNs,
                                        mib * 1e9 / Math.max(1, compressNs), mib * 1e9 / Math.max(1, decompressNs),
                                        hex(actual).substring(0, 16)));
                            }
                        }
                    }
                    csv.flush();
                    System.out.println("PASS fork=" + fork + " dataset=" + dataset + " MiB=" + size / 1048576
                            + " (5 codecs; 5 warm-up + 7 measured each; SHA-256 all rounds)");
                }
            }
        }
    }

    private byte[] generate(String name, int size, long seed) {
        byte[] data = new byte[size];
        Random random = new Random(seed);
        if (name.equals("SINGLE_BYTE")) {
            Arrays.fill(data, (byte) 'A');
        } else if (name.equals("UNIFORM_RANDOM")) {
            random.nextBytes(data);
        } else if (name.equals("LONG_RUNS")) {
            int position = 0;
            while (position < size) {
                int run = 1 + (int) (Math.log1p(-random.nextDouble()) / Math.log(31.0 / 32.0));
                int end = position + Math.min(run, size - position);
                Arrays.fill(data, position, end, (byte) random.nextInt(256));
                position = end;
            }
        } else if (name.equals("WORD_TEXT")) {
            // A deterministic 1000-word vocabulary sampled with Zipf weights 1/rank.
            byte[][] vocabulary = new byte[1000][];
            double[] cumulative = new double[1000];
            double total = 0;
            for (int word = 0; word < vocabulary.length; word++) {
                byte[] letters = new byte[3 + random.nextInt(8)];
                for (int p = 0; p < letters.length; p++) letters[p] = (byte) ('a' + random.nextInt(26));
                vocabulary[word] = letters;
                total += 1.0 / (word + 1);
                cumulative[word] = total;
            }
            int position = 0;
            while (position < size) {
                double draw = random.nextDouble() * total;
                int index = Arrays.binarySearch(cumulative, draw);
                if (index < 0) index = -index - 1;
                byte[] word = vocabulary[index];
                int count = Math.min(word.length, size - position);
                System.arraycopy(word, 0, data, position, count);
                position += count;
                if (position < size) data[position++] = (byte) ' ';
            }
        } else {
            throw new IllegalArgumentException("Unknown dataset: " + name);
        }
        return data;
    }

    private byte[] digest(byte[] input) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(input);
    }

    private String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value & 255));
        return result.toString();
    }
}
