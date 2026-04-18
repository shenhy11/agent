package com.agent.controller;

import com.agent.model.ApiResult;
import com.agent.model.SysUser;
import com.agent.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员控制器：用户管理（需 ADMIN 角色）
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    /** 获取所有用户列表 */
    @GetMapping("/users")
    public ApiResult<List<Map<String, Object>>> listUsers() {
        List<Map<String, Object>> users = userService.findAllUsers().stream()
                .map(u -> Map.<String, Object>of(
                    "id", u.getId(),
                    "username", u.getUsername(),
                    "nickname", u.getNickname() != null ? u.getNickname() : "",
                    "role", u.getRole(),
                    "enabled", u.getEnabled(),
                    "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
                ))
                .toList();
        return ApiResult.success(users);
    }

    /** 创建新用户 */
    @PostMapping("/users")
    public ApiResult<Map<String, Object>> createUser(@Valid @RequestBody CreateUserRequest request) {
        SysUser user = userService.createUser(
            request.getUsername(),
            request.getPassword(),
            request.getRole(),
            request.getNickname()
        );
        return ApiResult.success(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "role", user.getRole()
        ));
    }

    // ===== 内部 DTO =====

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String role = "USER";
        private String nickname;
    }
}
