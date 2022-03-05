package servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import entity.M3UEntry;
import http.client.GFCall;
import http.client.GFHttpClient;
import http.client.GFResponse;
import http.client.enumeration.RequestMethod;

@WebServlet("/tv")
public class TvService extends HttpServlet {

    private static final long serialVersionUID = -4645128584699214422L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pass=StringUtils.defaultIfBlank(req.getParameter("p"), "");
        if(!StringUtils.equals(pass, "GuiGabi2020"))
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        
        String url = getUrl(req);
        
        String[] tokens = getTokens(req);

        GFCall call = GFCall.builder()
                .method(RequestMethod.GET)
                .headers(Collections.singletonMap("User-Agent", "Mozilla/5.0 Firefox/26.0"))
                .url(url)
                .build();
        GFResponse httpResp = GFHttpClient.call(call);
        String m3uContent = new String(httpResp.getContentByte());
        String[] lines = StringUtils.split(m3uContent, System.lineSeparator());

        List<M3UEntry> list = new ArrayList<M3UEntry>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (StringUtils.containsIgnoreCase(line, "#EXTINF")) {
                if (lineContains(line, tokens)) {
                    list.add(M3UEntry.builder()
                            .data(line)
                            .url(StringUtils.defaultIfBlank(lines[i + 1], ""))
                            .build());
                }

                i++;
            }

        }

        System.out.println(list.size());

        renderResp(list, resp);
    }

    private boolean lineContains(String line, String[] tokens) {
        Optional<String> findFirst = Arrays.stream(tokens).filter(t -> StringUtils.containsIgnoreCase(line, t)).findFirst();
        return findFirst.isPresent();
    }

    private void renderResp(List<M3UEntry> list, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        resp.setHeader("Content-Disposition", "attachment; filename=\"channels.m3u\"");
        try {
            OutputStream outputStream = resp.getOutputStream();
            String outputResult = "#EXTM3U" + System.lineSeparator()
                    + list.stream().map(M3UEntry::getChannel).collect(Collectors.joining(System.lineSeparator()));
            outputStream.write(outputResult.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getUrl(HttpServletRequest req) {
        String url = "http://brlv.net/DXQTG";
        return StringUtils.defaultIfBlank(req.getParameter("url"), url);
    }

    private String[] getTokens(HttpServletRequest req) {
        String[] tokens = { "sport" };
        String tokensStr = StringUtils.defaultIfBlank(req.getParameter("tokens"), "");
        if (StringUtils.isNotEmpty(tokensStr)) {
            tokens = StringUtils.split(tokensStr, ";");
        }

        return tokens;
    }

}
