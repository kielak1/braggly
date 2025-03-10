# Braggly Backend

## Opis projektu

Projekt Braggly Backend to aplikacja serwerowa napisana w Spring Boot, która docelowo będzie wpsierać analizę spektrogramów rentgenowskic. Aktualnie obsługuje jedynie różne operacje związane z użytkownikami, takie jak logowanie, tworzenie użytkowników, usuwanie użytkowników oraz wyświetlanie informacji o zalogowanym użytkowniku.

## Dostępność

System produkcyjny jes dostępny pod adresem: https://bragglybackend.kielak.com

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

- Imię Nazwisko - [GitHub](https://github.com/kielak1)

## Licencja

.....