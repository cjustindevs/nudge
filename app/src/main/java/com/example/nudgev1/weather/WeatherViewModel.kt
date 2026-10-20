package com.example.nudgev1.weather

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WeatherUiState(
    val city: String = "Loading...",
    val temp: String = "--°",
    val condition: String = "Fetching weather...",
    val feelsLike: String = "--°",
    val humidity: String = "--%",
    val wind: String = "-- km/h",
    val iconCode: String = "01d",
    val isLoading: Boolean = false,
    val error: String? = null
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val apiKey = "YOUR_OPENWEATHER_API_KEY"

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState

    fun fetchWeatherForCity(cityName: String) {
        if (cityName.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = WeatherApi.service.getWeatherByCity(cityName, apiKey)
                _uiState.value = WeatherUiState(
                    city = response.name,
                    temp = "${response.main.temp.toInt()}°C",
                    condition = response.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Clear",
                    feelsLike = "${response.main.feels_like.toInt()}°",
                    humidity = "${response.main.humidity}%",
                    wind = "${response.wind.speed} km/h",
                    iconCode = response.weather.firstOrNull()?.icon ?: "01d",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = WeatherUiState(
                    city = cityName,
                    temp = "--°C",
                    condition = "Offline / Unknown",
                    isLoading = false,
                    error = "Could not fetch weather for $cityName."
                )
            }
        }
    }
}
