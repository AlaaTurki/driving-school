package com.drivingschool.candidate;

import java.util.UUID;

public class CandidateUserNotFoundException extends RuntimeException {

    public CandidateUserNotFoundException(UUID userId) {
        super("User not found: " + userId);
    }
}
