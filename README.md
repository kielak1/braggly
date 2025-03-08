# Braggly Backend

## Opis projektu

Projekt Braggly Backend to aplikacja serwerowa napisana w Spring Boot, która obsługuje różne operacje związane z użytkownikami, takie jak logowanie, tworzenie użytkowników, usuwanie użytkowników oraz wyświetlanie informacji o zalogowanym użytkowniku.

## Wymagania

- Java 17 lub nowsza
- Maven 3.6.3 lub nowszy
- Baza danych (np. MySQL, PostgreSQL)

## Konfiguracja

1. Sklonuj repozytorium:
   ```bash
   git clone https://github.com/twoje-repozytorium/braggly-backend.git
   cd braggly-backend
   ```

2. Skonfiguruj bazę danych w pliku `application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/braggly
   spring.datasource.username=root
   spring.datasource.password=haslo
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL5Dialect
   ```

3. Uruchom aplikację:
   ```bash
   mvn spring-boot:run
   ```

## Endpointy

### Autoryzacja

#### Logowanie

- **Endpoint:** `/api/auth/login`
- **Metoda:** `POST`
- **Opis:** Logowanie użytkownika i generowanie tokenu JWT.
- **Przykład żądania:**
  ```json
  {
    "username": "admin",
    "password": "admin"
  }
  ```
- **Przykład odpowiedzi:**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
  ```

### Admin

#### Tworzenie użytkownika

- **Endpoint:** `/api/admin/create-user`
- **Metoda:** `POST`
- **Opis:** Tworzenie nowego użytkownika (wymaga roli ADMIN).
- **Nagłówek:** `Authorization: Bearer <token>`
- **Przykład żądania:**
  ```json
  {
    "username": "newuser",
    "password": "password"
  }
  ```
- **Przykład odpowiedzi:**
  ```json
  {
    "message": "User created successfully."
  }
  ```

#### Usuwanie użytkownika

- **Endpoint:** `/api/admin/delete-user`
- **Metoda:** `DELETE`
- **Opis:** Usuwanie użytkownika (wymaga roli ADMIN).
- **Nagłówek:** `Authorization: Bearer <token>`
- **Przykład żądania:**
  ```http
  DELETE /api/admin/delete-user?username=newuser
  ```
- **Przykład odpowiedzi:**
  ```json
  {
    "message": "User deleted successfully."
  }
  ```

#### Lista użytkowników

- **Endpoint:** `/api/admin/list-user`
- **Metoda:** `GET`
- **Opis:** Pobieranie listy wszystkich użytkowników (wymaga roli ADMIN).
- **Nagłówek:** `Authorization: Bearer <token>`
- **Przykład odpowiedzi:**
  ```json
  [
    {
      "username": "admin",
      "role": "ADMIN"
    },
    {
      "username": "newuser",
      "role": "USER"
    }
  ]
  ```

### Informacje o zalogowanym użytkowniku

#### Who Am I

- **Endpoint:** `/api/whoami`
- **Metoda:** `GET`
- **Opis:** Pobieranie informacji o zalogowanym użytkowniku.
- **Nagłówek:** `Authorization: Bearer <token>`
- **Przykład odpowiedzi:**
  ```json
  {
    "username": "admin",
    "role": "ADMIN"
  }
  ```

### Hello

#### Hello World

- **Endpoint:** `/api/hello`
- **Metoda:** `GET`
- **Opis:** Prosty endpoint zwracający "Hello, world!".
- **Przykład odpowiedzi:**
  ```json
  {
    "message": "Hello, world!"
  }
  ```

## Autorzy

- Imię Nazwisko - [GitHub](https://github.com/twoje-konto)

## Licencja

Ten projekt jest licencjonowany na warunkach licencji MIT - zobacz plik [LICENSE](LICENSE) po więcej szczegółów.