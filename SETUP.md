# MomentForeverApp Setup Guide

This guide describes how to set up and run the MomentForeverApp project.

## Prerequisites

- **Java 17 CLI** (Ensure `java -version` returns 17)
- **VS Code** with the **Extension Pack for Java** installed.

## Setup Steps

1.  **Open the Project in VS Code:**
    - Open the `MomentForeverApp` folder in VS Code.
    - When prompted, install the recommended extensions.

2.  **Install Dependencies:**
    - Open a terminal in VS Code (Ctrl+`).
    - Run the following command to download all dependencies and build the project:
      ```powershell
      mvn clean install
      ```
    - The first run may take a few minutes as it downloads dependencies.
    - *Note:* the `mvnw` / `mvnw.cmd` wrapper scripts are **not** present in this
      repository (only `.mvn/wrapper/maven-wrapper.properties`), so use a locally
      installed Maven 3.8+, or regenerate the wrapper with `mvn wrapper:wrapper`.

3.  **Run the Application:**
    - The main application class is `MomentForeverApp` in `moment_forever_core`.
    - Right-click and choose "Run".
    - Or run from the command line:
      ```powershell
      mvn spring-boot:run -pl moment_forever_core
      ```
    - The service starts on `http://localhost:8081/platform`.
    - Backing services must be reachable first: PostgreSQL, MongoDB, Redis and
      Kafka. See [README.md](README.md) for the environment variables that point
      the service at them.

## Module Structure

- `moment_forever_commons`: Shared DTOs, Kafka event contracts and snapshots, error handling.
- `moment_forever_data`: JPA entities and the DAO layer.
- `moment_forever_core`: The runnable Spring Boot service - controllers, services, Kafka producer/consumers, Quartz jobs.
- `moment_forever_security`: JWT service, gateway header authentication filter, Spring Security configuration.
- `moment_forever_object_store`: Object storage abstraction (MongoDB GridFS, S3).

For the service architecture and the booking event flow, see
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and
[docs/BOOKING_EVENT_FLOW.md](docs/BOOKING_EVENT_FLOW.md).

## Troubleshooting

- If you see "Project configuration is not up-to-date with pom.xml", right-click `pom.xml` and choose **Update Project** or **Reload Project**.
- Ensure your `JAVA_HOME` environment variable points to a valid JDK 17 installation.
