package com.yrails.travelup.models

import java.io.Serializable

class Comment : Serializable {
    var commentId: String? = null
    var commentText: String? = null
    var commentTimeCreated: Long = 0

    var user: String? = null
    var userUid: String? = null
    var userPhotoUrl: String? = null
}