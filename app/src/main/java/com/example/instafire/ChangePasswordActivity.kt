package com.example.instafire

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_change_password.*
import kotlinx.android.synthetic.main.activity_login.*

private const val TAG = "ChangePasswordActivity"

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        btnConfirm.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser

            val oldPassword = etOldPassword.text.toString()
            if(oldPassword.isEmpty()){
                etOldPassword.error = "Password not entered"
                return@setOnClickListener
            }

            //authenticate this password
            val credential = EmailAuthProvider
                .getCredential(user?.email.toString(), oldPassword)

            user?.reauthenticate(credential)
                ?.addOnSuccessListener {
                    updatePasscode()
                }
                ?.addOnFailureListener {
                    etOldPassword.error = "Incorrect password"
                }
        }

        tvForgot.setOnClickListener {
            Log.d(TAG, "Try to show reset password activity")
            //launch the reset password activity somehow
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
        }

    }

    private fun updatePasscode(){
        val password1 = etPassword1.text.toString()
        val password2 = etPassword2.text.toString()
        if (password1.isEmpty()){
            etPassword1.error = "Password not entered"
            return
        }
        if (password2.isEmpty()){
            etPassword2.error = "Password not entered"
            return
        }
        if (password1 == password2){
            FirebaseAuth.getInstance().currentUser?.updatePassword(password1)
                ?.addOnSuccessListener {
                    Toast.makeText(this, "Password changed", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, ProfileSettingActivity::class.java)
                    startActivity(intent)
                    finish()
                }
        }
        if(password1 != password2){
            etPassword2.error = "Different passwords entered"
        }
    }
}