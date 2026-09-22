# URL shortener

This deliberately tiny Kotlin/Spring Boot URL shortener is a Render deployment exercise. It has one page, one database table, and a process health endpoint.

## Local development

Use JDK 21. Run tests with `./gradlew test`, then start the server with `./gradlew bootRun`. Open <http://localhost:8080>.

For a quick HTTP check:

```sh
curl http://localhost:8080/healthz
curl -i -X POST -d 'targetUrl=https://example.com' http://localhost:8080/links
```

The default configuration uses an in-memory H2 database. Links disappear when the local process stops.

## Configuration

The `prod` profile uses PostgreSQL. Set these environment variables (see `.env.example`):

| Variable | Value |
| --- | --- |
| `SPRING_DATASOURCE_URL` | JDBC URL such as `jdbc:postgresql://internal-host:5432/database_name` |
| `SPRING_DATASOURCE_USERNAME` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | Database password |

Use the host, port, and database name from Render Postgres's **internal** connection details when the service and database are in the same region. Render's `postgres://...` URL cannot be pasted directly into `SPRING_DATASOURCE_URL`; Spring needs the `jdbc:postgresql://...` form and separate credentials. Flyway applies the schema migration at startup. Hibernate only validates the schema.

## Render deployment prerequisites

Render currently runs Kotlin/JVM services from a Dockerfile. Choose its Docker runtime for a Web Service backed by this Git repository. The Dockerfile runs `./gradlew --no-daemon clean build` to build and test, then `java -jar app.jar` to start; Render does not expose separate build and start command fields for Docker services. The container uses JDK 21 and activates the `prod` profile.

Set the three datasource variables above. The application listens on Render's `PORT` (the container defaults to `10000`), binds to all interfaces, and exposes `/healthz` for an HTTP health check. A Git provider repository is needed before Render can deploy this local project from Git.

## Click counting

Each successful short-link redirect increments its click count. The recent-links list links each slug to that redirect and displays its count. Flyway migration V2 initializes existing links to zero.

## Deployment visibility

The home page displays the running Git commit. Responses include `X-App-Version`, and non-health requests log the commit, method, path, and status. Render supplies `RENDER_GIT_COMMIT` automatically; local runs display `local`.

## Deliberately omitted features

Authentication, custom slugs, detailed analytics, caching, queues, and background jobs are outside scope.
