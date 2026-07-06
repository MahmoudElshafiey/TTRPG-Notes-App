package com.example.dndnotes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dndnotes.data.model.Campaign
import com.example.dndnotes.data.repository.DndRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CampaignViewModel(private val repository: DndRepository) : ViewModel() {
    val allCampaigns: StateFlow<List<Campaign>> = repository.allCampaigns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCampaign(name: String, description: String = "") {
        viewModelScope.launch {
            repository.insertCampaign(Campaign(name = name, description = description))
        }
    }

    fun updateCampaign(campaign: Campaign) {
        viewModelScope.launch {
            repository.updateCampaign(campaign)
        }
    }

    fun deleteCampaign(campaign: Campaign) {
        viewModelScope.launch {
            repository.deleteCampaign(campaign)
        }
    }
}
