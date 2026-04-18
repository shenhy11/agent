package com.agent.controller;

import com.agent.config.JwtUtil;
import com.agent.model.ApiResult;
import com.agent.model.BusinessException;
import com.agent.model.ErrorCode;
import com.agent.model.SysUser;
import com.agent.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器：登录、刷新 Token
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 用户登录，返回 Access + Refresh Token
     */
    @PostMapping("/login")
    public ApiResult<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        SysUser user = userService.authenticate(request.getUsername(), request.getPassword());
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        return ApiResult.success(Map.of(
            "accessToken", accessToken,
            "refreshToken", refreshToken,
            "username", user.getUsername(),
            "role", user.getRole(),
            "nickname", user.getNickname() != null ? user.getNickname() : user.getUsername()
        ));
    }

    /**
     * 刷新 Access Token
     */
    @PostMapping("/refresh")
    public ApiResult<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || !jwtUtil.isTokenValid(refreshToken)) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }
        String username = jwtUtil.extractUsername(refreshToken);
        SysUser user = userService.findByUsername(username);
        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole());
        return ApiResult.success(Map.of("accessToken", newAccessToken));
    }

    // ===== 内部 DTO =====

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }
}
