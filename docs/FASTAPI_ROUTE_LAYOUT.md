# Рекомендуемая структура FastAPI backend

Маршруты ниже подключаются к единому префиксу `/api/v1`. Это карта реализации по контракту `API_CONTRACT.md`, а не готовый серверный исходный код.

```text
backend/
  app/
    main.py
    api/
      v1/
        router.py
        routes/
          auth.py
          profile.py
          onboarding.py
          learning.py
          context.py
          lessons.py
          progress.py
          review.py
          sync.py
          admin/
            dashboard.py
            users.py
            content.py
            lesson_templates.py
            ai_prompts.py
            algorithms.py
            reports.py
            audit.py
            system.py
    core/
      config.py
      security.py
      permissions.py
      errors.py
    schemas/
    models/
    repositories/
    services/
      caola/
      apla/
      content/
      llm/
      synchronization/
    db/
      session.py
      migrations/
```

## Подключение роутеров

```python
from fastapi import APIRouter
from app.api.v1.routes import auth, profile, onboarding, learning, context, lessons, progress, review, sync

api_router = APIRouter()
api_router.include_router(auth.router, prefix="/auth", tags=["auth"])
api_router.include_router(profile.router, prefix="/profile", tags=["profile"])
api_router.include_router(onboarding.router, prefix="/onboarding", tags=["onboarding"])
api_router.include_router(learning.router, prefix="/learning", tags=["learning"])
api_router.include_router(context.router, prefix="/context", tags=["context"])
api_router.include_router(lessons.router, prefix="/lessons", tags=["lessons"])
api_router.include_router(progress.router, prefix="/progress", tags=["progress"])
api_router.include_router(review.router, prefix="/review", tags=["review"])
api_router.include_router(sync.router, prefix="/sync", tags=["sync"])
```

В `main.py`:

```python
from fastapi import FastAPI
from app.api.v1.router import api_router

app = FastAPI(title="CAOLA / APLA API", version="1.0.0")
app.include_router(api_router, prefix="/api/v1")
```

Для `profile` и `onboarding` при таком подключении рекомендуется использовать `APIRouter()` и объявлять `@router.get("")`, `@router.patch("")`, чтобы итоговые URL были `/profile` и `/onboarding`, как в мобильном контракте.

## Принципы ответственности

- `auth.py`: регистрация, login, refresh rotation, logout, выпуск access/refresh токенов.
- `profile.py`: только профиль текущего пользователя; идентификатор пользователя брать из access-token dependency, не из тела запроса.
- `onboarding.py`: справочники и первичная настройка, сохранение версий согласий.
- `learning.py`: dashboard, trajectory, CAOLA recommendation, запись пользовательского решения.
- `context.py`: проверка согласий, нормализация и ограничение срока хранения агрегированных событий.
- `lessons.py`: создание сессии, выдача упражнений без эталонных ответов, серверная проверка ответов, обновление знаний и завершение.
- `progress.py` и `review.py`: агрегаты прогресса и APLA очередь повторения.
- `sync.py`: приём офлайн-событий, дедупликация по event_id / Idempotency-Key, ответ accepted/duplicates/rejected.

## Обязательные backend-зависимости

1. Dependency аутентификации для всех приватных маршрутов.
2. Проверка владельца каждого `lesson_id`, `recommendation_id` и пользовательского объекта.
3. Сервис CAOLA отдельно от HTTP-роутера; сервис APLA отдельно от HTTP-роутера.
4. Сервис проверки упражнений и обновления KnowledgeState — серверный, транзакционный.
5. LLM adapter вызывается только сервером; API-ключи берутся из секретов окружения.
6. Общий обработчик ошибок, request-id middleware, ограничение частоты и структурированные логи.
7. Миграции PostgreSQL и аудит изменений конфигураций CAOLA/APLA.

## Важное уточнение

Исходное ТЗ задаёт функциональные модули и точные пользовательские маршруты, но не определяет конкретную файловую структуру FastAPI. Структура выше — рекомендация для реализации, которую можно адаптировать к существующему backend.
