@file:JvmName("App")

package io.nshusa

import io.nshusa.controller.Controller
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.io.File
import java.nio.file.Paths
import java.util.*

class App : Application() {

    val properties = Properties()

    override fun init() {
        try {
            properties.load(App::class.java.getResourceAsStream("/settings.properties"))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun start(stage: Stage?) {
        mainStage = stage!!
        root = FXMLLoader.load(App::class.java.getResource("/Main.fxml"))
        stage.title = "Better Sprite Packer"
        val scene = Scene(root)
        scene.stylesheets.add(App::class.java.getResource("/style.css").toExternalForm())
        stage.scene = scene
        stage.icons?.add(Image(App::class.java.getResourceAsStream("/icons/icon.png")))
        stage.centerOnScreen()
        stage.initStyle(StageStyle.UNDECORATED)
        stage.show()
    }

    companion object {

        lateinit var mainStage : Stage

        lateinit var root : Parent

        lateinit var controller: Controller

        @JvmStatic
        fun main(args : Array<String>) {
            launch(App::class.java)
        }
    }

}