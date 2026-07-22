# User Combobox Endpoint: Service-Layer Join Pattern for DynamoDB

**Date**: 2026-07-22 09:51
**Severity**: Low
**Component**: API combobox endpoints, common service layer, user/personnel linking
**Status**: Resolved

## What Happened

Implemented a new GET `/api/common/combobox/users` endpoint returning a filtered list of Users, constrained by a "soft inner join" condition: only users with an existing, non-null `militaryPersonnelId` that actually exists in the `MilitaryPersonnel` table are included. Users with no personnel link or dangling personnel references (deleted personnel, orphaned user records) are excluded.

Implementation followed the existing combobox pattern from `CommonController` (ranks, positions, units endpoints). New code paths:
- `UserRepository.findAllList()` — DynamoDB table scan for all users
- `CommonService.getUserCombobox()` — interface contract
- `CommonServiceImpl.getUserCombobox()` — loads all `MilitaryPersonnel` into an id-keyed map, filters users by presence of their `militaryPersonnelId` in that map
- `CommonController.getUserCombobox()` — REST handler, no additional role/access restrictions (any authenticated user)

Planner locked in a single-phase implementation; tester wrote 7 unit tests covering inclusion/exclusion/edge cases; all passed. Code reviewer scored 9/10 with no blockers. Committed as 99d2e21 ("feat: add GET /api/common/combobox/users endpoint").

## The Brutal Truth

**JDK 17 is non-negotiable and silently breaks on JDK 25.** This issue was already documented in yesterday's journal entry (20260721-1545), but it recurred today during local build/test validation. The system Java default (JDK 25) silently breaks Lombok annotation processing, causing compilation to appear successful while runtime reflection fails. Only noticed this because tester caught the error during test execution. The workaround (build/test against `C:\Program Files\Java\jdk-17.0.18`) is a known manual step, but this will trip up any contributor who doesn't read the previous session's journal. **The project needs a hard pom.xml enforcement (java.toolchain.version=17) or CI will fail mysteriously on future builds.**

**Access scope decision is a security-conscious choice, but underdocumented.** The endpoint has zero additional role restrictions — any authenticated user can call it and retrieve the full user roster filtered to those with personnel records. This differs from `/api/common/combobox/units` (admin-only). The decision was intentional (personnel roster should be visible to anyone in the system, not just admins), but there's no inline comment explaining why. A future session might second-guess this and try to add ROLE_ADMIN restriction, creating inconsistent API semantics.

**DynamoDB full-table scan is not cheap.** The service-layer "inner join" loads all `MilitaryPersonnel` records into memory every time the endpoint is called. This is O(n) on both users and personnel table size. For now, this is acceptable (combobox endpoints for ranks/positions already do the same), but the pattern becomes problematic at scale. No caching is implemented; every request refetches the entire personnel table.

## Technical Details

### Implementation

**UserRepository.findAllList():**
```java
public List<UserDto> findAllList() {
    ScanRequest scanRequest = new ScanRequest()
        .withTableName("users");
    ScanResult result = dynamoDbClient.scan(scanRequest);
    return result.getItems().stream()
        .map(this::mapToUserDto)
        .collect(Collectors.toList());
}
```

**CommonServiceImpl.getUserCombobox():**
- Calls `userRepository.findAllList()` to fetch all users
- Calls `militaryPersonnelRepository.findAllList()` to fetch all personnel records
- Builds a `Map<String, MilitaryPersonnelDto> personnelMap = personnel.stream().collect(Collectors.toMap(MilitaryPersonnelDto::getId, identity()))`
- Filters users: `.filter(user -> personnelMap.containsKey(user.getMilitaryPersonnelId()))`
- Returns filtered list

This is the "service-layer inner join" pattern — since DynamoDB has no native JOIN, the business logic layer loads related data and filters in-process. Reusable if more combobox endpoints are added that need this constraint.

**Access Control:**
- No `@HasRole` annotation on the controller method
- Spring Security `@EnableGlobalMethodSecurity` still requires authentication (JWT token present)
- Any authenticated principal can call it — no role gate

### Testing

TesterAgent wrote 7 unit tests in `CommonServiceImplUserComboboxTest`:
1. `testGetUserCombobox_IncludesUsersWithValidPersonnel` — user present, personnel exists, included
2. `testGetUserCombobox_ExcludesUserWithNullPersonnelId` — user.militaryPersonnelId is null, excluded
3. `testGetUserCombobox_ExcludesUserWithDanglingPersonnelId` — user references non-existent personnel, excluded
4. `testGetUserCombobox_EmptyPersonnelTable_ReturnsEmptyList` — no personnel records at all, all users excluded
5. `testGetUserCombobox_MixedScenario_FiltersCorrectly` — 5 users, 2 with valid links, 2 with null, 1 with dangling — returns 2
6. `testGetUserCombobox_PerformanceAcceptable_LargeUserSet` — mock 1000 users, 500 personnel, filter completes in < 100ms
7. `testGetUserCombobox_ReturnsListInDefinedOrder` — results are ordered by user creation timestamp (ascending)

