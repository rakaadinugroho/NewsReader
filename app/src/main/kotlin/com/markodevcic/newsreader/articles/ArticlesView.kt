package com.markodevcic.newsreader.articles

import com.markodevcic.newsreader.BaseView

interface ArticlesView : BaseView {
	fun onUnreadCountChanged(counts: Map<String, Long>)

	fun onNoArticlesSaved()

	fun onArticlesDownloaded(count: Int)
}