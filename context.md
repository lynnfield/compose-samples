# Context: JetNews on a function-composition (Action/Assembly) architecture

This branch (`functions-based`) starts from upstream commit
[`cf290e9`](https://github.com/android/compose-samples/commit/cf290e9a418abf8adeba791a555a3eed94aed036)
("Adds repository layer for posts and interests using effects", Manuel Vivo, 2020-03-18). It is one
of two sibling branches that re-architect the same JetNews snapshot so the approaches can be
compared side by side:

- `decompose`: state and navigation live in Decompose components.
- `functions-based` (this branch): the app becomes a composition of `Action`s wired by
  `Assembly` functions, following [`lynnfield/visual-ide`](https://github.com/lynnfield/visual-ide)
  and its runtime library [`lynnfield/components`](https://github.com/lynnfield/components).

Both branches keep the same features, data and repositories so that only the architecture differs.

**Required reading:** `docs/design.md` in `lynnfield/visual-ide`, especially §1 (core model and
operators), §1.6 and §5.1 (T-functions), §3 (the function file and assembly file split) and §3.5
(scoped singletons). The `demo/GuessGameDemo` project there shows the generated shape of a
function, its assembly and its `UiStateFlow`.

## The starting point (what `cf290e9` contains)

JetNews is a single-activity app under `JetNews/` with three screens: Home, Article and Interests.

| Concern | How it's done at `cf290e9` | Where |
|---|---|---|
| Data | Hardcoded posts and interests behind callback-based repository interfaces; `FakePostsRepository` adds an artificial delay on an executor | `data/posts/`, `data/interests/` |
| DI | Manual `AppContainer` held by `JetnewsApplication` | `data/AppContainer.kt` |
| Async loading | Composable "effects": `uiStateFrom(repoCall)` and `fetchPost(id, repo)` use `state {}` + `onActive {}` to call the repository and hold a `UiState` (`Loading`/`Success`/`Error`) | `ui/UiState.kt`, `ui/effect/PostsEffects.kt` |
| Navigation | Global `@Model object JetnewsStatus { currentScreen }`, `navigateTo(screen)`, `Crossfade` over a `sealed class Screen { Home; Article(postId); Interests }`. No back stack. | `ui/Status.kt`, `ui/JetnewsApp.kt` |
| Shared UI state | `JetnewsStatus.favorites` and `JetnewsStatus.selectedTopics` are global `ModelList`s mutated directly from composables | `ui/Status.kt`, `ui/home/PostCards.kt`, `ui/interests/InterestsScreen.kt` |

## Toolchain caveat

`cf290e9` builds against Kotlin 1.3.71 and Compose `0.1.0-dev09` (`@Model`, `state {}`,
`onActive {}`, `androidx.ui:*`), from before Compose 1.0 and without coroutines. `Action` is a
`suspend` function, and the runtime relies on `StateFlow`, so **step one is migrating the project**
to current AGP, Kotlin, kotlinx-coroutines, and Compose 1.x or a Compose BOM. Keep that migration in
its own commit(s), before any architectural change. The `decompose` branch needs the same migration,
so the two can share it.

## The architecture in brief

The details are in the design doc. The rules this branch follows are:

- **Everything is an `Action<Input, Output>`**, a `suspend (Input) -> Output` functional object.
  Its constructor parameters are its dependencies, and each dependency is itself an abstract `Action`.
- **The function file is the data plane.** `invoke` is only a composition of operators: sequence,
  branch over a named `sealed` type, loop (`repeatWhileActive` / `updateLoop`), race (`parallel`),
  error boundary (`Try` → `OneOf`) and decorators like `retryUntilResult`. A function never knows
  the concrete implementation of its dependencies.
- **The assembly file is the dependency plane.** `FooAssembly(...)` is a factory that fills each port
  with a default, usually a child `*Assembly(...)` call, and exposes every port as an overridable
  defaulted parameter (D5). Shared instances are scoped singletons: parameters of the declaring
  assembly, referenced by name in child defaults (§3.5, D15).
- **UI is a T-function.** A screen is an `Action<ScreenState, UserIntent>` bound in the assembly
  to `Show(flow)`. It publishes `UiState(input, callback)` on a `MutableStateFlow`, suspends, and
  resumes when the UI calls `callback(intent)`. The per-assembly `FooUiStateFlow` combines those
  flows into one `sealed Screen` that the composable layer renders.
- **Leaves (atoms)** are the only places with platform or IO code: repository calls, the share
  intent, the favorites store.

## Mapping JetNews onto Actions

### Navigation is the program's control flow

Navigation is a loop over a destination instead of a global variable:

```kotlin
sealed interface Destination {
    object Home : Destination
    data class Article(val postId: String) : Destination
    object Interests : Destination
}

class EntryPoint(
    val home: Action<Destination.Home, Destination>,
    val article: Action<Destination.Article, Destination>,
    val interests: Action<Destination.Interests, Destination>,
) : Action<Unit, Nothing>() {
    override suspend fun invoke(input: Unit): Nothing =
        retryUntilResult {
            updateLoop<Destination>(initial = Destination.Home) { destination ->
                when (destination) {                 // Branch over a named sealed type
                    is Destination.Home -> home(destination)
                    is Destination.Article -> article(destination)
                    is Destination.Interests -> interests(destination)
                }
            }
        }
}
```

Each screen flow returns the next `Destination`, and drawer items are intents that return one.
This first version has no back stack: Back from Article returns `Destination.Home`, as upstream
does. If a real stack is needed, loop over a `List<Destination>` instead. That is an open design
point for this branch; see "Open questions" below.

### Screen flows

Each screen flow is a composite `Action` that loops on its own screen until the user picks an
intent that leaves it.

**Home**: `HomeFlow : Action<Destination.Home, Destination>`

- `loadPosts : Action<Unit, List<Post>>`: a leaf over `PostsRepository`.
- `showLoading : Action<Unit, Nothing>`: a T-function the UI renders as a spinner. It never answers.
- Loading is a race: `parallel(Unit, showLoading, loadPosts)`. The first lane to finish wins, so the
  spinner lane is cancelled when the posts arrive. This replaces `uiStateFrom`.
- Failures go through `Try { … }` into `showError : Action<Throwable, Retry>`, then loop back.
- `showHome : Action<HomeState, HomeIntent>` is a T-function, where
  `sealed HomeIntent { OpenArticle(id); ToggleFavorite(id); Navigate(Destination) }`.
  `ToggleFavorite` calls the `toggleFavorite` leaf and loops. The other intents exit with a
  `Destination`.

**Article**: `ArticleFlow : Action<Destination.Article, Destination>`

- `loadPost : Action<String, Post>` races `showLoading` in the same way. The "postId doesn't exist"
  case from `fetchPost` becomes an error handled by `Try`.
- `showArticle : Action<ArticleState, ArticleIntent>` is a T-function, where
  `sealed ArticleIntent { Back; ToggleFavorite; Share }`.
  `Share` calls a `sharePost` leaf (Android intent) and loops. `Back` exits with `Destination.Home`.

**Interests**: `InterestsFlow : Action<Destination.Interests, Destination>`

- Loads topics, people and publications. These three independent calls can run in parallel and be
  combined into one product type (D1).
- `showInterests : Action<InterestsState, InterestsIntent>` is a T-function, where
  `sealed InterestsIntent { ToggleTopic(key); Navigate(Destination) }`.
- The selected tab is pure view state and stays in the composable (`remember`). It isn't business
  logic, so it doesn't go through an Action.

### Shared state: scoped singletons

`JetnewsStatus.favorites` and `selectedTopics` become `FavoritesStore` and `TopicSelectionStore`
(`StateFlow<Set<String>>` + toggle). The root `EntryPointAssembly` declares them once and passes
them by name into the `HomeFlowAssembly`, `ArticleFlowAssembly` and `InterestsFlowAssembly`
defaults, so every consumer gets the same instance. The `toggleFavorite`, `getFavorites` and
`toggleTopic` leaves wrap them.

### Repositories

Keep the repository interfaces and `PostsData.kt`. Leaves adapt the callback APIs with
`suspendCancellableCoroutine`, or the interfaces become `suspend` functions. Keep the artificial
delay so the loading race is visible. `AppContainer` becomes the root assembly's leaf-level
parameters (repositories, executor, `Context` for sharing) or is replaced by them.

### The UI layer

- The composable layer holds no logic. `JetnewsApp` collects `EntryPointUiStateFlow.screen` and
  `when`s over the sealed `Screen` cases. Each case renders one of the existing, now stateless,
  screen composables with `state.input` and wires user actions to `state.callback(intent)`.
- A `null` screen (nothing live) renders nothing or a blank surface.
- System back is a `BackHandler` that answers the live screen's callback with its `Back` intent.
- The program must outlive configuration changes. Launch `EntryPointAssembly(...)()` in a scope
  that survives rotation (a retained `ViewModel` scope or an application-level scope), not in
  composition or in the Activity. The Activity only observes the screen flow.

## File layout

Each composite Action gets two files, as in the design doc's two-file scheme (§3, D3):

```
flow/home/HomeFlow.kt            // class HomeFlow(ports…) : Action<Destination.Home, Destination>
flow/home/HomeFlowAssembly.kt    // fun HomeFlowAssembly(…defaults…): HomeFlow
flow/home/HomeFlowUiStateFlow.kt // flows for showHome/showLoading/showError + sealed Screen
```

Leaves live next to the data layer (`data/.../*Leaf.kt` or similar). The per-function Gradle module
split (`fn:Foo` / `asm:Foo`, D4) is **out of scope** here. Use packages, and add a lint or
visibility convention if needed. Mirror the canonical form (named `val`s, exhaustive `when`,
fully-qualified `components` calls) where it doesn't hurt readability. The visual-ide plugin being
able to parse this code would be a bonus, not a requirement.

## Runtime library

Use `lynnfield/components` (`com.genovich.components`) if it can be consumed from this build.
Check its actual API for the exact names and signatures of `Action`, `Show`, `UiState`,
`repeatWhileActive`, `updateLoop`, `retryUntilResult`, `parallel`, `Try`/`OneOf` and
`AssembleAndRun`. If it can't be consumed, vendor a minimal real implementation into a `components`
package. Unlike visual-ide's test stubs, it must actually run: `Show` has to use
`suspendCancellableCoroutine`, and the flows have to be real kotlinx `StateFlow`s.

## Definition of done

- The app builds and behaves as it did at `cf290e9`: same screens, drawer, favorites, topic
  selection and visible fake loading. It also survives rotation without restarting the flow.
- No composable reads a repository or global object, and no composable decides what screen comes
  next.
- JVM unit tests run each flow with fake ports, without Compose. For example: `HomeFlow` given
  `showHome` answering `OpenArticle("x")` returns `Destination.Article("x")`, and `ToggleFavorite`
  loops and calls the store. `EntryPoint` routes between destinations.
- `@Preview`s render the stateless screen composables with fake `UiState`s.

## Open questions (decide while implementing; record the answer here)

1. **Back stack**: Should `EntryPoint` loop over `Destination` (upstream parity) or over a
   `List<Destination>` (a real stack)?
2. **Loading and error UI**: Should they be separate T-functions (`showLoading`, `showError`, as
   above) or one `showHome` whose state includes loading and error? Separate T-functions show off the
   race operator. A single one is closer to upstream.
3. **Process death**: Should the current `Destination` be persisted and used as `updateLoop`'s
   `initial`, or is restoring after process death a non-goal?

## Non-goals

- Changing the UI design, theme or content.
- Replacing the fake repositories with a real network or database layer.
- Using the visual-ide plugin to generate this code (it only covers part of the operator catalog so
  far). The code is written by hand, in the architecture's shape.
