package com.fooddelivery.mapper;

import com.fooddelivery.auth.dto.CreateCafeAdminRequest;
import com.fooddelivery.auth.dto.RegisterRequest;
import com.fooddelivery.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toUser(RegisterRequest request) {
        if (request == null) {
            return null;
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        return user;
    }

    public User toUser(CreateCafeAdminRequest request) {
        if (request == null) {
            return null;
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setCafeId(request.getCafeId());
        return user;
    }
}