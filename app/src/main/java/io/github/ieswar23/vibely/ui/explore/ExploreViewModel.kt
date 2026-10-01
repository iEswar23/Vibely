package io.github.ieswar23.vibely.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.vibely.data.repository.PostRepository
import io.github.ieswar23.vibely.data.repository.UserRepository
import io.github.ieswar23.vibely.domain.model.HashtagStat
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.domain.model.User
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchResults(
    val query: String = "",
    val users: List<User> = emptyList(),
    val hashtags: List<HashtagStat> = emptyList(),
) {
    val isEmpty: Boolean get() = users.isEmpty() && hashtags.isEmpty()
}

data class ExploreUiState(
    val trending: List<HashtagStat> = emptyList(),
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val loadState = MutableStateFlow(LoadState())

    val uiState: StateFlow<ExploreUiState> = combine(
        postRepository.trendingHashtags(limit = 12),
        postRepository.explorePosts(),
        loadState,
    ) { trending, posts, load ->
        ExploreUiState(
            trending = trending,
            posts = posts,
            isLoading = load.loading && posts.isEmpty(),
            loadFailed = load.failed && posts.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())

    private val allHashtags: Flow<List<HashtagStat>> = postRepository.trendingHashtags(limit = 500)

    val searchResults: StateFlow<SearchResults> = _query
        .map { it.trim() }
        .debounce(250)
        .distinctUntilChanged()
        .flatMapLatest { q -> if (q.isEmpty()) flowOf(SearchResults()) else search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults())

    init {
        refresh()
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun refresh() {
        viewModelScope.launch {
            loadState.value = LoadState(loading = true)
            val result = postRepository.refreshExplore()
            loadState.value = LoadState(loading = false, failed = result.isFailure)
        }
    }

    private fun search(query: String): Flow<SearchResults> {
        val tagQuery = query.removePrefix("#").lowercase()
        val users = if (query.startsWith("#")) flowOf(emptyList()) else userRepository.search(query)
        val tags = allHashtags.map { stats ->
            if (tagQuery.isEmpty() || query.startsWith("@")) emptyList()
            else stats.filter { it.tag.contains(tagQuery) }
                .sortedWith(compareBy<HashtagStat> { !it.tag.startsWith(tagQuery) }.thenByDescending { it.postCount })
                .take(20)
        }
        return combine(users, tags) { u, t -> SearchResults(query = query, users = u, hashtags = t) }
    }

    private data class LoadState(val loading: Boolean = true, val failed: Boolean = false)
}
