import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class Braillify {
	public String toBraille(BufferedImage image, boolean invert, int mode, double brightness) {
		String str = "";
		for (int i = 0; i < image.getHeight(); i += 4) {
			for (int j = 0; j < image.getWidth(); j += 2) {
				int blank = 10240;
				for (int k = 0; k < 8; k++) {
					Color pixelColor;
					if (k < 6) {
						pixelColor = new Color(image.getRGB(j + k / 3, i + k % 3));
					} else {
						pixelColor = new Color(image.getRGB(j + k % 2, i + 3));
					}
					boolean dot = false;
					double pixelBrightness = -1;
					switch (mode) {
					case 0:
						pixelBrightness = Math.min(pixelColor.getRed(),
								Math.min(pixelColor.getGreen(), pixelColor.getBlue()));
						break;
					case 1:
						pixelBrightness = (pixelColor.getRed() + pixelColor.getGreen() + pixelColor.getBlue()) / 3.0;
						break;
					case 2:
						pixelBrightness = Math.max(pixelColor.getRed(),
								Math.max(pixelColor.getGreen(), pixelColor.getBlue()));
						break;
					}
					pixelBrightness /= 255;
					if (pixelBrightness == brightness) {
						dot = pixelBrightness > (255 * 0.5);
					} else {
						dot = pixelBrightness > brightness;
					}
					if (dot != invert) {
						blank += 1 << k;
					}
				}
				if (blank == 10240) {
					str += " ";
				} else {
					str += ((char) blank);
				}
			}
			str += "\n";
		}
		return str;
	}

	public void init(BufferedImage inBImage, int width, int height, boolean invert, int mode, double brightness) {
		Image outImage = inBImage.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH);
		BufferedImage outBImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		outBImage.getGraphics().drawImage(outImage, 0, 0, null);
		System.out.println(toBraille(outBImage, invert, mode, brightness));
	}

	public static void main(String[] args) {
		String path = "";
		int width = -1;
		int height = -1;
		int brightness = 50;
		int mode = 1;
		boolean invert = false;
		File image;
		BufferedImage inBImage = null;
		try {
			for (int i = 0; i < args.length; i += 2) {
				if (args[i].charAt(0) != '-') {
					throw new Exception();
				}
				switch (args[i].charAt(1)) {
				case 'p':
					path = args[i + 1];
					break;
				case 'd':
					width = Integer.parseInt(args[i + 1].split(",")[0]);
					height = Integer.parseInt(args[i + 1].split(",")[1]);
					break;
				case 'i':
					invert = args[i + 1].toUpperCase().charAt(0) == 'Y';
					break;
				case 'm':
					switch (args[i + 1].toLowerCase()) {
					case "min":
						mode = 0;
						break;
					case "avg":
						mode = 1;
						break;
					case "max":
						mode = 2;
						break;
					}
					break;
				case 'b':
					brightness = Integer.parseInt(args[i + 1]);
					break;
				default:
					throw new Exception();
				}
			}
			image = new File(path.replaceAll("\"", ""));
			if (image.exists() == false || image.isFile() == false || image.canRead() == false) {
				throw new Exception();
			}
			inBImage = ImageIO.read(image);
			if (inBImage == null) {
				throw new Exception();
			}
			if (width == -1 || height == -1) {
				width = inBImage.getWidth();
				height = inBImage.getHeight();
			}
			width += width % 2;
			height += 4 - (height % 4);
		} catch (Exception e) {
			System.out.println(
					"Usage: java -jar Braillify.jar -p <image path> [-d <width>,<height>] [-i <invert Y/N>] [-m <mode min/avg/max>] [-b <brightness%>]");
			System.exit(0);
		}
		System.out.print("\033c");
		System.out.flush();
		Braillify b = new Braillify();
		b.init(inBImage, width, height, invert, mode, brightness / 100.0);
	}

}
