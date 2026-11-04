package com.pomori

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Uygulama girişi. Hilt'in bağımlılık grafiğini başlatır. */
@HiltAndroidApp
class PomoriApp : Application()
