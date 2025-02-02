package com.prgrms.ijuju.domain.stock.mid.service;

import com.prgrms.ijuju.domain.member.entity.Member;
import com.prgrms.ijuju.domain.stock.mid.dto.response.MidStockPriceResponse;
import com.prgrms.ijuju.domain.stock.mid.dto.response.MidStockResponse;
import com.prgrms.ijuju.domain.stock.mid.dto.response.MidStockTradeInfo;
import com.prgrms.ijuju.domain.stock.mid.dto.response.MidStockWithTradesResponse;
import com.prgrms.ijuju.domain.stock.mid.entity.MidStock;
import com.prgrms.ijuju.domain.stock.mid.entity.MidStockPrice;
import com.prgrms.ijuju.domain.stock.mid.entity.MidStockTrade;
import com.prgrms.ijuju.domain.stock.mid.entity.TradeType;
import com.prgrms.ijuju.domain.stock.mid.exception.MidStockNotFoundException;
import com.prgrms.ijuju.domain.stock.mid.repository.MidStockPriceRepository;
import com.prgrms.ijuju.domain.stock.mid.repository.MidStockRepository;
import com.prgrms.ijuju.domain.stock.mid.repository.MidStockTradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

@ExtendWith(MockitoExtension.class)
class MidStockServiceTest {

    @InjectMocks
    private MidStockService midStockService;

    @Mock
    private MidStockRepository midStockRepository;

    @Mock
    private MidStockTradeRepository midStockTradeRepository;

    @Mock
    private MidStockPriceRepository midStockPriceRepository;

    private MidStock sampleStock;
    private Member sampleMember;
    private MidStockTrade sampleTrade;
    private MidStockPrice samplePrice;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        sampleStock = new MidStock("삼성전자");
        sampleMember = Member.builder().id(1L).build();

        sampleTrade = MidStockTrade.builder()
                .id(1L)
                .tradePoint(10L)
                .pricePerStock(70000L)
                .tradeType(TradeType.BUY)
                .midStock(sampleStock)
                .member(sampleMember)
                .build();

        samplePrice = MidStockPrice.builder()
                .highPrice(71000L)
                .lowPrice(69000L)
                .avgPrice(70000L)
                .priceDate(now)
                .midStock(sampleStock)
                .build();
    }

    @Test
    @DisplayName("전체 중급 주식 목록 조회")
    void findAllStocks() {
        given(midStockRepository.findAll()).willReturn(List.of(sampleStock));

        List<MidStockResponse> result = midStockService.findAllStocks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).midName()).isEqualTo("삼성전자");
        verify(midStockRepository).findAll();
    }

    @Test
    @DisplayName("특정 회원의 주식 거래 내역 조회")
    void findStockTrades() {
        Long memberId = 1L;
        Long stockId = 1L;
        given(midStockRepository.findById(stockId)).willReturn(Optional.of(sampleStock));
        given(midStockTradeRepository.findBuyMidStock(memberId, stockId))
                .willReturn(List.of(sampleTrade));

        List<MidStockTradeInfo> result = midStockService.findStockTrades(memberId, stockId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tradePoint()).isEqualTo(10L);
        verify(midStockTradeRepository).findBuyMidStock(memberId, stockId);
    }

    @Test
    @DisplayName("존재하지 않는 주식 조회시 예외 발생")
    void findStockTradesWithInvalidStock() {
        Long memberId = 1L;
        Long stockId = 999L;
        given(midStockRepository.findById(stockId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> midStockService.findStockTrades(memberId, stockId))
                .isInstanceOf(MidStockNotFoundException.class);
    }

    @Test
    @DisplayName("회원의 전체 주식 보유 현황 조회")
    void getMemberStocksAndTrades() {
        // given
        Long memberId = 1L;

        MidStock stock = new MidStock("삼성전자");
        ReflectionTestUtils.setField(stock, "id", 1L);  // private id 필드 설정

        MidStockTrade trade = MidStockTrade.builder()
                .tradePoint(10L)
                .pricePerStock(70000L)
                .tradeType(TradeType.BUY)
                .member(sampleMember)
                .build();
        trade.setMidStock(stock);

        given(midStockTradeRepository.findAllBuyMidStock(memberId))
                .willReturn(List.of(trade));

        // when
        List<MidStockWithTradesResponse> result = midStockService.getMemberStocksAndTrades(memberId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).midName()).isEqualTo("삼성전자");
        assertThat(result.get(0).trades()).hasSize(1);
    }

    @Test
    @DisplayName("주식 차트 정보 2주치 조회")
    void findStockChartInfo() {
        Long stockId = 1L;
        given(midStockRepository.findById(stockId)).willReturn(Optional.of(sampleStock));
        given(midStockPriceRepository.find2WeeksPriceInfo(stockId))
                .willReturn(List.of(samplePrice));

        List<MidStockPriceResponse> result = midStockService.findStockChartInfo(stockId);

        assertThat(result).hasSize(1);
        MidStockPriceResponse response = result.get(0);
        assertThat(response.highPrice()).isEqualTo(71000L);
        assertThat(response.lowPrice()).isEqualTo(69000L);
        assertThat(response.avgPrice()).isEqualTo(70000L);
        verify(midStockPriceRepository).find2WeeksPriceInfo(stockId);
    }
}
