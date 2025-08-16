package com.org.shoppinglist.ui.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
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
    private val onItemPlanned: (ShoppingItem) -> Unit,
    private val onItemEdit: (ShoppingItem) -> Unit,
    private val onItemDelete: (ShoppingItem) -> Unit,
    private val onItemMove: (ShoppingItem) -> Unit
) : ListAdapter<SectionWithItems, SectionAdapter.SectionViewHolder>(SectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_section, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateShoppingMode(newShoppingMode: Boolean) {
        isShoppingMode = newShoppingMode
        notifyDataSetChanged() // Consider more granular updates if performance is an issue
    }


    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sectionTitle: TextView = itemView.findViewById(R.id.sectionTitle)
        private val itemsRecyclerView: RecyclerView = itemView.findViewById(R.id.itemsRecyclerView)
        private val expandButton: ImageButton = itemView.findViewById(R.id.expandButton)
        private val sectionHeaderContainer: LinearLayout = itemView.findViewById(R.id.sectionHeaderContainer)

        // Updated buttons
        private val addItemToSectionButton: ImageButton = itemView.findViewById(R.id.addItemToSectionButton)
        private val editSectionButton: ImageButton = itemView.findViewById(R.id.editSectionButton)
        private val deleteSectionButton: ImageButton = itemView.findViewById(R.id.deleteSectionButton)

        private val shoppingProgressContainer: LinearLayout = itemView.findViewById(R.id.shoppingProgressContainer)
        private val checkedCountText: TextView = itemView.findViewById(R.id.checkedCountText)
        private val totalCountText: TextView = itemView.findViewById(R.id.totalCountText)
        private val completionIcon: View = itemView.findViewById(R.id.completionIcon)
        private val itemCountText: TextView = itemView.findViewById(R.id.itemCountText)


        private lateinit var section: Section
        private lateinit var itemAdapter: ItemAdapter // Using the external ItemAdapter

        init {
            itemAdapter = ItemAdapter( // Instantiate the external adapter
                isShoppingMode = isShoppingMode,
                onItemChecked = { item, isChecked -> onItemChecked(item, isChecked) },
                onItemPlanned = { item -> onItemPlanned(item) },
                onItemEdit = { item -> onItemEdit(item) },
                onItemDelete = { item -> onItemDelete(item) },
                onItemMove = { item -> onItemMove(item) }
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
            sectionHeaderContainer.setOnClickListener { // << CLICK LISTENER MOVED HERE
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
            this.section = sectionWithItems.section
            sectionTitle.text = section.name
            itemsRecyclerView.visibility = if (section.isExpanded) View.VISIBLE else View.GONE
            expandButton.setImageResource(
                if (section.isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )

            val sectionCard = itemView as? MaterialCardView
            if (section.isDefault) {
                sectionCard?.strokeColor = ContextCompat.getColor(context, R.color.default_section_stroke_color)
                sectionCard?.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.default_section_stroke_width)
            } else {
                sectionCard?.strokeWidth = 0
            }


            itemAdapter.updateShoppingMode(isShoppingMode) // Ensure inner adapter mode is also updated
            itemAdapter.submitList(sectionWithItems.items)

            // Update button visibility based on mode and if it's a default section
            if (!isShoppingMode && !section.isDefault) {
                editSectionButton.visible()
                deleteSectionButton.visible()
            } else {
                editSectionButton.gone()
                deleteSectionButton.gone()
            }
            // addItemToSectionButton is always visible for now, or its visibility could be tied to !isShoppingMode
            addItemToSectionButton.visible()


            // Progress and item count display logic
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
                    // Use a default or less prominent color if not fully complete
                    // For now, let's keep it success_color or define a different one for partial
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
}


// Top-level class in the same file as SectionAdapter
class SectionDiffCallback : DiffUtil.ItemCallback<SectionWithItems>() {
    override fun areItemsTheSame(oldItem: SectionWithItems, newItem: SectionWithItems): Boolean {
        return oldItem.section.id == newItem.section.id
    }

    override fun areContentsTheSame(oldItem: SectionWithItems, newItem: SectionWithItems): Boolean {
        // Explicitly check if the expansion state is different.
        // If it is, the content is considered different for UI purposes.
        if (oldItem.section.isExpanded != newItem.section.isExpanded) {
            return false
        }
        // If expansion state is the same, then compare other relevant properties
        // that define the "content" of the section for display.
        return oldItem.section.name == newItem.section.name &&
               oldItem.items == newItem.items
    }
}
