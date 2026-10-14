package com.profession.suggest.database.repositories.dataanalys.comparison;

import com.profession.suggest.database.entities.dataanalys.comparison.ComparisonSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ComparisonSessionRepository extends JpaRepository<ComparisonSession, Long> {
    Optional<ComparisonSession> findByClientSessionId(String clientSessionId);
    List<ComparisonSession> findByAccountId(Long accountId);

    Page<ComparisonSession> findByAccountId(Long accountId, Pageable pageable);

    List<ComparisonSession> findByCollectionId(Long collectionId);

    @Query("SELECT s FROM ComparisonSession s WHERE s.account.id = :accountId AND s.collection.id = :collectionId ORDER BY s.createdAt DESC")
    List<ComparisonSession> findByAccountIdAndCollectionId(@Param("accountId") Long accountId,
                                                           @Param("collectionId") Long collectionId);

    @Query("SELECT s FROM ComparisonSession s WHERE s.account.id = :accountId AND s.isCompleted = true")
    List<ComparisonSession> findCompletedByAccountId(@Param("accountId") Long accountId);

    long countByAccountIdAndCollectionId(Long accountId, Long collectionId);

}
