import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;

import javax.imageio.ImageIO;

public class Braillify {
	public int clamp(int val, int min, int max) {
		return Math.min(Math.max(val, min), max);
	}

	public BufferedImage convolute(BufferedImage image, double[][] kernel) {
		int size = kernel.length;
		BufferedImage imageOut = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		for (int j = 0; j < image.getHeight(); j++) {
			for (int i = 0; i < image.getWidth(); i++) {
				Color[][] p = new Color[size][size];
				for (int l = 0; l < size; l++) {
					for (int k = 0; k < size; k++) {
						p[k][l] = new Color(image.getRGB(clamp(i + k - size / 2, 0, image.getWidth() - 1),
								clamp(j + l - size / 2, 0, image.getHeight() - 1)));
					}
				}
				double sumr = 0;
				double sumg = 0;
				double sumb = 0;
				for (int l = 0; l < size; l++) {
					for (int k = 0; k < size; k++) {
						sumr += p[k][l].getRed() * kernel[k][l];
						sumg += p[k][l].getGreen() * kernel[k][l];
						sumb += p[k][l].getBlue() * kernel[k][l];
					}
				}
				imageOut.setRGB(i, j, new Color((int) sumr, (int) sumg, (int) sumb).getRGB());
			}
		}
		return imageOut;
	}

	public BufferedImage convoluteComplex(BufferedImage image, double[][] kernelReal, double[][] kernelImag) {
		int size = kernelReal.length;
		BufferedImage imageOut = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		for (int j = 0; j < image.getHeight(); j++) {
			for (int i = 0; i < image.getWidth(); i++) {
				Color[][] p = new Color[size][size];

				for (int l = 0; l < size; l++) {
					for (int k = 0; k < size; k++) {
						p[k][l] = new Color(image.getRGB(clamp(i + k - size / 2, 0, image.getWidth() - 1),
								clamp(j + l - size / 2, 0, image.getHeight() - 1)));
					}
				}
				double sumRealr = 0;
				double sumRealg = 0;
				double sumRealb = 0;
				double sumImagr = 0;
				double sumImagg = 0;
				double sumImagb = 0;
				for (int l = 0; l < size; l++) {
					for (int k = 0; k < size; k++) {
						sumRealr += p[k][l].getRed() * kernelReal[k][l];
						sumRealg += p[k][l].getGreen() * kernelReal[k][l];
						sumRealb += p[k][l].getBlue() * kernelReal[k][l];
						sumImagr += p[k][l].getRed() * kernelImag[k][l];
						sumImagg += p[k][l].getGreen() * kernelImag[k][l];
						sumImagb += p[k][l].getBlue() * kernelImag[k][l];
					}
				}
				imageOut.setRGB(i, j,
						new Color((int) Math.sqrt((sumRealr * sumRealr + sumImagr * sumImagr) / 2),
								(int) Math.sqrt((sumRealg * sumRealg + sumImagg * sumImagg) / 2),
								(int) Math.sqrt((sumRealb * sumRealb + sumImagb * sumImagb) / 2)).getRGB());
			}
		}
		return imageOut;
	}

	public BufferedImage edgeDetect(BufferedImage image) {
		double[][] gaussian = { { 1 / 16.0, 2 / 16.0, 1 / 16.0 }, { 2 / 16.0, 4 / 16.0, 2 / 16.0 },
				{ 1 / 16.0, 2 / 16.0, 1 / 16.0 } };
		double[][] sobelx = { { 1 / 4.0, 0, -1 / 4.0 }, { 2 / 4.0, 0, -2 / 4.0 }, { 1 / 4.0, 0, -1 / 4.0 } };
		double[][] sobely = { { 1 / 4.0, 2 / 4.0, 1 / 4.0 }, { 0, 0, 0 }, { -1 / 4.0, -2 / 4.0, -1 / 4.0 } };
		image = convolute(image, gaussian);
		image = convoluteComplex(image, sobelx, sobely);
		return image;
	}

