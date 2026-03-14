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
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email: " + email)
                );

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is disabled");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}
```
Назначение:
* проверка активности пользователя

* интеграция пользователей с Spring Security

* установка ролей пользователя (SUPER_ADMIN, CAFE_ADMIN, CLIENT)

* загрузка пользователя из базы данных

* передача данных в Spring Security

## Security Configuration

Конфигурация шифрования паролей.
```
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth

                        // публичные auth endpoints
                        .requestMatchers("/auth/**").permitAll()

                        // swagger
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // публичный список ресторанов
                        .requestMatchers("/restaurants").permitAll()

                        // остальные endpoints требуют авторизацию
                        .anyRequest().authenticated()
                )

                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
```
Особенности:

* используется BCrypt для хэширования паролей

* включена Method Security (@PreAuthorize)

* публичные endpoints: /auth/** ,/restaurants

* Swagger документация

* остальные API требуют JWT авторизации

* доступ к методам контроллеров ограничивается ролями пользователей

Пример:
```
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
```
Используется для ограничения доступа к административным операциям.

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
# 🧩 Реализация Catalog Module

Модуль Catalog отвечает за управление ресторанами и их меню.
## Restaurant Entity
```
@Entity
@Table(name = "restaurants")
public class Restaurant {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    private String slug;

    private String description;

    private String address;

    private String city;

    private String logoUrl;

    @Enumerated(EnumType.STRING)
    private CuisineType cuisineType;

    private BigDecimal minOrderAmount;

    private Double deliveryZoneRadiusKm;

    private boolean isActive = true;

}
```
## CuisineType Enum
```
public enum CuisineType {

    BURGERS,
    PIZZA,
    SUSHI,
    ASIAN,
    LOCAL,
    COFFEE,
    OTHER

}
```
## RestaurantRepository
```
public interface RestaurantRepository
extends JpaRepository<Restaurant, UUID> {
}
```
---
# 🍽 Restaurant API
Получить список ресторанов
```
GET /restaurants
```
Публичный endpoint.

Возвращает список доступных ресторанов.

---
Создать ресторан
```
POST /admin/restaurants
```
Доступно только роли:

* SUPER_ADMIN

Пример запроса:
```
{
"name": "Burger House",
"description": "Лучшие бургеры",
"address": "ул. Киевская 120",
"city": "Bishkek",
"cuisineType": "BURGERS",
"minOrderAmount": 300,
"deliveryZoneRadiusKm": 5
}
```
Обновить ресторан
```
PUT /cafe/profile/{id}
```
Доступно роли:

* CAFE_ADMIN
* SUPER_ADMIN

Обновляет информацию о ресторане.

---
# 🧩 Реализация Catalog Module

Создана сущность для категорий меню ресторана
```
@Entity
@Table(name = "menu_categories")
public class MenuCategory {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID restaurantId;

    @Column(nullable = false)
    private String name;

    private Integer position;

    private boolean isActive = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```
Особенности:

* UUID используется как primary key

* restaurantId связывает категорию с рестораном

* position позволяет сортировать категории

* isActive — для активации/деактивации категории

## MenuCategoryRepository

Репозиторий для CRUD операций категорий меню:
```
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

    boolean existsByIdAndDishesIsEmpty(UUID id); // проверка перед удалением, если нет блюд
}
```
## 🍽 MenuCategory API

Эндпоинты для управления категориями меню ресторана
```
POST /cafe/menu/categories — Создать новую категорию меню (роль: CAFE_ADMIN)

PUT /cafe/menu/categories/{id} — Обновить категорию меню (роль: CAFE_ADMIN)

DELETE /cafe/menu/categories/{id} — Удалить категорию (только если нет блюд) (роль: CAFE_ADMIN)
```
Пример запроса POST
```
{
"restaurantId": "uuid-ресторана",
"name": "Burgers",
"position": 1
}
```
---
# 🧩 Реализация Catalog Module — MenuItem (позиции меню)

Создана сущность для позиций меню ресторана:
```
@Entity
@Table(name = "menu_items")
public class MenuItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    private String imageUrl;

    private boolean isAvailable = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```
Особенности:

* UUID используется как primary key

* categoryId связывает позицию с категорией меню

* isAvailable — для активации/деактивации позиции

* createdAt / updatedAt — метки времени для управления

## MenuItemRepository

Репозиторий для CRUD операций с позициями меню:
```
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findAllByCategoryId(UUID categoryId);
}
```
## 🍽 MenuItem API

Эндпоинты для управления позициями меню ресторана:
```
Метод	URL	Роль	Описание
POST	/cafe/menu/items	CAFE_ADMIN	Создать новую позицию меню
PUT	/cafe/menu/items/{id}	CAFE_ADMIN	Обновить существующую позицию меню
PATCH	/cafe/menu/items/{id}/availability	CAFE_ADMIN	Обновить статус доступности позиции
GET	/cafe/menu/items/category/{categoryId}	PUBLIC	Получить все позиции по категории
```
Пример запроса POST:
```
{
"categoryId": "uuid-категории",
"name": "Чизбургер",
"description": "Сочный чизбургер с сыром",
"price": 450.50,
"imageUrl": "https://example.com/image.jpg",
"isAvailable": true
}
```
---

# 🍔 Order Module (Заказы)

Модуль orders отвечает за создание, управление и отслеживание заказов пользователей, а также работу администраторов кафе.

## Order Entity
Сущность заказа:
```
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue
    private UUID id;

    private String orderNumber;

    private UUID clientId;

    private UUID restaurantId;

    @Column(columnDefinition = "TEXT")
    private String items; // JSON snapshot заказанных блюд

    private BigDecimal subtotal;

    private BigDecimal deliveryFee;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String deliveryAddress;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```
Особенности:

* UUID используется как primary key

* items хранит snapshot заказанных позиций меню

* subtotal, deliveryFee, totalAmount — расчёт стоимости

* статус заказа (PENDING, CONFIRMED, CANCELLED)

* связь с клиентом и рестораном через UUID

## OrderStatus Enum
```
public enum OrderStatus {
PENDING,     // заказ создан, ожидает подтверждения
CONFIRMED,   // заказ подтверждён кафе
CANCELLED    // заказ отменён клиентом
}
```
## OrderRepository

Репозиторий для работы с заказами:
```
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByClientId(UUID clientId);

    List<Order> findByRestaurantId(UUID restaurantId);
}
```
## DTO для создания заказа
```
public class OrderItemDto {
private UUID itemId;
private int quantity;
}

