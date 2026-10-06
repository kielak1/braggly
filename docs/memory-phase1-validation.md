# Faza 1 optymalizacji pamięci — raport przed deploymentem

Zmiany są wyłącznie lokalne. Railway, credentiale, produkcyjna baza, deployment, branch i HEAD pozostają bez zmian. Frontend nie był modyfikowany. Rekomendacja: wdrożenie backendu tej fazy po osobnym poleceniu użytkownika.

## Baseline i metoda

Baseline historyczny jest zapisany w [memory-phase1-baseline.md](memory-phase1-baseline.md): produkcja warm 0,95–0,98 GB, średnia 30 dni 0,89 GB, peak 1,426 GB; lokalnie warm około 565 MiB, wcześniejszy eksperymentalny profil 256 MiB około 400 MiB. Tej fazy nie wdrożono, więc nie ma jeszcze pomiarów produkcji po zmianach.

Do tabeli wykonano nowe porównywalne pomiary zachowanego JAR-a sprzed zmian i finalnego JAR-a. Java 21 Temurin, izolowany Podman/PostgreSQL, syntetyczne serwisy COD/CIF/B2/OpenAI i sztuczne credentiale. Ten sam `/proc` RSS, `jcmd`, NMT detail i zestaw requestów. Idle/warm: 75 sekund, próbki co 5 sekund. Peak workloadu: RSS co 200 ms, ta sama metoda w obu przebiegach. Po zadaniu: 30 sekund idle oraz osobny lokalny wymuszony GC z odczekaniem 5 sekund. Wymuszony GC nie jest traktowany jako naturalny steady-state ani ustawienie produkcyjne.

BEFORE: ustawienie 48 widocznych CPU odtwarza poprzednią metodę i obserwowany rozmiar pul produkcji. AFTER: `-XX:ActiveProcessorCount=2 -Xms32m -Xmx512m`, domyślny G1, Hikari 4/1, Tomcat 20/2. Komenda Nixpacks dodatkowo jawnie zachowuje ten sam G1 przez `-XX:+UseG1GC`. Nie ograniczano stacków, metaspace, code cache ani poziomu kompilacji JIT.

## Memory benchmark

Wszystkie wartości poniżej to MiB RSS procesu, nie limit heapu ani metryka rozliczeniowa kontenera Railway. Nowy pomiar BEFORE warm wyniósł 513 MiB, podczas gdy historyczny wynosił około 565 MiB; w tabeli użyto aktualnego porównywalnego przebiegu. Wyniki pojedynczych prób zależą od ergonomii G1/JIT i warunków hosta.

| Scenariusz | Before | After | Difference |
| --- | ---: | ---: | ---: |
| Startup RSS (HTTP ready) | 414.7 | 367.0 | -47.7 |
| Warm idle RSS | 513.4 | 414.0 | -99.4 |
| Zwykłe endpointy, potem idle | 475.0 | 398.3 | -76.8 |
| 1000 COD — peak RSS | 520.2 | 415.9 | -104.4 |
| 10000 COD — peak RSS | 574.3 | 428.0 | -146.3 |
| 100000 COD — peak RSS | 606.4 | 433.5 | -172.8 |
| 250000 COD — peak RSS | 643.3 | 425.5 | -217.9 |
| CIF: 100k atomów — peak RSS | 737.0 | 461.7 | -275.3 |

Warm RSS: redukcja 19.4% w bieżącym porównaniu. Peak 250k COD: redukcja 33.9%. Peak pojedynczego CIF: redukcja 37.4%. To nie jest jeszcze dramatyczna redukcja stałego baseline całego Spring Boota.

### Po imporcie i GC

| Rekordy | Peak RSS | RSS po 30 s idle | RSS po GC | Żywy heap po GC | Czas importu |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 1000 | 415.9 | 415.8 | 417.4 | 52.1 | 1.9 s |
| 10000 | 428.0 | 416.0 | 416.3 | 52.2 | 3.2 s |
| 100000 | 433.5 | 424.4 | 424.4 | 52.2 | 15.4 s |
| 250000 | 425.5 | 425.5 | 427.5 | 46.7 | 34.4 s |

