package com.splitwisex.controller;

import com.splitwisex.dto.balance.GroupBalanceDto;
import com.splitwisex.dto.balance.SettlementDto;
import com.splitwisex.entity.User;
import com.splitwisex.service.BalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Stage 4: dynamically calculated balances and settlement plan for a group.
 * Nothing here is persisted — every call re-derives the result from the
 * group's current expenses (see BalanceService), and every call verifies
 * the authenticated user actually belongs to the group before returning
 * anything.
 */
@RestController
@RequestMapping("/api/groups/{groupId}")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/balances")
    public ResponseEntity<GroupBalanceDto> getBalances(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(balanceService.getGroupBalances(currentUser, groupId));
    }

    @GetMapping("/settlements")
    public ResponseEntity<List<SettlementDto>> getSettlements(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(balanceService.getGroupSettlements(currentUser, groupId));
    }
}
