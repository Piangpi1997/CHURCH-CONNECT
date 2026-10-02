package org.cmf.churchconnect

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import org.cmf.churchconnect.data.ChurchRepository
import org.cmf.churchconnect.data.FirebaseChurchRepository
import org.cmf.churchconnect.data.LocalDemoRepository
import org.cmf.churchconnect.ui.ChurchConnectApp
import org.cmf.churchconnect.ui.ChurchViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private lateinit var repository: ChurchRepository
    private var churchViewModel: ChurchViewModel? = null
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) churchViewModel?.enablePushNotifications()
    }
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firebase = FirebaseApp.initializeApp(applicationContext)
        if (firebase != null) AppCheckInstaller.install(firebase)
        repository = if (firebase != null) FirebaseChurchRepository() else LocalDemoRepository()
        setContent {
            val vm: ChurchViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = ChurchViewModel(repository, application) as T
            })
            churchViewModel = vm
            ChurchConnectApp(
                vm = vm,
                onEnablePush = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else vm.enablePushNotifications()
                },
                onLanguageChanged = { tag ->
                    AppLocale.save(this, tag)
                    vm.dismissMessage()
                    recreate()
                }
            )
        }
    }
}
