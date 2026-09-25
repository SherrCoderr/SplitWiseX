package com.splitwisex.service;

import com.splitwisex.dto.balance.GroupBalanceDto;
import com.splitwisex.dto.balance.MemberBalanceDto;
import com.splitwisex.dto.balance.SettlementDto;
import com.splitwisex.entity.Expense;
import com.splitwisex.entity.ExpenseParticipant;
import com.splitwisex.entity.Group;
import com.splitwisex.entity.GroupMember;
import com.splitwisex.entity.User;
import com.splitwisex.exception.NotGroupMemberException;
import com.splitwisex.mapper.ExpenseMapper;
import com.splitwisex.mapper.UserMapper;
import com.splitwisex.repository.ExpenseParticipantRepository;
import com.splitwisex.repository.ExpenseRepository;
import com.splitwisex.repository.GroupMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private GroupService groupService;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseParticipantRepository participantRepository;

    private BalanceService balanceService;

    private User sameer;
    private User rahul;
    private User arjun;
    private Group goaTrip;
    private List<GroupMember> members;

    @BeforeEach
    void setUp() {
        balanceService = new BalanceService(
                groupService, groupMemberRepository, expenseRepository, participantRepository,
                new ExpenseMapper(new UserMapper()), new SettlementCalculator()
        );

        sameer = User.builder().id(1L).name("Sameer").email("sameer@example.com").passwordHash("h").build();
        rahul = User.builder().id(2L).name("Rahul").email("rahul@example.com").passwordHash("h").build();
        arjun = User.builder().id(3L).name("Arjun").email("arjun@example.com").passwordHash("h").build();
        goaTrip = Group.builder().id(10L).name("Goa Trip").createdBy(sameer).createdAt(Instant.now()).build();

        members = List.of(
                GroupMember.builder().id(100L).group(goaTrip).user(sameer).joinedAt(Instant.now()).build(),
                GroupMember.builder().id(101L).group(goaTrip).user(rahul).joinedAt(Instant.now()).build(),
                GroupMember.builder().id(102L).group(goaTrip).user(arjun).joinedAt(Instant.now()).build()
        );
    }

    private Expense expense(long id, BigDecimal amount, User paidBy) {
        return Expense.builder().id(id).group(goaTrip).description("Expense " + id)
                .amount(amount).paidBy(paidBy).createdAt(Instant.now()).build();
    }

    private ExpenseParticipant participant(long id, Expense expense, User user) {
        return ExpenseParticipant.builder().id(id).expense(expense).user(user).build();
    }

    @Test
    void onePayerTwoParticipants_balancesMatchExpectedNetAmounts() {
        // Dinner ₹900, Sameer pays, Sameer+Rahul+Arjun all participate.
        Expense dinner = expense(500L, new BigDecimal("900.00"), sameer);

        when(groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(10L)).thenReturn(members);
        when(expenseRepository.findByGroupIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(dinner));
        when(participantRepository.findAllForGroup(10L)).thenReturn(List.of(
                participant(1L, dinner, sameer),
                participant(2L, dinner, rahul),
                participant(3L, dinner, arjun)
        ));

        GroupBalanceDto result = balanceService.getGroupBalances(sameer, 10L);

        Map<Long, MemberBalanceDto> byUserId = toMap(result);
        assertThat(byUserId.get(1L).netBalance()).isEqualByComparingTo("600.00");
        assertThat(byUserId.get(2L).netBalance()).isEqualByComparingTo("-300.00");
        assertThat(byUserId.get(3L).netBalance()).isEqualByComparingTo("-300.00");

        assertSumIsZero(result);
    }

    @Test
    void onePayerTwoParticipants_settlementMatchesExpectation() {
        Expense dinner = expense(500L, new BigDecimal("900.00"), sameer);

        when(groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(10L)).thenReturn(members);
        when(expenseRepository.findByGroupIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(dinner));
        when(participantRepository.findAllForGroup(10L)).thenReturn(List.of(
                participant(1L, dinner, sameer),
                participant(2L, dinner, rahul),
                participant(3L, dinner, arjun)
        ));

        List<SettlementDto> settlements = balanceService.getGroupSettlements(sameer, 10L);

        assertThat(settlements).hasSize(2);
        assertThat(settlements).allSatisfy(s -> assertThat(s.to().name()).isEqualTo("Sameer"));
        assertThat(settlements.stream().map(s -> s.from().name()).toList())
                .containsExactlyInAnyOrder("Rahul", "Arjun");
        assertThat(settlements.stream().map(SettlementDto::amount).toList())
                .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("300.00"));
    }

    @Test
    void multipleExpenses_totalsAccumulateAcrossExpenses() {
        // Sameer pays ₹900 (all three participate), Rahul pays ₹600 (all
        // three participate).
        Expense dinner = expense(500L, new BigDecimal("900.00"), sameer);
        Expense cab = expense(501L, new BigDecimal("600.00"), rahul);

        when(groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(10L)).thenReturn(members);
        when(expenseRepository.findByGroupIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(dinner, cab));
        when(participantRepository.findAllForGroup(10L)).thenReturn(List.of(
                participant(1L, dinner, sameer),
                participant(2L, dinner, rahul),
                participant(3L, dinner, arjun),
                participant(4L, cab, sameer),
                participant(5L, cab, rahul),
                participant(6L, cab, arjun)
        ));

        GroupBalanceDto result = balanceService.getGroupBalances(sameer, 10L);
        Map<Long, MemberBalanceDto> byUserId = toMap(result);

        assertThat(byUserId.get(1L).totalPaid()).isEqualByComparingTo("900.00");
        assertThat(byUserId.get(2L).totalPaid()).isEqualByComparingTo("600.00");
        assertThat(byUserId.get(3L).totalPaid()).isEqualByComparingTo("0.00");

        assertThat(byUserId.get(1L).totalShare()).isEqualByComparingTo("500.00");
        assertThat(byUserId.get(2L).totalShare()).isEqualByComparingTo("500.00");
        assertThat(byUserId.get(3L).totalShare()).isEqualByComparingTo("500.00");

        assertThat(byUserId.get(1L).netBalance()).isEqualByComparingTo("400.00");
        assertThat(byUserId.get(2L).netBalance()).isEqualByComparingTo("100.00");
        assertThat(byUserId.get(3L).netBalance()).isEqualByComparingTo("-500.00");

        assertSumIsZero(result);

        List<SettlementDto> settlements = balanceService.getGroupSettlements(sameer, 10L);
        assertThat(settlements).hasSize(2);
        assertThat(settlements.get(0).from().name()).isEqualTo("Arjun");
        assertThat(settlements.get(0).to().name()).isEqualTo("Sameer");
        assertThat(settlements.get(0).amount()).isEqualByComparingTo("400.00");
        assertThat(settlements.get(1).from().name()).isEqualTo("Arjun");
        assertThat(settlements.get(1).to().name()).isEqualTo("Rahul");
        assertThat(settlements.get(1).amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void groupWithNoExpensesIsAlreadySettled() {
        when(groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(10L)).thenReturn(members);
        when(expenseRepository.findByGroupIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

        GroupBalanceDto result = balanceService.getGroupBalances(sameer, 10L);

        assertThat(result.members()).hasSize(3);
        assertThat(result.members()).allSatisfy(m -> {
            assertThat(m.totalPaid()).isEqualByComparingTo("0.00");
            assertThat(m.totalShare()).isEqualByComparingTo("0.00");
            assertThat(m.netBalance()).isEqualByComparingTo("0.00");
        });

        assertThat(balanceService.getGroupSettlements(sameer, 10L)).isEmpty();
    }

    @Test
    void unevenThreeWaySplitStillSumsToZero() {
        // ₹100 split 3 ways: 33.34/33.33/33.33 (see ExpenseMapperTest).
        Expense lunch = expense(500L, new BigDecimal("100.00"), sameer);

        when(groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(10L)).thenReturn(members);
        when(expenseRepository.findByGroupIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(lunch));
        when(participantRepository.findAllForGroup(10L)).thenReturn(List.of(
                participant(1L, lunch, sameer),
                participant(2L, lunch, rahul),
                participant(3L, lunch, arjun)
        ));

        GroupBalanceDto result = balanceService.getGroupBalances(sameer, 10L);
        assertSumIsZero(result);

        Map<Long, MemberBalanceDto> byUserId = toMap(result);
        assertThat(byUserId.get(1L).netBalance()).isEqualByComparingTo("66.66"); // 100.00 - 33.34
        assertThat(byUserId.get(2L).netBalance()).isEqualByComparingTo("-33.33");
        assertThat(byUserId.get(3L).netBalance()).isEqualByComparingTo("-33.33");
    }

    @Test
    void nonMemberCannotViewBalances() {
        User outsider = User.builder().id(99L).name("Outsider").email("outsider@example.com").passwordHash("h").build();
        doThrow(new NotGroupMemberException(10L)).when(groupService).assertMembership(10L, 99L);

        assertThatThrownBy(() -> balanceService.getGroupBalances(outsider, 10L))
                .isInstanceOf(NotGroupMemberException.class);
    }

    @Test
    void nonMemberCannotViewSettlements() {
        User outsider = User.builder().id(99L).name("Outsider").email("outsider@example.com").passwordHash("h").build();
        doThrow(new NotGroupMemberException(10L)).when(groupService).assertMembership(10L, 99L);

        assertThatThrownBy(() -> balanceService.getGroupSettlements(outsider, 10L))
                .isInstanceOf(NotGroupMemberException.class);
    }

    private static Map<Long, MemberBalanceDto> toMap(GroupBalanceDto dto) {
        return dto.members().stream()
                .collect(java.util.stream.Collectors.toMap(MemberBalanceDto::userId, m -> m));
    }

    private static void assertSumIsZero(GroupBalanceDto dto) {
        BigDecimal sum = dto.members().stream()
                .map(MemberBalanceDto::netBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
