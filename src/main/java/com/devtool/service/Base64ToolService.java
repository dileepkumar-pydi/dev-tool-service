package com.devtool.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class Base64ToolService {

	private static final Base64.Encoder ENCODER = Base64.getEncoder();
	private static final Base64.Decoder DECODER = Base64.getDecoder();

	public Map<String, List<String>> supportedTools() {
		Map<String, List<String>> tools = new LinkedHashMap<>();
		tools.put("decoders", List.of("Base64 to ASCII", "Base64 to Audio", "Basic Auth Decode", "Base64 to File",
				"Base64 to Hex", "Base64 to Image", "Base64 to PDF", "Base64 to Text", "Base64 to Video"));
		tools.put("encoders", List.of("Audio to Base64", "CSS to Base64", "File to Base64", "Hex to Base64",
				"HTML to Base64", "Image to Base64", "PDF to Base64", "Text to Base64", "URL to Base64",
				"Video to Base64"));
		return tools;
	}

	public String encodeText(String value) {
		return encodeString(value, StandardCharsets.UTF_8);
	}

	public String encodeCss(String value) {
		return encodeString(value, StandardCharsets.UTF_8);
	}

	public String encodeHtml(String value) {
		return encodeString(value, StandardCharsets.UTF_8);
	}

	public String encodeUrl(String value) {
		return encodeString(value, StandardCharsets.UTF_8);
	}

	public String encodeHex(String hexValue) {
		return ENCODER.encodeToString(hexToBytes(hexValue));
	}

	public String encodeFile(MultipartFile file) {
		return wrapFileRead(() -> ENCODER.encodeToString(file.getBytes()));
	}

	public String decodeText(String base64Value) {
		return new String(decodeBase64(base64Value), StandardCharsets.UTF_8);
	}

	public String decodeAscii(String base64Value) {
		return new String(decodeBase64(base64Value), StandardCharsets.US_ASCII);
	}

	public String decodeHex(String base64Value) {
		byte[] decodedBytes = decodeBase64(base64Value);
		StringBuilder hex = new StringBuilder(decodedBytes.length * 2);
		for (byte decodedByte : decodedBytes) {
			hex.append(String.format("%02x", decodedByte));
		}
		return hex.toString();
	}

	public BasicAuthParts decodeBasicAuth(String authorizationHeader) {
		String normalizedValue = authorizationHeader == null ? "" : authorizationHeader.trim();
		if (normalizedValue.regionMatches(true, 0, "Basic ", 0, 6)) {
			normalizedValue = normalizedValue.substring(6).trim();
		}

		String decoded = decodeText(normalizedValue);
		int separatorIndex = decoded.indexOf(':');
		if (separatorIndex < 0) {
			throw new IllegalArgumentException("Basic auth payload must contain username and password separated by ':'.");
		}

		return new BasicAuthParts(decoded.substring(0, separatorIndex), decoded.substring(separatorIndex + 1));
	}

	public DecodedFile decodeBinaryFile(String base64Value, String fileName, String contentType, String defaultContentType) {
		byte[] content = decodeBase64(base64Value);
		String resolvedFileName = (fileName == null || fileName.isBlank()) ? "decoded-file" : fileName;
		String resolvedContentType = (contentType == null || contentType.isBlank()) ? defaultContentType : contentType;
		return new DecodedFile(resolvedFileName, resolvedContentType, content);
	}

	public MediaType resolveMediaType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
		return MediaType.parseMediaType(contentType);
	}

	public String decodeUrlEncodedText(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private String encodeString(String value, java.nio.charset.Charset charset) {
		return ENCODER.encodeToString(safeValue(value).getBytes(charset));
	}

	private String safeValue(String value) {
		return value == null ? "" : value;
	}

	private byte[] decodeBase64(String base64Value) {
		try {
			return DECODER.decode(safeValue(base64Value));
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Invalid Base64 input.", ex);
		}
	}

	private byte[] hexToBytes(String hexValue) {
		String normalized = safeValue(hexValue).replaceAll("\\s+", "");
		if (normalized.length() % 2 != 0) {
			throw new IllegalArgumentException("Hex input must contain an even number of characters.");
		}

		byte[] bytes = new byte[normalized.length() / 2];
		for (int i = 0; i < normalized.length(); i += 2) {
			int high = Character.digit(normalized.charAt(i), 16);
			int low = Character.digit(normalized.charAt(i + 1), 16);
			if (high < 0 || low < 0) {
				throw new IllegalArgumentException("Hex input contains invalid characters.");
			}
			bytes[i / 2] = (byte) ((high << 4) + low);
		}
		return bytes;
	}

	private String wrapFileRead(FileSupplier supplier) {
		try {
			return supplier.read();
		} catch (Exception ex) {
			throw new IllegalArgumentException("Unable to read uploaded file.", ex);
		}
	}

	@FunctionalInterface
	private interface FileSupplier {
		String read() throws Exception;
	}

	public record BasicAuthParts(String username, String password) {
	}

	public record DecodedFile(String fileName, String contentType, byte[] content) {
	}
}
