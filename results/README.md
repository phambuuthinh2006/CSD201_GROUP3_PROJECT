# Kết quả chạy thử LZ4

`lz4-smoke.csv` là 4 lượt đo trên chính `LZ4Codec.java` (hai hash log × hai lượt, một warm-up mỗi hash log), tất cả SHA-256 PASS. Lưu môi trường trong `lz4-smoke-environment.log`.

Đây là smoke test lệnh chạy, **không phải** corpus RQ3 và không đủ để kết luận về hiệu năng. Nhiều warm-up/lượt đo cùng corpus chuẩn và phương pháp đo chung còn cần TV4/TV6 chốt.

Tái tạo sau `mvn compile` (dùng tên output mới vì runner không ghi đè):

```sh
java -Xmx1g -cp target/classes vn.csd.compression.metrics.LZ4Rq3Runner \
  results/lz4-smoke-new.csv 1 2 src/main/java/vn/csd/compression/codec/LZ4Codec.java \
  2> results/lz4-smoke-new-environment.log
```
