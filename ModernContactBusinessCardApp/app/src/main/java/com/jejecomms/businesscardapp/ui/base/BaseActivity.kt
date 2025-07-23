package com.jejecomms.businesscardapp

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * Activity base class.
 *
 * @param <Binding> View binding class.
 * @param <ViewModel> View model class.
 */
abstract class BaseActivity<Binding : ViewBinding, ViewModel : BaseViewModel>
    : AppCompatActivity() {

    /**
     * View Model class instance.
     */
    protected var viewModel: ViewModel? = null

    /**
     * View binding class instance.
     */
    protected var binding: Binding? = null

    /**
     * Abstract method to create a view model instance.
     */
    protected abstract fun createViewModel(): ViewModel?

    /**
     * Abstract method to create a view binding instance.
     */
    protected abstract fun createViewBinding(layoutInflater: LayoutInflater?): Binding?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = createViewBinding(LayoutInflater.from(this))
        setContentView(binding?.getRoot())
        viewModel = createViewModel()
    }
}