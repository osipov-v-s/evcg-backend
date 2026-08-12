# Database migration plan

The source database must not be modified in place. Restore a full backup into a new career-guidance database, validate it, and only then consider removing legacy B2B data.

| OLD TABLE/ENTITY | TARGET TABLE/ENTITY | ACTION | DATA TRANSFORMATION | DEPENDENCIES | VALIDATION |
|---|---|---|---|---|---|
| `account` | `account` | KEEP | Preserve IDs, emails, password hashes and first-login state | `account_roles`, user profiles | Counts and login checks by active role |
| `role` | `role` | ADAPT | Add `CURATOR`; keep historical role rows during transition | `account_roles` | Active application access is only ADMIN/SPECIALIST/PUPIL/CURATOR |
| `pupil` | `pupil` | ADAPT | Preserve legacy `school`; add nullable `school_id` and backfill by normalized school name | school, tests, grades, simulations, predictions | Counts, null links, sample profiles and calculations |
| `specialist` | `specialist` | KEEP | Preserve profession and informational company link | profession, company, tests, predictions | Counts and reference-model samples |
| `company` | `company` | KEEP | No employee/HR workflow; retain workplace information | specialist | Every non-null specialist company still resolves |
| free-text `pupil.school` | `school` | NEW | Insert distinct trimmed names, then link matching pupils | pupil | Compare distinct names and linked pupil counts |
| none | `curator` | NEW | Create profiles linked one-to-one to account and many-to-one to school | account, school, CURATOR role | Curator A cannot read pupils of school B |
| `profession`, `profession_sphere` | same | KEEP | No ID/name rewrite | specialist, VR tests, simulations, predictions | Counts and FK integrity |
| psych-test tables | same | KEEP | No transformation | pupil/specialist | Recent/completed test queries and ownership |
| subject/grade/profile tables | same | KEEP | No transformation | pupil | Counts and pupil educational-profile reads |
| VR test/type/answer tables | same | KEEP | No transformation | pupil/specialist/profession | Per-account and per-profession reads |
| simulation/scenario/source/type | same | KEEP | No transformation | pupil/profession/files | File links, source metadata and date filters |
| DataAnalysis comparison/session/sample tables | same | KEEP | No transformation | account, uploaded files | Session ownership and stored-file checks |
| EEG data represented in uploaded session/simulation files | same | KEEP | Preserve records, files and metadata paths | pupil/session, filesystem/object storage | File existence, hashes where available, record counts |
| eye/gaze tracking fields and uploaded data | same | KEEP | Preserve flags, records, files and metadata | pupil/session, filesystem/object storage | Sample session read and file checks |
| face tracking fields and uploaded data | same | KEEP | Preserve flags, records, files and metadata | pupil/session, filesystem/object storage | Sample session read and file checks |
| uploaded external-data files | same | KEEP | Preserve paths and metadata; copy storage separately with integrity checks | simulation/comparison sessions | Record counts, path resolution and hashes where available |
| prediction/reference data | same | KEEP | No formula or identifier changes | pupil/profession/specialist | Known deterministic sample and latest-result read |
| recommendation output implemented through predictions/ranked professions | same | KEEP | Preserve existing results; do not fabricate a separate recommendation model | prediction, profession, pupil | Latest-result structure and profession ordering |
| `HR` role assignments | historical rows | LEGACY_TEMPORARY | Do not grant active access; decide archive/reassignment after counting | account_roles | Query counts before and after approved cleanup |
| `APPLICANT` role assignments | historical rows | LEGACY_TEMPORARY | Do not grant active access; decide archive/reassignment after counting | account_roles | Query counts before and after approved cleanup |
| `invitation` | none | DROP | Drop only when a transactional guard confirms zero rows; otherwise abort | none in supplied dump | Guarded count and absence after migration |

## Safe sequence

1. Back up the current database and record checksums/counts.
2. Restore the complete backup into a separate database.
3. Configure the new deployment to use only that copy.
4. Apply `database/legacy_to_current.sql` with `ON_ERROR_STOP=1`.
5. Run `database/validate_migration.sql`.
6. Compare account, pupil, specialist, profession, test, grade, VR, simulation, comparison and prediction counts.
7. Check unmatched `pupil.school` values and case-normalized school counts.
8. Verify ADMIN, SPECIALIST, PUPIL and CURATOR scenarios, including cross-school denial.
9. Verify stored file paths and external VR/EEG/eye/face ingestion against the copied deployment.
10. Decide the disposition of historical HR/APPLICANT accounts with the product owner.
11. Keep historical role assignments unless a separate, approved archival policy is defined.

The production database was not migrated or modified while preparing this plan.
