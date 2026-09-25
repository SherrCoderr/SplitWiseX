package com.splitwisex.websocket;

import com.splitwisex.dto.event.GroupEventType;
import com.splitwisex.event.GroupChangeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * The only thing ExpenseService/GroupService know about Stage 6: "tell the
 * publisher a group changed." It just republishes as a Spring
 * ApplicationEvent — GroupWebSocketNotifier is what actually knows about
 * STOMP/SimpMessagingTemplate, and only reacts after the current
 * transaction commits. This keeps WebSocket concerns out of the business
 * services entirely.
 */
@Component
public class GroupEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public GroupEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(GroupEventType type, Long groupId, Long entityId) {
        eventPublisher.publishEvent(new GroupChangeEvent(type, groupId, entityId));
    }
}
