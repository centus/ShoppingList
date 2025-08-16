package com.org.shoppinglist.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.org.shoppinglist.R
import com.org.shoppinglist.data.ShoppingItem

class ItemAdapter(
    private var isShoppingMode: Boolean,
    private val onItemChecked: (ShoppingItem, Boolean) -> Unit,
    private val onItemEdit: (ShoppingItem, String) -> Unit, // Ensures ShoppingItem is passed
    private val onItemDelete: (ShoppingItem) -> Unit,
    private val onItemMove: (ShoppingItem, Long) -> Unit // Callback: item to move, current sectionId
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var items: List<ShoppingItem> = emptyList()
    fun updateShoppingMode(newShoppingMode: Boolean) {
        this.isShoppingMode = newShoppingMode
        // You might need to refresh the displayed items if the mode change affects their appearance
        notifyDataSetChanged()
    }
    fun updateItems(newItems: List<ShoppingItem>) {
        val diffCallback = ShoppingItemDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    private class ShoppingItemDiffCallback(
        private val oldList: List<ShoppingItem>,
        private val newList: List<ShoppingItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.name == newItem.name &&
                   oldItem.isChecked == newItem.isChecked &&
                   oldItem.isAdHoc == newItem.isAdHoc &&
                   oldItem.sectionId == newItem.sectionId
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_item, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemCheckBox: CheckBox = itemView.findViewById(R.id.itemCheckBox)
        private val itemName: TextView = itemView.findViewById(R.id.itemName)
        private val editItemButton: ImageButton = itemView.findViewById(R.id.editItemButton)
        private val deleteItemButton: ImageButton = itemView.findViewById(R.id.deleteItemButton)
        private val moveItemButton: ImageButton = itemView.findViewById(R.id.moveItemButton)

        private var isBinding = false

        init {
            itemCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (!isBinding && adapterPosition != RecyclerView.NO_POSITION) {
                    // val itemId = items[adapterPosition].id // Not needed if passing full item
                    onItemChecked(items[adapterPosition], isChecked)
                }
            }

            editItemButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = items[adapterPosition]
                    onItemEdit(item, item.name) // Ensures ShoppingItem is passed
                }
            }

            deleteItemButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    // val itemId = items[adapterPosition].id // Not needed if passing full item
                    onItemDelete(items[adapterPosition])
                }
            }

            moveItemButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = items[adapterPosition]
                    onItemMove(item, item.sectionId) // Pass the item and its current sectionId
                }
            }
        }

        fun bind(item: ShoppingItem) {
            isBinding = true
            itemName.text = item.name
            itemCheckBox.isChecked = item.isChecked
            isBinding = false

            // Apply visual styling based on checked status
            if (item.isChecked) {
                itemName.alpha = 0.5f
                itemName.paintFlags = itemName.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                itemName.alpha = 1.0f
                itemName.paintFlags = itemName.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Show/hide buttons based on mode
            if (isShoppingMode) {
                editItemButton.visibility = View.GONE
                deleteItemButton.visibility = View.GONE
                moveItemButton.visibility = View.GONE
            } else {
                editItemButton.visibility = View.VISIBLE
                deleteItemButton.visibility = View.VISIBLE
                moveItemButton.visibility = View.VISIBLE
            }

            // Show ad-hoc indicator
            if (item.isAdHoc) {
                itemName.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_add_circle, 0, 0, 0
                )
            } else {
                itemName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }
    }
}
