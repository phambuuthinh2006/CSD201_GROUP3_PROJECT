package vn.csd.compression.io;

public class BitReader {

    private final byte[] data;
    private final int offset;
    private final int length;
    private int bytePos;
    private long bitBuffer;
    private int bitCount;
    private long totalBitsRead;

    public BitReader(byte[] data) {
        this(data, 0, data != null ? data.length : 0);
    }

    public BitReader(byte[] data, int offset, int length) {
        if (data == null) {
            throw new NullPointerException("data cannot be null");
        }
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new IndexOutOfBoundsException("Invalid offset or length for BitReader");
        }
        this.data = data;
        this.offset = offset;
        this.length = length;
        this.bytePos = offset;
        this.bitBuffer = 0L;
        this.bitCount = 0;
        this.totalBitsRead = 0L;
    }

   
    private void refill() {
        while (bitCount <= 48 && bytePos < offset + length) {
            int b = data[bytePos++] & 0xFF;
            bitBuffer = (bitBuffer << 8) | b;
            bitCount += 8;
        }
    }

    public int readBit() {
        if (bitCount == 0) {
            if (bytePos >= offset + length) {
                return -1;
            }
            refill();
        }
        bitCount--;
        totalBitsRead++;
        int bit = (int) ((bitBuffer >>> bitCount) & 1L);
        if (bitCount == 0) {
            bitBuffer = 0L;
        }
        return bit;
    }


    public int readBits(int numBits) {
        if (numBits <= 0) {
            return 0;
        }
        if (numBits > 32) {
            throw new IllegalArgumentException("numBits must be between 1 and 32");
        }

        while (bitCount < numBits) {
            if (bytePos >= offset + length) {
                throw new IllegalArgumentException("Unexpected end of bitstream (needed " + numBits + " bits, have " + bitCount + ")");
            }
            refill();
        }

        bitCount -= numBits;
        totalBitsRead += numBits;
        long mask = (numBits == 32) ? 0xFFFFFFFFL : ((1L << numBits) - 1L);
        int result = (int) ((bitBuffer >>> bitCount) & mask);
        if (bitCount == 0) {
            bitBuffer = 0L;
        }
        return result;
    }

   
    public boolean hasRemainingBits() {
        return bitCount > 0 || bytePos < offset + length;
    }


    public long getTotalBitsRead() {
        return totalBitsRead;
    }
}
