package tslc.beihaiyun.lyra.webdav;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tslc.beihaiyun.lyra.service.JwtService;

/**
 * WebDAV 认证处理器
 * 
 * 提供WebDAV协议所需的认证支持，包括：
 * 1. HTTP Basic认证（WebDAV客户端常用）
 * 2. Bearer Token认证（JWT）
 * 3. 认证质询响应处理
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Component
public class WebDavAuthenticationHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebDavAuthenticationHandler.class);
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;

    public WebDavAuthenticationHandler(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            AuthenticationManager authenticationManager) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * 处理WebDAV认证
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @return 认证是否成功
     * @throws IOException IO异常
     */
    public boolean handleAuthentication(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (!StringUtils.hasText(authorizationHeader)) {
            logger.debug("WebDAV请求缺少Authorization头: {}", request.getRequestURI());
            sendAuthenticationChallenge(response);
            return false;
        }

        try {
            Authentication authentication = null;

            if (authorizationHeader.startsWith(BASIC_PREFIX)) {
                // 处理HTTP Basic认证
                authentication = handleBasicAuthentication(authorizationHeader);
            } else if (authorizationHeader.startsWith(BEARER_PREFIX)) {
                // 处理JWT Bearer Token认证
                authentication = handleBearerAuthentication(authorizationHeader);
            } else {
                logger.warn("不支持的认证类型: {}", authorizationHeader.substring(0, Math.min(20, authorizationHeader.length())));
                sendAuthenticationChallenge(response);
                return false;
            }

            if (authentication != null && authentication.isAuthenticated()) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("WebDAV认证成功: {}", authentication.getName());
                return true;
            } else {
                logger.debug("WebDAV认证失败");
                sendAuthenticationChallenge(response);
                return false;
            }

        } catch (AuthenticationException e) {
            logger.warn("WebDAV认证异常: {}", e.getMessage());
            sendAuthenticationChallenge(response);
            return false;
        } catch (Exception e) {
            logger.error("WebDAV认证处理错误: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "认证处理错误");
            return false;
        }
    }

    /**
     * 处理HTTP Basic认证
     * 
     * @param authorizationHeader Authorization头内容
     * @return 认证结果
     * @throws AuthenticationException 认证异常
     */
    private Authentication handleBasicAuthentication(String authorizationHeader) 
            throws AuthenticationException {
        
        String base64Credentials = authorizationHeader.substring(BASIC_PREFIX.length()).trim();
        
        try {
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            
            final String[] values = credentials.split(":", 2);
            if (values.length != 2) {
                logger.warn("Basic认证凭据格式错误");
                return null;
            }
            
            String username = values[0];
            String password = values[1];
            
            logger.debug("尝试Basic认证: {}", username);
            
            // 使用Spring Security的认证管理器进行认证
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(username, password);
            
            return authenticationManager.authenticate(authToken);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Basic认证Base64解码失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 处理Bearer Token认证
     * 
     * @param authorizationHeader Authorization头内容
     * @return 认证结果
     */
    private Authentication handleBearerAuthentication(String authorizationHeader) {
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        
        try {
            if (jwtService.isTokenValid(token)) {
                String username = jwtService.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                if (jwtService.isTokenValid(token, userDetails)) {
                    logger.debug("JWT认证成功: {}", username);
                    
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    
                    return authToken;
                }
            }
            
            logger.debug("JWT认证失败");
            return null;
            
        } catch (Exception e) {
            logger.warn("JWT认证处理错误: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 发送认证质询响应
     * 
     * @param response HTTP响应
     * @throws IOException IO异常
     */
    private void sendAuthenticationChallenge(HttpServletResponse response) throws IOException {
        // 支持多种认证方式的质询
        response.setHeader(WWW_AUTHENTICATE_HEADER, 
            "Basic realm=\"Lyra WebDAV\", Bearer realm=\"Lyra WebDAV\"");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // 设置响应体为JSON格式（可选）
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("{\"error\":\"认证失败\",\"message\":\"请提供有效的认证凭据\"}");
    }

    /**
     * 检查请求是否已认证
     * 
     * @return 是否已认证
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && 
               !authentication.getName().equals("anonymousUser");
    }

    /**
     * 获取当前认证用户名
     * 
     * @return 用户名，如果未认证则返回null
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * 清除当前认证上下文
     */
    public void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
} 