	public String toBraille(BufferedImage image, char space, boolean invert, boolean edge, int mode, double brightness,
			boolean colour) {
		String str = "";
		BufferedImage brightnessImage = null;
		if (edge) {
			brightnessImage = edgeDetect(image);
		} else {
			brightnessImage = image;
		}
		for (int i = 0; i < image.getHeight(); i += 4) {
			for (int j = 0; j < image.getWidth(); j += 2) {
				int blank = 10240;
				int rSqSum = 0;
				int gSqSum = 0;
				int bSqSum = 0;
				int count = 0;
				for (int k = 0; k < 8; k++) {
					Color pixelColor;
					Color pixelBrightnessColor;
					if (k < 6) {
						pixelColor = new Color(image.getRGB(j + k / 3, i + k % 3));
						pixelBrightnessColor = new Color(brightnessImage.getRGB(j + k / 3, i + k % 3));
					} else {
						pixelColor = new Color(image.getRGB(j + k % 2, i + 3));
						pixelBrightnessColor = new Color(brightnessImage.getRGB(j + k % 2, i + 3));
					}
					boolean dot = false;
					double pixelBrightness = -1;
					switch (mode) {
					case 0:
						pixelBrightness = Math.min(pixelBrightnessColor.getRed(),
								Math.min(pixelBrightnessColor.getGreen(), pixelBrightnessColor.getBlue()));
						break;
					case 1:
						pixelBrightness = Math.sqrt((pixelBrightnessColor.getRed() * pixelBrightnessColor.getRed()
								+ pixelBrightnessColor.getGreen() * pixelBrightnessColor.getGreen()
								+ pixelBrightnessColor.getBlue() * pixelBrightnessColor.getBlue()) / 3.0);
						break;
					case 2:
						pixelBrightness = Math.max(pixelBrightnessColor.getRed(),
								Math.max(pixelBrightnessColor.getGreen(), pixelBrightnessColor.getBlue()));
						break;
					case 3:
						pixelBrightness = pixelBrightnessColor.getRed();
						break;
					case 4:
						pixelBrightness = pixelBrightnessColor.getGreen();
						break;
					case 5:
						pixelBrightness = pixelBrightnessColor.getBlue();
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
						rSqSum += pixelColor.getRed() * pixelColor.getRed();
						gSqSum += pixelColor.getGreen() * pixelColor.getGreen();
						bSqSum += pixelColor.getBlue() * pixelColor.getBlue();
						count++;
					}
				}
				if (blank == 10240) {
					if (colour) {
						str += "\033[38;2;0;0;0m";
					}
					str += space;
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

	public String init(BufferedImage inBImage, int width, int height, char space, boolean invert, boolean edge,
			int mode, double brightness, boolean colour) {
		Image outImage = inBImage.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH);
		BufferedImage outBImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		outBImage.getGraphics().drawImage(outImage, 0, 0, null);
		return toBraille(outBImage, space, invert, edge, mode, brightness, colour);
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
		int mode = 2;
		char space = ' ';
		boolean invert = false;
		boolean edge = false;
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
				case 's':
					switch (args[i + 1].toLowerCase()) {
					case "space":
						space = ' ';
						break;
					case "blank":
						space = (char) 10240;
						break;
					case "dot":
						space = (char) 10241;
						break;
					}
					break;
				case 'i':
					invert = (args[i + 1].toUpperCase().charAt(0) == 'Y');
					break;
				case 'e':
					edge = (args[i + 1].toUpperCase().charAt(0) == 'Y');
					break;
				case 'c':
					colour = (args[i + 1].toUpperCase().charAt(0) == 'Y');
					break;
				case 'm':
					switch (args[i + 1].toLowerCase()) {
					case "min":
						mode = 0;
						break;
					case "rms":
						mode = 1;
						break;
					case "max":
						mode = 2;
						break;
					case "r":
						mode = 3;
						break;
					case "g":
						mode = 4;
						break;
					case "b":
						mode = 5;
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

			if (outPath == "") {
				System.out.print("\033c");
				System.out.flush();
			}

			Braillify b = new Braillify();
			String brailleText = b.init(inBImage, width, height, space, invert, edge, mode, brightness / 100.0, colour);

			if (outPath != "") {
				FileWriter fw = new FileWriter(outPath);
				fw.write(brailleText);
				fw.close();
			} else {
				System.out.println(brailleText);
			}

		} catch (Exception e) {
			System.out.println(
					"Usage: java -jar Braillify.jar -p <image path> [-o <out path>] [-d <width>,<height>] [-s <space character space/blank/dot>] [-i <invert Y/N>] [-e <edges Y/N>] [-c <colour Y/N>] [-m <mode min/rms/max/r/g/b>] [-b <brightness%>]");
			e.printStackTrace();
			System.exit(0);
		}

	}

}
