package com.example.fotografpaylasma.view

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation
import com.example.fotografpaylasma.R
import com.example.fotografpaylasma.databinding.FragmentKullaniciBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.activity.result.contract.ActivityResultContracts // Yeni API

class KullaniciFragment : Fragment() {

    private var _binding: FragmentKullaniciBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let {
                firebaseAuthWithGoogle(it)
            }
        } catch (e: ApiException) {
            Log.w("KullaniciFragment", "Google sign in failed: ${e.statusCode}", e)
            Toast.makeText(context, "Google giriş başarısız: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentKullaniciBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.kayitButton.setOnClickListener { kayitOl() }
        binding.girisYap.setOnClickListener { girisYap() }
        binding.googleButton.setOnClickListener { googleSignIn() }

        // Show/Hide Password Button İşlevselliği
        val passwordEditText = binding.passwordText
        val showPasswordButton = binding.showPasswordButton
        var isPasswordVisible = false

        showPasswordButton.setOnClickListener {
            if (isPasswordVisible) {
                // Şifreyi Gizle
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                showPasswordButton.setImageResource(R.drawable.hidden) // Şifreyi göster ikonu
            } else {
                // Şifreyi Göster
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT
                showPasswordButton.setImageResource(R.drawable.eye) // Şifreyi gizle ikonu
            }
            // İmleci şifre alanının sonuna taşı
            passwordEditText.setSelection(passwordEditText.text.length)
            isPasswordVisible = !isPasswordVisible
        }

        val guncelKullanici = auth.currentUser
        if (guncelKullanici != null) {
            navigateToFeedFragment()
        }
    }

    private fun kayitOl() {
        val email = binding.emailText.text.toString()
        val password = binding.passwordText.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        navigateToFeedFragment()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(), "Email ve şifre boş olamaz", Toast.LENGTH_LONG).show()
        }
    }

    private fun girisYap() {
        val email = binding.emailText.text.toString()
        val password = binding.passwordText.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    navigateToFeedFragment()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(), "Email ve şifre boş olamaz", Toast.LENGTH_LONG).show()
        }
    }

    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("KullaniciFragment", "signInWithCredential:success")
                    navigateToFeedFragment()
                } else {
                    Log.w("KullaniciFragment", "signInWithCredential:failure", task.exception)
                    Toast.makeText(context, "Firebase doğrulama başarısız", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToFeedFragment() {
        val action = KullaniciFragmentDirections.actionKullaniciFragmentToFeedFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
