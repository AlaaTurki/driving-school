DO $$
DECLARE
    conflict RECORD;
BEGIN
    SELECT 'instructor' AS resource, first_session.id AS first_id, second_session.id AS second_id
    INTO conflict
    FROM driving_sessions first_session
    JOIN driving_sessions second_session
      ON first_session.id < second_session.id
     AND first_session.instructor_id = second_session.instructor_id
     AND first_session.session_date = second_session.session_date
    WHERE first_session.status <> 'CANCELLED'
      AND second_session.status <> 'CANCELLED'
      AND tsrange(
              first_session.session_date + first_session.start_time,
              first_session.session_date + first_session.end_time,
              '[)'
          ) && tsrange(
              second_session.session_date + second_session.start_time,
              second_session.session_date + second_session.end_time,
              '[)'
          )
    LIMIT 1;
    IF FOUND THEN
        RAISE EXCEPTION 'Cannot install schedule constraints: overlapping instructor sessions % and %',
            conflict.first_id, conflict.second_id;
    END IF;

    SELECT 'candidate' AS resource, first_session.id AS first_id, second_session.id AS second_id
    INTO conflict
    FROM driving_sessions first_session
    JOIN driving_sessions second_session
      ON first_session.id < second_session.id
     AND first_session.candidate_id = second_session.candidate_id
     AND first_session.session_date = second_session.session_date
    WHERE first_session.status <> 'CANCELLED'
      AND second_session.status <> 'CANCELLED'
      AND tsrange(
              first_session.session_date + first_session.start_time,
              first_session.session_date + first_session.end_time,
              '[)'
          ) && tsrange(
              second_session.session_date + second_session.start_time,
              second_session.session_date + second_session.end_time,
              '[)'
          )
    LIMIT 1;
    IF FOUND THEN
        RAISE EXCEPTION 'Cannot install schedule constraints: overlapping candidate sessions % and %',
            conflict.first_id, conflict.second_id;
    END IF;

    SELECT 'vehicle' AS resource, first_session.id AS first_id, second_session.id AS second_id
    INTO conflict
    FROM driving_sessions first_session
    JOIN driving_sessions second_session
      ON first_session.id < second_session.id
     AND first_session.vehicle_id = second_session.vehicle_id
     AND first_session.session_date = second_session.session_date
    WHERE first_session.vehicle_id IS NOT NULL
      AND first_session.status <> 'CANCELLED'
      AND second_session.status <> 'CANCELLED'
      AND tsrange(
              first_session.session_date + first_session.start_time,
              first_session.session_date + first_session.end_time,
              '[)'
          ) && tsrange(
              second_session.session_date + second_session.start_time,
              second_session.session_date + second_session.end_time,
              '[)'
          )
    LIMIT 1;
    IF FOUND THEN
        RAISE EXCEPTION 'Cannot install schedule constraints: overlapping vehicle sessions % and %',
            conflict.first_id, conflict.second_id;
    END IF;
END
$$;

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE driving_sessions
    ADD CONSTRAINT excl_driving_sessions_instructor_time
        EXCLUDE USING gist (
            instructor_id WITH =,
            tsrange(session_date + start_time, session_date + end_time, '[)') WITH &&
        )
        WHERE (status <> 'CANCELLED');

ALTER TABLE driving_sessions
    ADD CONSTRAINT excl_driving_sessions_vehicle_time
        EXCLUDE USING gist (
            vehicle_id WITH =,
            tsrange(session_date + start_time, session_date + end_time, '[)') WITH &&
        )
        WHERE (status <> 'CANCELLED' AND vehicle_id IS NOT NULL);

ALTER TABLE driving_sessions
    ADD CONSTRAINT excl_driving_sessions_candidate_time
        EXCLUDE USING gist (
            candidate_id WITH =,
            tsrange(session_date + start_time, session_date + end_time, '[)') WITH &&
        )
        WHERE (status <> 'CANCELLED');
