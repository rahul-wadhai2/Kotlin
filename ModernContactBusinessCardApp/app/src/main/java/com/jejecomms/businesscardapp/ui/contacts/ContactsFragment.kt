package com.jejecomms.businesscardapp.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.jejecomms.businesscardapp.databinding.FragmentContactsBinding
import com.jejecomms.businesscardapp.ui.base.BaseFragment

class ContactsFragment : BaseFragment<FragmentContactsBinding, ContactsViewModel>() {

    override fun createViewModel(): ContactsViewModel? {
        return ViewModelProvider(this).get(ContactsViewModel::class.java)
    }

    override fun createViewBinding(
        layoutInflater: LayoutInflater?,
        container: ViewGroup?
    ): FragmentContactsBinding? {
       return layoutInflater?.let { FragmentContactsBinding.inflate(it, container, false) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textView: TextView = binding?.textContacts!!
        viewModel?.text?.observe(viewLifecycleOwner) {
            textView.text = it
        }
    }
}