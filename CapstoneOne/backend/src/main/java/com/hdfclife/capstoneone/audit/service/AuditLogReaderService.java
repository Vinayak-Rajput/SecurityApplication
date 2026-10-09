package com.hdfclife.capstoneone.audit.service;

import com.hdfclife.capstoneone.audit.dto.AuditLogResponse;
import com.hdfclife.capstoneone.audit.mapper.AuditLogMapper;
import com.hdfclife.capstoneone.common.exception.InvalidRequestException;
import com.hdfclife.capstoneone.audit.repository.AuditLogReaderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogReaderService {

    private final AuditLogReaderRepository auditLogReaderRepository;
    private final AuditLogMapper auditLogMapper;

    @Transactional(transactionManager = "auditLogReaderTransactionManager", readOnly = true)
    public List<AuditLogResponse> findRecentLogs(int limit, int offset) {

        if (limit < 1 || limit > 500 || offset < 0) {
            throw new InvalidRequestException("limit must be between 1 and 500 and offset must be non-negative");
        }

        return auditLogReaderRepository.findRecentLogs(limit, offset).stream()
                .map(auditLogMapper::toResponse)
                .toList();
    }
}
