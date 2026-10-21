package vn.csd.compression;

import net.jpountz.lz4.LZ4Factory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.csd.compression.codec.LZ4Codec;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Assertions;

class LZ4CodecTest {
    private byte[] container(int size, byte... block) {
        return ByteBuffer.allocate(16 + block.length).putInt(0x4c5a3442)
                .putLong(size).putInt(block.length).put(block).array();
    }

    private void roundTrip(LZ4Codec codec, byte[] input) {
        byte[] before = input.clone();
        byte[] encoded = codec.compress(input);
        byte[] saved = encoded.clone();
        Assertions.assertArrayEquals(input, codec.decompress(encoded));
        Assertions.assertArrayEquals(before, input);
        Assertions.assertArrayEquals(saved, encoded);
        // Cross-hash decoding: decoder does not depend on encoder's table size.
        Assertions.assertArrayEquals(input, new LZ4Codec(codec.getHashLog() == 12 ? 16 : 12).decompress(encoded));
    }

    @ParameterizedTest @ValueSource(ints = {12, 16})
    void emptyAndShortInputsAreLiteralOnly(int log) {
        LZ4Codec codec = new LZ4Codec(log);
        for (int n = 0; n < 13; n++) {
            byte[] input = new byte[n];
            Arrays.fill(input, (byte) 65);
            roundTrip(codec, input);
            byte[] encoded = codec.compress(input);
            Assertions.assertEquals(n + 17, encoded.length);
            Assertions.assertEquals(n << 4, encoded[16] & 255);
            Assertions.assertEquals(0, codec.analyze(encoded).getMatchCount());
        }
        Assertions.assertEquals(1, codec.analyze(codec.compress(new byte[0])).getSequenceCount());
    }

    @ParameterizedTest @ValueSource(ints = {12, 16})
    void allBytesAndLiteralExtensionBoundaries(int log) {
        LZ4Codec codec = new LZ4Codec(log);
        for (int n : new int[]{13, 14, 15, 16, 254, 255, 256, 269, 270, 271, 525, 4096}) {
            byte[] input = new byte[n];
            new Random(n).nextBytes(input);
            roundTrip(codec, input);
            if (n <= 271) {
                byte[] encoded = codec.compress(input);
                Assertions.assertEquals(0, codec.analyze(encoded).getMatchCount());
                if (n == 15) Assertions.assertEquals(0, encoded[17]);
                if (n == 270) { Assertions.assertEquals(255, encoded[17] & 255); Assertions.assertEquals(0, encoded[18]); }
            }
        }
        byte[] all = new byte[256];
        for (int i = 0; i < 256; i++) all[i] = (byte) i;
        roundTrip(codec, all);
    }

    @ParameterizedTest @ValueSource(ints = {12, 16})
    void overlappingMatchesAndMatchExtensions(int log) {
        LZ4Codec codec = new LZ4Codec(log);
        for (int n : new int[]{13, 14, 23, 24, 25, 278, 279, 280, 1024 * 1024}) {
            byte[] input = new byte[n];
            Arrays.fill(input, (byte) 'A');
            roundTrip(codec, input);
            byte[] encoded = codec.compress(input);
            LZ4Codec.BlockStatistics stats = codec.analyze(encoded);
            Assertions.assertEquals(2, stats.getSequenceCount());
            Assertions.assertEquals(1, stats.getMatchCount());
            Assertions.assertEquals(n - 6, stats.getTotalMatchLength());
            Assertions.assertEquals(n - 6, stats.getAverageMatchLength());
            Assertions.assertEquals(1, encoded[18]); // offset=1 little-endian
            Assertions.assertEquals(0, encoded[19]);
            Assertions.assertEquals(0x50, encoded[encoded.length - 6]);
        }
    }

    @ParameterizedTest @ValueSource(ints = {12, 16})
    void seededRandomAndRepetitiveInputs(int log) {
        Random random = new Random(20261021);
        LZ4Codec codec = new LZ4Codec(log);
        for (int i = 0; i < 120; i++) {
            byte[] input = new byte[random.nextInt(100000)];
            random.nextBytes(input);
            if (i % 2 == 0) {
                int period = 1 + random.nextInt(512);
                for (int p = period; p < input.length; p++) input[p] = input[p % period];
            }
            roundTrip(codec, input);
        }
    }

    @ParameterizedTest @ValueSource(ints = {12, 16})
    void compatibleWithIndependentJavaOracleInBothDirections(int log) {
        LZ4Factory oracle = LZ4Factory.safeInstance();
        LZ4Codec codec = new LZ4Codec(log);
        Random random = new Random(42);
        for (int n : new int[]{0, 1, 12, 13, 15, 270, 4096, 70000, 1048576}) {
            for (boolean repeated : new boolean[]{false, true}) {
                byte[] input = new byte[n];
                random.nextBytes(input);
                if (repeated) for (int p = 31; p < n; p++) input[p] = input[p % 31];
                byte[] ours = codec.compress(input);
                byte[] decoded = new byte[n];
                Assertions.assertEquals(n, oracle.safeDecompressor().decompress(ours, 16, ours.length - 16, decoded, 0, n));
                Assertions.assertArrayEquals(input, decoded);
                byte[] other = oracle.fastCompressor().compress(input);
                Assertions.assertArrayEquals(input, codec.decompress(container(n, other)));
                byte[] high = oracle.highCompressor().compress(input);
                Assertions.assertArrayEquals(input, codec.decompress(container(n, high)));
            }
        }
    }

