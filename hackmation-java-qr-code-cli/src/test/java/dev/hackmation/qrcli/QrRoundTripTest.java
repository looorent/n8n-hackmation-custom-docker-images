package dev.hackmation.qrcli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

class QrRoundTripTest {

	@Test
	void encodedPngIsAValidQrCodeOfTheInputText() throws Exception {
		String text = "hello hackmation";
		BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 256, 256);

		byte[] png = PngEncoder.encode(matrix);

		assertArrayEquals(
				new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' },
				Arrays.copyOf(png, 8));

		BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
		assertEquals(256, image.getWidth());
		assertEquals(256, image.getHeight());

		Result decoded = new MultiFormatReader()
				.decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image))));
		assertEquals(text, decoded.getText());
	}
}
