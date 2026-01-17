package com.org.shoppinglist.ui.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.org.shoppinglist.R
import com.org.shoppinglist.data.ShoppingItem

class ItemAdapter(
    var isShoppingMode: Boolean,
    private val onItemChecked: (ShoppingItem, Boolean) -> Unit,
    private val onItemPlanned: (ShoppingItem, Boolean) -> Unit,
    private val onItemEdit: (ShoppingItem) -> Unit,
    private val onItemDelete: (ShoppingItem) -> Unit,
    private val onItemMove: (ShoppingItem) -> Unit,
    private val onItemQuantityChanged: (ShoppingItem, Int) -> Unit,
    private val onItemDetails: (ShoppingItem) -> Unit,
    private val onItemImagePreview: (ShoppingItem) -> Unit,
    private val onItemLinkClick: (ShoppingItem) -> Unit
) : ListAdapter<ShoppingItem, ItemAdapter.ShoppingItemViewHolder>(ShoppingItemDiffCallback()) {

    fun updateShoppingMode(newShoppingMode: Boolean) {
        val oldShoppingMode = isShoppingMode
        isShoppingMode = newShoppingMode
        if (oldShoppingMode != newShoppingMode) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_MODE_CHANGED)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_item, parent, false)
        return ShoppingItemViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ShoppingItemViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_MODE_CHANGED)) {
            holder.bindShoppingMode(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: ShoppingItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShoppingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemName: TextView = itemView.findViewById(R.id.itemName)
        private val itemCheckBox: CheckBox = itemView.findViewById(R.id.itemCheckBox)
        private val itemEditButton: ImageButton = itemView.findViewById(R.id.itemEditButton)
        private val itemDeleteButton: ImageButton = itemView.findViewById(R.id.itemDeleteButton)
        private val itemMoveButton: ImageButton = itemView.findViewById(R.id.itemMoveButton)
        private val itemDetailsButton: ImageButton = itemView.findViewById(R.id.itemDetailsButton)

        private val itemQuantity: TextView = itemView.findViewById(R.id.itemQuantity)
        private val quantityEditor: LinearLayout = itemView.findViewById(R.id.quantityEditor)
        private val quantityValue: TextView = itemView.findViewById(R.id.quantityValue)
        private val increaseQuantityButton: ImageButton = itemView.findViewById(R.id.increaseQuantityButton)
        private val decreaseQuantityButton: ImageButton = itemView.findViewById(R.id.decreaseQuantityButton)
        private val itemLinkButton: ImageView = itemView.findViewById(R.id.itemLinkButton)
        private val itemImageMini: ImageView = itemView.findViewById(R.id.itemImageMini)
        private var isBinding = false

        fun bind(shoppingItem: ShoppingItem) {
            isBinding = true
            itemName.text = shoppingItem.name
            quantityValue.text = shoppingItem.quantity.toString()

            bindShoppingMode(shoppingItem)
            bindLinkButton(shoppingItem)
            isBinding = false

            itemCheckBox.setOnCheckedChangeListener { _, isNowChecked ->
                if (isBinding) {
                    return@setOnCheckedChangeListener
                }
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = getItem(adapterPosition)
                    if (isShoppingMode) {
                        onItemChecked(item, isNowChecked)
                    } else {
                        onItemPlanned(item, isNowChecked)
                    }
                }
            }

            itemName.setOnClickListener {
                itemCheckBox.toggle()
            }

            itemEditButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemEdit(getItem(adapterPosition))
                }
            }

            itemDeleteButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemDelete(getItem(adapterPosition))
                }
            }
            itemMoveButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemMove(getItem(adapterPosition))
                }
            }
            itemDetailsButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemDetails(getItem(adapterPosition))
                }
            }
            itemImageMini.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemImagePreview(getItem(adapterPosition))
                }
            }
            itemLinkButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemLinkClick(getItem(adapterPosition))
                }
            }

            increaseQuantityButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = getItem(adapterPosition)
                    onItemQuantityChanged(item, item.quantity + 1)
                }
            }

            decreaseQuantityButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = getItem(adapterPosition)
                    if (item.quantity > 1) {
                        onItemQuantityChanged(item, item.quantity - 1)
                    }
                }
            }
        }

        fun bindShoppingMode(shoppingItem: ShoppingItem) {
            if (this@ItemAdapter.isShoppingMode) {
                // SHOPPING MODE
                itemQuantity.visibility = if (shoppingItem.quantity > 1) View.VISIBLE else View.GONE
                itemQuantity.text = "x${shoppingItem.quantity}"
                quantityEditor.visibility = View.GONE

                itemCheckBox.isChecked = shoppingItem.isChecked
                itemName.paintFlags = if (shoppingItem.isChecked) {
                    itemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    itemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                itemEditButton.visibility = View.GONE
                itemDeleteButton.visibility = View.GONE
                itemMoveButton.visibility = View.GONE
                itemDetailsButton.visibility = View.GONE
                itemLinkButton.visibility = if (hasLink(shoppingItem)) View.VISIBLE else View.GONE
                bindImageMini(shoppingItem)
            } else {
                // PLANNING MODE
                itemQuantity.visibility = View.GONE
                quantityEditor.visibility = View.VISIBLE

                itemCheckBox.isChecked = shoppingItem.isPlanned
                itemName.paintFlags = itemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv() // No strike-through
                itemEditButton.visibility = View.VISIBLE
                itemDeleteButton.visibility = View.VISIBLE
                itemMoveButton.visibility = View.VISIBLE
                itemDetailsButton.visibility = View.VISIBLE
                itemImageMini.visibility = View.GONE
                itemLinkButton.visibility = View.GONE
            }
        }

        private fun bindLinkButton(shoppingItem: ShoppingItem) {
            itemLinkButton.visibility = if (this@ItemAdapter.isShoppingMode && hasLink(shoppingItem)) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        private fun bindImageMini(shoppingItem: ShoppingItem) {
            val imageUri = shoppingItem.imageUri
            if (imageUri.isNullOrBlank()) {
                itemImageMini.visibility = View.GONE
                itemImageMini.setImageDrawable(null)
                return
            }
            itemImageMini.visibility = View.VISIBLE
            itemImageMini.load(imageUri) {
                placeholder(R.drawable.ic_image)
                error(R.drawable.ic_image)
            }
        }

        private fun hasLink(shoppingItem: ShoppingItem): Boolean {
            return !shoppingItem.productLink.isNullOrBlank()
        }
    }

    companion object {
        private const val PAYLOAD_MODE_CHANGED = "PAYLOAD_MODE_CHANGED"
    }
}

class ShoppingItemDiffCallback : DiffUtil.ItemCallback<ShoppingItem>() {
    override fun areItemsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
        return oldItem == newItem
    }
}
