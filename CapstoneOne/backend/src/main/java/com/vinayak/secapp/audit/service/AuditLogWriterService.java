package com.vinayak.secapp.audit.service;

import com.vinayak.secapp.audit.entity.AuditLogRecord;
import com.vinayak.secapp.audit.model.AuditEvent;
import com.vinayak.secapp.audit.repository.AuditLogWriterRepository;
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
