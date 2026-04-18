# Smart Authentication Module

Модуль интеллектуальной аутентификации для Spring Boot приложений, разработанный в рамках дипломного проекта. Предоставляет современные, безопасные методы входа без использования паролей.

## Возможности

Модуль предлагает три передовых метода аутентификации:

*   **Passkeys / WebAuthn** — Беспарольный вход с использованием биометрии устройства (Touch ID, Face ID, Windows Hello) или аппаратных ключей безопасности (YubiKey).
*   **Face Recognition** — Аутентификация по лицу с динамическим порогом срабатывания через интеграцию с CompreFace.
*   **Admin Dashboard** — Веб-интерфейс для администратора с дашбордом и журналом событий аутентификации.

##  Технологический стек

### Backend
*   **Java 17**
*   **Spring Boot 3.1.5** (Web, Security, Data JPA, Actuator)
*   **PostgreSQL 15** — основная база данных
*   **Redis 7** — кэш для учета неудачных попыток входа
*   **Yubico WebAuthn Server Core** — реализация стандарта WebAuthn / FIDO2
*   **Thymeleaf** — серверный рендеринг админ-панели

### Инфраструктура
*   **Docker / Docker Compose** — контейнеризация приложения
*   **Kubernetes / K8s** — манифесты для развертывания в кластере

## Системные требования

*   **JDK 17** или выше
*   **Maven 3.6+**
*   **Docker и Docker Compose** (опционально, для контейнеризации)
*   **PostgreSQL 15+** (если запуск без Docker)
*   **Redis 7+** (если запуск без Docker)

## Быстрый старт

### 1. Клонирование репозитория

```bash
git clone https://github.com/fedorovvladisla/smart-authentication-module.git
cd smart-authentication-module
```

### 2. Настройка переменных окружения
   Создайте в корне проекта файл .env (он уже добавлен в .gitignore) и заполните его по примеру из .env.example:

```env
DB_PASSWORD=ваш_пароль_к_БД
JWT_SECRET=ваш_секретный_ключ_для_JWT
COMPREFACE_API_KEY=ваш_api_ключ_от_CompreFace
```
## Запуск с помощью Docker Compose
```bash
# Собрать образ и запустить все сервисы (приложение, PostgreSQL, Redis)
docker-compose up -d

# Проверить статус
docker-compose ps
```
## Структура проекта
```
src/
├── main/
│   ├── java/com/vkr/auth/
│   │   ├── config/          # Конфигурации Spring (Security, AppConfig)
│   │   ├── controller/      # REST и Web контроллеры
│   │   ├── dto/             # Объекты передачи данных
│   │   ├── model/           # JPA сущности
│   │   ├── repository/      # Spring Data JPA репозитории
│   │   ├── service/         # Бизнес-логика (WebAuthn, Face, User)
│   │   └── cache/           # Работа с кэшем неудачных попыток
│   └── resources/
│       ├── templates/       # HTML шаблоны админ-панели
│       └── application.yml  # Основной конфигурационный файл
├── k8s/                     # Манифесты Kubernetes
├── docker-compose.yml       # Конфигурация Docker Compose
├── Dockerfile               # Инструкции для сборки Docker образа
└── pom.xml                  # Maven конфигурация
```

## Тестирование
Будет сделано в будущем