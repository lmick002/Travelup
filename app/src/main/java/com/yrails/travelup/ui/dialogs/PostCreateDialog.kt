package com.yrails.travelup.ui.dialogs

import android.annotation.SuppressLint
import android.app.*
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.google.firebase.database.*
import com.yrails.travelup.R
import com.yrails.travelup.models.Post
import com.yrails.travelup.models.User
import com.yrails.travelup.ui.activities.PostActivity
import com.yrails.travelup.utils.Constants
import com.yrails.travelup.utils.FirebaseUtils
import java.util.*

class PostCreateDialog : DialogFragment(), View.OnClickListener, AdapterView.OnItemSelectedListener {
    private var mPost: Post? = null
    private var mRootView: View? = null

    private var mPostImageView: ImageView? = null
    private var mEditText: EditText? = null
    private var mCategorySpinner: Spinner? = null

    private var mProgressDialog: ProgressDialog? = null
    private var mImageUri: Uri? = null
    private var mCategory: String? = null

    private var mCalendar: Calendar? = null
    private var mDateButton: Button? = null
    private var meetYear: Int = 0
    private var meetMonth: Int = 0
    private var meetDay: Int = 0

    private var mTimeButton: Button? = null
    private var meetHour: Int = 0
    private var meetMinute: Int = 0

    private val mDateListener = DatePickerDialog.OnDateSetListener { _, arg1, arg2, arg3 ->
        meetYear = arg1
        meetMonth = arg2 + 1
        meetDay = arg3
        showDate(meetYear, meetMonth, meetDay)
    }

    private val mTimeListener = TimePickerDialog.OnTimeSetListener { _, arg1, arg2 ->
        meetHour = arg1
        meetMinute = arg2
        showTime(meetHour, meetMinute)
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        mPost = Post()

        mRootView = activity.layoutInflater.inflate(R.layout.dialog_create_post, null)
        mPostImageView = mRootView!!.findViewById(R.id.iv_dialog_post) as ImageView
        mRootView!!.findViewById(R.id.iv_dialog_send).setOnClickListener(this)
        mRootView!!.findViewById(R.id.iv_dialog_select).setOnClickListener(this)

        mEditText = mRootView!!.findViewById(R.id.et_dialog_post) as EditText
        mEditText!!.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if(mEditText!!.isInLayout) {
                    if (mEditText!!.layout!!.lineCount > 10)
                        mEditText!!.text.delete(mEditText!!.text.length - 1, mEditText!!.text.length)
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })

        mCategorySpinner = mRootView!!.findViewById(R.id.spinner_dialog_category) as Spinner
        mCategorySpinner!!.onItemSelectedListener = this

        mDateButton = mRootView!!.findViewById(R.id.date_picker_btn) as Button
        mDateButton!!.setOnClickListener(this)
        mCalendar = Calendar.getInstance()
        meetYear = mCalendar!!.get(Calendar.YEAR)
        meetMonth = mCalendar!!.get(Calendar.MONTH) + 1
        meetDay = mCalendar!!.get(Calendar.DAY_OF_MONTH)
        showDate(meetYear, meetMonth, meetDay)

        mTimeButton = mRootView!!.findViewById(R.id.time_picker_btn) as Button
        mTimeButton!!.setOnClickListener(this)
        meetHour = (mCalendar!!.get(Calendar.HOUR_OF_DAY) + 1) % 24
        meetMinute = 0
        showTime(meetHour, meetMinute)

        if (savedInstanceState != null) {
            if(savedInstanceState.containsKey(IMAGE_URL)) {
                mImageUri = Uri.parse(savedInstanceState.getString(IMAGE_URL))
                mPostImageView!!.setImageURI(mImageUri)
                mPostImageView!!.visibility = View.VISIBLE
            }
            else{
                meetYear = savedInstanceState.getInt(MEET_YEAR)
                meetMonth = savedInstanceState.getInt(MEET_MONTH)
                meetDay = savedInstanceState.getInt(MEET_DAY)
                showDate(meetYear, meetMonth, meetDay)

                meetHour = savedInstanceState.getInt(MEET_HOUR)
                meetMinute = savedInstanceState.getInt(MEET_MINUTE)
                showTime(meetHour, meetMinute)
            }
        }

