package com.agent.service;

import com.agent.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务：注册、登录验证、查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 注册新用户（ADMIN 调用）
     */
    @Transactional
    public SysUser createUser(String username, String rawPassword, String role, String nickname) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USER_001);
        }
        SysUser user = SysUser.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(role != null ? role : "USER")
                .nickname(nickname != null ? nickname : username)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    /**
     * 验证用户名密码是否正确
     */
    public SysUser authenticate(String username, String rawPassword) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_001));
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.USER_003);
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.AUTH_001);
        }
        return user;
    }

    /**
     * 根据用户名查询用户（Spring Security 加载用时使用）
     */
    public SysUser findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_002));
    }

    /**
     * 查询所有用户（ADMIN 使用）
     */
    public List<SysUser> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 初始化默认管理员（应用首次启动时调用）
     */
    @Transactional
    public void initAdminIfAbsent() {
        if (!userRepository.existsByUsername("admin")) {
            createUser("admin", "Admin@123456", "ADMIN", "系统管理员");
            log.info("默认管理员账号已初始化，用户名: admin，密码: Admin@123456，请尽快修改！");
        }
    }
}
