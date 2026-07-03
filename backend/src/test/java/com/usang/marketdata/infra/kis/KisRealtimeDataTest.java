package com.usang.marketdata.infra.kis;

import com.usang.marketdata.infra.kis.dto.KisRealtimeData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KisRealtimeDataTest {

    // KIS H0STCNT0 실제 데이터 샘플 (인덱스 기준)
    // [0]종목코드 [1]체결시간 [2]현재가 [3]부호 [4]전일대비 [5]대비율
    // [6]가중평균 [7]시가 [8]고가 [9]저가 [10]매도호가 [11]매수호가 [12]체결량
    private static final String SAMPLE =
            "005930^153515^83000^5^100^0.12^82500^83000^83200^82800^82900^82700^1500";

    @Test
    @DisplayName("정상 데이터를 파싱하면 각 필드가 올바르게 추출된다")
    void parse_extracts_all_fields_correctly() {
        KisRealtimeData data = KisRealtimeData.parse(SAMPLE);

        assertThat(data.symbol()).isEqualTo("005930");
        assertThat(data.price()).isEqualTo(83000.0);   // [2] 현재가
        assertThat(data.open()).isEqualTo(83000.0);    // [7] 시가
        assertThat(data.high()).isEqualTo(83200.0);    // [8] 고가
        assertThat(data.low()).isEqualTo(82800.0);     // [9] 저가
        assertThat(data.volume()).isEqualTo(1500.0);   // [12] 체결량
    }

    @Test
    @DisplayName("종목코드가 다른 여러 데이터도 올바르게 파싱된다")
    void parse_works_for_different_symbols() {
        String sample = "000660^090001^120000^5^500^0.42^119800^119500^120500^119000^120100^119900^3000";

        KisRealtimeData data = KisRealtimeData.parse(sample);

        assertThat(data.symbol()).isEqualTo("000660");
        assertThat(data.price()).isEqualTo(120000.0);
        assertThat(data.volume()).isEqualTo(3000.0);
    }

    @Test
    @DisplayName("필드 수가 부족하면 ArrayIndexOutOfBoundsException이 발생한다")
    void parse_throws_when_fields_insufficient() {
        String incomplete = "005930^153515^83000"; // 인덱스 12까지 필요한데 3개뿐

        assertThatThrownBy(() -> KisRealtimeData.parse(incomplete))
                .isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

    @Test
    @DisplayName("가격 필드가 숫자가 아니면 NumberFormatException이 발생한다")
    void parse_throws_when_price_is_not_numeric() {
        String invalid = "005930^153515^INVALID^5^100^0.12^82500^83000^83200^82800^82900^82700^1500";

        assertThatThrownBy(() -> KisRealtimeData.parse(invalid))
                .isInstanceOf(NumberFormatException.class);
    }
}
