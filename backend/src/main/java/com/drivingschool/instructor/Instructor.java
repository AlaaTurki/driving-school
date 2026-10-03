package com.drivingschool.instructor;

import com.drivingschool.user.User;
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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "instructors", uniqueConstraints = {
        @UniqueConstraint(name = "uk_instructors_user_id", columnNames = "user_id"),
        @UniqueConstraint(name = "uk_instructors_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_instructors_license_number", columnNames = "license_number")
})
public class Instructor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "first_name", nullable = false, length = 120)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 120)
    private String lastName;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "license_number", nullable = false, length = 100)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstructorStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Instructor() {
    }

    public Instructor(
            User user,
            String firstName,
            String lastName,
            String phone,
            String email,
            String licenseNumber,
            InstructorStatus status
    ) {
        this.user = user;
        update(firstName, lastName, phone, email, licenseNumber, status);
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
            String firstName,
            String lastName,
            String phone,
            String email,
            String licenseNumber,
            InstructorStatus status
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.licenseNumber = licenseNumber;
        this.status = status;
        user.setEnabled(status == InstructorStatus.ACTIVE);
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public InstructorStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
