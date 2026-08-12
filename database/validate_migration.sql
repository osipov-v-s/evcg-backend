-- Fail-fast checks for a database migrated by legacy_to_current.sql.
-- The result sets contain only counts and identifiers, never personal fields.

\set ON_ERROR_STOP on

DO $validation$
DECLARE
    missing_tables TEXT;
    admin_count BIGINT;
    broken_count BIGINT;
    table_name TEXT;
    sequence_name TEXT;
    maximum_id BIGINT;
    sequence_value BIGINT;
BEGIN
    SELECT STRING_AGG(required.name, ', ' ORDER BY required.name)
    INTO missing_tables
    FROM (VALUES
        ('account'), ('account_roles'), ('role'), ('pupil'), ('specialist'),
        ('profession'), ('psych_test'), ('psych_test_param'), ('psych_param'),
        ('prediction'), ('school'), ('curator')
    ) AS required(name)
    WHERE TO_REGCLASS('public.' || required.name) IS NULL;

    IF missing_tables IS NOT NULL THEN
        RAISE EXCEPTION 'Missing required table(s): %', missing_tables;
    END IF;

    IF TO_REGCLASS('public.invitation') IS NOT NULL THEN
        RAISE EXCEPTION 'Legacy table invitation still exists';
    END IF;

    SELECT COUNT(DISTINCT ar.account_id)
    INTO admin_count
    FROM account_roles ar
    JOIN role r ON r.id = ar.role_id
    WHERE r.name = 'ADMIN';

    IF admin_count = 0 THEN
        RAISE EXCEPTION 'No ADMIN account remains after migration';
    END IF;

    SELECT COUNT(*) INTO broken_count
    FROM pupil p LEFT JOIN account a ON a.id = p.account_id
    WHERE p.account_id IS NOT NULL AND a.id IS NULL;
    IF broken_count <> 0 THEN
        RAISE EXCEPTION 'Broken pupil -> account links: %', broken_count;
    END IF;

    SELECT COUNT(*) INTO broken_count
    FROM specialist s LEFT JOIN account a ON a.id = s.account_id
    WHERE s.account_id IS NOT NULL AND a.id IS NULL;
    IF broken_count <> 0 THEN
        RAISE EXCEPTION 'Broken specialist -> account links: %', broken_count;
    END IF;

    SELECT COUNT(*) INTO broken_count
    FROM specialist s LEFT JOIN profession p ON p.id = s.profession_id
    WHERE s.profession_id IS NOT NULL AND p.id IS NULL;
    IF broken_count <> 0 THEN
        RAISE EXCEPTION 'Broken specialist -> profession links: %', broken_count;
    END IF;

    SELECT COUNT(*) INTO broken_count
    FROM prediction p LEFT JOIN pupil u ON u.id = p.pupil_id
    WHERE p.pupil_id IS NOT NULL AND u.id IS NULL;
    IF broken_count <> 0 THEN
        RAISE EXCEPTION 'Broken prediction -> pupil links: %', broken_count;
    END IF;

    SELECT COUNT(*) INTO broken_count
    FROM prediction p
    LEFT JOIN profession f ON f.id = p.predicted_profession_id
    WHERE p.predicted_profession_id IS NOT NULL AND f.id IS NULL;
    IF broken_count <> 0 THEN
        RAISE EXCEPTION 'Broken prediction -> predicted profession links: %', broken_count;
    END IF;

    SELECT COUNT(*) INTO broken_count
    FROM prediction p
    LEFT JOIN specialist s ON s.id = p.nearest_specialist_id
    WHERE p.nearest_specialist_id IS NOT NULL AND s.id IS NULL;
    IF broken_count <> 0 THEN
        RAISE EXCEPTION 'Broken prediction -> nearest specialist links: %', broken_count;
    END IF;

    SELECT COUNT(*) INTO broken_count
    FROM pupil p LEFT JOIN school s ON s.id = p.school_id
    WHERE p.school_id IS NOT NULL AND s.id IS NULL;
    IF broken_count <> 0 THEN
        RAISE EXCEPTION 'Broken pupil -> school links: %', broken_count;
    END IF;

    SELECT COUNT(*) INTO broken_count
    FROM psych_test t LEFT JOIN pupil p ON p.id = t.pupil_id
    WHERE t.pupil_id IS NOT NULL AND p.id IS NULL;
    IF broken_count <> 0 THEN
        RAISE EXCEPTION 'Broken psych_test -> pupil links: %', broken_count;
    END IF;

    SELECT COUNT(*) INTO broken_count
    FROM psych_test t LEFT JOIN specialist s ON s.id = t.specialist_id
    WHERE t.specialist_id IS NOT NULL AND s.id IS NULL;
    IF broken_count <> 0 THEN
        RAISE EXCEPTION 'Broken psych_test -> specialist links: %', broken_count;
    END IF;

    SELECT COUNT(*) INTO broken_count
    FROM psych_test_param link
    LEFT JOIN psych_test t ON t.id = link.psych_test_id
    LEFT JOIN psych_param p ON p.id = link.psych_param_id
    WHERE t.id IS NULL OR p.id IS NULL;
    IF broken_count <> 0 THEN
        RAISE EXCEPTION 'Broken psych_test_param links: %', broken_count;
    END IF;

    SELECT COUNT(*) INTO broken_count
    FROM pupil
    WHERE NULLIF(TRIM(school), '') IS NOT NULL AND school_id IS NULL;
    IF broken_count <> 0 THEN
        RAISE EXCEPTION 'Legacy school values not mapped: %', broken_count;
    END IF;

    IF EXISTS (
        SELECT 1 FROM account GROUP BY LOWER(email) HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate account emails after case normalization';
    END IF;

    IF EXISTS (
        SELECT 1 FROM account_roles GROUP BY account_id, role_id HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate account role assignments';
    END IF;

    IF EXISTS (
        SELECT 1 FROM role WHERE name = 'CURATOR' GROUP BY name HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate CURATOR roles';
    END IF;

    FOREACH table_name IN ARRAY ARRAY[
        'account', 'role', 'pupil', 'specialist', 'profession', 'psych_test',
        'psych_param', 'prediction', 'school', 'curator'
    ] LOOP
        sequence_name := PG_GET_SERIAL_SEQUENCE(table_name, 'id');
        IF sequence_name IS NULL THEN
            RAISE EXCEPTION 'No identity/serial sequence for %.id', table_name;
        END IF;

        EXECUTE FORMAT('SELECT MAX(id) FROM %I', table_name) INTO maximum_id;
        EXECUTE FORMAT('SELECT last_value FROM %s', sequence_name)
            INTO sequence_value;

        IF maximum_id IS NOT NULL AND sequence_value < maximum_id THEN
            RAISE EXCEPTION
                'Sequence % is behind %.id: % < %',
                sequence_name, table_name, sequence_value, maximum_id;
        END IF;
    END LOOP;
END
$validation$;

SELECT 'account' AS entity, COUNT(*) AS row_count FROM account
UNION ALL SELECT 'pupil', COUNT(*) FROM pupil
UNION ALL SELECT 'specialist', COUNT(*) FROM specialist
UNION ALL SELECT 'profession', COUNT(*) FROM profession
UNION ALL SELECT 'psych_test', COUNT(*) FROM psych_test
UNION ALL SELECT 'psych_test_param', COUNT(*) FROM psych_test_param
UNION ALL SELECT 'psych_param', COUNT(*) FROM psych_param
UNION ALL SELECT 'prediction', COUNT(*) FROM prediction
UNION ALL SELECT 'school', COUNT(*) FROM school
UNION ALL SELECT 'curator', COUNT(*) FROM curator
ORDER BY entity;

SELECT r.name AS role_name, COUNT(DISTINCT ar.account_id) AS account_count
FROM role r
LEFT JOIN account_roles ar ON ar.role_id = r.id
GROUP BY r.name
ORDER BY r.name;

SELECT
    COUNT(*) FILTER (WHERE NULLIF(TRIM(school), '') IS NOT NULL) AS school_text_count,
    COUNT(*) FILTER (WHERE school_id IS NOT NULL) AS structured_school_count,
    COUNT(*) FILTER (
        WHERE NULLIF(TRIM(school), '') IS NOT NULL AND school_id IS NULL
    ) AS unmapped_school_count
FROM pupil;

SELECT
    COUNT(*) AS prediction_count,
    COUNT(*) FILTER (WHERE pupil_id IS NULL) AS missing_pupil_count,
    COUNT(*) FILTER (WHERE prediction_type_id IS NULL) AS missing_type_count
FROM prediction;

-- Preserved source-data quality exceptions. These are counts, not failures:
-- calculation maps nullable legacy values to the configured default_value.
SELECT
    COUNT(*) FILTER (WHERE pupil_id IS NULL AND specialist_id IS NULL) AS tests_without_owner,
    COUNT(*) FILTER (WHERE psych_test_type_id IS NULL) AS tests_without_type,
    COUNT(*) FILTER (WHERE pupil_id IS NOT NULL AND specialist_id IS NOT NULL) AS tests_with_two_owners
FROM psych_test;

SELECT
    COUNT(*) FILTER (WHERE psych_param_name_id IS NULL) AS params_without_name,
    COUNT(*) FILTER (WHERE param IS NULL) AS params_with_null_value
FROM psych_param;
