package com.yrails.travelup.ui.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import com.facebook.*
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.yrails.travelup.R
import com.yrails.travelup.utils.BaseActivity
import com.yrails.travelup.utils.FirebaseUtils


class RegisterActivity : BaseActivity(), View.OnClickListener {
    private var mGso: GoogleSignInOptions? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mCallbackManager: CallbackManager? = null
    private var mAuthCredential: AuthCredential? = null
    private var mProgressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        findViewById(R.id.sign_in_google).setOnClickListener(this)
        findViewById(R.id.sign_in_facebook).setOnClickListener(this)

        mGso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build()

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mGso!!)
                .build()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.sign_in_google -> {
                showProgressDialog()
                signInGoogle()
            }
            R.id.sign_in_facebook -> {
                showProgressDialog()
                signInFacebook()
            }
        }
    }

    private fun signInFacebook() {
        mCallbackManager = CallbackManager.Factory.create()
        val loginButton = findViewById(R.id.sign_in_facebook) as LoginButton
        loginButton.setReadPermissions("email", "public_profile")
        loginButton.registerCallback(mCallbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                mAuth = FirebaseAuth.getInstance()
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
            }

            override fun onError(error: FacebookException) {
            }
        })
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        mAuthCredential = FacebookAuthProvider.getCredential(token.token)
        mAuth!!.signInWithCredential(mAuthCredential!!).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val request = GraphRequest.newMeRequest(token) { json, response ->
                    if (response.error == null) {
                        val fbUserId = json.optString("id")
                        FirebaseUtils.getUserRef(mAuth!!.currentUser!!.uid).child("email").setValue(json.optString("email"))
                        FirebaseUtils.getUserRef(mAuth!!.currentUser!!.uid).child("user").setValue(json.optString("name"))
                        FirebaseUtils.getUserRef(mAuth!!.currentUser!!.uid).child("uid").setValue(mAuth!!.currentUser!!.uid)
                        FirebaseUtils.getUserRef(mAuth!!.currentUser!!.uid).child("photoUrl").setValue("http://graph.facebook.com/$fbUserId/picture?type=large")
                    }
                }
                val prams = Bundle()
                prams.putString("fields", "id, name, email")
                request.parameters = prams
                request.executeAsync()

                mFirebaseUser = mAuth!!.currentUser

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                dismissProgressDialog()
            }
        }
    }

    private fun signInGoogle() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RC_SIGN_IN) {
                val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                if (result.isSuccess) {
                    val account = result.signInAccount
                    if (account != null) {
                        firebaseAuthWithGoogle(account)
                    }
                } else {
                    dismissProgressDialog()
                }
            } else {
                mCallbackManager!!.onActivityResult(requestCode, resultCode, data)
                dismissProgressDialog()
            }
        } else {
            dismissProgressDialog()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d("TAG", "firebaseAuthWithGoogle:" + account.getId())
        mAuthCredential = GoogleAuthProvider.getCredential(account.idToken, null)
        mAuth!!.signInWithCredential(mAuthCredential!!).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                FirebaseUtils.getUserRef(mAuth!!.currentUser!!.uid).child("email").setValue(account.email)
                FirebaseUtils.getUserRef(mAuth!!.currentUser!!.uid).child("user").setValue(account.displayName)
                FirebaseUtils.getUserRef(mAuth!!.currentUser!!.uid).child("uid").setValue(mAuth!!.currentUser!!.uid)
                if (account.photoUrl != null) {
                    FirebaseUtils.getUserRef(mAuth!!.currentUser!!.uid).child("photoUrl").setValue(account.photoUrl!!.toString())
                }

                mFirebaseUser = mAuth!!.currentUser
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                dismissProgressDialog()
            }
        }
    }

    override fun showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog(this)
            mProgressDialog!!.isIndeterminate = true
            mProgressDialog!!.setCancelable(true)
            mProgressDialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        mProgressDialog!!.show()
        mProgressDialog!!.setContentView(R.layout.progress_dialog)
    }

    companion object {
        private val RC_SIGN_IN = 9001
    }
}
