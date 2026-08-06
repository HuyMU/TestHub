# Database Migrations (Flyway)

## Naming Conventions
Flyway migration files follow the standard SQL naming pattern:
`V<Version>__<Description>.sql`

### Rules:
1. `V` must be capitalized.
2. Version number uses sequential integers (e.g. `V1`, `V2`, `V3`).
3. Double underscores `__` separate version from description.
4. Description uses snake_case (e.g. `V1__init_schema.sql`, `V2__add_index_on_test_cases.sql`).
5. Migration scripts are immutable once applied to any shared or production database.
