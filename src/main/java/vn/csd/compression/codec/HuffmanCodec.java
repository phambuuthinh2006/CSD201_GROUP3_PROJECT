package vn.csd.compression.codec;

import vn.csd.compression.io.BitReader;
import vn.csd.compression.io.BitWriter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;


public class HuffmanCodec implements Codec {

    public static final byte[] MAGIC = new byte[]{'H', 'U', 'F', '1'};
    public static final int HEADER_SIZE = 268; // 4 byte magic + 8 byte length + 256 byte code lengths
    public static final int MAX_INPUT_SIZE = 64 * 1024 * 1024; // 64 MiB

    @Override
    public String getName() {
        return "huffman";
    }

   
    @Override
    public byte[] compress(byte[] input) {
        Objects.requireNonNull(input, "Input cannot be null");
        if (input.length > MAX_INPUT_SIZE) {
            throw new IllegalArgumentException("Input exceeds maximum allowed size of 64 MiB");
        }

        
        if (input.length == 0) {
            byte[] headerOnly = new byte[HEADER_SIZE];
            System.arraycopy(MAGIC, 0, headerOnly, 0, 4);
            return headerOnly;
        }

    
        long[] freq = new long[256];
        for (byte b : input) {
            freq[b & 0xFF]++;
        }

      
        int[] codeLengths = new int[256];
        int distinctCount = 0;
        int singleSymbol = -1;
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                distinctCount++;
                singleSymbol = i;
            }
        }

        if (distinctCount == 1) {
            codeLengths[singleSymbol] = 1;
        } else {
            buildTreeAndComputeLengths(freq, codeLengths);
        }

        
        List<CanonicalSymbol> canonicalList = generateCanonicalCodes(codeLengths);
        int[] symbolCode = new int[256];
        int[] symbolLen = new int[256];
        for (CanonicalSymbol cs : canonicalList) {
            symbolCode[cs.symbol] = cs.code;
            symbolLen[cs.symbol] = cs.length;
        }

        
        BitWriter bitWriter = new BitWriter(Math.min(input.length + 512, MAX_INPUT_SIZE));
        for (byte b : input) {
            int s = b & 0xFF;
            bitWriter.writeBits(symbolCode[s], symbolLen[s]);
        }
        byte[] payload = bitWriter.toByteArray();

        
        byte[] compressed = new byte[HEADER_SIZE + payload.length];
        System.arraycopy(MAGIC, 0, compressed, 0, 4);

        ByteBuffer.wrap(compressed, 4, 8).putLong(input.length);

        for (int i = 0; i < 256; i++) {
            compressed[12 + i] = (byte) (codeLengths[i] & 0xFF);
        }

        if (payload.length > 0) {
            System.arraycopy(payload, 0, compressed, HEADER_SIZE, payload.length);
        }

        return compressed;
    }

   
    @Override
    public byte[] decompress(byte[] compressed) {
        Objects.requireNonNull(compressed, "Compressed data cannot be null");
        if (compressed.length < HEADER_SIZE) {
            throw new IllegalArgumentException("Compressed data is too short for Huffman header (minimum 268 bytes)");
        }

   
        for (int i = 0; i < 4; i++) {
            if (compressed[i] != MAGIC[i]) {
                throw new IllegalArgumentException("Invalid Huffman magic header: expected HUF1");
            }
        }

      
        long origLen = ByteBuffer.wrap(compressed, 4, 8).getLong();
        if (origLen < 0 || origLen > MAX_INPUT_SIZE) {
            throw new IllegalArgumentException("Invalid original length in header: " + origLen);
        }


        if (origLen == 0) {
            return new byte[0];
        }

    
        int[] codeLengths = new int[256];
        int distinctCount = 0;
        int singleSymbol = -1;
        for (int i = 0; i < 256; i++) {
            codeLengths[i] = compressed[12 + i] & 0xFF;
            if (codeLengths[i] > 0) {
                distinctCount++;
                singleSymbol = i;
            }
        }

        if (distinctCount == 0) {
            throw new IllegalArgumentException("Corrupted Huffman code table: no symbols defined");
        }

       
        if (distinctCount == 1) {
            if (codeLengths[singleSymbol] != 1) {
                throw new IllegalArgumentException("Single symbol Huffman code must have length 1");
            }
            BitReader bitReader = new BitReader(compressed, HEADER_SIZE, compressed.length - HEADER_SIZE);
            byte[] output = new byte[(int) origLen];
            for (int i = 0; i < (int) origLen; i++) {
                int bit = bitReader.readBit();
                if (bit != 0) {
                    throw new IllegalArgumentException("Corrupted bitstream for single symbol Huffman");
                }
                output[i] = (byte) singleSymbol;
            }
            return output;
        }

      
        List<CanonicalSymbol> canonicalList = generateCanonicalCodes(codeLengths);
        DecodeNode root = buildDecodeTree(canonicalList);


        BitReader bitReader = new BitReader(compressed, HEADER_SIZE, compressed.length - HEADER_SIZE);
        byte[] output = new byte[(int) origLen];

        for (int i = 0; i < (int) origLen; i++) {
            DecodeNode curr = root;
            while (curr.symbol == -1) {
                int bit = bitReader.readBit();
                if (bit == -1) {
                    throw new IllegalArgumentException("Premature end of bitstream at symbol index " + i + " / " + origLen);
                }
                curr = (bit == 0) ? curr.left : curr.right;
                if (curr == null) {
                    throw new IllegalArgumentException("Corrupted Huffman bitstream: invalid code sequence");
                }
            }
            output[i] = (byte) curr.symbol;
        }

        return output;
    }

    
    public int[] computeCodeLengths(byte[] input) {
        Objects.requireNonNull(input, "Input cannot be null");
        int[] codeLengths = new int[256];
        if (input.length == 0) {
            return codeLengths;
        }

        long[] freq = new long[256];
        for (byte b : input) {
            freq[b & 0xFF]++;
        }

        int distinctCount = 0;
        int singleSymbol = -1;
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                distinctCount++;
                singleSymbol = i;
            }
        }

        if (distinctCount == 1) {
            codeLengths[singleSymbol] = 1;
        } else {
            buildTreeAndComputeLengths(freq, codeLengths);
        }
        return codeLengths;
    }

    
    public long calculateExpectedPayloadBits(byte[] input) {
        if (input == null || input.length == 0) {
            return 0L;
        }
        long[] freq = new long[256];
        for (byte b : input) {
            freq[b & 0xFF]++;
        }

        int distinct = 0;
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                distinct++;
            }
        }
        if (distinct == 1) {
            return input.length; 
        }

        int[] codeLengths = computeCodeLengths(input);
        long totalBits = 0L;
        for (int i = 0; i < 256; i++) {
            totalBits += freq[i] * codeLengths[i];
        }
        return totalBits;
    }

   
    private static class HuffmanNode implements Comparable<HuffmanNode> {
        final long weight;
        final int symbol;      
        final int minSymbol;   
        final HuffmanNode left;
        final HuffmanNode right;

        HuffmanNode(long weight, int symbol) {
            this.weight = weight;
            this.symbol = symbol;
            this.minSymbol = symbol;
            this.left = null;
            this.right = null;
        }

        HuffmanNode(HuffmanNode left, HuffmanNode right) {
            this.weight = left.weight + right.weight;
            this.symbol = -1;
            this.minSymbol = Math.min(left.minSymbol, right.minSymbol);
            this.left = left;
            this.right = right;
        }

        @Override
        public int compareTo(HuffmanNode o) {
            if (this.weight != o.weight) {
                return Long.compare(this.weight, o.weight);
            }
            return Integer.compare(this.minSymbol, o.minSymbol);
        }
    }

    private void buildTreeAndComputeLengths(long[] freq, int[] codeLengths) {
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<HuffmanNode>();
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                pq.add(new HuffmanNode(freq[i], i));
            }
        }

        while (pq.size() > 1) {
            HuffmanNode left = pq.poll();
            HuffmanNode right = pq.poll();
            pq.add(new HuffmanNode(left, right));
        }

        HuffmanNode root = pq.poll();
        if (root != null) {
            traverseTreeForLengths(root, 0, codeLengths);
        }
    }

    private void traverseTreeForLengths(HuffmanNode node, int depth, int[] codeLengths) {
        if (node.symbol >= 0) {
            codeLengths[node.symbol] = depth;
            return;
        }
        if (node.left != null) {
            traverseTreeForLengths(node.left, depth + 1, codeLengths);
        }
        if (node.right != null) {
            traverseTreeForLengths(node.right, depth + 1, codeLengths);
        }
    }

    public static class CanonicalSymbol implements Comparable<CanonicalSymbol> {
        public final int symbol;
        public final int length;
        public int code;

        public CanonicalSymbol(int symbol, int length) {
            this.symbol = symbol;
            this.length = length;
        }

        @Override
        public int compareTo(CanonicalSymbol o) {
            if (this.length != o.length) {
                return Integer.compare(this.length, o.length);
            }
            return Integer.compare(this.symbol, o.symbol);
        }
    }

    
    public static List<CanonicalSymbol> generateCanonicalCodes(int[] codeLengths) {
        List<CanonicalSymbol> list = new ArrayList<CanonicalSymbol>();
        for (int i = 0; i < 256; i++) {
            if (codeLengths[i] > 0) {
                list.add(new CanonicalSymbol(i, codeLengths[i]));
            }
        }
        Collections.sort(list);

        int code = 0;
        int prevLen = 0;
        for (CanonicalSymbol cs : list) {
            if (prevLen > 0) {
                code = (code + 1) << (cs.length - prevLen);
            }
            cs.code = code;
            prevLen = cs.length;
        }
        return list;
    }

    private static class DecodeNode {
        DecodeNode left;
        DecodeNode right;
        int symbol = -1;
    }

    private static DecodeNode buildDecodeTree(List<CanonicalSymbol> symbols) {
        DecodeNode root = new DecodeNode();
        for (CanonicalSymbol cs : symbols) {
            DecodeNode curr = root;
            int code = cs.code;
            int len = cs.length;
            for (int bitIdx = len - 1; bitIdx >= 0; bitIdx--) {
                int bit = (code >>> bitIdx) & 1;
                if (bit == 0) {
                    if (curr.left == null) {
                        curr.left = new DecodeNode();
                    }
                    curr = curr.left;
                } else {
                    if (curr.right == null) {
                        curr.right = new DecodeNode();
                    }
                    curr = curr.right;
                }
            }
            if (curr.symbol != -1) {
                throw new IllegalArgumentException("Prefix collision in canonical tree");
            }
            curr.symbol = cs.symbol;
        }
        return root;
    }
}
