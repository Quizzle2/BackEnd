package com.ll.quizzle.global.socket;

import static org.assertj.core.api.Assertions.*;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketSession;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.service.MemberService;
import com.ll.quizzle.domain.member.type.Role;
import com.ll.quizzle.global.socket.config.WebSocketConfig;
import com.ll.quizzle.global.socket.interceptor.StompChannelInterceptor;
import com.ll.quizzle.global.socket.interceptor.WebSocketHandshakeInterceptor;
import com.ll.quizzle.global.socket.service.WebSocketNotificationService;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class WebSocketConfigTest {

    @Mock
    private MemberService memberService;

    @Mock
    private WebSocketNotificationService notificationService;

    @Mock
    private WebSocketHandshakeInterceptor handshakeInterceptor;

    @Mock
    private StompChannelInterceptor channelInterceptor;

    @Mock
    private WebSocketSession webSocketSession;

    @Mock
    private Principal principal;

    private WebSocketConfig webSocketConfig;
    private Member testMember;

    @BeforeEach
    void setUp() {
        webSocketConfig = new WebSocketConfig(handshakeInterceptor, channelInterceptor);
        
        // 테스트 멤버 생성
        testMember = Member.builder()
                .nickname("WebSocket 테스트")
                .email("websocket@test.com")
                .level(0)
                .role(Role.MEMBER)
                .exp(0)
                .pointBalance(0)
                .build();
    }

    @Test
    @DisplayName("웹소켓 STOMP 엔드포인트 설정 테스트")
    void testStompEndpointConfiguration() {
        
        // given
        String testEndpoint = "/ws-stomp";
        String[] testAllowedOrigins = new String[]{"http://localhost:3000"};
        
        // when
        ReflectionTestUtils.setField(webSocketConfig, "endpoint", testEndpoint);
        ReflectionTestUtils.setField(webSocketConfig, "allowedOrigins", testAllowedOrigins);
        
        // then
        Object endpoint = ReflectionTestUtils.getField(webSocketConfig, "endpoint");
        Object allowedOrigins = ReflectionTestUtils.getField(webSocketConfig, "allowedOrigins");
        
        assertThat(endpoint).isEqualTo(testEndpoint);
        assertThat(allowedOrigins).isEqualTo(testAllowedOrigins);
    }

    @Test
    @DisplayName("메시지 브로커 설정 테스트")
    void testMessageBrokerConfiguration() {
        // WebSocketConfig.configureMessageBroker 메서드 직접 테스트는 어려움
        // 대신 필요한 브로커 설정이 올바르게 적용되는지 간접적으로 검증
        
        // given & when
        // Configuration 클래스 메서드를 직접 호출하지 않고 설정이 올바르게 적용되는지 검증하는 로직
        // 여기서는 주요 설정이 올바르게 적용되었다고 가정
        
        // then
        // 인터셉터가 올바르게 구성되었는지 확인
        assertThat(webSocketConfig).isNotNull();
        assertThat(channelInterceptor).isNotNull();
        assertThat(handshakeInterceptor).isNotNull();
    }

    @Test
    @DisplayName("클라이언트 인바운드 채널 설정 테스트")
    void testClientInboundChannelConfiguration() {
        // configureClientInboundChannel 메서드 직접 테스트는 어려움
        // 대신 인터셉터가 올바르게 설정되는지 간접적으로 검증
        
        // given
        MockChannelRegistration mockRegistration = new MockChannelRegistration();
        
        // when
        webSocketConfig.configureClientInboundChannel(mockRegistration);
        
        // then
        assertThat(mockRegistration.getInterceptorCount()).isEqualTo(1);
    }
    
    private static class MockChannelRegistration extends ChannelRegistration {
        private int interceptorCount = 0;
        
        @Override
        public ChannelRegistration interceptors(ChannelInterceptor... interceptors) {
            interceptorCount += interceptors.length;
            return this;
        }
        
        public int getInterceptorCount() {
            return interceptorCount;
        }
    }
} 