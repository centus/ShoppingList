package com.org.shoppinglist.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.org.shoppinglist.R
import com.org.shoppinglist.data.Section
import com.org.shoppinglist.data.SectionWithItems
import com.org.shoppinglist.data.ShoppingItem
import com.google.android.material.card.MaterialCardView
import com.org.shoppinglist.ui.gone
import com.org.shoppinglist.ui.visible

class SectionAdapter(
    private val context: Context,
    private var isShoppingMode: Boolean,
    private val onAddItemToSection: (Long) -> Unit,
    private val onSectionEdit: (Section) -> Unit,
    private val onSectionDelete: (Section) -> Unit,
    private val onSectionExpanded: (Section, Boolean) -> Unit,
    private val onItemChecked: (ShoppingItem, Boolean) -> Unit,
    private val onItemPlanned: (ShoppingItem, Boolean) -> Unit,
    private val onItemEdit: (ShoppingItem) -> Unit,
    private val onItemDelete: (ShoppingItem) -> Unit,
    private val onItemMove: (ShoppingItem) -> Unit,
    private val onItemQuantityChanged: (ShoppingItem, Int) -> Unit,
    private val onItemDetails: (ShoppingItem) -> Unit,
    private val onItemImagePreview: (ShoppingItem) -> Unit,
    private val onItemLinkClick: (ShoppingItem) -> Unit
) : ListAdapter<SectionWithItems, SectionAdapter.SectionViewHolder>(SectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_section, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: SectionViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_MODE_CHANGED)) {
            holder.bindShoppingMode(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateShoppingMode(newShoppingMode: Boolean) {
        val oldShoppingMode = isShoppingMode
        isShoppingMode = newShoppingMode
        if (oldShoppingMode != newShoppingMode) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_MODE_CHANGED)
        }
    }

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sectionTitle: TextView = itemView.findViewById(R.id.sectionTitle)
        private val itemsRecyclerView: RecyclerView = itemView.findViewById(R.id.itemsRecyclerView)
        private val expandButton: ImageButton = itemView.findViewById(R.id.expandButton)
        private val sectionHeaderContainer: LinearLayout = itemView.findViewById(R.id.sectionHeaderContainer)

        private val addItemToSectionButton: ImageButton = itemView.findViewById(R.id.addItemToSectionButton)
        private val editSectionButton: ImageButton = itemView.findViewById(R.id.editSectionButton)
        private val deleteSectionButton: ImageButton = itemView.findViewById(R.id.deleteSectionButton)

        private val shoppingProgressContainer: LinearLayout = itemView.findViewById(R.id.shoppingProgressContainer)
        private val checkedCountText: TextView = itemView.findViewById(R.id.checkedCountText)
        private val totalCountText: TextView = itemView.findViewById(R.id.totalCountText)
        private val completionIcon: View = itemView.findViewById(R.id.completionIcon)
        private val itemCountText: TextView = itemView.findViewById(R.id.itemCountText)

        private val itemAdapter: ItemAdapter

        init {
            itemAdapter = ItemAdapter(
                isShoppingMode = isShoppingMode,
                onItemChecked = { item, isChecked -> onItemChecked(item, isChecked) },
                onItemPlanned = { item, isPlanned -> onItemPlanned(item, isPlanned) },
                onItemEdit = { item -> onItemEdit(item) },
                onItemDelete = { item -> onItemDelete(item) },
                onItemMove = { item -> onItemMove(item) },
                onItemQuantityChanged = { item, quantity -> onItemQuantityChanged(item, quantity) },
                onItemDetails = { item -> onItemDetails(item) },
                onItemImagePreview = { item -> onItemImagePreview(item) },
                onItemLinkClick = { item -> onItemLinkClick(item) }
            )
            itemsRecyclerView.adapter = itemAdapter
            itemsRecyclerView.layoutManager = LinearLayoutManager(itemView.context)

            expandButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val currentSectionWithItems = getItem(adapterPosition)
                    val newExpandedState = !currentSectionWithItems.section.isExpanded
                    onSectionExpanded(currentSectionWithItems.section, newExpandedState)
                }
            }
            sectionHeaderContainer.setOnClickListener { 
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val currentSectionWithItems = getItem(adapterPosition)
                    val newExpandedState = !currentSectionWithItems.section.isExpanded
                    onSectionExpanded(currentSectionWithItems.section, newExpandedState)
                }
            }
            addItemToSectionButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onAddItemToSection(getItem(adapterPosition).section.id)
                }
            }
            editSectionButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onSectionEdit(getItem(adapterPosition).section)
                }
            }
            deleteSectionButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onSectionDelete(getItem(adapterPosition).section)
                }
            }
        }

        fun bind(sectionWithItems: SectionWithItems) {
            sectionTitle.text = sectionWithItems.section.name
            itemsRecyclerView.visibility = if (sectionWithItems.section.isExpanded) View.VISIBLE else View.GONE
            expandButton.setImageResource(
                if (sectionWithItems.section.isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )

            val sectionCard = itemView as? MaterialCardView
            if (sectionWithItems.section.isDefault) {
                sectionCard?.strokeColor = ContextCompat.getColor(context, R.color.default_section_stroke_color)
                sectionCard?.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.default_section_stroke_width)
            } else {
                sectionCard?.strokeWidth = 0
            }

            itemAdapter.submitList(sectionWithItems.items)

            bindShoppingMode(sectionWithItems)
        }

        fun bindShoppingMode(sectionWithItems: SectionWithItems) {
            itemAdapter.updateShoppingMode(isShoppingMode)

            if (!isShoppingMode && !sectionWithItems.section.isDefault) {
                editSectionButton.visible()
                deleteSectionButton.visible()
            } else {
                editSectionButton.gone()
                deleteSectionButton.gone()
            }
            addItemToSectionButton.visible()

            if (isShoppingMode) {
                shoppingProgressContainer.visible()
                itemCountText.gone()
                val checkedCount = sectionWithItems.items.count { it.isChecked }
                val totalCount = sectionWithItems.items.size
                checkedCountText.text = checkedCount.toString()
                totalCountText.text = totalCount.toString()
                completionIcon.isVisible = totalCount > 0 && checkedCount == totalCount

                if (totalCount > 0 && checkedCount == totalCount) {
                    checkedCountText.setTextColor(ContextCompat.getColor(context, R.color.success_color))
                } else {
                    checkedCountText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }

            } else { // Planning mode
                shoppingProgressContainer.gone()
                itemCountText.visible()
                itemCountText.text = context.resources.getQuantityString(
                    R.plurals.item_count_placeholder,
                    sectionWithItems.items.size,
                    sectionWithItems.items.size
                )
            }
        }
    }

    companion object {
        private const val PAYLOAD_MODE_CHANGED = "PAYLOAD_MODE_CHANGED"
    }
}

class SectionDiffCallback : DiffUtil.ItemCallback<SectionWithItems>() {
    override fun areItemsTheSame(oldItem: SectionWithItems, newItem: SectionWithItems): Boolean {
        return oldItem.section.id == newItem.section.id
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: SectionWithItems, newItem: SectionWithItems): Boolean {
        return oldItem.section.isExpanded == newItem.section.isExpanded && oldItem.items == newItem.items
    }
}
