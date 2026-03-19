package app.filter;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import app.dao.TenantConfigDAO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import app.tenants.context.TenantContext;

import java.io.IOException;

@Slf4j
@WebFilter(urlPatterns = "/page/*")
public class TenantIdFilter implements Filter {

    @Inject
    TenantConfigDAO dao;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization code if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false); // Don't create new session

        printHeaders(request);

        String paginaAtual = httpRequest.getRequestURL().toString();

        if (!isResource(paginaAtual)) {

            String tenantId = session == null ? null : (String) session.getAttribute("currentTenantId");
            log.debug("  trying to retrieve tenantId from session: {}", tenantId);

            if (StringUtils.isBlank(tenantId)) {
                //Try Get TenantId from header
                tenantId = getTenantIdFromHeader(httpRequest);
                log.debug("  trying to retrieve tenantId from header: {}", tenantId);

                //Try Get TenantId from server name
                if (StringUtils.isBlank(tenantId)) {
                    String host = httpRequest.getServerName();
                    host = StringUtils.startsWith(host, "www.") ? StringUtils.replace(host, "www.", "") : host;
                    log.debug("   getting host name: '{}'", host);

                    tenantId = dao.findTenantByHost(host);
                    log.debug("  trying to retrieve tenantId from host: {}", tenantId);
                }

                if (StringUtils.isNotBlank(tenantId)) {
                    if (session == null) {
                        session = httpRequest.getSession(true);
                    }
                    session.setAttribute("currentTenantId", tenantId);

                    log.debug("TenantIdFilter: Set tenantId from header: {}", tenantId);
                }
            }

            if (StringUtils.isNotBlank(tenantId)) {
                TenantContext.setTenantId(tenantId);
                log.info("TenantIdFilter: Set tenantId from session: {}", tenantId);
            } else {
                // This might happen for requests to the login page itself,
                // or if the session expired. For these cases, DEFAULT_TENANT_ID will be used
                // by the resolver, pointing to the central DB.
                log.debug("TenantIdFilter: No tenantId in session. Resolver will use default.");
            }

        } else {
            log.debug("  ignoring resource request: '{}' ...", paginaAtual);
        }

        try {
            chain.doFilter(request, response); // Continue with the request
        } finally {
            // Ensure the tenant ID is cleared from the ThreadLocal after the request
            TenantContext.clear();
            log.debug("TenantIdFilter: Cleared TenantContext.");
        }
    }

    private String getTenantIdFromHeader(HttpServletRequest httpRequest) {
        return StringUtils.defaultIfBlank(httpRequest.getHeader("tid"), null);
    }


    private boolean isResource(String paginaAtual) {
        if (paginaAtual.lastIndexOf("/css/") > -1) return true;
        if (paginaAtual.lastIndexOf("/javax.faces.resource/") > -1) return true;
        return StringUtils.endsWithAny(paginaAtual,
                ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".tiff", ".svg", ".webp",
                ".woff", ".woff2", ".ttf", ".eot", ".otf", ".ico", ".css", ".js",
                ".html", ".json", ".xml", ".svgz", ".mp4", ".mp3", ".wav", ".ogg",
                ".pdf", ".txt", ".csv");
    }

    private void printHeaders(ServletRequest request) {
        log.debug("  =====  TenantIdFilter: Headers  =====  ");
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            java.util.Set<String> commonHeaders = java.util.Set.of(
                    "user-agent", "accept", "accept-encoding", "accept-language", "connection", "content-length", "Priority=u", "Pragma", "content-type", "cookie", "cache-control", "upgrade-insecure-requests", "referer", "sec-fetch-mode", "sec-fetch-site", "sec-fetch-user", "sec-fetch-dest"
            );
            java.util.Enumeration<String> headerNames = httpRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String header = headerNames.nextElement();
                if (!commonHeaders.contains(header.toLowerCase())) {
                    log.debug("TenantIdFilter: Header '{}'='{}'", header, httpRequest.getHeader(header));
                }
            }
        }
        log.debug("  ========================================  ");
    }

    @Override
    public void destroy() {
        // Cleanup code if needed
    }
}