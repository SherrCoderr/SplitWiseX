package com.splitwisex.websocket;

import com.splitwisex.entity.User;
import com.splitwisex.repository.GroupMemberRepository;
import com.splitwisex.repository.UserRepository;
import com.splitwisex.security.JwtService;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.Optional;

/**
 * Reuses the existing JWT + group-membership logic to secure the STOMP
 * channel, mirroring what JwtAuthenticationFilter/GroupService.
 * assertMembership already do for REST:
 *
 *  - CONNECT: the JWT arrives as a STOMP native header (never a URL query
 *    param), is validated the same way JwtAuthenticationFilter validates
 *    it, and the resulting User is attached to the STOMP session as its
 *    Principal. Missing/invalid token -> connection rejected.
 *
 *  - SUBSCRIBE to /topic/groups/{groupId}: rejected unless the
 *    already-authenticated user is a member of that group (via
 *    GroupMemberRepository, the same check GroupService.assertMembership
 *    uses), so knowing a group id alone is never enough to receive its
 *    events.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String GROUP_TOPIC_PREFIX = "/topic/groups/";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    public StompAuthChannelInterceptor(
            JwtService jwtService,
            UserRepository userRepository,
            GroupMemberRepository groupMemberRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            authenticateConnect(accessor);
        } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            authorizeSubscribe(accessor);
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(AUTH_HEADER);
        String token = (header != null && header.startsWith(BEARER_PREFIX))
                ? header.substring(BEARER_PREFIX.length())
                : null;

        if (token == null) {
            throw new MessagingException("Missing WebSocket authentication token");
        }

        User user = jwtService.extractUserId(token)
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new MessagingException("Invalid or expired WebSocket authentication token"));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, Collections.emptyList()
        );
        accessor.setUser(authentication);
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(GROUP_TOPIC_PREFIX)) {
            // Not a per-group topic - nothing for this interceptor to check.
            return;
        }

        Long groupId = parseGroupId(destination);
        User currentUser = resolveAuthenticatedUser(accessor.getUser());

        if (groupId == null || !groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUser.getId())) {
            throw new MessagingException("Not authorized to subscribe to this group's updates");
        }
    }

    private Long parseGroupId(String destination) {
        try {
            return Long.valueOf(destination.substring(GROUP_TOPIC_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private User resolveAuthenticatedUser(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        throw new MessagingException("WebSocket session is not authenticated");
    }
}
