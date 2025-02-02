package com.prgrms.ijuju.domain.stock.mid.service;

import com.prgrms.ijuju.domain.member.entity.Member;
import com.prgrms.ijuju.domain.member.repository.MemberRepository;
import com.prgrms.ijuju.domain.stock.mid.entity.MidStock;
import com.prgrms.ijuju.domain.stock.mid.entity.MidStockPrice;
import com.prgrms.ijuju.domain.stock.mid.entity.MidStockTrade;
import com.prgrms.ijuju.domain.stock.mid.entity.TradeType;
import com.prgrms.ijuju.domain.stock.mid.repository.MidStockPriceRepository;
import com.prgrms.ijuju.domain.stock.mid.repository.MidStockRepository;
import com.prgrms.ijuju.domain.stock.mid.repository.MidStockTradeRepository;
import com.prgrms.ijuju.domain.wallet.dto.request.StockPointRequestDTO;
import com.prgrms.ijuju.domain.wallet.entity.Wallet;
import com.prgrms.ijuju.domain.wallet.repository.WalletRepository;
import com.prgrms.ijuju.domain.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MidStockTradeServiceTest {
    @InjectMocks
    private MidStockTradeService tradeService;

    @Mock
    private MidStockTradeRepository tradeRepository;
    @Mock
    private MidStockRepository stockRepository;
    @Mock
    private MidStockPriceRepository priceRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private WalletRepository walletRepository;

    private MidStock stock;
    private Member member;
    private Wallet wallet;
    private MidStockPrice price;

    @BeforeEach
    void setUp() {
        stock = new MidStock("삼성전자");
        ReflectionTestUtils.setField(stock, "id", 1L);

        member = Member.builder().id(1L).build();

        wallet = Wallet.builder()
                .currentPoints(100000L)
                .member(member)
                .build();

        price = MidStockPrice.builder()
                .avgPrice(50000L)
                .highPrice(55000L)
                .lowPrice(45000L)
                .priceDate(LocalDateTime.now())
                .midStock(stock)
                .build();
    }

    @Test
    @DisplayName("주식 매수 성공")
    void buyStock() {
        given(stockRepository.findById(1L)).willReturn(Optional.of(stock));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(walletRepository.findByMemberId(1L)).willReturn(Optional.of(wallet));
        given(tradeRepository.findTodayBuyMidStock(1L, 1L, LocalDate.now())).willReturn(Optional.empty());
        given(priceRepository.findTodayPrice(1L)).willReturn(Optional.of(price));

        boolean result = tradeService.buyStock(1L, 1L, 50000L);

        assertThat(result).isFalse();
        verify(tradeRepository).save(any(MidStockTrade.class));
        verify(walletService).simulateStockInvestment(any(StockPointRequestDTO.class));
    }

    @Test
    @DisplayName("주식 매도 성공")
    void sellStock() {
        MidStockTrade trade = MidStockTrade.builder()
                .midStock(stock)
                .member(member)
                .tradePoint(50000L)
                .pricePerStock(50000L)
                .tradeType(TradeType.BUY)
                .build();

        given(tradeRepository.findBuyMidStock(1L, 1L)).willReturn(List.of(trade));
        given(tradeRepository.findTodaySellMidStock(1L, 1L, LocalDate.now())).willReturn(Optional.empty());
        given(priceRepository.findTodayAvgPrice(1L)).willReturn(55000L);

        var result = tradeService.sellStock(1L, 1L);

        assertThat(result).containsKey("totalPoints");
        assertThat(result).containsKey("earnedPoints");
        verify(walletService).simulateStockInvestment(any(StockPointRequestDTO.class));
    }
}
