package io.nshusa.controller

import io.nshusa.App
import io.nshusa.util.Dialogue
import io.nshusa.util.BSPUtils
import io.nshusa.util.ImageUtils
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.stage.StageStyle
import net.openrs.cache.sprite.Sprite
import org.CacheManager
import org.displee.SpritePacker
import java.awt.image.BufferedImage
import java.io.*
import java.net.URL
import java.nio.ByteBuffer
import java.nio.file.Paths
import java.util.*
import java.nio.file.Files
import javax.imageio.ImageIO

class Controller : Initializable {

    private var offsetX = 0.0

    private var offsetY = 0.0

    private var offsetXFrameViewer = 0.0

    private var offsetYFrameViewer = 0.0

    @FXML
    lateinit var listView: ListView<Sprite>

    @FXML
    lateinit var frameView: ListView<BufferedImage>

    @FXML
    lateinit var searchTf: TextField

    lateinit var placeholderIcon: Image

    private val userHome = Paths.get(".")

    public var observableList: ObservableList<Sprite> = FXCollections.observableArrayList()

    private lateinit var filteredList: FilteredList<Sprite>

    private lateinit var displayedImage: Image

    override fun initialize(location: URL?, resource: ResourceBundle?) {
        App.controller = this

        listView.selectionModel.selectionMode = SelectionMode.MULTIPLE
        try {
            placeholderIcon = Image("icons/placeholder.png")
        } catch (ex: Exception) {
            println("Failed to load icons.")
        }

        filteredList = FilteredList(observableList, { _ -> true })

        searchTf.textProperty().addListener({ _, _, newValue ->
            filteredList.setPredicate({

                if (newValue.isEmpty()) {
                    true
                } else {
                    "${it.index} ${it.frames.size}" == newValue
                }

            })
        })

        listView.items = this.filteredList

        listView.setCellFactory({ _ ->
            object : ListCell<Sprite>() {
                private val imageView = ImageView()

                override fun updateItem(sprite: Sprite?, empty: Boolean) {
                    super.updateItem(sprite, empty)

                    if (empty) {
                        text = ""
                        graphic = null
                    } else {

                        try {
                            val image = sprite?.getFrame(0)
                            imageView.fitWidth = (if (image?.width!! >  128) 128.0 else image?.width?.toDouble()!!)
                            imageView.fitHeight = (if (image?.height!! > 128) 128.0 else image?.height?.toDouble()!!)
                            imageView.isPreserveRatio = true
                            displayedImage = SwingFXUtils.toFXImage(image, null)
                            imageView.image = displayedImage
                            text = "${sprite?.index} ${sprite?.frames.size} sprites"
                            graphic = imageView
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        })
    }

    lateinit var selectedSprite: Sprite

    @FXML
    fun showFrames()
    {
        selectedSprite = listView.selectionModel.selectedItem ?: return

        updateFrameView();
    }

    private lateinit var filteredFrames: FilteredList<BufferedImage>

    var observableFrames: ObservableList<BufferedImage> = FXCollections.observableArrayList()

    fun updateFrameView()
    {
        observableFrames.clear()

        if(selectedSprite == null)
            return

        for (i in selectedSprite.frames)
        {
            observableFrames.add(i)
        }

        filteredFrames = FilteredList(observableFrames, { _ -> true })

        frameView.items = this.filteredFrames

        frameView.setCellFactory({ _ ->
            object : ListCell<BufferedImage>() {
                private val imageView = ImageView()

                override fun updateItem(sprite: BufferedImage?, empty: Boolean) {
                    super.updateItem(sprite, empty)

                    if (empty) {
                        text = ""
                        graphic = null
                    } else {

                        try {
                            var index = 0;
                            for(img in filteredFrames)
                            {
                                if(img == sprite)
                                {
                                    break;
                                }
                                index++;
                            }


                            val image = sprite
                            imageView.fitWidth = (if (image?.width!! >  128) 128.0 else image?.width?.toDouble()!!)
                            imageView.fitHeight = (if (image?.height!! > 128) 128.0 else image?.height?.toDouble()!!)
                            imageView.isPreserveRatio = true
                            displayedImage = SwingFXUtils.toFXImage(image, null)
                            imageView.image = displayedImage
                            text = Integer.toString(index)
                            graphic = imageView
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        })
    }

    @FXML
    fun importImages() {
        val chooser = FileChooser()
        chooser.initialDirectory = userHome.toFile()
        val selectedFiles = chooser.showOpenMultipleDialog(App.mainStage) ?: return

        val sortedFiles = selectedFiles.toTypedArray()

        for (i in 0 until sortedFiles.size) {
            var selectedSprite = selectedSprite

            var images = selectedSprite.frames.toMutableList()

            var image = ImageIO.read(sortedFiles[i])

            images.add(image)

            val frames: Array<BufferedImage?> = kotlin.arrayOfNulls<BufferedImage>(images.size)

            var index = 0
            for (img in images) {
                frames[index] = img
                index++
            }

            selectedSprite.offsetsX.add(0)
            selectedSprite.offsetsY.add(0)
            selectedSprite.subWidths.add(image.getWidth(null))
            selectedSprite.subHeights.add(image.getHeight(null))

            selectedSprite.frames = frames
        }

        SpritePacker.packSprite(selectedSprite.index, selectedSprite)

        update();
        updateFrameView();
    }

    @FXML
    fun exportImages() {
        if (observableList.isEmpty()) {
            Dialogue.showInfo("There isn't anything to export silly!").showAndWait()
            return
        }

        val chooser = DirectoryChooser()
        chooser.initialDirectory = userHome.toFile()
        val selectedDirectory = chooser.showDialog(App.mainStage) ?: return

        val task: Task<Boolean> = object : Task<Boolean>() {

            override fun call(): Boolean {
                for (sprite in filteredList) {
                    var index = 0
                    for(image in sprite.frames)
                    {
                        ImageIO.write(image, "png", File("$selectedDirectory/${sprite.index}_$index"+".png"))
                        index++;
                    }
                }

                Platform.runLater({ Dialogue.openDirectory("Would you like to view the exported sprites?", selectedDirectory) })
                return true
            }
        }

        Thread(task).start()
    }

    @FXML
    fun exportImage()
    {
        var selectedIndex = frameView.selectionModel.selectedIndex ?: return

        var image = selectedSprite.getFrame(selectedIndex)

        val chooser = DirectoryChooser()
        chooser.initialDirectory = userHome.toFile()
        val selectedDirectory = chooser.showDialog(App.mainStage) ?: return

        ImageIO.write(image, "png", File("$selectedDirectory/${selectedSprite.index} $selectedIndex"+".png"))
    }

    @FXML
    fun exportFrames()
    {
        val chooser = DirectoryChooser()
        chooser.initialDirectory = userHome.toFile()
        val selectedDirectory = chooser.showDialog(App.mainStage) ?: return

        selectedSprite = listView.selectionModel.selectedItem ?: return

        var index = 0
        for(image in selectedSprite.frames)
        {
            ImageIO.write(image, "png", File("$selectedDirectory/${selectedSprite.index} $index"+".png"))
            index++;
        }
    }

    @FXML
    fun removeImage() {
        var selectedIndex = frameView.selectionModel.selectedIndex ?: return

        if(selectedSprite.frames.size < 2){
            Dialogue.showInfo("Remove frame instead of the image").showAndWait()
            return
        }

        var images = selectedSprite.frames.toMutableList()

        images.removeAt(selectedIndex)

        val frames : Array<BufferedImage?> = kotlin.arrayOfNulls<BufferedImage>(images.size)

        var index = 0
        for(img in images)
        {
            frames[index] = img
            index++
        }

        selectedSprite.frames = frames

        SpritePacker.packSprite(selectedSprite.index,  selectedSprite)

        update()
        updateFrameView()
    }

    @FXML
    fun replaceImage() {
        val chooser = FileChooser()
        chooser.initialDirectory = userHome.toFile()
        val selectedFile = chooser.showOpenDialog(App.mainStage) ?: return

        var selectedIndex = frameView.selectionModel.selectedIndex ?: return

        selectedSprite.frames[selectedIndex] = ImageIO.read(selectedFile)

        selectedSprite.subWidths[selectedIndex] = selectedSprite.frames[selectedIndex].width
        selectedSprite.subHeights[selectedIndex] = selectedSprite.frames[selectedIndex].height

        println(selectedSprite.offsetsX[selectedIndex])
        println(selectedSprite.offsetsY[selectedIndex])

        selectedSprite.offsetsX[selectedIndex] = 0;
        selectedSprite.offsetsY[selectedIndex] = 0;

        SpritePacker.packSprite(selectedSprite.index,  selectedSprite)

        update()
        updateFrameView()
    }

    @FXML
    fun importBinary() {

        val chooser = FileChooser()
        chooser.initialDirectory = userHome.toFile()
        chooser.extensionFilters.add(FileChooser.ExtensionFilter("main_file_cache.dat2", "*.dat2"))
        val selectedFile = chooser.showOpenDialog(App.mainStage) ?: return

        CacheManager.loadCache(observableList, selectedFile);
    }

    @FXML
    fun importFrames(){
        val chooser = FileChooser()
        chooser.initialDirectory = userHome.toFile()
        val selectedFiles = chooser.showOpenMultipleDialog(App.mainStage) ?: return

        val sortedFiles = selectedFiles.toTypedArray()

        val frames = mutableListOf<BufferedImage>()

        val offsetsX = arrayListOf<Int>()
        val offsetsY = arrayListOf<Int>()
        val subWidths = arrayListOf<Int>()
        val subHeights = arrayListOf<Int>()

        var maxWidth = 0
        var maxHeight = 0

        for (i in 0 until sortedFiles.size) {
            val img = ImageIO.read(sortedFiles[i])
            frames.add(img)

            offsetsX.add(0)
            offsetsY.add(0)
            subWidths.add(img.width)
            subHeights.add(img.height)

            if(img.width > maxWidth)
            {
                maxWidth = img.width
            }

            if(img.height > maxHeight)
            {
                maxHeight = img.height
            }
        }

        val sprite = Sprite(maxWidth, maxHeight, frames.toTypedArray())

        sprite.offsetsX = offsetsX
        sprite.offsetsY = offsetsY
        sprite.subWidths = subWidths
        sprite.subHeights = subHeights

        sprite.index = observableList.size

        SpritePacker.addSprite(sprite)

        observableList.add(sprite)

        update()
    }

    @FXML
    fun removeFrames(){
        var selectedIndex = listView.selectionModel.selectedIndex ?: return

        selectedSprite = listView.selectionModel.selectedItem ?: return

        SpritePacker.removeFrame(selectedIndex)

        observableList.remove(selectedSprite)

        update()
    }

    fun update()
    {
        filteredList = FilteredList(observableList, { _ -> true })

        searchTf.textProperty().addListener({ _, _, newValue ->
            filteredList.setPredicate({

                if (newValue.isEmpty()) {
                    true
                } else {
                    "${it.index} ${it.frames.size}" == newValue
                }

            })
        })

        listView.items = this.filteredList

        listView.setCellFactory({ _ ->
            object : ListCell<Sprite>() {
                private val imageView = ImageView()

                override fun updateItem(sprite: Sprite?, empty: Boolean) {
                    super.updateItem(sprite, empty)

                    if (empty) {
                        text = ""
                        graphic = null
                    } else {

                        try {
                            val image = sprite?.getFrame(0)
                            imageView.fitWidth = (if (image?.width!! >  128) 128.0 else image?.width?.toDouble()!!)
                            imageView.fitHeight = (if (image?.height!! > 128) 128.0 else image?.height?.toDouble()!!)
                            imageView.isPreserveRatio = true
                            displayedImage = SwingFXUtils.toFXImage(image, null)
                            imageView.image = displayedImage
                            text = "${sprite?.index} ${sprite?.frames.size} sprites"
                            graphic = imageView
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        })
    }

    @FXML
    fun handleMouseDragged(event: MouseEvent) {
        val stage = App.mainStage

        stage.x = event.screenX - offsetX
        stage.y = event.screenY - offsetY
    }

    @FXML
    fun handleMousePressed(event: MouseEvent) {
        offsetX = event.sceneX
        offsetY = event.sceneY
    }

    @FXML
    fun minimizeProgram() {
        App.mainStage.isIconified = true
    }

    @FXML
    fun closeProgram() {
        Platform.exit()
    }

}