package com.splitwisex.mapper;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseMapperTest {

    private final ExpenseMapper expenseMapper = new ExpenseMapper(new UserMapper());

    @Test
    void splitsEvenlyWhenTheAmountDividesCleanly() {
        List<BigDecimal> shares = expenseMapper.calculateEqualShares(new BigDecimal("900.00"), 3);

        assertThat(shares).hasSize(3);
        shares.forEach(share -> assertThat(share).isEqualByComparingTo("300.00"));
    }

    @Test
    void distributesLeftoverCentsToTheFirstParticipantsWhenItDoesNotDivideEvenly() {
        // ₹100 / 3 = ₹33.33 each with ₹0.01 left over. That leftover cent
        // must go to exactly one participant so the shares sum back to ₹100.
        List<BigDecimal> shares = expenseMapper.calculateEqualShares(new BigDecimal("100.00"), 3);

        assertThat(shares).containsExactly(
                new BigDecimal("33.34"),
                new BigDecimal("33.33"),
                new BigDecimal("33.33")
        );

        BigDecimal sum = shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("100.00");
    }

    @Test
    void neverUsesFloatingPointRoundingForMoney() {
        // ₹10 / 7 would repeat indefinitely as a double; cent-based
        // arithmetic must still land on an exact two-decimal total.
        List<BigDecimal> shares = expenseMapper.calculateEqualShares(new BigDecimal("10.00"), 7);

        BigDecimal sum = shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("10.00");
        assertThat(shares).allSatisfy(share -> assertThat(share.scale()).isEqualTo(2));
    }

    @Test
    void singleParticipantGetsTheFullAmount() {
        List<BigDecimal> shares = expenseMapper.calculateEqualShares(new BigDecimal("450.50"), 1);

        assertThat(shares).containsExactly(new BigDecimal("450.50"));
    }
}
