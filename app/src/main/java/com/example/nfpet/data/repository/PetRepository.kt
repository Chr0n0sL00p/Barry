package com.example.nfpet.data.repository

import android.content.Context
import com.example.nfpet.data.model.PetReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class PetRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("nfpet_prefs", Context.MODE_PRIVATE)
    
    private val _reports = MutableStateFlow<List<PetReport>>(emptyList())
    val reports: StateFlow<List<PetReport>> = _reports.asStateFlow()

    init {
        loadReports()
    }

    @Synchronized
    private fun loadReports() {
        val reportsJson = sharedPrefs.getString("pet_reports", null)
        if (reportsJson != null) {
            try {
                val list = mutableListOf<PetReport>()
                val array = JSONArray(reportsJson)
                for (i in 0 until array.length()) {
                    list.add(PetReport.fromJsonObject(array.getJSONObject(i)))
                }
                // Sort by newest first
                _reports.value = list.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                e.printStackTrace()
                prepopulateMockData()
            }
        } else {
            prepopulateMockData()
        }
    }

    @Synchronized
    fun addReport(report: PetReport) {
        val currentList = _reports.value.toMutableList()
        // Check if report already exists, update or add
        val index = currentList.indexOfFirst { it.id == report.id }
        if (index != -1) {
            currentList[index] = report
        } else {
            currentList.add(report)
        }
        
        val sortedList = currentList.sortedByDescending { it.timestamp }
        _reports.value = sortedList
        saveReportsToDisk(sortedList)
    }

    @Synchronized
    fun deleteReport(reportId: String) {
        val currentList = _reports.value.filter { it.id != reportId }
        _reports.value = currentList
        saveReportsToDisk(currentList)
    }

    private fun saveReportsToDisk(list: List<PetReport>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJsonObject()) }
        sharedPrefs.edit().putString("pet_reports", array.toString()).apply()
    }

    /**
     * Pre-populates the app with highly visual and realistic mock data
     * so that the user gets an immediately premium experience.
     */
    fun prepopulateMockData(userLatitude: Double = -33.4489, userLongitude: Double = -70.6693, city: String = "Santiago") {
        val mockPets = listOf(
            PetReport(
                name = "Max",
                description = "Golden Retriever macho, de 2 años. Lleva un collar azul con su nombre. Muy manso y amigable, se asustó por unos fuegos artificiales.",
                photoUri = "mock_golden",
                latitude = userLatitude + 0.0035,
                longitude = userLongitude - 0.0042,
                city = city,
                reporterName = "Daniela Gómez",
                phoneNumber = "+56912345678",
                status = "LOST",
                timestamp = System.currentTimeMillis() - 3600000 * 2 // 2 hours ago
            ),
            PetReport(
                name = "Luna",
                description = "Gatita Siamés hembra, ojos azules intensos. Responde al sonido de su lata de comida. Es un poco tímida con los extraños.",
                photoUri = "mock_siamese",
                latitude = userLatitude - 0.0028,
                longitude = userLongitude + 0.0051,
                city = city,
                reporterName = "Cristóbal Muñoz",
                phoneNumber = "+56987654321",
                status = "LOST",
                timestamp = System.currentTimeMillis() - 3600000 * 8 // 8 hours ago
            ),
            PetReport(
                name = "Toby",
                description = "Pug carlino arena. Es viejito, tiene cataratas leves y cojea un poco de la pata trasera izquierda. Necesita medicamentos diarios.",
                photoUri = "mock_pug",
                latitude = userLatitude + 0.0062,
                longitude = userLongitude + 0.0015,
                city = city,
                reporterName = "Sofía Altamirano",
                phoneNumber = "+56955554444",
                status = "LOST",
                timestamp = System.currentTimeMillis() - 3600000 * 24 // 24 hours ago
            ),
            PetReport(
                name = "Coco",
                description = "Caniche micro toy blanco recién pelado. Encontrado merodeando la plaza principal de la zona. Es juguetón y busca refugio.",
                photoUri = "mock_poodle",
                latitude = userLatitude - 0.0055,
                longitude = userLongitude - 0.0065,
                city = city,
                reporterName = "Andrés Soto",
                phoneNumber = "+56933332222",
                status = "FOUND", // This pet was found!
                timestamp = System.currentTimeMillis() - 3600000 * 48 // 48 hours ago
            )
        )

        // Only load mocks if the current list is empty or is the default mock list
        val currentJson = sharedPrefs.getString("pet_reports", null)
        if (currentJson == null) {
            _reports.value = mockPets
            saveReportsToDisk(mockPets)
        }
    }

    /**
     * Helper to force refresh or regenerate mock data relative to new coordinates.
     */
    fun updateMockLocations(lat: Double, lng: Double, cityName: String) {
        val current = _reports.value
        // If the reports list only contains mock items (which we identify by having no custom file URI paths or mock strings)
        // or if it contains less than 5 items, we can clear and regenerate them relative to the new city so the map pins are visible.
        val onlyHasMocks = current.all { it.photoUri?.startsWith("mock_") == true }
        if (onlyHasMocks || current.isEmpty()) {
            sharedPrefs.edit().remove("pet_reports").apply()
            prepopulateMockData(lat, lng, cityName)
            loadReports()
        }
    }
}
