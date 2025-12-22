package com.op.back.common.filter;

import com.op.back.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }

        String token = authHeader.substring(7);
        try{
            if(!jwtUtil.validateToken(token)){
                throw new RuntimeException("Invalid Token");
            }

            Claims claims = jwtUtil.getClaims(token);
            String uid = claims.getSubject();
            String email = claims.get("email", String.class); // 없으면 null

            UsernamePasswordAuthenticationToken auth =
                    new  UsernamePasswordAuthenticationToken(uid,null, new ArrayList<>());

            SecurityContextHolder.getContext().setAuthentication(auth);
        }catch(Exception e){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            filterChain.doFilter(request,response);

            return;
        }

        filterChain.doFilter(request,response);
    }
}
