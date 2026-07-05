"""
KRX에서 수동 다운로드한 CSV를 프론트엔드용 JSON으로 변환.

CSV 출처: data.krx.co.kr → 기본통계 → 주식 → 종목 → 전종목 기본정보
인코딩: EUC-KR
컬럼: 표준코드, 단축코드, 한글 종목명, 한글 종목약명, 영문 종목명, 상장일, 시장구분, 증권구분, ...

실행: python3 generate_kr_stocks.py <CSV파일명>
출력: ../frontend/public/kr-stocks.json
"""
import csv
import json
import sys
import os

INCLUDE_MARKETS = {"KOSPI", "KOSDAQ", "KOSDAQ GLOBAL"}

def convert(csv_path: str, output_path: str):
    result = []

    with open(csv_path, encoding="euc-kr") as f:
        reader = csv.reader(f)
        next(reader)  # 헤더 스킵

        for row in reader:
            if len(row) < 8:
                continue

            symbol   = row[1].strip()   # 단축코드 (6자리)
            name     = row[3].strip()   # 한글 종목약명 (예: 삼성전자)
            market   = row[6].strip()   # 시장구분
            sec_type = row[7].strip()   # 증권구분

            # ETF·ETN 등 제외, 주권(일반 주식)만 포함
            if sec_type != "주권":
                continue
            if market not in INCLUDE_MARKETS:
                continue

            result.append({"symbol": symbol, "name": name})

    # 종목명 오름차순 정렬
    result.sort(key=lambda x: x["name"])

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print(f"총 {len(result)}개 종목 저장 완료 → {output_path}")

if __name__ == "__main__":
    csv_file = sys.argv[1] if len(sys.argv) > 1 else "data_5113_20260705.csv"
    script_dir = os.path.dirname(os.path.abspath(__file__))
    csv_path = os.path.join(script_dir, csv_file)
    output_path = os.path.join(script_dir, "../frontend/public/kr-stocks.json")

    convert(csv_path, output_path)
