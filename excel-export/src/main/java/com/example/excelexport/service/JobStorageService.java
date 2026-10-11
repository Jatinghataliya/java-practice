package com.example.excelexport.service;

import com.example.excelexport.model.ExportFormat;
import com.example.excelexport.model.ExportJobEntity;
import com.example.excelexport.model.ExportStatus;
import com.example.excelexport.repository.ExportJobRepository;
import com.example.excelexport.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobStorageService {

    private static final Logger log = LoggerFactory.getLogger(JobStorageService.class);

    private final ExportJobRepository exportJobRepository;
    private final StorageService storageService;

    public JobStorageService(ExportJobRepository exportJobRepository, StorageService storageService) {
        this.exportJobRepository = exportJobRepository;
        this.storageService = storageService;
    }

    @Transactional
    public ExportJobEntity createJob(String jobId, ExportFormat format, String department, Long maxRows) {
        ExportJobEntity job = new ExportJobEntity(jobId);
        job.setFormat(format != null ? format : ExportFormat.XLSX);
        job.setDepartmentFilter(department);
        job.setMaxRowsLimit(maxRows);
        job.setStatus(ExportStatus.PENDING);
        job.setCreatedAt(LocalDateTime.now());
        return exportJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Optional<ExportJobEntity> getJob(String jobId) {
        return exportJobRepository.findByJobId(jobId);
    }

    @Transactional(readOnly = true)
    public Optional<ExportJobEntity> getJobByToken(String token) {
        return exportJobRepository.findByDownloadToken(token);
    }

    @Transactional
    public ExportJobEntity saveJob(ExportJobEntity job) {
        return exportJobRepository.save(job);
    }

    @Transactional
    public boolean removeJob(String jobId) {
        Optional<ExportJobEntity> jobOpt = exportJobRepository.findByJobId(jobId);
        if (jobOpt.isPresent()) {
            ExportJobEntity job = jobOpt.get();
            if (job.getStorageKey() != null) {
                storageService.deleteFile(job.getStorageKey());
            }
            exportJobRepository.delete(job);
            return true;
        }
        return false;
    }

    @Transactional
    public String generateDownloadToken(String jobId) {
        Optional<ExportJobEntity> jobOpt = exportJobRepository.findByJobId(jobId);
        if (jobOpt.isPresent()) {
            ExportJobEntity job = jobOpt.get();
            String token = UUID.randomUUID().toString().replace("-", "");
            job.setDownloadToken(token);
            job.setTokenExpiresAt(LocalDateTime.now().plusHours(2));
            exportJobRepository.save(job);
            return token;
        }
        return null;
    }

    /**
     * Periodic cleanup of expired jobs & storage files older than 24 hours
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void cleanupOldJobs() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<ExportJobEntity> expiredJobs = exportJobRepository.findByCreatedAtBefore(cutoff);

        for (ExportJobEntity job : expiredJobs) {
            if (job.getStorageKey() != null) {
                storageService.deleteFile(job.getStorageKey());
            }
            exportJobRepository.delete(job);
            log.info("Cleaned up expired export job {}", job.getJobId());
        }
    }
}
