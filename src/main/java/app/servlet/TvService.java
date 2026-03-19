package app.servlet;

import app.entity.M3UEntry;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

@WebServlet("/tv")
public class TvService extends HttpServlet {

    private static final long serialVersionUID = -4645128584699214422L;
    private static final Logger LOGGER = Logger.getLogger(TvService.class.getName());
    private static final String ACCESS_TOKEN = "GuiGabi2020";
    private static final String DEFAULT_PLAYLIST_URL = "http://brlv.net/DXQTG";

    private static final String DEFAULT_SERVER_URL = "http://17345604.97qaz.com";
    private static final String DEFAULT_SERVER_USERNAME = "F1H2HA0XX6";
    private static final String DEFAULT_SERVER_PASSWORD = "38501789";


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

        String url = getUrl(req);
        String[] includeTokens = getIncludeTokens(req);
        String[] excludeTokens = getExcludeTokens(req);

        HttpResponse<String> httpResp = Unirest.get(url)
                .header("User-Agent", "Mozilla/5.0 Firefox/26.0")
                .asString();

        if (httpResp.getStatus() < 200 || httpResp.getStatus() >= 300) {
            resp.sendError(HttpServletResponse.SC_BAD_GATEWAY,
                    "Playlist server returned status " + httpResp.getStatus());
            return;
        }

        String m3uContent = httpResp.getBody();
        String[] lines = StringUtils.splitByWholeSeparatorPreserveAllTokens(m3uContent, "\n");

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

        renderResp(list, resp);
    }

    private boolean lineMatchesFilters(String line, String[] includeTokens, String[] excludeTokens) {
        boolean includeMatch = includeTokens.length == 0
                || Arrays.stream(includeTokens).anyMatch(t -> StringUtils.containsIgnoreCase(line, t));
        if (!includeMatch) {
            return false;
        }

        return Arrays.stream(excludeTokens).noneMatch(t -> StringUtils.containsIgnoreCase(line, t));
    }

    private void renderResp(List<M3UEntry> list, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setHeader("Content-Disposition", "attachment; filename=\"channels.m3u\"");
        try (OutputStream outputStream = resp.getOutputStream()) {
            String outputResult = "#EXTM3U" + System.lineSeparator()
                    + list.stream().map(M3UEntry::getChannel).collect(Collectors.joining(System.lineSeparator()));
            outputStream.write(outputResult.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    private String getUrl(HttpServletRequest req) {
        String directUrl = StringUtils.trimToEmpty(req.getParameter("url"));
        if (StringUtils.isNotBlank(directUrl)) {
            return directUrl;
        }

        String server = StringUtils.defaultIfBlank(StringUtils.trimToEmpty(req.getParameter("server")),
                DEFAULT_SERVER_URL);
        String user = StringUtils.defaultIfBlank(StringUtils.trimToEmpty(req.getParameter("user")),
                DEFAULT_SERVER_USERNAME);
        String password = StringUtils.defaultIfBlank(StringUtils.trimToEmpty(req.getParameter("pass")),
                DEFAULT_SERVER_PASSWORD);
        if (StringUtils.isNotBlank(server) && StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password)) {
            return buildXtremeM3uUrl(server, user, password, req.getParameter("type"), req.getParameter("output"));
        }

        return DEFAULT_PLAYLIST_URL;
    }

    private String buildXtremeM3uUrl(String server,
                                     String user,
                                     String password,
                                     String type,
                                     String output) {
        String normalizedServer = StringUtils.removeEnd(StringUtils.trimToEmpty(server), "/");
        if (!StringUtils.startsWithIgnoreCase(normalizedServer, "http://")
                && !StringUtils.startsWithIgnoreCase(normalizedServer, "https://")) {
            normalizedServer = "http://" + normalizedServer;
        }

        String playlistType = StringUtils.defaultIfBlank(type, "m3u_plus");
        String playlistOutput = StringUtils.defaultIfBlank(output, "ts");

        return normalizedServer + "/get.php?username=" + encode(user)
                + "&password=" + encode(password)
                + "&type=" + encode(playlistType)
                + "&output=" + encode(playlistOutput);
    }

    private String[] getIncludeTokens(HttpServletRequest req) {
        String include = StringUtils.defaultIfBlank(req.getParameter("include"), req.getParameter("tokens"));
        return splitTokens(include);
    }

    private String[] getExcludeTokens(HttpServletRequest req) {
        return splitTokens(req.getParameter("exclude"));
    }

    private String[] splitTokens(String value) {
        if (StringUtils.isBlank(value)) {
            return new String[0];
        }

        return Arrays.stream(StringUtils.split(value, ";,"))
                .map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotBlank)
                .toArray(String[]::new);
    }

    private String encode(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }

}
