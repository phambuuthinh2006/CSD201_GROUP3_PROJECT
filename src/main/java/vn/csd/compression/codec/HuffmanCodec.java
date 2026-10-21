package vn.csd.compression.codec;

/**
 * Thuật toán nén Huffman tĩnh hai lượt (Canonical Huffman).
 * Phụ trách: TV3 (Cặp 2)
 */
public class HuffmanCodec implements Codec {

    @Override
    public String getName() {
        return "huffman";
    }

    @Override
    public byte[] compress(byte[] input) {
        // TODO (TV3): Cài đặt bộ nén Huffman canonical
        throw new UnsupportedOperationException("Chưa cài đặt HuffmanCodec");
    }

    @Override
    public byte[] decompress(byte[] compressed) {
        // TODO (TV3): Cài đặt bộ giải nén Huffman canonical
        throw new UnsupportedOperationException("Chưa cài đặt HuffmanCodec");
    }
}
