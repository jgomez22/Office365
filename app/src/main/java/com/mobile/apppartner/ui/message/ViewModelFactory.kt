package com.mobile.apppartner.ui.message

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.support.v7.app.AppCompatActivity
import com.mobile.apppartner.ui.chat.UserMatchListViewModel

class ViewModelFactory(private val uid: String): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageListViewModel::class.java)) {
            return MessageListViewModel(uid) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")

    }
}