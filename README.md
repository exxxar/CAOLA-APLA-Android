# CAOLA / APLA — Android demo client

Русскоязычный Android-проект на Kotlin. Каждый экран находится в отдельном Activity и отдельном Kotlin-файле.

## Визуальный интерфейс (обновление)
- Единая тёмная тема с графитово-синим фоном и мятным акцентом.
- Общая шапка CAOLA / APLA и нижняя навигация на пяти основных разделах.
- Главный экран со сводными метриками и выделенной рекомендацией CAOLA.
- Карточный каталог микроуроков с уровнем, длительностью и количеством заданий.
- Экран прогресса с нативным графиком активности и полосами освоения тем.
- Самостоятельные визуальные экраны CAOLA и APLA с оценками, объяснениями и демонстрационными действиями.

## Демо-функциональность
- Стартовый выбор демо-режима или основного режима.
- Главная панель с профилем, учебной целью и сводкой.
- Каталог микроуроков с проверкой ответов и сохранением результатов.
- Словарь с переводами, примерами, темами и статусами.
- Аудирование с Android TextToSpeech (если на устройстве установлен английский голосовой пакет).
- Диалоговая практика с выбором реплик.
- CAOLA: демонстрационные временные окна, оценка уместности, принятие/отклонение/откладывание.
- APLA: демонстрационные приоритеты учебных элементов и сценарии изменения состояния.
- Контекст: расписание, активность, тихие часы и переключение демонстрационных источников.
- Прогресс и история занятий.
- Настройки API и контекстных источников.
- Отладка: постоянный локальный журнал, перехват необработанных исключений, копирование и экспорт.
- Встроенный JSON с тестовыми данными; демо-режим работает без сети.
- Room: сущность и DAO для дальнейшей офлайн-очереди.
- Тематическая иконка CAOLA/APLA: SVG в assets и Android VectorDrawable/adaptive launcher icon.

## Сборка
Откройте проект в Android Studio с JDK 17, дождитесь синхронизации Gradle и выполните:
Build → Build Bundle(s) / APK(s) → Build APK(s).

## Что пока демонстрационное
- Основной режим пока не подключён к серверу: поле API сохраняется, но авторизация и HTTP-клиент будут следующим этапом.
- CAOLA/APLA представлены сценариями и демонстрационными оценками; исследовательские алгоритмы и калибровка требуют отдельной реализации.
- Переключатели контекстных источников имитируют согласия и не включают реальные системные датчики/календарь.
- Аудирование использует системный TTS для озвучивания примера, а не потоковую запись диктора.
- UI собран программно в Kotlin, каждый экран остаётся отдельной Activity. Это позволяет быстро стабилизировать демо без усложнения сборки XML-ресурсами.


## Сборка через GitHub Actions

В репозитории добавлен workflow `.github/workflows/android-build.yml`. Он запускается при push/pull request в `main`, `master`, `develop`, а также вручную через вкладку **Actions → Android CI build → Run workflow**. Workflow устанавливает JDK 17 и Gradle 8.9, выполняет `clean assembleDebug` и публикует APK как артефакт `CAOLA-APLA-debug-apk` на 14 дней. APK можно скачать на странице завершённого запуска workflow в разделе **Artifacts**. Это debug-сборка, не подписанный релиз для публикации в Google Play.

## Настройки реального сервера

На экране **Настройки** доступны: режим DEMO/PRODUCTION, базовый URL, префикс API (по умолчанию `/api/v1`), Bearer-токен, тайм-аут соединения и тайм-аут ответа. Токен сохраняется зашифрованным через Android Keystore. Для release-сборки серверный режим требует HTTPS. В debug-сборке разрешён HTTP только для loopback и частных IPv4-адресов локальной разработки.

## Сетевой слой и API-контракт (v5)

В Android добавлены Retrofit + OkHttp + Gson. Интерфейс `ApiService` содержит мобильные маршруты из раздела 5.3 ТЗ; `ApiRepository` предоставляет методы для экранов и ViewModel. `ApiClient` добавляет Bearer access token, `X-Request-ID`, идентификатор платформы, тайм-ауты и повтор соединения на уровне OkHttp. Access и refresh токены хранятся раздельно, в зашифрованном виде через Android Keystore. Для ранее сохранённого в v4 access token предусмотрена миграция.

В настройках доступен реальный сетевой тест `GET /api/v1/onboarding`. HTTP 401/403 трактуются как подтверждение доступности сервера при проблеме авторизации; 404 указывает на несовпадение маршрутов/версии API. Основной режим загружает серверные данные и не подменяет их встроенной демо-статистикой.

Полная таблица маршрутов, JSON-контракты, коды ошибок, idempotency и рекомендуемые административные расширения: `docs/API_CONTRACT.md`. Мобильные маршруты в документе соответствуют разделу 5.3 исходного ТЗ. Административные URL отдельно помечены как предлагаемые, поскольку исходное ТЗ определяет разделы и роли админки, но не закрепляет их пути.

