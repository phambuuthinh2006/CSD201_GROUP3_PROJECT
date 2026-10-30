package vn.csd.compression;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vn.csd.compression.codec.HuffmanCodec;
import vn.csd.compression.io.BitReader;
import vn.csd.compression.io.BitWriter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;


public class HuffmanCodecTest {

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void pass(String message) {
        System.out.println("[PASS] PASSED: " + message);
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  BAT DAU CHAY BO TEST NGHIEM THU CHO HUFFMAN (TV3)");
        System.out.println("=================================================");

        HuffmanCodecTest t = new HuffmanCodecTest();

        
        t.testT01_EmptyInput_HeaderSize();
        t.testT01_EmptyInput_RoundTrip();

        
        t.testT02_SingleByte_RoundTrip();
        t.testT02_SingleByte_CompressedSize269();
        t.testT02_SingleByte_CodeLength1Bit();

        
        t.testT03_RepeatedByte_RoundTrip();
        t.testT03_RepeatedByte_TheoreticalSize();
        t.testT03_RepeatedByte_CompressionRatio();

        
        t.testT04_All256Bytes_RoundTrip();
        t.testT04_All256Bytes_All8Bits();

      
        t.testT09_InvalidMagic_Rejected();

       
        t.testT10_PayloadBitsFormula_ABRACADABRA();
        t.testT10_PayloadBitsFormula_Shakespeare();
        t.testT10_PayloadBitsFormula_Seashore();
        t.testT10_PayloadBitsFormula_CSD201Text();

        
        t.testT12_RandomData_100KB_SHA256();
        t.testT12_SimulatedText_300KB_SHA256();
        t.testT12_SimulatedText_CompressionRatio();

        
        t.testShortText_VietnameseUTF8();
        t.testBitWriter_WriteAndPadding();
        t.testBitReader_ReadBitAndBits();
        t.testBoundary_NullInputRejected();
        t.testBoundary_TruncatedDataRejected();

        System.out.println("=================================================");
        System.out.println(">>> CHUC MUNG: TAT CA CAC BAI TEST HUFFMAN DA PASS!");
        System.out.println("=================================================");
    }

    
    @Test
    public void testT01_EmptyInput_HeaderSize() {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] compressed = codec.compress(new byte[0]);
        Assertions.assertEquals(HuffmanCodec.HEADER_SIZE, compressed.length);
        pass("T01 - Header kich thuoc dung chuan 268 byte (" + compressed.length + " bytes)");
    }

    @Test
    public void testT01_EmptyInput_RoundTrip() {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] decompressed = codec.decompress(codec.compress(new byte[0]));
        Assertions.assertEquals(0, decompressed.length);
        pass("T01 - Input rong round-trip thanh cong, tra ve mang 0 byte");
    }

  

    @Test
    public void testT02_SingleByte_RoundTrip() {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] input = new byte[]{(byte) 'Z'};
        byte[] decompressed = codec.decompress(codec.compress(input));
        Assertions.assertArrayEquals(input, decompressed);
        pass("T02 - Input 1 byte round-trip chinh xac 100%");
    }

    @Test
    public void testT02_SingleByte_CompressedSize269() {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] compressed = codec.compress(new byte[]{(byte) 'Z'});
        Assertions.assertEquals(269, compressed.length);
        pass("T02 - File nen 1 byte co kich thuoc dung 269 byte (gom 1 bit padding thanh 1 byte payload)");
    }

    @Test
    public void testT02_SingleByte_CodeLength1Bit() {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] input = new byte[]{(byte) 'Z'};
        int[] codeLengths = codec.computeCodeLengths(input);
        Assertions.assertEquals(1, codeLengths['Z']);
        pass("T02 - Ky hieu don le co do dai ma chinh xac la 1 bit");
    }

  

    @Test
    public void testT03_RepeatedByte_RoundTrip() {
        HuffmanCodec codec = new HuffmanCodec();
        int size = 1024 * 1024;
        byte[] input = new byte[size];
        Arrays.fill(input, (byte) 'A');
        byte[] decompressed = codec.decompress(codec.compress(input));
        Assertions.assertArrayEquals(input, decompressed);
        pass("T03 - 1 MiB cung 1 byte giai nen khop 100%");
    }

    @Test
    public void testT03_RepeatedByte_TheoreticalSize() {
        HuffmanCodec codec = new HuffmanCodec();
        int size = 1024 * 1024;
        byte[] input = new byte[size];
        Arrays.fill(input, (byte) 'A');
        byte[] compressed = codec.compress(input);
        Assertions.assertEquals(268 + (size / 8), compressed.length);
        pass("T03 - Kich thuoc nen dung chuan ly thuyet: 268 header + 131072 payload");
    }

    @Test
    public void testT03_RepeatedByte_CompressionRatio() {
        HuffmanCodec codec = new HuffmanCodec();
        int size = 1024 * 1024;
        byte[] input = new byte[size];
        Arrays.fill(input, (byte) 'A');
        byte[] compressed = codec.compress(input);
        double ratio = (double) compressed.length / input.length;
        System.out.printf("   [T03 Info] Goc: %d bytes -> Nen: %d bytes (Ty le: %.4f)\n",
                input.length, compressed.length, ratio);
        Assertions.assertTrue(ratio < 0.13);
        pass("T03 - Du lieu 1 byte lap nen dat ty le ~12.5% (dung 1 bit/byte)");
    }

    

    @Test
    public void testT04_All256Bytes_RoundTrip() {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] input = new byte[256];
        for (int i = 0; i < 256; i++) {
            input[i] = (byte) i;
        }
        byte[] decompressed = codec.decompress(codec.compress(input));
        Assertions.assertArrayEquals(input, decompressed);
        pass("T04 - Du 256 byte khac nhau round-trip chinh xac");
    }

    @Test
    public void testT04_All256Bytes_All8Bits() {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] input = new byte[256];
        for (int i = 0; i < 256; i++) {
            input[i] = (byte) i;
        }
        int[] lengths = codec.computeCodeLengths(input);
        for (int i = 0; i < 256; i++) {
            Assertions.assertEquals(8, lengths[i]);
        }
        pass("T04 - 256 ky hieu co tan suat bang nhau (1) deu duoc gan ma 8 bit");
    }



    @Test
    public void testT09_InvalidMagic_Rejected() {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] fakeData = new byte[HuffmanCodec.HEADER_SIZE];
        fakeData[0] = 'B';
        fakeData[1] = 'A';
        fakeData[2] = 'D';
        fakeData[3] = '!';

        boolean thrown = false;
        try {
            codec.decompress(fakeData);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        Assertions.assertTrue(thrown);
        pass("T09 - Decoder tu choi magic sai hop le");
    }

  

    private void verifyT10Sample(String sample) {
        HuffmanCodec codec = new HuffmanCodec();
        byte[] input = sample.getBytes(StandardCharsets.UTF_8);

        long expectedBits = codec.calculateExpectedPayloadBits(input);

        long[] freq = new long[256];
        for (byte b : input) {
            freq[b & 0xFF]++;
        }
        int[] codeLengths = codec.computeCodeLengths(input);
        long manualSum = 0;
        for (int i = 0; i < 256; i++) {
            manualSum += freq[i] * codeLengths[i];
        }

        Assertions.assertEquals(expectedBits, manualSum);
        pass("T10 - Tinh doc lap: expectedBits (" + expectedBits + ") == SUM(freq * codeLength) (" + manualSum + ")");

        byte[] compressed = codec.compress(input);
        int actualPayloadBytes = compressed.length - HuffmanCodec.HEADER_SIZE;
        int expectedPayloadBytes = (int) ((expectedBits + 7) / 8);

        Assertions.assertEquals(expectedPayloadBytes, actualPayloadBytes);
        pass("T10 - Kich thuoc payload byte (" + actualPayloadBytes + ") khop chinh xac tran so bit / 8 (" + expectedPayloadBytes + ")");
    }

    @Test
    public void testT10_PayloadBitsFormula_ABRACADABRA() {
        verifyT10Sample("ABRACADABRA");
    }

    @Test
    public void testT10_PayloadBitsFormula_Shakespeare() {
        verifyT10Sample("TO BE OR NOT TO BE THAT IS THE QUESTION");
    }

    @Test
    public void testT10_PayloadBitsFormula_Seashore() {
        verifyT10Sample("She sells seashells on the seashore");
    }

    @Test
    public void testT10_PayloadBitsFormula_CSD201Text() {
        verifyT10Sample("CSD201 Lossless Compression Project Canonical Huffman by TV3");
    }

    

    @Test
    public void testT12_RandomData_100KB_SHA256() {
        HuffmanCodec codec = new HuffmanCodec();
        Random rnd = new Random(20261021);
        byte[] input = new byte[100000];
        rnd.nextBytes(input);

        String shaBefore = sha256Hex(input);
        byte[] decomp = codec.decompress(codec.compress(input));
        String shaAfter = sha256Hex(decomp);

        Assertions.assertEquals(shaBefore, shaAfter);
        pass("T12 - 100.000 byte random SHA-256 truoc va sau giai nen trung khop tuyet doi");
    }

    @Test
    public void testT12_SimulatedText_300KB_SHA256() {
        HuffmanCodec codec = new HuffmanCodec();
        Random rnd = new Random(20261021);
        byte[] input = new byte[300000];
        for (int i = 0; i < input.length; i++) {
            if (rnd.nextInt(100) < 70) {
                input[i] = (byte) ('a' + rnd.nextInt(26));
            } else {
                input[i] = (byte) rnd.nextInt(256);
            }
        }

        String shaBefore = sha256Hex(input);
        byte[] decomp = codec.decompress(codec.compress(input));
        String shaAfter = sha256Hex(decomp);

        Assertions.assertEquals(shaBefore, shaAfter);
        pass("T12 - 300.000 byte van ban mo phong SHA-256 truoc va sau giai nen trung khop tuyet doi");
    }

    @Test
    public void testT12_SimulatedText_CompressionRatio() {
        HuffmanCodec codec = new HuffmanCodec();
        Random rnd = new Random(20261021);
        byte[] input = new byte[300000];
        for (int i = 0; i < input.length; i++) {
            if (rnd.nextInt(100) < 70) {
                input[i] = (byte) ('a' + rnd.nextInt(26));
            } else {
                input[i] = (byte) rnd.nextInt(256);
            }
        }

        byte[] comp = codec.compress(input);
        double ratio = (double) comp.length / input.length;
        System.out.printf("   [T12 Info] 300 KB van ban mo phong: nen dat ty le %.4f (tiet kiem %.1f%%)\n",
                ratio, (1.0 - ratio) * 100);
        Assertions.assertTrue(ratio < 0.85);
        pass("T12 - Van ban mo phong nen tot (< 85%)");
    }

    

    @Test
    public void testShortText_VietnameseUTF8() {
        HuffmanCodec codec = new HuffmanCodec();
        String text = "Xin chao! Day la thuat toan nen Canonical Huffman do TV3 (Nhom 3 - CSD201) cai dat.";
        byte[] input = text.getBytes(StandardCharsets.UTF_8);
        byte[] restored = codec.decompress(codec.compress(input));

        String restoredText = new String(restored, StandardCharsets.UTF_8);
        Assertions.assertEquals(text, restoredText);
        pass("Chuoi van ban UTF-8 round-trip chinh xac 100%");
    }

    @Test
    public void testBitWriter_WriteAndPadding() {
        BitWriter writer = new BitWriter();
        writer.writeBit(1);                
        writer.writeBits(0b101, 3);          
        writer.writeBits(0b110011, 6);        
        writer.writeBits(0xABCD, 16);         
        writer.writeBit(0);                   

        Assertions.assertEquals(27, writer.getTotalBitsWritten());
        pass("BitWriter ghi dung 27 bit");

        byte[] bytes = writer.toByteArray();
        Assertions.assertEquals(4, bytes.length);
        pass("27 bit duoc xa bu 0 thanh 4 byte");
    }

    @Test
    public void testBitReader_ReadBitAndBits() {
        BitWriter writer = new BitWriter();
        writer.writeBit(1);
        writer.writeBits(0b101, 3);
        writer.writeBits(0b110011, 6);
        writer.writeBits(0xABCD, 16);
        writer.writeBit(0);
        byte[] bytes = writer.toByteArray();

        BitReader reader = new BitReader(bytes);
        int bit1 = reader.readBit();
        int val3 = reader.readBits(3);
        int val6 = reader.readBits(6);
        int val16 = reader.readBits(16);
        int bitEnd = reader.readBit();

        Assertions.assertEquals(1, bit1);
        pass("BitReader doc bit 1 dung");

        Assertions.assertEquals(0b101, val3);
        pass("BitReader doc 3 bit dung");

        Assertions.assertEquals(0b110011, val6);
        pass("BitReader doc 6 bit dung");

        Assertions.assertEquals(0xABCD, val16);
        pass("BitReader doc 16 bit dung");

        Assertions.assertEquals(0, bitEnd);
        pass("BitReader doc bit cuoi dung");

        Assertions.assertEquals(27, reader.getTotalBitsRead());
        pass("BitReader da doc dung 27 bit");
    }

    @Test
    public void testBoundary_NullInputRejected() {
        HuffmanCodec codec = new HuffmanCodec();
        boolean thrown = false;
        try {
            codec.compress(null);
        } catch (NullPointerException e) {
            thrown = true;
        }
        Assertions.assertTrue(thrown);
        pass("Tu choi compress(null) bang NullPointerException");
    }

    @Test
    public void testBoundary_TruncatedDataRejected() {
        HuffmanCodec codec = new HuffmanCodec();
        boolean thrown = false;
        try {
            codec.decompress(new byte[100]);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        Assertions.assertTrue(thrown);
        pass("Tu choi compressed data < 268 bytes");
    }
}
