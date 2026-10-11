# High-Performance Millions-Record Excel Export Service (Phase 1)

A production-ready Spring Boot service designed to export large datasets (thousands to millions of records) to `.xlsx` Excel spreadsheets with low memory footprint and zero HTTP timeouts.

---

## Architecture Highlights

1. **Non-Blocking Asynchronous Jobs (`202 Accepted`):**
   - The client triggers an export via `POST /api/v1/exports/generate` and immediately receives a `jobId`.
   - Excel generation runs in a dedicated background `ThreadPoolTaskExecutor`.
   - Eliminates gateway timeouts (Cloudflare, Nginx, ALB 504 errors).

2. **Fast Streaming Excel Generation (`FastExcel`):**
   - Uses `org.dhatim:fastexcel` which streams raw XML directly to disk/ZIP stream.
   - **3x–5x faster** than Apache POI with a tiny fraction of the memory footprint.

3. **Memory Safety with JDBC Cursor Streaming:**
   - Rows are fetched via `JdbcTemplate.query(..., RowCallbackHandler)` with a batch `fetchSize = 2000`.
   - Never loads millions of rows into a `List<Employee>` on the JVM heap.

4. **Automatic Sheet Rollover:**
   - Automatically rolls over to multiple worksheets (`Employees Part 1`, `Employees Part 2`) when record counts exceed the Excel worksheet limit of 1,000,000 rows.

5. **Real-time Progress Polling:**
   - Exposes `GET /api/v1/exports/status/{jobId}` returning `percentage`, `processedRecords`, and `totalRecords`.

---

## API Endpoints

### 1. Trigger Export Job
**Request:**
```http
POST /api/v1/exports/generate
Content-Type: application/json

{
  "department": "Engineering",
  "maxRows": 500000
}
```
*(Both `department` and `maxRows` are optional)*

**Response (`202 Accepted`):**
```json
{
  "jobId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "PENDING",
  "totalRecords": 0,
  "processedRecords": 0,
  "percentage": 0,
  "fileName": null,
  "fileSizeBytes": 0,
  "downloadUrl": null,
  "errorMessage": null,
  "createdAt": "2025-05-18T10:15:30.123"
}
```

---

### 2. Poll Job Status
**Request:**
```http
GET /api/v1/exports/status/{jobId}
```

**Response (`200 OK` - While processing):**
```json
{
  "jobId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "IN_PROGRESS",
  "totalRecords": 250000,
  "processedRecords": 120000,
  "percentage": 48,
  "downloadUrl": null
}
```

**Response (`200 OK` - When completed):**
```json
{
  "jobId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "COMPLETED",
  "totalRecords": 250000,
  "processedRecords": 250000,
  "percentage": 100,
  "fileName": "employees_export_3fa85f64-5717-4562-b3fc-2c963f66afa6.xlsx",
  "fileSizeBytes": 14250320,
  "downloadUrl": "/api/v1/exports/download/3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "completedAt": "2025-05-18T10:15:34.890"
}
```

---

### 3. Download Generated Excel
**Request:**
```http
GET /api/v1/exports/download/{jobId}
```
**Response:** Binary `.xlsx` file attachment with Content-Disposition headers.

---

### 4. Seed Test Records
**Request:**
```http
POST /api/v1/exports/seed?count=200000
```
**Response:**
```text
Seeded 200000 employees in 2450 ms.
```

---

## How to Run & Test

```bash
# 1. Navigate to the project folder
cd excel-export

# 2. Build and run tests
mvn clean test

# 3. Start the application
mvn spring-boot:run
```
