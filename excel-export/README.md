# Distributed Enterprise Millions-Record Export Service (Phase 2)

An enterprise-grade, distributed, high-performance export architecture for Spring Boot designed to generate and stream multi-million record datasets (`.xlsx` or `.csv.gz`) with zero HTTP timeouts, low memory overhead, persistent job tracking, and real-time progress push notifications.

---

## 🏛️ Phase 2 Architecture Overview

```
                      ┌──────────────────────────────────────────────┐
                      │              Web Client (UI)                 │
                      └───────┬──────────────────────────────▲───────┘
                              │ 1. POST /generate            │ 3. SSE Stream
                              │    (202 Accepted)            │    (/notifications/sse/{id})
                              ▼                              │
                 ┌─────────────────────────┐                 │
                 │   Spring REST API       │                 │
                 └───────┬─────────┬───────┘                 │
                         │         │                         │
     2. Enqueue Task     │         │ Persist Initial Job     │
                         ▼         ▼                         │
          ┌──────────────────┐  ┌──────────────────┐         │
          │ Distributed      │  │ Persistent Store │         │
          │ Job Queue        │  │ (RDBMS / JPA)    │         │
          └────────┬─────────┘  └────────▲─────────┘         │
                   │                     │                   │
                   ▼                     │ Updates Status    │
          ┌──────────────────┐           │ & Progress        │
          │ Dedicated Worker │───────────┴───────────────────┘
          │ (Consumer Pool)  │
          └────────┬─────────┘
                   │
                   │ 4. Fast Streaming Gen + Upload
                   ▼
          ┌───────────────────────────────────┐
          │ Object Storage (Local/S3/MinIO/...)│
          └───────────────────────────────────┘
```

---

## 🚀 Key Features

1. **Decoupled Job Queue & Worker Consumers:**
   - Client requests are accepted immediately (`202 Accepted`) and submitted to a resilient job queue.
   - Dedicated worker pool consumes tasks with backpressure handling, error isolation, and **exponential backoff retries** (up to 3 retries).

2. **Persistent Job Store (Spring Data JPA):**
   - Job metadata and execution lifecycles are persisted in the database (`export_jobs`).
   - Survives server restarts and allows cross-cluster status queries.

3. **Real-Time Push Notifications via Server-Sent Events (SSE):**
   - Clients connect via `GET /api/v1/exports/notifications/sse/{jobId}`.
   - Workers push live percentage progress updates (0% -> 100%) and instant download URLs directly to the client without polling.

4. **Pluggable Object Storage Providers:**
   - **`Local`**: Fast local file storage with secure download tokens.
   - **`AWS S3`**: AWS SDK v2 with `S3Presigner` for direct client downloads.
   - **`MinIO`**: On-premise S3-compatible object storage with custom endpoints.
   - **`Azure Blob`**: Microsoft Azure Storage with SAS token generation.
   - **`Google GCS`**: Google Cloud Storage with V4 Signed URLs.

5. **Multi-Format Support:**
   - **`XLSX` (FastExcel):** Multi-sheet rollover (>1,000,000 rows/sheet) with 3x–5x performance over Apache POI.
   - **`CSV_GZ` (GZipped CSV):** Ultra-high throughput compressed CSV for massive datasets (5M–10M+ rows).

6. **Memory Safety with JDBC Cursor Streaming:**
   - Constant low heap footprint (<64MB JVM heap used even during 1M+ exports).

---

## 🐳 Docker & Docker Compose Deployment

### Run Complete Stack (Spring Boot + MinIO Object Storage)

```bash
cd Thread/excel-export

# Spin up Spring Boot service and MinIO Object Storage
docker compose up -d --build
```

- **Spring Boot API:** `http://localhost:8080`
- **MinIO S3 Endpoint:** `http://localhost:9000`
- **MinIO Web Console:** `http://localhost:9001` (User: `minioadmin`, Pass: `minioadmin`)

### Build & Run Single Container

```bash
# Build Docker image
docker build -t excel-export-service:latest .

# Run container with resource limits
docker run -d -p 8080:8080 \
  -e APP_STORAGE_TYPE=local \
  -m 512m --cpus 2.0 \
  --name excel-export excel-export-service:latest
```

---

## 📡 API Reference

### 1. Trigger Export Job
```http
POST /api/v1/exports/generate
Content-Type: application/json

{
  "format": "XLSX",           // or "CSV_GZ"
  "department": "Engineering", // optional filter
  "maxRows": 500000           // optional limit
}
```

**Response (`202 Accepted`):**
```json
{
  "jobId": "e306fc6e-07a8-4bb9-906d-b6574f179c3d",
  "status": "PENDING",
  "format": "XLSX",
  "totalRecords": 0,
  "processedRecords": 0,
  "percentage": 0,
  "downloadUrl": null,
  "createdAt": "2026-10-11T12:00:00.000"
}
```

---

### 2. Real-Time SSE Progress Stream
```http
GET /api/v1/exports/notifications/sse/{jobId}
Accept: text/event-stream
```

**Stream Events Received:**
```
event: INIT
data: Connected to progress stream for job e306fc6e-07a8-4bb9-906d-b6574f179c3d

event: PROGRESS
data: {"jobId":"...","status":"IN_PROGRESS","totalRecords":50000,"processedRecords":20000,"percentage":40}

event: PROGRESS
data: {"jobId":"...","status":"COMPLETED","totalRecords":50000,"processedRecords":50000,"percentage":100,"downloadUrl":"/api/v1/exports/download/e306fc6e-07a8-4bb9-906d-b6574f179c3d.xlsx"}
```

---

### 3. Poll Job Status (Alternative to SSE)
```http
GET /api/v1/exports/status/{jobId}
```

---

### 4. Download Exported File
```http
GET /api/v1/exports/download/{storageKey}
```

---

### 5. Seed Test Records
```http
POST /api/v1/exports/seed?count=100000
```

---

## 🧪 Local Testing Without Docker

```bash
# Run unit and integration tests
mvn clean test

# Run Spring Boot application locally
mvn spring-boot:run
```
