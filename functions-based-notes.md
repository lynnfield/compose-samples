# Functions-based migration: decisions, deviations and open questions

Working notes for the `functions-based` branch, with the same headings as `decompose-notes.md` on
the `decompose` branch. `context.md` is the original brief. The plan this step follows is
"JetNews step 1: base walkthrough and next steps" (2026-11-14). This file records what was decided
while implementing it, where it deviates from the brief, what is still open and what remains to be
verified on a real Android toolchain.

## Decisions

| Topic | Decision | Why |
|---|---|---|
| Step 1 scope | Parity with the base commit `cf290e9`: Home shows "Loading" (via `ShowProgress`); an error, and an article or Interests still loading, are blank. No `TryAgain`, error screen or article spinner. | Upstream's next commit (`5a40e834`, loading and error screens, swipe to refresh) is step 2. Keeping it out makes the comparison with `decompose` fair. |
| Runtime | `com.genovich.components` **0.2.0** from Maven Central: `action`, `one-of`, `uistate`, `while-active`, `update-loop`, `parallel`, `combine`, `show-progress`, `on-demand-feature`. | `try` and `try-again` aren't needed until step 2. |
| Navigation | No navigation object or `Destination` loop. Article and Interests are `OnDemandFeature`s: `open` is a T-function (a card tap answers the post id; the drawer's Interests item answers `Unit`), `close` is a T-function (Back, or the drawer's Home item). Nesting is the back stack: JetNews ⊃ Interests, Home ⊃ Article. | Vladimir's redesign (plan v4). |
| Root | `JetNews` runs Home and, in parallel, `whileActive { interests() }`. | The drawer can open Interests at any time, including while Home is still loading, as in the base app. |
| Load then show | One generic `LoadAndShow<Input, Data, Output>(load, show, showError)` instead of `LoadAndShowHome` / `LoadAndShowArticle` / `LoadAndShowInterests`. | The three were the same shape. Its projection is `LoadAndShowUiState { Loading; Content; Error }`. |
| UI state | Every function file has the class plus a same-named Pair overload returning `Pair<Action, StateFlow<sealed UI state?>>`. Precedence between live T-functions is decided there (an open feature wins over the screen under it). The library's `OnDemandFeatureUiState` is reused as is. | The composables do a flat `when` with no rules of their own. |
| Shared state | `FavoritesStore` and `SelectedTopicsStore`, identical to `decompose`, held by `AppContainer` and reached through the leaves `GetFavorites`, `ToggleFavorite`, `GetSelectedTopics`, `SelectTopic`. Each screen re-reads them every time it is shown again. | Same data layer on both branches. |
| Wiring | `JetNewsAssembly(postsRepository, interestsRepository, sharePost, …)`: every port has a default; the stores are declared once and passed by name. Only the repositories and `SharePost` must be given, so a JVM test builds the real app. | Design doc D5 / D15. |
| `AppContainer` | Kept for the repositories (executor, main-thread `Handler`, `Resources`) and the two stores, as on `decompose`. | The 5 s first load stays once per process, and favorites survive finishing and relaunching the Activity, as with the base app's global object. |
| Where it runs | `JetnewsViewModel` runs the program in `viewModelScope`; `MainActivity` only renders `uiState`. | Survives rotation; cancelled when the Activity finishes. |
| Side effects | Share is the `SharePost(context)` leaf (`FLAG_ACTIVITY_NEW_TASK`, since it uses the application context). The "Functionality not available" dialog stays local UI state. | The dialog is pure view state, like the selected Interests tab. |
| Previews | `previewPosts()` / `previewPost(id)` / `previewData(call)` in `PreviewUtils.kt`, loading images through `PreviewPostsRepository`, as on `decompose`. | Same previews on both branches. |
| ktlint | Stay on 0.36.0: no trailing commas, `sealed interface` or `data object` in app code. | As on `decompose`. |

## Implementation status

1. Dependencies (components 0.2.0, coroutines-android, activity-ktx, lifecycle-viewmodel-ktx, JUnit, coroutines-test)
2. Repository leaves (`RepositoryCall.await()`, `LoadPosts`, `LoadPost`, `LoadTopics`, `LoadPeople`, `LoadPublications`), stores and their leaves
3. `LoadAndShow`, `ShowHome`
4. `ShowArticle`, `ArticleContent`, `ArticleFeature`, `SharePost`
5. `LoadInterests`, `ShowInterests`, `InterestsContent`, `InterestsFeature`
6. `JetNews` root, `JetNewsUiState`, `JetNewsAssembly`, integration test
7. Stateless post cards, drawer and screens (temporary wrappers kept the app on the globals)
8. `JetnewsViewModel`, `JetnewsApp` renders `JetNewsUiState`; `Status.kt`, `UiState.kt`, `PostsEffects.kt` removed
9. This file
10. Opt in to `@ExperimentalAnimationApi`: `Transition.Crossfade(contentKey = …)` keys the fade on the screen's case, so re-showing Home after a bookmark doesn't fade. Found by the first real build; commit 8 alone doesn't compile without it.

