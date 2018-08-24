package org.displee.utilities;

import java.nio.ByteBuffer;

import org.displee.cache.index.archive.ArchiveInformation;
import org.displee.io.impl.InputStream;
import org.displee.io.impl.OutputStream;

/**
 * A class used to (de)compress the data of an {@link ArchiveInformation}.
 * @author Displee
 * @author Apache Ah64
 */
public class Compression {

	/**
	 * Compress archive data with the argued compression type.
	 * @param uncompressed The data to compressed.
	 * @param compressionType The compression type.
	 * @param xteas The xteas.
	 * @param revision The revision.
	 * @return The compressed data.
	 */
	public static byte[] compress(byte[] uncompressed, CompressionTypes compressionType, int[] xteas, int revision) {
		final OutputStream outputStream = new OutputStream();
		final byte[] compressed;
		switch(compressionType) {
		case BZIP2:
			compressed = BZIP2Compressor.compress(uncompressed);
			break;
		case GZIP:
			compressed = GZIPCompressor.deflate(uncompressed);
			break;
		default:
			compressed = uncompressed;
			break;
		}
		outputStream.writeByte(compressionType.ordinal());
		outputStream.writeInt(compressed.length);
		if (!compressionType.equals(CompressionTypes.NONE)) {
			outputStream.writeInt(uncompressed.length);
		}
		outputStream.writeBytes(compressed);
		if (xteas != null && (xteas[0] != 0 || xteas[1] != 0 || xteas[2] != 0 || 0 != xteas[3])) {
			outputStream.encodeXTEA(xteas, 5, outputStream.getOffset());
		}
		if (revision != -1) {
			outputStream.writeShort(revision);
		}
		return outputStream.flip();
	}

	/**
	 * Decompress an archive its data.
	 * @param archiveInformation The archive to decompress.
	 * @param keys The tea keys.
	 * @return The decompressed data.
	 */
	public static byte[] decompress(ArchiveInformation archiveInformation, int[] keys) {
		byte[] packedData = archiveInformation.getData();
		InputStream inputStream = new InputStream(packedData);
		if(keys != null && (keys[0] != 0 || keys[1] != 0 || keys[2] != 0 || 0 != keys[3])) {
			inputStream.decodeXTEA(keys, 5, packedData.length);
		}
		int type = inputStream.readByte() & 0xFF;
		archiveInformation.setCompression(CompressionTypes.values()[type]);
		if (type > CompressionTypes.values().length - 1) {
			throw new RuntimeException("Unknown compression type - type=" + type);
		}
		int compressedSize = inputStream.readInt() & 0xFFFFFF;
		if (compressedSize > 1000000) {
			throw new RuntimeException("Compression size is too big - size=" + compressedSize);
		}
		if (type != 0) {
			int decompressedSize = inputStream.readInt() & 0xFFFFFF;
			byte[] decompressed = new byte[decompressedSize];
			if (type == CompressionTypes.BZIP2.ordinal()) {
				BZIP2Compressor.decompress(decompressed, decompressed.length, archiveInformation.getData(), compressedSize, 9);
			} else if (type == CompressionTypes.GZIP.ordinal()) {
				if (!GZIPCompressor.inflate(inputStream, decompressed)) {
					return null;
				}
			}
			return decompressed;
		}
		return inputStream.flip(compressedSize);
	}

	/**
	 * An enumeration of compression types.
	 * @author Displee
	 */
	public enum CompressionTypes {

		/**
		 * {@link #NONE} Represents the 'none' compression type.
		 */
		NONE,

		/**
		 * {@link #BZIP2} Represents the 'bzip2' compression type.
		 */
		BZIP2,

		/**
		 * {@link #GZIP} Represents the 'gzip' compression type.
		 */
		GZIP

	}

}