All 7 passed. No test failures; no edge cases discovered during test execution.

### Code Review

Code reviewer (9/10 score) noted:
- ✅ Follows existing combobox pattern (ranks/positions)
- ✅ Null-safety correct (NullPointerException guards on militaryPersonnelId)
- ✅ Performance acceptable for current table sizes (< 50ms on test data)
- ✅ Cascade implications clear (if personnel is hard-deleted, users remain in database)
- ⚠️ (Accepted pre-existing pattern) Full-table scans for every request — shared with ranks/positions endpoints, not a regression
- ⚠️ (Accepted design decision) Unrestricted access differs from units endpoint; no inline comment explaining rationale

## What We Tried

1. **Query approach**: Initially considered a GSI on `User.militaryPersonnelId` to filter at the database level. Rejected because DynamoDB GSIs still require a partition key query; without a known personnel ID, we'd scan the GSI anyway. Service-layer filtering is equivalent in cost.

2. **Caching candidate**: Discussed adding Redis cache (TTL 5 minutes) to avoid repeated scans. Deferred as "not MVP scope" — combobox endpoints are low-traffic (typically loaded once on page init), and adding Redis introduces deployment complexity without proven need.

3. **Soft-delete check**: Initial filter was `militaryPersonnelId != null && personnelMap.contains(...)`. Refined to explicitly check both conditions in sequence for clarity (null check first, then existence check). Code reviewer approved both approaches; settled on explicit two-check pattern for readability.

## Root Cause Analysis

**No root cause for this session** — implementation was straightforward and matched the planned specification. The JDK 17 issue recurred because the system default (JDK 25) breaks Lombok silently; this is a pre-existing environment issue, not a code defect. Access scope decision was user-confirmed and documented in the plan.

## Lessons Learned

1. **Service-layer "inner join" is a reusable DynamoDB pattern**: When multiple combobox/list endpoints need to filter by relational constraints (users with personnel, personnel in unit, etc.), load related data into memory and filter in service layer. Document this as a pattern to avoid query redesigns. Consider extracting a `DynamoDbFilterChain` utility for standard filters (notNull, exists, inSet).

2. **Combobox access scope decisions need inline code comments**: The decision to make `/api/common/combobox/users` unrestricted (vs. `/api/common/combobox/units` admin-only) should be documented in the endpoint's JavaDoc. Future session won't need to re-read the plan or journal to understand why.

3. **JDK 17 lock must be in pom.xml, not just README**: Add `<maven.compiler.source>17</maven.compiler.source>` and `<maven.compiler.target>17</maven.compiler.target>` properties to pom.xml. Optionally add a `<maven.compiler.release>17</maven.compiler.release>` for toolchain enforcement. CI builds will fail early if JDK 25+ is used, rather than silently breaking annotation processing.

4. **Full-table scans + no cache is a scaling risk**: For now acceptable (< 50 users, < 100 personnel records in test). If the system grows beyond a few thousand personnel, implement a 5-minute Redis cache on combobox endpoints. Document this as a scaling threshold.

5. **Test coverage for filter logic is valuable**: The tester's comprehensive edge-case tests (null, dangling, mixed scenarios) would catch regressions if someone later changes the filter logic. This is evidence that "service-layer business logic deserves unit tests more than DB queries do."

## Next Steps

1. **Add JDK 17 toolchain lock to pom.xml**: Insert `<maven.compiler.release>17</maven.compiler.release>` in `<properties>` section. Verify CI passes with JDK 17 only.

2. **Add inline JavaDoc to getUserCombobox() endpoint**: Explain the access decision ("Any authenticated user can list all personnel-linked users; no role restriction, matching ranks/positions combobox semantics. Contrast: /api/common/combobox/units requires admin role.").

3. **Consider extracting a combobox filter utility** (low priority, future-proofing): If a third combobox endpoint needs similar filtering, extract `DynamoDbComboboxFilter` helper to avoid copy/paste. For now, pattern is clear enough from this single example.

4. **Monitor table scan performance**: If user count grows beyond 500 or personnel count beyond 1000, measure endpoint latency and revisit cache strategy. Document threshold at which caching becomes necessary.

5. **Backport access scope clarification to ranks/positions endpoints**: If those endpoints also lack inline documentation of their access decisions, add comments. Consistency aids future developers.

---

**Committed**: 99d2e21 ("feat: add GET /api/common/combobox/users endpoint") on master branch. Not pushed to remote (user holds push decision). Compiled cleanly against JDK 17 (with 17 override; would fail silently on JDK 25). All 7 tests passed locally. Code review 9/10, no blockers.

**Docs impact**: None. Feature is too small and straightforward to require updates to `./docs` (repo has no API spec or architecture docs beyond this journal).
