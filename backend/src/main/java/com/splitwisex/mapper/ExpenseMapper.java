package com.splitwisex.mapper;

import com.splitwisex.dto.expense.ExpenseDto;
import com.splitwisex.dto.expense.ExpenseParticipantDto;
import com.splitwisex.entity.Expense;
import com.splitwisex.entity.ExpenseParticipant;
import com.splitwisex.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExpenseMapper {

    private final UserMapper userMapper;

    public ExpenseMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public ExpenseDto toDto(Expense expense, List<ExpenseParticipant> participants) {
        List<BigDecimal> shares = calculateEqualShares(expense.getAmount(), participants.size());

        List<ExpenseParticipantDto> participantDtos = new ArrayList<>(participants.size());
        for (int i = 0; i < participants.size(); i++) {
            User user = participants.get(i).getUser();
            participantDtos.add(new ExpenseParticipantDto(user.getId(), user.getName(), user.getEmail(), shares.get(i)));
        }

        return new ExpenseDto(
                expense.getId(),
                expense.getGroup().getId(),
                expense.getDescription(),
                expense.getAmount(),
                userMapper.toSummary(expense.getPaidBy()),
                participantDtos,
                expense.getCreatedAt()
        );
    }

    /**
     * Splits {@code totalAmount} equally across {@code count} participants
     * using integer-cent arithmetic (BigDecimal, scale 2) rather than plain
     * division, so the returned shares always sum back exactly to the
     * total — dividing ₹900 by ... say 7 people as plain decimal division
     * would either lose or gain a cent to rounding. Any leftover cent(s)
     * from the division go to the first participants, in the order they
     * were passed to {@link com.splitwisex.service.ExpenseService}, so the
     * split is deterministic for a given expense.
     */
    public List<BigDecimal> calculateEqualShares(BigDecimal totalAmount, int count) {
        if (count <= 0) {
            return List.of();
        }

        long totalCents = totalAmount
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();

        long baseShareCents = totalCents / count;
        long remainderCents = totalCents % count;

        List<BigDecimal> shares = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long shareCents = baseShareCents + (i < remainderCents ? 1 : 0);
            shares.add(BigDecimal.valueOf(shareCents, 2));
        }
        return shares;
    }
}
