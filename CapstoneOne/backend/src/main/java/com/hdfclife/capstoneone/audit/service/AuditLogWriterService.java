package com.hdfclife.capstoneone.audit.service;

import com.hdfclife.capstoneone.audit.entity.AuditLogRecord;
import com.hdfclife.capstoneone.audit.model.AuditEvent;
import com.hdfclife.capstoneone.audit.repository.AuditLogWriterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogWriterService {

    private final AuditLogWriterRepository auditLogWriterRepository;

    @Transactional(transactionManager = "auditLogWriterTransactionManager")
    public void persist(AuditEvent event) {

        auditLogWriterRepository.save(AuditLogRecord.from(event));
    }
}
