# Decompose migration: decisions, deviations and open questions

Working notes for the `decompose` branch. `context.md` is the brief; this file records what was
decided while implementing it, where the implementation deviates from the brief, what is still
open, and what remains to be verified on a real Android toolchain. Edit freely and iterate.

## Decisions

| Topic | Decision | Why |
|---|---|---|
| Retention across configuration changes | Each `Default*Component` keeps its model and coroutine scope in an `InstanceKeeper.Instance` (`Retained`) | Stable (non-experimental) API; retention is explicit per component and testable with `InstanceKeeperDispatcher`. `retainedComponent {}` was the alternative (less code, but `@ExperimentalDecomposeApi` and easy to leak the Activity). |
| Coroutine scope ownership | The scope belongs to the `Retained` instance and is cancelled in `onDestroy()` | A scope tied to the `ComponentContext` lifecycle would be cancelled on rotation (see deviation 1). |
| Dispatcher | `Default*` components take `mainContext: CoroutineContext`; production passes `Dispatchers.Main.immediate`, tests pass a test dispatcher | No `Dispatchers.setMain` needed in tests. |
| Drawer | Stays inside the Home and Interests `Scaffold`s; drawer items call `RootComponent.onHomeClicked()` / `onInterestsClicked()` passed down from `JetnewsApp` | Keeps the UI identical (non-goal: changing the UI design). |
| Navigation ops | Open article: `pushNew(Config.Article(id))`; Article back: `pop()`; drawer Home: `popToFirst()`; drawer Interests: `bringToFront(Config.Interests)` | Home is always at the bottom of the stack; `pushNew` ignores double taps. |
| Repositories | Interfaces and fake data unchanged; `data/RepositoryCoroutines.kt` adds `suspendCancellableCoroutine` adapters (`awaitPosts()`, `awaitPost(id)`, `awaitTopics()`, ...) | Keeps the data layer identical to `functions-based`; the adapter is architecture-neutral and reusable there. |
| Shared state | `FavoritesStore` and `SelectedTopicsStore`, app-scoped in `AppContainer`, exposed as `StateFlow` | Per `context.md`; replaces `JetnewsStatus.favorites` / `selectedTopics`. |
| Root dependencies | `DefaultRootComponent` takes repositories and stores explicitly, not `AppContainer` | `AppContainer` needs `Context`/`Handler` and cannot be built in JVM tests (see deviation 2). |
| Versions | Decompose + extensions-compose **3.3.0**, Essenty 2.5.0 (transitive), kotlinx-serialization-core **1.7.3**, serialization plugin **2.1.0**, kotlinx-coroutines **1.9.0** | All built for Kotlin 2.1.0. Decompose 3.4.0+ pulls Compose foundation 1.8.x and would lift Compose past BOM 2024.12.01. |
| ktlint | Stay on 0.36.0; write code its (pre-Kotlin-1.4) parser accepts: no `data object`, `sealed interface` or trailing commas; `@Composable() () -> Unit` | Avoids a reformatting diff mixed into the architecture change. |
| Verification in the cloud session | A scratchpad-only JVM harness (Compose Multiplatform desktop 1.7.3 + `android-all` + stubs + Decompose JVM) compiles `main` and runs `test`; not committed | `dl.google.com` is blocked there, so no real Android build. |

## Deviations from context.md

1. **Lifecycle vs retention.** "Launch work in a scope tied to the `ComponentContext` lifecycle" and
   "survive configuration changes via `InstanceKeeper`" conflict: a lifecycle scope is cancelled on
   rotation. The scope is tied to the retained instance instead.
2. **`DefaultRootComponent(defaultComponentContext(), appContainer)`** becomes explicit dependencies;
   `MainActivity` unpacks the container. (`(application as JetnewsApplication).container` already
   exists in `MainActivity` from upstream.)
3. **`HomeComponent.onRefresh`** from the brief's example is dropped: the app at `cf290e9` has no
   refresh UI, so it would be a new feature.
4. **"The drawer is root-level UI"**: callbacks are root-level, the drawer UI stays in screen
   `Scaffold`s (see Decisions).
