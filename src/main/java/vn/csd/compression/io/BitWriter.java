package vn.csd.compression.io;

import java.io.ByteArrayOutputStream;

public class BitWriter {

    private final ByteArrayOutputStream out;
    private long bitBuffer;
    private int bitCount;
    private long totalBitsWritten;

    public BitWriter() {
        this.out = new ByteArrayOutputStream();
        this.bitBuffer = 0L;
        this.bitCount = 0;
        this.totalBitsWritten = 0L;
    }

    public BitWriter(int initialCapacity) {
        this.out = new ByteArrayOutputStream(Math.max(initialCapacity, 32));
        this.bitBuffer = 0L;
        this.bitCount = 0;
        this.totalBitsWritten = 0L;
    }

  
    public void writeBit(int bit) {
        writeBits(bit & 1, 1);
    }

    
    public void writeBits(int value, int numBits) {
        if (numBits <= 0) {
            return;
        }
        if (numBits > 32) {
            throw new IllegalArgumentException("numBits must be between 1 and 32");
        }

        long mask = (numBits == 32) ? 0xFFFFFFFFL : ((1L << numBits) - 1L);
        long bits = value & mask;

        bitBuffer = (bitBuffer << numBits) | bits;
        bitCount += numBits;
        totalBitsWritten += numBits;

       
        while (bitCount >= 8) {
            int byteVal = (int) ((bitBuffer >>> (bitCount - 8)) & 0xFFL);
            out.write(byteVal);
            bitCount -= 8;
        }

       
        if (bitCount > 0) {
            bitBuffer &= (1L << bitCount) - 1L;
        } else {
            bitBuffer = 0L;
        }
    }

   
    public void flush() {
        if (bitCount > 0) {
            int byteVal = (int) ((bitBuffer << (8 - bitCount)) & 0xFFL);
            out.write(byteVal);
            bitBuffer = 0L;
            bitCount = 0;
        }
    }
  
    public byte[] toByteArray() {
        flush();
        return out.toByteArray();
    }
  
    public long getTotalBitsWritten() {
        return totalBitsWritten;
    }

    public int size() {
        return out.size();
    }
}
