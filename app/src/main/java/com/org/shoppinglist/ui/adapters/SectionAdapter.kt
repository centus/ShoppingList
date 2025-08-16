package com.org.shoppinglist.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.org.shoppinglist.R
import com.org.shoppinglist.data.SectionWithItems
import com.org.shoppinglist.data.ShoppingItem // Added import for ShoppingItem

class SectionAdapter(
    private var isShoppingMode: Boolean,
    private val onItemChecked: (ShoppingItem, Boolean) -> Unit,
    private val onItemEdit: (ShoppingItem, String) -> Unit, // Changed Long to ShoppingItem
    private val onItemDelete: (ShoppingItem) -> Unit,
    private val onItemMove: (ShoppingItem, Long) -> Unit, // Assuming this is for item ID and new section ID
    private val onSectionEdit: (Long, String) -> Unit,
    private val onSectionDelete: (Long) -> Unit,
    private val onAddItem: (Long) -> Unit
) : RecyclerView.Adapter<SectionAdapter.SectionViewHolder>() {

    private var sections: List<SectionWithItems> = emptyList()
    private val expandedSections = mutableSetOf<Long>()

    fun updateSections(newSections: List<SectionWithItems>) {
        sections = newSections
        notifyDataSetChanged()
    }

    fun updateShoppingMode(newShoppingMode: Boolean) {
        isShoppingMode = newShoppingMode
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_section, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    override fun getItemCount(): Int = sections.size

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sectionTitle: TextView = itemView.findViewById(R.id.sectionTitle)
        private val expandButton: ImageButton = itemView.findViewById(R.id.expandButton)
        private val editSectionButton: ImageButton = itemView.findViewById(R.id.editSectionButton)
        private val deleteSectionButton: ImageButton = itemView.findViewById(R.id.deleteSectionButton)
        private val addItemButton: ImageButton = itemView.findViewById(R.id.addItemButton)
        private val itemsRecyclerView: RecyclerView = itemView.findViewById(R.id.itemsRecyclerView)
        private val itemCountText: TextView = itemView.findViewById(R.id.itemCountText)
        private val shoppingProgressContainer: LinearLayout = itemView.findViewById(R.id.shoppingProgressContainer)
        private val checkedCountText: TextView = itemView.findViewById(R.id.checkedCountText)
        private val totalCountText: TextView = itemView.findViewById(R.id.totalCountText)
        private val completionIcon: ImageView = itemView.findViewById(R.id.completionIcon)
        private val sectionHeaderContainer: LinearLayout = itemView.findViewById(R.id.sectionHeaderContainer)

        private val itemAdapter = ItemAdapter(
            isShoppingMode = isShoppingMode, // Pass the current shopping mode
            onItemChecked = onItemChecked,
            onItemEdit = onItemEdit, // This is now (ShoppingItem, String) -> Unit
            onItemDelete = onItemDelete,
            onItemMove = onItemMove
        )

        init {
            itemsRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
            itemsRecyclerView.adapter = itemAdapter

            expandButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val sectionId = sections[adapterPosition].section.id
                    if (expandedSections.contains(sectionId)) {
                        expandedSections.remove(sectionId)
                    } else {
                        expandedSections.add(sectionId)
                    }
                    notifyItemChanged(adapterPosition)
                }
            }

            editSectionButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val section = sections[adapterPosition].section
                    onSectionEdit(section.id, section.name)
                }
            }

            deleteSectionButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val sectionId = sections[adapterPosition].section.id
                    onSectionDelete(sectionId)
                }
            }

            addItemButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val sectionId = sections[adapterPosition].section.id
                    onAddItem(sectionId)
                }
            }
        }

        fun bind(sectionWithItems: SectionWithItems) {
            val section = sectionWithItems.section
            val items = sectionWithItems.items

            sectionTitle.text = section.name

            // Calculate shopping progress
            val checkedCount = items.count { it.isChecked }
            val totalCount = items.size

            if (isShoppingMode && totalCount > 0) {
                // Show shopping progress in shopping mode
                shoppingProgressContainer.visibility = View.VISIBLE
                itemCountText.visibility = View.GONE

                checkedCountText.text = checkedCount.toString()
                totalCountText.text = totalCount.toString()

                // Change text color and background based on completion
                if (checkedCount == totalCount) {
                    checkedCountText.setTextColor(itemView.context.getColor(R.color.success_color))
                    totalCountText.setTextColor(itemView.context.getColor(R.color.success_color))
                    completionIcon.visibility = View.VISIBLE
                    // Subtle background change for completed sections
                    sectionHeaderContainer.setBackgroundColor(itemView.context.getColor(R.color.success_color))
                    sectionHeaderContainer.alpha = 0.1f
                } else {
                    checkedCountText.setTextColor(itemView.context.getColor(R.color.success_color))
                    totalCountText.setTextColor(itemView.context.getColor(R.color.text_secondary))
                    completionIcon.visibility = View.GONE
                    // Reset background for incomplete sections
                    sectionHeaderContainer.setBackgroundColor(itemView.context.getColor(R.color.section_header_color))
                    sectionHeaderContainer.alpha = 1.0f
                }
            } else {
                // Show regular item count in planning mode or empty sections
                shoppingProgressContainer.visibility = View.GONE
                itemCountText.visibility = View.VISIBLE
                itemCountText.text = "${items.size} ${itemView.context.getString(R.string.item_count)}"

                // Reset background for planning mode
                sectionHeaderContainer.setBackgroundColor(itemView.context.getColor(R.color.section_header_color))
                sectionHeaderContainer.alpha = 1.0f
            }

            val isExpanded = expandedSections.contains(section.id)
            expandButton.setImageResource(
                if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )

            itemsRecyclerView.visibility = if (isExpanded) View.VISIBLE else View.GONE

            // Show/hide edit buttons based on mode and section type
            val isDefaultSection = section.isDefault
            editSectionButton.visibility = if (isShoppingMode || isDefaultSection) View.GONE else View.VISIBLE
            deleteSectionButton.visibility = if (isShoppingMode || isDefaultSection) View.GONE else View.VISIBLE
            addItemButton.visibility = if (isShoppingMode) View.VISIBLE else View.VISIBLE // Corrected logic for addItemButton visibility

            itemAdapter.updateItems(items)
            itemAdapter.updateShoppingMode(isShoppingMode) // Ensure ItemAdapter's mode is also updated
        }
    }
}
