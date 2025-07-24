package com.jejecomms.businesscardapp.ui.contacts

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.jejecomms.businesscardapp.R
import com.jejecomms.businesscardapp.databinding.FragmentContactsBinding
import com.jejecomms.businesscardapp.model.ContactsModel
import com.jejecomms.businesscardapp.repository.ContactsRepository
import com.jejecomms.businesscardapp.ui.base.BaseFragment

/**
 * Fragment for displaying a list of contacts.
 */
class ContactsFragment() : BaseFragment<FragmentContactsBinding, ContactsViewModel>() {

    private lateinit var contactsAdapter: ContactAdapter
   // private var mContactList: MutableList<ContactsModel> = mutableListOf()
    private val mContactList by lazy { ArrayList<ContactsModel>() }

    override fun createViewModel(): ContactsViewModel? {
        val contactsRepository = ContactsRepository()
        val factory = ContactsViewModelFactory(contactsRepository)
        return ViewModelProvider(this, factory).get(ContactsViewModel::class.java)
    }

    override fun createViewBinding(
        layoutInflater: LayoutInflater?,
        container: ViewGroup?
    ): FragmentContactsBinding? {
       return layoutInflater?.let { FragmentContactsBinding.inflate(it, container, false) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupPullToRefresh()

        // Set up SearchView
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search submission
                query?.let {
                    contactsAdapter.filter(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Handle text changes
                newText?.let {
                    contactsAdapter.filter(it)
                }
                return true
            }
        })
    }

    /**
     * Sets up the RecyclerView for displaying the list of contacts.
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun setupRecyclerView() {
        contactsAdapter = ContactAdapter(requireContext()) {  clickedUser ->
            viewModel?.onContactFavoriteClicked(requireContext(), clickedUser)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@ContactsFragment.contactsAdapter
            setIndexTextSize(12)
            setIndexBarColor(R.color.white)
            setIndexBarTextColor(R.color.gray_light)
            setIndexBarTransparentValue(0.3.toFloat())
            setPreviewTextSize(60)
            setIndexBarStrokeVisibility(false)
            setPreviewVisibility(true)
        }

        viewModel?.contactsListLiveData?.observe(viewLifecycleOwner, Observer { contacts ->
            mContactList.clear()
            mContactList.addAll(contacts)
            contactsAdapter.updateContactsList(mContactList)
        })

        // Observe loading state from ViewModel
        viewModel?.mIsLoading?.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        })
        viewModel?.loadContacts(requireContext())
    }

    /**
     * Sets up the SwipeRefreshLayout for pull-to-refresh.
     */
    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.apply {
            setColorSchemeResources(R.color.secondary)
            setProgressBackgroundColorSchemeResource(R.color.white)

            // Set the listener for refresh action
            setOnRefreshListener {
                // When user pulls to refresh, tell ViewModel to reload contacts
                viewModel?.loadContacts(requireContext())
            }
        }
    }
}