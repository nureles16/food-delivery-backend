package com.fooddelivery.auth.repository;


import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {

        User user = new User();
        user.setEmail("test@mail.com");
        user.setPasswordHash("hashedpassword");
        user.setRole(Role.CLIENT);

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@mail.com");

        assertTrue(found.isPresent());
        assertEquals(Role.CLIENT, found.get().getRole());
    }
}