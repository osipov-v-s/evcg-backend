# Изменения backend профориентационной системы

## Назначение документа

Документ описывает актуальную архитектуру Java backend после выделения
профориентационной версии продукта. Его стоит прочитать перед изменением API,
ролей, школы/куратора, Prediction или структуры PostgreSQL.

## Роль backend в системе

Backend — единственная доверенная граница между frontend, PostgreSQL и Python
Prediction Service.

```text
React frontend
    -> REST /api/** + JWT
Spring Boot backend
    -> JPA/PostgreSQL
    -> HTTP POST Python /predict
Python возвращает расчёт
Spring проверяет доменные ссылки и сохраняет Prediction
```

Frontend никогда не должен обращаться к Python напрямую.

## Основная структура исходников

| Пакет | Ответственность |
|---|---|
| `controllers` | HTTP-контракты, роли и преобразование результата в ResponseEntity |
| `database/entities` | JPA-модель текущего домена |
| `database/repositories` | Spring Data запросы, пагинация и выборки |
| `database/services` | Бизнес-правила, область доступа и транзакции |
| `dto` | Внешние request/response модели |
| `configuration` | CORS, interceptor, RestTemplate и настройки |
| `services/jwt` | Создание и проверка JWT |
| `exceptions` | Безопасные структурированные ответы об ошибках |
| `src/main/resources/db/migration` | Версионированные миграции для будущего Flyway-процесса |
| `database` | Ручная миграция legacy-базы, проверка и полный готовый дамп |

## Актуальный домен

### Пользователи и роли

Активные роли:

- `ADMIN`;
- `PUPIL`;
- `SPECIALIST`;
- `CURATOR`.

Исторические роли читаются из старой базы, но не дают доступ к продукту:
`HR`, `APPLICANT`, `TEACHER`, `DIRECTOR`, `EMPLOYEE`.

`RoleEnum.isActive()` — единая проверка активности роли. Login отклоняет
аккаунт, у которого нет ни одной активной роли. В JWT записываются только
активные роли.

### Образовательные организации

Добавлены:

- `School` — структурированная образовательная организация;
- `Curator` — профиль с обязательными связями `Account` и `School`;
- `Pupil.schoolId` — новая структурированная связь ученика со школой.

Старое строковое поле `Pupil.school` оставлено для сохранения мигрированных
данных. Новая логика должна использовать `schoolId`/`School`.

Куратор не передаёт школу в запросах. `CuratorService` получает её по
`accountId`, извлечённому из JWT. Список учеников, результаты и сброс пароля
ограничиваются этой школой на сервере.

### Специалисты и компании

`Specialist` сохраняет профессию, тесты и необязательную информационную связь
с `Company`. Контур HR/Employee/Invitation удалён из Java-кода. Компания больше
не является контейнером управления сотрудниками.

### Тесты

В `psych_test_type` и `vr_test_type` добавлен `is_active`. Отключение типа
скрывает его из активного пользовательского сценария, но не удаляет старые
результаты.

### Prediction

`Prediction` теперь сохраняет:

- ученика;
- тип расчёта;
- номер кластера;
- расстояние до ближайшего специалиста;
- категорию уверенности;
- предсказанную профессию;
- ближайшего специалиста;
- время создания.

Старые записи могут иметь только legacy-поля `file_path`, `prediction_type_id`
и `pupil_id`. Новые поля миграции nullable именно для сохранения истории.

## Безопасность запросов

В проекте используются два уровня прикладной проверки:

1. `JWTValidationInterceptor` валидирует заголовок `Authorization`, извлекает
   `accountId` из subject JWT и добавляет его в request attributes.
2. `@HasRole` + `RoleAspect` проверяет роль аккаунта для конкретного метода.

Важно: Spring Security сейчас разрешает `/api/**`, поэтому защищённый endpoint
должен одновременно:

- быть включён в patterns `WebConfig.addInterceptors()`;
- иметь корректную аннотацию `@HasRole`.

Если забыть один из этих шагов, получится либо недоступный endpoint, либо
ошибка в границе авторизации. При добавлении API обязательно проверять 401,
403 и доступ каждой активной роли.

Frontend-параметры `accountId`, `pupilId` или `schoolId` нельзя использовать как
доказательство владения. Область данных определяется сервером по JWT.

## Основные API-модули

| Контроллер | Назначение |
|---|---|
| `AuthController` | Регистрация, login, активные роли и смена пароля |
| `PupilController` | Профиль ученика, списки, фильтры, школа и результаты |
| `SpecialistController` | Профиль, списки, профессии и данные для кластеризации |
| `SchoolController` | CRUD школ и создание/просмотр кураторов |
| `CuratorController` | Профиль куратора и действия только в своей школе |
| `PsychTestsController` | Сохранение и чтение психологических тестов |
| `VRTestController` | VR-тесты и управление активностью типов |
| `PredictionController` | Расчёт и чтение Prediction |
| `SimulationController` | Симуляции и внешние файлы/метаданные |
| `ComparisonController` | Сессии сравнения и связанные внешние данные |
| `CompanyController` | Информационный справочник компаний |

