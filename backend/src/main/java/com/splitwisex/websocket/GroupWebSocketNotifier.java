package com.splitwisex.websocket;

import com.splitwisex.dto.event.GroupEventMessage;
import com.splitwisex.event.GroupChangeEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

/**
 * Bridges committed GroupChangeEvents onto /topic/groups/{groupId}.
 *
 * Using @TransactionalEventListener(phase = AFTER_COMMIT) instead of
 * publishing directly from ExpenseService/GroupService is what guarantees
 * "publish only after the database operation succeeds": if the
 * transaction rolls back, this listener simply never runs, with no
 * try/catch bookkeeping needed in the service layer.
 */
@Component
public class GroupWebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public GroupWebSocketNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGroupChange(GroupChangeEvent event) {
        GroupEventMessage message = new GroupEventMessage(
                event.type(), event.groupId(), event.entityId(), Instant.now()
        );
        messagingTemplate.convertAndSend("/topic/groups/" + event.groupId(), message);
    }
}
