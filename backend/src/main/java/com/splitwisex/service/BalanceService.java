package com.splitwisex.service;

import com.splitwisex.dto.balance.GroupBalanceDto;
import com.splitwisex.dto.balance.MemberBalanceDto;
import com.splitwisex.dto.balance.SettlementDto;
import com.splitwisex.entity.Expense;
import com.splitwisex.entity.ExpenseParticipant;
import com.splitwisex.entity.GroupMember;
import com.splitwisex.entity.User;
import com.splitwisex.mapper.ExpenseMapper;
import com.splitwisex.repository.ExpenseParticipantRepository;
import com.splitwisex.repository.ExpenseRepository;
import com.splitwisex.repository.GroupMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes each group member's running balance from the group's existing
 * expenses (nothing is persisted — see BalanceController), and hands the
 * result to SettlementCalculator to produce the minimum-transaction
 * settlement plan.
 *
 * <h2>Balance calculation</h2>
 * For every expense in the group:
 * <ol>
 *   <li>The full expense amount is added to the payer's {@code totalPaid}.</li>
 *   <li>The expense is split into equal shares using
 *       {@link ExpenseMapper#calculateEqualShares} — the exact same method
 *       Stage 3 uses to display an individual expense's split, so a
 *       member's contribution to a group balance always matches what they
 *       see on that expense. There is deliberately only one place this
 *       rounding logic lives.</li>
 *   <li>Each participant's share is added to their {@code totalShare}.</li>
 * </ol>
 * Finally, {@code netBalance = totalPaid - totalShare} for each member.
 * Because every rupee paid into an expense is fully accounted for by the
 * shares taken out of it (equal-split shares always sum back to the exact
 * expense amount), the sum of all members' net balances is always exactly
 * zero.
 */
@Service
@Transactional(readOnly = true)
public class BalanceService {

    private final GroupService groupService;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseParticipantRepository participantRepository;
    private final ExpenseMapper expenseMapper;
    private final SettlementCalculator settlementCalculator;

    public BalanceService(
            GroupService groupService,
            GroupMemberRepository groupMemberRepository,
            ExpenseRepository expenseRepository,
            ExpenseParticipantRepository participantRepository,
            ExpenseMapper expenseMapper,
            SettlementCalculator settlementCalculator
    ) {
        this.groupService = groupService;
        this.groupMemberRepository = groupMemberRepository;
        this.expenseRepository = expenseRepository;
        this.participantRepository = participantRepository;
        this.expenseMapper = expenseMapper;
        this.settlementCalculator = settlementCalculator;
    }

    public GroupBalanceDto getGroupBalances(User currentUser, Long groupId) {
        groupService.getGroupOrThrow(groupId);
        groupService.assertMembership(groupId, currentUser.getId());
        return computeBalances(groupId);
    }

    public List<SettlementDto> getGroupSettlements(User currentUser, Long groupId) {
        groupService.getGroupOrThrow(groupId);
        groupService.assertMembership(groupId, currentUser.getId());
        GroupBalanceDto balances = computeBalances(groupId);
        return settlementCalculator.calculate(balances.members());
    }

    private GroupBalanceDto computeBalances(Long groupId) {
        List<GroupMember> members = groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(groupId);

        // One running total per member, in the group's member order.
        // LinkedHashMap keeps that order stable in the response and (via
        // SettlementCalculator's stable sort on ties) in the settlement
        // plan too.
        Map<Long, Ledger> ledgers = new LinkedHashMap<>();
        for (GroupMember member : members) {
            User user = member.getUser();
            ledgers.put(user.getId(), new Ledger(user.getId(), user.getName()));
        }

        List<Expense> expenses = expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        if (!expenses.isEmpty()) {
            Map<Long, List<ExpenseParticipant>> participantsByExpenseId =
                    participantRepository.findAllForGroup(groupId).stream()
                            .collect(Collectors.groupingBy(p -> p.getExpense().getId()));

            for (Expense expense : expenses) {
                Ledger payerLedger = ledgers.get(expense.getPaidBy().getId());
                if (payerLedger != null) {
                    payerLedger.addPaid(expense.getAmount());
                }

                List<ExpenseParticipant> participants =
                        participantsByExpenseId.getOrDefault(expense.getId(), List.of());
                List<BigDecimal> shares = expenseMapper.calculateEqualShares(expense.getAmount(), participants.size());

                for (int i = 0; i < participants.size(); i++) {
                    Ledger participantLedger = ledgers.get(participants.get(i).getUser().getId());
                    if (participantLedger != null) {
                        participantLedger.addShare(shares.get(i));
                    }
                }
            }
        }

        List<MemberBalanceDto> memberBalances = new ArrayList<>(ledgers.size());
        for (Ledger ledger : ledgers.values()) {
            memberBalances.add(ledger.toDto());
        }

        return new GroupBalanceDto(groupId, memberBalances);
    }

    /** Mutable running total for one member while walking a group's expenses. */
    private static final class Ledger {
        private final Long userId;
        private final String name;
        private BigDecimal totalPaid = BigDecimal.ZERO.setScale(2);
        private BigDecimal totalShare = BigDecimal.ZERO.setScale(2);

        private Ledger(Long userId, String name) {
            this.userId = userId;
            this.name = name;
        }

        void addPaid(BigDecimal amount) {
            totalPaid = totalPaid.add(amount);
        }

        void addShare(BigDecimal amount) {
            totalShare = totalShare.add(amount);
        }

        MemberBalanceDto toDto() {
            return new MemberBalanceDto(userId, name, totalPaid, totalShare, totalPaid.subtract(totalShare));
        }
    }
}
