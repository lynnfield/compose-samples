# Context: JetNews on a Decompose-based architecture

This branch (`decompose`) starts from upstream commit
[`cf290e9`](https://github.com/android/compose-samples/commit/cf290e9a418abf8adeba791a555a3eed94aed036)
("Adds repository layer for posts and interests using effects", Manuel Vivo, 2020-03-18). It is one
of two sibling branches that re-architect the same JetNews snapshot so the approaches can be
compared side by side:

- `decompose` (this branch): state and navigation live in [Decompose](https://github.com/arkivanov/Decompose)
  components, outside the composable tree.
- `functions-based`: the same app rebuilt with the Action/Assembly function-composition
  architecture from [`lynnfield/visual-ide`](https://github.com/lynnfield/visual-ide).

Both branches keep the same features, data and repositories so that only the architecture differs.

## The starting point (what `cf290e9` contains)

JetNews is a single-activity app under `JetNews/` with three screens: Home, Article and Interests.

| Concern | How it's done at `cf290e9` | Where |
|---|---|---|
| Data | Hardcoded posts and interests behind callback-based repository interfaces; `FakePostsRepository` adds an artificial delay on an executor | `data/posts/`, `data/interests/` |
| DI | Manual `AppContainer` held by `JetnewsApplication` | `data/AppContainer.kt` |
| Async loading | Composable "effects": `uiStateFrom(repoCall)` and `fetchPost(id, repo)` use `state {}` + `onActive {}` to call the repository and hold a `UiState` (`Loading`/`Success`/`Error`) | `ui/UiState.kt`, `ui/effect/PostsEffects.kt` |
| Navigation | Global `@Model object JetnewsStatus { currentScreen }`, `navigateTo(screen)`, `Crossfade` over a `sealed class Screen { Home; Article(postId); Interests }`. No back stack. | `ui/Status.kt`, `ui/JetnewsApp.kt` |
| Shared UI state | `JetnewsStatus.favorites` and `JetnewsStatus.selectedTopics` are global `ModelList`s mutated directly from composables | `ui/Status.kt`, `ui/home/PostCards.kt`, `ui/interests/InterestsScreen.kt` |

The main problems the new architecture should fix:

- State lives in composition, so it is lost on configuration change and reloaded on every entry.
- Navigation is a global mutable variable with no back stack and no system back handling.
- Business logic (favorites, topic selection) is mixed into UI code through global singletons.
- Screens can't be tested without Compose.

## Toolchain caveat

`cf290e9` builds against Kotlin 1.3.71 and Compose `0.1.0-dev09` (`androidx.ui:*` artifacts, `@Model`,
`state {}`, `onActive {}`), from before Compose 1.0. Decompose needs a modern Kotlin and Compose
toolchain, so **step one is migrating the project** to current AGP, Kotlin, and Compose 1.x or a
Compose BOM (`remember`, `mutableStateOf`, `LaunchedEffect`, `androidx.compose.*` packages). Keep that
migration in its own commit(s), before any architectural change, so the architecture diff stays
readable. The `functions-based` branch needs the same migration, so the two can share it.

## Target architecture

### Principles

- **Components own state and logic; composables only render.** Each screen is a plain Kotlin
  component class that exposes `Value<Model>` (or `StateFlow`) and methods for user intents. The
  composable subscribes and calls those methods.
- **Navigation is state held by the root component.** Use `StackNavigation` + `childStack` with a
  `@Serializable` config per screen. That provides a real back stack, process-death restoration and
  system back handling via `BackHandler`.
- **Lifecycle-aware async.** Components launch work in a coroutine scope tied to their
  `ComponentContext` lifecycle, not in composition. Loaded data survives configuration changes via
  `InstanceKeeper` or retained components.
- **Interfaces at the boundary.** Every component is an interface plus a `Default*` implementation,
  so previews and tests can use fakes.

### Component tree

```
RootComponent                      childStack<Config, Child>
├── Child.Home       → HomeComponent       (posts list, favorites, open article, open drawer)
├── Child.Article    → ArticleComponent    (postId; load post, toggle favorite, share, back)
└── Child.Interests  → InterestsComponent  (tabs: Topics / People / Publications; toggle selection)
```

- `Config` replaces `ui/Status.kt`'s `Screen`: `Home`, `Article(postId: String)`, `Interests`.
- The drawer is root-level UI. Drawer items call `RootComponent.onHomeClicked()` and
  `onInterestsClicked()`, which `bringToFront` or `replaceAll` on the stack.
- Opening a post calls `navigation.push(Config.Article(id))`. Back pops the stack.
- Interests tabs can be a local `selectedTab` in `InterestsComponent`'s model. Use `childPages`
  only if tabs get their own logic.

### State shape

Keep the existing `UiState<T>` (`Loading`/`Success`/`Error`) as the loading-state type inside each
component model, for example:

```kotlin
interface HomeComponent {
    val model: Value<Model>
    fun onPostClicked(postId: String)
    fun onFavoriteToggled(postId: String)
    fun onRefresh()
    data class Model(val posts: UiState<List<Post>>, val favorites: Set<String>)
}
```

### Replacing global state

`JetnewsStatus.favorites` and `selectedTopics` are shared across screens. Move them into
app-scoped stores or repositories, created once in `AppContainer` and exposed as `StateFlow`, and
inject them into the components that need them. Do not keep them as globals, and do not let one
screen component own them.

### Repositories

Keep the repository interfaces and the fake data. Either wrap the callback APIs with
`suspendCancellableCoroutine` in a thin adapter, or convert the interfaces to `suspend` functions.
Keep the artificial delay so loading states are still visible. `PostsData.kt` stays untouched.

### Wiring

- `AppContainer` builds repositories and stores as it does today.
- `MainActivity` creates `DefaultRootComponent(defaultComponentContext(), appContainer)` and calls
  `setContent { JetnewsApp(root) }`.
- `JetnewsApp` renders the root `childStack` with the Decompose Compose extensions
  (`Children(stack) { child -> when (child.instance) { … } }`), keeping the existing Crossfade feel.
  A stack animation is a nice extra but not required.

## Mapping to change

| Now | After |
|---|---|
| `ui/Status.kt` (`Screen`, `JetnewsStatus`, `navigateTo`) | Removed. `Config` lives in `RootComponent`; favorites and topics move to stores. |
| `ui/effect/PostsEffects.kt` `fetchPost` | Removed. Loading happens in `DefaultArticleComponent`. |
| `ui/UiState.kt` `uiStateFrom` / `previewDataFrom` | `UiState` stays. The effect helpers are removed; previews use fake components. |
| `HomeScreen(postsRepository)` | `HomeScreen(component: HomeComponent)`: stateless rendering of `component.model` |
| `ArticleScreen(postId, postsRepository)` | `ArticleScreen(component: ArticleComponent)` |
| `InterestsScreen(interestsRepository)` | `InterestsScreen(component: InterestsComponent)` |
| `navigateTo(Screen.Article(id))` in cards | Cards take an `onClick: (String) -> Unit` lambda wired to `component::onPostClicked` |

## Definition of done

- The app builds and behaves as it did at `cf290e9`: same screens, drawer, favorites, topic
  selection and fake loading delay. It also gains a working system back button and survives
  rotation without reloading.
- No composable reads a repository or a global object directly.
- Unit tests cover at least `DefaultHomeComponent` and `DefaultArticleComponent` (loading →
  success/error, favorite toggle) and root navigation (push Article, back to Home), running on the
  JVM without Compose.
- `@Preview`s keep working through fake component implementations.

## Non-goals

- Changing the UI design, theme or content.
- Replacing the fake repositories with a real network or database layer.
- Adding a DI framework. Manual wiring through `AppContainer` stays.
