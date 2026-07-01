package com.jv.loyaltycardswallet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.jv.loyaltycardswallet.databinding.ActivityMainBinding
import com.jv.loyaltycardswallet.databinding.DialogAddCardBinding
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cardViewModel: CardViewModel
    private lateinit var adapter: CardAdapter
    private var currentDialogBinding: DialogAddCardBinding? = null

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        val scannedValue = result.contents
        if (scannedValue != null) {
            currentDialogBinding?.let { binding ->
                binding.editTextCardNumber.setText(scannedValue)
                // Also trigger barcode update if visible
                if (binding.imageViewBarcode.visibility == View.VISIBLE) {
                    updateBarcodePreview(scannedValue, binding)
                }
            } ?: run {
                Toast.makeText(this, "Scan result: $scannedValue", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importCards(it) }
    }

    private val backupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { saveBackup(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView
        adapter = CardAdapter { card -> onCardClick(card) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Setup ViewModel
        val repository = CardRepository(CardDatabase.getDatabase(this).cardDao())
        cardViewModel = ViewModelProvider(
            this,
            CardViewModelFactory(repository)
        )[CardViewModel::class.java]

        cardViewModel.allCards.observe(this) { cards ->
            cards?.let { adapter.submitList(it) }
        }

        // Setup FAB
        binding.fab.setOnClickListener { showAddCardDialog() }

        binding.buttonBackup.setOnClickListener { backupCards() }
        binding.buttonImport.setOnClickListener { importLauncher.launch("application/json") }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showAddCardDialog() {
        val dialogBinding = DialogAddCardBinding.inflate(LayoutInflater.from(this))
        currentDialogBinding = dialogBinding

        dialogBinding.textInputLayoutCardNumber.setEndIconOnClickListener {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            options.setPrompt("Scan a card")
            options.setBeepEnabled(true)
            options.setOrientationLocked(true)
            options.setCaptureActivity(CaptureActivityPortrait::class.java)
            barcodeLauncher.launch(options)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Add New Card")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { dialog, _ ->
                val name = dialogBinding.editTextName.text.toString().trim()
                val cardNumber = dialogBinding.editTextCardNumber.text.toString().trim()

                if (name.isNotEmpty() && cardNumber.isNotEmpty()) {
                    val card = Card(
                        name = name,
                        cardNumber = cardNumber,
                        barcodeType = "CODE128"
                    )
                    cardViewModel.insert(card)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Please fill name and card number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { currentDialogBinding = null }
            .show()
    }

    private fun onCardClick(card: Card) {
        val dialogBinding = DialogAddCardBinding.inflate(LayoutInflater.from(this))
        currentDialogBinding = dialogBinding

        dialogBinding.editTextName.setText(card.name)
        dialogBinding.editTextCardNumber.setText(card.cardNumber)

        dialogBinding.textInputLayoutCardNumber.setEndIconOnClickListener {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            options.setPrompt("Scan a card")
            options.setBeepEnabled(true)
            options.setOrientationLocked(true)
            options.setCaptureActivity(CaptureActivityPortrait::class.java)
            barcodeLauncher.launch(options)
        }

        // Generate and show barcode
        updateBarcodePreview(card.cardNumber, dialogBinding)

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Card")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { dialog, _ ->
                val name = dialogBinding.editTextName.text.toString().trim()
                val cardNumber = dialogBinding.editTextCardNumber.text.toString().trim()

                if (name.isNotEmpty() && cardNumber.isNotEmpty()) {
                    val updatedCard = card.copy(
                        name = name,
                        cardNumber = cardNumber
                    )
                    cardViewModel.update(updatedCard)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Please fill name and card number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Delete") { dialog, _ ->
                cardViewModel.delete(card)
                dialog.dismiss()
            }
            .setOnDismissListener { currentDialogBinding = null }
            .show()
    }

    private fun updateBarcodePreview(cardNumber: String, binding: DialogAddCardBinding) {
        if (cardNumber.isEmpty()) {
            binding.imageViewBarcode.visibility = View.GONE
            return
        }
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(cardNumber, BarcodeFormat.CODE_128, 600, 200)
            binding.imageViewBarcode.setImageBitmap(bitmap)
            binding.imageViewBarcode.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
            binding.imageViewBarcode.visibility = View.GONE
        }
    }

    private fun backupCards() {
        val cards = cardViewModel.allCards.value ?: emptyList()
        if (cards.isEmpty()) {
            Toast.makeText(this, "No cards to backup", Toast.LENGTH_SHORT).show()
            return
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "loyalty_cards_backup_$timeStamp.json"
        
        backupLauncher.launch(fileName)
    }

    private fun saveBackup(uri: Uri) {
        val cards = cardViewModel.allCards.value ?: emptyList()
        val json = Gson().toJson(cards)
        
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = BufferedWriter(OutputStreamWriter(outputStream))
                writer.write(json)
                writer.flush()
                Toast.makeText(this, "Backup saved successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save backup: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun importCards(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val json = reader.readText()
                val type = object : TypeToken<List<Card>>() {}.type
                val cards: List<Card> = Gson().fromJson(json, type)

                cards.forEach { card ->
                    // Reset ID to 0 for Room to autogenerate new IDs
                    val newCard = card.copy(id = 0)
                    cardViewModel.insert(newCard)
                }
                Toast.makeText(this, "Imported ${cards.size} cards", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
