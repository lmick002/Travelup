package com.yrails.travelup.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

object FirebaseUtils {
    val uid: String
        get() {
            val path = FirebaseDatabase.getInstance().reference.push().toString()
            return path.substring(path.lastIndexOf("/") + 1)
        }

    val currentUser: FirebaseUser
        get() = FirebaseAuth.getInstance().currentUser!!

    val postsRef: DatabaseReference
        get() = FirebaseDatabase.getInstance().getReference(Constants.POSTS_KEY)

    val imagesRef: StorageReference
        get() = FirebaseStorage.getInstance().getReference(Constants.POST_IMAGES)

    fun getUserRef(uid: String): DatabaseReference {
        return FirebaseDatabase.getInstance().getReference(Constants.USERS_KEY).child(uid)
    }

    fun getCommentRef(postId: String): DatabaseReference {
        return FirebaseDatabase.getInstance().getReference(Constants.COMMENTS_KEY).child(postId)
    }
}