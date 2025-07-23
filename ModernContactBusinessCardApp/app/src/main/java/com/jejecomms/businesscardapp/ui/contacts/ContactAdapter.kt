package com.jejecomms.businesscardapp.ui.contacts

import AvatarColorProvider
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.jejecomms.businesscardapp.R
import com.jejecomms.businesscardapp.databinding.ItemContactBinding
import com.jejecomms.businesscardapp.model.ContactsModel
import com.jejecomms.businesscardapp.utils.SharedPreferencesManager
import createAvatarPlaceholder

class ContactAdapter(private val context: Context,private val onContactFavoriteClicked: (ContactsModel) -> Unit
) : ListAdapter<ContactsModel, ContactAdapter.ContactViewHolder>(UserDiffCallback()) {

    private lateinit var listContactId: List<String>
    private var lastAnimatedPosition = -1

    inner class ContactViewHolder(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(contactsModel: ContactsModel) {
            with(binding) {
               userCompany.text = contactsModel.company

                listContactId = SharedPreferencesManager.getList<String>(context,
                    SharedPreferencesManager.KEY_USER_FAVORITE)

                if (listContactId.isNotEmpty()) {
                    if(listContactId.contains(contactsModel.id)) {
                        favoriteStar.isChecked = true
                    } else {
                        favoriteStar.isChecked = false
                    }
                }

                favoriteStar.setOnClickListener {
                    onContactFavoriteClicked(contactsModel)
                }

                if (contactsModel.name.isNotEmpty()) {
                    val placeHolderDrawable = createAvatarPlaceholder(
                        contactsModel.name.first().toString().uppercase(),
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
        val contact = getItem(position)
        holder.bind(contact)
        // Apply animation scrolling Up/down.
       val animation = AnimationUtils.loadAnimation(holder.itemView.context
                ,R.anim.fade_slide_in_from_right)
       holder.itemView.startAnimation(animation)
    }

    /**
     *Call this when you want to reset the animation state (e.g., on refresh or filter)
     */
    fun resetAnimationState() {
        lastAnimatedPosition = -1
    }

    override fun onViewDetachedFromWindow(holder: ContactViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    class UserDiffCallback : DiffUtil.ItemCallback<ContactsModel>() {
        override fun areItemsTheSame(oldItem: ContactsModel, newItem: ContactsModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ContactsModel, newItem: ContactsModel): Boolean {
            return oldItem == newItem
        }
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
}