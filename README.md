# Food Delivery Backend

Backend сервис для платформы доставки еды.
Проект реализован на **Java + Spring Boot** и предоставляет API для работы с пользователями, кафе, каталогом еды и заказами.
---

# 📦 Технологический стек

* **Java 17**
* **Spring Boot 3**
* **Spring Security**
* **Spring Data JPA**
* **PostgreSQL**
* **JWT Authentication**
* **Maven**
---

# 🏗 Архитектура проекта

Проект разделён на модули:

```
src/main/java/com/fooddelivery

auth        → аутентификация и пользователи  
catalog     → кафе и меню  
orders      → заказы  
config      → конфигурации (security, redis, s3)  
common      → общие классы
```

---

# ⚙️ Настройка проекта

## 1. Клонировать репозиторий

```bash
git clone https://github.com/nureles16/food-delivery-backend.git
cd food-delivery-backend
```

---

## 2. Настроить PostgreSQL

Создать базу данных:

```
food_delivery
```

Создать пользователя:

```
username: postgres
password: postgres
```

---

## 3. Настроить application.properties

```
spring.application.name=fooddelivery

# --- PostgreSQL ---
spring.datasource.url=jdbc:postgresql://localhost:5432/food_delivery
spring.datasource.username=postgres
spring.datasource.password=***
spring.datasource.driver-class-name=org.postgresql.Driver

# --- HikariCP pool ---
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.idle-timeout=30000
spring.datasource.hikari.pool-name=FoodDeliveryHikariCP

# --- JPA / Hibernate ---
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

---

# 🚀 Запуск проекта

Запуск через Maven:

```bash
./mvnw spring-boot:run
```

или

```bash
mvn spring-boot:run
```

Приложение будет доступно:

```
http://localhost:8080
```

---

# 🔐 Аутентификация

Поддерживаются роли:

* **SUPER_ADMIN**
* **CAFE_ADMIN**
* **CLIENT**

Функционал:

* регистрация пользователей
* авторизация
* JWT токены
* управление ролями

---

# 🧩 Реализация Auth Module
## User Entity
Основная таблица пользователей.
```
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    private UUID cafeId;

    private boolean isActive = true;

    private boolean forcePasswordChange = false;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```
Особенности:
* UUID используется как primary key
* email уникален
* пароль хранится в виде BCrypt hash
* поддерживаются роли пользователей

## Role Enum

Определяет роли пользователей.
```
public enum Role {
SUPER_ADMIN,
CAFE_ADMIN,
CLIENT
}
```

## UserRepository

Репозиторий для работы с пользователями.
```
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

}
```
Возможности:

* поиск пользователя по email

* проверка существования email

* стандартные CRUD операции

## CustomUserDetailsService

Интеграция пользователей с Spring Security.
```
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}
```
Назначение:

* загрузка пользователя из базы данных

* передача данных в Spring Security

* установка ролей пользователя

## Security Configuration

Конфигурация шифрования паролей.
```
@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

}
```
Особенности:

* используется BCrypt

* cost factor = 12

* безопасное хранение паролей
---

## Auth API

Система аутентификации предоставляет REST API для регистрации, входа и обновления JWT токенов.

### Register User

Регистрация нового пользователя с ролью **CLIENT**.

```
POST /auth/register
```

Пример запроса:

```json
{
  "email": "user@mail.com",
  "password": "123456",
  "phone": "+996555123456"
}
```

Логика:

* валидация email и пароля
* проверка существования email
* хэширование пароля через **BCrypt**
* сохранение пользователя в базе данных
* роль по умолчанию → **CLIENT**

---

### Login User

Авторизация пользователя.

```
POST /auth/login
```

Пример запроса:

```json
{
  "email": "user@mail.com",
  "password": "123456"
}
```

Ответ:

```json
{
  "accessToken": "JWT_ACCESS_TOKEN",
  "refreshToken": "JWT_REFRESH_TOKEN"
}
```

Логика:

* поиск пользователя по email
* проверка пароля через BCrypt
* генерация **JWT Access Token (15 минут)**
* генерация **JWT Refresh Token (30 дней)**

---

### Refresh Token

Обновление access token с помощью refresh token.

```
POST /auth/refresh
```

Пример запроса:

```json
{
  "refreshToken": "JWT_REFRESH_TOKEN"
}
```

Ответ:

```json
{
  "accessToken": "NEW_ACCESS_TOKEN",
  "refreshToken": "JWT_REFRESH_TOKEN"
}
```

Логика:

* извлечение email из refresh token
* генерация нового **access token**
* refresh token остаётся прежним

---

## JWT Configuration

JWT используется для аутентификации пользователей.

Особенности:

* Access Token TTL → **15 минут**
* Refresh Token TTL → **30 дней**
* подпись токена через **HMAC SHA256**
* токен содержит **email пользователя**

JWT используется для доступа к защищённым API.

Пример заголовка запроса:

```
Authorization: Bearer ACCESS_TOKEN
```

---
# 📝 Документация API (Swagger / OpenAPI)

Проект использует Springdoc OpenAPI для генерации интерактивной документации Swagger UI.

После запуска приложения документация будет доступна по адресу:
```
http://localhost:8080/swagger-ui.html
```
или через стандартный OpenAPI endpoint:
```
http://localhost:8080/v3/api-docs
```
---
# 📁 Структура проекта

```
food-delivery-backend
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.fooddelivery
│   │   │        ├── auth
│   │   │        │    ├── controller
│   │   │        │    │     └── AuthController
│   │   │        │    │
│   │   │        │    ├── service
│   │   │        │    │     └── AuthService
│   │   │        │    │
│   │   │        │    ├── dto
│   │   │        │    │     ├── RegisterRequest
│   │   │        │    │     ├── LoginRequest
│   │   │        │    │     ├── RefreshRequest
│   │   │        │    │     └── AuthResponse
│   │   │        │    │
│   │   │        │    ├── entity
│   │   │        │    │     └── User
│   │   │        │    │
│   │   │        │    ├── repository
│   │   │        │    │     └── UserRepository
│   │   │        │    │
│   │   │        │    └── security
│   │   │        │          ├── CustomUserDetailsService
│   │   │        │          └── JwtService
│   │   │        │
│   │   │        ├── catalog
│   │   │        │    ├── entity
│   │   │        │    ├── repository
│   │   │        │    ├── service
│   │   │        │    └── controller
│   │   │        │
│   │   │        ├── orders
│   │   │        │    ├── entity
│   │   │        │    ├── repository
│   │   │        │    ├── service
│   │   │        │    └── controller
│   │   │        │
│   │   │        └── config
│   │   │             ├── SecurityConfig
│   │   │             └── OpenApiConfig
│   │   │
│   │   └── resources
│   │        └── application.properties
│
├── src/test
│
├── pom.xml
└── README.md
```

---

# 👨‍💻 Автор

**Nureles Almazbekov**

Java Backend Developer
Spring Boot | PostgreSQL
