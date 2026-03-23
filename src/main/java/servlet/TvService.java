package servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import entity.M3UEntry;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@WebServlet(value = "/tv", loadOnStartup = 97)
public class TvService extends HttpServlet {

    private static final long serialVersionUID = -4645128584699214422L;
    private static final Logger LOGGER = Logger.getLogger(TvService.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ACCESS_TOKEN = "GuiGabi2020";
    private static final String DEFAULT_PLAYLIST_URL = "http://brlv.net/DXQTG";
    private static final String DEFAULT_SERVER_URL = "http://17345604.97qaz.com";
    private static final String DEFAULT_SERVER_USERNAME = "F1H2HA0XX6";
    private static final String DEFAULT_SERVER_PASSWORD = "38501789";
    private static final String EPG_RESOURCE = "epg-sqxwph.xml";
    private static final Pattern TVG_ID_PATTERN = Pattern.compile("tvg-id=\\\"[^\\\"]*\\\"");
    private static final Pattern TVG_NAME_PATTERN = Pattern.compile("tvg-name=\\\"([^\\\"]+)\\\"");
    private static final Pattern GROUP_TITLE_PATTERN = Pattern.compile("group-title=\\\"([^\\\"]+)\\\"");
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "br", "brazil", "tv", "channel", "hd", "fhd", "uhd", "sd", "4k", "ao", "vivo", "live"
    ));
    private static final long CACHE_TTL_MS = TimeUnit.HOURS.toMillis(1);
    private static final ConcurrentHashMap<String, CacheEntry> RESULT_CACHE = new ConcurrentHashMap<>();

    private volatile List<EpgChannel> epgChannelsCache;
    private ScheduledExecutorService scheduler;


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "tv-cache-refresher");
            t.setDaemon(true);
            return t;
        });
        // Warm up default params immediately, then refresh every hour
        scheduler.scheduleAtFixedRate(this::refreshDefaultCache, 0, 1, TimeUnit.HOURS);

        this.refreshDefaultCache();
    }

    @Override
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        super.destroy();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pass = StringUtils.defaultIfBlank(req.getParameter("p"), "");
        if (!StringUtils.equals(pass, ACCESS_TOKEN)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        boolean plainFormat = isPlainFormat(req.getParameter("format"));

        String directUrl = StringUtils.trimToEmpty(req.getParameter("url"));
        String server = normalizeServer(StringUtils.defaultIfBlank(
                StringUtils.trimToEmpty(req.getParameter("server")), DEFAULT_SERVER_URL));
        String user = StringUtils.defaultIfBlank(
                StringUtils.trimToEmpty(req.getParameter("user")), DEFAULT_SERVER_USERNAME);
        String password = StringUtils.defaultIfBlank(
                StringUtils.trimToEmpty(req.getParameter("pass")), DEFAULT_SERVER_PASSWORD);
        String ext = StringUtils.defaultIfBlank(req.getParameter("output"), "m3u8");
        String[] includeTokens = getIncludeTokens(req);
        String[] excludeTokens = getExcludeTokens(req);
        String[] categoryTokens = splitTokens(req.getParameter("category"));

        String cacheKey = buildCacheKey(server, user, password, ext, includeTokens, excludeTokens, categoryTokens, directUrl);
        CacheEntry cached = RESULT_CACHE.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            LOGGER.info("Serving TV response from cache (key=" + cacheKey + ")");
            renderStringResponse(cached.content, resp, plainFormat);
            return;
        }

        try {
            String result;
            if (StringUtils.isNotBlank(directUrl)) {
                result = computeDirectM3u(directUrl, includeTokens, excludeTokens);
            } else {
                result = computeXtreamM3u(server, user, password, ext, includeTokens, excludeTokens, categoryTokens);
            }
            RESULT_CACHE.put(cacheKey, new CacheEntry(result));
            renderStringResponse(result, resp, plainFormat);
        } catch (IOException e) {
            LOGGER.warning("TV computation failed [" + e.getClass().getSimpleName() + "]: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Upstream service error");
        }
    }

    // -------------------------------------------------------------------------
    // Cache helpers
    // -------------------------------------------------------------------------

    private String buildCacheKey(String server, String user, String password, String ext,
                                  String[] includeTokens, String[] excludeTokens,
                                  String[] categoryTokens, String directUrl) {
        String[] sortedInclude = includeTokens.clone();
        Arrays.sort(sortedInclude);
        String[] sortedExclude = excludeTokens.clone();
        Arrays.sort(sortedExclude);
        String[] sortedCategory = categoryTokens.clone();
        Arrays.sort(sortedCategory);
        // Hash credentials to avoid storing them in plain text within cache keys
        int credHash = Objects.hash(user, password);
        return "url=" + directUrl
                + "|server=" + server
                + "|cred=" + Integer.toHexString(credHash)
                + "|ext=" + ext
                + "|include=" + String.join(",", sortedInclude)
                + "|exclude=" + String.join(",", sortedExclude)
                + "|category=" + String.join(",", sortedCategory);
    }

    private void refreshDefaultCache() {
        LOGGER.info("Refreshing default TV cache...");
        try {
            String normalizedServer = normalizeServer(DEFAULT_SERVER_URL);
            String result = computeXtreamM3u(normalizedServer, DEFAULT_SERVER_USERNAME,
                    DEFAULT_SERVER_PASSWORD, "m3u8", new String[0], new String[0], new String[0]);
            String key = buildCacheKey(normalizedServer, DEFAULT_SERVER_USERNAME, DEFAULT_SERVER_PASSWORD,
                    "m3u8", new String[0], new String[0], new String[0], "");
            RESULT_CACHE.put(key, new CacheEntry(result));
            LOGGER.info("Default TV cache refreshed successfully.");
        } catch (Exception e) {
            LOGGER.warning("Failed to refresh default TV cache [" + e.getClass().getSimpleName() + "]: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Xtream API helpers
    // -------------------------------------------------------------------------

    private String computeXtreamM3u(String server, String user, String password, String ext,
                                     String[] includeTokens, String[] excludeTokens,
                                     String[] categoryTokens) throws IOException {
        // 1) fetch category id -> name map
        Map<String, String> categoriesMap = fetchCategories(server, user, password);
        LOGGER.info("Fetched " + categoriesMap.size() + " categories");

        // 2) fetch + filter live streams
        List<JsonNode> streams = fetchAndFilterStreams(
                server, user, password, includeTokens, excludeTokens, categoryTokens, categoriesMap);

        // 3) build M3U entries
        List<M3UEntry> entries = streams.stream().map(stream -> {
            String name     = stream.path("name").asText("");
            String icon     = stream.path("stream_icon").asText("");
            String streamId = stream.path("stream_id").asText("");
            String catId    = stream.path("category_id").asText("");
            String catName  = categoriesMap.getOrDefault(catId, catId);
            String epgId    = stream.path("epg_channel_id").asText("");

            String extinf = "#EXTINF:-1 tvg-id=\"" + epgId
                    + "\" tvg-name=\"" + name
                    + "\" tvg-logo=\"" + icon
                    + "\" group-title=\"" + catName + "\"," + name;

            String streamUrl = server + "/live/" + encode(user) + "/" + encode(password) + "/" + streamId + "." + ext;

            return M3UEntry.builder().data(extinf).epgId(epgId).url(streamUrl).build();
        }).collect(Collectors.toList());

        LOGGER.info("TV channels after filters: " + entries.size());
        applyEPGIds(entries);
        return buildM3uString(entries);
    }

    private Map<String, String> fetchCategories(String server, String user, String password) {
        Map<String, String> map = new LinkedHashMap<>();
        try {
            String url = server + "/player_api.php?username=" + encode(user)
                    + "&password=" + encode(password) + "&action=get_live_categories";
            try (InputStream is = openStream(url);
                 MappingIterator<JsonNode> it = MAPPER.readerFor(JsonNode.class).readValues(is)) {
                while (it.hasNext()) {
                    JsonNode cat = it.next();
                    String catName = cat.path("category_name").asText();
                    if (StringUtils.containsAny(catName, "CA ", "US ❖ ", "BR ❖ BRAZIL")) {
                        map.put(cat.path("category_id").asText(), catName);
                    } else{
                        LOGGER.info("Skipping category '" + catName + "' (id: " + cat.path("category_id").asText() + ") because it looks like a movie category");
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to fetch categories: " + e.getMessage());
        }
        return map;
    }

    private List<JsonNode> fetchAndFilterStreams(String server, String user, String password,
                                                 String[] includeTokens, String[] excludeTokens,
                                                 String[] categoryTokens,
                                                 Map<String, String> categoriesMap) throws IOException {
        String url = server + "/player_api.php?username=" + encode(user)
                + "&password=" + encode(password) + "&action=get_live_streams";
        LOGGER.info("Fetching streams: " + url);

        List<JsonNode> result = new ArrayList<>();
        try (InputStream is = openStream(url);
             MappingIterator<JsonNode> it = MAPPER.readerFor(JsonNode.class).readValues(is)) {
            while (it.hasNext()) {
                JsonNode stream = it.next();
                String name    = stream.path("name").asText("");
                String catId   = stream.path("category_id").asText("");
                String catName = categoriesMap.getOrDefault(catId, "");

                // name filter
                if (!lineMatchesFilters(name, includeTokens, excludeTokens)) continue;

                if(!categoriesMap.containsKey(catId)) continue;

                // category filter (matches on category name or id)
                if (categoryTokens.length > 0 && Arrays.stream(categoryTokens).noneMatch(c ->
                        StringUtils.containsIgnoreCase(catName, c) ||
                        StringUtils.containsIgnoreCase(catId, c))) continue;

                result.add(stream);
            }
        }
        return result;
    }

    /** Opens a GET stream to {@code urlString}; throws IOException if the server responds with non-2xx. */
    private InputStream openStream(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 Firefox/26.0");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(60_000);
        conn.setInstanceFollowRedirects(true);
        conn.connect();
        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            conn.disconnect();
            throw new IOException("Upstream returned HTTP " + status + " for " + urlString);
        }
        return conn.getInputStream();
    }

    // -------------------------------------------------------------------------
    // Direct M3U URL fallback
    // -------------------------------------------------------------------------

    private String computeDirectM3u(String url, String[] includeTokens, String[] excludeTokens) throws IOException {
        HttpResponse<String> httpResp = Unirest.get(url)
                .header("User-Agent", "Mozilla/5.0 Firefox/26.0").asString();

        if (httpResp.getStatus() < 200 || httpResp.getStatus() >= 300) {
            throw new IOException("Playlist server returned status " + httpResp.getStatus());
        }

        String[] lines = StringUtils.splitByWholeSeparatorPreserveAllTokens(httpResp.getBody(), "\n");
        List<M3UEntry> list = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = StringUtils.removeEnd(lines[i], "\r");
            if (StringUtils.containsIgnoreCase(line, "#EXTINF")) {
                String streamUrl = i + 1 < lines.length ? StringUtils.removeEnd(lines[i + 1], "\r") : "";
                if (lineMatchesFilters(line, includeTokens, excludeTokens)) {
                    list.add(M3UEntry.builder()
                            .data(line)
                            .url(StringUtils.defaultIfBlank(streamUrl, ""))
                            .build());
                }
                i++;
            }
        }
        LOGGER.info("TV channels after filters: " + list.size());
        LOGGER.info("Apply EPG ids: " + list.size());
        applyEPGIds(list);
        return buildM3uString(list);
    }

    private void applyEPGIds(List<M3UEntry> list) {
        if (list == null || list.isEmpty()) return;

        List<EpgChannel> epgChannels = getEpgChannels();
        if (epgChannels.isEmpty()) {
            LOGGER.warning("EPG mapping skipped because XML channels are unavailable");
            return;
        }

        int replaced = 0;
        int skippedNonBr = 0;
        for (M3UEntry entry : list) {
            if (entry == null) continue;

            String currentId = extractCurrentEpgId(entry);
            if (!needsEpgInference(currentId)) continue;

            String channelName = extractChannelName(entry.getData());
            if (StringUtils.isBlank(channelName)) continue;
            if (!isBrazilChannel(entry, channelName, currentId)) {
                skippedNonBr++;
                continue;
            }

            MatchResult match = findBestEpgMatch(channelName, epgChannels);
            if (match == null || match.score < 0.58d) continue;

            entry.setEpgId(match.channel.id);
            entry.setData(upsertTvgId(entry.getData(), match.channel.id));
            replaced++;
            LOGGER.info("Inferred tvg-id '" + match.channel.id + "' for channel '" + channelName + "'"
                    + " (previous='" + currentId + "', score=" + String.format(Locale.US, "%.2f", match.score) + ")");
        }

        LOGGER.info("EPG ids inferred/replaced: " + replaced + " (skipped non-BR: " + skippedNonBr + ")");
    }

    private String extractCurrentEpgId(M3UEntry entry) {
        String value = StringUtils.trimToEmpty(entry.getEpgId());
        if (StringUtils.isNotBlank(value)) return value;

        Matcher matcher = TVG_ID_PATTERN.matcher(StringUtils.defaultString(entry.getData()));
        if (!matcher.find()) return "";

        String match = matcher.group();
        return StringUtils.substringBetween(match, "\"", "\"");
    }

    private boolean needsEpgInference(String epgId) {
        return StringUtils.isBlank(epgId) || StringUtils.containsIgnoreCase(epgId, "br#");
    }

    private boolean isBrazilChannel(M3UEntry entry, String channelName, String currentId) {
        String data = StringUtils.defaultString(entry.getData());
        String groupTitle = "";
        Matcher groupMatcher = GROUP_TITLE_PATTERN.matcher(data);
        if (groupMatcher.find()) {
            groupTitle = StringUtils.trimToEmpty(groupMatcher.group(1));
        }

        String allHints = StringUtils.joinWith(" ", groupTitle, channelName, currentId, data).toLowerCase(Locale.ROOT);

        if (StringUtils.contains(allHints, " us ") || StringUtils.contains(allHints, " u.s")
                || StringUtils.contains(allHints, " usa") || StringUtils.contains(allHints, "canada")
                || StringUtils.contains(allHints, " ca ")) {
            return false;
        }

        return StringUtils.containsIgnoreCase(groupTitle, "br")
                || StringUtils.containsIgnoreCase(groupTitle, "brazil")
                || StringUtils.containsIgnoreCase(channelName, "br")
                || StringUtils.containsIgnoreCase(channelName, "brazil")
                || StringUtils.containsIgnoreCase(currentId, ".br")
                || StringUtils.containsIgnoreCase(currentId, "br#");
    }

    private String extractChannelName(String extInfLine) {
        String line = StringUtils.defaultString(extInfLine);
        Matcher matcher = TVG_NAME_PATTERN.matcher(line);
        if (matcher.find()) {
            return StringUtils.trimToEmpty(matcher.group(1));
        }
        int commaPos = line.lastIndexOf(',');
        if (commaPos >= 0 && commaPos + 1 < line.length()) {
            return StringUtils.trimToEmpty(line.substring(commaPos + 1));
        }
        return "";
    }

    private String upsertTvgId(String extInfLine, String epgId) {
        String line = StringUtils.defaultString(extInfLine);
        String replacement = "tvg-id=\"" + epgId + "\"";

        Matcher matcher = TVG_ID_PATTERN.matcher(line);
        if (matcher.find()) {
            return matcher.replaceFirst(Matcher.quoteReplacement(replacement));
        }

        String prefix = "#EXTINF:-1";
        if (StringUtils.startsWith(line, prefix)) {
            return prefix + " " + replacement + line.substring(prefix.length());
        }
        return line;
    }

    private MatchResult findBestEpgMatch(String channelName, List<EpgChannel> epgChannels) {
        String normalizedChannel = normalize(channelName);
        if (StringUtils.isBlank(normalizedChannel)) return null;

        MatchResult best = null;
        for (EpgChannel channel : epgChannels) {
            for (String candidate : channel.searchableNames) {
                double score = similarityScore(normalizedChannel, candidate);
                if (best == null || score > best.score) {
                    best = new MatchResult(channel, score);
                }
            }
        }
        return best;
    }

    private double similarityScore(String a, String b) {
        if (StringUtils.equals(a, b)) return 1.0d;
        String compactA = StringUtils.deleteWhitespace(a);
        String compactB = StringUtils.deleteWhitespace(b);
        if (StringUtils.equals(compactA, compactB)) return 0.99d;

        double containsScore = (StringUtils.contains(compactA, compactB) || StringUtils.contains(compactB, compactA))
                ? 0.95d : 0.0d;

        Set<String> tokensA = new HashSet<>(Arrays.asList(StringUtils.split(a, ' ')));
        Set<String> tokensB = new HashSet<>(Arrays.asList(StringUtils.split(b, ' ')));
        tokensA.removeIf(StringUtils::isBlank);
        tokensB.removeIf(StringUtils::isBlank);

        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);
        int maxTokenSize = Math.max(tokensA.size(), tokensB.size());
        double tokenScore = maxTokenSize == 0 ? 0.0d : ((double) intersection.size() / (double) maxTokenSize);

        int maxLen = Math.max(compactA.length(), compactB.length());
        int distance = StringUtils.getLevenshteinDistance(compactA, compactB);
        double levenshteinScore = maxLen == 0 ? 0.0d : (1.0d - ((double) distance / (double) maxLen));

        return Math.max(containsScore, (0.35d * tokenScore) + (0.65d * levenshteinScore));
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(StringUtils.defaultString(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('&', ' ')
                .replace('+', ' ')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        if (StringUtils.isBlank(normalized)) return "";

        return Arrays.stream(StringUtils.split(normalized, ' '))
                .filter(StringUtils::isNotBlank)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.joining(" "));
    }

    private List<EpgChannel> getEpgChannels() {
        List<EpgChannel> cached = epgChannelsCache;
        if (cached != null) return cached;

        synchronized (this) {
            if (epgChannelsCache != null) return epgChannelsCache;
            epgChannelsCache = loadEpgChannels();
            return epgChannelsCache;
        }
    }

    private List<EpgChannel> loadEpgChannels() {
        InputStream stream = TvService.class.getClassLoader().getResourceAsStream(EPG_RESOURCE);
        if (stream == null) {
            LOGGER.warning("EPG resource not found: " + EPG_RESOURCE);
            return Collections.emptyList();
        }

        List<EpgChannel> channels = new ArrayList<>();
        XMLInputFactory factory = XMLInputFactory.newFactory();

        try (InputStream is = stream) {
            XMLStreamReader reader = factory.createXMLStreamReader(is, StandardCharsets.UTF_8.name());
            try {
                String currentId = null;
                List<String> displayNames = new ArrayList<>();
                StringBuilder text = new StringBuilder();
                boolean inDisplayName = false;

                while (reader.hasNext()) {
                    int eventType = reader.next();
                    if (eventType == XMLStreamConstants.START_ELEMENT) {
                        String local = reader.getLocalName();
                        if ("channel".equals(local)) {
                            currentId = StringUtils.trimToEmpty(reader.getAttributeValue(null, "id"));
                            displayNames.clear();
                        } else if ("display-name".equals(local)) {
                            inDisplayName = true;
                            text.setLength(0);
                        }
                    } else if (eventType == XMLStreamConstants.CHARACTERS || eventType == XMLStreamConstants.CDATA) {
                        if (inDisplayName) text.append(reader.getText());
                    } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                        String local = reader.getLocalName();
                        if ("display-name".equals(local)) {
                            inDisplayName = false;
                            String displayName = StringUtils.trimToEmpty(text.toString());
                            if (StringUtils.isNotBlank(displayName)) displayNames.add(displayName);
                        } else if ("channel".equals(local)) {
                            if (StringUtils.isNotBlank(currentId) && isBrazilEpgChannel(currentId, displayNames)) {
                                channels.add(new EpgChannel(currentId, displayNames));
                            }
                            currentId = null;
                            displayNames = new ArrayList<>();
                        }
                    }
                }
            } finally {
                reader.close();
            }
        } catch (IOException | XMLStreamException e) {
            LOGGER.warning("Failed to parse EPG XML: " + e.getMessage());
            return Collections.emptyList();
        }

        LOGGER.info("Loaded EPG channels: " + channels.size());
        return channels;
    }

    private boolean isBrazilEpgChannel(String id, List<String> displayNames) {
        if (StringUtils.endsWithIgnoreCase(StringUtils.trimToEmpty(id), ".br")) {
            return true;
        }
        for (String displayName : displayNames) {
            String value = StringUtils.trimToEmpty(displayName);
            if (StringUtils.startsWithIgnoreCase(value, "BR -") || StringUtils.containsIgnoreCase(value, "brazil")) {
                return true;
            }
        }
        return false;
    }

    private static class EpgChannel {
        private final String id;
        private final List<String> searchableNames;

        private EpgChannel(String id, List<String> displayNames) {
            this.id = id;
            this.searchableNames = new ArrayList<>();

            String normalizedId = normalizeStatic(id);
            if (StringUtils.isNotBlank(normalizedId)) this.searchableNames.add(normalizedId);

            for (String name : displayNames) {
                String normalizedName = normalizeStatic(name);
                if (StringUtils.isNotBlank(normalizedName)) this.searchableNames.add(normalizedName);
            }
        }

        private static String normalizeStatic(String value) {
            String normalized = Normalizer.normalize(StringUtils.defaultString(value), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "")
                    .toLowerCase(Locale.ROOT)
                    .replace('&', ' ')
                    .replace('+', ' ')
                    .replaceAll("[^a-z0-9]+", " ")
                    .trim();
            if (StringUtils.isBlank(normalized)) return "";

            return Arrays.stream(StringUtils.split(normalized, ' '))
                    .filter(StringUtils::isNotBlank)
                    .filter(token -> !STOP_WORDS.contains(token))
                    .collect(Collectors.joining(" "));
        }
    }

    private static class MatchResult {
        private final EpgChannel channel;
        private final double score;

        private MatchResult(EpgChannel channel, double score) {
            this.channel = channel;
            this.score = score;
        }
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private boolean lineMatchesFilters(String line, String[] includeTokens, String[] excludeTokens) {
        boolean includeMatch = includeTokens.length == 0
                || Arrays.stream(includeTokens).anyMatch(t -> StringUtils.containsIgnoreCase(line, t));
        if (!includeMatch) return false;
        return Arrays.stream(excludeTokens).noneMatch(t -> StringUtils.containsIgnoreCase(line, t));
    }

    private String buildM3uString(List<M3UEntry> list) {
        return "#EXTM3U" + System.lineSeparator()
                + list.stream().map(M3UEntry::getChannel)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private void renderStringResponse(String content, HttpServletResponse resp, boolean plainFormat) throws IOException {
        resp.setContentType("text/plain");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (plainFormat) {
            resp.setHeader("Content-Disposition", null);
        } else {
            resp.setHeader("Content-Disposition", "attachment; filename=\"channels.m3u\"");
        }
        try (OutputStream outputStream = resp.getOutputStream()) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    private String normalizeServer(String server) {
        String s = StringUtils.removeEnd(StringUtils.trimToEmpty(server), "/");
        if (!StringUtils.startsWithIgnoreCase(s, "http://") &&
                !StringUtils.startsWithIgnoreCase(s, "https://")) {
            s = "http://" + s;
        }
        return s;
    }

    private String[] getIncludeTokens(HttpServletRequest req) {
        String include = StringUtils.defaultIfBlank(req.getParameter("include"), req.getParameter("tokens"));
        return splitTokens(include);
    }

    private String[] getExcludeTokens(HttpServletRequest req) {
        return splitTokens(req.getParameter("exclude"));
    }

    private boolean isPlainFormat(String format) {
        return StringUtils.equalsIgnoreCase(StringUtils.trimToEmpty(format), "plain");
    }

    private String[] splitTokens(String value) {
        if (StringUtils.isBlank(value)) return new String[0];
        return Arrays.stream(StringUtils.split(value, ";,"))
                .map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotBlank)
                .toArray(String[]::new);
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class CacheEntry {
        final String content;
        final long cachedAt;

        CacheEntry(String content) {
            this.content = content;
            this.cachedAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - cachedAt > CACHE_TTL_MS;
        }
    }
}
