package com.yrails.travelup.ui.activities

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.yrails.travelup.R
import com.yrails.travelup.models.Comment
import com.yrails.travelup.models.Post
import com.yrails.travelup.models.User
import com.yrails.travelup.utils.Categories
import com.yrails.travelup.utils.Constants
import com.yrails.travelup.utils.FirebaseUtils


class PostActivity : AppCompatActivity(), View.OnClickListener {
    private var mPost: Post? = null
    private var mComment: Comment? = null
    private var mCommentEditText: EditText? = null

    private var mProgressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        val i = intent
        mPost = i.getSerializableExtra(Constants.EXTRA_POST) as Post

        mCommentEditText = findViewById(R.id.et_comment) as EditText
        mCommentEditText!!.setOnEditorActionListener { _, _, _ ->
            sendComment()
            true
        }
        findViewById(R.id.iv_send).setOnClickListener(this)

        initPost()
        initComments()


        FirebaseUtils.getUserRef(FirebaseUtils.currentUser.uid).child(Constants.NUM_COMMENTS_KEY).runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, b: Boolean, dataSnapshot2: DataSnapshot) {
            }
        })

        FirebaseUtils.getUserRef(FirebaseUtils.currentUser.uid).child(Constants.NUM_POSTS_KEY).runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, b: Boolean, dataSnapshot2: DataSnapshot) {
            }
        })
    }

    private fun initPost() {
        val postLayout = findViewById(R.id.layout_post) as LinearLayout
        val userProfile = findViewById(R.id.user_profile) as LinearLayout

        val postCategoryImageView = findViewById(R.id.iv_category) as ImageView
        val postPostUserImageView = findViewById(R.id.iv_post_user) as ImageView

        val postCategoryTextView = findViewById(R.id.tv_category) as TextView
        val postPostUserTextView = findViewById(R.id.tv_post_user) as TextView
        val postTimeCreatedTextView = findViewById(R.id.tv_time_created) as TextView
        val postMeetDateTextView = findViewById(R.id.tv_meet_date) as TextView
        val postMeetTimeTextView = findViewById(R.id.tv_meet_time) as TextView

        val postPostImageView = findViewById(R.id.iv_post_image) as ImageView
        val postPostTextView = findViewById(R.id.tv_post_text) as TextView

        val postNumViewsTextView = findViewById(R.id.tv_views) as TextView
        val postNumCommentsTextView = findViewById(R.id.tv_comments) as TextView

        mPost!!.numViews++
        FirebaseUtils.postsRef.child(mPost!!.postId).child(Constants.NUM_VIEWS_KEY).runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val num = mutableData.value as Long
                mutableData.value = num + 1
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, b: Boolean, dataSnapshot: DataSnapshot) {
            }
        })

        postCategoryImageView.setImageResource(Categories.getCategoryImage(mPost!!.postCategory))
        Glide.with(this@PostActivity).load(mPost!!.userPhotoUrl).into(postPostUserImageView)

        postCategoryTextView.text = mPost!!.postCategory
        postPostUserTextView.text = mPost!!.user
        postTimeCreatedTextView.text = DateUtils.getRelativeTimeSpanString(mPost!!.postTimeCreated)

        postMeetDateTextView.text = StringBuilder().append(mPost!!.meetYear).append("년 ").append(mPost!!.meetMonth).append("월 ").append(mPost!!.meetDay).append("일")
        val hText: String = when {
            mPost!!.meetHour < 10 -> "0" + mPost!!.meetHour
            else -> mPost!!.meetHour.toString()
        }
        val mText: String = when {
            mPost!!.meetMinute < 10 -> "0" + mPost!!.meetMinute
            else -> mPost!!.meetMinute.toString()
        }
        postMeetTimeTextView.text = StringBuilder().append(hText).append(":").append(mText)

        if (mPost!!.postImageUrl != null) {
            postPostImageView.visibility = View.VISIBLE
            val storageReference = FirebaseStorage.getInstance().getReference(Constants.POST_IMAGES + "/" + mPost!!.postImageUrl)
            Glide.with(this@PostActivity).using(FirebaseImageLoader()).load(storageReference).into(postPostImageView)
        } else {
            postPostImageView.setImageBitmap(null)
            postPostImageView.visibility = View.GONE
        }
        postPostTextView.text = mPost!!.postText

        postNumViewsTextView.text = mPost!!.numViews.toString()
        postNumCommentsTextView.text = mPost!!.numComments.toString()

        postLayout.setOnLongClickListener {
            if (mPost!!.userUid == FirebaseUtils.currentUser.uid) {
                AlertDialog.Builder(this@PostActivity)
                        .setMessage("글을 삭제하시겠습니까?")
                        .setPositiveButton(android.R.string.yes, { _, _ -> deletePost(mPost!!) })
                        .setNegativeButton(android.R.string.no, null).show()
            }
            false
        }
        userProfile.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            i.putExtra("userUid", mPost!!.userUid)
            startActivity(i)
            finish()
        }
        userProfile.setOnLongClickListener {
            if (mPost!!.userUid == FirebaseUtils.currentUser.uid) {
                AlertDialog.Builder(this@PostActivity)
                        .setMessage("글을 삭제하시겠습니까?")
                        .setPositiveButton(android.R.string.yes, { _, _ -> deletePost(mPost!!) })
                        .setNegativeButton(android.R.string.no, null).show()
            }
            false
        }
    }

    private fun deletePost(post: Post) {
        FirebaseUtils.getUserRef(FirebaseUtils.currentUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val postId = post.postId

                if (post.postImageUrl != null) {
                    FirebaseUtils.imagesRef.child(post.postImageUrl!!).delete().addOnSuccessListener(this@PostActivity) {
                        delToPosts(postId!!)
                    }
                } else {
                    delToPosts(postId!!)
                }
            }

            override fun onCancelled(databaseError: DatabaseError?) {
            }
        })

    }

    private fun delToPosts(postId: String) {
        FirebaseUtils.postsRef.child(postId).removeValue()

        FirebaseUtils.getUserRef(FirebaseUtils.currentUser.uid).child(Constants.NUM_POSTS_KEY).runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                var num: Long = 0
                if(mutableData.value != null) {
                    num = mutableData.value.toString().toLong()
                }
                num--
                mutableData.value = num.toString()
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError2: DatabaseError?, b2: Boolean, dataSnapshot3: DataSnapshot) {
            }
        })

        finish()
    }

    private fun initComments() {
        val commentRecyclerView = findViewById(R.id.comment_recyclerview) as RecyclerView
        commentRecyclerView.layoutManager = LinearLayoutManager(this@PostActivity)

        val commentAdapter = object : FirebaseRecyclerAdapter<Comment, CommentHolder>(Comment::class.java, R.layout.row_comment, CommentHolder::class.java, FirebaseUtils.getCommentRef(mPost!!.postId!!)) {
            override fun populateViewHolder(holder: CommentHolder, comment: Comment, position: Int) {
                holder.commentUserTextView.text = comment.user
                holder.commentTextView.text = comment.commentText
                holder.commentTimeTextView.text = DateUtils.getRelativeTimeSpanString(comment.commentTimeCreated)

                Glide.with(this@PostActivity).load(comment.userPhotoUrl).into(holder.commentUserImageView)

                holder.commentLayout.setOnLongClickListener {
                    if (comment.userUid == FirebaseUtils.currentUser.uid) {
                        AlertDialog.Builder(this@PostActivity)
                                .setMessage("댓글을 삭제하시겠습니까?")
                                .setPositiveButton(android.R.string.yes, { _, _ -> delComment(comment) })
                                .setNegativeButton(android.R.string.no, null).show()
                    }
                    false
                }

                holder.commentUserImageView.setOnClickListener {
                    val i = Intent(this@PostActivity, MainActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    i.putExtra("userUid", comment.userUid)
                    startActivity(i)
                    finish()
                }
            }
        }
        commentRecyclerView.adapter = commentAdapter
    }

    private fun delComment(comment: Comment): Boolean {
        FirebaseUtils.getUserRef(FirebaseUtils.currentUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                FirebaseUtils.getCommentRef(mPost!!.postId!!).child(comment.commentId).removeValue()

                FirebaseUtils.postsRef.child(mPost!!.postId!!).child(Constants.NUM_COMMENTS_KEY).runTransaction(object : Transaction.Handler {
                    override fun doTransaction(mutableData: MutableData): Transaction.Result {
                        val num = mutableData.value as Long
                        mutableData.value = num - 1
                        return Transaction.success(mutableData)
                    }

                    override fun onComplete(databaseError: DatabaseError?, b: Boolean, dataSnapshot2: DataSnapshot) {
                        mPost!!.numComments--
                        val postNumCommentsTextView = findViewById(R.id.tv_comments) as TextView
                        postNumCommentsTextView.text = mPost!!.numComments.toString()

                        FirebaseUtils.getUserRef(FirebaseUtils.currentUser.uid).child(Constants.NUM_COMMENTS_KEY).runTransaction(object : Transaction.Handler {
                            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                var num: Long = 0
                                if(mutableData.value != null) {
                                    num = mutableData.value.toString().toLong()
                                }
                                num--
                                mutableData.value = num.toString()
                                return Transaction.success(mutableData)
                            }

                            override fun onComplete(databaseError2: DatabaseError?, b2: Boolean, dataSnapshot3: DataSnapshot) {
                            }
                        })
                    }
                })
            }

            override fun onCancelled(databaseError: DatabaseError?) {
            }
        })
        return false
    }

    private fun sendComment() {
        if (mCommentEditText!!.text.toString() == "") return

        showProgressDialog()

        val commentId = FirebaseUtils.uid
        val commentText = mCommentEditText!!.text.toString()
        mCommentEditText!!.setText("")

        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(mCommentEditText!!.windowToken, 0)

        mComment = Comment()
        mComment!!.commentId = commentId
        mComment!!.commentText = commentText
        mComment!!.commentTimeCreated = System.currentTimeMillis()

        FirebaseUtils.getUserRef(FirebaseUtils.currentUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)

                mComment!!.user = user!!.user
                mComment!!.userUid = user.uid
                mComment!!.userPhotoUrl = user.photoUrl

                FirebaseUtils.getCommentRef(mPost!!.postId!!).child(commentId).setValue(mComment)

                FirebaseUtils.postsRef.child(mPost!!.postId!!).child(Constants.NUM_COMMENTS_KEY).runTransaction(object : Transaction.Handler {
                    override fun doTransaction(mutableData: MutableData): Transaction.Result {
                        val num = mutableData.value as Long
                        mutableData.value = num + 1
                        return Transaction.success(mutableData)
                    }

                    override fun onComplete(databaseError: DatabaseError?, b: Boolean, dataSnapshot2: DataSnapshot) {
                        dismissProgressDialog()
                        mPost!!.numComments++
                        val postNumCommentsTextView = findViewById(R.id.tv_comments) as TextView
                        postNumCommentsTextView.text = mPost!!.numComments.toString()

                        FirebaseUtils.getUserRef(FirebaseUtils.currentUser.uid).child(Constants.NUM_COMMENTS_KEY).runTransaction(object : Transaction.Handler {
                            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                var num: Long = 0
                                if(mutableData.value != null) {
                                    num = mutableData.value.toString().toLong()
                                }
                                num++
                                mutableData.value = num.toString()
                                return Transaction.success(mutableData)
                            }

                            override fun onComplete(databaseError2: DatabaseError?, b2: Boolean, dataSnapshot3: DataSnapshot) {
                            }
                        })
                    }
                })
            }

            override fun onCancelled(databaseError: DatabaseError?) {
                dismissProgressDialog()
            }
        })
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.iv_send -> sendComment()
        }
    }

    private fun showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog(this)
            mProgressDialog!!.isIndeterminate = true
            mProgressDialog!!.setCancelable(false)
            mProgressDialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        mProgressDialog!!.show()
        mProgressDialog!!.setContentView(R.layout.progress_dialog)
    }

    private fun dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
            mProgressDialog!!.dismiss()
        }
    }

    private class CommentHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var commentLayout = itemView.findViewById(R.id.layout_comment) as LinearLayout
        internal var commentUserImageView = itemView.findViewById(R.id.iv_comment_user) as ImageView
        internal var commentUserTextView = itemView.findViewById(R.id.tv_comment_user) as TextView
        internal var commentTextView = itemView.findViewById(R.id.tv_comment) as TextView
        internal var commentTimeTextView = itemView.findViewById(R.id.tv_time) as TextView
    }
}