5. **Share** stays in the Article composable: it needs an Android `Context`/`Intent`, which should
   not enter a plain-Kotlin component.
6. **Back from Interests now returns to Home** (Interests is brought to front above Home). Before
   there was no back stack at all.
7. **Interests data loads once** per component instead of on every tab switch. Not visible, since
   `FakeInterestsRepository` is synchronous.
8. **Favorites and selected topics are still lost on process death** (in-memory app stores, like
   `JetnewsStatus`), while the navigation stack is restored. Persisting them is out of scope.
9. **"Survives rotation without reloading"** is hard to observe even at `cf290e9`, because
   `FakePostsRepository` only sleeps on its first call and outlives the Activity. The InstanceKeeper
   retention unit tests are the real check.

## Open questions

- [ ] Should `InterestsComponent`'s selected tab also survive process death (via `stateKeeper`)?
      Current default: no, rotation only, like the original.
- [ ] Keep dropping `onRefresh` (deviation 3), or add a refresh/retry affordance on both branches?
- [ ] Upgrade path: moving to Decompose 3.4+/3.5 requires a Compose BOM bump (foundation 1.8.x).
      Do it on both branches together, or leave pinned?
- [ ] ktlint 0.36.0 is very old; upgrade it on both branches in a separate change?
- [ ] Should favorites/topics move into the repositories (as upstream `main` did) instead of
      separate stores? The brief says stores; this changes the data layer, so it would affect the
      `functions-based` comparison too.

## Comparison baseline on upstream `main`

Besides `functions-based`, this branch can be compared with the upstream `main` commits that later
added architecture to the same parts of JetNews:

| Concern | Upstream `main` commit(s) | What they did | This branch |
|---|---|---|---|
| Navigation and back stack | `f75013e6` (2020-06) "Move navigation state to a ViewModel with SavedStateHandler to survive process death"; `b866e1cb` (2021-04) "[JetNews] Implement navigation library"; `e8d5f4f2` (2026-03) "Migrate Jetnews to Navigation 3" | `Screen` held in a NavigationViewModel, then Navigation Compose `NavHost`, then a Navigation 3 back stack | `RootComponent` with `childStack` and a `@Serializable Config` |
| Async loading and repositories | `2e4980a5` (2020-08) "Add ViewModel to JetNews"; `19b5e1cc` "Replace ViewModel with launchUiStateProducer"; `d5b6566f` (2021-08) "[Jetnews] Migrate to AAC ViewModels" | Repositories converted to `suspend` + `Flow`; loading moved to ViewModels / state producers | Interfaces kept; suspend adapter; loading in `Default*Component` |
| Favorites and selected topics | `2e4980a5`, `9f6c86c0` | Moved into the repositories (`observeFavorites()`, `toggleFavorite`, `observeTopicsSelected()`) | Separate app-scoped `FavoritesStore` / `SelectedTopicsStore` |

## Follow-up TODO: verify on a local Android toolchain

The cloud session cannot build Android (no SDK, no Google Maven), so these were **not** verified
there:

- [ ] `./gradlew check` (AGP build, resource/manifest merge, ktlint, unit tests via `testDebugUnitTest`)
- [ ] `./gradlew connectedDebugAndroidTest` (updated `TestHelper`/`JetnewsUiTest`)
- [ ] App launches; Home shows "Loading" for ~5 s on first load, then posts
- [ ] Opening an article from each card type (top, simple, popular, history)
- [ ] Article toolbar back arrow and system back both return to Home; system back on Home exits
- [ ] Drawer: Home ↔ Interests, correct highlighted item; back from Interests returns to Home
- [ ] Bookmark toggled on Home is reflected in Article and vice versa
- [ ] Topic selection persists across Interests tabs and after leaving/re-entering Interests
- [ ] Rotation on each screen: no "Loading" flash, selected Interests tab kept, stack kept
- [ ] "Don't keep activities" / process death while on an Article restores that Article
- [ ] Share action opens the chooser; "Functionality not available" dialog still works
- [ ] `@Preview`s render in Android Studio
