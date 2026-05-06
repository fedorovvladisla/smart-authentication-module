<div align="center">

# Smart Authentication Module


**Модуль интеллектуальной аутентификации для Spring Boot приложений**

[![Java](https://img.shields.io/badge/Java-17-orange.svg?logo=openjdk&logoColor=white)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.1.5-brightgreen.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg?logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-24.0-blue.svg?logo=docker&logoColor=white)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-1.28-blue.svg?logo=kubernetes&logoColor=white)](https://kubernetes.io/)
[![HashiCorp Vault](https://img.shields.io/badge/Vault-2.0-black.svg?logo=vault&logoColor=white)](https://www.vaultproject.io/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3.1-green.svg?logo=thymeleaf&logoColor=white)](https://www.thymeleaf.org/)
[![WebAuthn](https://img.shields.io/badge/WebAuthn-FIDO2-purple.svg?logo=yubico&logoColor=white)](https://fidoalliance.org/)
[![JJWT](https://img.shields.io/badge/JJWT-0.12.3-blue.svg?logo=jsonwebtokens&logoColor=white)](https://github.com/jwtk/jjwt)
[![JUnit](https://img.shields.io/badge/JUnit-5-green.svg?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Mockito](https://img.shields.io/badge/Mockito-5.3-green.svg?logo=mockito&logoColor=white)](https://site.mockito.org/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-1.19-blue.svg?logo=docker&logoColor=white)](https://testcontainers.com/)
[![Tests](https://img.shields.io/badge/tests-21%20passed-success.svg)]()

</div>

---

##  О проекте

`smart-authentication-module` — это готовый к внедрению программный модуль, разработанный в рамках дипломного проекта. Он заменяет уязвимую парольную аутентификацию на современные беспарольные методы, включая **Passkey (WebAuthn/FIDO2)**, **распознавание лиц** и **двухфакторную аутентификацию**. Модуль легко интегрируется в существующие Java-приложения или разворачивается как самостоятельный сервис.

---

##  Ключевые возможности

###  Passkey / WebAuthn (FIDO2)
- **Беспарольный вход** с использованием биометрии устройства (Touch ID, Face ID, Windows Hello) или аппаратных ключей (YubiKey).
- Полное соответствие стандарту **W3C WebAuthn**.
- Криптографическая защита от фишинга: закрытый ключ никогда не покидает устройство пользователя.

###  Распознавание лиц (Face Recognition)
- Интеграция с open-source системой **CompreFace**.
- **Проверка живости (Liveness Detection)** через Pose Plugin — защита от подмены фотографией.
- **Динамический порог срабатывания**: система автоматически ужесточает требования при подозрительной активности (время суток, количество неудачных попыток).
- Защита от дублирования: одно лицо не может быть зарегистрировано на несколько аккаунтов.

### Двухфакторная аутентификация (2FA / MFA)
- **Адаптивная логика**: если уверенность распознавания лица ниже высокого порога, система запрашивает подтверждение через Passkey.
- Метод аутентификации **«Face + Passkey»** автоматически применяется при недостаточной уверенности.

### Безопасность на уровне Enterprise
- **HashiCorp Vault** для централизованного хранения секретов (JWT-ключи, пароли БД, API-ключи).
- **JWT-аутентификация** с поддержкой **refresh-токенов** (ротация, отзыв).
- **BCrypt** для хеширования паролей администраторов.
- Полный аудит всех попыток входа (журнал событий с IP, геолокацией, методом и результатом).

###  Административная панель (Dashboard)
- **Просмотр логов**: все попытки аутентификации с фильтрацией по методу, результату, времени.
- **Управление пользователями**: блокировка, разблокировка, удаление (с очисткой всех связанных данных).
- **Настройка порогов безопасности**: изменение базового порога, штрафа за попытки, ночного коэффициента.

---

##  Технологический стек

| Категория | Технологии |
|-----------|------------|
| **Язык** | Java 17 |
| **Фреймворк** | Spring Boot 3.1.5 (Web, Security, Data JPA, Actuator) |
| **База данных** | PostgreSQL 15 |
| **Кэш** | Redis 7 |
| **WebAuthn** | Yubico WebAuthn Server Core 2.5.0 |
| **Face Recognition** | CompreFace 1.2.0 (REST API) |
| **Секреты** | HashiCorp Vault |
| **JWT** | JWT 0.12.3 |
| **Шаблоны** | Thymeleaf |
| **Контейнеризация** | Docker, Docker Compose |
| **Оркестрация** | Kubernetes (манифесты) |
| **Тестирование** | JUnit 5, Mockito, Testcontainers |

---

##  Быстрый старт

### Предварительные требования
- **JDK 17** или выше
- **Maven 3.6+** (опционально, для сборки без Docker)
- **Docker** и **Docker Compose**

### 1. Клонирование репозитория
```bash
    git clone https://github.com/fedorovvladisla/smart-authentication-module.git
    cd smart-authentication-module
```
### 2. Настройка переменных окружения

Создайте файл .env в корне проекта:

```env
    DB_PASSWORD=secret
    JWT_SECRET=ваш_секретный_ключ_для_JWT
    COMPREFACE_API_KEY=ваш_api_ключ_от_CompreFace
```

### 3. Запуск CompreFace (для Face ID)
```bash
   cd CompreFace
   docker-compose up -d
   cd ..
```

### 4. Запуск Vault (для хранения секретов)
```bash
   cd vault
   docker-compose up -d
   cd ..
```
### 5. Запуск приложения
```bash
   docker-compose up -d --build
```
После запуска приложение будет доступно по адресу: http://localhost:8080

Примечание: при первом запуске автоматически создаётся администратор с логином admin и паролем admin123.

# Тестирование
Модуль покрыт модульными и интеграционными тестами:

```bash
    # Запуск всех тестов
    mvn test
    # Запуск конкретного тестового класса
    mvn test -Dtest=JwtServiceTest
    mvn test -Dtest=DynamicThresholdServiceTest
    mvn test -Dtest=PasskeyControllerTest
    mvn test -Dtest=AdminControllerTest
    mvn test -Dtest=TokenControllerTest
    mvn test -Dtest=ConsentControllerTest 
```

Покрытие тестами:

- JwtService — генерация, валидация, отзыв токенов

- DynamicThresholdService — расчёт порога с учётом времени и попыток

- PasskeyController — регистрация, вход, блокировка, MFA

- AdminController — логи, пользователи, настройки, удаление

- TokenController — обновление токенов

- ConsentController — запись и проверка согласий

# Структура проекта
```text
smart-authentication-module/
├── src/main/java/com/vkr/auth/
│   ├── cache/              # Кэш неудачных попыток (Redis)
│   ├── config/             # Конфигурации (Security, AppConfig, WebAuthn, Vault)
│   ├── controller/         # REST и Web контроллеры
│   ├── dto/                # DTO-объекты
│   ├── model/              # JPA-сущности (User, AuthLog, WebAuthnCredential, etc.)
│   ├── repository/         # Spring Data JPA репозитории
│   ├── security/           # JWT-фильтр
│   └── service/            # Бизнес-логика (WebAuthn, Face, JWT, Threshold)
├── src/main/resources/
│   ├── static/             # Статические ресурсы (index.html)
│   ├── templates/          # Thymeleaf-шаблоны админ-панели
│   └── application.yml     # Основной конфигурационный файл
├── CompreFace/             # Docker Compose для CompreFace
├── vault/                  # Docker Compose для HashiCorp Vault
├── k8s/                    # Манифесты Kubernetes
├── docker-compose.yml      # Основной Docker Compose
├── Dockerfile              # Инструкции для сборки Docker-образа
└── pom.xml                 # Maven-конфигурация
```

# Будущие улучшения 
- CI/CD: автоматический прогон тестов и сборка образов через GitHub Actions

- Мониторинг: интеграция Prometheus + Grafana для отслеживания метрик

- Улучшенная проверка живости: интеграция более продвинутых методов (3D-камеры, анализ моргания)

- Мобильное приложение-аутентификатор: для удобной работы с Passkey на смартфонах

- Федеративная аутентификация: поддержка OAuth2/OIDC (Google, GitHub, Yandex)

- Шифрование биометрических данных: использование Vault Transit для защиты лиц в CompreFace