Списки учеников и специалистов используют `Page` и серверные фильтры. Не стоит
возвращать полный набор данных ради клиентской фильтрации.

## Поток расчёта Prediction

```text
POST /api/predictions/predict
  -> JWTValidationInterceptor извлекает accountId
  -> @HasRole(PUPIL)
  -> PredictionService получает Pupil из Account
  -> PsychTestService собирает последние тесты
  -> POST ${prediction.service.url}
  -> backend проверяет pupilId, cluster, distance и обязательные строки
  -> проверяет существование Profession и Specialist
  -> сохраняет Prediction
  -> возвращает сохранённый результат frontend
```

Frontend не передаёт `pupilId`. Python-ответ с другим `pupilId`, неизвестной
профессией, неизвестным специалистом, отрицательным/нечисловым расстоянием или
пустой категорией отклоняется.

Структурированные коды ошибок:

| Код | Смысл |
|---|---|
| `PREDICTION_ACCOUNT_INVALID` | Аккаунт не связан с Pupil |
| `PREDICTION_REQUEST_REJECTED` | Python отклонил входные тесты |
| `PREDICTION_TIMEOUT` | Истёк read timeout |
| `PREDICTION_SERVICE_UNAVAILABLE` | Сервис недоступен |
| `PREDICTION_SERVICE_ERROR` | Python вернул server error |
| `PREDICTION_INVALID_RESPONSE` | Ответ не прошёл доменную проверку |

`PredictionExceptionHandler` возвращает только безопасное сообщение и не
публикует stack trace или внутренние адреса.

## Данные для Python

`GET /api/specialists/reference-data` — защищённый ADMIN endpoint, которым
пользуется Python-сервис при построении кластеров. Название endpoint сохранено
ради внешней совместимости. Внутри нового Python-кода термин `Reference` не
используется.

Выборка должна возвращать только пригодных к расчёту специалистов с ID,
профессией и последними результатами тестов. Изменение этого DTO необходимо
согласовывать с `models.Specialist` Python-сервиса.

## Миграция PostgreSQL

Backend не меняет структуру базы при запуске:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.generate-ddl=false
spring.flyway.enabled=false
```

То есть схема должна быть подготовлена до старта приложения.

```text
database/
  legacy_to_current.sql       однократная миграция восстановленной legacy-БД
  validate_migration.sql      fail-fast проверки и безопасные counts
  career_guidance_ready.sql   полный готовый дамп с реальными данными, Git ignored
  manual/                     только явно подтверждаемые legacy-операции

DATABASE_MIGRATION.md         воспроизводимая инструкция и результаты
DATABASE_MIGRATION_PLAN.md    карта старых и новых сущностей
```

Для нового окружения рекомендуется восстановить `career_guidance_ready.sql`,
выполнить `validate_migration.sql` и только затем запускать backend.

Для копии старой базы:

```text
restore исходного backup
  -> database/legacy_to_current.sql
  -> database/validate_migration.sql
  -> backend startup с ddl-auto=validate
```

Не переносить `legacy_to_current.sql` в автоматический startup hook. Скрипт
предназначен для контролируемой подготовки конкретной legacy-базы. Будущие
небольшие изменения следует оформлять отдельными версионированными Flyway
миграциями после согласования baseline существующей схемы.

## Конфигурация

Основные переменные окружения приведены в `.env.example`:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
APP_SECRET_KEY
PREDICTION_SERVICE_URL
PREDICTION_SERVICE_CONNECT_TIMEOUT_MS
PREDICTION_SERVICE_READ_TIMEOUT_MS
```

Также deployment должен задать доступные каталоги `public.folder` и
`public.folder.simulations`. Секреты не добавляются в Git и не записываются в
документацию с реальными значениями.

## Проверка

```text
gradlew.bat test
```

Дополнительно для интеграционного окружения:

1. Восстановить готовый дамп в пустую БД.
2. Выполнить `database/validate_migration.sql`.
3. Запустить backend с `ddl-auto=validate`.
4. Проверить login и `/api/auth/account-roles`.
5. Проверить ADMIN endpoint данных специалистов.
6. Запустить Python и выполнить PUPIL prediction.
7. Проверить, что `/api/predictions/latest` возвращает сохранённую запись.

Полный сценарий Backend → Python → Backend persistence уже проходил на
изолированной тестовой БД.

## Правила дальнейших изменений

1. Контроллер принимает/возвращает DTO, а не JPA entity.
2. Проверка области доступа выполняется в service, а не во frontend.
3. Новый защищённый endpoint получает interceptor pattern и `@HasRole`.
4. CURATOR никогда не выбирает школу запроса самостоятельно.
5. Исторические роли остаются читаемыми, но не становятся активными.
6. Изменение Prediction DTO синхронизируется с Python и frontend.
7. Изменение схемы оформляется SQL-миграцией и отдельной валидацией.
8. Реальные дампы, пароли, JWT и персональные строки не коммитятся.
9. Перед передачей изменений запускаются тесты и `git diff --check`.

