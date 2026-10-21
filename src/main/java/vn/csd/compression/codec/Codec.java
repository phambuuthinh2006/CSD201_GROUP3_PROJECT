package vn.csd.compression.codec;

/**
 * Interface chung cho 4 codec nén dữ liệu: RLE, Huffman, LZW, LZ4.
 * Tất cả các codec phải cài đặt interface này theo quy định đề tài.
 */
public interface Codec {
    /**
     * Nén mảng byte dữ liệu gốc thành mảng byte đã nén (gồm cả header, payload).
     *
     * @param input Dữ liệu gốc cần nén
     * @return Dữ liệu đã nén
     */
    byte[] compress(byte[] input);

    /**
     * Giải nén dữ liệu đã nén trở về dữ liệu gốc.
     * Phải kiểm tra magic header; từ chối nếu magic không khớp.
     *
     * @param compressed Dữ liệu đã nén
     * @return Dữ liệu gốc sau giải nén
     */
    byte[] decompress(byte[] compressed);

    /**
     * Tên định danh của codec (rle, huffman, lzw, lz4).
     */
    String getName();
}
