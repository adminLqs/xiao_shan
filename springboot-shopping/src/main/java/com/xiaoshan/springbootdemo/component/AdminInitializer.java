package com.xiaoshan.springbootdemo.component;

import com.xiaoshan.springbootdemo.entity.Role;
import com.xiaoshan.springbootdemo.entity.User;
import com.xiaoshan.springbootdemo.entity.UserRole;

import java.time.LocalDateTime;
import com.xiaoshan.springbootdemo.mapper.RoleMapper;
import com.xiaoshan.springbootdemo.mapper.UserMapper;
import com.xiaoshan.springbootdemo.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdmin();
    }

    private void initAdmin() {
        String adminAccount = "13295370591";
        String adminPassword = "123123";

        if (userMapper.findByAccount(adminAccount).isPresent()) {
            log.info("管理员账号已存在，跳过初始化");
            return;
        }

        User admin = new User();
        admin.setAccount(adminAccount);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setStatus(true);
        admin.setRole("ROLE_ADMIN");
        admin.setCreatedAt(LocalDateTime.now());
        userMapper.insert(admin);

        Role adminRole = roleMapper.findByName("ROLE_ADMIN");
        if (adminRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(admin.getId());
            userRole.setRoleId(adminRole.getId());
            userRoleMapper.insert(userRole);
        }

        log.info("管理员账号初始化完成: {}", adminAccount);
    }
}