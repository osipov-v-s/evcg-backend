-- DO NOT RUN AUTOMATICALLY
-- RUN ONLY AFTER BACKUP AND VALIDATION
--
-- This script is intentionally destructive. Execute only against the copied
-- career-guidance database after application verification and record counts.

BEGIN;

-- Review affected accounts before removing legacy assignments:
SELECT r.name, COUNT(DISTINCT ar.account_id) AS account_count
FROM account_roles ar
JOIN role r ON r.id = ar.role_id
WHERE r.name IN ('HR', 'APPLICANT')
GROUP BY r.name;

-- Uncomment only after a product owner has decided how historical accounts
-- should be archived or reassigned.
-- DELETE FROM account_roles
-- WHERE role_id IN (SELECT id FROM role WHERE name IN ('HR', 'APPLICANT'));
-- DELETE FROM role WHERE name IN ('HR', 'APPLICANT');

-- The application no longer maps this HR-only table. Drop it only after
-- confirming that no external integration or audit process needs its history.
-- DROP TABLE invitation;

ROLLBACK;
