# notification-service

Turns events into **emails and push messages**, and remembers which browsers allowed push.
Port **8083**. Design: `infrastructure/docs/mvp/05-components/notification-service.md`.

This service is **domain-free**. It knows no shop words. It only does:
`event name -> template -> fill the blanks -> send`.
All event names live in one file: [`notification-events.yaml`](src/main/resources/notification-events.yaml).

## How a message is handled

1. RabbitMQ delivers an event to queue `notification.email` or `notification.push`.
2. The event name is the routing key. The headers `messageId` (UUID) and `traceId` come with it.
3. Already sent for this `messageId` and channel? Stop (the table `sent_notifications` remembers).
4. Look up the rule in `notification-events.yaml`. No rule? Ignore.
5. Email: send to `email` in the body. Push: send to every saved browser of `userId`.
6. Write the result to `sent_notifications`.

If sending fails, the message is tried 3 times. After that it goes to `notification.email.dlq`
or `notification.push.dlq` (dead-letter queue) for a person to check.
A message that can never work (no `messageId`, no email address) goes there at once.

The event body is **never** written to the log (a password reset body holds a secret link).

## Add a new message

1. Add an entry to `notification-events.yaml`.
2. For email, add `src/main/resources/templates/emails/<template>.html`.
3. Restart. The queue bindings follow the file by themselves.

## API

| Method | Path | Access |
|---|---|---|
| POST | `/api/v1/notification/push-devices` body `{"token":"..."}` | Login |
| DELETE | `/api/v1/notification/push-devices/{token}` | Login (your own token only) |
| GET | `/actuator/health` | Public (not routed by Nginx) |

Errors use the shared 7-field JSON (`timestamp, status, code, message, path, refreshable, details`).

## Settings

| Setting | Env name | Meaning |
|---|---|---|
| `spring.datasource.*` | `DB_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Its own database `notification_db` |
| `spring.rabbitmq.*` | `SPRING_RABBITMQ_HOST`, `_PORT`, `_USERNAME`, `_PASSWORD` | RabbitMQ |
| `spring.mail.*` | `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` | SMTP (Mailpit on a laptop) |
| `auth.token.secret` | `JWT_SECRET` | Same value as user-service |
| `notification.push.enabled` | `NOTIFICATION_PUSH_ENABLED` | `false` = push is only logged |
| `notification.push.firebase-credentials-file` | `NOTIFICATION_PUSH_FIREBASE_CREDENTIALS_FILE` | Firebase key file (a secret, never in git) |

Profiles: `dev` (default, IntelliJ, helpers on `localhost`), `docker` (all values from the env file), `prod`.

## Run and test

```bash
./mvnw test                      # unit and web tests (H2, no Docker needed)
./mvnw spring-boot:run           # needs the helpers: docker compose up -d postgres rabbitmq mailpit
```

Check: open http://localhost:8083/actuator/health. You should see `{"status":"UP"}`.
