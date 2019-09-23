package ro.code4.monitorizarevot.ui.branch

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.koin.core.inject
import ro.code4.monitorizarevot.R
import ro.code4.monitorizarevot.data.model.BranchDetails
import ro.code4.monitorizarevot.data.model.County
import ro.code4.monitorizarevot.helper.*
import ro.code4.monitorizarevot.repositories.Repository
import ro.code4.monitorizarevot.ui.base.BaseViewModel
import java.util.*


class BranchViewModel : BaseViewModel() {
    private val nextLiveData = SingleLiveEvent<Boolean>()
    private val nextToMainLiveData = SingleLiveEvent<Boolean>()
    private val titleLiveData = MutableLiveData<String>()
    private val branchBarTextLiveData = MutableLiveData<String>()
    private val preferences: SharedPreferences by inject()
    private val app: Application by inject()
    private val repository: Repository by inject()
    private lateinit var selectedCounty: County
    private var selectedBranchNumber: Int = -1
    private lateinit var arrivalTime: Calendar
    private var departureTime: Calendar? = null
    fun next(): LiveData<Boolean> = nextLiveData
    fun nextToMain(): LiveData<Boolean> = nextToMainLiveData

    fun title(): LiveData<String> = titleLiveData
    fun setTitle(title: String) = titleLiveData.postValue(title)

    fun branchBarText(): LiveData<String> = branchBarTextLiveData

    fun getBranchBarText() {
        branchBarTextLiveData.postValue("${selectedCounty.code} $selectedBranchNumber")
    }

    fun validateInputDetails(
        environmentId: Int,
        sexId: Int
    ) {
        when {
            environmentId == -1 -> messageIdToastLiveData.postValue(app.getString(R.string.invalid_branch_environment))
            sexId == -1 -> messageIdToastLiveData.postValue(app.getString(R.string.invalid_branch_sex))
            !::arrivalTime.isInitialized -> messageIdToastLiveData.postValue(app.getString(R.string.invalid_branch_time_in))
            !checkTime() -> messageIdToastLiveData.postValue(app.getString(R.string.invalid_time_input))
            else -> {
                persistSelection(environmentId, sexId)
                nextToMainLiveData.postValue(true)
            }
        }
    }

    private fun persistSelection(environmentId: Int, sexId: Int) {
        val branchDetails = BranchDetails(
            selectedCounty.code,
            selectedBranchNumber,
            environmentId == R.id.urbanEnvironment,
            sexId == R.id.femaleSex,
            arrivalTime.getDateText(),
            departureTime.getDateText()
        )
        repository.saveBranchDetails(branchDetails) //TODO research when to send to backend this info
    }

    private fun checkTime(): Boolean {
        return departureTime == null || arrivalTime.before(departureTime)
    }

    fun selectCounty(county: County?) {
        county?.let {
            selectedCounty = it
        }
    }

    private fun validCounty(): Boolean = ::selectedCounty.isInitialized
    fun validBranchInput(branchNumberText: CharSequence) {
        if (!validCounty()) {
            messageIdToastLiveData.postValue(app.getString(R.string.invalid_branch_county))
            return
        }
        val branchNumber = getBranchNumber(branchNumberText)
        when {
            branchNumberText.isEmpty() -> messageIdToastLiveData.postValue(app.getString(R.string.invalid_branch_number))
            branchNumber <= 0 -> messageIdToastLiveData.postValue(
                app.getString(R.string.invalid_branch_number_minus)
            )
            branchNumber > selectedCounty.branchesCount -> messageIdToastLiveData.postValue(
                app.getString(
                    R.string.invalid_branch_number_max,
                    selectedCounty.name,
                    selectedCounty.branchesCount
                )
            )
            else -> {
                selectedBranchNumber = branchNumber
                preferences.saveCountyCode(selectedCounty.code)
                preferences.saveBranchNumber(selectedBranchNumber)
                nextLiveData.postValue(true)
            }
        }
    }

    private fun getBranchNumber(branchNumberText: CharSequence): Int {
        return try {
            Integer.parseInt(branchNumberText.toString())
        } catch (e: NumberFormatException) {
            -1
        }

    }


    fun setArrivalTime(hourOfDay: Int, minute: Int) {
        arrivalTime = Calendar.getInstance()
        arrivalTime.updateTime(hourOfDay, minute)

    }

    fun setDepartureTime(hourOfDay: Int, minute: Int) {
        departureTime = Calendar.getInstance()
        departureTime?.updateTime(hourOfDay, minute)
    }

    fun getDepartureTime(): String = departureTime?.getTimeText() ?: ""
    fun getArrivalTime(): String = arrivalTime.getTimeText()
}