package com.feige.wonderfuleditor

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.feige.wonderfuleditor.model.Document
import com.feige.wonderfuleditor.storage.DocumentManager
import com.feige.wonderfuleditor.ui.screens.AboutScreen
import com.feige.wonderfuleditor.ui.screens.EditorScreen
import com.feige.wonderfuleditor.ui.screens.LibraryScreen
import com.feige.wonderfuleditor.ui.screens.TrashScreen
import com.feige.wonderfuleditor.ui.theme.WonderfulEditorTheme
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var documentManager: DocumentManager
    private lateinit var sharedPref: SharedPreferences

    override fun attachBaseContext(newBase: Context) {
        val sp = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lang = sp.getString("lang", "ru") ?: "ru"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        documentManager = DocumentManager(this)
        sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)

        setContent {
            var currentScreen by remember { mutableStateOf("library") }
            var activeDocument by remember { mutableStateOf<Document?>(null) }
            
            // Настройки темы
            var themeName by remember { 
                mutableStateOf(sharedPref.getString("theme", "dark") ?: "dark") 
            }
            
            // Настройки языка
            val currentLang = remember { 
                sharedPref.getString("lang", "ru") ?: "ru" 
            }

            WonderfulEditorTheme(themeName = themeName) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Обработка кнопки назад
                    BackHandler(enabled = currentScreen != "library") {
                        currentScreen = "library"
                        activeDocument = null
                    }

                    when (currentScreen) {
                        "library" -> {
                            LibraryScreen(
                                documentManager = documentManager,
                                currentTheme = themeName,
                                onThemeChange = { newTheme ->
                                    themeName = newTheme
                                    sharedPref.edit().putString("theme", newTheme).apply()
                                },
                                currentLang = currentLang,
                                onLangChange = { newLang ->
                                    sharedPref.edit().putString("lang", newLang).apply()
                                    // Пересоздаем Activity для смены локали
                                    recreate()
                                },
                                onOpenDocument = { doc ->
                                    activeDocument = doc
                                    currentScreen = "editor"
                                },
                                onCreateDocument = {
                                    val newDoc = Document()
                                    documentManager.saveDocument(newDoc)
                                    activeDocument = newDoc
                                    currentScreen = "editor"
                                },
                                onOpenTrash = {
                                    currentScreen = "trash"
                                },
                                onOpenAbout = {
                                    currentScreen = "about"
                                }
                            )
                        }
                        "editor" -> {
                            activeDocument?.let { doc ->
                                EditorScreen(
                                    document = doc,
                                    documentManager = documentManager,
                                    onBack = {
                                        currentScreen = "library"
                                        activeDocument = null
                                    }
                                )
                            }
                        }
                        "trash" -> {
                            TrashScreen(
                                documentManager = documentManager,
                                onBack = {
                                    currentScreen = "library"
                                }
                            )
                        }
                        "about" -> {
                            AboutScreen(
                                onBack = {
                                    currentScreen = "library"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
