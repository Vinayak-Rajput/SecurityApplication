package com.vinayak.secapp.audit.repository;

import com.vinayak.secapp.audit.entity.AuditLogRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogWriterRepository extends JpaRepository<AuditLogRecord, Long> {
}
