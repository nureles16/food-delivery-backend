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

# 📁 Структура проекта

```
food-delivery-backend
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.fooddelivery
│   │   │        ├── auth
│   │   │        ├── catalog
│   │   │        ├── orders
│   │   │        └── config
│   │   │
│   │   └── resources
│   │        ├── application.properties
│
├── src/test
│
├── pom.xml
└── README.md
```

---

# 📌 Roadmap

### Week 1

* Auth module
* User management
* PostgreSQL setup
* 
### Week 2

* Cafe management
* Menu CRUD
* Category management

### Week 3

* Orders
* Payments
* Notifications

---

# 👨‍💻 Автор

**Nureles Almazbekov**

Java Backend Developer
Spring Boot | PostgreSQL | Angular
