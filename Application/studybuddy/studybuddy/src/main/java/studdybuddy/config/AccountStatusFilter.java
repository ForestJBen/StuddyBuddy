package studdybuddy.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import studdybuddy.user.UserRepository;

/** Revalidates persisted logins so disabling an account also revokes existing sessions. */
public class AccountStatusFilter extends OncePerRequestFilter {
    private final UserRepository users;
    public AccountStatusFilter(UserRepository users) { this.users=users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var session=request.getSession(false);
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(session != null && session.getAttribute("SPRING_SECURITY_CONTEXT") != null && auth != null
                && !users.findByEmail(auth.getName()).map(u -> u.isEnabled()).orElse(false)) {
            session.invalidate(); SecurityContextHolder.clearContext();
            if(request.getRequestURI().startsWith("/api/")) {
                response.setStatus(401); response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Account is disabled or no longer exists. Please log in again.\"}");
            } else response.sendRedirect("/login");
            return;
        }
        chain.doFilter(request,response);
    }
}
