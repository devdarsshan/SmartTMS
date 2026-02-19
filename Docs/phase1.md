
---

# 🔐 PHASE 1 – Authentication & Security Foundation

---

## 🎯 Phase 1 Goal

By the end of this phase:

* Users can register & login
* JWT tokens are generated
* API Gateway validates JWT
* Role-based access works
* Only authenticated users can access microservices

No business logic yet — only **security layer**.

---

# 🧱 Architecture for Phase 1

```
Client
   ↓
API Gateway (JWT validation)
   ↓
Auth Service (login/register)
   ↓
Other Services (protected endpoints)
```

---

# 🛠 STEP 1: Auth Service Implementation

---

## 1️⃣ Dependencies (auth-service)

Add in `auth-service/pom.xml`:

```xml
<dependencies>

    <!-- Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>

    <!-- JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- MySQL -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>

</dependencies>
```

---

# 🧩 STEP 2: Database Design (auth-service)

Create table: `usr_users`

```sql
CREATE TABLE usr_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);
```

Roles:

* ADMIN
* DISPATCHER
* DRIVER

---

# 🧠 STEP 3: Entity Layer

```java
@Entity
@Table(name = "usr_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String role;
}
```

---

# 📦 STEP 4: Repository

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

---

# 🔑 STEP 5: JWT Utility Class

Create `JwtUtil.java`

Responsibilities:

* Generate token
* Validate token
* Extract username
* Extract role

Token should contain:

* username
* role
* expiration

Example claims:

```json
{
  "sub": "john",
  "role": "DRIVER",
  "exp": 1700000000
}
```

Expiration: 24 hours.

---

# 🔐 STEP 6: Password Encoding

Configure BCrypt:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Never store plain passwords.

---

# 🧾 STEP 7: Auth Controller

Create endpoints:

### Register

```
POST /auth/register
```

### Login

```
POST /auth/login
```

Login flow:

1. Fetch user
2. Match password
3. Generate JWT
4. Return token

Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

# 🔐 STEP 8: Security Configuration (auth-service)

Permit:

* `/auth/login`
* `/auth/register`

Protect everything else.

Use SecurityFilterChain configuration.

---

# 🚪 STEP 9: API Gateway JWT Validation

Now move to `api-gateway`.

---

## Add dependency (if not added)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

---

## Create Custom JWT Filter

Responsibilities:

* Read Authorization header
* Extract token
* Validate token
* Extract username & role
* Add to request headers (optional)

---

## Security Config in Gateway

* All routes require authentication
* Except:

    * `/auth/login`
    * `/auth/register`

---

# 🔒 STEP 10: Protect Other Microservices

For now, simplest approach:

Each service:

* Trust Gateway
* Do NOT implement security yet

Later (Phase 2+), you can add downstream security.

---

# 🧪 STEP 11: Testing Flow

1. Start Eureka
2. Start Gateway
3. Start Auth service
4. Register user
5. Login
6. Get token
7. Call protected endpoint with:

   ```
   Authorization: Bearer <token>
   ```

If token invalid → 401

---

# ✅ Phase 1 Completion Checklist

Before moving ahead:

* Register works
* Login works
* JWT generated
* JWT validated at Gateway
* Unauthorized access blocked
* Role claim present in token

---

# 🎯 Interview Explanation

You should confidently say:

> “Authentication is centralized in Auth Service, JWT tokens are issued upon login, and API Gateway validates tokens for all downstream requests, enforcing stateless security.”

