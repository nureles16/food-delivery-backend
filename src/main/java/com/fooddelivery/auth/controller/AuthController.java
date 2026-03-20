package com.fooddelivery.auth.controller;

import com.fooddelivery.auth.dto.*;
import com.fooddelivery.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "API для регистрации, входа и обновления токена")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Регистрирует нового клиента по email + пароль или phone",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешная регистрация"),
                    @ApiResponse(responseCode = "400", description = "Некорректные данные")
            }
    )
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered successfully");
    }

    @Operation(
            summary = "Вход пользователя",
            description = "Проверяет email и пароль, возвращает JWT Access и Refresh токены",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный вход",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неверные данные")
            }
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Обновление Access токена",
            description = "Возвращает новый Access Token по Refresh Token",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Access Token обновлён",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неверный Refresh Token")
            }
    )
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshRequest request){

        AuthResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Запрос на сброс пароля",
            description = "Отправляет ссылку для сброса пароля на email пользователя",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Если email существует — письмо отправлено"),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                            content = @Content(schema = @Schema(example = """
                        {
                          "email": "Некорректный email"
                        }
                        """)))
            }
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok("Password reset token sent (check console/email)");
    }

    @Operation(
            summary = "Сброс пароля",
            description = "Сбрасывает пароль пользователя по токену из email",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пароль успешно обновлён"),
                    @ApiResponse(responseCode = "400", description = "Неверный токен или ошибка валидации")
            }
    )
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Password has been reset successfully");
    }

    @Operation(
            summary = "Выход пользователя",
            description = "Отмечает Refresh Token как отозванный (Blacklist) для последующего запрета обновления Access Token",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пользователь успешно вышел",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "401", description = "Refresh Token не найден или уже отозван")
            }
    )
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@Valid @RequestBody RefreshRequest request) {

        authService.logout(request.getRefreshToken());

        return ResponseEntity.ok("Logged out successfully");
    }
}