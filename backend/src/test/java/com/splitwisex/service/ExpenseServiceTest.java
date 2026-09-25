package com.splitwisex.service;

import com.splitwisex.dto.expense.CreateExpenseRequest;
import com.splitwisex.dto.expense.ExpenseDto;
import com.splitwisex.entity.Expense;
import com.splitwisex.entity.ExpenseParticipant;
import com.splitwisex.entity.Group;
import com.splitwisex.entity.User;
import com.splitwisex.exception.NotExpenseOwnerException;
import com.splitwisex.exception.NotGroupMemberException;
import com.splitwisex.exception.UserNotFoundException;
import com.splitwisex.mapper.ExpenseMapper;
import com.splitwisex.mapper.UserMapper;
import com.splitwisex.repository.ExpenseParticipantRepository;
import com.splitwisex.repository.ExpenseRepository;
import com.splitwisex.repository.GroupMemberRepository;
import com.splitwisex.repository.UserRepository;
import com.splitwisex.websocket.GroupEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseParticipantRepository participantRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupService groupService;

    @Mock
    private GroupEventPublisher groupEventPublisher;

    private ExpenseService expenseService;

    private User sameer;
    private User rahul;
    private User arjun;
    private Group goaTrip;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(
                expenseRepository, participantRepository, groupMemberRepository, userRepository,
                groupService, new ExpenseMapper(new UserMapper()), groupEventPublisher
        );

        sameer = User.builder().id(1L).name("Sameer").email("sameer@example.com").passwordHash("h").build();
        rahul = User.builder().id(2L).name("Rahul").email("rahul@example.com").passwordHash("h").build();
        arjun = User.builder().id(3L).name("Arjun").email("arjun@example.com").passwordHash("h").build();
        goaTrip = Group.builder().id(10L).name("Goa Trip").createdBy(sameer).createdAt(Instant.now()).build();
    }

    @Test
    void memberCanCreateAnExpenseAndTheEqualShareIsCalculatedCorrectly() {
        CreateExpenseRequest request = new CreateExpenseRequest(
                "Dinner", new BigDecimal("900.00"), 1L, List.of(1L, 2L, 3L)
        );

        when(groupService.getGroupOrThrow(10L)).thenReturn(goaTrip);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sameer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(rahul));
        when(userRepository.findById(3L)).thenReturn(Optional.of(arjun));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(true);
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(true);
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 3L)).thenReturn(true);

        Expense savedExpense = Expense.builder()
                .id(500L).group(goaTrip).description("Dinner")
                .amount(new BigDecimal("900.00")).paidBy(sameer).createdAt(Instant.now()).build();
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        when(participantRepository.findByExpenseIdOrderByIdAsc(500L)).thenReturn(List.of(
                ExpenseParticipant.builder().id(1L).expense(savedExpense).user(sameer).build(),
                ExpenseParticipant.builder().id(2L).expense(savedExpense).user(rahul).build(),
                ExpenseParticipant.builder().id(3L).expense(savedExpense).user(arjun).build()
        ));

        ExpenseDto result = expenseService.createExpense(sameer, 10L, request);

        assertThat(result.amount()).isEqualByComparingTo("900.00");
        assertThat(result.participants()).hasSize(3);
        assertThat(result.participants()).allSatisfy(p -> assertThat(p.share()).isEqualByComparingTo("300.00"));

        verify(participantRepository).saveAll(anyList());
    }

    @Test
    void nonMemberCannotCreateAnExpense() {
        CreateExpenseRequest request = new CreateExpenseRequest(
                "Dinner", new BigDecimal("900.00"), 1L, List.of(1L, 2L)
        );

        when(groupService.getGroupOrThrow(10L)).thenReturn(goaTrip);
        org.mockito.Mockito.doThrow(new NotGroupMemberException(10L))
                .when(groupService).assertMembership(10L, 4L);

        User outsider = User.builder().id(4L).name("Outsider").email("outsider@example.com").passwordHash("h").build();

        assertThatThrownBy(() -> expenseService.createExpense(outsider, 10L, request))
                .isInstanceOf(NotGroupMemberException.class);

        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    void payerMustBelongToTheGroup() {
        CreateExpenseRequest request = new CreateExpenseRequest(
                "Dinner", new BigDecimal("900.00"), 2L, List.of(1L, 2L)
        );

        when(groupService.getGroupOrThrow(10L)).thenReturn(goaTrip);
        when(userRepository.findById(2L)).thenReturn(Optional.of(rahul));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> expenseService.createExpense(sameer, 10L, request))
                .isInstanceOf(NotGroupMemberException.class);

        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    void everyParticipantMustBelongToTheGroup() {
        CreateExpenseRequest request = new CreateExpenseRequest(
                "Dinner", new BigDecimal("900.00"), 1L, List.of(1L, 99L)
        );

        when(groupService.getGroupOrThrow(10L)).thenReturn(goaTrip);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sameer));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(true);

        User outsider = User.builder().id(99L).name("Outsider").email("outsider@example.com").passwordHash("h").build();
        when(userRepository.findById(99L)).thenReturn(Optional.of(outsider));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> expenseService.createExpense(sameer, 10L, request))
                .isInstanceOf(NotGroupMemberException.class);

        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    void aNonexistentPayerFailsWithUserNotFound() {
        CreateExpenseRequest request = new CreateExpenseRequest(
                "Dinner", new BigDecimal("900.00"), 999L, List.of(1L)
        );

        when(groupService.getGroupOrThrow(10L)).thenReturn(goaTrip);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.createExpense(sameer, 10L, request))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void expenseCanBeRetrievedByAGroupMember() {
        Expense expense = Expense.builder()
                .id(500L).group(goaTrip).description("Dinner")
                .amount(new BigDecimal("900.00")).paidBy(sameer).createdAt(Instant.now()).build();
        when(expenseRepository.findById(500L)).thenReturn(Optional.of(expense));
        when(participantRepository.findByExpenseIdOrderByIdAsc(500L)).thenReturn(List.of(
                ExpenseParticipant.builder().id(1L).expense(expense).user(sameer).build()
        ));

        ExpenseDto result = expenseService.getExpense(sameer, 500L);

        assertThat(result.description()).isEqualTo("Dinner");
        verify(groupService).assertMembership(10L, 1L);
    }

    @Test
    void expenseCanBeDeletedByThePayer() {
        Expense expense = Expense.builder()
                .id(500L).group(goaTrip).description("Dinner")
                .amount(new BigDecimal("900.00")).paidBy(sameer).createdAt(Instant.now()).build();
        when(expenseRepository.findById(500L)).thenReturn(Optional.of(expense));

        expenseService.deleteExpense(sameer, 500L);

        verify(expenseRepository).delete(expense);
    }

    @Test
    void aGroupMemberWhoDidNotPayCannotDeleteTheExpense() {
        Expense expense = Expense.builder()
                .id(500L).group(goaTrip).description("Dinner")
                .amount(new BigDecimal("900.00")).paidBy(sameer).createdAt(Instant.now()).build();
        when(expenseRepository.findById(500L)).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> expenseService.deleteExpense(rahul, 500L))
                .isInstanceOf(NotExpenseOwnerException.class);

        verify(expenseRepository, never()).delete(any(Expense.class));
    }
}
