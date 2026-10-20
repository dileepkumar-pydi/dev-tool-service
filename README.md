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

## Render deployment for Office/PDF conversions

The PDF conversion endpoints for `Word/Excel/PowerPoint <-> PDF` depend on LibreOffice (`soffice`).

If you deploy this service on Render:

- deploy it as a `Docker` web service, not a plain native Java service
- use the included [Dockerfile](/Users/mac/Documents/code/devTools/dev-tool-service/Dockerfile), which installs LibreOffice
- keep `SOFFICE_PATH=/usr/bin/soffice` in the runtime environment

Without LibreOffice in the runtime image, these endpoints will return:

- `Office conversion is unavailable in this environment.`


## Image tools

The Image Tools workspace uses the backend, independently of iLoveIMG. `GET /api/image/tools` lists the operation IDs; `POST /api/image/{operation}` accepts multipart `files` (one or more) and string parameters. Watermarking optionally accepts a separate `logo` file. Responses follow the existing API envelope and include `data.files` with file name, MIME type, original/output byte counts, dimensions, and base64 file content, plus `data.warnings`.

| Operation | Parameters / behavior |
| --- | --- |
| `compress` | `quality` 1–100 for JPEG; other inputs are re-encoded as PNG; retains original bytes when output is no smaller |
| `resize` | `mode=pixels`, `width`, `height`, `keepAspect=true/false`; or `mode=percent`, `percent` 1–400 |
| `crop` | `x`, `y`, `width`, `height` in source pixels; same rectangle applied to every file |
| `convert-to-jpg` | `quality`, `background` six-digit HEX color for alpha flattening |
| `convert-from-jpg` | JPEG input only; `format=png/gif`; multiple equal-sized JPEGs produce GIF frames in upload order; `delay` 20–10000 ms, `loop=true/false` |
| `photo-editor` | `brightness` −100–100, `contrast` 0–200, `filter=none/grayscale/sepia/invert`, `border` 0–100, optional `text`, `fontSize`, `color`, `position`, `opacity` |
| `upscale` | `scale=2/4`; bicubic interpolation, not AI restoration |
| `remove-background` | `background` HEX and `tolerance` 0–255; edge-connected color removal, not AI segmentation; transparent PNG |
| `watermark` | `text` or multipart `logo`; `fontSize`, `logoScale` %, `color`, `opacity` %, `position=top-left/top-right/center/bottom-left/bottom-right/tile` |
| `meme` | `topText`, `bottomText`, `fontSize`; single-line captions fitted within image width |
| `rotate` | `angle=0/90/180/270` clockwise, `flip=none/horizontal/vertical` before rotation |
| `html-to-image` | One uploaded static HTML file ≤500 KB; `width`/`height` 64–3000 px and `format=png/jpg`; paginated HTML/CSS rendering, no JavaScript or external resources; no URL capture |
| `blur` | `x`, `y`, `width`, `height`, `strength` 4–100 px; manual region pixelation, no automatic face detection |

Supported image inputs are JPG, PNG, static GIF, BMP, and single-page TIFF. Animated and multipage inputs are rejected explicitly. SVG, WebP, HEIC, PSD, and RAW decoding are not included. Image edits export PNG unless the operation specifies otherwise. Compression may change format; outputs use matching file extensions. Metadata is removed when an image is re-encoded; compression that retains original bytes also retains original metadata.

Limits: 1–20 images, 20 MB/file, 80 MB total input, 12 million pixels and 12000 px/side per image, 40 million decoded input/output pixels per batch, and 60 MB encoded batch output. HTML output is limited to 10 pages and 40 million pixels. The servlet upload envelope is 20 MB/file and 85 MB/request. Processing is synchronous and files are not deliberately persisted by the image service; the servlet container may spool uploads temporarily. Browser cancellation stops waiting for the response and does not interrupt work already running on the server.

Run tests with `mvn test`; image and PDF tests run in headless mode. Deploy the updated backend together with the frontend for `/api/image/*` to be available on a hosted installation. No new runtime dependencies, credentials, or AI models are required.
