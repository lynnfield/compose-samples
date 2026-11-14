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

- [ ] Step 2: wrap Home, Article and Interests content in `TryAgain` and add the error screen, the article spinner and swipe to refresh (upstream `5a40e834`).
- [ ] Process death: is restoring the open article worth a design (deviation 10), or a non-goal for the comparison?
- [ ] `showChannel()` is a local helper because the library's no-argument `Show()` is deprecated. Should `uistate` grow a non-deprecated binding?
- [ ] The UI layer collects nested flows (`StateFlow<StateFlow<…>>`), which makes `JetnewsApp` and the routes verbose. Would a flattening helper in `combine` (or projections that carry values instead of flows) read better?
- [ ] Move `LoadAndShow` into the components library (review comment on PR #1). Not now; it stays local for step 1.
- [ ] Move `showChannel()` into `uistate` in the components library (review comment on PR #1). Not now; same question as the `Show()` item above.
- [ ] `SharePost` returns as soon as the chooser starts, so the Article loop accepts a new tap while the share sheet is still open. Waiting until the share closes needs an `ActivityResultLauncher` registered by `MainActivity` and handed to the leaf through a port that outlives the Activity (the leaf lives in the ViewModel). Parked for step 2 (decided in review on PR #1).
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
