package com.jejecomms.businesscardapp.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.jejecomms.businesscardapp.databinding.FragmentContactsBinding
import com.jejecomms.businesscardapp.repository.ContactsRepository
import com.jejecomms.businesscardapp.ui.base.BaseFragment

/**
 * Fragment for displaying a list of contacts.
 */
class ContactsFragment : BaseFragment<FragmentContactsBinding, ContactsViewModel>() {

    private lateinit var adapter: ContactAdapter

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
    }

    /**
     * Sets up the RecyclerView for displaying the list of contacts.
     */
    private fun setupRecyclerView() {
        adapter = ContactAdapter(requireContext()) {  clickedUser ->
            viewModel?.onContactFavoriteClicked(requireContext(), clickedUser)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@ContactsFragment.adapter
        }

        viewModel?.contactsListLiveData?.observe(viewLifecycleOwner, Observer { contacts ->
            adapter.submitList(contacts)
        })
        viewModel?.loadContacts(requireContext())
    }
}