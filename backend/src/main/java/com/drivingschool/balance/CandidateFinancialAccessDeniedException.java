package com.drivingschool.balance;

public class CandidateFinancialAccessDeniedException extends RuntimeException {

    public CandidateFinancialAccessDeniedException() {
        super("You are not allowed to access this candidate's financial information");
    }
}
