package vn.csd.compression;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vn.csd.compression.dataset.ByteOrdering;
import vn.csd.compression.metrics.Entropy;

import java.util.Random;


public class ByteOrderingTest {

    private static int[] getHistogram(byte[] data) {
        int[] freq = new int[256];
        for (byte b : data) {
            freq[b & 0xFF]++;
        }
        return freq;
    }

    @Test
    public void testT11_HistogramInvariance_GroupedAndShuffled() {
        byte[] original = new byte[10000];
        Random rnd = new Random(42L);
        rnd.nextBytes(original);

        byte[] grouped = ByteOrdering.grouped(original);
        byte[] shuffled = ByteOrdering.shuffled(original, 20261020L);

    
        Assertions.assertEquals(original.length, grouped.length, "Grouped phai giu nguyen do dai");
        Assertions.assertEquals(original.length, shuffled.length, "Shuffled phai giu nguyen do dai");

      
        int[] histOrig = getHistogram(original);
        int[] histGroup = getHistogram(grouped);
        int[] histShuff = getHistogram(shuffled);

        Assertions.assertArrayEquals(histOrig, histGroup, "Histogram cua Grouped phai trung 100% Original");
        Assertions.assertArrayEquals(histOrig, histShuff, "Histogram cua Shuffled phai trung 100% Original");

      
        double h0Orig = Entropy.calculateH0(original);
        double h0Group = Entropy.calculateH0(grouped);
        double h0Shuff = Entropy.calculateH0(shuffled);

        Assertions.assertEquals(h0Orig, h0Group, 1e-9, "Entropy H0 cua Grouped phai bang Original");
        Assertions.assertEquals(h0Orig, h0Shuff, 1e-9, "Entropy H0 cua Shuffled phai bang Original");

        System.out.println("[PASS] Test T11 Histogram and H0 Invariance passed successfully!");
    }

    @Test
    public void testGrouped_IsSortedAscending() {
        byte[] input = new byte[]{5, -1, 0, 100, 5, -1, 0}; 
        byte[] grouped = ByteOrdering.grouped(input);

        for (int i = 0; i < grouped.length - 1; i++) {
            int current = grouped[i] & 0xFF;
            int next = grouped[i + 1] & 0xFF;
            Assertions.assertTrue(current <= next, "Grouped phai sap xep tang dan unsigned");
        }
    }

    public static void main(String[] args) {
        System.out.println("Dang chay ByteOrderingTest (T11)...");
        ByteOrderingTest test = new ByteOrderingTest();
        test.testT11_HistogramInvariance_GroupedAndShuffled();
        test.testGrouped_IsSortedAscending();
        System.out.println("Tat ca cac test trong ByteOrderingTest deu PASS!");
    }
}