Pamięć zatrzymywana przez import jest O(batch size), z batchem 500. Wzrost peak RSS wynosi około 17.7 MiB przy wzroście liczby rekordów 250 razy. Histogram w trakcie kolejnego importu po zapisaniu 54500 rekordów wykazał 500 żywych CodEntry i zero CodImportResult. Żywy heap po GC wynosi 46.7–52.2 MiB i nie rośnie wraz z liczbą rekordów. Po 250k RSS około 427.5 MiB jest blisko warm 414.0 MiB; nie oznacza to idealnie identycznego RSS, ponieważ pozostają rozgrzane klasy/JIT/native memory.

### CIF przy granicach bezpieczeństwa

187 rzeczywistych plików B2 sprawdzono wyłącznie odczytowo do doboru limitów: największy 2 243 835 bajtów, mediana 13 590, p95 590 424; największa liczba atomów 231, mediana 32, p95 77. Produkcyjne CIF-y nie były zapisywane ani używane przez lokalną aplikację.

Limity: 32 MiB, 100 000 atomów, dwie równoległe operacje. Dwa syntetyczne pliki po około 31,99 MiB i 100k atomów przeszły jednocześnie, HTTP 200/200. Peak RSS 734.4 MiB, po GC 474.6 MiB. `Xmx512m` ogranicza heap, nie RSS; cały proces może więc zajmować więcej niż 512 MiB. Przekroczenie bajtów lub atomów zwraca HTTP 413. Sprawdzenie plików tymczasowych po workloadzie: zero pozostałości. Parser ciftools nadal tworzy dokument w pamięci; usunięto własne kopie byte[]/String i buforowanie uploadu w heapie.

## Wątki i pule

| Scenariusz | Before | After |
| --- | ---: | ---: |
| Idle przed workloadem | 76 | 30 |
| Warm idle | 125 | 36 |
| Peak podczas 250k COD | 130 | 37 |

Sześć równoległych zgłoszeń: 3 × HTTP 200, 3 × HTTP 429; 1 RUNNING, 2 PENDING, 3 FAILED. Po wykonaniu: 3 COMPLETED, 3 FAILED. Jeden worker COD; deduplikacja 24 równoległych znormalizowanych zgłoszeń tworzy jedno zadanie. Histogram/pomiary nie wykazały zatrzymywania wszystkich wyników. W trakcie rzeczywistego lokalnego importu health/hello/credits/public-files odpowiadały HTTP 200 w około 12–14 ms.

Hikari max 4/min idle 1 potwierdzono przez metryki; działają startup, Hibernate, zwykłe requesty i importy. V7 zweryfikowano na lokalnym starym schemacie PostgreSQL: completed/progress zachowane, przerwane zadania FAILED. Główne testy integracyjne używają lokalnego schematu Hibernate, osobnej bazy i wyłączonych historycznych migracji V0–V6; nowa migracja była sprawdzona oddzielnie. Nie zmieniano historycznych migracji ani produkcyjnej bazy.

## Testy

| Kontrola | Wynik |
| --- | --- |
| Kompilacja i bootJar Java 21 | PASS |
| Pełny zestaw: 40 testów, 0 błędów, 0 pominięć | PASS |
| Atomowa deduplikacja, kolejka 1+2, HTTP 429 | PASS |
| IOException/timeout/RuntimeException/DB failure → FAILED | PASS |
| Osobne transakcje batchy, rollback uszkodzonego batcha | PASS |
| Odzyskiwanie terminalnego statusu po awarii DB | PASS |
| Zamknięcie strumieni i sprzątanie tymczasowego CIF przy błędzie | PASS |
| Limity CIF, współbieżność i koniec serializacji | PASS |
| 1k/10k/100k/250k COD + CIF + granice z Xmx512m | PASS |
| GC: najdłuższa normalna pauza 38.3 ms; bez OOM/StackOverflow | PASS |

