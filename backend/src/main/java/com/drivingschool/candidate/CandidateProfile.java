package com.drivingschool.candidate;

import com.drivingschool.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "candidate_profiles")
public class CandidateProfile {

    @Id
    @Column(name = "user_id")
    private UUID id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 30)
    private String phone;

    protected CandidateProfile() {
    }

    public CandidateProfile(User user, String fullName, String phone) {
        this.user = user;
        this.fullName = fullName;
        this.phone = phone;
    }

    public User getUser() {
        return user;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public CandidateStatus getStatus() {
        return user.isEnabled() ? CandidateStatus.ACTIVE : CandidateStatus.INACTIVE;
    }

    public void update(String fullName, String phone, CandidateStatus status) {
        this.fullName = fullName;
        this.phone = phone;
        user.setEnabled(status == CandidateStatus.ACTIVE);
    }
}
