package com.yrails.travelup.models

import java.io.Serializable

class User : Serializable {
    var uid: String? = null
    var user: String? = null
    var email: String? = null
    var photoUrl: String? = null

    var numPosts: String? = null
    var numComments: String? = null
}
