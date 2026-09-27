package com.example.jetnews.ui.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.example.jetnews.ui.article.ArticleComponent
import com.example.jetnews.ui.home.HomeComponent
import com.example.jetnews.ui.interests.InterestsComponent

/**
 * Root of the component tree. Holds the navigation stack of the app's screens.
 */
interface RootComponent {

    val stack: Value<ChildStack<*, Child>>

    fun onHomeClicked()

    fun onInterestsClicked()

    sealed class Child {
        class Home(val component: HomeComponent) : Child()
        class Article(val component: ArticleComponent) : Child()
        class Interests(val component: InterestsComponent) : Child()
    }
}
