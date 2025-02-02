package com.prgrms.ijuju.domain.stock.mid.repository;

import com.prgrms.ijuju.domain.stock.mid.entity.MidStock;
import com.prgrms.ijuju.domain.stock.mid.entity.MidStockPrice;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(MidStockPriceRepositoryImpl.class)
class MidStockPriceRepositoryTest {

    @Autowired
    private MidStockPriceRepository midStockPriceRepository;

    @Autowired
    private EntityManager entityManager;

    private MidStock midStock;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        // 테스트의 now를 오늘 날짜의 자정으로 설정
        now = LocalDateTime.now().withHour(0).withMinute(1);
        midStock = new MidStock("Test Stock");
        entityManager.persist(midStock);

        createTestData();
        entityManager.flush();
        entityManager.clear();
    }

    private void createTestData() {
        // 3주 전 데이터
        LocalDateTime threeWeeksAgo = now.minusWeeks(3);
        createAndPersistPrice(10000L, 8000L, 9000L, threeWeeksAgo);

        // 2주 전 데이터
        LocalDateTime twoWeeksAgo = now.minusWeeks(2);
        createAndPersistPrice(11000L, 9000L, 10000L, twoWeeksAgo);

        // 1주 전 데이터
        LocalDateTime oneWeekAgo = now.minusWeeks(1);
        createAndPersistPrice(12000L, 10000L, 11000L, oneWeekAgo);

        // 오늘 데이터
        createAndPersistPrice(13000L, 11000L, 12000L, now);

        // 다음 주 데이터
        createAndPersistPrice(14000L, 12000L, 13000L, now.plusWeeks(1));
    }

    private void createAndPersistPrice(Long highPrice, Long lowPrice, Long avgPrice, LocalDateTime priceDate) {
        MidStockPrice price = MidStockPrice.builder()
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .avgPrice(avgPrice)
                .priceDate(priceDate)
                .midStock(midStock)
                .build();
        entityManager.persist(price);
    }

    @Test
    @DisplayName("오래된 데이터 삭제 테스트")
    void deleteOldDataTest() {
        LocalDateTime cutoffDate = now.minusWeeks(2).minusDays(1);

        midStockPriceRepository.deleteOldData(cutoffDate);
        entityManager.flush();
        entityManager.clear();

        List<MidStockPrice> remainingPrices = midStockPriceRepository.findByMidStockId(midStock.getId());
        assertThat(remainingPrices).hasSize(4);
        assertThat(remainingPrices)
                .allMatch(price -> price.getPriceDate().isAfter(cutoffDate) ||
                        price.getPriceDate().isEqual(cutoffDate));
    }

    @Test
    @DisplayName("오늘의 평균 가격 조회 테스트")
    void findTodayAvgPriceTest() {
        // 테스트 데이터가 오늘 날짜의 자정으로 설정되어 있음
        long todayAvgPrice = midStockPriceRepository.findTodayAvgPrice(midStock.getId());

        assertThat(todayAvgPrice).isEqualTo(12000L);
    }

    @Test
    @DisplayName("최근 가격 조회 테스트")
    void findLatestPriceTest() {
        Optional<MidStockPrice> latestPrice = midStockPriceRepository.findLatestPrice(midStock);

        assertThat(latestPrice).isPresent();
        assertThat(latestPrice.get().getAvgPrice()).isEqualTo(13000L);
    }

    @Test
    @DisplayName("2주 가격 정보 조회 테스트")
    void find2WeeksPriceInfoTest() {
        List<MidStockPrice> twoWeeksPrices = midStockPriceRepository.find2WeeksPriceInfo(midStock.getId());

        assertThat(twoWeeksPrices).hasSize(3);
        assertThat(twoWeeksPrices.get(0).getAvgPrice()).isEqualTo(10000L);
        assertThat(twoWeeksPrices.get(twoWeeksPrices.size() - 1).getAvgPrice()).isEqualTo(12000L);
    }

    @Test
    @DisplayName("오늘 가격 조회 테스트")
    void findTodayPriceTest() {
        Optional<MidStockPrice> todayPrice = midStockPriceRepository.findTodayPrice(midStock.getId());

        assertThat(todayPrice).isPresent();
        assertThat(todayPrice.get().getAvgPrice()).isEqualTo(12000L);
    }

    @Test
    @DisplayName("특정 주식의 모든 가격 정보 조회 테스트")
    void findByMidStockIdTest() {
        List<MidStockPrice> allPrices = midStockPriceRepository.findByMidStockId(midStock.getId());

        assertThat(allPrices).hasSize(5);
    }

    @Test
    @DisplayName("향후 2주 가격 정보 조회 테스트")
    void findFuture2WeeksPriceInfoTest() {
        List<MidStockPrice> futurePrices = midStockPriceRepository.findFuture2WeeksPriceInfo(midStock.getId());

        assertThat(futurePrices).hasSize(2);
        assertThat(futurePrices)
                .allMatch(price -> price.getPriceDate().isAfter(now.minusDays(1)));
    }
}
