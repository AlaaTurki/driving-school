package com.drivingschool.drivingsession;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DrivingSessionRepository extends JpaRepository<DrivingSession, UUID>,
        JpaSpecificationExecutor<DrivingSession> {

    @EntityGraph(attributePaths = {"candidate", "instructor", "vehicle", "circuit"})
    @Override
    Page<DrivingSession> findAll(Specification<DrivingSession> specification, Pageable pageable);

    default Page<DrivingSession> search(
            LocalDate dateFrom,
            LocalDate dateTo,
            UUID candidateId,
            UUID instructorId,
            DrivingSessionStatus status,
            UUID circuitId,
            UUID instructorUserId,
            UUID candidateUserId,
            Pageable pageable
    ) {
        return findAll(DrivingSessionSpecifications.search(
                dateFrom,
                dateTo,
                candidateId,
                instructorId,
                status,
                circuitId,
                instructorUserId,
                candidateUserId
        ), pageable);
    }

    @EntityGraph(attributePaths = {"candidate", "instructor", "vehicle", "circuit"})
    Optional<DrivingSession> findById(UUID id);

    @EntityGraph(attributePaths = {"candidate", "instructor", "vehicle", "circuit"})
    Optional<DrivingSession> findByIdAndInstructor_User_Id(UUID id, UUID userId);

    boolean existsByInstructor_Id(UUID instructorId);

    boolean existsByCandidate_Id(UUID candidateId);

    boolean existsByCandidate_IdAndInstructor_User_Id(UUID candidateId, UUID instructorUserId);

    boolean existsByVehicle_Id(UUID vehicleId);

    boolean existsByCircuit_Id(UUID circuitId);

    long countBySessionDate(LocalDate sessionDate);

    long countBySessionDateBetween(LocalDate from, LocalDate to);

    @Query("""
            select session.sessionDate as sessionDate, count(session.id) as sessionCount
            from DrivingSession session
            where session.sessionDate between :from and :to
            group by session.sessionDate
            """)
    List<SessionCountByDay> countSessionsByDay(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select session.status as status, count(session.id) as sessionCount
            from DrivingSession session
            group by session.status
            """)
    List<SessionCountByStatus> countSessionsByStatus();

    @Query("""
            select count(distinct session.candidate.id)
            from DrivingSession session
            where session.instructor.user.id = :userId
            """)
    long countDistinctCandidatesForInstructor(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"candidate", "instructor", "circuit"})
    List<DrivingSession> findAllByInstructor_User_IdAndSessionDateOrderByStartTimeAsc(
            UUID userId,
            LocalDate sessionDate
    );

    @EntityGraph(attributePaths = {"candidate", "instructor", "circuit"})
    @Query("""
            select session from DrivingSession session
            where session.instructor.user.id = :userId
              and session.status = :status
              and (session.sessionDate > :today
                   or (session.sessionDate = :today and session.startTime >= :currentTime))
            order by session.sessionDate, session.startTime, session.id
            """)
    List<DrivingSession> findUpcomingForInstructor(
            @Param("userId") UUID userId,
            @Param("status") DrivingSessionStatus status,
            @Param("today") LocalDate today,
            @Param("currentTime") LocalTime currentTime,
            org.springframework.data.domain.Pageable pageable
    );

    @Query(value = """
            with billable_totals as (
                select candidate_id, sum(price) as total
                from driving_sessions
                where status in (:statuses)
                group by candidate_id
            ), payment_totals as (
                select candidate_id, sum(amount) as total
                from payments
                group by candidate_id
            )
            select coalesce(sum(greatest(
                coalesce(billable_totals.total, 0) - coalesce(payment_totals.total, 0), 0
            )), 0)
            from candidates
            left join billable_totals on billable_totals.candidate_id = candidates.id
            left join payment_totals on payment_totals.candidate_id = candidates.id
            """, nativeQuery = true)
    BigDecimal sumPositiveRemainingAcrossCandidates(@Param("statuses") List<String> statuses);

    @Query(value = """
            select coalesce(sum(extract(epoch from (session.end_time - session.start_time))::numeric / 3600), 0)
                       as "totalHours",
                   coalesce(sum(case
                       when session.session_date >= :monthStart and session.session_date < :nextMonthStart
                       then extract(epoch from (session.end_time - session.start_time))::numeric / 3600
                       else 0
                   end), 0) as "monthHours"
            from driving_sessions session
            join instructors instructor on instructor.id = session.instructor_id
            where instructor.user_id = :userId and session.status = 'DONE'
            """, nativeQuery = true)
    InstructorHours findInstructorHours(
            @Param("userId") UUID userId,
            @Param("monthStart") LocalDate monthStart,
            @Param("nextMonthStart") LocalDate nextMonthStart
    );

    @Query("""
            select coalesce(sum(session.price), 0)
            from DrivingSession session
            where session.candidate.id = :candidateId and session.status in :statuses
            """)
    java.math.BigDecimal sumBillablePricesByCandidateId(
            @Param("candidateId") UUID candidateId,
            @Param("statuses") Set<DrivingSessionStatus> statuses
    );

    @Query("""
            select session.id as sessionId,
                   session.sessionDate as sessionDate,
                   session.startTime as startTime,
                   session.endTime as endTime,
                   session.price as price,
                   session.status as status
            from DrivingSession session
            where session.candidate.id = :candidateId and session.status in :statuses
            order by session.sessionDate, session.startTime, session.id
            """)
    List<BillableSessionProjection> findBillableSessionProjections(
            @Param("candidateId") UUID candidateId,
            @Param("statuses") Set<DrivingSessionStatus> statuses
    );

    @EntityGraph(attributePaths = {"candidate", "instructor", "vehicle", "circuit"})
    List<DrivingSession> findAllBySessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            LocalDate from,
            LocalDate to
    );

    interface SessionCountByDay {
        LocalDate getSessionDate();

        long getSessionCount();
    }

    interface SessionCountByStatus {
        DrivingSessionStatus getStatus();

        long getSessionCount();
    }

    interface InstructorHours {
        BigDecimal getTotalHours();

        BigDecimal getMonthHours();
    }

}