Unit tests (`JetNews/app/src/test`, `./gradlew :app:testDebugUnitTest`) in
`RepositoryCallTest`, `PostsLeavesTest`, `FavoritesTest`, `InterestsDataTest`, `LoadAndShowTest`,
`ShowHomeTest`, `ShowArticleTest`, `ArticleFeatureTest`, `InterestsFeatureTest` and
`JetNewsAssemblyTest`. They run every function with the real `Show` T-functions and answer them
through their UI state flows, the way the composables do. No Compose, no Android.

### How the cloud session verified it

The cloud environment can't build Android (`dl.google.com` is blocked, no SDK). A throwaway JVM
Gradle project (not committed) compiled the data, model and `flow` packages against components
0.2.0 (with a one-line stub for `ImageBitmap`) and ran all of `src/test`. The Compose UI layer and
the Android build were verified on Vladimir's Mac with Temurin 21 (Android Studio's bundled JDK 25 is too new for Gradle 8.11.1): `assembleDebug`, `testDebugUnitTest`, `ktlint` and `assembleDebugAndroidTest` pass, and a smoke test on a Pixel 8 API 35 emulator went through the checklist below with no crashes.

## Deviations from context.md and the plan

1. **No `Destination` / `EntryPoint` loop** (context.md): replaced by `OnDemandFeature` nesting, per the redesign.
2. **No `TryAgain`, error screen or article spinner** (plan v4): moved to step 2 for parity with base.
3. **`LoadInterests` uses early returns, not `Try`.** `Try`'s `catch` yields one value type and the three loaders return different types, so `Try` would need casts.
4. **One generic `LoadAndShow`** instead of three named ones.
5. **`AppContainer` stays** (the plan said the wiring replaces it); the wiring takes the repositories and stores from it. Same reason as `decompose`: it needs `Context`/`Handler`, and keeping it keeps the data layer identical.
6. **Home is not reloaded** after an article or Interests closes: Home stays live underneath. The base app re-ran `getPosts` every time Home re-entered composition (fast after the first 5 s).
7. **Back from Interests returns to Home** (system back). The base app had no back handling; `decompose` does the same.
8. **Interests data loads once per opening**, not on every tab switch (same as `decompose`; not visible, since the fake repository is synchronous).
9. **The drawer closes before navigating** (same as `decompose`).
10. **Process death is not handled:** the program restarts at Home with a new load. `decompose` restores its stack; restoring here would mean replaying the `open` answers, a separate design.

## Open questions

