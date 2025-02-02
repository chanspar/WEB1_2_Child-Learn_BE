package com.prgrms.ijuju.domain.stock.mid.repository;

import com.prgrms.ijuju.domain.member.entity.Member;
import com.prgrms.ijuju.domain.member.entity.Role;
import com.prgrms.ijuju.domain.stock.mid.entity.MidStock;
import com.prgrms.ijuju.domain.stock.mid.entity.MidStockTrade;
import com.prgrms.ijuju.domain.stock.mid.entity.TradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class MidStockTradeRepositoryTest {

    @Autowired
    private MidStockTradeRepository midStockTradeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Member testMember;
    private MidStock testMidStock;
    private LocalDate currentDate;

    @BeforeEach
    void setUp() {
        currentDate = LocalDate.now();

        testMember = Member.builder()
                .loginId("testUser")
                .pw("password")
                .username("Test User")
                .email("test@example.com")
                .birth(LocalDate.of(1990, 1, 1))
                .role(Role.USER)
                .build();
        entityManager.persist(testMember);

        testMidStock = new MidStock("Test Stock");
        entityManager.persist(testMidStock);

        MidStockTrade buyTrade = MidStockTrade.builder()
                .tradePoint(1000L)
                .pricePerStock(100L)
                .tradeType(TradeType.BUY)
                .member(testMember)
                .midStock(testMidStock)
                .build();
        entityManager.persist(buyTrade);

        entityManager.flush();
    }

    @Test
    @DisplayName("회원의 모든 매수 종목 조회 테스트")
    void findAllBuyMidStock() {
        // when
        List<MidStockTrade> trades = midStockTradeRepository.findAllBuyMidStock(testMember.getId());

        // then
        assertThat(trades).isNotEmpty();
        assertThat(trades).allMatch(trade -> trade.getTradeType() == TradeType.BUY);
        assertThat(trades).allMatch(trade -> trade.getMember().getId().equals(testMember.getId()));
    }

    @Test
    @DisplayName("회원의 특정 매수 종목 조회 테스트")
    void findBuyMidStock() {
        // when
        List<MidStockTrade> trades = midStockTradeRepository.findBuyMidStock(
                testMember.getId(),
                testMidStock.getId()
        );

        // then
        assertThat(trades).isNotEmpty();
        assertThat(trades).allMatch(trade -> trade.getMidStock().getId().equals(testMidStock.getId()));
        assertThat(trades).allMatch(trade -> trade.getTradeType() == TradeType.BUY);
    }

    @Test
    @DisplayName("오늘 매수 여부 확인 테스트")
    void findTodayBuyMidStock() {
        // when
        Optional<MidStockTrade> trade = midStockTradeRepository.findTodayBuyMidStock(
                testMember.getId(),
                testMidStock.getId(),
                currentDate
        );

        // then
        assertThat(trade).isPresent();
        assertThat(trade.get().getMember().getId()).isEqualTo(testMember.getId());
        assertThat(trade.get().getMidStock().getId()).isEqualTo(testMidStock.getId());
    }

    @Test
    @DisplayName("오늘 매도 여부 확인 테스트")
    void findTodaySellMidStock() {
        // given
        MidStockTrade sellTrade = MidStockTrade.builder()
                .tradePoint(1000L)
                .pricePerStock(120L)
                .tradeType(TradeType.SELL)
                .member(testMember)
                .midStock(testMidStock)
                .build();
        entityManager.persist(sellTrade);
        entityManager.flush();

        // when
        Optional<MidStockTrade> trade = midStockTradeRepository.findTodaySellMidStock(
                testMember.getId(),
                testMidStock.getId(),
                currentDate
        );

        // then
        assertThat(trade).isPresent();
        assertThat(trade.get().getTradeType()).isEqualTo(TradeType.SELL);
        assertThat(trade.get().getMember().getId()).isEqualTo(testMember.getId());
        assertThat(trade.get().getMidStock().getId()).isEqualTo(testMidStock.getId());
    }

    @Test
    @DisplayName("다른 날짜의 매수 거래는 조회되지 않아야 함")
    void shouldNotFindBuyTradeFromDifferentDate() {
        // when
        Optional<MidStockTrade> trade = midStockTradeRepository.findTodayBuyMidStock(
                testMember.getId(),
                testMidStock.getId(),
                currentDate.plusDays(1) // 내일 날짜로 검색
        );

        // then
        assertThat(trade).isEmpty();
    }

    @Test
    @DisplayName("다른 날짜의 매도 거래는 조회되지 않아야 함")
    void shouldNotFindSellTradeFromDifferentDate() {
        // given
        MidStockTrade sellTrade = MidStockTrade.builder()
                .tradePoint(1000L)
                .pricePerStock(120L)
                .tradeType(TradeType.SELL)
                .member(testMember)
                .midStock(testMidStock)
                .build();
        entityManager.persist(sellTrade);
        entityManager.flush();

        // when
        Optional<MidStockTrade> trade = midStockTradeRepository.findTodaySellMidStock(
                testMember.getId(),
                testMidStock.getId(),
                currentDate.plusDays(1)
        );

        // then
        assertThat(trade).isEmpty();
    }
}
