package com.devtool.service;

import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.devtool.dto.FileMetadataResponse;

@Service
public class FileToolService {

	public Map<String, List<String>> supportedTools() {
		Map<String, List<String>> tools = new LinkedHashMap<>();
		tools.put("fileTools", List.of("MIME Type Detector", "Checksum Generator", "File Metadata Viewer"));
		return tools;
	}

	public FileMetadataResponse inspect(MultipartFile file) {
		try {
			byte[] bytes = file.getBytes();
			String fileName = file.getOriginalFilename();
			String contentType = resolveContentType(file);
			return new FileMetadataResponse("File Metadata Viewer", fileName, contentType, extension(fileName), file.getSize(),
					hash(bytes, "MD5"), hash(bytes, "SHA-1"), hash(bytes, "SHA-256"));
		} catch (Exception ex) {
			throw new IllegalArgumentException("Unable to inspect uploaded file.", ex);
		}
	}

	private String resolveContentType(MultipartFile file) {
		if (file.getContentType() != null && !file.getContentType().isBlank()) {
			return file.getContentType();
		}
		String guessed = URLConnection.guessContentTypeFromName(file.getOriginalFilename());
		return guessed == null ? "application/octet-stream" : guessed;
	}

	private String extension(String fileName) {
		if (fileName == null || !fileName.contains(".")) {
			return "";
		}
		return fileName.substring(fileName.lastIndexOf('.') + 1);
	}

	private String hash(byte[] bytes, String algorithm) throws Exception {
		MessageDigest digest = MessageDigest.getInstance(algorithm);
		return HexFormat.of().formatHex(digest.digest(bytes));
	}
}
