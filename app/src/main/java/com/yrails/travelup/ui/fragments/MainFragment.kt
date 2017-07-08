package com.yrails.travelup.ui.fragments


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.Query
import com.yrails.travelup.R
import com.yrails.travelup.models.Post
import com.yrails.travelup.ui.activities.PostActivity
import com.yrails.travelup.ui.dialogs.PostCreateDialog
import com.yrails.travelup.utils.Categories
import com.yrails.travelup.utils.Constants
import com.yrails.travelup.utils.FirebaseUtils

class MainFragment : Fragment() {
    private var mRootView: View? = null
    private var mPostRecyclerView: RecyclerView? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var mPostAdapter: FirebaseRecyclerAdapter<Post, PostHolder>? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mRootView = inflater!!.inflate(R.layout.fragment_main, container, false)
        mRootView!!.findViewById(R.id.float_action_btn).setOnClickListener {
            PostCreateDialog().show(fragmentManager, "postWrite")
        }

        mPostRecyclerView = mRootView!!.findViewById(R.id.recyclerview_post) as RecyclerView
        mLayoutManager = LinearLayoutManager(context)
        mLayoutManager!!.reverseLayout = true
        mLayoutManager!!.stackFromEnd = true
        mPostRecyclerView!!.layoutManager = mLayoutManager

        setupAdapter()
        mPostRecyclerView!!.adapter = mPostAdapter

        return mRootView
    }

    private fun setupAdapter() {
        val mValue: String?
        val mReference: Query?

        if (arguments.containsKey("userUid")) {
            mValue = arguments.getString("userUid")
            mReference = FirebaseUtils.postsRef.orderByChild("userUid").equalTo(mValue)
        } else if (arguments.containsKey("postText")) {
            mValue = arguments.getString("postText")
            mReference = FirebaseUtils.postsRef.orderByChild("postText").startAt(mValue).endAt(mValue + "~")
        } else if (arguments.containsKey("postCategory")) {
            mValue = arguments.getString("postCategory")
            mReference = FirebaseUtils.postsRef.orderByChild("postCategory").equalTo(mValue)
        } else {
            mReference = FirebaseUtils.postsRef
        }

        mPostAdapter = object : FirebaseRecyclerAdapter<Post, PostHolder>(Post::class.java, R.layout.row_post, PostHolder::class.java, mReference) {
            override fun populateViewHolder(holder: PostHolder, comment: Post, position: Int) {
                holder.postCategoryImageView.setImageResource(Categories.getCategoryImage(comment.postCategory))
                Glide.with(activity).load(comment.userPhotoUrl).into(holder.postPostUserImageView)

                holder.postCategoryTextView.text = comment.postCategory
                holder.postPostUserTextView.text = comment.user
                holder.postTimeCreatedTextView.text = DateUtils.getRelativeTimeSpanString(comment.postTimeCreated)

                holder.postMeetDateTextView.text = StringBuilder().append(comment.meetYear).append("년 ")
                        .append(comment.meetMonth).append("월 ").append(comment.meetDay).append("일")
                val hText: String = when {
                    comment.meetHour < 10 -> "0" + comment.meetHour
                    else -> comment.meetHour.toString()
                }
                val mText: String = when {
                    comment.meetMinute < 10 -> "0" + comment.meetMinute
                    else -> comment.meetMinute.toString()
                }
                holder.postMeetTimeTextView.text = StringBuilder().append(hText).append(":").append(mText)

                holder.postPostTextView.text = comment.postText
                holder.postPostTextView.setTextIsSelectable(false)
                holder.postNumViewsTextView.text = comment.numViews.toString()
                holder.postNumCommentsTextView.text = comment.numComments.toString()

                holder.postLayout.setOnClickListener {
                    val i = Intent(context, PostActivity::class.java)
                    i.putExtra(Constants.EXTRA_POST, comment)
                    startActivity(i)
                    i.action
                }
            }
        }
    }

    class PostHolder(v: View) : RecyclerView.ViewHolder(v) {
        internal var postLayout = v.findViewById(R.id.layout_post) as LinearLayout
        internal var postCategoryImageView = v.findViewById(R.id.iv_category) as ImageView
        internal var postPostUserImageView = v.findViewById(R.id.iv_post_user) as ImageView

        internal var postCategoryTextView = v.findViewById(R.id.tv_category) as TextView
        internal var postPostUserTextView = v.findViewById(R.id.tv_post_user) as TextView
        internal var postTimeCreatedTextView = v.findViewById(R.id.tv_time_created) as TextView

        internal var postMeetDateTextView = v.findViewById(R.id.tv_meet_date) as TextView
        internal var postMeetTimeTextView = v.findViewById(R.id.tv_meet_time) as TextView

        internal var postPostTextView = v.findViewById(R.id.tv_post_text) as TextView
        internal var postNumViewsTextView = v.findViewById(R.id.tv_views) as TextView
        internal var postNumCommentsTextView = v.findViewById(R.id.tv_comments) as TextView
    }
}