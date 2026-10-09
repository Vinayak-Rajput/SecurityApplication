package com.hdfclife.capstoneone.audit.repository;

import com.hdfclife.capstoneone.audit.entity.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogReaderRepository extends JpaRepository<AuditLogEntry, Long> {

    // Keep exact limit/offset semantics and stable ordering without fetching a page-count query.
    @Query(value = """
            SELECT *
            FROM public.application_logs
            ORDER BY event_time DESC, id DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<AuditLogEntry> findRecentLogs(@Param("limit") int limit, @Param("offset") int offset);
}
