package com.prettierjavaplugin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(
    name = "PrettierJavaSettings",
    storages = [Storage("prettierJavaSettings.xml")]
)
class PrettierJavaSettings : PersistentStateComponent<PrettierJavaSettings.State> {

    data class State(
        var globalProfile: String = "Enterprise/Spring", // Profiles: "Enterprise/Spring", "Google Style", "Custom"
        var enabled: Boolean = true,
        var formatOnSave: Boolean = false
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        @JvmStatic
        fun getInstance(): PrettierJavaSettings = service()
    }
}
