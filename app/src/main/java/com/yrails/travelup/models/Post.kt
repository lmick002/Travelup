package com.yrails.travelup.models

import java.io.Serializable

class Post : Serializable {
    var postId: String? = null
    var postCategory: String? = null
    var postText: String? = null
    var postImageUrl: String? = null
    var postTimeCreated: Long = 0

    var user: String? = null
    var userUid: String? = null
    var userPhotoUrl: String? = null

    var meetYear: Int = 0
    var meetMonth: Int = 0
    var meetDay: Int = 0
    var meetHour: Int = 0
    var meetMinute: Int = 0

    var numViews: Long = 0
    var numComments: Long = 0
}
