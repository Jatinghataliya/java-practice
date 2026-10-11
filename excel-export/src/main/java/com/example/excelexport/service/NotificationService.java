package com.example.excelexport.service;

import com.example.excelexport.dto.JobProgressEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    // Emitters by Job ID (supports multiple browser tabs / clients subscribed to same job)
    private final Map<String, List<SseEmitter>> emitterMap = new ConcurrentHashMap<>();

    /**
     * Subscribe to real-time SSE progress events for a specific job
     */
    public SseEmitter subscribe(String jobId) {
        // 10 minutes timeout for long-running export jobs
        SseEmitter emitter = new SseEmitter(600_000L);

        emitterMap.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(jobId, emitter));
        emitter.onTimeout(() -> removeEmitter(jobId, emitter));
        emitter.onError(e -> removeEmitter(jobId, emitter));

        try {
            // Send initial connected heartbeat
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("Connected to progress stream for job " + jobId));
        } catch (IOException e) {
            removeEmitter(jobId, emitter);
        }

        return emitter;
    }

    /**
     * Broadcast progress event to all active subscribers for the job
     */
    public void notifyProgress(JobProgressEvent event) {
        List<SseEmitter> emitters = emitterMap.get(event.getJobId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("PROGRESS")
                        .data(event));

                // If job is finished, complete the emitter stream
                if (event.getStatus().name().equals("COMPLETED") ||
                    event.getStatus().name().equals("FAILED") ||
                    event.getStatus().name().equals("CANCELLED")) {
                    emitter.complete();
                    removeEmitter(event.getJobId(), emitter);
                }
            } catch (Exception e) {
                removeEmitter(event.getJobId(), emitter);
            }
        }
    }

    private void removeEmitter(String jobId, SseEmitter emitter) {
        List<SseEmitter> emitters = emitterMap.get(jobId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emitterMap.remove(jobId);
            }
        }
    }
}
