# Flyway migration seed split

- Added Flyway dependencies and profile-specific locations.
- Added V0 baseline schema and split common/dev repeatable seeds.
- Removed init.sql bootstrap path from dev config.
- Verified dev/prod scratch PostgreSQL migrations and login behavior.
- ./gradlew test still has pre-existing InMemorySignupVerificationAdapterTest assertion failures unrelated to Flyway.
