Chore(ci): add CI workflows, tests, CloudSync abstraction, and Room migration

Summary
- Add GitHub Actions CI to run Gradle build, lint and unit tests.
- Add a workflow to generate & commit the Gradle wrapper on-demand.
- Introduce `CloudSync` abstraction + `CloudSyncAdapter` to decouple sync logic from `WeightViewModel`.
- Refactor `WeightViewModel` to accept injectable `WeightRepository` and `CloudSync` for easier testing.
- Bump Room schema to v2 and add a no-op `MIGRATION_1_2` to avoid destructive migrations.
- Add unit tests: DAO, ViewModel (in-memory Room + fake CloudSync), and basic Compose tests for `WeeklyWeightChart`.

Files of interest
- app/src/main/java/com/example/data/CloudSync.kt
- app/src/main/java/com/example/data/CloudSyncService.kt
- app/src/main/java/com/example/data/AppDatabase.kt
- app/src/main/java/com/example/ui/viewmodel/WeightViewModel.kt
- app/src/test/java/com/example/data/WeightDaoTest.kt
- app/src/test/java/com/example/ui/viewmodel/WeightViewModelTest.kt
- app/src/test/java/com/example/ui/components/WeeklyWeightChartTest.kt
- .github/workflows/android-build.yml
- .github/workflows/generate-wrapper.yml

How to test locally
1. Create or run the Gradle wrapper:
   - Option A (recommended): Run the workflow "Generate Gradle Wrapper" in Actions, then pull the generated wrapper.
   - Option B (local Gradle):
     ```powershell
     cd "d:\APRIS FILE\weight tracker project\weight-tracker"
     gradle wrapper --gradle-version 8.4
     ```
2. Run tests:
```powershell
cd "d:\APRIS FILE\weight tracker project\weight-tracker"
.\gradlew.bat test
```
3. Build:
```powershell
.\gradlew.bat build
```

Reviewer checklist
- [ ] Confirm CI workflow fits org policies (JDK, Gradle version, caching).
- [ ] Review Room version bump and `MIGRATION_1_2` placeholder; replace with real migrations if schema changes.
- [ ] Verify tests run successfully in CI or locally.
- [ ] Decide whether to commit the generated Gradle wrapper (workflow available to generate it).

Notes / Next steps
- Replace the no-op migration with real migrations when schema changes occur.
- Implement a real CloudSync client behind `CloudSync` for production sync.
- Add instrumented UI tests (Espresso/Compose) if desired.
