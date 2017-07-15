package com.markodevcic.newsreader.storage

import io.realm.RealmModel
import io.realm.RealmQuery
import java.io.Closeable

interface Repository<T> : Closeable  where T : RealmModel {
	suspend fun getById(id: String): T

	fun getAll(): List<T>

	suspend fun deleteAll()

	suspend fun update(id:String, modifier: T.() -> Unit)

	suspend fun add(item: T)

	suspend fun addAll(items: List<T>)

	fun count(query: RealmQuery<T>.() -> Unit): Long

	fun query(init: RealmQuery<T>.() -> Unit): List<T>

	val clazz: Class<T>
}