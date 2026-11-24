# CAOLA / APLA — REST API contract v1

Документ фиксирует контракт мобильного клиента Android и FastAPI backend. Базовый перечень пользовательских маршрутов сохранён по разделу 5.3 исходного ТЗ. Поля JSON ниже — проектная детализация, необходимая для согласованной реализации клиента и сервера; до фиксации схемы PostgreSQL допускается уточнение названий отдельных полей без изменения назначения маршрутов.

## 1. Общие правила

- Base URL: `https://<host>/api/v1/`.
- Формат: JSON, кодировка UTF-8.
- Все маршруты, кроме регистрации и входа, требуют `Authorization: Bearer <access_token>`.
- Клиент передаёт `X-Request-ID` для трассировки. Для повторно отправляемых операций передаётся `Idempotency-Key`.
- Внешний доступ — только HTTPS. Секреты LLM и административные ключи никогда не передаются в Android.
- Даты и время — ISO 8601 с часовым поясом; сервер хранит время в UTC, профиль содержит IANA timezone пользователя.
- Серверная логика является источником истины для проверки ответов, CAOLA/APLA и состояния знаний. LLM не изменяет KnowledgeState напрямую.

## 2. Формат ошибок

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Проверьте переданные поля",
    "details": [{"field": "email", "reason": "invalid_format"}],
    "request_id": "a7c4..."
  }
}
```

Рекомендуемые коды: `400` некорректный запрос, `401` отсутствующая/недействительная сессия, `403` недостаточно прав, `404` объект не найден, `409` конфликт версии/события, `422` ошибка валидации, `429` превышен лимит, `5xx` внутренняя ошибка. Клиент не должен отображать пользователю stack trace или внутренние сведения.

## 3. Авторизация

| Метод | Маршрут | Назначение | Доступ |
|---|---|---|---|
| POST | `/auth/register` | Регистрация | Публичный |
| POST | `/auth/login` | Вход | Публичный |
| POST | `/auth/refresh` | Обновление пары токенов | Refresh token |
| POST | `/auth/logout` | Завершение текущей сессии | Access token |

Примеры запросов:

```json
{"email":"learner@example.com","password":"strong-password","display_name":"Alex"}
```

```json
{"email":"learner@example.com","password":"strong-password"}
```

Успешный ответ входа/регистрации:

```json
{
  "access_token":"<short-lived-token>",
  "refresh_token":"<rotating-refresh-token>",
  "token_type":"bearer",
  "expires_in":900,
  "user":{"id":"usr_123","display_name":"Alex","status":"active"}
}
```

Refresh: `{"refresh_token":"<refresh-token>"}`. Сервер должен ротировать refresh token и отзывать использованный. Logout отзывает текущую серверную сессию. Пароли хранятся только в стойком хеше с солью.

## 4. Профиль и onboarding

| Метод | Маршрут | Назначение |
|---|---|---|
| GET | `/profile` | Получить профиль |
| PATCH | `/profile` | Изменить профиль |
| GET | `/onboarding` | Получить справочники и конфигурацию первичной настройки |
| POST | `/onboarding/complete` | Сохранить первичную настройку |

Рекомендуемые поля профиля: `native_language`, `target_language`, `timezone`, `current_level`, `target_level`, `goals[]`, `daily_minutes`, `preferred_formats[]`, `quiet_hours`, `onboarding_completed`. `PATCH` принимает только изменяемые поля.

`GET /onboarding` возвращает доступные языки, уровни CEFR, цели, форматы упражнений и значения по умолчанию. `POST /onboarding/complete` принимает выбранные значения и версии согласий; ответ содержит актуальный профиль и начальную траекторию.

## 5. Главная, траектория и CAOLA

| Метод | Маршрут | Назначение |
|---|---|---|
| GET | `/learning/dashboard` | Данные главного экрана |
| GET | `/learning/trajectory` | Индивидуальная траектория и доступные микроуроки |
| GET | `/learning/recommendation` | Текущая рекомендация CAOLA |
| POST | `/learning/recommendation/{id}/decision` | Принять, отклонить или отложить рекомендацию |

Пример ответа `GET /learning/dashboard`:

```json
{
  "profile":{"display_name":"Alex","goal":"Разговорная практика","target_language":"English","level":"A2"},
  "metrics":{"streak_days":3,"weekly_minutes":95,"mastered_items":120},
  "recommendation":{"id":"rec_42","title":"Повседневные диалоги","duration_minutes":8,"due_reviews":4,"reason":"Есть короткое доступное окно"}
}
```

`GET /learning/trajectory` возвращает `items[]` или `lessons[]`; каждый элемент содержит `id`, `title`, `description`, `level`, `duration_minutes`, `topic`, `status` и `exercises_count`.

Рекомендация CAOLA содержит `id`, `title`, `window_start`, `window_end`, `duration_minutes`, `format`, `score` (0..1), `confidence` (0..1), `interruption_risk` (0..1), `reason` и `expires_at`. Сервер не запускает занятие автоматически.

Решение пользователя:

```json
{"decision":"accept"}
```

Допустимые значения: `accept`, `reject`, `postpone`. Для `postpone` дополнительно передаётся `postpone_until`. Ответ возвращает сохранённое решение и обновлённый статус рекомендации.

## 6. Контекст и согласия

| Метод | Маршрут | Назначение |
|---|---|---|
| POST | `/context/events` | Передать разрешённые агрегированные контекстные события |
| GET | `/context/settings` | Получить настройки источников |
| PATCH | `/context/settings` | Изменить разрешения и настройки источников |

Пример настроек:

```json
{
  "sources":[
    {"source":"schedule","enabled":true,"processing_mode":"aggregated"},
    {"source":"activity","enabled":false,"processing_mode":"disabled"},
    {"source":"calendar","enabled":false,"processing_mode":"disabled"}
  ],
  "quiet_hours":{"from":"22:00","to":"08:00"},
  "recommendations_enabled":true
}
```

Событие содержит `event_id`, `source`, `observed_at`, `aggregate`, `quality`, `consent_version`, `expires_at`. Не передавать необработанные аудио/видео по умолчанию. Каждый источник имеет независимое согласие и может быть отключён пользователем. Отсутствие движения/речи нельзя трактовать как доказательство свободного времени.

## 7. Уроки и ответы

| Метод | Маршрут | Назначение |
|---|---|---|
| POST | `/lessons/start` | Создать учебную сессию |
| GET | `/lessons/{id}` | Получить содержание занятия |
| POST | `/lessons/{id}/answers` | Передать ответ на упражнение |
| POST | `/lessons/{id}/complete` | Завершить занятие |

`POST /lessons/start` принимает `trajectory_item_id` и/или `recommendation_id`, а также подтверждённый пользователем `format`. Ответ содержит `lesson_id`, `status`, `started_at` и краткие метаданные занятия.

`GET /lessons/{id}` возвращает `id`, `title`, `description`, `duration_minutes`, `exercises[]`. Упражнение: `id`, `type`, `prompt`, `options[]` (объекты `{id, text}`, если применимо), `media`, `position`. Эталонные ответы и закрытые критерии проверки клиенту не передаются.

Ответ:

```json
{"event_id":"evt_001","exercise_id":"ex_12","answer":{"selected_option_id":"opt_b"},"elapsed_ms":12400}
```

Сервер отвечает `correct`, `score`, `feedback`, `explanation` и при необходимости `next_action`. Правильность и числовое обновление модели знаний рассчитываются сервером.

Завершение:

```json
{"event_id":"evt_002","elapsed_seconds":510,"answered_count":8}
```

Ответ содержит `lesson_status`, `score`, `correct_count`, `total_count`, `knowledge_updates[]`, `review_scheduled[]`.

Для POST-запросов ответов/завершения клиент передаёт уникальный `Idempotency-Key`. Сервер сохраняет результат обработки ключа и при повторе возвращает первоначальный результат без повторного изменения прогресса.

## 8. Прогресс и APLA

| Метод | Маршрут | Назначение |
|---|---|---|
| GET | `/progress/summary` | Сводка обучения |
| GET | `/progress/history` | История занятий и результатов |
| GET | `/review/queue` | Очередь повторения APLA |

`GET /progress/summary` возвращает `completion_percent`, `lessons_completed`, `accuracy_percent`, `weekly_minutes[]`, `topics[]` и `updated_at`. История поддерживает `page`, `page_size`, `from`, `to`; ответ содержит `items[]`, `page`, `page_size`, `total`.

Очередь APLA возвращает `items[]`: `learning_item_id`, `label`, `priority_score` (0..1), `mastery` (0..1), `recall_probability` (0..1), `due_at`, `reason`, `recommended_exercise_type`. Клиент показывает приоритет, но не пересчитывает серверное состояние знаний самостоятельно.

## 9. Офлайн-синхронизация

| Метод | Маршрут | Назначение |
|---|---|---|
| POST | `/sync` | Передать накопленные локальные события |

Пример:

```json
{
  "batch_id":"batch_2026_0001",
  "client_id":"installation_uuid",
  "events":[
    {"event_id":"evt_001","event_type":"answer_submitted","occurred_at":"2026-11-24T12:00:00Z","payload":{"lesson_id":"les_1","exercise_id":"ex_1","answer":{"text":"hello"}}}
  ]
}
```

Ответ: `accepted[]`, `duplicates[]`, `rejected[]`, `server_time`. Идемпотентность обеспечивается по `batch_id`/`Idempotency-Key` и уникальному `event_id`. Сервер возвращает причину отклонения события; клиент не удаляет локальную запись до подтверждения.

## 10. Административный API — рекомендуемое расширение

Исходное ТЗ определяет роли и разделы админ-панели, но не фиксирует точные URL этих операций. Ниже — предлагаемый неймспейс; это не часть исходного перечня мобильных маршрутов и подлежит отдельному согласованию.

| Метод | Предлагаемый маршрут | Роли |
|---|---|---|
| GET | `/admin/dashboard` | Суперадминистратор, аналитик |
| GET/POST | `/admin/users` | Суперадминистратор, оператор поддержки |
| GET/PATCH | `/admin/users/{id}` | Суперадминистратор, оператор поддержки |
| GET/POST/PATCH | `/admin/content/languages` | Суперадминистратор, администратор контента |
| GET/POST/PATCH | `/admin/content/topics` | Суперадминистратор, администратор контента |
| GET/POST/PATCH | `/admin/content/items` | Суперадминистратор, администратор контента |
| GET/POST/PATCH | `/admin/content/exercises` | Суперадминистратор, администратор контента |
| GET/POST/PATCH | `/admin/lesson-templates` | Суперадминистратор, администратор контента |
| GET/POST/PATCH | `/admin/ai/prompts` | Суперадминистратор |
| GET/POST/PATCH | `/admin/algorithms/caola/configurations` | Суперадминистратор |
| GET/POST/PATCH | `/admin/algorithms/apla/configurations` | Суперадминистратор |
| GET | `/admin/reports/learning` | Суперадминистратор, аналитик |
| GET | `/admin/audit` | Суперадминистратор |
| GET/PATCH | `/admin/system/integrations` | Суперадминистратор |

Все административные маршруты требуют RBAC, аудит критичных изменений, версионирование конфигураций и маскирование секретов. Аналитик получает агрегированные данные без лишних персональных сведений.

## 11. Соответствие экранов Android маршрутам

| Экран | Основные запросы |
|---|---|
| Первичная настройка | `GET /onboarding`, `POST /onboarding/complete` |
| Настройки профиля | `GET/PATCH /profile` |
| Главная | `GET /learning/dashboard`, `GET /learning/recommendation` |
| Обучение | `GET /learning/trajectory`, `POST /lessons/start`, `GET /lessons/{id}`, `POST /lessons/{id}/answers`, `POST /lessons/{id}/complete` |
| CAOLA | `GET /learning/recommendation`, `POST /learning/recommendation/{id}/decision`, `GET/PATCH /context/settings`, `POST /context/events` |
| APLA | `GET /review/queue`, `GET /learning/trajectory` |
| Прогресс | `GET /progress/summary`, `GET /progress/history` |
| Синхронизация | `POST /sync` |
