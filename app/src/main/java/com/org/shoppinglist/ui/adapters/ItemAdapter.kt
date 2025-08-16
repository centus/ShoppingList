package com.org.shoppinglist.ui.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.org.shoppinglist.R
import com.org.shoppinglist.data.ShoppingItem

class ItemAdapter(
    var isShoppingMode: Boolean,
    private val onItemChecked: (ShoppingItem, Boolean) -> Unit,
    private val onItemPlanned: (ShoppingItem) -> Unit,
    private val onItemEdit: (ShoppingItem) -> Unit,
    private val onItemDelete: (ShoppingItem) -> Unit,
    private val onItemMove: (ShoppingItem) -> Unit
) : ListAdapter<ShoppingItem, ItemAdapter.ShoppingItemViewHolder>(ShoppingItemDiffCallback()) {

    fun updateShoppingMode(newShoppingMode: Boolean) {
        isShoppingMode = newShoppingMode
        notifyDataSetChanged() // Re-bind all visible items to reflect mode change
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_item, parent, false)
        return ShoppingItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShoppingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemName: TextView = itemView.findViewById(R.id.itemName)
        private val itemCheckBox: CheckBox = itemView.findViewById(R.id.itemCheckBox)
        private val itemMenuButton: ImageButton = itemView.findViewById(R.id.itemMenuButton)

        fun bind(shoppingItem: ShoppingItem) {
            itemName.text = shoppingItem.name
            itemCheckBox.setOnCheckedChangeListener(null) // Clear listener before setting new state

            if (this@ItemAdapter.isShoppingMode) {
                // SHOPPING MODE
                itemCheckBox.isChecked = shoppingItem.isChecked
                itemCheckBox.setOnCheckedChangeListener { _, isNowChecked ->
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        onItemChecked(getItem(adapterPosition), isNowChecked)
                    }
                }
                itemName.paintFlags = if (shoppingItem.isChecked) {
                    itemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    itemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                itemMenuButton.visibility = View.GONE // Typically no menu for items in shopping mode
            } else {
                // PLANNING MODE
                itemCheckBox.isChecked = shoppingItem.isPlanned
                itemCheckBox.setOnCheckedChangeListener { _, _ ->
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        onItemPlanned(getItem(adapterPosition))
                    }
                }
                itemName.paintFlags = itemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv() // No strike-through
                itemMenuButton.visibility = View.VISIBLE // Show menu in planning mode
            }

            itemMenuButton.setOnClickListener { view ->
                if (!this@ItemAdapter.isShoppingMode && adapterPosition != RecyclerView.NO_POSITION) { // Only show menu in planning mode
                    showItemPopupMenu(view, getItem(adapterPosition))
                }
            }
        }

        private fun showItemPopupMenu(anchorView: View, shoppingItem: ShoppingItem) {
            val popup = PopupMenu(itemView.context, anchorView)
            popup.menuInflater.inflate(R.menu.shopping_item_menu, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit_item -> {
                        onItemEdit(shoppingItem)
                        true
                    }
                    R.id.action_delete_item -> {
                        onItemDelete(shoppingItem)
                        true
                    }
                    R.id.action_move_item -> {
                        onItemMove(shoppingItem)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
}

// Top-level class in the same file
class ShoppingItemDiffCallback : DiffUtil.ItemCallback<ShoppingItem>() {
    override fun areItemsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
        // Compare all fields that determine if the content is the same
        return oldItem.name == newItem.name &&
               oldItem.isChecked == newItem.isChecked &&
               oldItem.isPlanned == newItem.isPlanned &&
               oldItem.sectionId == newItem.sectionId &&
               oldItem.isAdHoc == newItem.isAdHoc &&
               oldItem.orderIndex == newItem.orderIndex
    }
}
