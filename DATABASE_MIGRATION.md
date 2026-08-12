# Database migration report

## Source and safety boundary

The migration was performed only on local disposable PostgreSQL databases.
The source file was never edited:

- source: `E:\PROJECTS\database\backup_20260812_131434.sql`
- source SHA-256: `B43CC11E89E251A88B7D76544D95BF6552B2909D2FBABD187F08731EBF59415F`
- source PostgreSQL version: 15.17
- verification PostgreSQL version: 16.14, local port 55432

The generated dump contains real source data and must not be committed or
shared through a public channel. `.gitignore` excludes it by default.

## What changed

`database/legacy_to_current.sql` is transactional and repeatable. It:

1. Creates `school` and `curator`.
2. Preserves the original text in `pupil.school` and fills `pupil.school_id`.
   School names are grouped by trimmed, case-insensitive value; the supplied
   data contains two case variants of one school name.
3. Adds `CURATOR`, keeping all historical roles and assignments.
4. Adds `is_active` to both test-type tables.
5. Adds the current calculation fields and domain foreign keys to `prediction`.
6. Drops `invitation` only after a guard proves that it contains zero rows.

No account, pupil, specialist, test, profession, prediction, company or
historical role assignment is deleted. Two pupil patronymics are null in the
source, so the Java mapping correctly keeps that field nullable.

## Verified counts

| Entity | Before | After migration | After clean restore |
|---|---:|---:|---:|
| account | 654 | 654 | 654 |
| pupil | 457 | 457 | 457 |
| specialist | 127 | 127 | 127 |
| profession | 18 | 18 | 18 |
| psych_test | 1,610 | 1,610 | 1,610 |
| psych_test_param | 5,744 | 5,744 | 5,744 |
| psych_param | 5,744 | 5,744 | 5,744 |
| prediction | 128 | 128 | 128 |
| school | — | 38 | 38 |
| curator | — | 0 | 0 |

All 382 pupils with nonblank legacy school text received a structured school
link. The validation found no unmapped school values or broken checked foreign
keys. Three ADMIN accounts remain. Identity/serial sequences are not behind
their table maxima.

The validator also reports, without rewriting history, one legacy test without
an owner/type, 15 parameter rows without a name link, and five null parameter
values. All non-null test/parameter relationships are valid. Python maps those
five null values to the same configured `default_value` used for a missing
feature; a regression test covers this behavior.

Historical role counts are intentionally preserved: HR has five accounts and
EMPLOYEE has three; those roles are not active product roles in Java. The dump
contains no APPLICANT role row and the invitation table contains zero rows.

## Reproducible procedure

Use PostgreSQL client tools and provide credentials outside the command history,
for example through the process-local `PGPASSWORD` environment variable.

```powershell
createdb -h 127.0.0.1 -p 5432 -U postgres career_legacy
psql -h 127.0.0.1 -p 5432 -U postgres -d career_legacy `
  -v ON_ERROR_STOP=1 -f E:\PROJECTS\database\backup_20260812_131434.sql

psql -h 127.0.0.1 -p 5432 -U postgres -d career_legacy `
  -v ON_ERROR_STOP=1 -f database\legacy_to_current.sql
psql -h 127.0.0.1 -p 5432 -U postgres -d career_legacy `
  -v ON_ERROR_STOP=1 -f database\validate_migration.sql

pg_dump -h 127.0.0.1 -p 5432 -U postgres -d career_legacy `
  --format=plain --encoding=UTF8 --no-owner --no-privileges `
  --file=database\career_guidance_ready.sql

createdb -h 127.0.0.1 -p 5432 -U postgres career_ready_verify
psql -h 127.0.0.1 -p 5432 -U postgres -d career_ready_verify `
  -v ON_ERROR_STOP=1 -f database\career_guidance_ready.sql
psql -h 127.0.0.1 -p 5432 -U postgres -d career_ready_verify `
  -v ON_ERROR_STOP=1 -f database\validate_migration.sql
```

Generated artifact:

- `database/career_guidance_ready.sql`
- SHA-256: `E8DCA1306C3D73D0F009A7E7C9C10FAF9833862D641EE24B342B0C85542B4D4B`
- size: 420,567 bytes

## Application verification

The backend started against the second cleanly restored database with
`spring.jpa.hibernate.ddl-auto=validate`; it did not rely on Hibernate schema
generation. The protected specialist dataset returned 103 usable profiles.
Python then built the full cluster artifact set from those profiles and
reported a fresh persistent state. A real backend `POST /api/predictions/predict`
request completed successfully, stored its result, and `GET /latest` returned
the matching record. Only the second disposable database was changed with a
temporary integration account/password and the new smoke-test prediction.

Verification commands completed:

- backend: `gradlew.bat test` — passed;
- Python: `pytest -q` — 16 passed;
- frontend: `npm.cmd test -- --runInBand` — 6 passed;
- frontend: `npm.cmd run build` — passed;
- migration validation — passed before dump and after clean restore;
- full Backend → Python → Backend persistence smoke — passed.

The frontend build reports only Vite's existing large-chunk warning. External
uploaded files referenced by database paths were not part of the SQL backup, so
their byte-level existence and hashes cannot be verified from this artifact.
