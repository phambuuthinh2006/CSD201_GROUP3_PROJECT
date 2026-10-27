package vn.csd.compression;

import org.junit.jupiter.api.Test;
import vn.csd.compression.codec.RLECodec;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bộ kiểm thử JUnit 5 cho RLECodec.
 * Phụ trách: TV1 (Cặp 1)
 */
class RLECodecTest {
    private final RLECodec codec = new RLECodec();

    @Test
    void T01_emptyInputRoundTripsWithHeaderOnly() throws Exception {
        byte[] input = new byte[0];
        byte[] encoded = codec.compress(input);

        assertEquals("rle", codec.getName());
        assertEquals(12, encoded.length);
        assertArrayEquals(new byte[]{'R', 'L', 'E', '1'},
                Arrays.copyOfRange(encoded, 0, 4));
        assertEquals(0L, ByteBuffer.wrap(encoded, 4, 8).getLong());
        assertRoundTripAndHash(input, encoded);
    }

    @Test
    void T02_oneByteRoundTrips() throws Exception {
        byte[] input = {(byte) 0xa5};
        byte[] encoded = codec.compress(input);

        assertArrayEquals(new byte[]{1, (byte) 0xa5},
                Arrays.copyOfRange(encoded, 12, encoded.length));
        assertRoundTripAndHash(input, encoded);
    }

    @Test
    void T03_oneMiBRepeatedByteUsesOnlyValidRunCounts() throws Exception {
        byte[] input = repeated(1024 * 1024, (byte) 0xaa);
        byte[] encoded = codec.compress(input);

        assertEquals(0, (encoded.length - 12) % 2);
        for (int position = 12; position < encoded.length; position += 2) {
            int count = encoded[position] & 0xff;
            assertTrue(count >= 1 && count <= 255);
        }
        assertRoundTripAndHash(input, encoded);
    }

    @Test
    void T07_runLengths255And256AreEncodedAsExpected() throws Exception {
        byte[] input255 = repeated(255, (byte) 0x7e);
        byte[] encoded255 = codec.compress(input255);
        assertArrayEquals(new byte[]{(byte) 255, 0x7e},
                Arrays.copyOfRange(encoded255, 12, encoded255.length));
        assertRoundTripAndHash(input255, encoded255);

        byte[] input256 = repeated(256, (byte) 0x7e);
        byte[] encoded256 = codec.compress(input256);
        assertArrayEquals(new byte[]{(byte) 255, 0x7e, 1, 0x7e},
                Arrays.copyOfRange(encoded256, 12, encoded256.length));
        assertRoundTripAndHash(input256, encoded256);
    }

    @Test
    void T09_wrongMagicIsRejected() {
        byte[] encoded = codec.compress(new byte[]{1, 2, 3});
        encoded[0] = 'X';

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> codec.decompress(encoded));
        assertTrue(exception.getMessage().contains("Bad RLE1 magic"));
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
    void malformedContainersAndNullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> codec.compress(null));
        assertThrows(NullPointerException.class, () -> codec.decompress(null));
        assertThrows(IllegalArgumentException.class, () -> codec.decompress(new byte[11]));
        assertThrows(IllegalArgumentException.class, () -> codec.decompress(container(-1, new byte[0])));
        assertThrows(IllegalArgumentException.class,
                () -> codec.decompress(container((long) Integer.MAX_VALUE + 1, new byte[0])));
        assertThrows(IllegalArgumentException.class, () -> codec.decompress(container(1, new byte[]{1})));
        assertThrows(IllegalArgumentException.class,
                () -> codec.decompress(container(1, new byte[]{0, 0x11})));
        assertThrows(IllegalArgumentException.class,
                () -> codec.decompress(container(2, new byte[]{1, 0x11})));
        assertThrows(IllegalArgumentException.class,
                () -> codec.decompress(container(1, new byte[]{2, 0x11})));
        assertThrows(IllegalArgumentException.class,
                () -> codec.decompress(container(0, new byte[]{1, 0x11})));
    }

    private void assertRoundTripAndHash(byte[] input, byte[] encoded) throws Exception {
        byte[] decoded = codec.decompress(encoded);
        assertArrayEquals(input, decoded);
        assertArrayEquals(
                MessageDigest.getInstance("SHA-256").digest(input),
                MessageDigest.getInstance("SHA-256").digest(decoded));
    }

    private byte[] repeated(int length, byte value) {
        byte[] result = new byte[length];
        Arrays.fill(result, value);
        return result;
    }

    private byte[] container(long originalLength, byte[] payload) {
        ByteBuffer buffer = ByteBuffer.allocate(12 + payload.length);
        buffer.putInt(0x524c4531).putLong(originalLength).put(payload);
        return buffer.array();
    }
}