    @Test void handCraftedOffsetsAndMaximumDistance() {
        LZ4Codec codec = new LZ4Codec();
        // 8 literals, 7-byte match offset=8, 5 final literals; last match at size-12.
        Assertions.assertArrayEquals("abcdefghabcdefg12345".getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                codec.decompress(container(20, (byte) 0x83, (byte)'a', (byte)'b', (byte)'c', (byte)'d',
                        (byte)'e', (byte)'f', (byte)'g', (byte)'h', (byte)8, (byte)0,
                        (byte)0x50, (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5')));
        // Force a valid 65535 offset independently of the encoder hash collisions.
        java.io.ByteArrayOutputStream block = new java.io.ByteArrayOutputStream();
        block.write(0xf3); // 65535 literals, 7-byte match
        int extra = 65535 - 15;
        while (extra >= 255) { block.write(255); extra -= 255; }
        block.write(extra);
        byte[] literals = new byte[65535]; new Random(7).nextBytes(literals);
        block.write(literals, 0, literals.length);
        block.write(255); block.write(255);
        block.write(0x50);
        for (int i = 0; i < 5; i++) block.write(i);
        byte[] decoded = codec.decompress(container(65547, block.toByteArray()));
        Assertions.assertArrayEquals(Arrays.copyOf(literals, 7), Arrays.copyOfRange(decoded, 65535, 65542));
        Assertions.assertEquals(7, codec.analyze(container(65547, block.toByteArray())).getAverageMatchLength());
    }

    @Test void rejectsCorruptHeadersAndTruncations() {
        LZ4Codec codec = new LZ4Codec();
        byte[] good = codec.compress(new byte[1024]);
        for (int n = 0; n < good.length; n++) {
            byte[] truncated = Arrays.copyOf(good, n);
            Assertions.assertThrows(IllegalArgumentException.class, () -> codec.decompress(truncated));
        }
        byte[] magic = good.clone(); magic[0] = 0;
        Assertions.assertThrows(IllegalArgumentException.class, () -> codec.decompress(magic));
        for (long n : new long[]{-1, 1, 1023, 1025, 67108865L, Long.MAX_VALUE}) {
            byte[] bad = good.clone(); ByteBuffer.wrap(bad).putLong(4, n);
            Assertions.assertThrows(IllegalArgumentException.class, () -> codec.decompress(bad));
        }
        byte[] trailing = Arrays.copyOf(good, good.length + 1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> codec.decompress(trailing));
    }

    @Test void rejectsMalformedBlocksWithConsistentHeader() {
        LZ4Codec codec = new LZ4Codec();
        byte[][] blocks = {
            {(byte)0xf0}, // truncated literal extension
            {(byte)0xf0, (byte)255},
            {0x20, 1}, // truncated literal
            {0x10, 65, 0}, // truncated offset
            {0x10, 65, 0, 0, 0x50, 1, 2, 3, 4, 5}, // offset zero
            {0x10, 65, 2, 0, 0x50, 1, 2, 3, 4, 5}, // offset before output
            {0x1f, 65, 1, 0}, // truncated match extension
            {0x10, 65, 1, 0}, // missing tail
            {0x10, 65, 1, 0, 0x40, 1, 2, 3, 4}, // only four final literals
            {0x10, 65, 1, 0, 0x50, 1, 2, 3, 4, 5} // match starts less than 12 before end
        };
        for (byte[] block : blocks) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> codec.decompress(container(10, block)));
        }
        byte[] bomb = new byte[9_000_000];
        bomb[0] = (byte)0xf0; Arrays.fill(bomb, 1, bomb.length, (byte)255);
        Assertions.assertThrows(IllegalArgumentException.class, () -> codec.decompress(container(67108864, bomb)));
    }

    @Test void parametersAndNulls() {
        Assertions.assertEquals(12, new LZ4Codec().getHashLog());
        Assertions.assertEquals("LZ4", new LZ4Codec().getName());
        for (int log : new int[]{0, 11, 13, 15, 17, 32}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> new LZ4Codec(log));
        }
        Assertions.assertThrows(NullPointerException.class, () -> new LZ4Codec().compress(null));
        Assertions.assertThrows(NullPointerException.class, () -> new LZ4Codec().decompress(null));
    }

    @ParameterizedTest @ValueSource(ints = {12, 16})
    void full64MiBBoundary(int log) {
        LZ4Codec codec = new LZ4Codec(log);
        byte[] input = new byte[codec.getMaxInputSize()];
        new Random(64).nextBytes(input);
        byte[] encoded = codec.compress(input);
        Assertions.assertArrayEquals(input, codec.decompress(encoded));
        Arrays.fill(input, (byte)0xff);
        encoded = codec.compress(input);
        Assertions.assertArrayEquals(input, codec.decompress(encoded));
        Assertions.assertThrows(IllegalArgumentException.class, () -> codec.compress(new byte[codec.getMaxInputSize() + 1]));
    }
}
