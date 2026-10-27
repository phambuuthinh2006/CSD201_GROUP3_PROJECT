#!/usr/bin/env bash
# Terminal demo for video recording; does not change Git state or algorithms.
set -euo pipefail

csd_mode=${1:-tv5}
case "$csd_mode" in
  tv5|all) ;;
  *) printf 'Cách dùng: bash scripts/demo-terminal.sh [tv5|all]\n' >&2; exit 2 ;;
esac
csd_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$csd_root"
csd_maven=${CSD_MAVEN_BIN:-}
if [[ -z "$csd_maven" ]]; then
  if command -v mvn >/dev/null 2>&1; then
    csd_maven=$(command -v mvn)
  elif [[ -x "$HOME/.cache/csd-tools/apache-maven-3.9.9/bin/mvn" ]]; then
    csd_maven="$HOME/.cache/csd-tools/apache-maven-3.9.9/bin/mvn"
  else
    printf 'Chưa tìm thấy Maven. Cài Maven hoặc đặt CSD_MAVEN_BIN=/đường/dẫn/bin/mvn.\n' >&2
    exit 1
  fi
fi
command -v java >/dev/null
command -v python3 >/dev/null
[[ -x "$csd_maven" ]] || { printf 'Maven không thực thi được: %s\n' "$csd_maven" >&2; exit 1; }
mkdir -p results
csd_run_dir=$(mktemp -d "$csd_root/results/video-$(date +%Y%m%d-%H%M%S)-XXXXXX")
exec > >(tee "$csd_run_dir/terminal.log") 2>&1

printf '\nCSD201 — DEMO TEST TERMINAL (%s)\n' "$csd_mode"
printf 'Thư mục: %s\n' "$csd_root"
printf 'Lưu kết quả: %s\n' "$csd_run_dir"
printf '\n1. MÔI TRƯỜNG CHẠY\n'
java -version
"$csd_maven" -version
if command -v git >/dev/null 2>&1; then git log -1 --oneline; fi

csd_test_args=()
if [[ "$csd_mode" == tv5 ]]; then
  csd_test_args=(-Dtest=LZ4CodecTest,LZ4Rq3RunnerTest)
fi
printf '\n2. BUILD VÀ KIỂM THỬ JUNIT (%s)\n' "$csd_mode"
"$csd_maven" --batch-mode -Dstyle.color=never "${csd_test_args[@]}" clean verify
# Check XML explicitly: shared pom currently has testFailureIgnore=true.
python3 - <<'PYXML'
from pathlib import Path
import xml.etree.ElementTree as ET
reports = sorted(Path("target/surefire-reports").glob("TEST-*.xml"))
assert reports, "Không tìm thấy báo cáo JUnit"
totals = dict(tests=0, failures=0, errors=0, skipped=0)
for path in reports:
    suite = ET.parse(path).getroot()
    for key in totals:
        totals[key] += int(suite.attrib.get(key, 0))
    print(suite.attrib["name"], {key: suite.attrib.get(key, "0") for key in totals})
print("Tổng JUnit:", totals)
if totals["failures"] or totals["errors"]:
    raise SystemExit("Có test lỗi; không dùng BUILD SUCCESS làm bằng chứng PASS.")
PYXML
printf '\nCác ca JUnit vừa chạy:\n'
sed -n 's/.*<testcase name="\([^"]*\)".*/  Test: \1/p' target/surefire-reports/TEST-*.xml
cp target/surefire-reports/TEST-*.xml "$csd_run_dir/"

if [[ "$csd_mode" == all ]]; then
  printf '\n3. TEST GENERATOR C1/C2/C3 CỦA TV6 (MAIN)\n'
  if java -Xmx1g -cp target/classes:target/test-classes vn.csd.compression.DataGeneratorTest \
      > "$csd_run_dir/generator.log" 2>&1; then
    grep -E -- '---|Info|size 0|size 1|cung seed|khac seed|Tu vung dung|C2 sinh|C3 sinh|CHUC MUNG|\[FAIL\]' \
      "$csd_run_dir/generator.log"
  else
    cat "$csd_run_dir/generator.log"
    exit 1
  fi
  printf '\n4. ĐO TỐC ĐỘ TẤT CẢ THUẬT TOÁN: RLE, HUFFMAN, LZW, LZ4-12/16\n'
  printf 'Bốn kiểu dữ liệu, 1/4 MiB; 3 JVM tuần tự; 5 warm-up + 7 lượt đo mỗi cấu hình/JVM.\n'
  printf 'Đang đo mới, không đọc lại kết quả cũ. Mọi lượt kiểm đầy đủ SHA-256.\n'
  CSD_MAVEN_BIN="$csd_maven" bash scripts/benchmark/run-comparison.sh "$csd_run_dir/comparison"
  python3 scripts/benchmark/summarize-comparison.py "$csd_run_dir/comparison"
  printf '\n5. BẢNG TỐC ĐỘ VÀ TỶ LỆ NÉN — INPUT 1 MiB\n'
  python3 scripts/report-terminal.py benchmark "$csd_run_dir/comparison/summary.csv" --mib 1
  printf '\n6. BẢNG TỐC ĐỘ VÀ TỶ LỆ NÉN — INPUT 4 MiB\n'
  python3 scripts/report-terminal.py benchmark "$csd_run_dir/comparison/summary.csv" --mib 4
  printf '\nĐã đo mới 840 lượt + 600 warm-up; 5 biến thể của 4 thuật toán; SHA-256 PASS.\n'
  printf 'Cột Ratio nhỏ hơn tốt hơn; Compress/Decompress MiB/s lớn hơn tốt hơn.\n'
  printf 'Các bảng vừa đọc summary của phép đo mới ở bước 4; không phải một phép đo thứ hai.\n'
  printf 'Đây là benchmark khảo sát, chưa phải RQ3 đầy đủ hoặc phép đo peak heap.\n'
else
  printf '\n3. DEMO RIÊNG LZ4 HASH_LOG=12 VÀ 16, ĐỐI CHIẾU SHA-256\n'
  printf 'Input demo: README.md (không phải corpus RQ3 chính thức).\n'
  sha256sum README.md
  java -Xmx1g -cp target/classes vn.csd.compression.metrics.LZ4Rq3Runner \
    "$csd_run_dir/lz4-demo.csv" 1 2 README.md
  cat "$csd_run_dir/lz4-demo.csv"
  printf 'Mỗi hash log: 1 warm-up + 2 lượt đo; CSV có 4 dòng PASS nếu đúng dữ liệu.\n'
fi
printf '\nĐã chạy xong các bước đã chọn mà không có lệnh báo lỗi.\n'
printf 'Log, báo cáo JUnit và CSV: %s\n' "$csd_run_dir"
