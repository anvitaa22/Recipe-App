package com.example.instafire

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.instafire.models.Post
import com.example.instafire.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_create.*
import kotlinx.android.synthetic.main.activity_create.imageView
import kotlinx.android.synthetic.main.activity_create.etDescription
import kotlinx.android.synthetic.main.activity_recipe_page.*

private const val TAG = "CreateActivity"
private const val PICK_PHOTO_CODE = 1234
class CreateActivity : AppCompatActivity() {
    private var photoUri: Uri? = null
    private var signedInUser: User? = null
    private lateinit var firestoreDb: FirebaseFirestore   // points to root of firestore
    private lateinit var storageReference: StorageReference     // points to storage reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)
        storageReference = FirebaseStorage.getInstance().reference
        firestoreDb = FirebaseFirestore.getInstance()
        firestoreDb.collection("users")
                // gets uid of current logged in user
                .document(FirebaseAuth.getInstance().currentUser?.uid as String)
                .get()
                .addOnSuccessListener { userSnapshot ->
                    signedInUser = userSnapshot.toObject(User::class.java)
                    Log.i(TAG, "signed in user: $signedInUser")
                }
                .addOnFailureListener { exception ->
                    Log.i(TAG, "Failure fetching signed in user", exception)
                }

        btnPickImage.setOnClickListener {
            Log.i(TAG, "Open up image picker on device")
            val imagePickerIntent = Intent(Intent.ACTION_GET_CONTENT)
            imagePickerIntent.type = "image/*"
            if (imagePickerIntent.resolveActivity(packageManager) != null) {
                Log.i(TAG, "startActivityForResult")
                startActivityForResult(imagePickerIntent, PICK_PHOTO_CODE)
            }
            else
                Log.i(TAG, "no application can handle this intent")
        }

        btnSubmit.setOnClickListener {
            handleSubmitButtonClick()
        }

        searchButton_create.setOnClickListener {
            val intent = Intent(this, ExploreActivity::class.java)
            startActivity(intent)
        }

        homeButton_create.setOnClickListener {
            val intent = Intent(this, PostActivity::class.java)
            startActivity(intent)
        }

        profileButton_create.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun handleSubmitButtonClick() {
        if (photoUri == null) {
            Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show()
            return
        }

        if (etDescription.text.isBlank()) {
            Toast.makeText(this, "Description cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (signedInUser == null) {
            Toast.makeText(this, "No signed in user, please wait", Toast.LENGTH_SHORT).show()
            return
        }

        // ASYNCHRONOUS

        btnSubmit.isEnabled = false     // disable submit button until upload is complete
        val photoUploadUri = photoUri as Uri       // cast as non null uri
        // unique
        val photoReference = storageReference.child("images/${System.currentTimeMillis()}-photo.jpg")
        // Upload photo to Firebase Storage
        photoReference.putFile(photoUploadUri)
                .continueWithTask { photoUploadTask ->
                    Log.i(TAG, "uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                    // Retrieve image url of the uploaded image
                    photoReference.downloadUrl
                }.continueWithTask { downloadUrlTask ->
                    // Create a post object with the image URL and add that to the posts collection
                    // TODO: update fake data

                    var recipeSteps = etRecipe.text.toString();
                    var stepsArray = recipeSteps.split(",");

                    var ingredients = tvIngredients.text.toString();
                    var ingredArray = ingredients.split(",");

                    val post = Post(etDishName.text.toString(),
                            etDescription.text.toString(),

                            Integer.parseInt(etDifficulty.text.toString()),
                            ingredArray,
                            stepsArray,
                            Integer.parseInt(etTime.text.toString()),
                            downloadUrlTask.result.toString(),
                            System.currentTimeMillis(),
                            signedInUser
                    )
                    firestoreDb.collection("posts").add(post)
                }.addOnCompleteListener { postCreationTask ->
                    btnSubmit.isEnabled = true
                    if (!postCreationTask.isSuccessful) {
                        Log.e(TAG, "Exception during Firebase operations", postCreationTask.exception)
                        Toast.makeText(this, "Failed to save post", Toast.LENGTH_SHORT).show()
                    }
                    etDescription.text.clear()
                    imageView.setImageResource(0)
                    Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()

                    val profileIntent = Intent(this, ProfileActivity::class.java)
                    //postIntent.putExtra(EXTRA_USERNAME, signedInUser?.username)
                    startActivity(profileIntent)
                    finish()    // once created, remove create activity from back stack
                }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_PHOTO_CODE) {
            // user has selected image
            if (resultCode == Activity.RESULT_OK) {
                photoUri = data?.data   // location of photo
                Log.i(TAG, "photoUri $photoUri")
                imageView.setImageURI(photoUri)     // imageView is layout id
            }

            else {
                Toast.makeText(this, "Image picker action canceled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}