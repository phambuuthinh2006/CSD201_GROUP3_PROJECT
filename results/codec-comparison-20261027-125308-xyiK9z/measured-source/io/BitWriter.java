package vn.csd.compression.io;

import java.io.ByteArrayOutputStream;

/**
 * Công cụ đóng gói chuỗi bit vào mảng byte theo thứ tự bit cao trước (MSB-first).
 * Sử dụng bộ đệm bit dạng long theo đặc tả Đề tài 4 (CSD201).
 * Phụ trách: TV3 (Cặp 2)
 */
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

    /**
     * Ghi 1 bit (0 hoặc 1) vào luồng bit.
     */
    public void writeBit(int bit) {
        writeBits(bit & 1, 1);
    }

    /**
     * Ghi numBits bit từ value vào luồng bit theo thứ tự MSB-first.
     * numBits từ 1 đến 32.
     * Bit cao nhất của value (tại vị trí numBits - 1) sẽ được ghi trước.
     */
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

        // Đẩy từng byte đầy (8-bit) từ MSB ra output stream
        while (bitCount >= 8) {
            int byteVal = (int) ((bitBuffer >>> (bitCount - 8)) & 0xFFL);
            out.write(byteVal);
            bitCount -= 8;
        }

        // Giữ lại đúng bitCount bit chưa ghi trong bitBuffer
        if (bitCount > 0) {
            bitBuffer &= (1L << bitCount) - 1L;
        } else {
            bitBuffer = 0L;
        }
    }

    /**
     * Xả bộ đệm: nếu còn các bit lẻ dở dang (< 8 bit),
     * các bit đó được căn trái (MSB) và bù các bit 0 vào phần còn lại của byte cuối.
     */
    public void flush() {
        if (bitCount > 0) {
            int byteVal = (int) ((bitBuffer << (8 - bitCount)) & 0xFFL);
            out.write(byteVal);
            bitBuffer = 0L;
            bitCount = 0;
        }
    }

    /**
     * Trả về mảng byte đã hoàn chỉnh sau khi flush bù 0.
     */
    public byte[] toByteArray() {
        flush();
        return out.toByteArray();
    }

    /**
     * Trả về tổng số bit thực tế đã ghi vào luồng (không tính các bit 0 padding cuối).
     * Phục vụ kiểm thử T10: Huffman payload bits = Σ (frequency × codeLength).
     */
    public long getTotalBitsWritten() {
        return totalBitsWritten;
    }

    /**
     * Trả về số byte hiện tại đã ghi ra stream (chưa tính các bit đang nằm trong buffer chưa flush).
     */
    public int size() {
        return out.size();
    }
}
