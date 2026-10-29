package com.drivingschool.payment;

import com.drivingschool.candidate.Candidate;
import com.drivingschool.candidate.CandidateNotFoundException;
import com.drivingschool.candidate.CandidateRepository;
import com.drivingschool.payment.dto.PaymentResponse;
import com.drivingschool.payment.dto.SavePaymentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CandidateRepository candidateRepository;

    public PaymentService(PaymentRepository paymentRepository, CandidateRepository candidateRepository) {
        this.paymentRepository = paymentRepository;
        this.candidateRepository = candidateRepository;
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findAll(
            UUID candidateId,
            LocalDate dateFrom,
            LocalDate dateTo,
            PaymentMethod paymentMethod,
            Pageable pageable
    ) {
        validateDateRange(dateFrom, dateTo);
        return paymentRepository.findAll(
                        PaymentSpecifications.search(candidateId, dateFrom, dateTo, paymentMethod),
                        pageable
                )
                .map(PaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(UUID id) {
        return paymentRepository.findById(id)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    @Transactional
    public PaymentResponse create(SavePaymentRequest request) {
        Candidate candidate = resolveCandidate(request.candidateId());
        Payment payment = new Payment(
                candidate,
                request.amount(),
                request.paymentDate(),
                request.paymentMethod(),
                normalizeNotes(request.notes())
        );
        return PaymentResponse.from(paymentRepository.saveAndFlush(payment));
    }

    @Transactional
    public PaymentResponse update(UUID id, SavePaymentRequest request) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        payment.setCandidate(resolveCandidate(request.candidateId()));
        payment.update(
                request.amount(),
                request.paymentDate(),
                request.paymentMethod(),
                normalizeNotes(request.notes())
        );
        return PaymentResponse.from(paymentRepository.saveAndFlush(payment));
    }

    @Transactional
    public void delete(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        paymentRepository.delete(payment);
    }

    private Candidate resolveCandidate(UUID candidateId) {
        return candidateRepository.findById(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException(candidateId));
    }

    private static void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateTo.isBefore(dateFrom)) {
            throw new InvalidPaymentException("dateTo must not be before dateFrom");
        }
    }

    private static String normalizeNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        return notes.trim();
    }
}
