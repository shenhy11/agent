package com.agent.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 系统用户 Repository
 */
@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    /** 根据用户名查询用户 */
    Optional<SysUser> findByUsername(String username);

    /** 判断用户名是否已存在 */
    boolean existsByUsername(String username);
}
