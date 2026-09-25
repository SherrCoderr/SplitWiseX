package com.splitwisex.controller;

import com.splitwisex.dto.expense.ExpenseDto;
import com.splitwisex.entity.User;
import com.splitwisex.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single-expense operations addressed by the expense's own id. Both
 * endpoints verify the authenticated user belongs to the expense's group
 * before returning/deleting anything (see ExpenseService) — an id alone in
 * the URL is never enough.
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ExpenseDto> getExpense(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long expenseId
    ) {
        return ResponseEntity.ok(expenseService.getExpense(currentUser, expenseId));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long expenseId
    ) {
        expenseService.deleteExpense(currentUser, expenseId);
        return ResponseEntity.noContent().build();
    }
}
