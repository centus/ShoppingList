package com.org.shoppinglist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.org.shoppinglist.data.*
import com.org.shoppinglist.databinding.ActivityMainBinding
import com.org.shoppinglist.ui.ShoppingViewModel
import com.org.shoppinglist.ui.ShoppingViewModelFactory
import com.org.shoppinglist.ui.adapters.SectionAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ShoppingViewModel
    private lateinit var sectionAdapter: SectionAdapter

    // For Import/Export
    private val exportListLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Handle result if needed, though ACTION_SEND often doesn't give a direct result
    }
    private val importListLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            importListFromUri(it)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupDatabase()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val resetShoppingItem = menu.findItem(R.id.action_reset_shopping_list)
        resetShoppingItem?.isVisible = viewModel.isShoppingMode.value ?: false
        return super.onPrepareOptionsMenu(menu)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reset_all -> {
                showResetAllConfirmationDialog()
                true
            }
            R.id.action_share_list -> {
                exportShoppingList()
                true
            }
            R.id.action_import_list -> {
                importShoppingList()
                true
            }
            R.id.action_reset_shopping_list -> {
                showResetShoppingConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showResetShoppingConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_reset_shopping_list_title))
            .setMessage(getString(R.string.confirm_reset_shopping_list_message))
            .setPositiveButton(getString(R.string.reset_action)) { _, _ ->
                viewModel.performFullShoppingReset()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }


    private fun showResetAllConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_reset_all_title))
            .setMessage(getString(R.string.confirm_reset_all_message))
            .setPositiveButton(R.string.delete) { _, _ -> 
                viewModel.resetAllItemCheckedStates()
            }
            .setNegativeButton(getString(R.string.cancel), null) // Corrected this line
            .show()
    }

    private fun setupDatabase() {
        // Repository is instantiated here but should not be a member field of MainActivity
        val localRepository = ShoppingRepository(
            ShoppingDatabase.getDatabase(this, CoroutineScope(SupervisorJob() + Dispatchers.IO)).shoppingDao(),
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
        val factory = ShoppingViewModelFactory(localRepository) // Pass localRepository
        viewModel = ViewModelProvider(this, factory)[ShoppingViewModel::class.java]
    }

    private fun setupRecyclerView() {
        sectionAdapter = SectionAdapter(
            isShoppingMode = false, 
            onItemChecked = { item, isChecked ->
                viewModel.toggleItemChecked(item, isChecked)
            },
            onItemEdit = { item, currentName ->
                showEditItemDialog(item, currentName)
            },
            onItemDelete = { item ->
                showDeleteItemConfirmation(item)
            },
            onItemMove = { item, currentItemSectionId ->
                val currentSection = viewModel.allSectionsWithItems.value
                    ?.firstOrNull { it.section.id == currentItemSectionId }?.section

                if (currentSection != null) {
                    showMoveItemDialog(item, currentSection)
                } else {
                    Log.e("MainActivity", "Current section with ID $currentItemSectionId not found for item ${item.name}")
                }
            },
            onSectionEdit = { sectionId, currentName ->
                 val sectionToEdit = viewModel.allSectionsWithItems.value
                    ?.map { it.section }
                    ?.find { it.id == sectionId }
                if(sectionToEdit != null) {
                    showEditSectionDialog(sectionToEdit, currentName)
                }
            },
            onSectionDelete = { sectionId ->
                val sectionToDelete = viewModel.allSectionsWithItems.value
                    ?.map { it.section }
                    ?.find { it.id == sectionId }
                if(sectionToDelete != null) {
                    showDeleteSectionConfirmation(sectionToDelete)
                }
            },
            onAddItem = { sectionId ->
                showAddItemDialog(sectionId)
            }
        )

        binding.sectionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = sectionAdapter
        }
    }

    private fun setupObservers() {
        viewModel.allSectionsWithItems.observe(this) { sections ->
            val logOutput = sections.joinToString(separator = "\n") { sectionWithItems ->
                val itemNames = sectionWithItems.items.joinToString(", ") { shoppingItem ->
                    "'${shoppingItem.name}' (id: ${shoppingItem.id}, secId: ${shoppingItem.sectionId})"
                }
                "Section '${sectionWithItems.section.name}' (id: ${sectionWithItems.section.id}): [${itemNames}]"
            }
            Log.d("MainActivity", "allSectionsWithItems observer triggered. Sections:\n$logOutput")

            sectionAdapter.updateSections(sections)
        }


        viewModel.uncheckedItemsCount.observe(this) { count ->
            binding.uncheckedCountText.text = "$count ${getString(R.string.items_left)}"
        }

        viewModel.checkedItemsCount.observe(this) { count ->
            binding.checkedCountText.text = "$count ${getString(R.string.items_done)}"
        }

        viewModel.isShoppingMode.observe(this) { isShoppingMode ->
            updateUIForMode(isShoppingMode)
            invalidateOptionsMenu() 
        }
    }

    private fun setupClickListeners() {
        binding.modeToggleButton.setOnClickListener {
            viewModel.toggleShoppingMode()
        }

        binding.addSectionFab.setOnClickListener {
            showAddSectionDialog()
        }
    }

    private fun updateUIForMode(isShoppingMode: Boolean) {
        binding.modeToggleButton.text = if (isShoppingMode) getString(R.string.shopping_mode) else getString(R.string.planning_mode)
        binding.addSectionFab.visibility = if (isShoppingMode) android.view.View.GONE else android.view.View.VISIBLE
        sectionAdapter.updateShoppingMode(isShoppingMode)
    }

    private fun showAddSectionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputEditText)
        editText.hint = getString(R.string.enter_section_name)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_section))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val sectionName = editText.text.toString().trim()
                if (sectionName.isNotEmpty()) {
                    viewModel.addSection(sectionName)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showEditSectionDialog(section: Section, currentName: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputEditText)
        editText.setText(currentName)
        editText.hint = getString(R.string.enter_section_name)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_section))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.updateSection(section, newName)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteSectionConfirmation(section: Section) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_section))
            .setMessage(getString(R.string.delete_section_message, section.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteSection(section)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showAddItemDialog(sectionId: Long, isAdHoc: Boolean = false) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputEditText)
        editText.hint = getString(R.string.enter_item_name)

        val title = if (isAdHoc && viewModel.isShoppingMode.value == true) getString(R.string.add_ad_hoc_item) else getString(R.string.add_item)


        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val itemName = editText.text.toString().trim()
                if (itemName.isNotEmpty()) {
                    val adHocFlag = viewModel.isShoppingMode.value == true && isAdHoc
                    viewModel.addItem(itemName, sectionId, adHocFlag)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showEditItemDialog(item: ShoppingItem, currentName: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputEditText)
        editText.setText(currentName)
        editText.hint = getString(R.string.enter_item_name)

        AlertDialog.Builder(this) // Corrected this line
            .setTitle(getString(R.string.edit_item))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.updateItem(item, newName)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteItemConfirmation(item: ShoppingItem) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_item))
            .setMessage(getString(R.string.delete_item_message, item.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteItem(item)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showMoveItemDialog(item: ShoppingItem, currentSection: Section) {
        val allSections = viewModel.allSectionsWithItems.value?.map { it.section } ?: emptyList()
        val availableSectionsToMoveTo = allSections.filter { it.id != currentSection.id && !it.isDefault }

        if (availableSectionsToMoveTo.isNotEmpty()) {
            val sectionNames = availableSectionsToMoveTo.map { it.name }.toTypedArray()
            val currentSectionIndex = -1 

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.move_item_to_section, item.name))
                .setSingleChoiceItems(sectionNames, currentSectionIndex) { dialog, which ->
                    val selectedNewSection = availableSectionsToMoveTo[which]
                    viewModel.moveItemToSection(item, selectedNewSection.id)
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } else {
             android.widget.Toast.makeText(this, getString(R.string.no_other_sections_to_move), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportShoppingList() {
        lifecycleScope.launch {
            val sectionsWithItems = viewModel.allSectionsWithItems.value ?: return@launch
            val exportData = sectionsWithItems.map { swi ->
                SimpleSection(swi.section.name, swi.items.map { SimpleItem(it.name) })
            }
            val gson = Gson()
            val jsonString = gson.toJson(exportData)

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, jsonString)
                type = "text/plain"
            }
            exportListLauncher.launch(Intent.createChooser(shareIntent, getString(R.string.action_share_list)))
        }
    }

    private fun importShoppingList() {
        importListLauncher.launch("text/plain")
    }

    private fun importListFromUri(uri: android.net.Uri) {
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
                val jsonString = stringBuilder.toString()
                Log.d("MainActivity", "Imported JSON: $jsonString")

                val typeToken = object : TypeToken<List<SimpleSection>>() {}.type
                val importedSections: List<SimpleSection> = Gson().fromJson(jsonString, typeToken)

                viewModel.importShoppingListData(importedSections) 

                Log.d("MainActivity", "Import process initiated via ViewModel.")

            } catch (e: Exception) {
                Log.e("MainActivity", "Error importing list", e)
                val errorMessage = "Error importing list: ${e.message}" 
                android.widget.Toast.makeText(this@MainActivity, errorMessage, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}

