package dev.hackmation.qrcli;

import java.util.Base64;
import java.util.concurrent.Callable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@TopCommand
@Command(
		name = "qr-code-cli",
		mixinStandardHelpOptions = true,
		version = "1.0.0",
		description = "Encodes a string as a QR code and prints it as a Base64-encoded PNG.")
public class QrCommand implements Callable<Integer> {

	@Parameters(index = "0", paramLabel = "TEXT", description = "The text to encode.")
	String text;

	@Option(names = { "-s", "--size" }, defaultValue = "256", description = "Image size in pixels (default: ${DEFAULT-VALUE}).")
	int size;

	@Override
	public Integer call() throws Exception {
		BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
		System.out.println(Base64.getEncoder().encodeToString(PngEncoder.encode(matrix)));
		return 0;
	}
}
