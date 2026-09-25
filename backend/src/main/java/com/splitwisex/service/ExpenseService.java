package com.splitwisex.service;

import com.splitwisex.dto.expense.CreateExpenseRequest;
import com.splitwisex.dto.expense.ExpenseDto;
import com.splitwisex.entity.Expense;
import com.splitwisex.entity.ExpenseParticipant;
import com.splitwisex.entity.Group;
import com.splitwisex.entity.User;
import com.splitwisex.exception.ExpenseNotFoundException;
import com.splitwisex.exception.NotExpenseOwnerException;
import com.splitwisex.exception.NotGroupMemberException;
import com.splitwisex.exception.UserNotFoundException;
import com.splitwisex.dto.event.GroupEventType;
import com.splitwisex.mapper.ExpenseMapper;
import com.splitwisex.repository.ExpenseParticipantRepository;
import com.splitwisex.repository.ExpenseRepository;
import com.splitwisex.repository.GroupMemberRepository;
import com.splitwisex.repository.UserRepository;
import com.splitwisex.websocket.GroupEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Owns expense creation/lookup/deletion. Every operation re-derives group
 * membership through GroupService rather than trusting anything the caller
 * sent — group ids and user ids in the request are just candidate values
 * until they're checked against group_members.
 */
@Service
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseParticipantRepository participantRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final ExpenseMapper expenseMapper;
    private final GroupEventPublisher groupEventPublisher;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            ExpenseParticipantRepository participantRepository,
            GroupMemberRepository groupMemberRepository,
            UserRepository userRepository,
            GroupService groupService,
            ExpenseMapper expenseMapper,
            GroupEventPublisher groupEventPublisher
    ) {
        this.expenseRepository = expenseRepository;
        this.participantRepository = participantRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.groupService = groupService;
        this.expenseMapper = expenseMapper;
        this.groupEventPublisher = groupEventPublisher;
    }

    @Transactional
    public ExpenseDto createExpense(User currentUser, Long groupId, CreateExpenseRequest request) {
        Group group = groupService.getGroupOrThrow(groupId);
        groupService.assertMembership(groupId, currentUser.getId());

        User payer = userRepository.findById(request.paidBy())
                .orElseThrow(() -> new UserNotFoundException(request.paidBy()));
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, payer.getId())) {
            throw new NotGroupMemberException(groupId, "The payer");
        }

        // De-duplicate while preserving the order the caller supplied, so
        // the equal-split remainder distribution (see ExpenseMapper) is
        // deterministic and unaffected by accidental duplicate ids.
        List<Long> uniqueParticipantIds = new ArrayList<>(new LinkedHashSet<>(request.participantIds()));

        List<User> participants = new ArrayList<>(uniqueParticipantIds.size());
        for (Long participantId : uniqueParticipantIds) {
            User participant = userRepository.findById(participantId)
                    .orElseThrow(() -> new UserNotFoundException(participantId));
            if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, participantId)) {
                throw new NotGroupMemberException(groupId, "Participant '" + participant.getEmail() + "'");
            }
            participants.add(participant);
        }

        Expense expense = Expense.builder()
                .group(group)
                .description(request.description().trim())
                .amount(request.amount())
                .paidBy(payer)
                .build();
        Expense savedExpense = expenseRepository.save(expense);

        List<ExpenseParticipant> participantEntities = participants.stream()
                .map(user -> ExpenseParticipant.builder().expense(savedExpense).user(user).build())
                .toList();
        participantRepository.saveAll(participantEntities);

        // Published now, but only actually broadcast to WebSocket
        // subscribers after this transaction commits — see
        // GroupWebSocketNotifier.
        groupEventPublisher.publish(GroupEventType.EXPENSE_CREATED, groupId, savedExpense.getId());

        return toDto(savedExpense);
    }

    public List<ExpenseDto> getGroupExpenses(User currentUser, Long groupId) {
        groupService.getGroupOrThrow(groupId);
        groupService.assertMembership(groupId, currentUser.getId());
        return expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(this::toDto)
                .toList();
    }

    public ExpenseDto getExpense(User currentUser, Long expenseId) {
        Expense expense = getExpenseOrThrow(expenseId);
        groupService.assertMembership(expense.getGroup().getId(), currentUser.getId());
        return toDto(expense);
    }

    @Transactional
    public void deleteExpense(User currentUser, Long expenseId) {
        Expense expense = getExpenseOrThrow(expenseId);
        groupService.assertMembership(expense.getGroup().getId(), currentUser.getId());

        // Stage 3 permission rule: only the person who paid can delete
        // their own expense.
        if (!expense.getPaidBy().getId().equals(currentUser.getId())) {
            throw new NotExpenseOwnerException(expenseId);
        }

        Long groupId = expense.getGroup().getId();
        expenseRepository.delete(expense);

        groupEventPublisher.publish(GroupEventType.EXPENSE_DELETED, groupId, expenseId);
    }

    private Expense getExpenseOrThrow(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId));
    }

    private ExpenseDto toDto(Expense expense) {
        List<ExpenseParticipant> participants = participantRepository.findByExpenseIdOrderByIdAsc(expense.getId());
        return expenseMapper.toDto(expense, participants);
    }
}
