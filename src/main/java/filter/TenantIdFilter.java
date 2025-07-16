package filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import tenants.context.TenantContext;

import java.io.IOException;

@WebFilter(urlPatterns = "/*")
public class TenantIdFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization code if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false); // Don't create new session

        String tenantId = null;
        if (session != null) {
            // Retrieve tenantId from session once user is logged in
            tenantId = (String) session.getAttribute("currentTenantId");
        }

        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
            System.out.println("TenantIdFilter: Set tenantId from session: " + tenantId);
        } else {
            // This might happen for requests to the login page itself,
            // or if the session expired. For these cases, DEFAULT_TENANT_ID will be used
            // by the resolver, pointing to the central DB.
            System.out.println("TenantIdFilter: No tenantId in session. Resolver will use default.");
        }

        try {
            chain.doFilter(request, response); // Continue with the request
        } finally {
            // Ensure the tenant ID is cleared from the ThreadLocal after the request
            TenantContext.clear();
            System.out.println("TenantIdFilter: Cleared TenantContext.");
        }
    }

    @Override
    public void destroy() {
        // Cleanup code if needed
    }
}