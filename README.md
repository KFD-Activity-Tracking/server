# KFD Activity Tracker — Server

Spring Boot 3.x REST API для хранения и анализа статистики активности пользователей.

## Запуск

```bash
# Требуется Java 21 (Kotlin 1.9.25 несовместим с Java 26+)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew bootRun
```

По умолчанию поднимается на `localhost:8765` с H2 in-memory базой.

### С MySQL через Docker Compose

```bash
docker-compose -f src/main/resources/docker/docker-compose.yml up -d
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew bootRun
```

## Тестовые аккаунты

При первом запуске автоматически создаются:

| Логин | Пароль | Роль |
|-------|--------|------|
| admin | admin | ADMIN |
| manager1 | manager1 | MANAGER |
| manager2 | manager2 | MANAGER |
| user1–user4 | user1–user4 | USER |

manager1 → user1, user2 / manager2 → user3, user4

## Архитектура

```
Controllers/   — REST: Auth, Action, Statistics, User
Services/      — бизнес-логика: Actions, Statistics, AppStatistic, ActionAnalysis, Archive
Entities/      — JPA-модели:
  Actions/     — Action (base, single-table inheritance) → Mouse, Keyboard, App
  Statistics/  — Statistics, AppStatistics
  UserTypes/   — Users (роли: ADMIN, MANAGER, USER)
Repositories.kt        — все Spring Data репозитории
AuthentificateService  — JWT (jjwt 0.11.5)
```

### Иерархия ролей

- **ADMIN** — видит всех менеджеров, может создавать пользователей любой роли
- **MANAGER** — видит только своих подчинённых (`admin_lookup` join table)
- **USER** — доступ только к своей статистике

### Сессии и метрики

- `POST /api/statistics/sessions/start` — открыть сессию
- `POST /api/statistics/sessions/end` — закрыть сессию, принимает `SessionMetricsDto { avgCpu, avgRam, avgGpu }`
- `StatisticCollector` (@Scheduled, каждую минуту) — автоматически завершает сессии по таймауту неактивности (5 мин)

## API

Все запросы (кроме логина) требуют заголовок `Authorization: Bearer <token>`.

| Метод | Endpoint | Описание |
|-------|----------|----------|
| POST | /auth/login | Логин → JWT |
| GET | /api/users/all | Список пользователей (фильтруется по роли caller'а) |
| GET | /api/users/owninfo | Текущий пользователь |
| POST | /api/users/add | Создать пользователя |
| POST | /api/statistics/sessions/start | Начать сессию |
| POST | /api/statistics/sessions/end | Завершить сессию (+ метрики CPU/RAM/GPU) |
| GET | /api/statistics/from/{userId} | Статистика пользователя (`?archived=true`) |
| POST | /api/actions/add | Отправить батч действий |
