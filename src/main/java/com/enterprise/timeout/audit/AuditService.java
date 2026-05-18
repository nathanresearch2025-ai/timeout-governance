package com.enterprise.timeout.audit;

import com.enterprise.timeout.model.AuditAction;
import com.enterprise.timeout.model.AuditLog;
import com.enterprise.timeout.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(AuditAction action, String actor, String targetType, String targetId, String detail) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .actor(actor)
                .targetType(targetType)
                .targetId(targetId)
                .detail(detail)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit: [{}] {} -> {}:{} - {}", action, actor, targetType, targetId, detail);
    }

    public List<AuditLog> getByTarget(String targetType, String targetId) {
        return auditLogRepository.findByTargetTypeAndTargetId(targetType, targetId);
    }

    public List<AuditLog> getByTimeRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetween(start, end);
    }
}
