package com.example.fotografpaylasma.view

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fotografpaylasma.databinding.FragmentFeedBinding
import com.example.fotografpaylasma.model.Post
import com.example.fotografpaylasma.R
import com.example.fotografpaylasma.adapter.PostAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase

class FeedFragment : Fragment(), PopupMenu.OnMenuItemClickListener {
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val postList: ArrayList<Post> = arrayListOf()
    private var adapter: PostAdapter? = null
    private var firestoreListenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
        Log.d("FirestoreDebug", "Firestore initialized")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.floatingActionButton2.setOnClickListener { showPopupMenu(it) }
        fireStoreVerileriAl()

        adapter = PostAdapter(postList)
        binding.feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.feedRecyclerView.adapter = adapter
    }

    private fun fireStoreVerileriAl() {
        firestoreListenerRegistration = db.collection("posts")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING) // Sıralama burada
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_LONG).show()
                    Log.e("FirestoreError", "Error: ${error.localizedMessage}")
                } else {
                    value?.let { snapshot ->
                        if (!snapshot.isEmpty) {
                            postList.clear()
                            snapshot.documents.forEach { document ->
                                val comment = document.getString("comment") ?: ""
                                val email = document.getString("email") ?: ""
                                val downloadUrl = document.getString("downloadUrl") ?: ""
                                val post = Post(email, comment, downloadUrl)
                                postList.add(post)
                                Log.d("FirestoreDebug", "email: $email, comment: $comment, downloadUrl: $downloadUrl")
                            }
                            adapter?.notifyDataSetChanged()
                        } else {
                            Log.d("FirestoreDebug", "No documents found in 'Posts' collection.")
                        }
                    }
                }
            }
    }


    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.my_popus_menu, popup.menu)
        popup.setOnMenuItemClickListener(this)
        popup.show()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.yuklemeItem -> {
                findNavController().navigate(R.id.action_feedFragment_to_yuklemeFragment)
                true
            }
            R.id.cikisItem -> {
                auth.signOut()
                findNavController().navigate(R.id.action_feedFragment_to_kullaniciFragment)
                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListenerRegistration?.remove()
        _binding = null
    }
}
