# Orchestrator Plan

This is the execution plan for implementing the advanced tracking and player features in Animetail, and resolving the GDrive/Tracking API 404 and login loop errors.

## Milestones

1. **M1: Exploration & 404/Login Loop Diagnosis**
   - Goal: Explore codebase, target files, and identify the root cause of the HTTPS 404 errors for all tracking and Google Drive APIs, and diagnose the browser authentication login loop.
   - Status: DONE (Completed under explorer_m1)

2. **M2: Custom Speed Cycle (R1)**
   - Goal: Update player controls to cycle through custom speeds: 0.25x, 0.5x, 1.0x, 1.25x, 1.5x, 1.75x, 2.0x, and implement hold + swipe speed cycle gesture.
   - Status: IN_PROGRESS (Dispatched to worker_m1)

3. **M3: Local Media Injector (R2)**
   - Goal: Capture external video intents and symlink them into the app's local anime directory structure.
   - Status: IN_PROGRESS (Dispatched to worker_m1)

4. **M4: API 404 & Login Loop Fix**
   - Goal: Fix the 404 errors on all trackers and Google Drive APIs, and resolve the browser OAuth login loop.
   - Status: IN_PROGRESS (Dispatched to worker_m1)

5. **M5: Tracking Tab UI & Multi-Tracker (R3)**
   - Goal: Implement the "Tracking" tab (renamed from Discover) with an animated custom icon, integrating APIs for all supported tracking sites (MAL, AniList, Kitsu, Trakt, MangaUpdates, Bangumi, Shikimori, Simkl, etc.) to show trending/top/seasonal data.
   - Status: IN_PROGRESS (Dispatched to worker_m1)

6. **M6: Profile View & Deep Sync (R4)**
   - Goal: Implement Profile Compose screen showing user tracking statistics.
   - Status: IN_PROGRESS (Dispatched to worker_m1)

7. **M7: E2E and Audit Verification**
   - Goal: Verify all features using E2E tests, Challengers, and Forensic Auditor verification.
   - Status: PLANNED

## Verification Gates
Each milestone implementation will be audited using the standard iteration loop:
- Explorer plans the changes.
- Worker implements the changes.
- Reviewer checks the implementation.
- Challenger checks correctness.
- Forensic Auditor verifies integrity.
