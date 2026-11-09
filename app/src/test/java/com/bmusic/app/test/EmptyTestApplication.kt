package com.bmusic.app.test

import android.app.Application

/**
 * Application Robolectric sans Koin (évite KoinApplicationAlreadyStartedException
 * quand la suite complète crée `App` plusieurs fois dans le même JVM).
 */
class EmptyTestApplication : Application()
