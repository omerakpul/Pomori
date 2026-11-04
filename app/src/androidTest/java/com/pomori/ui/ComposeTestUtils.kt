package com.pomori.ui

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule

typealias PomoriComposeRule = AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>

/** Hilt gerektirmeyen, durumsuz ekranları test etmek için boş bir ComponentActivity kuralı. */
fun pomoriComposeRule(): PomoriComposeRule = createAndroidComposeRule<ComponentActivity>()

/** Test cihazının dilinden bağımsız olarak, ekrandaki metni kaynaktan okur. */
fun PomoriComposeRule.str(@StringRes id: Int, vararg args: Any): String = activity.getString(id, *args)
