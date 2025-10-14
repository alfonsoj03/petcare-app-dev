package com.example.petcare.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petcare.R
import com.example.petcare.databinding.ActivityLoginBinding
import com.example.petcare.ui.main.MainActivity
import com.example.petcare.ui.register.RegisterActivity
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Login button click
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()

            if (validateInputs(email, password)) {
                // TODO: Implement actual login logic here
                showMessage("Iniciando sesión...")
                // For now, just navigate to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        // Forgot password click
        binding.forgotPassword.setOnClickListener {
            showMessage("Funcionalidad de recuperación de contraseña")
            // TODO: Implement password recovery flow
        }

        // Sign up text click
        binding.signUpText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.emailLayout.error = "El correo electrónico es requerido"
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Ingresa un correo electrónico válido"
            return false
        }
        
        if (password.isEmpty()) {
            binding.passwordLayout.error = "La contraseña es requerida"
            return false
        }
        
        if (password.length < 6) {
            binding.passwordLayout.error = "La contraseña debe tener al menos 6 caracteres"
            return false
        }
        
        // Clear any previous errors
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        
        return true
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