Testowy proces korzystał wyłącznie z syntetycznych credentiali i lokalnych mocków; nie wykonywano płatnych requestów OpenAI. Artefakty i logi: `/tmp/braggly-memory-phase1-20261006`; pliki `BEFORE/phase-result.json`, `AFTER/phase-result.json`, logi GC, NMT i histogram. Różne dodatkowe testy obciążenia po porównaniu bazowym są oznaczone osobno.

## Zmienione pliki

Ścieżki względem repozytorium backendu `/home/tadek/projekty/braggly/backend`.

| Plik | Cel |
| --- | --- |
| `backend/nixpacks.toml` | CPU2, Xms32m, przetestowany Xmx512m, G1. |
| `backend/src/main/resources/application.properties` | Pule Hikari/Tomcat i konfigurowalne limity COD/CIF. |
| `backend/src/main/java/com/example/backend/service/CodImportService.java` | Batch 500, flush/clear, brak results, worker 1 + kolejka 2, atomowa deduplikacja, terminalne statusy i ograniczone odzyskiwanie po awarii DB. |
| `backend/src/main/java/com/example/backend/service/CodCsvSource.java` | Download CSV na dysk, zamknięcie zasobów, timeouty, cleanup błędu. |
| `backend/src/main/java/com/example/backend/model/CodQuery.java` | Trwały status z zachowaniem completed. |
| `backend/src/main/java/com/example/backend/model/CodQueryStatus.java` | PENDING/RUNNING/COMPLETED/FAILED. |
| `backend/src/main/java/com/example/backend/repository/CodQueryRepository.java` | Odczyt terminalnych/aktywnych zadań i przerwane zadania → FAILED. |
| `backend/src/main/java/com/example/backend/init/StartupCleaner.java` | Zachowanie przerwanych zadań jako FAILED zamiast usuwania. |
| `backend/src/main/java/com/example/backend/dto/CodQueryStatusResponse.java` | Jawny status przy zachowaniu dotychczasowych pól. |
| `backend/src/main/java/com/example/backend/controller/CodController.java` | Jawne ponowienie retry=true, bez automatycznego restartowania FAILED przez polling. |
| `backend/src/main/java/com/example/backend/controller/CodExceptionHandler.java` | Kontrolowane 429/413 bez dodatkowego dispatchu /error. |
| `backend/src/main/resources/db/migration/V7__cod_query_terminal_status.sql` | Dodanie i backfill statusów, kontrola dozwolonych stanów. |
| `backend/src/main/java/com/example/backend/service/CloudStorageService.java` | Upload z pliku zamiast byte[], zamknięcie multipart stream i klienta SDK. |
| `backend/src/main/java/com/example/backend/service/CifInfoService.java` | Limitowane strumienie, pojedynczy plik tymczasowy, brak ponownego B2 GET i własnych pełnych kopii danych. |
| `backend/src/main/java/com/example/backend/service/CifLimits.java` | Konfigurowalne i walidowane limity. |
| `backend/src/main/java/com/example/backend/service/LimitedInputStream.java` | Limit faktycznych bajtów; abort nadmiarowego S3 streamu. |
| `backend/src/main/java/com/example/backend/service/CifConcurrencyFilter.java` | Limit CIF aż do zakończenia serializacji HTTP. |
| `backend/src/main/java/com/example/backend/service/XrdFileService.java` | Zamknięcie strumienia nagłówka pliku. |
| `backend/src/test/java/com/example/backend/StartupSafetyIntegrationTest.java` | Utrzymanie izolowanego testu startu z nowymi zależnościami transakcyjnymi. |
| `backend/src/test/java/com/example/backend/init/StartupCleanerTest.java` | Regresja statusów przerwanych importów. |
| `backend/src/test/java/com/example/backend/controller/CodExceptionHandlerTest.java` | Regresja 429/413 i statusu FAILED. |
| `backend/src/test/java/com/example/backend/service/CodImportServiceTest.java` | Concurrency, dedup, odrzucenie, błędy, shutdown, bounded batches i recovery DB. |
| `backend/src/test/java/com/example/backend/service/CodImportJpaIntegrationTest.java` | Rzeczywiste inserty/update i rollback osobnych batchy w PostgreSQL. |
| `backend/src/test/java/com/example/backend/service/CifInfoServiceTest.java` | Limity, stream cleanup, równoległość i usunięcie temporary files po błędach. |
| `backend/src/test/java/com/example/backend/service/CloudStorageServiceTest.java` | Zamknięcie multipart stream i upload z dysku ze znaną długością. |
| `backend/src/test/java/com/example/backend/service/LimitedInputStreamTest.java` | Limit bajtów i zabezpieczenie skip. |
| `backend/src/test/java/com/example/backend/service/CifConcurrencyFilterTest.java` | Limit obejmuje serializację, także błąd klienta. |
| `backend/src/test/java/com/example/backend/service/XrdFileServiceTest.java` | Regresja zamknięcia wejścia nagłówka. |
| `docs/memory-phase1-baseline.md` | Zapis baseline i danych użytych do doboru limitów. |
| `docs/memory-phase1-validation.md` | Ten raport i dokumentacja walidacji. |

