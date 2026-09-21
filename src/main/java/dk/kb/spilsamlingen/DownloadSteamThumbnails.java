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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads the Steam store header image for every game in the Danish game registry CSV
 * whose "Spilllets url" (spil_url) column links to the Steam store.
 *
 * App id:     taken from the link, e.g. https://store.steampowered.com/app/292030/The_Witcher_3/  ->  292030
 *
 * Image URL, tried in this order until one returns a real image:
 *   1. https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/{appid}/header.jpg
 *   2. https://cdn.cloudflare.steamstatic.com/steam/apps/{appid}/header.jpg
 *   3. the "header_image" address from Steam's appdetails service
 *      https://store.steampowered.com/api/appdetails?appids={appid}&filters=basic
 *      (only used when 1 and 2 fail; Steam limits this service to roughly 200 requests / 5 minutes)
 *
 * File name:  {spreadsheet ID}_1.{jpg|png|gif}      e.g. 5526_1.jpg
 *             - if the spreadsheet ID is blank:      steam{appid}_1.jpg
 *             - if the spreadsheet ID is used twice: {ID}-steam{appid}_1.jpg for the later record
 *             Every distinct app id is downloaded once and copied for each record that uses it.
 *
 * Run (JDK 11 or newer, no build step needed):
 *     java DownloadSteamThumbnails.java SpilregistrantV_03.csv
 *     java DownloadSteamThumbnails.java SpilregistrantV_03.csv my_output_folder
 *
 * Options:
 *     --dry-run               list the matching records and app ids, download nothing
 *     --delay=500             milliseconds between requests (default 500)
 *     --include-appid-column  also handle records that have no Steam link but a numeric "Steam APPID" value
 *     --base=URL              use one host for all three Steam addresses (for testing)
 */
public class DownloadSteamThumbnails {

    static final Pattern STEAM_APP = Pattern.compile("steampowered\\.com/app/(\\d+)", Pattern.CASE_INSENSITIVE);
    static final Pattern HEADER_IMAGE = Pattern.compile("\"header_image\"\\s*:\\s*\"([^\"]+)\"");
    static final String[] EXTENSIONS = {"jpg", "png", "gif"};

    static class Game {
        String spreadsheetId;
        String title;
        String platform;
        long appId;
        String source; // "url" or "Steam APPID column"
    }

    static class Fetched {
        int status;
        byte[] body;
    }

    /** Outcome of downloading one app id (shared by all records using that app id). */
    static class Image {
        byte[] body;
        String type;
        String url;
        String via;
        String error;
    }

    public static void main(String[] args) throws Exception {
        String csvPath = "/home/teg/workspace/kb-spilsamlingen/doc/SpilregistrantV.03.csv";
        String outPath = "thumbnails";
        String base = null;
        long delayMs = 500;
        boolean dryRun = false;
        boolean includeAppIdColumn = false;

        final String sharedHost = base != null ? base : "https://shared.akamai.steamstatic.com";
        final String cloudflareHost = base != null ? base : "https://cdn.cloudflare.steamstatic.com";
        final String apiHost = base != null ? base : "https://store.steampowered.com";

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
        int idCol = requireColumn(col, "ID");
        int titleCol = requireColumn(col, "titel");
        int platformCol = requireColumn(col, "platform");
        int urlCol = findUrlColumn(col);
        Integer appIdCol = col.get("Steam APPID");
        System.out.println((rows.size() - 1) + " records read.");

        // ---- select the games ------------------------------------------------------------------
        List<Game> games = new ArrayList<>();
        int noAppId = 0;
        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            String url = cell(row, urlCol).trim();
            Game g = new Game();
            g.spreadsheetId = cell(row, idCol).trim();
            g.title = cell(row, titleCol).trim();
            g.platform = cell(row, platformCol).trim();

            if (url.toLowerCase().contains("steampowered.com")) {
                Matcher m = STEAM_APP.matcher(url);
                if (m.find()) {
                    g.appId = Long.parseLong(m.group(1));
                    g.source = "url";
                    games.add(g);
                } else {
                    noAppId++;
                    System.err.println("No /app/<id> in Steam link (bundle/package?), skipped: "
                            + g.title + "  " + url);
                }
            } else if (includeAppIdColumn && appIdCol != null) {
                String value = cell(row, appIdCol).trim();
                if (value.matches("\\d+")) {
                    g.appId = Long.parseLong(value);
                    g.source = "Steam APPID column";
                    games.add(g);
                }
            }
        }
        Set<Long> distinct = new HashSet<>();
        for (Game g : games) {
            distinct.add(g.appId);
        }
        System.out.println(games.size() + " records with a Steam app id (" + distinct.size() + " distinct app ids)."
                + (noAppId > 0 ? " " + noAppId + " Steam link(s) without app id skipped." : ""));

        // ---- download -------------------------------------------------------------------------
        Path outDir = Paths.get(outPath);
        if (!dryRun) {
            Files.createDirectories(outDir);
        }
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        Map<Long, Image> cache = new HashMap<>();
        Set<String> usedNames = new HashSet<>();
        StringBuilder report = new StringBuilder("ID;titel;platform;steam_appid;source;image_url;file;status\n");
        int ok = 0, existing = 0, missing = 0, noId = 0, dupId = 0;

