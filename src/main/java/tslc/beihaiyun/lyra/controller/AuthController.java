package tslc.beihaiyun.lyra.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tslc.beihaiyun.lyra.config.LyraProperties;
import tslc.beihaiyun.lyra.dto.AuthRequest;
import tslc.beihaiyun.lyra.dto.AuthResponse;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import tslc.beihaiyun.lyra.service.JwtService;
import tslc.beihaiyun.lyra.service.UserService;

/**
 * 认证控制器
 * 处理用户登录、注册、令牌刷新、登出等认证相关操作
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final LyraProperties lyraProperties;

    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @param bindingResult 验证结果
     * @param request HTTP请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse.ApiResponse<AuthResponse.LoginResponse>> login(
            @Valid @RequestBody AuthRequest.LoginRequest loginRequest,
            BindingResult bindingResult,
            HttpServletRequest request) {
        
        log.info("用户登录请求: {}", loginRequest.getUsernameOrEmail());

        // 验证请求参数
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest()
                    .body(AuthResponse.ApiResponse.error("请求参数验证失败", errors));
        }

        try {
            // 查找用户
            Optional<User> userOpt = userService.findByUsernameOrEmail(loginRequest.getUsernameOrEmail());
            if (userOpt.isEmpty()) {
                log.warn("登录失败，用户不存在: {}", loginRequest.getUsernameOrEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.ApiResponse.error("用户名或密码错误"));
            }

            User user = userOpt.get();

            // 检查用户状态
            if (!userService.isUserActive(user)) {
                String errorMessage = getUserStatusMessage(user);
                log.warn("登录失败，用户状态异常: {} - {}", user.getUsername(), errorMessage);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.ApiResponse.error(errorMessage));
            }

            // 执行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(), // 使用用户名进行认证
                            loginRequest.getPassword()
                    )
            );

            // 生成令牌
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtService.generateTokenWithUserId(userDetails, user.getId());
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            // 更新登录信息
            String clientIp = getClientIpAddress(request);
            userService.updateLastLoginInfo(user.getId(), clientIp);
            userService.resetFailedLoginAttempts(user.getId());

            // 构建响应
            AuthResponse.LoginResponse loginResponse = new AuthResponse.LoginResponse();
            loginResponse.setAccessToken(accessToken);
            loginResponse.setRefreshToken(refreshToken);
            loginResponse.setExpiresIn(lyraProperties.getJwt().getExpiration() / 1000); // 转换为秒
            loginResponse.setUserInfo(AuthResponse.UserInfo.fromUser(user));

            log.info("用户登录成功: {}", user.getUsername());
            return ResponseEntity.ok(AuthResponse.ApiResponse.success("登录成功", loginResponse));

        } catch (BadCredentialsException e) {
            // 增加失败次数
            Optional<User> userOpt = userService.findByUsernameOrEmail(loginRequest.getUsernameOrEmail());
            if (userOpt.isPresent()) {
                userService.incrementFailedLoginAttempts(userOpt.get().getId());
            }
            
            log.warn("登录失败，凭据错误: {}", loginRequest.getUsernameOrEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.ApiResponse.error("用户名或密码错误"));
                    
        } catch (DisabledException e) {
            log.warn("登录失败，账户已禁用: {}", loginRequest.getUsernameOrEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.ApiResponse.error("账户已被禁用"));
                    
        } catch (LockedException e) {
            log.warn("登录失败，账户已锁定: {}", loginRequest.getUsernameOrEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.ApiResponse.error("账户已被锁定"));
                    
        } catch (Exception e) {
            log.error("登录过程中发生未知错误: {}", loginRequest.getUsernameOrEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.ApiResponse.error("登录失败，请稍后重试"));
        }
    }

    /**
     * 用户注册
     * 
     * @param registerRequest 注册请求
     * @param bindingResult 验证结果
     * @return 注册响应
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse.ApiResponse<AuthResponse.RegisterResponse>> register(
            @Valid @RequestBody AuthRequest.RegisterRequest registerRequest,
            BindingResult bindingResult) {
        
        log.info("用户注册请求: {}", registerRequest.getUsername());

        // 验证请求参数
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest()
                    .body(AuthResponse.ApiResponse.error("请求参数验证失败", errors));
        }

        try {
            // 执行用户注册
            User user = userService.registerUser(registerRequest);

            // 生成邮箱验证令牌
            String verificationToken = userService.generateEmailVerificationToken(user.getEmail());
            
            // TODO: 发送验证邮件（这里只是模拟，实际应该集成邮件服务）
            log.info("邮箱验证链接: /api/auth/verify-email?token={}", verificationToken);

            // 构建响应
            AuthResponse.RegisterResponse registerResponse = new AuthResponse.RegisterResponse();
            registerResponse.setUserId(user.getId());
            registerResponse.setUsername(user.getUsername());
            registerResponse.setEmail(user.getEmail());
            registerResponse.setStatus(user.getStatus());
            registerResponse.setRequiresApproval(User.UserStatus.PENDING.equals(user.getStatus()));
            registerResponse.setEmailVerificationSent(true);
            registerResponse.setMessage("注册成功，请检查邮箱进行验证，并等待管理员审核");

            log.info("用户注册成功: {} (ID: {})", user.getUsername(), user.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AuthResponse.ApiResponse.success("注册成功", registerResponse));

        } catch (IllegalArgumentException e) {
            log.warn("注册失败，参数错误: {} - {}", registerRequest.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(AuthResponse.ApiResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            log.error("注册过程中发生未知错误: {}", registerRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.ApiResponse.error("注册失败，请稍后重试"));
        }
    }

    /**
     * 刷新访问令牌
     * 
     * @param refreshRequest 刷新令牌请求
     * @param bindingResult 验证结果
     * @return 刷新响应
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse.ApiResponse<AuthResponse.RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody AuthRequest.RefreshTokenRequest refreshRequest,
            BindingResult bindingResult) {
        
        log.debug("令牌刷新请求");

        // 验证请求参数
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest()
                    .body(AuthResponse.ApiResponse.error("请求参数验证失败", errors));
        }

        try {
            String refreshToken = refreshRequest.getRefreshToken();
            
            // 验证刷新令牌
            if (!jwtService.isTokenValid(refreshToken)) {
                log.warn("无效的刷新令牌");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.ApiResponse.error("无效的刷新令牌"));
            }

            // 提取用户名
            String username = jwtService.extractUsername(refreshToken);
            Optional<User> userOpt = userService.findByUsernameOrEmail(username);
            
            if (userOpt.isEmpty()) {
                log.warn("刷新令牌对应的用户不存在: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.ApiResponse.error("用户不存在"));
            }

            User user = userOpt.get();
            
            // 检查用户状态
            if (!userService.isUserActive(user)) {
                String errorMessage = getUserStatusMessage(user);
                log.warn("令牌刷新失败，用户状态异常: {} - {}", user.getUsername(), errorMessage);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.ApiResponse.error(errorMessage));
            }

            // 生成新的访问令牌
            LyraUserPrincipal userPrincipal = LyraUserPrincipal.fromUser(user);
            String newAccessToken = jwtService.generateTokenWithUserId(userPrincipal, user.getId());

            // 构建响应
            AuthResponse.RefreshTokenResponse response = new AuthResponse.RefreshTokenResponse();
            response.setAccessToken(newAccessToken);
            response.setExpiresIn(lyraProperties.getJwt().getExpiration() / 1000); // 转换为秒

            log.debug("令牌刷新成功: {}", user.getUsername());
            return ResponseEntity.ok(AuthResponse.ApiResponse.success("令牌刷新成功", response));

        } catch (Exception e) {
            log.error("令牌刷新过程中发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.ApiResponse.error("令牌刷新失败，请重新登录"));
        }
    }

    /**
     * 用户登出
     * 
     * @param request HTTP请求
     * @return 登出响应
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse.ApiResponse<Void>> logout(HttpServletRequest request) {
        log.debug("用户登出请求");

        try {
            // 从请求头中提取令牌
            String token = extractTokenFromRequest(request);
            
            if (token != null) {
                // 将令牌加入黑名单
                jwtService.logoutToken(token);
                log.info("用户登出成功，令牌已加入黑名单");
            }

            // 清除安全上下文
            SecurityContextHolder.clearContext();

            return ResponseEntity.ok(AuthResponse.ApiResponse.success("登出成功"));

        } catch (Exception e) {
            log.error("登出过程中发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.ApiResponse.error("登出失败"));
        }
    }

    /**
     * 请求密码重置
     * 
     * @param resetRequest 密码重置请求
     * @param bindingResult 验证结果
     * @return 重置响应
     */
    @PostMapping("/password/reset-request")
    public ResponseEntity<AuthResponse.ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody AuthRequest.PasswordResetRequest resetRequest,
            BindingResult bindingResult) {
        
        log.info("密码重置请求: {}", resetRequest.getEmail());

        // 验证请求参数
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest()
                    .body(AuthResponse.ApiResponse.error("请求参数验证失败", errors));
        }

        try {
            String resetToken = userService.generatePasswordResetToken(resetRequest.getEmail());
            
            if (resetToken != null) {
                // TODO: 发送重置邮件（这里只是模拟，实际应该集成邮件服务）
                log.info("密码重置链接: /api/auth/password/reset?token={}", resetToken);
            }

            // 为了安全，无论用户是否存在都返回成功
            return ResponseEntity.ok(AuthResponse.ApiResponse.success(
                    "如果邮箱存在，我们已发送密码重置链接到您的邮箱"));

        } catch (Exception e) {
            log.error("密码重置请求处理错误: {}", resetRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.ApiResponse.error("密码重置请求失败，请稍后重试"));
        }
    }

    /**
     * 确认密码重置
     * 
     * @param confirmRequest 密码重置确认请求
     * @param bindingResult 验证结果
     * @return 重置响应
     */
    @PostMapping("/password/reset-confirm")
    public ResponseEntity<AuthResponse.ApiResponse<Void>> confirmPasswordReset(
            @Valid @RequestBody AuthRequest.PasswordResetConfirmRequest confirmRequest,
            BindingResult bindingResult) {
        
        log.info("密码重置确认请求");

        // 验证请求参数
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest()
                    .body(AuthResponse.ApiResponse.error("请求参数验证失败", errors));
        }

        // 验证密码确认
        if (!confirmRequest.getNewPassword().equals(confirmRequest.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.ApiResponse.error("新密码和确认密码不匹配"));
        }

        try {
            boolean success = userService.resetPassword(
                    confirmRequest.getResetToken(), 
                    confirmRequest.getNewPassword()
            );

            if (success) {
                log.info("密码重置成功");
                return ResponseEntity.ok(AuthResponse.ApiResponse.success("密码重置成功"));
            } else {
                log.warn("密码重置失败，令牌无效或已过期");
                return ResponseEntity.badRequest()
                        .body(AuthResponse.ApiResponse.error("重置令牌无效或已过期"));
            }

        } catch (Exception e) {
            log.error("密码重置确认处理错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.ApiResponse.error("密码重置失败，请稍后重试"));
        }
    }

    /**
     * 邮箱验证
     * 
     * @param verificationRequest 邮箱验证请求
     * @param bindingResult 验证结果
     * @return 验证响应
     */
    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse.ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody AuthRequest.EmailVerificationRequest verificationRequest,
            BindingResult bindingResult) {
        
        log.info("邮箱验证请求");

        // 验证请求参数
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest()
                    .body(AuthResponse.ApiResponse.error("请求参数验证失败", errors));
        }

        try {
            boolean success = userService.verifyEmail(verificationRequest.getVerificationToken());

            if (success) {
                log.info("邮箱验证成功");
                return ResponseEntity.ok(AuthResponse.ApiResponse.success("邮箱验证成功"));
            } else {
                log.warn("邮箱验证失败，令牌无效或已过期");
                return ResponseEntity.badRequest()
                        .body(AuthResponse.ApiResponse.error("验证令牌无效或已过期"));
            }

        } catch (Exception e) {
            log.error("邮箱验证处理错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.ApiResponse.error("邮箱验证失败，请稍后重试"));
        }
    }

    /**
     * 通过GET方式进行邮箱验证（用于邮件链接）
     * 
     * @param token 验证令牌
     * @return 验证响应
     */
    @GetMapping("/verify-email")
    public ResponseEntity<AuthResponse.ApiResponse<Void>> verifyEmailByGet(@RequestParam(required = false) String token) {
        log.info("通过GET方式进行邮箱验证");

        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.ApiResponse.error("验证令牌不能为空"));
        }

        try {
            boolean success = userService.verifyEmail(token);

            if (success) {
                log.info("邮箱验证成功");
                return ResponseEntity.ok(AuthResponse.ApiResponse.success("邮箱验证成功"));
            } else {
                log.warn("邮箱验证失败，令牌无效或已过期");
                return ResponseEntity.badRequest()
                        .body(AuthResponse.ApiResponse.error("验证令牌无效或已过期"));
            }

        } catch (Exception e) {
            log.error("邮箱验证处理错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.ApiResponse.error("邮箱验证失败，请稍后重试"));
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 根据用户状态获取错误消息
     */
    private String getUserStatusMessage(User user) {
        if (!user.getEnabled()) {
            return "账户未激活或已被禁用";
        }
        if (!user.getAccountNonLocked()) {
            return "账户已被锁定";
        }
        return switch (user.getStatus()) {
            case PENDING -> "账户等待审核中";
            case DISABLED -> "账户已被禁用";
            case LOCKED -> "账户已被锁定";
            case DEACTIVATED -> "账户已注销";
            default -> "账户状态异常";
        };
    }

    /**
     * 从请求头中提取JWT令牌
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
} 