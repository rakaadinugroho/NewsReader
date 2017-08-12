package com.markodevcic.newsreader

import android.app.Activity
import android.app.ProgressDialog
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.CheckBox
import com.markodevcic.newsreader.articles.ArticlesActivity
import com.markodevcic.newsreader.data.CATEGORIES_TO_RES_MAP
import com.markodevcic.newsreader.extensions.showToast
import com.markodevcic.newsreader.extensions.startActivity
import com.markodevcic.newsreader.injection.Injector
import kotlinx.android.synthetic.main.activity_startup.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

class StartupActivity : AppCompatActivity(), StartupView {

	@Inject
	lateinit var presenter: StartupPresenter

	private val job = Job()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		Injector.appComponent.inject(this)
		presenter.bind(this)

		if (intent.hasExtra(KEY_CHANGE_CATEGORY) || !presenter.canOpenMainView) {
			setContentView(R.layout.activity_startup)
			setSupportActionBar(toolbar)
			if (intent.hasExtra(KEY_CHANGE_CATEGORY)) {
				supportActionBar?.setDisplayHomeAsUpEnabled(true)
				supportActionBar?.title = "Categories"
			} else {
				supportActionBar?.title = getString(R.string.app_name)
			}
			for ((key, resId) in CATEGORIES_TO_RES_MAP) {
				val checkBox = LayoutInflater.from(this).inflate(R.layout.item_category, categoriesHost, false) as CheckBox
				checkBox.tag = key
				checkBox.setText(resId)
				checkBox.typeface = Typeface.SERIF
				checkBox.setOnCheckedChangeListener { box, checked ->
					presenter.onCategoryChanging(box.tag.toString(), checked)
				}
				categoriesHost.addView(checkBox)
			}
			saveCategoriesBtn.setOnClickListener {
				launch(UI + job) {
					val dialog = showProgressDialog()
					try {
						presenter.downloadAllArticlesAsync()
					} catch (fail: Throwable) {
						Log.e("Sync", fail.message, fail)
						dialog.dismiss()
						showToast("An error occurred while downloading articles")
					} finally {
						dialog.dismiss()
					}
				}
			}
			presenter.onStartCategorySelect()
		} else {
			startMainView()
		}
	}

	private fun showProgressDialog() = ProgressDialog.show(this, "Downloading sources", "", true, false)

	override fun showNoCategorySelected() {
		showToast("Please choose at least one category")
	}

	override fun startMainView() {
		setResult(Activity.RESULT_OK)
		startActivity<ArticlesActivity>()
	}

	override fun onCategorySelected(categorySet: Set<String>) {
		(0..categoriesHost.childCount - 1)
				.map { categoriesHost.getChildAt(it) }
				.filterIsInstance<CheckBox>()
				.filter { categorySet.contains(it.tag) }
				.forEach { it.isChecked = true }
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			onBackPressed()
			return true
		}
		return super.onOptionsItemSelected(item)
	}

	override fun onDestroy() {
		super.onDestroy()
		job.cancel()
	}

	companion object {
		const val KEY_CHANGE_CATEGORY = "KEY_CHANGE_CATEGORY"
	}
}
