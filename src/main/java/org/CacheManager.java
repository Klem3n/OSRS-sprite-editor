package org;

import io.nshusa.controller.Controller;
import javafx.collections.ObservableList;
import net.openrs.cache.Cache;
import net.openrs.cache.Container;
import net.openrs.cache.FileStore;
import net.openrs.cache.ReferenceTable;
import net.openrs.cache.sprite.Sprite;
import org.displee.CacheLibrary;
import org.displee.CacheLibraryMode;
import org.displee.cache.index.Index;
import org.displee.cache.index.archive.file.File;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class CacheManager {

    public static CacheLibrary cacheLibrary;
    public static List<Sprite> sprites = new ArrayList<>();

    public static void loadCache(ObservableList<Sprite> observableList, java.io.File selectedFile) throws Exception {

        cacheLibrary = new CacheLibrary(selectedFile.getParentFile().getAbsolutePath().toString() + "\\", CacheLibraryMode.CACHED, 172);

        Index archive = cacheLibrary.getIndex(8);

        try (Cache cache = new Cache(FileStore.open(selectedFile.getParentFile().getAbsolutePath().toString() + "\\"))) {
            ReferenceTable table = cache.getReferenceTable(8);
            for (int i = 0; i < table.capacity(); i++) {
                if (table.getEntry(i) == null)
                    continue;

                Container container = cache.read(8, i);
                Sprite sprite = Sprite.decode(container.getData());
                sprite.setIndex(i);
                sprites.add(sprite);
            }
        }

        int count = 0;

        for (Sprite sprite : sprites) {
            try {
                int index = 0;
                    /*for(BufferedImage image : sprite.getFrames())
                    {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write( image, "jpg", baos );
                        baos.flush();
                        byte[] imageInByte = baos.toByteArray();

                        io.nshusa.component.Sprite s = new io.nshusa.component.Sprite(count + index, imageInByte, "");

                        observableList.add(s);
                        index++;
                    }*/

                observableList.add(sprite);
                index++;

            } catch (Exception ex) {
                ex.printStackTrace();
            }
            count++;
        }

    }
}
