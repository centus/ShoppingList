package com.org.shoppinglist.data

data class SimpleSection(val name: String, val items: List<SimpleItem>)
data class SimpleItem(
    val name: String,
    val isPlanned: Boolean,
    val quantity: Int = 1,
    val imageUri: String? = null,
    val productLink: String? = null
)