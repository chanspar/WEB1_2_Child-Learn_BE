package com.prgrms.ijuju.global.auth;

import com.prgrms.ijuju.global.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
                // Query Parameter에서 토큰 확인
            String jwtToken = null;
            String query = request.getURI().getQuery();
            if (query != null) {
                // 모든 쿼리 파라미터를 Map으로 변환
                query = query.replace("+", " ");

                Map<String, String> queryParams = Arrays.stream(query.split("&"))
                        .map(param -> param.split("=", 2)) // '='로 나누되, 최대 2개만 분리
                        .filter(pair -> pair.length == 2) // 유효한 key-value 쌍만 유지
                        .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));

                jwtToken = queryParams.get("authorization");
            }
            if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
                jwtToken = jwtToken.substring("Bearer ".length());
            }

            if (jwtToken == null) {
                log.warn("JWT 토큰이 요청에 포함되지 않았습니다.");
                attributes.put("errorMessage", "JWT 토큰이 요청에 포함되지 않았습니다.");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // JWT 검증
            Claims claims = JwtUtil.decode(jwtToken);
            attributes.put("userClaims", claims); // 검증된 클레임을 WebSocket 세션에 저장
            log.info("WebSocket 연결 성공: {}", claims);
            return true;

        } catch (Exception e) {
            log.error("JWT 검증 실패: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 필요시 후처리 로직 작성
        log.info("WebSocket Handshake 완료");
    }
}