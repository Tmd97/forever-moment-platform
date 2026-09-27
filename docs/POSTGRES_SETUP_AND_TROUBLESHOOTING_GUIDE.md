# PostgreSQL Setup, Timezone & Database Migration Guide

This document records the exact configuration changes, root causes, and setup steps for PostgreSQL, Docker Compose, Spring Boot microservices, and DBeaver.

---

## 1. PostgreSQL Timezone Configuration (`Asia/Calcutta` / IST)

### **The Problem**
When running applications or connecting via DB tools (e.g., DBeaver, psql, Spring Boot) from a machine set to Indian Standard Time (IST), client drivers send `TimeZone="Asia/Calcutta"` during session startup. 

The default PostgreSQL Docker container uses the IANA timezone database where `Asia/Kolkata` is the primary name, but the legacy alias `Asia/Calcutta` is not present in `/usr/share/zoneinfo/Asia/`, causing the connection error:
```text
FATAL: invalid value for parameter "TimeZone": "Asia/Calcutta"
```

---

### **Solutions & Setup Steps**

#### **Step 1: Docker Compose Configuration (`docker-compose.yml`)**
Ensure all PostgreSQL database services are configured with `TZ`, `PGTZ`, and the `command` flag in `docker-compose.yml`:

```yaml
services:
  postgres-booking:
    image: postgres:15
    container_name: postgres-booking
    restart: always
    environment:
      POSTGRES_DB: moment_forever_book
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: groote
      TZ: Asia/Kolkata
      PGTZ: Asia/Kolkata
    command: postgres -c timezone=Asia/Kolkata
    ports:
      - "5433:5432"

  postgres-platform:
    image: postgres:15
    container_name: postgres-platform
    restart: always
    environment:
      POSTGRES_DB: moment_forever_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: groote
      TZ: Asia/Kolkata
      PGTZ: Asia/Kolkata
    command: postgres -c timezone=Asia/Kolkata
    ports:
      - "5432:5432"

  postgres-payment:
    image: postgres:15
    container_name: postgres-payment
    restart: always
    environment:
      POSTGRES_DB: moment_forever_payment
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: groote
      TZ: Asia/Kolkata
      PGTZ: Asia/Kolkata
    command: postgres -c timezone=Asia/Kolkata
    ports:
      - "5434:5432"
```

#### **Step 2: Create Timezone Symlink in Fresh Docker Containers**
For fresh Docker container setups, create the timezone alias symlink inside the containers so any client requesting `Asia/Calcutta` is natively accepted:

```bash
docker exec postgres-booking ln -s /usr/share/zoneinfo/Asia/Kolkata /usr/share/zoneinfo/Asia/Calcutta
docker exec postgres-platform ln -s /usr/share/zoneinfo/Asia/Kolkata /usr/share/zoneinfo/Asia/Calcutta
docker exec postgres-payment ln -s /usr/share/zoneinfo/Asia/Kolkata /usr/share/zoneinfo/Asia/Calcutta
```

#### **Step 3: DBeaver Connection Configuration**
If configuring DBeaver on a fresh machine:
1. Open DBeaver -> Edit Connection (`moment_forever_book` / `moment_forever_db` / `moment_forever_payment`).
2. Go to **Driver properties** tab.
3. Add or set `options`:
   ```text
   -c timezone=Asia/Kolkata
   ```
4. Click **Test Connection**.

---

## 2. Database Schema & Inventory Model (`current_bookings`)

### **The Problem**
Calling `/platform/admin/experiences/{expId}/locations/{locId}/timeslots/{slotId}` threw:
```text
ERROR: null value in column "current_bookings" of relation "experience_time_slot_mappers" violates not-null constraint
```

### **Root Cause**
Booking capacity and current bookings are managed by the `slot_inventory` entity table. The `current_bookings` column in the `experience_time_slot_mappers` table is obsolete and not mapped in Java's `ExperienceTimeSlotMapper` entity.

### **Fix Requirement**
Run this SQL command in DBeaver/psql on your database:

```sql
ALTER TABLE experience_time_slot_mappers DROP COLUMN IF EXISTS current_bookings;
```

---

## 3. Spring Boot Application Configuration

To ensure consistent time processing across microservices (`booking`, `platform`, `payment`), configure UTC/IST connection options in `application.yml` and the Application `main` class.

### **`application.yml` Datasource URL**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:5433/moment_forever_book?options=-c%20timezone%3DUTC
```

### **Spring Boot Main Application Class**
```java
@SpringBootApplication
@EnableDiscoveryClient
public class MomentForeverBookingApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(MomentForeverBookingApplication.class, args);
    }
}
```

---

## 4. Fresh Setup Quick Checklist

When setting up the project freshly on a new machine:

1. **Start Docker Infrastructure**:
   ```bash
   docker-compose up -d
   ```
2. **Apply Timezone Alias Symlinks**:
   ```bash
   docker exec postgres-booking ln -s /usr/share/zoneinfo/Asia/Kolkata /usr/share/zoneinfo/Asia/Calcutta
   docker exec postgres-platform ln -s /usr/share/zoneinfo/Asia/Kolkata /usr/share/zoneinfo/Asia/Calcutta
   docker exec postgres-payment ln -s /usr/share/zoneinfo/Asia/Kolkata /usr/share/zoneinfo/Asia/Calcutta
   ```
3. **Execute Database Migrations / Schema Cleanup**:
   ```sql
   ALTER TABLE experience_time_slot_mappers DROP COLUMN IF EXISTS current_bookings;
   ```
4. **Build & Launch Microservices**:
   ```bash
   mvn clean compile
   ```
