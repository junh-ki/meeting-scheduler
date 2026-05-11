# TODO

1. Timeslot mutation (merging timeslots again upon meeting deletion)
2. Get timeslot by status (sort by start and endtime - ascending)
3. Get meetings by user (sort by start and endtime - ascending)
4. Nested Unit-tests
5. DB indexing for better performance
6. Postman collection

---

# Meeting Scheduler

A REST API for scheduling meetings against organizer availability. Organizers publish free timeslots; booking a meeting automatically splits or shrinks the covering slot so the remaining availability stays accurate.

## Architecture

The project follows a standard layered architecture inside a single Spring Boot application.

```
meeting-scheduler/
├── user/           # User registration and lookup
├── timeslot/       # Availability management (CRUD + overlap/merge logic)
├── meeting/        # Meeting creation and listing
└── participant/    # Join entity linking meetings to attendees
```

Each domain package contains:

| Layer | Role |
|---|---|
| `*Controller` | HTTP entry point, validates input, delegates to service |
| `*Service` | Business logic and transaction boundary |
| `*Repository` | Spring Data JPA, derived queries only, no string SQL |
| `*Entity` | JPA-managed table |
| `*Mapper` | MapStruct, entity to DTO conversion |
| `dto/` | Request and response records with Bean Validation constraints |

**Tech stack:** Java 21 · Spring Boot 4 · Spring Data JPA · PostgreSQL · Flyway · MapStruct · Lombok · springdoc-openapi

**Database schema:**

```
app_user ──< timeslot
app_user ──< meeting ──< participant >── app_user
```

## Core Features

### Timeslot Management

An organizer publishes availability as a timeslot with a `FREE` or `BOOKED` status. When timeslots are created, adjacent or overlapping slots for the same organizer are **automatically merged** into one to keep availability contiguous.

**Overlap / adjacency merge example:**

| Action | Before | After |
|---|---|---|
| Create `10:00-11:00` | (none) | `10:00-11:00 FREE` |
| Create `11:00-12:00` | `10:00-11:00 FREE` | `10:00-12:00 FREE` (merged) |
| Create `09:00-10:30` | `10:00-12:00 FREE` | `09:00-12:00 FREE` (merged) |

### Meeting Creation with Timeslot Mutation

A meeting is booked by specifying the organizer ID, desired start time, and end time. 
Before the meeting is created, **every party must have a `FREE` timeslot that fully covers the requested range** — the organizer and each participant. If any party has no covering slot the request is rejected with `422 Unprocessable Entity`. When all slots are confirmed, each covering slot is split in exactly the same way.

**Split cases:**

**1. Exact fit:** meeting fills the slot exactly, slot becomes `BOOKED`

```
Before:  [10:00 ──────── 11:00] FREE
Meeting:  10:00 ──────── 11:00
After:   [10:00 ──────── 11:00] BOOKED
```

**2. Right remainder:** meeting starts at slot start but ends before slot end

```
Before:  [10:00 ──────────────── 12:00] FREE
Meeting:  10:00 ──────── 11:00
After:   [10:00 ──────── 11:00] BOOKED  +  [11:00 ──── 12:00] FREE
```

**3. Left remainder:** meeting starts after slot start but ends at slot end

```
Before:  [09:00 ──────────────── 11:00] FREE
Meeting:          10:00 ──────── 11:00
After:   [09:00 ── 10:00] FREE  +  [10:00 ──────── 11:00] BOOKED
```

**4. Both remainders:** meeting occupies the middle of the slot

```
Before:  [09:00 ──────────────────────── 12:00] FREE
Meeting:          10:00 ──────── 11:00
After:   [09:00 ── 10:00] FREE  +  [10:00 ──────── 11:00] BOOKED  +  [11:00 ──── 12:00] FREE
```

### Duplicate and Conflict Protection

| Scenario | Response |
|---|---|
| Timeslot with the same organizer, start, and end already exists | `409 Conflict` |
| User with the same email already exists | `409 Conflict` |
| Meeting with the same organizer and time range already exists | `409 Conflict` |
| Organizer has no FREE slot covering the requested range | `422 Unprocessable Entity` |
| Any participant has no FREE slot covering the requested range | `422 Unprocessable Entity` |

Pessimistic write locking (`SELECT FOR UPDATE`) on the organizer row serializes concurrent timeslot creation per user. A `UNIQUE` constraint on each table acts as the final safety net for concurrent requests that pass the application-level check simultaneously.

## Demo Start Instructions

**Prerequisites:** Java 21, Maven, Docker.

**1. Start the application:**

```bash
./run.sh
```

`run.sh` starts the Postgres container via Docker Compose, waits until the database is ready, builds the JAR, and launches the application. Flyway applies all migrations automatically on startup. The application listens on `http://localhost:8080`.

When you terminate the process (Ctrl+C), Spring Boot shuts down gracefully and the Postgres container is stopped, but the Docker volume is preserved so data persists across restarts.

To fully wipe the database:

```bash
docker compose down --volumes
```

**2. Run the tests:**

```bash
./mvnw clean test
```

## Resources

| Resource | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Spring Boot 4 docs | https://docs.spring.io/spring-boot/index.html |
| Spring Data JPA | https://docs.spring.io/spring-data/jpa/reference/jpa.html |
| Flyway migrations | https://documentation.red-gate.com/flyway |
