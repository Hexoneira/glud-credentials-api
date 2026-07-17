package org.glud.credentials.security.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.glud.credentials.security.components.JwtUtils;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.components.UserDetailsServiceImpl;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RequiredAuth extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequiredAuth.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsServiceImpl;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = jwtUtils.getJwtFromHeader(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                String extractedUserId = jwtUtils.getUserIdFromJwtToken(jwt);
                UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsServiceImpl.loadUserByUsername(extractedUserId);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        ); //revisar eso de credentials

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                request.setAttribute("user", userDetails); // Inyectar req.user
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Error en la autorización: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}

