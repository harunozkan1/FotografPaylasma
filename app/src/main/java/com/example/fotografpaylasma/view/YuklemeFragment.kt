package com.example.fotografpaylasma.view

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.example.fotografpaylasma.databinding.FragmentYuklemeBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

class YuklemeFragment : Fragment() {
    private var _binding: FragmentYuklemeBinding? = null
    private val binding get() = _binding!!
    private lateinit var cameraResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var secilenGorsel: Uri? = null
    var secilenBitmap: Bitmap? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        storage = Firebase.storage
        db = Firebase.firestore
        registerLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentYuklemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.yukleButton.setOnClickListener { yukleTiklandi() }
        binding.imageView.setOnClickListener { gorselSec() }
    }

    private fun yukleTiklandi() {
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Yükleniyor...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val uuid = UUID.randomUUID()
        val gorselAdi = "${uuid}.jpg"
        val reference = storage.reference
        val gorselReferansi = reference.child("images").child(gorselAdi)

        if (secilenBitmap != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val baos = ByteArrayOutputStream()
                    secilenBitmap!!.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                    val data = baos.toByteArray()

                    gorselReferansi.putBytes(data).await() // Görsel sıkıştırılarak yükleniyor
                    val downloadUri = gorselReferansi.downloadUrl.await()
                    val downloadUrl = downloadUri.toString()

                    if (auth.currentUser != null) {
                        val postMap = hashMapOf<String, Any>(
                            "downloadUrl" to downloadUrl,
                            "email" to auth.currentUser?.email.toString(),
                            "comment" to binding.commentText.text.toString(),
                            "date" to Timestamp.now() // Zaman damgası ekleniyor
                        )

                        // Firestore'a veri ekleniyor
                        db.collection("posts").add(postMap).await()
                        withContext(Dispatchers.Main) {
                            progressDialog.dismiss()
                            val action = YuklemeFragmentDirections.actionYuklemeFragmentToFeedFragment()
                            Navigation.findNavController(requireView()).navigate(action)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            progressDialog.dismiss()
                            Toast.makeText(requireContext(), "Kullanıcı giriş yapmamış", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            progressDialog.dismiss()
            Toast.makeText(requireContext(), "Lütfen bir görsel seçin", Toast.LENGTH_LONG).show()
        }
    }


    private fun gorselSec() {
        val options = arrayOf<CharSequence>("Kamera", "Galeri")
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Bir seçenek seçin")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> kameraAc()
                1 -> galeriSec()
            }
        }
        builder.show()
    }

    private fun kameraAc() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA)) {
                Snackbar.make(requireView(), "Kamerayı açmak için izin vermelisiniz", Snackbar.LENGTH_INDEFINITE)
                    .setAction("İzin ver") {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }.show()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (cameraIntent.resolveActivity(requireActivity().packageManager) != null) {
                cameraResultLauncher.launch(cameraIntent)
            } else {
                Toast.makeText(requireContext(), "Kamera uygulaması mevcut değil", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun galeriSec() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                ) {
                    Snackbar.make(requireView(), "Galeriye gitmek için izin ver", Snackbar.LENGTH_INDEFINITE)
                        .setAction("İzin ver") {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        }.show()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                openGallery()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    Snackbar.make(requireView(), "Galeriye gitmek için izin ver", Snackbar.LENGTH_INDEFINITE)
                        .setAction("İzin ver") {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }.show()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            } else {
                openGallery()
            }
        }
    }

    private fun openGallery() {
        val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryResultLauncher.launch(intentToGallery)
    }

    private fun registerLaunchers() {
        cameraResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    binding.imageView.setImageBitmap(imageBitmap)
                    secilenBitmap = imageBitmap
                } else {
                    Toast.makeText(requireContext(), "Görsel alınamadı", Toast.LENGTH_SHORT).show()
                }
            }
        }

        galleryResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                secilenGorsel = result.data?.data
                secilenGorsel?.let { uri ->
                    loadBitmapInBackground(uri)
                } ?: run {
                    Toast.makeText(requireContext(), "Görseli yüklerken bir hata oluştu", Toast.LENGTH_LONG).show()
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openGallery()
            } else {
                handlePermissionDenied(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun loadBitmapInBackground(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = decodeSampledBitmapFromUri(uri, binding.imageView.width, binding.imageView.height)
                withContext(Dispatchers.Main) {
                    binding.imageView.setImageBitmap(bitmap)
                    secilenBitmap = bitmap
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Görseli yüklerken bir hata oluştu", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun decodeSampledBitmapFromUri(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        val finalInputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(finalInputStream, null, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun handlePermissionDenied(permission: String) {
        Toast.makeText(requireContext(), "$permission izni verilmedi.", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
