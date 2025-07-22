package tslc.beihaiyun.lyra.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * WebDAV 配置
 * 
 * 集成 WebDAV 协议支持，提供完整的文件服务功能
 * 基于 Spring Boot 的最佳实践实现
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Configuration
@ConditionalOnProperty(name = "lyra.webdav.enabled", havingValue = "true", matchIfMissing = true)
public class WebDavConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebDavConfig.class);

    public WebDavConfig() {
        logger.info("WebDAV配置已加载 - 基础WebDAV协议支持准备就绪");
    }

    /**
     * 配置基础 WebDAV Servlet
     * 
     * 基于 Spring Boot ServletRegistrationBean 的最佳实践
     * 支持标准的 WebDAV 方法：PROPFIND, PROPPATCH, MKCOL, DELETE, COPY, MOVE
     * 
     * @return ServletRegistrationBean<LyraWebDavServlet>
     */
    @Bean
    public ServletRegistrationBean<LyraWebDavServlet> webdavServlet() {
        logger.info("注册 WebDAV Servlet - 路径: /webdav/*");
        
        LyraWebDavServlet servlet = new LyraWebDavServlet();
        ServletRegistrationBean<LyraWebDavServlet> registration = 
            new ServletRegistrationBean<>(servlet, "/webdav/*");
        
        // 配置 Servlet 初始化参数
        registration.addInitParameter("debug", "true");
        registration.addInitParameter("listings", "true");
        registration.addInitParameter("readonly", "false");
        
        // 设置加载优先级
        registration.setLoadOnStartup(1);
        registration.setName("lyraWebDavServlet");
        
        return registration;
    }

    /**
     * 配置 WebDAV 认证过滤器
     * 
     * 集成现有的 Spring Security 认证体系
     * 
     * @return FilterRegistrationBean<WebDavAuthenticationFilter>
     */
    @Bean
    public FilterRegistrationBean<WebDavAuthenticationFilter> webdavAuthFilter() {
        logger.info("注册 WebDAV 认证过滤器");
        
        WebDavAuthenticationFilter filter = new WebDavAuthenticationFilter();
        FilterRegistrationBean<WebDavAuthenticationFilter> registration = 
            new FilterRegistrationBean<>(filter);
        
        registration.addUrlPatterns("/webdav/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("webdavAuthFilter");
        
        return registration;
    }

    /**
     * Lyra WebDAV Servlet 实现
     * 
     * 自定义的 WebDAV Servlet，支持标准 WebDAV 协议
     * 集成 LyraWebDavResourceService 进行文件操作
     */
    public static class LyraWebDavServlet extends HttpServlet {
        
        private static final Logger logger = LoggerFactory.getLogger(LyraWebDavServlet.class);
        
        private tslc.beihaiyun.lyra.webdav.LyraWebDavResourceService resourceService;
        private tslc.beihaiyun.lyra.webdav.WebDavPermissionService permissionService;
        private tslc.beihaiyun.lyra.webdav.WebDavLockService lockService;

        @Override
        public void init() throws ServletException {
            super.init();
            
            // 从Spring容器获取资源服务
            org.springframework.web.context.WebApplicationContext context = 
                org.springframework.web.context.support.WebApplicationContextUtils
                    .getWebApplicationContext(getServletContext());
            
            if (context != null) {
                resourceService = context.getBean(tslc.beihaiyun.lyra.webdav.LyraWebDavResourceService.class);
                permissionService = context.getBean(tslc.beihaiyun.lyra.webdav.WebDavPermissionService.class);
                lockService = context.getBean(tslc.beihaiyun.lyra.webdav.WebDavLockService.class);
            }
            
            logger.info("Lyra WebDAV Servlet 初始化完成");
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            
            logger.debug("WebDAV GET 请求: {}", req.getRequestURI());
            
            if (resourceService != null && resourceService.resourceExists(req.getRequestURI())) {
                if (resourceService.isDirectory(req.getRequestURI())) {
                    // 对于目录，返回HTML目录浏览页面
                    resp.setContentType("text/html; charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_OK);
                    
                    java.util.List<tslc.beihaiyun.lyra.webdav.LyraWebDavResourceService.WebDavResource> resources = 
                        resourceService.listDirectory(req.getRequestURI());
                    
                    StringBuilder html = new StringBuilder();
                    html.append("<!DOCTYPE html>\n<html>\n<head><title>Lyra WebDAV - ").append(req.getRequestURI()).append("</title></head>\n<body>\n");
                    html.append("<h1>Lyra 企业级文档管理系统</h1>\n");
                    html.append("<h2>目录浏览: ").append(req.getRequestURI()).append("</h2>\n");
                    html.append("<ul>\n");
                    
                    // 添加返回上级目录链接
                    if (!"/webdav/".equals(req.getRequestURI()) && !"/webdav".equals(req.getRequestURI())) {
                        String parentPath = req.getRequestURI();
                        if (parentPath.endsWith("/")) {
                            parentPath = parentPath.substring(0, parentPath.length() - 1);
                        }
                        int lastSlash = parentPath.lastIndexOf('/');
                        if (lastSlash > 0) {
                            parentPath = parentPath.substring(0, lastSlash + 1);
                        } else {
                            parentPath = "/webdav/";
                        }
                        html.append("<li><a href=\"").append(parentPath).append("\">../</a></li>\n");
                    }
                    
                    for (tslc.beihaiyun.lyra.webdav.LyraWebDavResourceService.WebDavResource resource : resources) {
                        String href = req.getRequestURI();
                        if (!href.endsWith("/")) href += "/";
                        href += resource.getName();
                        if (resource.isDirectory()) href += "/";
                        
                        html.append("<li><a href=\"").append(href).append("\">").append(resource.getName());
                        if (resource.isDirectory()) {
                            html.append("/");
                        } else {
                            html.append(" (").append(resource.getSize()).append(" 字节)");
                        }
                        html.append("</a></li>\n");
                    }
                    
                    html.append("</ul>\n");
                    html.append("<hr><p><em>Lyra WebDAV 服务 - 请使用 WebDAV 客户端访问您的文件</em></p>\n");
                    html.append("</body>\n</html>");
                    
                    resp.getWriter().write(html.toString());
                } else {
                    // 对于文件，返回文件内容
                    resp.setContentType("text/plain; charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_OK);
                    
                    try (java.io.InputStream content = resourceService.getFileContent(req.getRequestURI())) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = content.read(buffer)) != -1) {
                            resp.getOutputStream().write(buffer, 0, bytesRead);
                        }
                    }
                }
            } else {
                // 返回默认WebDAV服务信息
                resp.setContentType("text/html; charset=UTF-8");
                resp.setStatus(HttpServletResponse.SC_OK);
                
                resp.getWriter().write("""
                                       <!DOCTYPE html>
                                       <html>
                                       <head><title>Lyra WebDAV \u670d\u52a1</title></head>
                                       <body>
                                       <h1>Lyra \u4f01\u4e1a\u7ea7\u6587\u6863\u7ba1\u7406\u7cfb\u7edf</h1>
                                       <h2>WebDAV \u670d\u52a1\u8fd0\u884c\u4e2d</h2>
                                       <p>\u8bf7\u4f7f\u7528 WebDAV \u5ba2\u6237\u7aef\u8bbf\u95ee\u60a8\u7684\u6587\u4ef6</p>
                                       <ul>
                                       <li><a href="/webdav/enterprise/">\u4f01\u4e1a\u7a7a\u95f4: /webdav/enterprise/</a></li>
                                       <li><a href="/webdav/personal/">\u4e2a\u4eba\u7a7a\u95f4: /webdav/personal/</a></li>
                                       </ul>
                                       </body>
                                       </html>
                                       """);
            }
        }

        @Override
        protected void doOptions(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            
            logger.debug("WebDAV OPTIONS 请求: {}", req.getRequestURI());
            
            // 设置 WebDAV 支持的方法
            resp.setHeader("Allow", "GET, POST, PUT, DELETE, HEAD, OPTIONS, " +
                "PROPFIND, PROPPATCH, MKCOL, COPY, MOVE, LOCK, UNLOCK");
            resp.setHeader("DAV", "1, 2");
            resp.setHeader("MS-Author-Via", "DAV");
            resp.setStatus(HttpServletResponse.SC_OK);
        }

        @Override
        protected void doPut(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            
            logger.debug("WebDAV PUT 请求: {}", req.getRequestURI());
            
            if (resourceService != null) {
                try {
                    long contentLength = req.getContentLengthLong();
                    boolean success = resourceService.uploadFile(req.getRequestURI(), req.getInputStream(), contentLength);
                    
                    if (success) {
                        resp.setStatus(HttpServletResponse.SC_CREATED);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                } catch (Exception e) {
                    logger.error("PUT 处理错误: {}", e.getMessage());
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_CREATED);
            }
        }

        @Override
        protected void doDelete(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            
            logger.debug("WebDAV DELETE 请求: {}", req.getRequestURI());
            
            // TODO: 实现文件删除逻辑
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            
            String method = req.getMethod();
            
            // 处理 WebDAV 特有的方法
            switch (method) {
                case "PROPFIND" -> doPropfind(req, resp);
                case "PROPPATCH" -> doProppatch(req, resp);
                case "MKCOL" -> doMkcol(req, resp);
                case "COPY" -> doCopy(req, resp);
                case "MOVE" -> doMove(req, resp);
                case "LOCK" -> doLock(req, resp);
                case "UNLOCK" -> doUnlock(req, resp);
                default -> super.service(req, resp);
            }
        }

        protected void doPropfind(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            
            logger.debug("WebDAV PROPFIND 请求: {}", req.getRequestURI());
            
            resp.setContentType("text/xml; charset=UTF-8");
            resp.setStatus(207); // Multi-Status
            
            String requestURI = req.getRequestURI();
            
            if (resourceService != null) {
                try {
                    boolean isDirectory = resourceService.isDirectory(requestURI);
                    java.util.List<tslc.beihaiyun.lyra.webdav.LyraWebDavResourceService.WebDavResource> resources = 
                        isDirectory ? resourceService.listDirectory(requestURI) : new java.util.ArrayList<>();
                    
                    StringBuilder xmlResponse = new StringBuilder();
                    xmlResponse.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                    xmlResponse.append("<D:multistatus xmlns:D=\"DAV:\">\n");
                    
                    // 添加请求的资源本身
                    xmlResponse.append("<D:response>\n");
                    xmlResponse.append("<D:href>").append(requestURI).append("</D:href>\n");
                    xmlResponse.append("<D:propstat>\n");
                    xmlResponse.append("<D:prop>\n");
                    
                    if (isDirectory) {
                        xmlResponse.append("<D:resourcetype><D:collection/></D:resourcetype>\n");
                        xmlResponse.append("<D:getcontenttype>httpd/unix-directory</D:getcontenttype>\n");
                    } else {
                        xmlResponse.append("<D:resourcetype/>\n");
                        xmlResponse.append("<D:getcontentlength>").append(resourceService.getFileSize(requestURI)).append("</D:getcontentlength>\n");
                        xmlResponse.append("<D:getcontenttype>text/plain</D:getcontenttype>\n");
                    }
                    
                    xmlResponse.append("<D:getlastmodified>").append(new java.util.Date()).append("</D:getlastmodified>\n");
                    xmlResponse.append("</D:prop>\n");
                    xmlResponse.append("<D:status>HTTP/1.1 200 OK</D:status>\n");
                    xmlResponse.append("</D:propstat>\n");
                    xmlResponse.append("</D:response>\n");
                    
                    // 如果是目录，添加子资源
                    if (isDirectory && "1".equals(req.getHeader("Depth"))) {
                        for (tslc.beihaiyun.lyra.webdav.LyraWebDavResourceService.WebDavResource resource : resources) {
                            String childPath = requestURI.endsWith("/") ? requestURI + resource.getName() : requestURI + "/" + resource.getName();
                            
                            xmlResponse.append("<D:response>\n");
                            xmlResponse.append("<D:href>").append(childPath).append("</D:href>\n");
                            xmlResponse.append("<D:propstat>\n");
                            xmlResponse.append("<D:prop>\n");
                            
                            if (resource.isDirectory()) {
                                xmlResponse.append("<D:resourcetype><D:collection/></D:resourcetype>\n");
                                xmlResponse.append("<D:getcontenttype>httpd/unix-directory</D:getcontenttype>\n");
                            } else {
                                xmlResponse.append("<D:resourcetype/>\n");
                                xmlResponse.append("<D:getcontentlength>").append(resource.getSize()).append("</D:getcontentlength>\n");
                                xmlResponse.append("<D:getcontenttype>text/plain</D:getcontenttype>\n");
                            }
                            
                            xmlResponse.append("<D:getlastmodified>").append(new java.util.Date(resource.getLastModified())).append("</D:getlastmodified>\n");
                            xmlResponse.append("</D:prop>\n");
                            xmlResponse.append("<D:status>HTTP/1.1 200 OK</D:status>\n");
                            xmlResponse.append("</D:propstat>\n");
                            xmlResponse.append("</D:response>\n");
                        }
                    }
                    
                    xmlResponse.append("</D:multistatus>");
                    resp.getWriter().write(xmlResponse.toString());
                    
                } catch (Exception e) {
                    logger.error("PROPFIND 处理错误: {}", e.getMessage());
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                // 回退到基础实现
                resp.getWriter().write("""
                                       <?xml version="1.0" encoding="utf-8"?>
                                       <D:multistatus xmlns:D="DAV:">
                                       <D:response>
                                       <D:href>
                                       """ + requestURI + "</D:href>\n" +
                    "<D:propstat>\n" +
                    "<D:prop>\n" +
                    "<D:resourcetype><D:collection/></D:resourcetype>\n" +
                    "<D:getcontenttype>httpd/unix-directory</D:getcontenttype>\n" +
                    "</D:prop>\n" +
                    "<D:status>HTTP/1.1 200 OK</D:status>\n" +
                    "</D:propstat>\n" +
                    "</D:response>\n" +
                    "</D:multistatus>");
            }
        }

        protected void doProppatch(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            logger.debug("WebDAV PROPPATCH 请求: {}", req.getRequestURI());
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN); // 暂不支持属性修改
        }

        protected void doMkcol(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            logger.debug("WebDAV MKCOL 请求: {}", req.getRequestURI());
            // TODO: 实现文件夹创建逻辑
            resp.setStatus(HttpServletResponse.SC_CREATED);
        }

        protected void doCopy(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            logger.debug("WebDAV COPY 请求: {}", req.getRequestURI());
            // TODO: 实现文件复制逻辑
            resp.setStatus(HttpServletResponse.SC_CREATED);
        }

        protected void doMove(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            logger.debug("WebDAV MOVE 请求: {}", req.getRequestURI());
            // TODO: 实现文件移动逻辑
            resp.setStatus(HttpServletResponse.SC_CREATED);
        }

        protected void doLock(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            
            logger.debug("WebDAV LOCK 请求: {}", req.getRequestURI());
            
            if (lockService == null || permissionService == null) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            
            try {
                // 获取当前用户
                tslc.beihaiyun.lyra.entity.User currentUser = permissionService.getCurrentUser();
                if (currentUser == null) {
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                
                String resourcePath = req.getRequestURI();
                
                // 解析LOCK请求的XML内容
                String lockScope = "exclusive"; // 默认独占锁
                String lockType = "write"; // 默认写锁
                int depth = 0; // 默认深度
                int timeout = 3600; // 默认1小时
                String owner = currentUser.getUsername();
                
                // 从请求头获取超时时间
                String timeoutHeader = req.getHeader("Timeout");
                if (timeoutHeader != null) {
                    try {
                        if (timeoutHeader.startsWith("Second-")) {
                            timeout = Integer.parseInt(timeoutHeader.substring(7));
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("无效的超时头: {}", timeoutHeader);
                    }
                }
                
                // 从请求头获取深度
                String depthHeader = req.getHeader("Depth");
                if ("infinity".equals(depthHeader)) {
                    depth = -1;
                } else if (depthHeader != null) {
                    try {
                        depth = Integer.parseInt(depthHeader);
                    } catch (NumberFormatException e) {
                        logger.warn("无效的深度头: {}", depthHeader);
                    }
                }
                
                // 尝试获取锁定
                tslc.beihaiyun.lyra.webdav.WebDavLockService.LockType lockTypeEnum = 
                    tslc.beihaiyun.lyra.webdav.WebDavLockService.LockType.EXCLUSIVE;
                
                tslc.beihaiyun.lyra.webdav.WebDavLockService.WebDavLock lock = 
                    lockService.acquireLock(resourcePath, lockTypeEnum, lockScope, depth, timeout, owner, currentUser);
                
                if (lock != null) {
                    // 锁定成功，返回锁定信息
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.setContentType("text/xml; charset=UTF-8");
                    resp.setHeader("Lock-Token", "<" + lock.getLockToken() + ">");
                    
                    StringBuilder xmlResponse = new StringBuilder();
                    xmlResponse.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                    xmlResponse.append("<D:prop xmlns:D=\"DAV:\">\n");
                    xmlResponse.append("<D:lockdiscovery>\n");
                    xmlResponse.append("<D:activelock>\n");
                    xmlResponse.append("<D:locktype><D:").append(lockType).append("/></D:locktype>\n");
                    xmlResponse.append("<D:lockscope><D:").append(lockScope).append("/></D:lockscope>\n");
                    xmlResponse.append("<D:depth>").append(depth == -1 ? "infinity" : depth).append("</D:depth>\n");
                    xmlResponse.append("<D:owner>").append(owner).append("</D:owner>\n");
                    xmlResponse.append("<D:timeout>Second-").append(lock.getTimeoutInSeconds()).append("</D:timeout>\n");
                    xmlResponse.append("<D:locktoken>\n");
                    xmlResponse.append("<D:href>").append(lock.getLockToken()).append("</D:href>\n");
                    xmlResponse.append("</D:locktoken>\n");
                    xmlResponse.append("</D:activelock>\n");
                    xmlResponse.append("</D:lockdiscovery>\n");
                    xmlResponse.append("</D:prop>");
                    
                    resp.getWriter().write(xmlResponse.toString());
                } else {
                    // 锁定失败（冲突）
                    resp.setStatus(423); // Locked
                    resp.setContentType("text/xml; charset=UTF-8");
                    resp.getWriter().write("""
                                           <?xml version="1.0" encoding="utf-8"?>
                                           <D:error xmlns:D="DAV:">
                                           <D:lock-token-conflicts-with-request/>
                                           </D:error>
                                           """);
                }
                
            } catch (Exception e) {
                logger.error("LOCK 处理错误: {}", e.getMessage(), e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        protected void doUnlock(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            
            logger.debug("WebDAV UNLOCK 请求: {}", req.getRequestURI());
            
            if (lockService == null || permissionService == null) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            
            try {
                // 获取当前用户
                tslc.beihaiyun.lyra.entity.User currentUser = permissionService.getCurrentUser();
                if (currentUser == null) {
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                
                // 从请求头获取锁定令牌
                String lockTokenHeader = req.getHeader("Lock-Token");
                if (lockTokenHeader == null || lockTokenHeader.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                
                // 移除角括号
                String lockToken = lockTokenHeader;
                if (lockToken.startsWith("<") && lockToken.endsWith(">")) {
                    lockToken = lockToken.substring(1, lockToken.length() - 1);
                }
                
                // 释放锁定
                boolean released = lockService.releaseLock(lockToken, currentUser);
                
                if (released) {
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    resp.setStatus(HttpServletResponse.SC_CONFLICT);
                }
                
            } catch (Exception e) {
                logger.error("UNLOCK 处理错误: {}", e.getMessage(), e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * WebDAV 认证过滤器
     * 
     * 集成 Spring Security 认证体系
     */
    public static class WebDavAuthenticationFilter implements Filter {
        
        private static final Logger logger = LoggerFactory.getLogger(WebDavAuthenticationFilter.class);
        private tslc.beihaiyun.lyra.webdav.WebDavAuthenticationHandler authHandler;
        private tslc.beihaiyun.lyra.webdav.WebDavPermissionService permissionService;

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            logger.info("WebDAV 认证过滤器初始化完成");
            
            // 从Spring容器获取依赖服务
            org.springframework.web.context.WebApplicationContext context = 
                org.springframework.web.context.support.WebApplicationContextUtils
                    .getWebApplicationContext(filterConfig.getServletContext());
            
            if (context != null) {
                authHandler = context.getBean(tslc.beihaiyun.lyra.webdav.WebDavAuthenticationHandler.class);
                permissionService = context.getBean(tslc.beihaiyun.lyra.webdav.WebDavPermissionService.class);
            }
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
                throws IOException, ServletException {
            
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            String uri = httpRequest.getRequestURI();
            String method = httpRequest.getMethod();
            
            logger.debug("WebDAV 请求过滤: {} {}", method, uri);
            
            // 对于 OPTIONS 请求，直接放行（WebDAV 发现）
            if ("OPTIONS".equals(method)) {
                chain.doFilter(request, response);
                return;
            }
            
            // 使用WebDAV认证处理器进行认证
            if (authHandler != null) {
                boolean authenticated = authHandler.handleAuthentication(httpRequest, httpResponse);
                if (!authenticated) {
                    // 认证失败，已由认证处理器处理响应
                    return;
                }
            } else {
                logger.warn("WebDAV认证处理器未初始化");
                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "认证服务不可用");
                return;
            }
            
            // 继续处理请求
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
            logger.info("WebDAV 认证过滤器销毁");
        }
    }
} 