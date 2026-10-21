package vn.csd.compression.codec;

/**
 * Thuật toán nén RLE (Run-Length Encoding).
 * Phụ trách: TV1 (Cặp 1)
 */
public class RLECodec implements Codec {

    @Override
    public String getName() {
        return "rle";
    }

    @Override
    public byte[] compress(byte[] input) {
        // TODO (TV1): Cài đặt bộ nén RLE
        throw new UnsupportedOperationException("Chưa cài đặt RLECodec");
    }

    @Override
    public byte[] decompress(byte[] compressed) {
        // TODO (TV1): Cài đặt bộ giải nén RLE
        throw new UnsupportedOperationException("Chưa cài đặt RLECodec");
    }
}
