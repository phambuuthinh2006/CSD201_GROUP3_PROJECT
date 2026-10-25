package vn.csd.compression.metrics;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CsvWriter {

    private static final String HEADER = "rq,dataset,file,variant,seed,input_bytes,codec,params,compressed_bytes,ratio,compress_ns,decompress_ns,compress_mib_s,decompress_mib_s,compress_heap_aux_bytes,decompress_heap_aux_bytes,entropy_h0,lzw_dictionary_full_at,lz4_sequence_count,lz4_avg_match_length,output_check,status";

    public static void writeResult(String filePath, BenchmarkResult result) {
        Path path = Paths.get(filePath);
        try {
            // Tự động tạo thư mục cha (ví dụ: thư mục results/) nếu chưa tồn tại
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            System.err.println("Không thể tạo thư mục chứa file CSV: " + e.getMessage());
        }

        boolean fileExists = Files.exists(path);
        
        // Mở file ở chế độ append (true) để ghi dồn kết quả thí nghiệm
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, true))) {
            if (!fileExists) {
                writer.println(HEADER);
            }
            writer.println(result.toCsvRow());
        } catch (IOException e) {
            System.err.println("Lỗi khi ghi file CSV: " + e.getMessage());
        }
    }
}