- [x] Step 2: wrap Home, Article and Interests content in `TryAgain` and add the error screen, the article spinner and swipe to refresh (upstream `5a40e834`). Done, see below.
- [ ] Process death: is restoring the open article worth a design (deviation 10), or a non-goal for the comparison?
- [ ] `showChannel()` is a local helper because the library's no-argument `Show()` is deprecated. Should `uistate` grow a non-deprecated binding?
- [ ] The UI layer collects nested flows (`StateFlow<StateFlow<…>>`), which makes `JetnewsApp` and the routes verbose. Would a flattening helper in `combine` (or projections that carry values instead of flows) read better?
- [ ] Move `LoadAndShow` into the components library (review comment on PR #1). Not now; it stays local for step 1.
- [ ] Move `showChannel()` into `uistate` in the components library (review comment on PR #1). Not now; same question as the `Show()` item above.
- [x] (Done in step 2.) `SharePost` returns as soon as the chooser starts, so the Article loop accepts a new tap while the share sheet is still open. Waiting until the share closes needs an `ActivityResultLauncher` registered by `MainActivity` and handed to the leaf through a port that outlives the Activity (the leaf lives in the ViewModel). Parked for step 2 (decided in review on PR #1).
- [ ] Parked idea (review comment on PR #1): create the program and hold its reference in `Application` instead of `JetnewsViewModel`, and inject it with `AppComponentFactory`, `ActivityLifecycleCallbacks` or `getSystemService()`.
- [ ] ktlint 0.36.0 is very old; upgrade it on both branches together?
- [ ] New files have no license header, as on `decompose`.

## Verification on a local Android toolchain

Done on 2026-11-14 on a Pixel 8 API 35 emulator:

- [x] `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:ktlint`
- [x] `./gradlew :app:assembleDebugAndroidTest` (compiled; not run)
- [x] App launches; Home shows "Loading", then posts after ~5 s
- [x] Opening an article from the top card; bookmark in the article; back returns to Home without "Loading"
- [x] Bookmark on Home fills immediately
- [x] Drawer → Interests; select a topic; back returns to Home; reopening Interests keeps the topic
- [x] Rotation on Home keeps the data, with no reload
- [x] No `AndroidRuntime` errors in logcat
- [ ] Share chooser, "Functionality not available" dialog, articles from the other card types, rotation on Article and Interests, drawer during loading
- [ ] `@Preview`s in Android Studio
- [ ] `connectedDebugAndroidTest` on a device

**Found, not caused by this branch:** on API 35 the app is drawn edge-to-edge (forced by `targetSdk 35`
from the toolchain migration `47e63f7`), so the app bar sits under the status bar and the drawer
button can't be tapped; the drawer still opens with an edge swipe. It affects `decompose` the same
way and should be fixed on both branches (window insets on the `TopAppBar` / `Scaffold`).

## Step 2: loading and error screens, pull to refresh (upstream `5a40e834`)

Commits, mirrored on `decompose` (5 is functions-only):

1. The fake posts repository waits 2 s on the first call and 1.5 s on every call, and fails 1/3 of
   the time (upstream). The failure rate is a parameter; the UI test helper sets it to 0.
2. Retry failed loads with `TryAgain` on Home, Article and Interests.
3. A spinner while Home and an article load.
4. Pull to refresh on Home.
5. `SharePost` returns only when the share sheet closes.

| Topic | Decision | Why |
|---|---|---|
| Errors | `LoadAndShow(load, show)` returns `OneOf<Output, Throwable>`; the library's `TryAgain` around it shows the error and runs it again when the error T-function is answered | The library already has the block. `LoadAndShow` lost its `showError` and its `Error` UI case; `RetryableUiState<T>` names the nested UI state. |
| Home error | An empty Home with upstream's snackbar and RETRY. The 5 s auto-hide is view state only: the error keeps waiting, and pulling to refresh answers it too | Upstream behaves the same: after the snackbar hides, you can still pull. |
| Refresh | A generic `Refresh(waitForRefresh, reload, showError)` (`flow/common`) returning the new data; its error answers `true` (Retry) or `false` (dismissed, keep the old data) | Upstream keeps the old posts on a failed refresh. |
| Home structure | `ShowHome` races bookmarks (an inner `whileActive` loop), `openArticle` and `refresh`. A card tap leaves the race and runs the article until Back. | A bookmark must not cancel a running refresh; a finished refresh must not close an open article (see the friction log). |
| Article | No longer an `OnDemandFeature`: `ArticleFeature(content, close)` is the content in parallel with Back, started with the id `openArticle` answered. One content instance is reused for every opening. | Its `open` had to move into Home's race. The library's `Show`, `ShowProgress` and `TryAgain` clear their state when cancelled, so reuse is safe. |
| Article and Interests errors | An error screen with Retry | Upstream left both blank; added on both branches. |
| Article loading | `ShowProgress` around `loadPost`, rendered as the app bar with Back above a spinner | Decided for step 2 in the plan. |
| Share | `SharePost(ActivityResultPort<Intent>)`: `MainActivity` registers a `StartActivityForResult` launcher, attaches it in `onCreate`, detaches it in `onDestroy` and reports the result | The program lives in the ViewModel and outlives the Activity. A result that arrives in a recreated Activity still resumes the program. |
| Swipe to refresh | Material `pullRefresh` + `PullRefreshIndicator` (`@ExperimentalMaterialApi`) | Same as `decompose`. |
| Library promotion | Nothing moved to `com.genovich.components` in this step | No block was written for a second time. Candidates for the next release: `LoadAndShow` (used 3 times), `Refresh`, `showChannel()`, `ActivityResultPort`. |

**Behavior to note:** opening an article cancels a refresh that is still running (as leaving Home
did upstream). On `decompose` it finishes in the background.

### Friction log

- Retry was nearly free: `TryAgain(LoadAndShow(…), showChannel())` on each screen. Interests
  changed by one line, against +22 on `decompose`.
- Refresh broke a step 1 property. In step 1 the article ran as a sibling lane of Home
  (`OnDemandFeature`), so Home stayed live under it. With refresh as another lane, a refresh that
  finishes while the article is open restarts Home's loop, and `parallel` cancels the article with
  it. The fix was to take the article out of the race: a card tap ends the race, then the article
  runs. The first design looked right and was wrong; only thinking through "which lane finishing
  cancels which" found it. Tests now pin it (`openingAnArticleCancelsARefresh`,
  `aBookmarkDoesNotCancelARefresh`).
- The same question came up for bookmarks: a tap used to restart the whole loop, which would
  cancel a running refresh. Bookmarks moved into an inner loop.
- The UI pays for the nesting. Home's route collects `TryAgainUiState` → `LoadAndShowUiState` →
  `ShowHomeUiState` → `RefreshUiState` → `UiState`, and decides which T-function a pull answers
  (refresh, retry the refresh, or retry the first load). `HomeScreen.kt` grew by 94 lines here
  against 60 on `decompose`.
- Two kinds of error under one snackbar: the first load's error waits forever (hiding the snackbar
  is view state), the refresh's error is answered "dismissed". Same pixels, different T-function
  types.
- Waiting for the share sheet needed a port (`ActivityResultPort`, attach/detach in
  `MainActivity`), about 30 lines, because the Activity and the program have different lifetimes.
  In exchange, "Share is busy until the sheet closes" is now visible in the program, and a second
  tap is ignored.

