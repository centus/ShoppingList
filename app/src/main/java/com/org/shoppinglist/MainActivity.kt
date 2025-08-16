package com.org.shoppinglist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
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

    private val exportListLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // ACTION_SEND often doesn't give a direct result, but one could log or toast here if desired
    }
    private val importJsonLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            importListFromUri(it, false) // false indicates it's a JSON import
        }
    }

    private val importTxtLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            importListFromUri(it, true) // true indicates it's a TXT import
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
            R.id.action_import_list_json -> {
                importShoppingListJson()
                true
            }
            R.id.action_import_list_txt -> {
                importShoppingListTxt()
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
                viewModel.performFullShoppingReset()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun setupDatabase() {
        val localRepository = ShoppingRepository(
            ShoppingDatabase.getDatabase(this, CoroutineScope(SupervisorJob() + Dispatchers.IO)).shoppingDao()
        )
        val factory = ShoppingViewModelFactory(localRepository)
        viewModel = ViewModelProvider(this, factory)[ShoppingViewModel::class.java]
    }

    private fun setupRecyclerView() {
        sectionAdapter = SectionAdapter(
            context = this@MainActivity,
            isShoppingMode = viewModel.isShoppingMode.value ?: false,
            onItemChecked = { item, isChecked ->
                viewModel.toggleItemChecked(item, isChecked)
            },
            onItemPlanned = { item ->
                viewModel.toggleItemPlanned(item)
            },
            onItemEdit = { item ->
                showEditItemDialog(item)
            },
            onItemDelete = { item ->
                showDeleteItemConfirmationDialog(item)
            },
            onItemMove = { item ->
                showMoveItemDialog(item)
            },
            onSectionEdit = { section ->
                showEditSectionDialog(section)
            },
            onSectionDelete = { section ->
                showDeleteSectionConfirmationDialog(section)
            },
            onSectionExpanded = { section, isExpanded ->
                viewModel.updateSectionExpansionState(section, isExpanded)
            },
            onAddItemToSection = { sectionId ->
                showAddItemDialog(sectionId)
            }
        )

        binding.sectionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = sectionAdapter
            itemAnimator = null
        }
    }

    private fun setupObservers() {
        viewModel.displayedList.observe(this) { sections ->
            Log.d("MainActivity", "displayedList observer triggered. Section count: ${sections.size}")
            sections.forEach { sectionWithItems ->
                val itemNames = sectionWithItems.items.joinToString(", ") { shoppingItem ->
                    "\'${shoppingItem.name}\' (id: ${shoppingItem.id}, planned: ${shoppingItem.isPlanned}, checked: ${shoppingItem.isChecked})"
                }
                Log.d("MainActivity", "Section \'${sectionWithItems.section.name}\': [$itemNames]")
            }
            sectionAdapter.submitList(sections)
        }

        viewModel.uncheckedItemsCount.observe(this) { count ->
            binding.uncheckedCountText.text = "$count ${getString(R.string.items_left)}"
        }

        viewModel.checkedItemsCount.observe(this) { count ->
            binding.checkedCountText.text = "$count ${getString(R.string.items_done)}"
        }

        viewModel.isShoppingMode.observe(this) { isShoppingMode ->
            Log.d("MainActivity", "isShoppingMode observer: $isShoppingMode")
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

    private fun showEditSectionDialog(section: Section) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputEditText)
        editText.setText(section.name)
        editText.hint = getString(R.string.enter_section_name)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_section))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.updateSectionName(section, newName)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteSectionConfirmationDialog(section: Section) {
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
                     viewModel.addItem(itemName, sectionId, isAdHoc = (isAdHoc && viewModel.isShoppingMode.value == true))
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showEditItemDialog(item: ShoppingItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputEditText)
        editText.setText(item.name)
        editText.hint = getString(R.string.enter_item_name)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_item))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.updateItemName(item, newName)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteItemConfirmationDialog(item: ShoppingItem) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_item))
            .setMessage(getString(R.string.delete_item_message, item.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteItem(item)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showMoveItemDialog(item: ShoppingItem) {
        lifecycleScope.launch {
            val allSections = viewModel.getAllSectionsNonLiveData()
            val currentSection = allSections.firstOrNull { it.id == item.sectionId }

            if (currentSection == null) {
                Toast.makeText(this@MainActivity, "Error: Current section not found for item.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val availableSectionsToMoveTo = allSections.filter { it.id != currentSection.id && !it.isDefault }

            if (availableSectionsToMoveTo.isNotEmpty()) {
                val sectionNames = availableSectionsToMoveTo.map { it.name }.toTypedArray()

                AlertDialog.Builder(this@MainActivity)
                    .setTitle(getString(R.string.move_item_to_section, item.name))
                    .setSingleChoiceItems(sectionNames, -1) { dialog, which ->
                        val selectedNewSection = availableSectionsToMoveTo[which]
                        viewModel.moveItemToSection(item, selectedNewSection.id)
                        dialog.dismiss()
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.no_other_sections_to_move), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportShoppingList() {
        lifecycleScope.launch {
            val sectionsWithItems = viewModel.displayedList.value ?: return@launch
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

    private fun importShoppingListJson() {
        importJsonLauncher.launch("application/json")
    }

    private fun importShoppingListTxt() {
        importTxtLauncher.launch("text/plain")
    }

    // Consolidated import logic
    private fun importListFromUri(uri: android.net.Uri, isTxtImport: Boolean) {
        lifecycleScope.launch {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val jsonString = reader.readText()
                        Log.d("MainActivity", "Imported content (isTxt: $isTxtImport): $jsonString")

                        // Both TXT and JSON imports will now use the same full overwrite logic
                        val typeToken = object : TypeToken<List<SimpleSection>>() {}.type
                        val importedSections: List<SimpleSection> = Gson().fromJson(jsonString, typeToken)
                        viewModel.importShoppingListData(importedSections)

                        val toastMessage = if (isTxtImport) "TXT List imported successfully!" else "JSON List imported successfully!"
                        Toast.makeText(this@MainActivity, toastMessage, Toast.LENGTH_SHORT).show()
                        Log.d("MainActivity", "Import process initiated via ViewModel.")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error importing list", e)
                Toast.makeText(this@MainActivity, "Error importing list: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

