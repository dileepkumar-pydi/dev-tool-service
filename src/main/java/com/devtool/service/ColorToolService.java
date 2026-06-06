package com.devtool.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.devtool.dto.ColorConversionResponse;
import com.devtool.dto.GradientResponse;

@Service
public class ColorToolService {

	public Map<String, List<String>> supportedTools() {
		Map<String, List<String>> tools = new LinkedHashMap<>();
		tools.put("colorTools", List.of("HEX to RGB/HSL", "RGB to HEX/HSL", "HSL to HEX/RGB", "Gradient Generator",
				"Palette Preview"));
		return tools;
	}

	public ColorConversionResponse fromHex(String hex) {
		int[] rgb = parseHex(hex);
		double[] hsl = rgbToHsl(rgb[0], rgb[1], rgb[2]);
		return build("HEX Converter", rgb[0], rgb[1], rgb[2], hsl);
	}

	public ColorConversionResponse fromRgb(int red, int green, int blue) {
		validateRgb(red, green, blue);
		double[] hsl = rgbToHsl(red, green, blue);
		return build("RGB Converter", red, green, blue, hsl);
	}

	public ColorConversionResponse fromHsl(double hue, double saturation, double lightness) {
		validateHsl(hue, saturation, lightness);
		int[] rgb = hslToRgb(hue, saturation, lightness);
		return new ColorConversionResponse("HSL Converter", toHex(rgb[0], rgb[1], rgb[2]), rgb[0], rgb[1], rgb[2], hue,
				saturation, lightness);
	}

	public GradientResponse gradient(String startHex, String endHex, Integer steps) {
		int count = steps == null || steps < 2 ? 5 : steps;
		int[] start = parseHex(startHex);
		int[] end = parseHex(endHex);
		List<String> palette = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			double ratio = count == 1 ? 0 : (double) i / (count - 1);
			int red = interpolate(start[0], end[0], ratio);
			int green = interpolate(start[1], end[1], ratio);
			int blue = interpolate(start[2], end[2], ratio);
			palette.add(toHex(red, green, blue));
		}
		return new GradientResponse("Gradient Generator",
				"linear-gradient(90deg, " + String.join(", ", palette) + ")", palette);
	}

	private ColorConversionResponse build(String tool, int red, int green, int blue, double[] hsl) {
		return new ColorConversionResponse(tool, toHex(red, green, blue), red, green, blue, round(hsl[0]), round(hsl[1]),
				round(hsl[2]));
	}

	private int[] parseHex(String value) {
		String normalized = safeValue(value).trim().replace("#", "");
		if (normalized.length() == 3) {
			normalized = "" + normalized.charAt(0) + normalized.charAt(0) + normalized.charAt(1) + normalized.charAt(1)
					+ normalized.charAt(2) + normalized.charAt(2);
		}
		if (!normalized.matches("[0-9a-fA-F]{6}")) {
			throw new IllegalArgumentException("Invalid HEX color.");
		}
		return new int[] { Integer.parseInt(normalized.substring(0, 2), 16), Integer.parseInt(normalized.substring(2, 4), 16),
				Integer.parseInt(normalized.substring(4, 6), 16) };
	}

	private String toHex(int red, int green, int blue) {
		return String.format(Locale.ROOT, "#%02X%02X%02X", red, green, blue);
	}

	private double[] rgbToHsl(int red, int green, int blue) {
		double r = red / 255.0;
		double g = green / 255.0;
		double b = blue / 255.0;
		double max = Math.max(r, Math.max(g, b));
		double min = Math.min(r, Math.min(g, b));
		double h;
		double s;
		double l = (max + min) / 2.0;

		if (max == min) {
			h = 0;
			s = 0;
		} else {
			double delta = max - min;
			s = l > 0.5 ? delta / (2.0 - max - min) : delta / (max + min);
			if (max == r) {
				h = (g - b) / delta + (g < b ? 6 : 0);
			} else if (max == g) {
				h = (b - r) / delta + 2;
			} else {
				h = (r - g) / delta + 4;
			}
			h *= 60;
		}
		return new double[] { h, s * 100, l * 100 };
	}

	private int[] hslToRgb(double hue, double saturation, double lightness) {
		double h = hue / 360.0;
		double s = saturation / 100.0;
		double l = lightness / 100.0;

		double r;
		double g;
		double b;
		if (s == 0) {
			r = g = b = l;
		} else {
			double q = l < 0.5 ? l * (1 + s) : (l + s) - (l * s);
			double p = 2 * l - q;
			r = hueToRgb(p, q, h + 1.0 / 3.0);
			g = hueToRgb(p, q, h);
			b = hueToRgb(p, q, h - 1.0 / 3.0);
		}
		return new int[] { (int) Math.round(r * 255), (int) Math.round(g * 255), (int) Math.round(b * 255) };
	}

	private double hueToRgb(double p, double q, double t) {
		if (t < 0) {
			t += 1;
		}
		if (t > 1) {
			t -= 1;
		}
		if (t < 1.0 / 6.0) {
			return p + (q - p) * 6 * t;
		}
		if (t < 1.0 / 2.0) {
			return q;
		}
		if (t < 2.0 / 3.0) {
			return p + (q - p) * (2.0 / 3.0 - t) * 6;
		}
		return p;
	}

	private int interpolate(int start, int end, double ratio) {
		return (int) Math.round(start + (end - start) * ratio);
	}

	private void validateRgb(int red, int green, int blue) {
		if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
			throw new IllegalArgumentException("RGB values must be between 0 and 255.");
		}
	}

	private void validateHsl(double hue, double saturation, double lightness) {
		if (hue < 0 || hue > 360 || saturation < 0 || saturation > 100 || lightness < 0 || lightness > 100) {
			throw new IllegalArgumentException("HSL values are out of range.");
		}
	}

	private double round(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
