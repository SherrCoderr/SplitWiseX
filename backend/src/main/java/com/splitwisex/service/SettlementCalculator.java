package com.splitwisex.service;

import com.splitwisex.dto.balance.MemberBalanceDto;
import com.splitwisex.dto.balance.SettlementDto;
import com.splitwisex.dto.balance.SettlementPartyDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns a set of net balances into the smallest practical set of
 * "who pays whom" transactions that settles everyone to zero.
 *
 * <h2>Algorithm</h2>
 * This is the classic greedy "largest debtor vs. largest creditor" cash-flow
 * minimization heuristic:
 * <ol>
 *   <li>Split members into creditors (net balance &gt; 0, i.e. they should
 *       receive money) and debtors (net balance &lt; 0, i.e. they owe
 *       money). Members already at zero need no transaction.</li>
 *   <li>Sort each group largest-outstanding-amount first.</li>
 *   <li>Repeatedly take the biggest remaining creditor and the biggest
 *       remaining debtor, and settle {@code min(creditor, debtor)} between
 *       them as one transaction.</li>
 *   <li>Whichever side hits zero first drops out of consideration; the
 *       other side carries its remainder into the next round.</li>
 *   <li>Repeat until both lists are exhausted.</li>
 * </ol>
 *
 * <h2>Why "greedy" and not "optimal"</h2>
 * Finding the mathematically <em>smallest possible</em> number of
 * transactions for an arbitrary set of balances is a harder combinatorial
 * problem (related to minimum-cost flow / subset-sum partitioning) and is
 * NP-hard in the general case. This greedy approach doesn't guarantee the
 * absolute minimum in every conceivable case, but it always produces at
 * most n - 1 transactions for n members (provably — each transaction fully
 * settles at least one person), never generates the naive "everyone pays
 * everyone" O(n^2) transaction set, and is simple enough to explain and
 * verify by hand. This is the standard approach used by real expense-
 * splitting apps and in technical interviews for this exact problem.
 *
 * <h2>Complexity</h2>
 * Sorting both lists is O(n log n). The matching loop then walks two
 * pointers forward and advances at least one of them every iteration, so
 * it runs at most n times — O(n). Overall: <b>O(n log n)</b> time,
 * O(n) extra space, where n is the number of group members.
 *
 * <h2>Precision</h2>
 * All arithmetic is done in integer cents (via BigDecimal, scale 2) rather
 * than direct BigDecimal subtraction/comparison in rupees, for the same
 * reason ExpenseMapper splits an expense in cents: it removes any
 * possibility of floating-point-style drift across many small
 * transactions, and keeps every intermediate and final amount exactly
 * reproducible.
 */
@Component
public class SettlementCalculator {

    public List<SettlementDto> calculate(List<MemberBalanceDto> balances) {
        List<Party> creditors = new ArrayList<>();
        List<Party> debtors = new ArrayList<>();

        for (MemberBalanceDto balance : balances) {
            long cents = toCents(balance.netBalance());
            if (cents > 0) {
                creditors.add(new Party(balance.userId(), balance.name(), cents));
            } else if (cents < 0) {
                debtors.add(new Party(balance.userId(), balance.name(), -cents));
            }
            // cents == 0: already settled, contributes no transaction.
        }

        // Largest-outstanding-amount-first is what lets the greedy matching
        // below settle each person in as few transactions as possible.
        creditors.sort(Comparator.comparingLong((Party p) -> p.remainingCents).reversed());
        debtors.sort(Comparator.comparingLong((Party p) -> p.remainingCents).reversed());

        List<SettlementDto> settlements = new ArrayList<>();
        int creditorIndex = 0;
        int debtorIndex = 0;

        while (creditorIndex < creditors.size() && debtorIndex < debtors.size()) {
            Party creditor = creditors.get(creditorIndex);
            Party debtor = debtors.get(debtorIndex);

            long settledCents = Math.min(creditor.remainingCents, debtor.remainingCents);

            settlements.add(new SettlementDto(
                    new SettlementPartyDto(debtor.userId, debtor.name),
                    new SettlementPartyDto(creditor.userId, creditor.name),
                    fromCents(settledCents)
            ));

            creditor.remainingCents -= settledCents;
            debtor.remainingCents -= settledCents;

            if (creditor.remainingCents == 0) {
                creditorIndex++;
            }
            if (debtor.remainingCents == 0) {
                debtorIndex++;
            }
        }

        return settlements;
    }

    private static long toCents(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    private static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }

    /** Mutable working copy of one member's outstanding amount, in cents. */
    private static final class Party {
        private final Long userId;
        private final String name;
        private long remainingCents;

        private Party(Long userId, String name, long remainingCents) {
            this.userId = userId;
            this.name = name;
            this.remainingCents = remainingCents;
        }
    }
}
