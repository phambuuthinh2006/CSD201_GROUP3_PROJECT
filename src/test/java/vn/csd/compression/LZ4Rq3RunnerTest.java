package vn.csd.compression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.csd.compression.metrics.LZ4Rq3Runner;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LZ4Rq3RunnerTest {
    @TempDir Path dir;

    @Test void csvHasBothHashLogsAndOnlyMeasuredIterations() throws Exception {
        Path input = dir.resolve("sample.bin");
        Files.write(input, "abcabcabcabcabcabcabcabcabcabc".getBytes(StandardCharsets.US_ASCII));
        Path csv = dir.resolve("rq3.csv");
        LZ4Rq3Runner.main(new String[]{csv.toString(), "1", "2", input.toString()});
        List<String> rows = Files.readAllLines(csv, StandardCharsets.UTF_8);
        assertEquals(5, rows.size());
        for (int i = 1; i < rows.size(); i++) {
            String[] columns = rows.get(i).split(",");
            assertEquals(i <= 2 ? "12" : "16", columns[1]);
            assertEquals(15, columns.length);
            assertTrue(Long.parseLong(columns[6]) > 0);
            assertTrue(Long.parseLong(columns[7]) > 0);
            assertEquals("PASS", columns[14]);
            assertEquals(rows.get(1).split(",")[13], columns[13]);
        }
        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> LZ4Rq3Runner.main(new String[]{csv.toString(), "0", "1", input.toString()}));
    }

    @Test void emptyFileHasNoRatioAndKnownSha256() throws Exception {
        Path input = dir.resolve("empty"); Files.write(input, new byte[0]);
        Path csv = dir.resolve("empty.csv");
        LZ4Rq3Runner.main(new String[]{csv.toString(), "0", "1", input.toString()});
        String[] row = Files.readAllLines(csv, StandardCharsets.UTF_8).get(1).split(",");
        assertEquals("", row[5]);
        assertEquals("e3b0c44298fc1c14", row[13]);
        assertEquals("1", row[10]);
        assertEquals("0", row[11]);
    }
}
