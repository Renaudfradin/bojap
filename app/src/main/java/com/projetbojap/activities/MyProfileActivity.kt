package com.projetbojap.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.projetbojap.R
import com.projetbojap.firebase.FirestoreClass
import com.projetbojap.model.User
import com.projetbojap.utils.Constants
import kotlinx.android.synthetic.main.activity_my_profile.*
import java.io.IOException

class MyProfileActivity : BaseActivity() {

    private var mSelectedImageFileUri: Uri? = null

    private lateinit var mUserDetails: User

    private var mProfileImageURL: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        setupActionBar()

        FirestoreClass().loadUserData(this@MyProfileActivity)

        iv_profile_user_image.setOnClickListener {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
            ) {
                Constants.showImageChooser(this@MyProfileActivity)
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        Constants.READ_STORAGE_PERMISSION_CODE
                )
            }
        }

        btn_update.setOnClickListener {

            if (mSelectedImageFileUri != null) {

                uploadUserImage()
            } else {

                showProgressDialog(resources.getString(R.string.please_wait))

                updateUserProfileData()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK
                && requestCode == Constants.PICK_IMAGE_REQUEST_CODE
                && data!!.data != null
        ) {
            mSelectedImageFileUri = data.data!!

            try {
                Glide
                        .with(this@MyProfileActivity)
                        .load(Uri.parse(mSelectedImageFileUri.toString())) // URI of the image
                        .centerCrop() // Scale type of the image.
                        .placeholder(R.drawable.ic_user_place_holder) // A default place holder
                        .into(iv_profile_user_image) // the view in which the image will be loaded.
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.READ_STORAGE_PERMISSION_CODE) {
            //If permission is granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Constants.showImageChooser(this@MyProfileActivity)
            } else {
                Toast.makeText(
                        this,
                        "L'accès n'est pas autorisé",
                        Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupActionBar() {

        setSupportActionBar(toolbar_my_profile_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title = resources.getString(R.string.my_profile)
        }

        toolbar_my_profile_activity.setNavigationOnClickListener { onBackPressed() }
    }

    fun setUserDataInUI(user: User) {

        mUserDetails = user

        Glide
                .with(this@MyProfileActivity)
                .load(user.image)
                .centerCrop()
                .placeholder(R.drawable.ic_user_place_holder)
                .into(iv_profile_user_image)

        et_name.setText(user.name)
        et_email.setText(user.email)
        if (user.mobile != 0L) {
            et_mobile.setText(user.mobile.toString())
        }
    }

    private fun uploadUserImage() {

        showProgressDialog(resources.getString(R.string.please_wait))

        if (mSelectedImageFileUri != null) {

            val sRef: StorageReference = FirebaseStorage.getInstance().reference.child(
                    "USER_IMAGE" + System.currentTimeMillis() + "."
                            + Constants.getFileExtension(this@MyProfileActivity, mSelectedImageFileUri)
            )

            sRef.putFile(mSelectedImageFileUri!!)
                    .addOnSuccessListener { taskSnapshot ->
                        // The image upload is success
                        Log.e(
                                "Firebase Image URL",
                                taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                        )

                        // Get the downloadable url from the task snapshot
                        taskSnapshot.metadata!!.reference!!.downloadUrl
                                .addOnSuccessListener { uri ->
                                    Log.e("Downloadable Image URL", uri.toString())

                                    // assign the image url to the variable.
                                    mProfileImageURL = uri.toString()

                                    // Call a function to update user details in the database.
                                    updateUserProfileData()
                                }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                                this@MyProfileActivity,
                                exception.message,
                                Toast.LENGTH_LONG
                        ).show()

                        hideProgressDialog()
                    }
        }
    }

    private fun updateUserProfileData() {

        val userHashMap = HashMap<String, Any>()

        if (mProfileImageURL.isNotEmpty() && mProfileImageURL != mUserDetails.image) {
            userHashMap[Constants.IMAGE] = mProfileImageURL
        }

        if (et_name.text.toString() != mUserDetails.name) {
            userHashMap[Constants.NAME] = et_name.text.toString()
        }

        if (et_mobile.text.toString() != mUserDetails.mobile.toString()) {
            userHashMap[Constants.MOBILE] = et_mobile.text.toString().toLong()
        }

        FirestoreClass().updateUserProfileData(this@MyProfileActivity, userHashMap)
    }

    fun profileUpdateSuccess() {

        hideProgressDialog()

        setResult(Activity.RESULT_OK)
        finish()
    }
}