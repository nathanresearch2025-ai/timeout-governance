package com.enterprise.timeout.repository;

import com.enterprise.timeout.model.TimeoutEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TimeoutEventRepository extends JpaRepository<TimeoutEvent, Long> {

    List<TimeoutEvent> findByResolvedAtIsNullAndEscalatedFalse();

    List<TimeoutEvent> findByWorkflowIdAndDetectedAtAfter(String workflowId, LocalDateTime after);

    List<TimeoutEvent> findByTeamIdAndDetectedAtBetween(String teamId, LocalDateTime start, LocalDateTime end);
}
