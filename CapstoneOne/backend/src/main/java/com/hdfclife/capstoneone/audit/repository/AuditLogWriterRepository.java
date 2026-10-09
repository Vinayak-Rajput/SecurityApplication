package com.hdfclife.capstoneone.audit.repository;

import com.hdfclife.capstoneone.audit.entity.AuditLogRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogWriterRepository extends JpaRepository<AuditLogRecord, Long> {
}
