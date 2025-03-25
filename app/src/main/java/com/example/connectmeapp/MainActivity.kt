package com.example.connectmeapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private val homeFragment = HomeFragment()
    private val searchFragment = SearchFragment()
    private val profileFragment = ProfileFragment()
    private val contactsFragment = ContactFragment()
    private var activeFragment: Fragment = homeFragment // Keeps track of the current active fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.itemActiveIndicatorColor = null
        bottomNavigation.setItemIconTintList(null)


        //  Initialize Fragments and Load HomeFragment by Default
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment, "HOME")
                .add(R.id.fragment_container, searchFragment, "SEARCH").hide(searchFragment)
                .add(R.id.fragment_container, profileFragment, "PROFILE").hide(profileFragment)
                .add(R.id.fragment_container, contactsFragment, "CONTACTS").hide(contactsFragment)
                .commit()
        }

        //  Handle Bottom Navigation Clicks
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(homeFragment, R.id.nav_home)
                R.id.nav_search -> switchFragment(searchFragment, R.id.nav_search)
                R.id.nav_add -> {
                    startActivity(Intent(this, AddPostActivity::class.java))
                    return@setOnItemSelectedListener false // Prevent highlight effect
                }
                R.id.nav_profile -> switchFragment(profileFragment, R.id.nav_profile)
                R.id.nav_contacts -> switchFragment(contactsFragment, R.id.nav_contacts)
            }
            true
        }

    }

    //  Switches to a new fragment only if it's different from the current one
    private fun switchFragment(newFragment: Fragment, selectedId: Int) {
        if (newFragment != activeFragment) { // Prevents unnecessary reloads
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(newFragment)
                .commit()
            activeFragment = newFragment // Updates the currently active fragment
        }
        updateIcons(selectedId)
    }

    //  Updates the icons dynamically when selecting a new tab
    private fun updateIcons(selectedId: Int) {
        bottomNavigation.menu.findItem(R.id.nav_home).setIcon(
            if (selectedId == R.id.nav_home) R.drawable.ic_home_active else R.drawable.ic_home_inactive
        )
        bottomNavigation.menu.findItem(R.id.nav_search).setIcon(
            if (selectedId == R.id.nav_search) R.drawable.ic_search_active else R.drawable.ic_search_inactive
        )
        bottomNavigation.menu.findItem(R.id.nav_profile).setIcon(
            if (selectedId == R.id.nav_profile) R.drawable.ic_profile_active else R.drawable.ic_profile_inactive
        )
        bottomNavigation.menu.findItem(R.id.nav_contacts).setIcon(
            if (selectedId == R.id.nav_contacts) R.drawable.ic_contacts_active else R.drawable.ic_contacts_inactive
        )
    }


}




