package com.yrails.travelup.ui.activities

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.yrails.travelup.R
import com.yrails.travelup.models.User
import com.yrails.travelup.ui.fragments.MainFragment
import com.yrails.travelup.utils.BaseActivity
import com.yrails.travelup.utils.FirebaseUtils

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var mAuthStateListener: FirebaseAuth.AuthStateListener? = null

    private var mUserRef: DatabaseReference? = null
    private var mValueEventListner: ValueEventListener? = null
    private var mUserImageView: ImageView? = null
    private var mUserTextView: TextView? = null
    private var mEmailTextView: TextView? = null

    private var pUserRef: DatabaseReference? = null
    private var pValueEventListner: ValueEventListener? = null
    private var pUserImageView: ImageView? = null
    private var pUserTextView: TextView? = null
    private var pEmailTextView: TextView? = null
    private var pPostCountTextView: TextView? = null
    private var pCommentCountTextView: TextView? = null

    private val mFragManager = supportFragmentManager
    private var mFragInfo: MainFragment? = null
    private var mBundle: Bundle? = null
    private var postText = ""
    private var userUid = ""
    private var category = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuthStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                startActivity(Intent(this@MainActivity, RegisterActivity::class.java))
                finish()
            }
        }

        if (mFirebaseUser != null) {
            mUserRef = FirebaseUtils.getUserRef(mFirebaseUser!!.uid)
            pUserRef = FirebaseUtils.getUserRef(mFirebaseUser!!.uid)
        }

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navView = findViewById(R.id.nav_view) as NavigationView
        navView.setNavigationItemSelectedListener(this)
        navView.itemIconTintList = null
        navView.menu.getItem(0).isChecked = true
        val navHeaderView = navView.getHeaderView(0)

        showProgressDialog()
        resetFragment()
        initNavHeader(navHeaderView)
        initProfile()

        val i = intent
        if (i.hasExtra("userUid")) {
            userUid = i.getStringExtra("userUid")
            pUserRef = FirebaseUtils.getUserRef(userUid)
            changeFragment("userUid", userUid)

            findViewById(R.id.profile_container).visibility = View.VISIBLE
            if (userUid == FirebaseUtils.currentUser.uid) {
                findViewById(R.id.tv_p_email).visibility = View.VISIBLE
            } else {
                findViewById(R.id.tv_p_email).visibility = View.GONE
            }
        }
    }

    private fun resetFragment() {
        mBundle = Bundle()
        mFragInfo = MainFragment()
        mFragInfo!!.arguments = mBundle
        mFragManager.beginTransaction().replace(R.id.frag_container, mFragInfo).commit()
        findViewById(R.id.profile_container).visibility = View.GONE
    }

    private fun changeFragment(k: String, v: String) {
        mBundle = Bundle()
        mBundle!!.putString(k, v)
        mFragManager.beginTransaction().remove(mFragInfo).commit()
        mFragInfo = MainFragment()
        mFragInfo!!.arguments = mBundle
        mFragManager.beginTransaction().replace(R.id.frag_container, mFragInfo).commit()
    }

    private fun initNavHeader(v: View) {
        mUserImageView = v.findViewById(R.id.iv_m_user) as ImageView
        mUserTextView = v.findViewById(R.id.tv_m_user) as TextView
        mEmailTextView = v.findViewById(R.id.tv_m_email) as TextView

        mValueEventListner = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value != null) {
                    val user = dataSnapshot.getValue(User::class.java)
                    Glide.with(this@MainActivity).load(user!!.photoUrl).into(mUserImageView)
                    mUserTextView!!.text = user.user
                    mEmailTextView!!.text = user.email
                }
                dismissProgressDialog()
            }

            override fun onCancelled(databaseError: DatabaseError?) {
                dismissProgressDialog()
            }
        }
    }

    private fun initProfile() {
        pUserImageView = findViewById(R.id.iv_p_user) as ImageView
        pUserTextView = findViewById(R.id.tv_p_user) as TextView
        pEmailTextView = findViewById(R.id.tv_p_email) as TextView
        pPostCountTextView = findViewById(R.id.tv_post_count) as TextView
        pCommentCountTextView = findViewById(R.id.tv_comment_count) as TextView

        pValueEventListner = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value != null) {
                    val user = dataSnapshot.getValue(User::class.java)
                    Glide.with(this@MainActivity).load(user!!.photoUrl).into(pUserImageView)
                    pUserTextView!!.text = user.user
                    pEmailTextView!!.text = user.email
                    if(user.numPosts != null) {
                        pPostCountTextView!!.text = user.numPosts.toString()
                    }
                    if(user.numComments != null) {
                        pCommentCountTextView!!.text = user.numComments.toString()
                    }
                }
                dismissProgressDialog()
            }

            override fun onCancelled(databaseError: DatabaseError?) {
                dismissProgressDialog()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.nav_menu_main, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        val navView = findViewById(R.id.nav_view) as NavigationView

        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                postText = p0!!
                changeFragment("postText", postText)
                findViewById(R.id.profile_container).visibility = View.GONE
                navView.menu.getItem(0).isChecked = true
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return true
            }
        })
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_logout -> {
                signOut()
                return true
            }
            R.id.nav_profile -> {
                userUid = FirebaseUtils.currentUser.uid
                pUserRef = FirebaseUtils.getUserRef(userUid)
                pUserRef!!.removeEventListener(pValueEventListner!!)
                pUserRef!!.addValueEventListener(pValueEventListner)
                changeFragment("userUid", userUid)

                findViewById(R.id.profile_container).visibility = View.VISIBLE
                if (userUid == FirebaseUtils.currentUser.uid) {
                    findViewById(R.id.tv_p_email).visibility = View.VISIBLE
                } else {
                    findViewById(R.id.tv_p_email).visibility = View.GONE
                }
            }
            R.id.nav_all -> {
                resetFragment()
            }
            else -> {
                category = item.toString()
                changeFragment("postCategory", category)
                findViewById(R.id.profile_container).visibility = View.GONE
            }
        }

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val navView = findViewById(R.id.nav_view) as NavigationView

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else if (postText != "" || userUid != "" || category != "") {
            postText = ""
            userUid = ""
            category = ""
            navView.menu.getItem(0).isChecked = true
            resetFragment()
        } else {
            super.onBackPressed()
        }
    }

    override fun onStart() {
        super.onStart()
        mAuth!!.addAuthStateListener(mAuthStateListener!!)
        if (mUserRef != null) {
            mUserRef!!.addValueEventListener(mValueEventListner)
        }
        if (pUserRef != null) {
            pUserRef!!.addValueEventListener(pValueEventListner)
        }
    }

    override fun onStop() {
        super.onStop()
        if (mAuthStateListener != null) {
            mAuth!!.removeAuthStateListener(mAuthStateListener!!)
        }
        if (mUserRef != null) {
            mUserRef!!.removeEventListener(mValueEventListner!!)
        }
        if (pUserRef != null) {
            pUserRef!!.removeEventListener(pValueEventListner!!)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val navView = findViewById(R.id.nav_view) as NavigationView
        navView.menu.getItem(0).isChecked = true
        super.onSaveInstanceState(outState)
    }
}
