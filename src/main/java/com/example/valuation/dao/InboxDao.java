package com.example.valuation.dao;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.valuation.entity.Inbox;

public interface InboxDao extends JpaRepository<Inbox, UUID>{
	boolean existsByEventId(UUID eventId);
	
	@Modifying
    @Transactional
    @Query(value = """
        UPDATE valuation_inbox
        WHERE event_id IN (
            SELECT event_id
            FROM valuation_inbox
            WHERE status = 'NEW'
            ORDER BY created_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
        )
        RETURNING *
        """, nativeQuery = true)
    List<Inbox> claimBatch(@Param("batchSize") int batchSize);

    @Modifying
    @Query("UPDATE Inbox i SET i.status = 'DONE' WHERE i.eventId = :id")
    void markDone(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Inbox i SET i.status = 'FAILED' WHERE i.eventId = :id")
    void markFailed(@Param("id") UUID id);

}
