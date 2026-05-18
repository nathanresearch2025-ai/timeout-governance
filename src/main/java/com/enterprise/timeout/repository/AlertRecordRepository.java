package com.enterprise.timeout.repository;

import com.enterprise.timeout.model.AlertRecord;
import com.enterprise.timeout.model.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {

    List<AlertRecord> findByTimeoutEventIdAndStatus(Long timeoutEventId, AlertStatus status);

    List<AlertRecord> findByStatus(AlertStatus status);
}
