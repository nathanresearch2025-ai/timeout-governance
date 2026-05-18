package com.enterprise.timeout.repository;

import com.enterprise.timeout.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTargetTypeAndTargetId(String targetType, String targetId);

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
