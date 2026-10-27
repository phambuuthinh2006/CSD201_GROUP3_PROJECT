package vn.csd.compression.codec;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Thuật toán nén RLE (Run-Length Encoding).
 * Phụ trách: TV1 (Cặp 1)
 */
public class RLECodec implements Codec {
    private static final int MAGIC = 0x524c4531;
    private static final int HEADER_SIZE = 12;
    private static final int MAX_RUN_LENGTH = 255;

    @Override
    public String getName() {
        return "rle";
    }

    @Override
    public byte[] compress(byte[] input) {
        Objects.requireNonNull(input, "input");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] header = ByteBuffer.allocate(HEADER_SIZE)
                .putInt(MAGIC)
                .putLong(input.length)
                .array();
        output.write(header, 0, header.length);

        int position = 0;
        while (position < input.length) {
            byte value = input[position];
            int count = 1;
            while (count < MAX_RUN_LENGTH
                    && count < input.length - position
                    && input[position + count] == value) {
                count++;
            }

            output.write(count);
            output.write(value & 0xff);
            position += count;
        }

        return output.toByteArray();
    }

    @Override
    public byte[] decompress(byte[] compressed) {
        Objects.requireNonNull(compressed, "compressed");
        if (compressed.length < HEADER_SIZE) {
            throw invalid("Truncated header");
        }

        ByteBuffer header = ByteBuffer.wrap(compressed);
        if (header.getInt() != MAGIC) {
            throw invalid("Bad RLE1 magic");
        }

        long originalLength = header.getLong();
        if (originalLength < 0 || originalLength > Integer.MAX_VALUE) {
            throw invalid("Invalid original length");
        }

        int payloadLength = compressed.length - HEADER_SIZE;
        if ((payloadLength & 1) != 0) {
            throw invalid("Truncated run pair");
        }

        long decodedLength = 0;
        for (int position = HEADER_SIZE; position < compressed.length; position += 2) {
            int count = compressed[position] & 0xff;
            if (count == 0) {
                throw invalid("Run count must be between 1 and 255");
            }
            decodedLength += count;
            if (decodedLength > originalLength) {
                throw invalid("Runs exceed original length");
            }
        }
        if (decodedLength != originalLength) {
            throw invalid("Original length mismatch");
        }

        byte[] output = new byte[(int) originalLength];
        int outputPosition = 0;
        for (int position = HEADER_SIZE; position < compressed.length; position += 2) {
            int count = compressed[position] & 0xff;
            byte value = compressed[position + 1];
            for (int i = 0; i < count; i++) {
                output[outputPosition++] = value;
            }
        }

        return output;
    }

    private IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("Invalid RLE1: " + message);
    }
}
