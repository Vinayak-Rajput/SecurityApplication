package com.vinayak.secapp.audit.service;

import com.vinayak.secapp.common.exception.InvalidRequestException;
import com.vinayak.secapp.audit.repository.AuditLogReaderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogReaderServiceTest {

    @InjectMocks
    private AuditLogService auditLogService;

    @Mock
    private AuditLogReaderRepository auditLogReaderRepository;

    @BeforeEach
    void setUp() {
    }

    @Test
    void findRecentLogsRejectsInvalidPagination() {
        assertThrows(InvalidRequestException.class, () -> auditLogService.findRecentLogs(0, 0));
        assertThrows(InvalidRequestException.class, () -> auditLogService.findRecentLogs(501, 0));
        assertThrows(InvalidRequestException.class, () -> auditLogService.findRecentLogs(10, -1));

        verify(auditLogReaderRepository, never()).findRecentLogs(0, 0);
    }

}
