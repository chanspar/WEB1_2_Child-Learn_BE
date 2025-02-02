package com.prgrms.ijuju.domain.stock.mid.service;

import com.prgrms.ijuju.domain.stock.mid.entity.MidStock;
import com.prgrms.ijuju.domain.stock.mid.entity.MidStockPrice;
import com.prgrms.ijuju.domain.stock.mid.repository.MidStockPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MidStockChartServiceTest {
    @InjectMocks
    private MidStockChartService chartService;

    @Mock
    private MidStockPriceRepository priceRepository;

    private MidStock stock;

    @BeforeEach
    void setUp() {
        stock = new MidStock("삼성전자");
        ReflectionTestUtils.setField(stock, "id", 1L);
    }

    @Test
    @DisplayName("일일 주식 가격 생성")
    void generateDailyPrice() {
        MidStockPrice lastPrice = MidStockPrice.builder()
                .avgPrice(50000L)
                .highPrice(55000L)
                .lowPrice(45000L)
                .priceDate(LocalDateTime.now())
                .midStock(stock)
                .build();

        given(priceRepository.findLatestPrice(stock)).willReturn(Optional.of(lastPrice));

        chartService.generateDailyPrice(stock);

        verify(priceRepository).deleteOldData(any(LocalDateTime.class));
        verify(priceRepository).save(any(MidStockPrice.class));
    }

    @Test
    @DisplayName("주식 가격 생성 로직 검증")
    void generatePrice() {
        LocalDateTime now = LocalDateTime.now();
        long lastAvgPrice = 50000L;

        MidStockPrice newPrice = chartService.generatePrice(stock, lastAvgPrice, now);

        assertThat(newPrice.getHighPrice()).isGreaterThan(newPrice.getLowPrice());
        assertThat(newPrice.getAvgPrice()).isBetween(newPrice.getLowPrice(), newPrice.getHighPrice());
        assertThat(Math.abs(newPrice.getAvgPrice() - lastAvgPrice))
                .isLessThanOrEqualTo((long)(lastAvgPrice * 0.05));
    }
}
