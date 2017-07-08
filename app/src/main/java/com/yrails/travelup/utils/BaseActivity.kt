package com.yrails.travelup.utils

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.yrails.travelup.R

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {
    protected var mFirebaseUser: FirebaseUser? = null
    protected var mGoogleApiClient: GoogleApiClient? = null
    protected var mAuth: FirebaseAuth? = null
    private var mProgressDialog: ProgressDialog? = null
    private var mGso: GoogleSignInOptions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        if (mAuth != null) mFirebaseUser = mAuth!!.currentUser

        mGso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build()

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mGso!!)
                .build()
    }

    protected open fun showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog(this)
            mProgressDialog!!.isIndeterminate = true
            mProgressDialog!!.setCancelable(false)
            mProgressDialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            mProgressDialog!!.window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        mProgressDialog!!.show()
        mProgressDialog!!.setContentView(R.layout.progress_dialog)
    }

    protected fun dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
            mProgressDialog!!.dismiss()
        }
    }

    protected fun signOut() {
        mAuth!!.signOut()
        LoginManager.getInstance().logOut()
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback { }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissProgressDialog()
    }
}
