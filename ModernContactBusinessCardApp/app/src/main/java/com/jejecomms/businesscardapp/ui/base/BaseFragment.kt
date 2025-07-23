package com.jejecomms.businesscardapp.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.jejecomms.businesscardapp.BaseViewModel

/**
 * Fragment base class.
 *
 * @param <Binding> View binding class.
 * @param <ViewModel> View model class.
 */
abstract class BaseFragment <Binding : ViewBinding, ViewModel : BaseViewModel>: Fragment() {
    /**
     * View Model class instance.
     */
    protected var viewModel: ViewModel? = null

    /**
     * View binding class instance.
     */
    protected var mBinding: Binding? = null

    protected val binding get() = mBinding!!

    /**
     * Abstract method to create a view model instance.
     */
    protected abstract fun createViewModel(): ViewModel?

    /**
     * Abstract method to create a view binding instance.
     */
    protected abstract fun createViewBinding(layoutInflater: LayoutInflater?,
                                             container: ViewGroup?): Binding?

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        mBinding = createViewBinding(inflater, container)
        viewModel = createViewModel()

        return mBinding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }
}