        builder.setView(mRootView)
        return builder.create()
    }

    private fun showDate(year: Int, month: Int, day: Int) {
        mDateButton!!.text = StringBuilder().append(year).append("년 ").append(month).append("월 ").append(day).append("일")
    }

    private fun showTime(hour: Int, minute: Int) {
        val hText: String = when {
            hour < 10 -> "0" + hour
            else -> hour.toString()
        }
        val mText: String = when {
            minute < 10 -> "0" + minute
            else -> minute.toString()
        }

        mTimeButton!!.text = StringBuilder().append(hText).append(":").append(mText)
    }

    private fun datePicker() {
        DatePickerDialog(activity, mDateListener, meetYear, meetMonth - 1, meetDay).show()
    }

    private fun timePicker() {
        TimePickerDialog(activity, mTimeListener, meetHour, meetMinute, false).show()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        mCategory = parent!!.getItemAtPosition(position).toString()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.iv_dialog_send -> sendPost()
            R.id.iv_dialog_select -> selectImage()
            R.id.date_picker_btn -> datePicker()
            R.id.time_picker_btn -> timePicker()
        }
    }

    private fun sendPost() {
        if (mEditText!!.text.toString() == "") return
        showProgressDialog()

        FirebaseUtils.getUserRef(FirebaseUtils.currentUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                val postId = FirebaseUtils.uid
                val postText = mEditText!!.text.toString()

                if (user!!.numPosts == null) {
                    FirebaseUtils.getUserRef(FirebaseUtils.currentUser.uid).child(Constants.NUM_POSTS_KEY).setValue("1")
                } else {
                    FirebaseUtils.getUserRef(FirebaseUtils.currentUser.uid).child(Constants.NUM_POSTS_KEY).runTransaction(object : Transaction.Handler {
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

                mPost!!.postId = postId
                mPost!!.postCategory = mCategory
                mPost!!.postText = postText
                mPost!!.postTimeCreated = System.currentTimeMillis()

                mPost!!.userUid = user.uid
                mPost!!.user = user.user
                mPost!!.userPhotoUrl = user.photoUrl

                mPost!!.meetYear = meetYear
                mPost!!.meetMonth = meetMonth
                mPost!!.meetDay = meetDay
                mPost!!.meetHour = meetHour
                mPost!!.meetMinute = meetMinute

                mPost!!.numComments = 0
                mPost!!.numViews = 0

                if (mImageUri != null) {
                    FirebaseUtils.imagesRef.child(mImageUri!!.lastPathSegment).putFile(mImageUri!!).addOnSuccessListener(activity) {
                        mPost!!.postImageUrl = mImageUri!!.lastPathSegment
                        addToPosts(postId)
                    }
                } else {
                    addToPosts(postId)
                }
            }

            override fun onCancelled(databaseError: DatabaseError?) {
                dismissProgressDialog()
            }
        })

    }

    private fun addToPosts(postId: String) {
        FirebaseUtils.postsRef.child(postId).setValue(mPost)

        val i = Intent(context, PostActivity::class.java)
        i.putExtra(Constants.EXTRA_POST, mPost)
        startActivity(i)
        dismissProgressDialog()
        dismiss()
    }

    private fun showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog(context)
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

    private fun selectImage() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        startActivityForResult(Intent.createChooser(i, "Complete action using"), PHOTO_PICKER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PHOTO_PICKER) {
            if (resultCode == RESULT_OK) {
                mImageUri = data!!.data
                mPostImageView!!.setImageURI(mImageUri)
                mPostImageView!!.visibility = View.VISIBLE
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if(mImageUri != null) outState.putString(IMAGE_URL, mImageUri.toString())
        outState.putInt(MEET_YEAR, meetYear)
        outState.putInt(MEET_MONTH, meetMonth)
        outState.putInt(MEET_DAY, meetDay)
        outState.putInt(MEET_HOUR, meetHour)
        outState.putInt(MEET_MINUTE, meetMinute)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private val IMAGE_URL = "mImageUrl"
        private val MEET_YEAR = "meetYear"
        private val MEET_MONTH = "meetMonth"
        private val MEET_DAY = "meetDay"
        private val MEET_HOUR = "meetHour"
        private val MEET_MINUTE = "meetMinute"
        private val PHOTO_PICKER = 1
    }
}
