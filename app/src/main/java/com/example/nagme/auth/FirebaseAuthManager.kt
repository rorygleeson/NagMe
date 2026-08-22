package com.example.nagme.auth

import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthManager {

    private val auth =
        FirebaseAuth.getInstance()

    fun ensureAnonymousUser(
        onSuccess: (String) -> Unit,
        onFailure: (Exception?) -> Unit
    ) {

        val currentUser =
            auth.currentUser

        if (currentUser != null) {
            onSuccess(currentUser.uid)
            return
        }

        auth.signInAnonymously()
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val uid =
                        auth.currentUser?.uid

                    if (uid != null) {
                        onSuccess(uid)
                    } else {
                        onFailure(null)
                    }

                } else {
                    onFailure(task.exception)
                }
            }
    }
}