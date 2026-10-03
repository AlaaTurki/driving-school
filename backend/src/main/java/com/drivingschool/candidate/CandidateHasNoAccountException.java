package com.drivingschool.candidate;

import java.util.UUID;

public class CandidateHasNoAccountException extends RuntimeException {

    public CandidateHasNoAccountException(UUID candidateId) {
        super("Candidate has no linked user account: " + candidateId);
    }
}
