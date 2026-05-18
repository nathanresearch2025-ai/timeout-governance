package com.enterprise.timeout.controller;

import com.enterprise.timeout.audit.AuditService;
import com.enterprise.timeout.model.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/target/{targetType}/{targetId}")
    public List<AuditLog> getByTarget(@PathVariable String targetType, @PathVariable String targetId) {
        return auditService.getByTarget(targetType, targetId);
    }

    @GetMapping("/range")
    public List<AuditLog> getByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return auditService.getByTimeRange(start, end);
    }
}
