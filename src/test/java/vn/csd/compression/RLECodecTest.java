package vn.csd.compression;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vn.csd.compression.codec.RLECodec;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

class RLECodecTest {
    private final RLECodec codec = new RLECodec();

    @Test
    void T01_emptyInputRoundTrips() throws Exception {
        byte[] input = new byte[0];
        byte[] compressed = codec.compress(input);

        Assertions.assertEquals("RLE", codec.getName());
        Assertions.assertEquals(12, compressed.length);
        Assertions.assertArrayEquals(new byte[]{'R', 'L', 'E', '1'},
                Arrays.copyOfRange(compressed, 0, 4));
        Assertions.assertArrayEquals(input, codec.decompress(compressed));
        assertSameSha256(input, codec.decompress(compressed));
        Assertions.assertEquals(0L, ByteBuffer.wrap(compressed, 4, 8).getLong());
    }

    @Test
    void T02_oneByteRoundTrips() throws Exception {
        byte[] input = {(byte) 0xa5};
        byte[] compressed = codec.compress(input);

        Assertions.assertArrayEquals(new byte[]{1, (byte) 0xa5},
                Arrays.copyOfRange(compressed, 12, compressed.length));
        assertRoundTripAndHash(input, compressed);
    }

    @Test
    void T03_oneMiBOfTheSameByteSplitsIntoValidRuns() throws Exception {
        byte[] input = new byte[1024 * 1024];
        Arrays.fill(input, (byte) 0xaa);
        byte[] compressed = codec.compress(input);

        assertRunsAreAtMost255(compressed);
        assertRoundTripAndHash(input, compressed);
    }

    @Test
    void T07_runLengths255And256HaveExactPairs() throws Exception {
        byte[] input255 = new byte[255];
        Arrays.fill(input255, (byte) 0x7e);
        byte[] compressed255 = codec.compress(input255);
        Assertions.assertArrayEquals(new byte[]{(byte) 255, 0x7e},
                Arrays.copyOfRange(compressed255, 12, compressed255.length));
        assertRoundTripAndHash(input255, compressed255);

        byte[] input256 = new byte[256];
        Arrays.fill(input256, (byte) 0x7e);
        byte[] compressed256 = codec.compress(input256);
        Assertions.assertArrayEquals(new byte[]{(byte) 255, 0x7e, 1, 0x7e},
                Arrays.copyOfRange(compressed256, 12, compressed256.length));
        assertRoundTripAndHash(input256, compressed256);
    }

    @Test
    void T09_wrongMagicIsRejected() {
        byte[] compressed = codec.compress(new byte[]{1, 2, 3});
        compressed[0] = (byte) 'X';

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class, () -> codec.decompress(compressed));
        Assertions.assertTrue(exception.getMessage().contains("Bad RLE1 magic"));
    }

    @Test
    void T12_seededRandomInputsAtMultipleSizesMatchSha256() throws Exception {
        Random random = new Random(20261021L);
        int[] sizes = {0, 1, 2, 254, 255, 256, 4096, 1024 * 1024};
        for (int size : sizes) {
            byte[] input = new byte[size];
            random.nextBytes(input);
            assertRoundTripAndHash(input, codec.compress(input));
        }
    }

    @Test
    void malformedRunPayloadsAreRejected() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> codec.decompress(new byte[11]));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> codec.decompress(container(-1, new byte[0])));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> codec.decompress(container((long) Integer.MAX_VALUE + 1, new byte[0])));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> codec.decompress(container(1, (byte) 1)));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> codec.decompress(container(2, (byte) 1, (byte) 0x11)));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> codec.decompress(container(0, (byte) 1, (byte) 0x11)));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> codec.decompress(container(1, (byte) 0, (byte) 0x11)));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> codec.decompress(container(1, (byte) 2, (byte) 0x11)));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> codec.decompress(container(1, (byte) 1, (byte) 0x11, (byte) 1)));
    }

    private void assertRoundTripAndHash(byte[] input, byte[] compressed) throws Exception {
        byte[] output = codec.decompress(compressed);
        Assertions.assertArrayEquals(input, output);
        assertSameSha256(input, output);
    }

    private void assertSameSha256(byte[] input, byte[] output) throws Exception {
        byte[] expected = MessageDigest.getInstance("SHA-256").digest(input);
        byte[] actual = MessageDigest.getInstance("SHA-256").digest(output);
        Assertions.assertArrayEquals(expected, actual);
    }

    private void assertRunsAreAtMost255(byte[] compressed) {
        Assertions.assertEquals(0, (compressed.length - 12) % 2);
        for (int position = 12; position < compressed.length; position += 2) {
            int count = compressed[position] & 0xff;
            Assertions.assertTrue(count >= 1 && count <= 255);
        }
    }

    private byte[] container(long originalLength, byte... payload) {
        ByteBuffer buffer = ByteBuffer.allocate(12 + payload.length);
        buffer.putInt(0x524c4531).putLong(originalLength).put(payload);
        return buffer.array();
    }
}
