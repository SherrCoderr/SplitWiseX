package com.splitwisex.repository;

import com.splitwisex.entity.ExpenseParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseParticipantRepository extends JpaRepository<ExpenseParticipant, Long> {

    /**
     * Ordered by id (i.e. insertion order) so the equal-split remainder
     * distribution in ExpenseMapper is deterministic.
     */
    List<ExpenseParticipant> findByExpenseIdOrderByIdAsc(Long expenseId);

    /**
     * Every participant row across every expense in a group, in one query
     * — used by BalanceService to compute totals without an N+1 lookup
     * per expense. Ordered the same way findByExpenseIdOrderByIdAsc is
     * (by expense, then by participant insertion order) so grouping this
     * back up per-expense reproduces the exact same equal-split remainder
     * assignment as the single-expense view.
     */
    @Query("SELECT ep FROM ExpenseParticipant ep " +
            "WHERE ep.expense.group.id = :groupId " +
            "ORDER BY ep.expense.id ASC, ep.id ASC")
    List<ExpenseParticipant> findAllForGroup(@Param("groupId") Long groupId);
}
