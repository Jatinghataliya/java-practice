package com.example.excelexport.repository;

import com.example.excelexport.model.ExportJobEntity;
import com.example.excelexport.model.ExportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJobEntity, String> {

    Optional<ExportJobEntity> findByJobId(String jobId);

    Optional<ExportJobEntity> findByDownloadToken(String downloadToken);

    List<ExportJobEntity> findByStatus(ExportStatus status);

    List<ExportJobEntity> findByCreatedAtBefore(LocalDateTime cutoff);

    @Modifying
    @Query("DELETE FROM ExportJobEntity j WHERE j.createdAt < :cutoff")
    int deleteOldJobs(@Param("cutoff") LocalDateTime cutoff);
}
