package com.example.icecastcontroller

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.icecastcontroller.databinding.ActivityAddSourceBinding

class AddSourceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddSourceBinding

    companion object {
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_MIME = "extra_mime"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSourceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Quelle hinzufügen"

        // MIME-Type Spinner
        val mimeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            StreamSource.MIME_TYPES
        )
        binding.spinnerMimeType.setAdapter(mimeAdapter)
        binding.spinnerMimeType.setText("audio/mpeg", false)

        // URL-Feld: MIME-Type automatisch vorschlagen
        binding.etUrl.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val guessed = StreamSource.guessMimeType(binding.etUrl.text.toString())
                binding.spinnerMimeType.setText(guessed, false)
            }
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val url = binding.etUrl.text.toString().trim()
            val mime = binding.spinnerMimeType.text.toString().trim()

            if (name.isEmpty()) {
                binding.etName.error = "Name ist erforderlich"
                return@setOnClickListener
            }
            if (url.isEmpty() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                binding.etUrl.error = "Gültige URL eingeben (http:// oder https://)"
                return@setOnClickListener
            }

            val result = Intent().apply {
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_MIME, mime)
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }

        binding.btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
