package com.drivingschool.drivingsession;

import com.drivingschool.candidate.Candidate;
import com.drivingschool.circuit.Circuit;
import com.drivingschool.instructor.Instructor;
import com.drivingschool.vehicle.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "driving_sessions")
public class DrivingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circuit_id")
    private Circuit circuit;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DrivingSessionStatus status = DrivingSessionStatus.PLANNED;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DrivingSession() {
    }

    public DrivingSession(
            Candidate candidate,
            Instructor instructor,
            Vehicle vehicle,
            Circuit circuit,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal price,
            String notes
    ) {
        this.candidate = candidate;
        this.instructor = instructor;
        this.vehicle = vehicle;
        this.circuit = circuit;
        update(sessionDate, startTime, endTime, price, notes);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal price,
            String notes
    ) {
        this.sessionDate = sessionDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.notes = notes;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public void setInstructor(Instructor instructor) {
        this.instructor = instructor;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public void setCircuit(Circuit circuit) {
        this.circuit = circuit;
    }

    public void changeStatus(DrivingSessionStatus status) {
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public Instructor getInstructor() {
        return instructor;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Circuit getCircuit() {
        return circuit;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public DrivingSessionStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
