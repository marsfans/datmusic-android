/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.navigation

import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import tm.alashow.Config
import tm.alashow.datmusic.data.repos.search.DatmusicSearchParams
import tm.alashow.datmusic.data.repos.search.DatmusicSearchParams.BackendType.Companion.toQueryParam
import tm.alashow.datmusic.domain.entities.Album
import tm.alashow.datmusic.domain.entities.ArtistId

const val QUERY_KEY = "query"
const val SEARCH_BACKENDS_KEY = "backends"
const val ARTIST_ID_KEY = "artist_id"
const val ALBUM_ID_KEY = "album_id"
const val ALBUM_OWNER_ID_KEY = "album_owner_id"
const val ALBUM_ACCESS_KEY = "album_access_key"

interface Screen {
    val route: String
}

sealed class RootScreen(
    override val route: String,
    val startScreen: LeafScreen,
    val arguments: List<NamedNavArgument> = emptyList(),
    val deepLinks: List<NavDeepLink> = emptyList(),
) : Screen {
    object Search : RootScreen("search_root", LeafScreen.Search)
    object Downloads : RootScreen("downloads_root", LeafScreen.Downloads)
    object Library : RootScreen("library_root", LeafScreen.Library)
    object Settings : RootScreen("settings_root", LeafScreen.Settings)
}

sealed class LeafScreen(
    override val route: String,
    val arguments: List<NamedNavArgument> = emptyList(),
    val deepLinks: List<NavDeepLink> = emptyList()
) : Screen {

    fun createRoute(root: RootScreen) = "${root.route}/$route"

    object Search : LeafScreen(
        "search/?$QUERY_KEY={$QUERY_KEY}&$SEARCH_BACKENDS_KEY={$SEARCH_BACKENDS_KEY}",
        arguments = listOf(
            navArgument(QUERY_KEY) {
                type = NavType.StringType
                nullable = true
            },
            navArgument(SEARCH_BACKENDS_KEY) {
                type = NavType.StringType
                nullable = true
            }
        ),
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "${Config.BASE_URL}search?$QUERY_KEY={$QUERY_KEY}&$SEARCH_BACKENDS_KEY={$SEARCH_BACKENDS_KEY}"
            }
        )
    ) {
        fun buildRoute(query: String, vararg backends: DatmusicSearchParams.BackendType) =
            "search/?$QUERY_KEY=$query&$SEARCH_BACKENDS_KEY=${backends.toSet().toQueryParam()}"

        fun buildUri(query: String) = "${Config.BASE_URL}search?q=$query".toUri()
    }

    object Downloads : LeafScreen("downloads")

    object Library : LeafScreen("library")

    object Settings : LeafScreen("settings")

    data class ArtistDetails(override val route: String = "artists/{$ARTIST_ID_KEY}") : LeafScreen(
        route,
        arguments = listOf(
            navArgument(ARTIST_ID_KEY) {
                type = NavType.StringType
            }
        ),
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "${Config.BASE_URL}artists/{$ARTIST_ID_KEY}"
            }
        )
    ) {
        companion object {

            fun createRoute(root: RootScreen) = ArtistDetails().let { it.copy(route = "${root.route}/${it.route}") }
            fun buildRoute(id: ArtistId, root: RootScreen = RootScreen.Search) = "${root.route}/artists/$id"
            fun buildUri(id: ArtistId) = "${Config.BASE_URL}artists/$id".toUri()
        }
    }

    data class AlbumDetails(override val route: String = "albums/{$ALBUM_ID_KEY}/{$ALBUM_OWNER_ID_KEY}/{$ALBUM_ACCESS_KEY}") : LeafScreen(
        route,
        arguments = listOf(
            navArgument(ALBUM_ID_KEY) {
                type = NavType.LongType
            },
            navArgument(ALBUM_OWNER_ID_KEY) {
                type = NavType.LongType
            },
            navArgument(ALBUM_ACCESS_KEY) {
                type = NavType.StringType
            }
        )
    ) {
        companion object {
            fun createRoute(root: RootScreen) = AlbumDetails().let { it.copy(route = "${root.route}/${it.route}") }
            fun buildRoute(album: Album, root: RootScreen = RootScreen.Search) = "${root.route}/albums/${album.id}/${album.ownerId}/${album.accessKey}"
            fun buildUri(album: Album) = "${Config.BASE_URL}albums/${album.id}".toUri()
        }
    }
}

fun NavGraphBuilder.composableScreen(screen: LeafScreen, content: @Composable (NavBackStackEntry) -> Unit) =
    composable(screen.route, screen.arguments, screen.deepLinks, content)

// https://stackoverflow.com/a/64961032/2897341
@Composable
inline fun <reified VM : ViewModel> NavBackStackEntry.scopedViewModel(navController: NavController): VM {
    val parentId = destination.parent!!.id
    val parentBackStackEntry = navController.getBackStackEntry(parentId)
    return hiltViewModel(parentBackStackEntry)
}
