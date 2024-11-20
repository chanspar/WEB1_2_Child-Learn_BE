package com.prgrms.ijuju.domain.stock.mid.service;

import com.prgrms.ijuju.domain.stock.mid.dto.response.MidStockPriceResponse;
import com.prgrms.ijuju.domain.stock.mid.dto.response.MidStockResponse;
import com.prgrms.ijuju.domain.stock.mid.dto.response.MidStockTradeResponse;
import com.prgrms.ijuju.domain.stock.mid.entity.MidStock;
import com.prgrms.ijuju.domain.stock.mid.entity.MidStockPrice;
import com.prgrms.ijuju.domain.stock.mid.entity.MidStockTrade;
import com.prgrms.ijuju.domain.stock.mid.repository.MidStockPriceRepository;
import com.prgrms.ijuju.domain.stock.mid.repository.MidStockRepository;
import com.prgrms.ijuju.domain.stock.mid.repository.MidStockTradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MidStockService {
    private final MidStockRepository midStockRepository;
    private final MidStockTradeRepository midStockTradeRepository;
    private final MidStockPriceRepository midStockPriceRepository;

    @Transactional(readOnly = true)
    public List<MidStockResponse> findAllStocks() {
        log.info("중급 종목리스트 찾기");
        List<MidStock> stocks = midStockRepository.findAll();
        return stocks.stream()
                .map(MidStockResponse::of)
                .collect(Collectors.toList());
    }

    // 나중에 멤버아이디를 받아서 그걸로 찾아야됨 수정 필요
    @Transactional(readOnly = true)
    public Page<MidStockTradeResponse> findAllStocksTrade(int page, int size) {
        log.info("중급 종목 거래내역 찾기");
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<MidStockTrade> tradePage = midStockTradeRepository.findAllWithMidStock(pageRequest);

        return tradePage.map(MidStockTradeResponse::of);
    }

    @Transactional(readOnly = true)
    public List<MidStockPriceResponse> findStockChartInfo(Long midStockId) {
        log.info("중급 종목 차트 정보 찾기");
        List<MidStockPrice> priceResponses = midStockPriceRepository.find2WeeksPriceInfo(midStockId);
        return priceResponses.stream()
                .map(MidStockPriceResponse::of)
                .collect(Collectors.toList());
    }

}
