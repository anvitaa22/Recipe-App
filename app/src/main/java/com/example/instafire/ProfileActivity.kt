package com.example.instafire

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.instafire.models.Post
import com.example.instafire.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_explore.*
import kotlinx.android.synthetic.main.activity_explore.homeButton
import kotlinx.android.synthetic.main.activity_explore.profileButton
import kotlinx.android.synthetic.main.activity_explore.rvPosts
import kotlinx.android.synthetic.main.activity_explore.searchButton
import kotlinx.android.synthetic.main.activity_post.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.item_post.view.*
import java.math.BigInteger
import java.security.MessageDigest

private const val TAG = "ProfileActivity"

//class ProfileActivity : PostActivity() {
//
//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.menu_profile, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if(item.itemId == R.id.menu_logout) {
//            Log.i(TAG, "User wants to logout")
//            FirebaseAuth.getInstance().signOut();
//            startActivity(Intent(this, LoginActivity::class.java))
//        }
//        return super.onOptionsItemSelected(item)
//    }
//}

class ProfileActivity : AppCompatActivity() {
    private var signedInUser: User? = null
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var posts: MutableList<Post>
    private lateinit var adapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        //create layout file that represents one post - done
        //create data source - done
        posts = mutableListOf()     // empty mutable list
        //create the adapter
        adapter = PostAdapter(this, posts)
        //bind adapter and layout manager to the RV
        rvPosts.adapter = adapter
        rvPosts.layoutManager = LinearLayoutManager(this)
        firestoreDb = FirebaseFirestore.getInstance()

        firestoreDb.collection("users")
                .document(FirebaseAuth.getInstance().currentUser?.uid as String)
                .get()
                .addOnSuccessListener {userSnapshot ->
                    signedInUser = userSnapshot.toObject(User::class.java)
                    profileBio.text = signedInUser?.bio
                    profileUsername.text = signedInUser?.username
                    Log.i(TAG, "signed in user: $signedInUser")
                }

                .addOnFailureListener {exception ->
                    Log.i(TAG, "failure fetching signed in user", exception)
                }

        var postsReference = firestoreDb
                .collection("posts")
                .limit(20)
        // .orderBy("creation_time", Query.Direction.DESCENDING)

        val username = intent.getStringExtra(EXTRA_USERNAME)
        if(username != null) {
            supportActionBar?.title = username
            postsReference = postsReference.whereEqualTo("user.username", username)
            // only display user's post on profile
            Glide.with(this).load(getProfileImageUrl(username)).into(profilePic)
        }


        postsReference.addSnapshotListener { snapshot, exception ->
            if (exception != null || snapshot == null) {
                Log.e(TAG, "Exception when querying posts", exception)
                return@addSnapshotListener
            }
            val postList = snapshot.toObjects(Post::class.java)
            posts.clear()
            posts.addAll(postList)
            adapter.notifyDataSetChanged()
            for (post in postList) {
                Log.i(TAG, "Post ${post}")
            }
        }

        searchButton.setOnClickListener {
            val intent = Intent(this, ExploreActivity::class.java)
            startActivity(intent)
        }

        homeButton.setOnClickListener {
            val intent = Intent(this, PostActivity::class.java)
            startActivity(intent)
        }

        profileButton.setOnClickListener {
            Toast.makeText(this, "You are already on Profile page!", Toast.LENGTH_SHORT).show()
        }

        profileSettingBtn.setOnClickListener {
            val intent = Intent(this, ProfileSettingActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun getProfileImageUrl(username: String): String {
        val digest = MessageDigest.getInstance("MD5");
        val hash = digest.digest(username.toByteArray());
        val bigInt = BigInteger(hash);
        val hex = bigInt.abs().toString(16);
        return "https://www.gravatar.com/avatar/$hex?d=identicon";
    }
}