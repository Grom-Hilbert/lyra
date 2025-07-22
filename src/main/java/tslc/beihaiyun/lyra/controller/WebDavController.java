package tslc.beihaiyun.lyra.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tslc.beihaiyun.lyra.webdav.LyraWebDavResourceService;
import tslc.beihaiyun.lyra.webdav.LyraResource;

/**
 * WebDAV 控制器
 * 
 * 处理WebDAV请求，支持标准WebDAV方法
 * 与ServletRegistrationBean配合使用，提供测试环境支持
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-22
 */
@RestController
@RequestMapping("/webdav")
@PreAuthorize("hasRole('USER')")
public class WebDavController {

    private static final Logger logger = LoggerFactory.getLogger(WebDavController.class);

    private final LyraWebDavResourceService resourceService;

    public WebDavController(LyraWebDavResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * 处理PROPFIND请求
     * 
     * @param request HTTP请求
     * @param response HTTP响应  
     * @param depth 深度参数
     * @return XML响应
     */
    @RequestMapping(value = "/**")
    public ResponseEntity<String> handleWebDavRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader(value = "Depth", defaultValue = "0") String depth) {
        
        String method = request.getMethod();
        
        if ("PROPFIND".equals(method)) {
            return handlePropfind(request, response, depth);
        } else if ("OPTIONS".equals(method)) {
            return handleOptions(response);
        } else {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }
    }
    
    /**
     * 处理PROPFIND请求
     */
    private ResponseEntity<String> handlePropfind(
            HttpServletRequest request,
            HttpServletResponse response,
            String depth) {

        String requestURI = request.getRequestURI();
        logger.debug("WebDAV PROPFIND 请求: {} (深度: {})", requestURI, depth);

        try {
            // 获取资源
            LyraResource resource = resourceService.getResource(requestURI);
            if (resource == null) {
                return ResponseEntity.notFound().build();
            }

            // 构建XML响应
            StringBuilder xmlResponse = new StringBuilder();
            xmlResponse.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            xmlResponse.append("<D:multistatus xmlns:D=\"DAV:\">\n");

            // 添加主资源
            appendResourceProps(xmlResponse, resource, requestURI);

            // 如果是目录且深度为1，添加子资源
            if ("1".equals(depth) && resource.isCollection() && resource.getChildren() != null) {
                for (LyraResource child : resource.getChildren()) {
                    String childURI = requestURI.endsWith("/") ? requestURI + child.getName() : requestURI + "/" + child.getName();
                    appendResourceProps(xmlResponse, child, childURI);
                }
            }

            xmlResponse.append("</D:multistatus>");

            return ResponseEntity.status(207) // Multi-Status
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xmlResponse.toString());

        } catch (Exception e) {
            logger.error("PROPFIND 处理失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 处理OPTIONS请求
     */
    private ResponseEntity<String> handleOptions(HttpServletResponse response) {
        
        response.setHeader("Allow", "GET, POST, PUT, DELETE, HEAD, OPTIONS, PROPFIND, PROPPATCH, MKCOL, COPY, MOVE");
        response.setHeader("DAV", "1, 2");
        response.setHeader("MS-Author-Via", "DAV");
        
        return ResponseEntity.ok().build();
    }

    /**
     * 添加资源属性到XML响应
     */
    private void appendResourceProps(StringBuilder xml, LyraResource resource, String href) {
        xml.append("<D:response>\n");
        xml.append("<D:href>").append(escapeXml(href)).append("</D:href>\n");
        xml.append("<D:propstat>\n");
        xml.append("<D:prop>\n");

        // 基本属性
        xml.append("<D:resourcetype>");
        if (resource.isCollection()) {
            xml.append("<D:collection/>");
        }
        xml.append("</D:resourcetype>\n");

        xml.append("<D:displayname>").append(escapeXml(resource.getName())).append("</D:displayname>\n");

        if (!resource.isCollection()) {
            xml.append("<D:getcontentlength>").append(resource.getSize()).append("</D:getcontentlength>\n");
            if (resource.getContentType() != null) {
                xml.append("<D:getcontenttype>").append(escapeXml(resource.getContentType())).append("</D:getcontenttype>\n");
            }
        }

        if (resource.getLastModified() != null) {
            xml.append("<D:getlastmodified>").append(formatHttpDate(resource.getLastModified())).append("</D:getlastmodified>\n");
        }

        if (resource.getCreationDate() != null) {
            xml.append("<D:creationdate>").append(formatISODate(resource.getCreationDate())).append("</D:creationdate>\n");
        }

        // 版本控制属性
        if (resource.getCurrentVersionNumber() != null) {
            xml.append("<version-number>").append(resource.getCurrentVersionNumber()).append("</version-number>\n");
        }
        if (resource.getTotalVersionCount() != null) {
            xml.append("<version-count>").append(resource.getTotalVersionCount()).append("</version-count>\n");
        }

        xml.append("</D:prop>\n");
        xml.append("<D:status>HTTP/1.1 200 OK</D:status>\n");
        xml.append("</D:propstat>\n");
        xml.append("</D:response>\n");
    }

    /**
     * 转义XML特殊字符
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }

    /**
     * 格式化HTTP日期
     */
    private String formatHttpDate(java.util.Date date) {
        return new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.ENGLISH)
                .format(date);
    }

    /**
     * 格式化ISO日期
     */
    private String formatISODate(java.util.Date date) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .format(date);
    }
} 