## Odpowiedzi i rekomendacja

1. COD nie zatrzymuje danych O(total rows): tak, brak listy wyników, kontekst najwyżej jednego batcha; peak 415.9–433.5 MiB dla 1k–250k.
2. Powrót po imporcie: tak, w okolice steady-state; po 250k około 427.5 MiB RSS i 46.7 MiB żywego heapu wobec warm 414.0 MiB.
3. Lokalny warm RSS: około 414.0 MiB w finalnym przebiegu. W poprzednich przebiegach weryfikacyjnych tej fazy było około 445–473 MiB; ergonomia G1/JIT powoduje różnice i wynik nie jest gwarantowanym minimum produkcji.
4. Xmx512m: PASS dla pełnego workloadu, w tym dwóch CIF przy prawie maksymalnym rozmiarze i atom count.
5. Deployment: rekomendowany dla usunięcia P0 i bezpiecznych P1, po osobnym poleceniu. Lokalny spadek idle 19.4%, peak 250k 33.9%; nie deklarujemy jeszcze docelowej oszczędności Railway.

Ograniczenia: deduplikacja i jeden worker koordynują pojedynczą instancję (obecny Railway ma jedną replikę). Przy całkowitej awarii DB nie da się od razu zapisać FAILED; najwyżej trzy terminalne rekordy pozostają w pamięci, nowe zadania są blokowane, jeden ograniczony retry zapisuje status po powrocie DB, a startup oznacza porzucone zadania FAILED.

Aktualny frontend pobiera CIF sekwencyjnie, ale nie obsługuje jeszcze jawnie FAILED/retry=true ani ponowienia po 429. W razie błędu może dalej wykonywać polling; frontend pozostawiono bez zmian w tej fazie backendu. Prawidłowe dane i happy-path są zachowane. To ograniczenie UX należy uwzględnić przed wdrożeniem, nie zmienia ono pomiarów pamięci.

Końcowy smoke test dokładnej komendy Nixpacks: G1 i heap max 512 MiB, health HTTP 200/UP, 413 dla przekroczenia atomów i dla nadmiarowego cached S3 response; zero plików tymczasowych po obu błędach. Sprawdzono także obsługę UncheckedIOException z ciftools. Produkcyjny deployment pozostaje ten sam, SUCCESS, bez zmian konfiguracji.

Kontrola bezpieczeństwa: sprawdzono 760 plików kodu/artefaktów/logów oraz diff i klasy aplikacji w JAR-ach; nie znaleziono rzeczywistych credentiali. Git diff --check: PASS. Repo backendu ma 30 lokalnie zmienionych/dodanych plików; HEAD 3487fa2f885c8adebc7f45288c28379b4a0d6c0a pozostaje bez zmian. Repo frontendu: CLEAN. Testowe kontenery, baza i wygenerowane materiały TLS zostały usunięte; artefakty pomiarowe zachowane.
