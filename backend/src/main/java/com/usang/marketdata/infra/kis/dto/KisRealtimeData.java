package com.usang.marketdata.infra.kis.dto;

// KIS WebSocket 수신 메시지는 pipe-delimited 포맷
// 예: 0|H0STCNT0|001|005930^83000^...
// 이 record는 '^'로 분리한 실제 데이터 필드를 담는다
public record KisRealtimeData(
        String symbol,      // [0] MKSC_SHRN_ISCD 유가증권단축종목코드 (예: 005930)
        double price,       // [2] STCK_PRPR 주식현재가
        double open,        // [7] STCK_OPRC 시가
        double high,        // [8] STCK_HGPR 고가
        double low,         // [9] STCK_LWPR 저가
        double volume       // [12] CNTG_VOL 체결거래량 (이 틱의 거래량)
) {
    // pipe-delimited 메시지의 4번째 필드(data 부분)를 '^'로 파싱
    public static KisRealtimeData parse(String data) {
        String[] fields = data.split("\\^");
        return new KisRealtimeData(
                fields[0],
                Double.parseDouble(fields[2]),
                Double.parseDouble(fields[7]),
                Double.parseDouble(fields[8]),
                Double.parseDouble(fields[9]),
                Double.parseDouble(fields[12])
        );
    }
}
