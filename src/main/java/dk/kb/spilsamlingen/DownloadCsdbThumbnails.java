package dk.kb.spilsamlingen;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads CSDb thumbnails for the Commodore 64 games in the Danish game registry CSV.
 *
 * Selection:  platform contains "Commodore 64" or "C64"
 *             AND the "Spilllets url" (spil_url) column starts with https://csdb.dk/release/
 *
 * Image URL:  https://csdb.dk/gfx/releases/{csdbId rounded down to nearest 1000}/{csdbId}.{png|jpg|gif}
 *             e.g. release id 254025  ->  https://csdb.dk/gfx/releases/254000/254025.png
 *
 * File name:  {spreadsheet ID}_1.{png|jpg|gif}     e.g. 10287_1.png
 *             (the extension is decided by what CSDb actually serves; it is verified from the file's magic bytes)
 *
 * Run (JDK 11 or newer, no build step needed):
 *     java DownloadCsdbThumbnails.java SpilregistrantV_03.csv
 *     java DownloadCsdbThumbnails.java SpilregistrantV_03.csv my_output_folder
 *
 * Options:
 *     --dry-run        only list the matching games and the image URLs, download nothing
 *     --delay=500      milliseconds to wait between requests (default 500)
 *     --base=URL       image host, default https://csdb.dk
 */
public class DownloadCsdbThumbnails {

    static final Pattern PLATFORM = Pattern.compile("commodore\\s*64|c64", Pattern.CASE_INSENSITIVE);
    static final Pattern CSDB_RELEASE = Pattern.compile("^https?://(www\\.)?csdb\\.dk/release/", Pattern.CASE_INSENSITIVE);
    static final Pattern ID_PARAM = Pattern.compile("[?&]id=(\\d+)");
    static final String[] EXTENSIONS = {"png", "jpg", "gif"}; // tried in this order

    static class Game {
        String spreadsheetId;
        String title;
        String releaseUrl;
        long csdbId;
    }

    static class Fetched {
        int status;
        byte[] body;
    }

    public static void main(String[] args) throws Exception {
        String csvPath = "/home/teg/workspace/kb-spilsamlingen/doc/SpilregistrantV.03.csv";
        String outPath = "thumbnails";
        String base = "https://csdb.dk";
      
        long delayMs = 500;
        boolean dryRun = false;


        // ---- read + parse the CSV -------------------------------------------------------------
        List<List<String>> rows = parseCsv(decode(Files.readAllBytes(Paths.get(csvPath))), ';');
        if (rows.isEmpty()) {
            System.err.println("The CSV file is empty.");
            System.exit(1);
        }
        Map<String, Integer> col = new HashMap<>();
        List<String> header = rows.get(0);
        for (int i = 0; i < header.size(); i++) {
            col.put(header.get(i).trim(), i);
        }
        int platformCol = requireColumn(col, "platform");
        int idCol = requireColumn(col, "ID");
        int titleCol = requireColumn(col, "titel");
        int urlCol = findUrlColumn(col);
        System.out.println((rows.size() - 1) + " records read.");

        // ---- select the games ------------------------------------------------------------------
        List<Game> games = new ArrayList<>();
        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (!PLATFORM.matcher(cell(row, platformCol)).find()) {
                continue;
            }
            String url = cell(row, urlCol).trim();
            if (!CSDB_RELEASE.matcher(url).find()) {
                continue;
            }
            Matcher m = ID_PARAM.matcher(url);
            if (!m.find()) {
                System.err.println("No release id in URL, skipped: " + url);
                continue;
            }
            Game g = new Game();
            g.spreadsheetId = cell(row, idCol).trim();
            g.title = cell(row, titleCol).trim();
            g.releaseUrl = url;
            g.csdbId = Long.parseLong(m.group(1));
            games.add(g);
        }
        System.out.println(games.size() + " Commodore 64 games with a csdb.dk/release link.");

        // ---- download -------------------------------------------------------------------------
        Path outDir = Paths.get(outPath);
        if (!dryRun) {
            Files.createDirectories(outDir);
        }
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        StringBuilder report = new StringBuilder("ID;titel;csdb_id;image_url;file;status\n");
        int ok = 0, existing = 0, missing = 0, errors = 0, noId = 0;

        for (int i = 0; i < games.size(); i++) {
            Game g = games.get(i);
            String prefix = g.spreadsheetId.isEmpty()
                    ? "" + g.csdbId
                    : g.spreadsheetId.replaceAll("[^A-Za-z0-9._-]", "_");
            boolean fallbackName = g.spreadsheetId.isEmpty();
            String label = "[" + (i + 1) + "/" + games.size() + "] " + g.title + " (csdb " + g.csdbId + ")";

            long folder = (g.csdbId / 1000) * 1000; // rounded down to nearest 1000
            String urlBase = base + "/gfx/releases/" + folder + "/" + g.csdbId + ".";

            if (dryRun) {
                System.out.println(label + " -> " + urlBase + "png|jpg|gif   file: " + prefix + "_1.<ext>");
                continue;
            }

            String status;
            String file = "";
            String imageUrl = "";

            String already = findExisting(outDir, prefix);
            if (already != null) {
                file = already;
                imageUrl = urlBase + already.substring(already.lastIndexOf('.') + 1);
                status = "already downloaded";
                existing++;
                System.out.println(label + " - already downloaded (" + already + ")");
            } else {
                String error = null;
                for (String ext : EXTENSIONS) {
                    String url = urlBase + ext;
                    Fetched f = get(client, url);
                    Thread.sleep(delayMs);
                    if (f.status == 200) {
                        String type = detectImageType(f.body);
                        if (type != null) {
                            file = prefix + "_1." + type;
                            Files.write(outDir.resolve(file), f.body);
                            imageUrl = url;
                            break;
                        }
                    } else if (f.status != 404) {
                        error = (f.status == -1 ? "network error" : "HTTP " + f.status) + " for " + url;
                    }
                }
                if (!file.isEmpty()) {
                    ok++;
                    status = fallbackName ? "ok (no spreadsheet ID, saved with csdb id)" : "ok";
                    if (fallbackName) {
                        noId++;
                    }
                    System.out.println(label + " - saved " + file + (fallbackName ? "   (spreadsheet ID is blank!)" : ""));
                } else if (error != null) {
                    errors++;
                    status = "error: " + error;
                    System.err.println(label + " - " + status);
                } else {
                    missing++;
                    status = "no image found (tried png, jpg, gif)";
                    System.out.println(label + " - " + status);
                }
            }
            report.append(csvField(g.spreadsheetId)).append(';')
                  .append(csvField(g.title)).append(';')
                  .append(g.csdbId).append(';')
                  .append(csvField(imageUrl)).append(';')
                  .append(csvField(file)).append(';')
                  .append(csvField(status)).append('\n');
        }

        if (!dryRun) {
            // UTF-8 with BOM so Excel opens the semicolon-separated report correctly
            byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
            byte[] text = report.toString().getBytes(StandardCharsets.UTF_8);
            byte[] all = new byte[bom.length + text.length];
            System.arraycopy(bom, 0, all, 0, bom.length);
            System.arraycopy(text, 0, all, bom.length, text.length);
            Files.write(outDir.resolve("thumbnails_report.csv"), all);

            System.out.println();
            System.out.println("Done. downloaded: " + ok + ", already present: " + existing
                    + ", no image found: " + missing + ", errors: " + errors);
            if (noId > 0) {
                System.out.println(noId + " game(s) have a blank ID in the spreadsheet and were saved as csdb<id>_1.<ext>."
                        + " See thumbnails_report.csv.");
            }
            System.out.println("Report: " + outDir.resolve("thumbnails_report.csv").toAbsolutePath());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // HTTP
    // ---------------------------------------------------------------------------------------------
    static Fetched get(HttpClient client, String url) throws InterruptedException {
        Fetched result = new Fetched();
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .header("User-Agent", "SpilregistrantThumbnailFetcher/1.0 (personal archive project)")
                        .GET()
                        .build();
                HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                result.status = resp.statusCode();
                result.body = resp.body();
                if (result.status < 500 && result.status != 429) {
                    return result;
                }
            } catch (IOException e) {
                result.status = -1; // network problem, retry
            }
            Thread.sleep(2000L * attempt);
        }
        return result;
    }

    /** Returns "png", "jpg" or "gif" based on the file's first bytes, or null if it is not one of those. */
    static String detectImageType(byte[] b) {
        if (b == null || b.length < 8) {
            return null;
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "png";
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return "gif";
        }
        return null;
    }

    static String findExisting(Path dir, String prefix) {
        for (String ext : EXTENSIONS) {
            if (Files.exists(dir.resolve(prefix + "_1." + ext))) {
                return prefix + "_1." + ext;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------
    // CSV
    // ---------------------------------------------------------------------------------------------
    /** The registry export is Latin-1/Windows-1252; accept UTF-8 too if that is what we are given. */
    static String decode(byte[] raw) {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(raw)).toString();
        } catch (CharacterCodingException e) {
            text = new String(raw, Charset.forName("windows-1252"));
        }
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    /** Minimal RFC-4180 parser: quoted fields, doubled quotes and line breaks inside quotes. */
    static List<List<String>> parseCsv(String text, char sep) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"' && field.length() == 0) {
                inQuotes = true;
            } else if (c == sep) {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\r') {
                // ignore, the '\n' ends the record
            } else if (c == '\n') {
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }

    static String cell(List<String> row, int index) {
        return index < row.size() ? row.get(index) : "";
    }

    static int requireColumn(Map<String, Integer> col, String name) {
        Integer idx = col.get(name);
        if (idx == null) {
            System.err.println("Column '" + name + "' not found. Columns: " + col.keySet());
            System.exit(1);
        }
        return idx;
    }

    /** The header is spelled "Spilllets url" (three l's); accept any "spil... url" header. */
    static int findUrlColumn(Map<String, Integer> col) {
        Integer exact = col.get("Spilllets url");
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, Integer> e : col.entrySet()) {
            String k = e.getKey().toLowerCase();
            if (k.startsWith("spil") && k.contains("url")) {
                return e.getValue();
            }
        }
        System.err.println("Could not find the game URL column (expected 'Spilllets url').");
        System.exit(1);
        return -1;
    }

    static String csvField(String s) {
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
