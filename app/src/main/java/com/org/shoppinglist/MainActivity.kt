package com.org.shoppinglist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
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
import java.io.File
import java.io.InputStreamReader
import java.io.FileOutputStream
// Removed: import kotlin.concurrent.write // This import seemed unused and potentially problematic
import java.text.SimpleDateFormat // ADDED for timestamp
import java.util.Date             // ADDED for timestamp
import java.util.Locale           // ADDED for timestamp

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ShoppingViewModel
    private lateinit var sectionAdapter: SectionAdapter
    private var pendingImageItem: ShoppingItem? = null
    private var pendingImagePreview: android.widget.ImageView? = null
    private var pendingImageUriUpdate: ((String?) -> Unit)? = null

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

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val item = pendingImageItem ?: return@registerForActivityResult
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: SecurityException) {
                Log.w("MainActivity", "Failed to take persistable permission for image Uri", e)
            }
            viewModel.updateItemImage(item, uri.toString())
            pendingImageUriUpdate?.invoke(uri.toString())
            pendingImagePreview?.apply {
                visibility = android.view.View.VISIBLE
                load(uri)
            }
        }
        pendingImageItem = null
        pendingImagePreview = null
        pendingImageUriUpdate = null
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

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent?.action == Intent.ACTION_VIEW && intent.type == "text/plain") {
            intent.data?.let { uri ->
                importListFromUri(uri, true)
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val resetShoppingItem = menu.findItem(R.id.action_reset_shopping_list)
        resetShoppingItem?.isVisible = viewModel.isShoppingMode.value ?: false
        val resetShoppingList = menu.findItem(R.id.action_reset_all)
        resetShoppingList?.isVisible = !(viewModel.isShoppingMode.value ?: false)
        return super.onPrepareOptionsMenu(menu)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share_list -> {
                exportShoppingList()
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
            R.id.action_reset_all -> {
                showResetAllConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun showResetAllConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_reset_all_title))
            .setMessage(getString(R.string.confirm_reset_all_message))
            .setPositiveButton(getString(R.string.reset_action)) { _, _ ->
                viewModel.performFullShoppingReset()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showResetShoppingConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_reset_shopping_list_title))
            .setMessage(getString(R.string.confirm_reset_shopping_list_message))
            .setPositiveButton(getString(R.string.reset_action)) { _, _ ->
                viewModel.resetAllItemCheckedStates()
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
            onItemPlanned = { item, isPlanned ->
                viewModel.setItemPlanned(item, isPlanned)
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
            onItemQuantityChanged = { item, newQuantity ->
                viewModel.updateItemQuantity(item, newQuantity)
            },
            onItemDetails = { item ->
                showItemDetailsDialog(item, viewModel.isShoppingMode.value ?: false)
            },
            onItemImagePreview = { item ->
                showItemImagePreviewDialog(item)
            },
            onItemLinkClick = { item ->
                openItemLink(item)
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
                    "'${shoppingItem.name}' (id: ${shoppingItem.id}, planned: ${shoppingItem.isPlanned}, checked: ${shoppingItem.isChecked})"
                }
                Log.d("MainActivity", "Section '${sectionWithItems.section.name}': [$itemNames]")
            }
            sectionAdapter.submitList(sections)
        }

        viewModel.uncheckedItemsCount.observe(this) { count ->
            binding.uncheckedCountText.text = getString(R.string.items_left_placeholder, count)
        }

        viewModel.checkedItemsCount.observe(this) { count ->
            binding.checkedCountText.text = getString(R.string.items_done_placeholder, count)
        }

        viewModel.isShoppingMode.observe(this) { isShoppingMode ->
            Log.d("MainActivity", "isShoppingMode observer: $isShoppingMode")
            binding.modeToggleButton.text = if (isShoppingMode) getString(R.string.shopping_mode) else getString(R.string.planning_mode)
            binding.addSectionFab.visibility = if (isShoppingMode) android.view.View.GONE else android.view.View.VISIBLE
            sectionAdapter.updateShoppingMode(isShoppingMode)
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

        val title = if (isAdHoc) getString(R.string.add_ad_hoc_item) else getString(R.string.add_item)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val itemName = editText.text.toString().trim()
                if (itemName.isNotEmpty()) {
                     viewModel.addItem(itemName, sectionId, isAdHoc = isAdHoc)
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
            .setNeutralButton(getString(R.string.item_details_action)) { _, _ ->
                showItemDetailsDialog(item, isShoppingMode = false)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showItemDetailsDialog(item: ShoppingItem, isShoppingMode: Boolean) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_item_details, null)
        val imagePreview = dialogView.findViewById<android.widget.ImageView>(R.id.itemImagePreview)
        val attachImageButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.attachImageButton)
        val removeImageButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.removeImageButton)
        val openLinkButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.openLinkButton)
        val linkInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.linkEditText)

        var currentImageUri = item.imageUri
        var currentLink = item.productLink
        val hasImage = !currentImageUri.isNullOrBlank()
        val hasLink = !currentLink.isNullOrBlank()

        if (hasImage) {
            imagePreview.visibility = android.view.View.VISIBLE
            imagePreview.load(item.imageUri)
            removeImageButton.visibility = android.view.View.VISIBLE
        } else {
            imagePreview.visibility = android.view.View.GONE
            removeImageButton.visibility = android.view.View.GONE
        }

        linkInput.setText(currentLink ?: "")
        linkInput.isEnabled = !isShoppingMode

        attachImageButton.visibility = if (isShoppingMode) android.view.View.GONE else android.view.View.VISIBLE
        removeImageButton.visibility = if (isShoppingMode || !hasImage) android.view.View.GONE else android.view.View.VISIBLE

        openLinkButton.visibility = if (hasLink) android.view.View.VISIBLE else android.view.View.GONE

        attachImageButton.setOnClickListener {
            pendingImageItem = item
            pendingImagePreview = imagePreview
            pendingImageUriUpdate = { updatedUri -> currentImageUri = updatedUri }
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        removeImageButton.setOnClickListener {
            currentImageUri = null
            viewModel.updateItemImage(item, null)
            pendingImageUriUpdate?.invoke(null)
            imagePreview.visibility = android.view.View.GONE
            removeImageButton.visibility = android.view.View.GONE
        }

        openLinkButton.setOnClickListener {
            val link = normalizeLink(linkInput.text?.toString())
            if (link == null) {
                Toast.makeText(this@MainActivity, getString(R.string.link_missing_message), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Patterns.WEB_URL.matcher(link).matches()) {
                Toast.makeText(this@MainActivity, getString(R.string.invalid_link_message), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.item_details_title))
            .setView(dialogView)

        if (isShoppingMode) {
            builder.setPositiveButton(getString(R.string.cancel), null)
        } else {
            builder.setPositiveButton(getString(R.string.save)) { _, _ ->
                val rawLink = linkInput.text?.toString().orEmpty()
                val normalized = normalizeLink(rawLink)
                if (normalized != null && !Patterns.WEB_URL.matcher(normalized).matches()) {
                    Toast.makeText(this@MainActivity, getString(R.string.invalid_link_message), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                currentLink = normalized
                viewModel.updateItemDetails(item, currentImageUri, currentLink)
            }
            builder.setNegativeButton(getString(R.string.cancel), null)
        }

        val dialog = builder.show()
        dialog.setOnDismissListener {
            pendingImageItem = null
            pendingImagePreview = null
            pendingImageUriUpdate = null
        }
    }

    private fun showItemImagePreviewDialog(item: ShoppingItem) {
        val imageUri = item.imageUri ?: return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_item_image, null)
        val imageView = dialogView.findViewById<android.widget.ImageView>(R.id.itemFullImageView)
        imageView.load(imageUri) {
            placeholder(R.drawable.ic_image)
            error(R.drawable.ic_image)
        }
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.cancel), null)
            .show()
    }

    private fun openItemLink(item: ShoppingItem) {
        val link = normalizeLink(item.productLink)
        if (link == null) {
            Toast.makeText(this@MainActivity, getString(R.string.link_missing_message), Toast.LENGTH_SHORT).show()
            return
        }
        if (!Patterns.WEB_URL.matcher(link).matches()) {
            Toast.makeText(this@MainActivity, getString(R.string.invalid_link_message), Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
    }

    private fun normalizeLink(rawLink: String?): String? {
        val trimmed = rawLink?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
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
            val sectionsWithItems = viewModel.displayedList.value ?: run {
                Toast.makeText(this@MainActivity, "List is empty, nothing to share.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (sectionsWithItems.isEmpty()) {
                Toast.makeText(this@MainActivity, "List is empty, nothing to share.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val exportData = sectionsWithItems.map { swi ->
                SimpleSection(
                    swi.section.name,
                    swi.items.map {
                        SimpleItem(
                            it.name,
                            it.isPlanned,
                            it.quantity,
                            it.imageUri,
                            it.productLink
                        )
                    }
                )
            }
            val gson = Gson()
            val jsonString = gson.toJson(exportData)

            if (jsonString.isBlank()) {
                Toast.makeText(this@MainActivity, "Failed to generate JSON data.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                // Generate timestamp for filename
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val timestamp = sdf.format(Date())
                val fileName = "shopping_list_export_$timestamp.txt" // New filename with timestamp and .txt extension

                val cacheSubDir = File(cacheDir, "shared_lists")
                cacheSubDir.mkdirs()
                val file = File(cacheSubDir, fileName) // Use the new dynamic filename

                FileOutputStream(file).use {
                    it.write(jsonString.toByteArray()) // Still writing the JSON string as content
                }

                val fileUri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "${applicationContext.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    type = "text/plain" // CHANGED: MIME type to text/plain for .txt file
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                exportListLauncher.launch(Intent.createChooser(shareIntent, getString(R.string.action_share_list)))

            } catch (e: Exception) {
                Log.e("MainActivityExport", "Error exporting list to TXT file", e) // Updated log tag
                Toast.makeText(this@MainActivity, "Error exporting list: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
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
