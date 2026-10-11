package com.example.excelexport.service;

import com.example.excelexport.model.ExportJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobStorageService {

    private static final Logger log = LoggerFactory.getLogger(JobStorageService.class);

    // In-memory ConcurrentHashMap for Phase 1. In Phase 2, this can be swapped with Redis / Postgres table.
    private final Map<String, ExportJob> jobMap = new ConcurrentHashMap<>();

    public ExportJob createJob(String jobId) {
        ExportJob job = new ExportJob(jobId);
        jobMap.put(jobId, job);
        return job;
    }

    public ExportJob getJob(String jobId) {
        return jobMap.get(jobId);
    }

    public boolean removeJob(String jobId) {
        ExportJob job = jobMap.remove(jobId);
        if (job != null && job.getFilePath() != null) {
            File file = new File(job.getFilePath());
            if (file.exists()) {
                return file.delete();
            }
        }
        return false;
    }

    public Map<String, ExportJob> getAllJobs() {
        return jobMap;
    }

    /**
     * Periodic cleanup of expired jobs & temp files older than 2 hours
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupOldJobs() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        jobMap.entrySet().removeIf(entry -> {
            ExportJob job = entry.getValue();
            if (job.getCreatedAt() != null && job.getCreatedAt().isBefore(cutoff)) {
                if (job.getFilePath() != null) {
                    File file = new File(job.getFilePath());
                    if (file.exists()) {
                        file.delete();
                    }
                }
                log.info("Cleaned up expired export job {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}
