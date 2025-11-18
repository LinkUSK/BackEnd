// src/main/java/com/example/demo/chat/StompAuthChannelInterceptor.java
package com.example.demo.chat;

import com.example.demo.security.JwtTokenProvider;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null) return message;

        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            String auth = acc.getFirstNativeHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                if (jwtTokenProvider.validate(token)) {
                    // 토큰 subject = userId(문자열) 이라고 가정
                    String userIdStr = jwtTokenProvider.getUsername(token);
                    User u = userRepository.findByUserId(userIdStr)
                            .orElse(null);
                    // Principal 이름을 **DB PK(Long)** 문자열로 심는다.
                    String principalName = (u != null) ? String.valueOf(u.getId()) : userIdStr;

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principalName, // ★ 여기서 Long id 문자열
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    acc.setUser(authentication);
                }
            }
        }
        return message;
    }
}
