package com.devtool.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class HashToolService {

	public Map<String, List<String>> supportedTools() {
		Map<String, List<String>> tools = new LinkedHashMap<>();
		tools.put("hashTools", List.of("MD5", "SHA-1", "SHA-256", "SHA-512", "HMAC Generator", "HMAC Validator"));
		return tools;
	}

	public String hash(String value, String algorithm) {
		try {
			MessageDigest digest = MessageDigest.getInstance(normalizeDigestAlgorithm(algorithm));
			return HexFormat.of().formatHex(digest.digest(safeValue(value).getBytes(StandardCharsets.UTF_8)));
		} catch (Exception ex) {
			throw new IllegalArgumentException("Unsupported hash algorithm: " + algorithm, ex);
		}
	}

	public String hmac(String value, String secret, String algorithm) {
		try {
			String normalized = normalizeHmacAlgorithm(algorithm);
			Mac mac = Mac.getInstance(normalized);
			mac.init(new SecretKeySpec(safeValue(secret).getBytes(StandardCharsets.UTF_8), normalized));
			return HexFormat.of().formatHex(mac.doFinal(safeValue(value).getBytes(StandardCharsets.UTF_8)));
		} catch (Exception ex) {
			throw new IllegalArgumentException("Unsupported HMAC algorithm: " + algorithm, ex);
		}
	}

	public boolean verify(String value, String expectedHash, String algorithm) {
		return hash(value, algorithm).equalsIgnoreCase(safeValue(expectedHash).trim());
	}

	public boolean verifyHmac(String value, String expectedHash, String secret, String algorithm) {
		return hmac(value, secret, algorithm).equalsIgnoreCase(safeValue(expectedHash).trim());
	}

	private String normalizeDigestAlgorithm(String algorithm) {
		return switch (safeValue(algorithm).trim().toUpperCase()) {
		case "MD5" -> "MD5";
		case "SHA1", "SHA-1" -> "SHA-1";
		case "SHA256", "SHA-256" -> "SHA-256";
		case "SHA512", "SHA-512" -> "SHA-512";
		default -> throw new IllegalArgumentException("Unsupported hash algorithm: " + algorithm);
		};
	}

	private String normalizeHmacAlgorithm(String algorithm) {
		return switch (safeValue(algorithm).trim().toUpperCase()) {
		case "MD5", "HMACMD5" -> "HmacMD5";
		case "SHA1", "SHA-1", "HMACSHA1" -> "HmacSHA1";
		case "SHA256", "SHA-256", "HMACSHA256" -> "HmacSHA256";
		case "SHA512", "SHA-512", "HMACSHA512" -> "HmacSHA512";
		default -> throw new IllegalArgumentException("Unsupported HMAC algorithm: " + algorithm);
		};
	}

	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
