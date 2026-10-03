package com.drivingschool.candidate;

import java.util.UUID;

public class CandidateNotFoundException extends RuntimeException {

    public CandidateNotFoundException(UUID id) {
        super("Candidate not found: " + id);
    }
}
