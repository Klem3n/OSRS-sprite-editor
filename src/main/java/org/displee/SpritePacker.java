package org.displee;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import io.nshusa.util.Dialogue;
import org.CacheManager;
import org.displee.cache.index.archive.Archive;

import net.openrs.cache.Cache;
import net.openrs.cache.FileStore;
import net.openrs.cache.sprite.Sprite;
import org.jetbrains.annotations.NotNull;

public class SpritePacker {

	public static void packSprite(int spriteId, Sprite sprite)
	{
		try{
			Sprite.decode(sprite.encode());
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		try {
			ByteBuffer buffer = sprite.encode();

			CacheLibrary library = CacheManager.cacheLibrary;

			Archive a = library.getIndex(8).getArchive(spriteId);

			a.addFile(0, buffer.array(), a.getFile(0).getName());

			library.getIndex(8).update();//Write custom sprite class

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void addSprite(@NotNull Sprite selectedSprite) throws IOException {
		CacheLibrary library = CacheManager.cacheLibrary;

		Archive a = library.getIndex(8).addArchive();

		a.addFile(selectedSprite.encode().array());

		library.getIndex(8).update();
	}

    public static void removeFrame(int selectedIndex) {
        CacheLibrary library = CacheManager.cacheLibrary;

        library.getIndex(8).removeArchive(selectedIndex);

        library.getIndex(8).update();
    }
}
