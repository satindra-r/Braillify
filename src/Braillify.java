import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;

import javax.imageio.ImageIO;

public class Braillify {
	public String toBraille(BufferedImage image, int invert, int mode, double brightness, boolean colour) {
		String str = "";
		for (int i = 0; i < image.getHeight(); i += 4) {
			for (int j = 0; j < image.getWidth(); j += 2) {
				int blank = 10240;
				int rSqSum = 0;
				int gSqSum = 0;
				int bSqSum = 0;
				int count = 0;
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
						pixelBrightness = Math.sqrt((pixelColor.getRed() * pixelColor.getRed()
								+ pixelColor.getGreen() * pixelColor.getGreen()
								+ pixelColor.getBlue() * pixelColor.getBlue()) / 3.0);
						break;
					case 3:
						pixelBrightness = Math.max(pixelColor.getRed(),
								Math.max(pixelColor.getGreen(), pixelColor.getBlue()));
						break;
					case 4:
						pixelBrightness = pixelColor.getRed();
						break;
					case 5:
						pixelBrightness = pixelColor.getGreen();
						break;
					case 6:
						pixelBrightness = pixelColor.getBlue();
						break;
					}
					pixelBrightness /= 255;
					if (pixelBrightness == brightness) {
						dot = pixelBrightness > (255 * 0.5);
					} else {
						dot = pixelBrightness > brightness;
					}
					if ((invert == -1) || (dot == true && invert == 0) || (dot == false && invert == 1)) {
						blank += 1 << k;
						rSqSum += pixelColor.getRed() * pixelColor.getRed();
						gSqSum += pixelColor.getGreen() * pixelColor.getGreen();
						bSqSum += pixelColor.getBlue() * pixelColor.getBlue();
						count++;
					}
				}
				if (blank == 10240) {
					str += " ";
				} else {
					String brailleColour = "";
					if (colour) {
						int r = (int) Math.round(Math.sqrt(rSqSum / count));
						int g = (int) Math.round(Math.sqrt(gSqSum / count));
						int b = (int) Math.round(Math.sqrt(bSqSum / count));

						brailleColour += "\033[38;2;";
						brailleColour += r + ";";
						brailleColour += g + ";";
						brailleColour += b + "m";
					}
					str += (brailleColour + (char) blank);
				}
			}
			str += "\n";
		}
		return str;
	}

	public String init(BufferedImage inBImage, int width, int height, int invert, int mode, double brightness,
			boolean colour) {
		Image outImage = inBImage.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH);
		BufferedImage outBImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		outBImage.getGraphics().drawImage(outImage, 0, 0, null);
		return toBraille(outBImage, invert, mode, brightness, colour);
	}

	public static void main(String[] args) {
		String inPath = "";
		File image;
		BufferedImage inBImage = null;
		String outPath = "";
		File braille;
		int width = -1;
		int height = -1;
		int brightness = 50;
		int mode = 1;
		int invert = 0;
		boolean colour = true;
		try {
			for (int i = 0; i < args.length; i += 2) {
				if (args[i].charAt(0) != '-') {
					throw new Exception();
				}
				switch (args[i].charAt(1)) {
				case 'p':
					inPath = args[i + 1].replaceAll("\"", "").replaceAll("'", "");
					break;
				case 'o':
					outPath = args[i + 1].replaceAll("\"", "").replaceAll("'", "");
					break;
				case 'd':
					width = Integer.parseInt(args[i + 1].split(",")[0]);
					height = Integer.parseInt(args[i + 1].split(",")[1]);
					break;
				case 'i':
					switch (args[i + 1].toUpperCase().charAt(0)) {
					case 'Y':
						invert = 1;
						break;
					case 'N':
						invert = 0;
						break;
					case 'B':
						invert = -1;
						break;
					}
					break;
				case 'm':
					switch (args[i + 1].toLowerCase()) {
					case "min":
						mode = 0;
						break;
					case "avg":
						mode = 1;
						break;
					case "rms":
						mode = 2;
						break;
					case "max":
						mode = 3;
						break;
					case "r":
						mode = 4;
						break;
					case "g":
						mode = 5;
						break;
					case "b":
						mode = 6;
						break;
					}
					break;
				case 'b':
					brightness = Integer.parseInt(args[i + 1]);
					break;
				default:
					System.out.println("Unknown flag: " + args[i]);
					throw new Exception();
				}
			}

			image = new File(inPath);
			if (!(image.exists() && image.isFile() && image.canRead())) {
				System.out.println("Image not found at: " + inPath);
				throw new Exception();
			}
			if (outPath != "") {
				braille = new File(outPath);
				braille.createNewFile();
				if (!braille.canWrite()) {
					System.out.println("Cannot write to: " + outPath);
					throw new Exception();
				}
			}
			colour = outPath == "";
			inBImage = ImageIO.read(image);
			if (inBImage == null) {
				System.out.println("Cannot read image at: " + inPath);
				throw new Exception();
			}

			if (width == -1 || height == -1) {
				width = inBImage.getWidth();
				height = inBImage.getHeight();
			}
			width += width % 2;
			height += 4 - (height % 4);

			if (colour) {
				System.out.print("\033c");
				System.out.flush();
			}

			Braillify b = new Braillify();
			String brailleText = b.init(inBImage, width, height, invert, mode, brightness / 100.0, colour);

			if (!colour) {
				FileWriter fw = new FileWriter(outPath);
				fw.write(brailleText);
				fw.close();
			} else {
				System.out.println(brailleText);
			}

		} catch (Exception e) {
			System.out.println(
					"Usage: java -jar Braillify.jar -p <image path> [-o <out path>] [-d <width>,<height>] [-i <invert (Y)es/(N)o/(B)oth>] [-m <mode min/avg/rms/max/r/g/b>] [-b <brightness%>]");
			System.exit(0);
		}

	}

}
