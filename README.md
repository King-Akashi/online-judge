# MiniJudge

MiniJudge is a Java-only online judge built from three Spring Boot services, a
shared contracts module, and a Picocli CLI. Problem data, submission state, and
judging work have separate owners and databases.

## Requirements

- JDK 21 or later
- Maven 3.9+
- Docker with Docker Compose

## Build and test

```powershell
mvn clean verify
```

## Run the full local stack

Copy `.env.example` to `.env` and replace `MINIJUDGE_INTERNAL_API_KEY` with a
random local value. These defaults are for development only.

```powershell
docker build -t minijudge/java-runner:21 -f judge-service/runner/Dockerfile judge-service/runner
docker compose -f infrastructure/docker-compose.yml up --build -d
```

The Compose stack builds and starts PostgreSQL, RabbitMQ, the three services,
and their health checks. PostgreSQL uses a separate database and login role for
each service. RabbitMQ's management UI is at <http://localhost:15672>.

| Component | Local URL |
| --- | --- |
| Problem Service health | <http://localhost:8081/actuator/health> |
| Submission Service health | <http://localhost:8082/actuator/health> |
| Judge Service health | <http://localhost:8083/actuator/health> |
| RabbitMQ management | <http://localhost:15672> |
| PostgreSQL | `localhost:5433` |

Ports and credentials can be overridden in `.env`. PostgreSQL and RabbitMQ
data live in named volumes and remain after `docker compose down`.

For a PostgreSQL volume created by an earlier single-role setup, run the
database initializer once, then grant each service role access to existing
objects:

```powershell
docker compose -f infrastructure/docker-compose.yml exec postgres psql -U minijudge -d postgres -f /docker-entrypoint-initdb.d/10-init-databases.sql
docker compose -f infrastructure/docker-compose.yml exec postgres psql -U minijudge -d problems -c "GRANT ALL ON SCHEMA public TO problem_service; GRANT ALL ON ALL TABLES IN SCHEMA public TO problem_service; GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO problem_service"
docker compose -f infrastructure/docker-compose.yml exec postgres psql -U minijudge -d submissions -c "GRANT ALL ON SCHEMA public TO submission_service; GRANT ALL ON ALL TABLES IN SCHEMA public TO submission_service; GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO submission_service"
docker compose -f infrastructure/docker-compose.yml exec postgres psql -U minijudge -d judge -c "GRANT ALL ON SCHEMA public TO judge_service; GRANT ALL ON ALL TABLES IN SCHEMA public TO judge_service; GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO judge_service"
```

## Use the CLI

The packaged CLI runs on the host and uses the default service URLs:

```powershell
java -jar minijudge-cli/target/minijudge-cli-1.0.0-SNAPSHOT.jar list
java -jar minijudge-cli/target/minijudge-cli-1.0.0-SNAPSHOT.jar show <problem-uuid>
java -jar minijudge-cli/target/minijudge-cli-1.0.0-SNAPSHOT.jar submit <problem-uuid> --file Main.java
java -jar minijudge-cli/target/minijudge-cli-1.0.0-SNAPSHOT.jar status <submission-uuid>
java -jar minijudge-cli/target/minijudge-cli-1.0.0-SNAPSHOT.jar history
```

`submit` waits for a result by default. Use `--no-wait` to return as soon as the
submission has been queued. Set `MINIJUDGE_PROBLEM_URL` and
`MINIJUDGE_SUBMISSION_URL` to use non-default URLs.

The CLI can also run from Compose:

```powershell
docker compose -f infrastructure/docker-compose.yml run --rm minijudge-cli list
```

## API and events

Public API routes, payloads, lifecycle transitions, and RabbitMQ event schemas
are documented in [docs/contracts.md](docs/contracts.md). The service layout
and data-flow are in [docs/architecture.md](docs/architecture.md); retry,
dead-letter, idempotency, and recovery behavior are in
[docs/reliability.md](docs/reliability.md).

## Execution isolation

The trusted Judge Service uses Docker to create a fresh bounded execution
container. Submitted code runs as an unprivileged user with networking disabled,
a read-only root filesystem, a read-only source volume, CPU/memory/PID limits,
and dropped Linux capabilities. The host Docker socket is mounted only into the
trusted Judge Service in local Compose; it is never mounted into a submitted
program's execution container. Use an isolated Docker host for deployments that
run code from untrusted users.