public class CreateOrderRequest {
private UUID restaurantId;
private List<OrderItemDto> items;
private String deliveryAddress;
}
```
## OrderService

Сервис реализует бизнес-логику:

* создание заказа

* получение истории заказов клиента

* получение заказов ресторана

* подтверждение заказа кафе

* отмена заказа клиентом

Методы:
```
createOrder(CreateOrderRequest request, UUID clientId)

getClientOrders(UUID clientId)

getRestaurantOrders(UUID restaurantId)

confirmOrder(UUID orderId)

cancelOrder(UUID orderId)
```
## OrderController

REST API для работы с заказами:
```
Метод	URL	Роль	Описание
POST	/orders	CLIENT	Создать заказ
GET	/orders/my	CLIENT	Получить историю заказов клиента
POST	/orders/{id}/cancel	CLIENT	Отменить заказ (если PENDING)
GET	/orders/cafe?restaurantId={id}	CAFE_ADMIN	Получить заказы ресторана
PATCH	/orders/cafe/{id}/confirm	CAFE_ADMIN	Подтвердить заказ кафе
```
Пример запроса POST /orders
```
{
"restaurantId": "uuid-ресторана",
"items": [
{
"itemId": "uuid-блюда",
"quantity": 2
},
{
"itemId": "uuid-блюда",
"quantity": 1
}
],
"deliveryAddress": "ул. Ленина 45, кв. 12"
}
```
Особенности Order Module

* Snapshot блюд хранится как JSON для истории заказа

* Подтверждение заказа и отмена возможны только при статусе PENDING

* Администратор кафе видит только заказы своего ресторана

* Клиент видит только свои заказы

* Интеграция с Spring Security через @PreAuthorize
---

# 💳 Payment Module

Модуль payment отвечает за обработку платежей пользователей за заказы.

## Payment Entity

Сущность платежа:
```
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private BigDecimal amount;

    private String paymentMethod; // Например: CARD, CASH, ONLINE

    private String transactionId; // Идентификатор транзакции в платёжной системе

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```
Особенности:

* orderId — связь с заказом

* clientId — идентификатор клиента

* status — текущий статус платежа

* paymentMethod — способ оплаты

* transactionId — внешний идентификатор платежа

Метки времени для аудита (createdAt / updatedAt)

## PaymentStatus Enum
```
public enum PaymentStatus {
PENDING,    // Ожидает оплаты
COMPLETED,  // Оплата прошла успешно
FAILED,     // Оплата не удалась
REFUNDED    // Возврат средств
}
```
## PaymentRepository

Репозиторий для работы с платежами:
```
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByClientId(UUID clientId);

    List<Payment> findByOrderId(UUID orderId);
}
```
## PaymentService

Сервис для бизнес-логики:
```
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    public Payment createPayment(UUID orderId, String paymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setClientId(order.getClientId());
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public Payment updatePaymentStatus(UUID paymentId, PaymentStatus status, String transactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(status);
        payment.setTransactionId(transactionId);
        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public List<Payment> getClientPayments(UUID clientId) {
        return paymentRepository.findByClientId(clientId);
    }
}
```
## PaymentController

REST API для работы с платежами:
```
Метод	URL	Роль	Описание
POST	/payments	CLIENT	Создать новый платеж для заказа
PATCH	/payments/{id}/status	CLIENT/ADMIN	Обновить статус платежа (например, после успешной оплаты)
GET	/payments/my	CLIENT	Получить список платежей текущего пользователя
GET	/payments/order/{orderId}	ADMIN	Получить платежи по конкретному заказу
```
Пример запроса POST /payments:
```
{
"orderId": "uuid-заказа",
"paymentMethod": "CARD"
}
```
Пример запроса PATCH /payments/{id}/status:
```
{
"status": "COMPLETED",
"transactionId": "txn_123456789"
}
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
Swagger автоматически документирует:
* Auth API
* Catalog API
* DTO модели
* HTTP ответы 
* параметры запросов
---
# 📁 Структура проекта

```
food-delivery-backend
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.fooddelivery
│   │   │
│   │   │        ├── auth
│   │   │        │    ├── controller
│   │   │        │    ├── service
│   │   │        │    ├── dto
│   │   │        │    ├── entity
│   │   │        │    ├── repository
│   │   │        │    └── security
│   │   │        │
│   │   │        ├── catalog
│   │   │        │    ├── controller
│   │   │        │    ├── service
│   │   │        │    ├── dto
│   │   │        │    ├── entity
│   │   │        │    └── repository
│   │   │        │
│   │   │        ├── orders
│   │   │        │    ├── controller
│   │   │        │    ├── service
│   │   │        │    ├── entity
│   │   │        │    └── repository
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