        for (int i = 0; i < games.size(); i++) {
            Game g = games.get(i);
            String label = "[" + (i + 1) + "/" + games.size() + "] " + g.title + " (app " + g.appId + ")";

            // decide the file name prefix
            String prefix;
            String note = "";
            if (g.spreadsheetId.isEmpty()) {
                prefix = "" + g.appId;
                note = "no spreadsheet ID";
            } else {
                prefix = g.spreadsheetId.replaceAll("[^A-Za-z0-9._-]", "_");
                if (usedNames.contains(prefix)) {
                    prefix = prefix + "-steam" + g.appId;
                    note = "spreadsheet ID used twice";
                }
            }
            usedNames.add(prefix);

            if (dryRun) {
                System.out.println(label + " -> " + prefix + "_1.<ext>" + (note.isEmpty() ? "" : "   (" + note + ")"));
                continue;
            }

            String status;
            String file = "";
            String imageUrl = "";

            String already = findExisting(outDir, prefix);
            if (already != null) {
                file = already;
                status = "already downloaded" + (note.isEmpty() ? "" : "; " + note);
                existing++;
                System.out.println(label + " - already downloaded (" + already + ")");
            } else {
                Image img = cache.get(g.appId);
                if (img == null) {
                    img = fetchImage(client, g.appId, sharedHost, cloudflareHost, apiHost, delayMs);
                    cache.put(g.appId, img);
                }
                if (img.body != null) {
                    file = prefix + "_1." + img.type;
                    Files.write(outDir.resolve(file), img.body);
                    imageUrl = img.url;
                    ok++;
                    status = "ok (" + img.via + ")" + (note.isEmpty() ? "" : "; " + note);
                    if (g.spreadsheetId.isEmpty()) {
                        noId++;
                    } else if (!note.isEmpty()) {
                        dupId++;
                    }
                    System.out.println(label + " - saved " + file + " via " + img.via
                            + (note.isEmpty() ? "" : "   (" + note + "!)"));
                } else {
                    missing++;
                    status = "no image: " + img.error;
                    System.out.println(label + " - " + status);
                }
            }
            report.append(csvField(g.spreadsheetId)).append(';')
                  .append(csvField(g.title)).append(';')
                  .append(csvField(g.platform)).append(';')
                  .append(g.appId).append(';')
                  .append(csvField(g.source)).append(';')
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
            Files.write(outDir.resolve("steam_report.csv"), all);

            System.out.println();
            System.out.println("Done. saved: " + ok + ", already present: " + existing + ", no image found: " + missing);
            if (noId > 0) {
                System.out.println(noId + " record(s) have a blank ID in the spreadsheet and were saved as steam<appid>_1.<ext>.");
            }
            if (dupId > 0) {
                System.out.println(dupId + " record(s) share a spreadsheet ID with an earlier record and were saved as <ID>-steam<appid>_1.<ext>.");
            }
            System.out.println("Report: " + outDir.resolve("steam_report.csv").toAbsolutePath());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Steam image lookup
    // ---------------------------------------------------------------------------------------------
    static Image fetchImage(HttpClient client, long appId, String sharedHost, String cloudflareHost,
                            String apiHost, long delayMs) throws InterruptedException {
        Image result = new Image();
        String lastProblem = null;

        String[][] candidates = {
            {sharedHost + "/store_item_assets/steam/apps/" + appId + "/header.jpg", "shared CDN"},
            {cloudflareHost + "/steam/apps/" + appId + "/header.jpg", "cloudflare CDN"},
        };
        for (String[] c : candidates) {
            Fetched f = get(client, c[0]);
            Thread.sleep(delayMs);
            String type = f.status == 200 ? detectImageType(f.body) : null;
            if (type != null) {
                result.body = f.body;
                result.type = type;
                result.url = c[0];
                result.via = c[1];
                return result;
            }
            if (f.status != 404 && f.status != 200) {
                lastProblem = (f.status == -1 ? "network error" : "HTTP " + f.status) + " for " + c[0];
            }
        }

        // fallback: ask Steam's appdetails service for the current header image address
        long apiDelay = Math.max(delayMs, 1500); // service is rate limited
        Fetched api = get(client, apiHost + "/api/appdetails?appids=" + appId + "&filters=basic");
        Thread.sleep(apiDelay);
        if (api.status == 200) {
            String json = new String(api.body, StandardCharsets.UTF_8);
            Matcher m = HEADER_IMAGE.matcher(json);
            if (m.find()) {
                String url = m.group(1).replace("\\/", "/").replace("\\u0026", "&");
                Fetched f = get(client, url);
                Thread.sleep(delayMs);
                String type = f.status == 200 ? detectImageType(f.body) : null;
                if (type != null) {
                    result.body = f.body;
                    result.type = type;
                    result.url = url;
                    result.via = "appdetails API";
                    return result;
                }
                lastProblem = "appdetails gave " + url + " but it could not be downloaded";
            } else {
                lastProblem = "not on CDN and appdetails has no header image (delisted or age-restricted?)";
            }
        } else if (api.status == 429) {
            lastProblem = "Steam rate limit (HTTP 429) - run again later, finished files are skipped";
        } else if (lastProblem == null) {
            lastProblem = "not on CDN; appdetails returned " + (api.status == -1 ? "network error" : "HTTP " + api.status);
        }
        result.error = lastProblem;
        return result;
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
            } catch (IOException | IllegalArgumentException e) {
                result.status = -1; // network problem or bad URL
                if (e instanceof IllegalArgumentException) {
                    return result;
                }
            }
            Thread.sleep(2000L * attempt);
        }
        return result;
    }

    /** Returns "jpg", "png" or "gif" based on the file's first bytes, or null if it is not one of those. */
    static String detectImageType(byte[] b) {
        if (b == null || b.length < 8) {
            return null;
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "png";
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