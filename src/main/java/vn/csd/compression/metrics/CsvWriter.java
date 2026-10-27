
package vn.csd.compression.metrics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class CsvWriter {

    private static final String HEADER =
            "rq,dataset,file,variant,seed,input_bytes,codec,params,"
          + "compressed_bytes,ratio,compress_ns,decompress_ns,"
          + "compress_mib_s,decompress_mib_s,"
          + "compress_heap_aux_bytes,decompress_heap_aux_bytes,"
          + "entropy_h0,lzw_dictionary_full_at,lz4_sequence_count,"
          + "lz4_avg_match_length,output_check,status";

    public static void writeResult(
            String filePath,
            BenchmarkRunner.BenchmarkResult result) throws IOException {

        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Đường dẫn CSV không được để trống");
        }

        if (result == null) {
            throw new IllegalArgumentException("BenchmarkResult không được null");
        }

        Path path = Paths.get(filePath);
        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        boolean needsHeader = !Files.exists(path) || Files.size(path) == 0;

        if (!needsHeader) {
            String existingHeader;

            try (java.io.BufferedReader reader = Files.newBufferedReader(
                    path, StandardCharsets.UTF_8)) {
                existingHeader = reader.readLine();
            }

            if (!HEADER.equals(existingHeader)) {
                throw new IOException(
                        "Header CSV không khớp schema hiện tại: " + path
                );
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                path,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {

            if (needsHeader) {
                writer.write(HEADER);
                writer.newLine();
            }

            writer.write(result.toCsvRow());
            writer.newLine();
        }
    }
}
