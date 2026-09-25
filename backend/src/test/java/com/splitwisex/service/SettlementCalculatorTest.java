package com.splitwisex.service;

import com.splitwisex.dto.balance.MemberBalanceDto;
import com.splitwisex.dto.balance.SettlementDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementCalculatorTest {

    private final SettlementCalculator calculator = new SettlementCalculator();

    private static MemberBalanceDto member(long id, String name, String net) {
        // totalPaid/totalShare aren't used by the calculator, only
        // netBalance — set them to something arbitrary but consistent.
        BigDecimal netBalance = new BigDecimal(net);
        return new MemberBalanceDto(id, name, netBalance, BigDecimal.ZERO, netBalance);
    }

    @Test
    void onePayerTwoParticipants() {
        // Sameer +600, Rahul -300, Arjun -300
        List<MemberBalanceDto> balances = List.of(
                member(1, "Sameer", "600.00"),
                member(2, "Rahul", "-300.00"),
                member(3, "Arjun", "-300.00")
        );

        List<SettlementDto> settlements = calculator.calculate(balances);

        assertThat(settlements).hasSize(2);
        assertThat(settlements.get(0).from().name()).isEqualTo("Rahul");
        assertThat(settlements.get(0).to().name()).isEqualTo("Sameer");
        assertThat(settlements.get(0).amount()).isEqualByComparingTo("300.00");
        assertThat(settlements.get(1).from().name()).isEqualTo("Arjun");
        assertThat(settlements.get(1).to().name()).isEqualTo("Sameer");
        assertThat(settlements.get(1).amount()).isEqualByComparingTo("300.00");
    }

    @Test
    void multipleExpensesScenario() {
        // Sameer +400, Rahul +100, Arjun -500
        List<MemberBalanceDto> balances = List.of(
                member(1, "Sameer", "400.00"),
                member(2, "Rahul", "100.00"),
                member(3, "Arjun", "-500.00")
        );

        List<SettlementDto> settlements = calculator.calculate(balances);

        assertThat(settlements).hasSize(2);
        assertThat(settlements.get(0).from().name()).isEqualTo("Arjun");
        assertThat(settlements.get(0).to().name()).isEqualTo("Sameer");
        assertThat(settlements.get(0).amount()).isEqualByComparingTo("400.00");
        assertThat(settlements.get(1).from().name()).isEqualTo("Arjun");
        assertThat(settlements.get(1).to().name()).isEqualTo("Rahul");
        assertThat(settlements.get(1).amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void alreadySettledGroupProducesNoTransactions() {
        List<MemberBalanceDto> balances = List.of(
                member(1, "Sameer", "0.00"),
                member(2, "Rahul", "0.00"),
                member(3, "Arjun", "0.00")
        );

        assertThat(calculator.calculate(balances)).isEmpty();
    }

    @Test
    void oneDebtorMultipleCreditors() {
        // A +600, B +200, C -800
        List<MemberBalanceDto> balances = List.of(
                member(1, "A", "600.00"),
                member(2, "B", "200.00"),
                member(3, "C", "-800.00")
        );

        List<SettlementDto> settlements = calculator.calculate(balances);

        assertThat(settlements).hasSize(2);
        assertThat(settlements.get(0).from().name()).isEqualTo("C");
        assertThat(settlements.get(0).to().name()).isEqualTo("A");
        assertThat(settlements.get(0).amount()).isEqualByComparingTo("600.00");
        assertThat(settlements.get(1).from().name()).isEqualTo("C");
        assertThat(settlements.get(1).to().name()).isEqualTo("B");
        assertThat(settlements.get(1).amount()).isEqualByComparingTo("200.00");
    }

    @Test
    void multipleDebtorsOneCreditor() {
        // A +800, B -500, C -300
        List<MemberBalanceDto> balances = List.of(
                member(1, "A", "800.00"),
                member(2, "B", "-500.00"),
                member(3, "C", "-300.00")
        );

        List<SettlementDto> settlements = calculator.calculate(balances);

        assertThat(settlements).hasSize(2);
        assertThat(settlements.get(0).from().name()).isEqualTo("B");
        assertThat(settlements.get(0).to().name()).isEqualTo("A");
        assertThat(settlements.get(0).amount()).isEqualByComparingTo("500.00");
        assertThat(settlements.get(1).from().name()).isEqualTo("C");
        assertThat(settlements.get(1).to().name()).isEqualTo("A");
        assertThat(settlements.get(1).amount()).isEqualByComparingTo("300.00");
    }

    @Test
    void roundingFromAnUnevenThreeWaySplitStillSettlesExactlyToZero() {
        // ₹100 split 3 ways is 33.34 / 33.33 / 33.33 (see ExpenseMapperTest).
        // If the payer keeps their own share, their net balance is
        // +66.66/+66.67 and the other two are negative by their share.
        List<MemberBalanceDto> balances = List.of(
                member(1, "Sameer", "66.66"),
                member(2, "Rahul", "-33.33"),
                member(3, "Arjun", "-33.33")
        );

        List<SettlementDto> settlements = calculator.calculate(balances);

        BigDecimal totalSettled = settlements.stream()
                .map(SettlementDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalSettled).isEqualByComparingTo("66.66");
        assertInvariants(balances, settlements);
    }

    @Test
    void emptyGroupProducesNoSettlements() {
        assertThat(calculator.calculate(List.of())).isEmpty();
    }

    // Group-membership authorization (Stage 4's "unauthorized access" test
    // case) is enforced in BalanceService before this class is ever
    // invoked — see BalanceServiceTest#nonMemberCannotViewBalances and
    // #nonMemberCannotViewSettlements. This class only ever sees balances
    // for a group the caller is already allowed to see.

    // ---- Property/invariant tests, run across every scenario above ----

    private static Stream<List<MemberBalanceDto>> allScenarios() {
        return Stream.of(
                List.of(member(1, "Sameer", "600.00"), member(2, "Rahul", "-300.00"), member(3, "Arjun", "-300.00")),
                List.of(member(1, "Sameer", "400.00"), member(2, "Rahul", "100.00"), member(3, "Arjun", "-500.00")),
                List.of(member(1, "A", "600.00"), member(2, "B", "200.00"), member(3, "C", "-800.00")),
                List.of(member(1, "A", "800.00"), member(2, "B", "-500.00"), member(3, "C", "-300.00")),
                List.of(member(1, "A", "150.75"), member(2, "B", "-50.25"), member(3, "C", "-100.50")),
                List.of(member(1, "A", "0.00"), member(2, "B", "0.00"))
        );
    }

    @Test
    void invariantsHoldAcrossAllScenarios() {
        allScenarios().forEach(balances -> {
            List<SettlementDto> settlements = calculator.calculate(balances);
            assertInvariants(balances, settlements);
        });
    }

    /**
     * Verifies, for a given input and the calculator's output:
     *  1. every settlement amount is strictly positive,
     *  2. every settlement's "from" is an actual debtor and "to" an actual
     *     creditor from the input,
     *  3. no settlement has the same user as sender and receiver,
     *  4. applying every settlement drives every member's balance to
     *     exactly zero.
     */
    private void assertInvariants(List<MemberBalanceDto> balances, List<SettlementDto> settlements) {
        // Invariant: sum of all net balances is zero to begin with (this is
        // guaranteed by BalanceService's construction, asserted here as a
        // sanity check on the fixtures themselves).
        BigDecimal sumOfBalances = balances.stream()
                .map(MemberBalanceDto::netBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumOfBalances).isEqualByComparingTo(BigDecimal.ZERO);

        java.util.Map<Long, BigDecimal> running = new java.util.HashMap<>();
        for (MemberBalanceDto balance : balances) {
            running.put(balance.userId(), balance.netBalance());
        }

        for (SettlementDto settlement : settlements) {
            assertThat(settlement.amount()).isGreaterThan(BigDecimal.ZERO);
            assertThat(settlement.from().id()).isNotEqualTo(settlement.to().id());

            BigDecimal fromBalance = running.get(settlement.from().id());
            BigDecimal toBalance = running.get(settlement.to().id());
            assertThat(fromBalance).as("settlement 'from' must be a debtor").isLessThan(BigDecimal.ZERO);
            assertThat(toBalance).as("settlement 'to' must be a creditor").isGreaterThan(BigDecimal.ZERO);

            running.put(settlement.from().id(), fromBalance.add(settlement.amount()));
            running.put(settlement.to().id(), toBalance.subtract(settlement.amount()));
        }

        running.values().forEach(finalBalance ->
                assertThat(finalBalance).as("every balance must reach zero after settlement").isEqualByComparingTo(BigDecimal.ZERO)
        );
    }
}
