package com.jejecomms.businesscardapp.ui.contacts

import AvatarColorProvider
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SectionIndexer
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.jejecomms.businesscardapp.R
import com.jejecomms.businesscardapp.databinding.ItemContactBinding
import com.jejecomms.businesscardapp.model.ContactsModel
import com.jejecomms.businesscardapp.utils.Helpers.Companion.sectionsHelper
import com.jejecomms.businesscardapp.utils.SharedPreferencesManager
import createAvatarPlaceholder
import java.util.Locale

class ContactAdapter(
    private val context: Context,
    private val onContactFavoriteClicked: (ContactsModel) -> Unit,
    private val onContactCallClicked: (ContactsModel) -> Unit,
    private val onContactDeleteClicked: (ContactsModel) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>()
     , SectionIndexer{

    private lateinit var listContactId: List<String>
    private var lastAnimatedPosition = -1
    private val mSections = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#"
    private var mSectionPositions: ArrayList<Int>? = null
    private var sectionsTranslator = HashMap<Int, Int>()
    private var mContactList: MutableList<ContactsModel> = mutableListOf()
    private var filterContactList: MutableList<ContactsModel> = mutableListOf()

    inner class ContactViewHolder(val binding: ItemContactBinding) : RecyclerView
        .ViewHolder(binding.root) {
        // References to the new views
        val frontView: RelativeLayout = binding.root.findViewById(R.id.front_view_content)
        val backView: LinearLayout = binding.root.findViewById(R.id.back_view_actions)

        fun bind(contactsModel: ContactsModel, holder: ContactViewHolder) {
            with(binding) {
               userCompany.text = contactsModel.company

                listContactId = SharedPreferencesManager.getList<String>(context,
                    SharedPreferencesManager.KEY_USER_FAVORITE)

                if (listContactId.isNotEmpty()) {
                    if (listContactId.contains(contactsModel.id)) {
                        favoriteStar.isChecked = true
                        favoriteStar.visibility = View.VISIBLE
                        btnFavoriteSwipe.isChecked = true
                    } else {
                        favoriteStar.visibility = View.GONE
                        btnFavoriteSwipe.isChecked = false
                    }
                } else {
                    favoriteStar.visibility = View.GONE
                }

                favoriteStar.setOnClickListener {
                    onContactFavoriteClicked(contactsModel)
                }

                // Set up click listeners for the revealed actions
                btnCall.setOnClickListener {
                    // Handle call action
                    hideShowFrontBackView(holder)
                }

               btnDelete.setOnClickListener {
                    // Handle delete action
                    hideShowFrontBackView(holder)
                }

                binding.btnFavoriteSwipe.setOnClickListener {
                    // Handle favorite action from swipe
                    onContactFavoriteClicked(contactsModel)
                    hideShowFrontBackView(holder)
                }


                if (contactsModel.name.isNotEmpty()) {
                    val placeHolderDrawable = createAvatarPlaceholder(
                        contactsModel.name.first().toString().uppercase()
                        ,contactsModel.color
                    )
                    avatarImageView.setImageDrawable(placeHolderDrawable)
                } else {
                    avatarImageView.setImageResource(R.drawable.default_avtar)
                }
                val currentDrawableOnImageView: Drawable? = avatarImageView.drawable
                var firstLetterColorForName = ContextCompat.getColor(itemView.context, R.color.black)
                if ( currentDrawableOnImageView != null) {
                    if (currentDrawableOnImageView is AvatarColorProvider) {
                        firstLetterColorForName = currentDrawableOnImageView.avatarBackgroundColor
                    }
                }

                // Set the name with the first letter colored
                if (contactsModel.name.isNotEmpty()) {
                    setFirstLetterColor(userName, contactsModel.name, firstLetterColorForName)
                } else {
                    userName.text = ""
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder,
                                  @SuppressLint("RecyclerView") position: Int) {
        val contact = mContactList[position]
        holder.bind(contact, holder)
        hideShowFrontBackView(holder)
        // Apply animation scrolling Up/down.
        if (position > lastAnimatedPosition) {
            val animation = AnimationUtils.loadAnimation(
                holder.itemView.context, R.anim.fade_slide_in_from_right
            )
            holder.itemView.startAnimation(animation)
            lastAnimatedPosition = position
        }
    }

    override fun getItemCount(): Int {
        return mContactList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateContactsList(contactList: ArrayList<ContactsModel>) {
        this.mContactList = ArrayList(contactList)
        this.filterContactList = ArrayList(contactList)
        notifyDataSetChanged()
    }

    /**
     * Filter the list of contacts based on the given query.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        mContactList.clear() // Clear the current list

        if (query.isBlank()) {
            // If the query is empty, show all original contacts
            mContactList.addAll(filterContactList)
        } else {
            // Otherwise, filter based on the query
            // Normalize query for case-insensitive search
            val lowerCaseQuery = query.lowercase().trim()

            for (contact in filterContactList) {
                // Check if the contact name contains the query (case-insensitive)
                if (contact.name.lowercase().contains(lowerCaseQuery)) {
                    mContactList.add(contact)
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun onViewDetachedFromWindow(holder: ContactViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    /**
     * Set the name with the first letter colored.
     */
    private fun setFirstLetterColor(userName: MaterialTextView,fullText: String
                                    ,firstLetterColor: Int) {
        if (fullText.isEmpty()) {
            userName.text = ""
            return
        }
        val spannableString = SpannableString(fullText)
        spannableString.setSpan(
            ForegroundColorSpan(firstLetterColor),
            0, 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        userName.text = spannableString
    }

    override fun getSections(): Array<out Any?>? {
        val sections: MutableList<String> = ArrayList(27)
        val alphabetFull = ArrayList<String>()
        mSectionPositions = ArrayList()
        run {
            var i = 0
            val size = mContactList.size
            while (i < size) {
                val section = mContactList[i].name.first().uppercase(Locale.getDefault())
                if (!sections.contains(section)) {
                    sections.add(section)
                    mSectionPositions?.add(i)
                }
                i++
            }
        }
        for (element in mSections) {
            alphabetFull.add(element.toString())
        }
        sectionsTranslator = sectionsHelper(sections, alphabetFull)
        return alphabetFull.toTypedArray()
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        return mSectionPositions!![sectionsTranslator[sectionIndex]!!]
    }

    override fun getSectionForPosition(position: Int): Int {
       return 0
    }

    /**
     * Hide the back view and show the front view.
     */
    fun hideShowFrontBackView(holder: ContactViewHolder) {
        holder.frontView.visibility = View.VISIBLE
        holder.backView.visibility = View.GONE
    }
}