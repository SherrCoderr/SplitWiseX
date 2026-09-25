package com.splitwisex.controller;

import com.splitwisex.dto.expense.CreateExpenseRequest;
import com.splitwisex.dto.expense.ExpenseDto;
import com.splitwisex.entity.User;
import com.splitwisex.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expense creation/listing, scoped under the owning group's id
 * (/api/groups/{groupId}/expenses). Lookup and deletion of a single
 * expense by its own id live in ExpenseController instead, since those
 * don't need the group id in the path.
 */
@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class GroupExpenseController {

    private final ExpenseService expenseService;

    public GroupExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseDto> createExpense(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long groupId,
            @Valid @RequestBody CreateExpenseRequest request
    ) {
        ExpenseDto expense = expenseService.createExpense(currentUser, groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(expense);
    }

    @GetMapping
    public ResponseEntity<List<ExpenseDto>> getGroupExpenses(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(expenseService.getGroupExpenses(currentUser, groupId));
    }
}
