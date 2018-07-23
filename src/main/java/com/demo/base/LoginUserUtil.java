package com.demo.base;

import com.demo.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

public class LoginUserUtil {

    public static User load() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal != null && principal instanceof User) {
            return (User) principal;
        }
        return null;
    }

    public static Long getLonginUserId() {
        User user = load();
        if (user == null) {
            return -1L;
        }
        return user.getId();
    }

}
