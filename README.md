# dev-tool-service
Backend service for DevTool.

## Base64 helper APIs

The service now includes `/api/base64` endpoints for:

- Encoders: text, HTML, CSS, URL, hex, file, image, audio, video, PDF
- Decoders: text, ASCII, hex, Basic Auth, file, image, audio, video, PDF

## JSON helper APIs

The service now includes `/api/json` endpoints for:

- JSON format
- JSON minify
- JSON validate
- JSON to YAML
- YAML to JSON
- JSON escape and unescape
- JSON path query with JSON Pointer syntax like `/user/name`

## Additional developer helper APIs

The service now also includes:

- `/api/hash` for digest and HMAC helpers
- `/api/text` for diff, case conversion, regex, slug, and text stats
- `/api/timestamp` for Unix/date/timezone conversion
- `/api/number-base` for base conversion and ASCII/Unicode helpers
- `/api/escape` for HTML, JSON, Java, SQL, and XML escape/unescape
- `/api/uuid` for UUID generation and validation
- `/api/color` for HEX/RGB/HSL conversion and gradients
- `/api/file` for file metadata and checksums
- `/api/web` for HTML/CSS/JS beautify, minify, and HTML escaping

Browse the API docs after starting the app:

- `http://localhost:8080/swagger-ui/index.html`
