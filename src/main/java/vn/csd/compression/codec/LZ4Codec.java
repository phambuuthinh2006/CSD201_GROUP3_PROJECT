package vn.csd.compression.codec;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/** Independent LZ4 block in an LZ4B container; lengths in header are big-endian. */
public final class LZ4Codec implements Codec {
    public static final int DEFAULT_HASH_LOG = 12;
    public static final int MAX_INPUT_SIZE = 64 * 1024 * 1024;
    public static final int HEADER_SIZE = 16;
    private static final int MAGIC = 0x4c5a3442;
    private final int hashLog;

    public LZ4Codec() { this(DEFAULT_HASH_LOG); }

    public LZ4Codec(int hashLog) {
        if (hashLog != 12 && hashLog != 16) {
            throw new IllegalArgumentException("hashLog must be 12 or 16");
        }
        this.hashLog = hashLog;
    }

    public int getHashLog() { return hashLog; }
    @Override public String getName() { return "LZ4"; }

    @Override public byte[] compress(byte[] input) {
        Objects.requireNonNull(input, "input");
        int n = input.length;
        if (n > MAX_INPUT_SIZE) throw new IllegalArgumentException("Input exceeds 64 MiB");
        int[] table = new int[1 << hashLog];
        Arrays.fill(table, -1);
        ByteArrayOutputStream out = new ByteArrayOutputStream(n + n / 255 + 32);
        for (int i = 0; i < HEADER_SIZE; i++) out.write(0);
        int anchor = 0;
        int pos = 0;
        while (pos <= n - 12) {
            int h = hash(input, pos);
            int candidate = table[h];
            table[h] = pos;
            if (candidate < 0 || pos - candidate > 65535
                    || readIntLE(input, candidate) != readIntLE(input, pos)) {
                pos++;
                continue;
            }
            int length = 4;
            while (pos + length < n - 5 && input[candidate + length] == input[pos + length]) {
                length++;
            }
            int literals = pos - anchor;
            int matchCode = length - 4;
            out.write((Math.min(literals, 15) << 4) | Math.min(matchCode, 15));
            if (literals >= 15) writeLength(out, literals - 15);
            out.write(input, anchor, literals);
            int offset = pos - candidate;
            out.write(offset & 255);
            out.write(offset >>> 8);
            if (matchCode >= 15) writeLength(out, matchCode - 15);
            int end = pos + length;
            // Refresh positions within the match, retaining the nearest candidate per hash.
            for (int p = pos + 1; p < end && p <= n - 12; p++) table[hash(input, p)] = p;
            pos = end;
            anchor = end;
        }
        int tail = n - anchor;
        out.write(Math.min(tail, 15) << 4);
        if (tail >= 15) writeLength(out, tail - 15);
        out.write(input, anchor, tail);
        byte[] result = out.toByteArray();
        ByteBuffer.wrap(result).putInt(MAGIC).putLong(n).putInt(result.length - HEADER_SIZE);
        return result;
    }

    @Override public byte[] decompress(byte[] compressed) {
        int size = originalSize(compressed);
        // Validate before allocating the advertised output; reject malformed blocks early.
        parse(compressed, size, null);
        byte[] result = new byte[size];
        parse(compressed, size, result);
        return result;
    }

    /** Parse outside timed compression/decompression to collect RQ3 statistics. */
    public BlockStatistics analyze(byte[] compressed) {
        return parse(compressed, originalSize(compressed), null);
    }

    private static int originalSize(byte[] data) {
        Objects.requireNonNull(data, "compressed");
        if (data.length < HEADER_SIZE + 1) throw invalid("Truncated header/block");
        ByteBuffer header = ByteBuffer.wrap(data);
        if (header.getInt() != MAGIC) throw invalid("Bad LZ4B magic");
        long length = header.getLong();
        if (length < 0 || length > MAX_INPUT_SIZE) throw invalid("Invalid original length");
        if (header.getInt() != data.length - HEADER_SIZE) throw invalid("Block length mismatch");
        return (int) length;
    }

    private static BlockStatistics parse(byte[] data, int size, byte[] output) {
        Cursor cursor = new Cursor(HEADER_SIZE);
        int written = 0;
        int sequences = 0;
        int matches = 0;
        long matchBytes = 0;
        int lastMatchStart = -1;
        while (cursor.pos < data.length) {
            int token = data[cursor.pos++] & 255;
            int literals = readLength(data, cursor, token >>> 4, size - written);
            if (literals > data.length - cursor.pos) throw invalid("Truncated literals");
            if (output != null) System.arraycopy(data, cursor.pos, output, written, literals);
            cursor.pos += literals;
            written += literals;
            sequences++;
            if (cursor.pos == data.length) {
                if (written != size) throw invalid("Original length mismatch");
                if (literals < Math.min(size, 5)) throw invalid("Missing final literals");
                if (lastMatchStart >= 0 && size - lastMatchStart < 12) throw invalid("Last match too late");
                return new BlockStatistics(sequences, matches, matchBytes);
            }
            if (data.length - cursor.pos < 2) throw invalid("Truncated offset");
            int offset = (data[cursor.pos] & 255) | ((data[cursor.pos + 1] & 255) << 8);
            cursor.pos += 2;
            if (offset == 0 || offset > written) throw invalid("Invalid match offset");
            if (size - written < 4) throw invalid("Match exceeds output");
            int length = 4 + readLength(data, cursor, token & 15, size - written - 4);
            lastMatchStart = written;
            if (output != null) {
                // Forward copy is necessary for overlapping matches (e.g. offset=1).
                for (int i = 0; i < length; i++) output[written + i] = output[written + i - offset];
            }
            written += length;
            matches++;
            matchBytes += length;
        }
        throw invalid("Missing final literal sequence");
    }

    private static int readLength(byte[] data, Cursor cursor, int base, int limit) {
        int length = base;
        if (length > limit) throw invalid("Length exceeds output");
        if (base == 15) {
            int extra;
            do {
                if (cursor.pos >= data.length) throw invalid("Truncated extension length");
                extra = data[cursor.pos++] & 255;
                if (extra > limit - length) throw invalid("Length exceeds output");
                length += extra;
            } while (extra == 255);
        }
        return length;
    }

    private int hash(byte[] input, int pos) {
        return (readIntLE(input, pos) * 0x9e3779b1) >>> (32 - hashLog);
    }

    private static int readIntLE(byte[] a, int p) {
        return (a[p] & 255) | ((a[p + 1] & 255) << 8)
                | ((a[p + 2] & 255) << 16) | ((a[p + 3] & 255) << 24);
    }

    private static void writeLength(ByteArrayOutputStream out, int length) {
        while (length >= 255) { out.write(255); length -= 255; }
        out.write(length);
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("Invalid LZ4B: " + message);
    }

    private static final class Cursor {
        private int pos;
        private Cursor(int pos) { this.pos = pos; }
    }

    public static final class BlockStatistics {
        private final int sequenceCount;
        private final int matchCount;
        private final long totalMatchLength;
        private BlockStatistics(int sequences, int matches, long bytes) {
            sequenceCount = sequences; matchCount = matches; totalMatchLength = bytes;
        }
        /** Includes the final literal-only sequence (empty input has one sequence). */
        public int getSequenceCount() { return sequenceCount; }
        public int getMatchCount() { return matchCount; }
        public long getTotalMatchLength() { return totalMatchLength; }
        public double getAverageMatchLength() {
            return matchCount == 0 ? 0.0 : (double) totalMatchLength / matchCount;
        }
    }
}
