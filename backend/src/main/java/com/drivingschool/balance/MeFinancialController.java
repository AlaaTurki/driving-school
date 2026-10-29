package com.drivingschool.balance;

import com.drivingschool.balance.dto.BalanceResponse;
import com.drivingschool.balance.dto.StatementResponse;
import com.drivingschool.drivingsession.DrivingSessionStatus;
import com.drivingschool.payment.dto.PaymentPageResponse;
import com.drivingschool.security.AppUserDetails;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/me")
@PreAuthorize("hasRole('CANDIDATE')")
public class MeFinancialController {

    private final BalanceService balanceService;

    public MeFinancialController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/balance")
    public BalanceResponse getMyBalance(@AuthenticationPrincipal AppUserDetails actor) {
        return balanceService.getMyBalance(actor);
    }

    @GetMapping("/payments")
    public PaymentPageResponse getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserDetails actor
    ) {
        validatePage(page, size);
        return PaymentPageResponse.from(balanceService.getMyPayments(
                PageRequest.of(page, size, Sort.by("paymentDate").descending().and(Sort.by("createdAt").descending())),
                actor
        ));
    }

    @GetMapping("/statement")
    public StatementResponse getMyStatement(
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) DrivingSessionStatus status,
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) UUID circuitId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserDetails actor
    ) {
        validatePage(page, size);
        return balanceService.getMyStatement(
                dateFrom,
                dateTo,
                status,
                instructorId,
                circuitId,
                PageRequest.of(page, size, Sort.by("sessionDate").descending().and(Sort.by("startTime").descending())),
                actor
        );
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }
    }
}
