package com.markodevcic.newsreader.articles

import android.content.SharedPreferences
import com.markodevcic.newsreader.Presenter
import com.markodevcic.newsreader.data.Article
import com.markodevcic.newsreader.data.Source
import com.markodevcic.newsreader.storage.Repository
import com.markodevcic.newsreader.sync.SyncService
import com.markodevcic.newsreader.util.KEY_CATEGORIES
import java.io.Closeable
import javax.inject.Inject

class ArticlesPresenter @Inject constructor(private val articlesUseCase: ArticlesUseCase,
											private val sourcesRepository: Repository<Source>,
											private val sharedPreferences: SharedPreferences,
											private val syncService: SyncService) : Presenter<ArticlesView>, Closeable {
	private lateinit var view: ArticlesView

	override fun bind(view: ArticlesView) {
		this.view = view
	}

	fun onStart() {
		view.onUnreadCountChanged(getUnreadCount())
		if (!articlesUseCase.hasArticles()) {
			view.onNoArticlesAvailable()
		}
	}

	suspend fun onSelectedCategoriesChangedAsync() {
		articlesUseCase.onCategoriesChangedAsync()
	}

	suspend fun syncCategoryAsync(category: String?) {
		val sources = sourcesRepository.query({
			if (category != null) {
				equalTo("category", category)
			} else {
				val selectedCategories = sharedPreferences.getStringSet(KEY_CATEGORIES, setOf())
				`in`("category", selectedCategories.toTypedArray())
			}
		}, null, null)
		var downloadCount = 0
		for (src in sources.toTypedArray()) { //seems to be a bug in coroutines, if looping over normal List, only first item in the list is processed and function never ends... Works OK with Arrays
			downloadCount += syncService.downloadArticlesAsync(src)
		}
		view.onUnreadCountChanged(getUnreadCount())
		view.onArticlesDownloaded(downloadCount)
	}

	fun markArticleRead(vararg articleUrl: String) {
		articlesUseCase.markArticleRead(*articleUrl)
		view.onUnreadCountChanged(getUnreadCount())
	}

	suspend fun getArticlesInCategoryAsync(category: String?): List<Article> =
			articlesUseCase.getArticlesAsync(category)

	private fun getUnreadCount(): Map<String, Long> = articlesUseCase.getUnreadCount()

	override fun close() {
		articlesUseCase.close()
	}
}