### Ожидаемый формат dashboard

См. пример `GET /learning/dashboard` в `docs/API_CONTRACT.md`: корневые объекты `profile`, `metrics`, `recommendation`. При изменении API-контракта обновить этот пример и парсер `DashboardActivity` синхронно с backend.

### Важные ограничения текущей версии

- Реализован транспортный слой и полный интерфейс мобильных маршрутов, но серверный FastAPI проект в этот Android-архив не входит.
- Вход/обновление/выход сессии реализованы в `ApiRepository`; экран входа/регистрации добавлен. Access/refresh token сохраняются в Android Keystore; при необходимости их можно также задать вручную в настройках.
- Главная, каталог уроков, выполнение урока, CAOLA, APLA, прогресс и проверка соединения в Production используют соответствующие реальные API-маршруты. Аудирование, диалог, словарь, история и часть контекстных настроек пока остаются локальными/демонстрационными и будут подключаться отдельным проходом.
- При HTTP 401 OkHttp Authenticator пытается один раз обновить пару токенов через `/auth/refresh` и повторить исходный запрос; refresh token ротируется и сохраняется в Android Keystore. Если refresh недействителен, приложение показывает ошибку авторизации.
- Сервер обязан проверять права, владельца ресурса, согласия, идемпотентность и структуру входных данных независимо от клиента.

OpenAPI 3.1-описание маршрутов для импорта в Swagger Editor / генераторы документации находится в `docs/openapi.yaml`. Подробные примеры тел запросов и ответов — в `docs/API_CONTRACT.md`; рекомендуемая раскладка FastAPI — в `docs/FASTAPI_ROUTE_LAYOUT.md`.
### JVM target compatibility

The Android module compiles both Java and Kotlin bytecode for JVM 17. This matches the JDK 17 configured in GitHub Actions and prevents the Java 1.8 / Kotlin 17 target mismatch during KAPT stub generation.


## Локальный сервер по HTTP (debug)

В debug-сборке серверный режим поддерживает HTTP только для локальных адресов разработки: `localhost`, `127.0.0.1`, `10.x.x.x`, `172.16.x.x`–`172.31.x.x`, `192.168.x.x` и Android Emulator host alias `10.0.2.2`. Для публичных доменов по-прежнему требуется HTTPS. Разрешение cleartext-трафика включено только в `app/src/debug/AndroidManifest.xml`; в release-сборке оно не включается, а проверка URL требует HTTPS.

Примеры адресов (поле базового URL, без `/api/v1`):
- Android Emulator, Backend на компьютере: `http://10.0.2.2:8000`
- Физический телефон: `http://192.168.1.50:8000` (заменить IP на LAN IPv4 компьютера)
- Backend на том же Android-устройстве: `http://127.0.0.1:8000`

Телефон и компьютер должны находиться в одной локальной сети; сервер должен слушать не только `127.0.0.1`, но и LAN-интерфейс (`0.0.0.0`), а входящий порт должен быть разрешён брандмауэром. HTTP использовать только в доверенной тестовой сети.


### Диагностика авторизации (v0.1.3)

При неуспешном входе Debug-журнал показывает HTTP-код, фактический URL запроса `auth/login` и текст ответа сервера. Пароль и тело отправленного запроса в журнал не записываются. Это помогает отличить неверные учётные данные от подключения к другой базе данных или экземпляру Backend.

## Authentication screens (v0.1.4)

- `LoginActivity` contains only email and password fields.
- `RegisterActivity` is a separate screen with display name, email, password and password confirmation.
- Navigation between the screens is explicit; registration is no longer embedded in the login form.
- Authentication failures log the request URL and server response body, but never the submitted password.
- `X-App-Version` is populated from the actual Gradle `BuildConfig.VERSION_NAME`.

If the local administrator signs in successfully in Admin Desktop but receives HTTP 401 on Android, compare the exact Backend URL configured in Admin Desktop with Android's Base URL. The Android emulator address `http://10.0.2.2:8000` should reach the same host machine service as desktop `http://127.0.0.1:8000`, provided both clients use the same port and Backend process. The login route and JSON fields are the same in both clients (`POST /api/v1/auth/login`, `email`, `password`). A 401 from this route means the Backend did not find the account or did not verify the supplied password; a role restriction would be a separate authorization check after login.


## Версия 0.1.5 (Android v5.3.6)

- Исправлен разбор `GET /api/v1/progress/summary`: `weekly_minutes` обрабатывается как массив объектов с полем `minutes`, а темы — по полям `title` и `mastery` согласно контракту Backend.
- Добавлена совместимость с прежним форматом массива чисел и альтернативными полями `name` / `mastery_percent`.
- Числовые и строковые значения обрабатываются безопасно при `null` или отсутствии поля.
- При первом открытии настроек сохраняется адрес по умолчанию, чтобы переход ко входу не приводил к пустому API endpoint.
