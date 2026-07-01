package com.jv.loyaltycardswallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.jv.loyaltycardswallet.databinding.ActivityMainBinding
import com.jv.loyaltycardswallet.databinding.DialogAddCardBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cardViewModel: CardViewModel
    private lateinit var adapter: CardAdapter

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showAddCardDialog() {
        val dialogBinding = DialogAddCardBinding.inflate(LayoutInflater.from(this))
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
            .show()
    }

    private fun onCardClick(card: Card) {
        val dialogBinding = DialogAddCardBinding.inflate(LayoutInflater.from(this))
        dialogBinding.editTextName.setText(card.name)
        dialogBinding.editTextCardNumber.setText(card.cardNumber)

        // Generate and show barcode
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(card.cardNumber, BarcodeFormat.CODE_128, 600, 200)
            dialogBinding.imageViewBarcode.setImageBitmap(bitmap)
            dialogBinding.imageViewBarcode.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
            .show()
    }
}
