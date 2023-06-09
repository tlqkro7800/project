package com.vp.project

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vp.project.databinding.FragmentUserSearchBinding
import com.vp.project.model.FollowEntity
import com.vp.project.model.UserCountEntity

class UserSearchFragment : Fragment() {


    private var firestore : FirebaseFirestore? = null
    private var binding: FragmentUserSearchBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        firestore = FirebaseFirestore.getInstance()
        binding = FragmentUserSearchBinding.inflate(layoutInflater)
        val view = binding!!.root
        var seluid :String = "UID"
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid.toString()

        val followsView  = view.findViewById<TextView>(R.id.FollowsSearch_Textview)
        val followersView  = view.findViewById<TextView>(R.id.FollowersSearch_Textview)
        val pinsView  = view.findViewById<TextView>(R.id.PinSearch_Textview)
        val nameView = view.findViewById<TextView>(R.id.NameSearch_Textview)
        val followButton = view.findViewById<Button>(R.id.Follow_Button)
        val BackButton = view.findViewById<Button>(R.id.Back_Button)

        var followsCnt :String = "0"
        var followersCnt :String = "0"
        var name :String = "0"
        var pins :Int = 0


        setFragmentResultListener("requestKey") { requestKey, bundle ->
            val UserBundle = bundle.getString("bundleKey")
            seluid = UserBundle.toString()
            //Toast.makeText(context,seluid.toString(), Toast.LENGTH_LONG).show()

            val db = firestore!!.collection("UserCount").document(seluid)
            db.get()
                .addOnSuccessListener { document ->
                    with(document) {
                        followsCnt = get("follows").toString()
                        followersCnt = get("followers").toString()
                        name = get("name").toString()
                        nameView?.text = name
                        followersView.text = followersCnt
                        followsView.text = followsCnt
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("TAG", "get failed with ", exception)
                }

            val db2 = firestore!!.collection("Users").document(seluid).collection("Pins")
            db2.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.apply {
                        pins = size()
                        pinsView.text = pins.toString()
                    }
                } else {
                    task.exception?.message?.let {
                        print(it)
                    }
                }
            }
            val db3 = firestore!!.collection("follow").document(currentUid)
            db3.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        var follows : MutableMap<String, Boolean> = HashMap()
                        follows = document.data?.get("followings") as MutableMap<String, Boolean>
                        val tmp = follows.forEach {
                            var tmpuid = it.key
                            if(tmpuid == seluid)
                                followButton.setText("언팔로우")
                        }
                    }
                }
        }

        followButton.setOnClickListener {
            val firestore = FirebaseFirestore.getInstance()
            val txDocTargetUser = firestore?.collection("follow").document(seluid)
            val FollowDB = firestore?.collection("UserCount").document(seluid)
            val rxDocTargetUser = firestore?.collection("follow").document(currentUid)
            val CurrentDB = firestore?.collection("UserCount").document(currentUid)
            var followDto : FollowEntity? = null
            var FollowCountDTO :UserCountEntity? = null

            if(followButton.text == "언팔로우") {
                txDocTargetUser.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            var followsup : MutableMap<String, Boolean>? = null
                            var followersup : MutableMap<String, Boolean>? = null

                            followsup = document.data?.get("follows") as MutableMap<String, Boolean>?
                            followersup = document.data?.get("followers") as MutableMap<String, Boolean>?

                            followersup?.remove(currentUid)
                            followersup?.remove(seluid)
                            followsup?.remove(currentUid)
                            followsup?.remove(seluid)

                            followDto?.followers = followersup!!
                            followDto?.follows = followsup!!

                            if (followDto != null) {
                                txDocTargetUser.set(followDto)
                            }
                        }
                    }

                rxDocTargetUser.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            var followsup : MutableMap<String, Boolean>? = HashMap()
                            var followersup : MutableMap<String, Boolean>? = HashMap()

                            followsup = document.data?.get("follows") as MutableMap<String, Boolean>?
                            followersup = document.data?.get("followers") as MutableMap<String, Boolean>?

                            followsup?.remove(seluid)

                            followDto?.followers = followersup!!
                            followDto?.follows = followsup!!

                            if (followDto != null) {
                                rxDocTargetUser.set(followDto)
                            }
                        }
                    }

                CurrentDB.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            var count :Int = document.data?.get("follows").toString().toInt() as Int
                            count = count - 1
                            FollowCountDTO?.name = document.data?.get("name").toString()
                            FollowCountDTO?.follows = count
                            FollowCountDTO?.followers = document.data?.get("followers").toString().toInt()
                            if (FollowCountDTO != null) {
                                firestore!!.collection("UserCount")?.document(currentUid)?.set(FollowCountDTO)
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d("TAG", "get failed with ", exception)
                    }

                FollowDB.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            var count :Int = document.data?.get("followers").toString().toInt() as Int
                            count = count - 1
                            FollowCountDTO?.name = document.data?.get("name").toString()
                            FollowCountDTO?.followers = count
                            FollowCountDTO?.follows = document.data?.get("follows").toString().toInt()
                            if (FollowCountDTO != null) {
                                firestore!!.collection("UserCount")?.document(seluid)?.set(FollowCountDTO)
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d("TAG", "get failed with ", exception)
                    }
                followButton.setText("팔로우")
            }
            else if (followButton.text == "팔로우") {
                txDocTargetUser.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            var followsup : MutableMap<String, Boolean>? = HashMap()
                            var followersup : MutableMap<String, Boolean>? = HashMap()

                            followsup = document.data?.get("follows") as MutableMap<String, Boolean>?
                            followersup = document.data?.get("followers") as MutableMap<String, Boolean>?

                            followersup?.set(currentUid, true)

                            followDto?.followers = followersup!!
                            followDto?.follows = followsup!!

                            if (followDto != null) {
                                txDocTargetUser.set(followDto)
                            }
                        }
                    }

                rxDocTargetUser.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            var followsup : MutableMap<String, Boolean>? = HashMap()
                            var followersup : MutableMap<String, Boolean>? = HashMap()

                            followsup = document.data?.get("follows") as MutableMap<String, Boolean>?
                            followersup = document.data?.get("followers") as MutableMap<String, Boolean>?

                            followsup?.set(seluid, true)

                            followDto?.followers = followersup!!
                            followDto?.follows = followsup!!

                            if (followDto != null) {
                                rxDocTargetUser.set(followDto)
                            }
                        }
                    }

                CurrentDB.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            var count :Int = document.data?.get("follows").toString().toInt() as Int
                            count = count + 1
                            FollowCountDTO?.name = document.data?.get("name").toString()
                            FollowCountDTO?.follows = count
                            FollowCountDTO?.followers = document.data?.get("followers").toString().toInt()
                            if (FollowCountDTO != null) {
                                firestore!!.collection("UserCount")?.document(currentUid)?.set(FollowCountDTO)
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d("TAG", "get failed with ", exception)
                    }
                FollowDB.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            var count :Int = document.data?.get("followers").toString().toInt() as Int
                            count = count + 1
                            FollowCountDTO?.name = document.data?.get("name").toString()
                            FollowCountDTO?.followers = count
                            FollowCountDTO?.follows = document.data?.get("follows").toString().toInt()
                            if (FollowCountDTO != null) {
                                firestore!!.collection("UserCount")?.document(seluid)?.set(FollowCountDTO)
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d("TAG", "get failed with ", exception)
                    }
                followButton.setText("언팔로우")
            }
            else {
                Log.d("ONFOLLOWBTN","error!!")
            }
        }
            //Firestore에 데이터 저장 : runTransaction{...}
            /*firestore.runTransaction {
                // it : Transaction
                // it.get(Document) : 해당 Document 받아오기
                // it.set(Document, Dto 객체) : 해당 Document에 Dto 객체 저장하기
                var followersDto = it.get(txDocTargetUser).toObject(FollowEntity::class.java)
                var followsDto = it.get(rxDocTargetUser).toObject(FollowEntity::class.java)

                var FollowCountDTO = UserCountEntity()

                if (followersDto == null) {

                    followersDto = FollowEntity().apply {
                        followers[currentUid!!] = true
                        //notifyFollow()
                    }
                    followsDto = FollowEntity().apply {
                        follows[currentUid!!] = true
                    }

                } else {
                    with(followersDto) {
                        //var userinfo :firebase = arrayOf(database)
                        if (followers.containsKey(currentUid!!)) {
                            // 언팔로우
                            followers.remove(currentUid!!)

                            followButton.setText("팔로우")
                            CurrentDB.get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        var count :Int = document.data?.get("follows").toString().toInt() as Int
                                        count = count - 1
                                        FollowCountDTO.name = document.data?.get("name").toString()
                                        FollowCountDTO.follows = count
                                        FollowCountDTO.followers = document.data?.get("followers").toString().toInt()
                                        firestore!!.collection("UserCount")?.document(currentUid)?.set(FollowCountDTO)
                                    } else {
                                        firestore!!.collection("UserCount")?.document(currentUid)?.set(FollowCountDTO)
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.d("TAG", "get failed with ", exception)
                                }
                            FollowDB.get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        var count :Int = document.data?.get("followers").toString().toInt() as Int
                                        count = count - 1
                                        FollowCountDTO.name = document.data?.get("name").toString()
                                        FollowCountDTO.followers = count
                                        FollowCountDTO.follows = document.data?.get("follows").toString().toInt()
                                        firestore!!.collection("UserCount")?.document(seluid)?.set(FollowCountDTO)
                                    } else {
                                        firestore!!.collection("UserCount")?.document(seluid)?.set(FollowCountDTO)
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.d("TAG", "get failed with ", exception)
                                }
                        } else {
                            // 팔로우
                            followers[currentUid!!] = true
                            followButton.setText("언팔로우")
                            CurrentDB.get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        var count :Int = document.data?.get("follows").toString().toInt() as Int
                                        count = count + 1
                                        FollowCountDTO.name = document.data?.get("name").toString()
                                        FollowCountDTO.follows = count
                                        FollowCountDTO.followers = document.data?.get("followers").toString().toInt()
                                        firestore!!.collection("UserCount")?.document(currentUid)?.set(FollowCountDTO)
                                    } else {
                                        firestore!!.collection("UserCount")?.document(currentUid)?.set(FollowCountDTO)
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.d("TAG", "get failed with ", exception)
                                }
                            FollowDB.get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        var count :Int = document.data?.get("followers").toString().toInt() as Int
                                        count = count + 1
                                        FollowCountDTO.name = document.data?.get("name").toString()
                                        FollowCountDTO.followers = count
                                        FollowCountDTO.follows = document.data?.get("follows").toString().toInt()
                                        firestore!!.collection("UserCount")?.document(seluid)?.set(FollowCountDTO)
                                    } else {
                                        firestore!!.collection("UserCount")?.document(seluid)?.set(FollowCountDTO)
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.d("TAG", "get failed with ", exception)
                                }

                            //notifyFollow()
                        }
                    }
                }
                it.set(txDocTargetUser, followersDto)
                return@runTransaction
            }
        }*/

        BackButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_activity_main, DashboardFragment())
                .commit()
        }

        return view
    }
}