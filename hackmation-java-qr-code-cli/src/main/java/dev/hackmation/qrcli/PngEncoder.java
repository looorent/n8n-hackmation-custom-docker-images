package dev.hackmation.qrcli;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import com.google.zxing.common.BitMatrix;

/**
 * Minimal 8-bit grayscale PNG encoder for {@link BitMatrix}.
 *
 * Hand-rolled instead of using javax.imageio so the native image does not
 * pull in java.awt, which would prevent a fully static executable.
 */
public final class PngEncoder {

	private static final byte[] PNG_SIGNATURE = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };

	private PngEncoder() {}

	public static byte[] encode(BitMatrix matrix) {
		int width = matrix.getWidth();
		int height = matrix.getHeight();

		byte[] scanlines = new byte[height * (width + 1)];
		int i = 0;
		for (int y = 0; y < height; y++) {
			scanlines[i++] = 0; // filter type: none
			for (int x = 0; x < width; x++) {
				scanlines[i++] = matrix.get(x, y) ? 0 : (byte) 0xFF;
			}
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.writeBytes(PNG_SIGNATURE);
		writeChunk(out, "IHDR", ByteBuffer.allocate(13)
				.putInt(width)
				.putInt(height)
				.put((byte) 8) // bit depth
				.put((byte) 0) // color type: grayscale
				.put((byte) 0) // compression
				.put((byte) 0) // filter
				.put((byte) 0) // interlace
				.array());
		writeChunk(out, "IDAT", deflate(scanlines));
		writeChunk(out, "IEND", new byte[0]);
		return out.toByteArray();
	}

	private static void writeChunk(ByteArrayOutputStream out, String type, byte[] data) {
		byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
		out.writeBytes(ByteBuffer.allocate(4).putInt(data.length).array());
		out.writeBytes(typeBytes);
		out.writeBytes(data);
		CRC32 crc = new CRC32();
		crc.update(typeBytes);
		crc.update(data);
		out.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
	}

	private static byte[] deflate(byte[] raw) {
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
		try {
			deflater.setInput(raw);
			deflater.finish();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			while (!deflater.finished()) {
				out.write(buffer, 0, deflater.deflate(buffer));
			}
			return out.toByteArray();
		} finally {
			deflater.end();
		}
	}
}
