# Lokalne skanowanie kodu za pomocą SonarQube

Niniejsza dokumentacja opisuje sposób uruchomienia serwera **SonarQube** lokalnie w środowisku Docker oraz przeprowadzania pełnej statycznej analizy kodu dla części backendowej (Java/Maven) i frontendowej (React/TypeScript).

Dzięki temu każdy członek zespołu może bez instalowania lokalnych narzędzi (Maven, Node, Sonar Scanner) dokonać pełnej weryfikacji jakości kodu na swoim komputerze.

---

## 🛠️ Wymagania wstępne

Przed rozpoczęciem upewnij się, że masz zainstalowane i uruchomione:
1. **Docker Desktop** (lub Docker Engine w systemie Linux/macOS).
2. **Git Bash** (zalecany terminal dla użytkowników systemu Windows).

---

## 🚀 Krok 1: Uruchomienie serwera SonarQube

Wszystkie potrzebne serwisy są już zdefiniowane w pliku `docker-compose.yml` w katalogu głównym projektu.

1. Otwórz terminal w głównym katalogu projektu i uruchom bazę danych oraz serwer SonarQube:
   ```bash
   docker compose up -d postgres sonarqube
   ```
2. Poczekaj chwilę, aż serwer SonarQube w pełni się uruchomi (może to zająć od 1 do 2 minut przy pierwszym uruchomieniu).
3. Wejdź w przeglądarce pod adres:
   👉 **[http://localhost:9000](http://localhost:9000)**
4. Zaloguj się domyślnymi danymi:
   * **Login:** `admin`
   * **Hasło:** `admin`
5. **Wymagana zmiana hasła:** Przy pierwszym logowaniu SonarQube wymusi ustawienie nowego hasła. Zapamiętaj je lub zapisz.

---

## 🔑 Krok 2: Generowanie tokenu dostępu

Token dostępu (Analysis Token) jest potrzebny, aby skanery mogły uwierzytelnić się w Twoim lokalnym serwerze SonarQube.

1. Po zalogowaniu kliknij ikonę swojego profilu w prawym górnym rogu.
2. Wybierz **My Account**, a następnie zakładkę **Security**.
3. W sekcji **Generate Tokens**:
   * Podaj nazwę tokenu (np. `cookmate-token`).
   * Jako typ wybierz **User Token** lub **Global Analysis Token**.
   * Kliknij **Generate**.
4. ⚠️ **Skopiuj wygenerowany token!** Pokazuje się on tylko raz. Będzie Ci potrzebny w kolejnych krokach jako `TWÓJ_TOKEN`.

---

## ☕ Krok 3: Skanowanie kodu Backendu (Java)

Analiza backendu uruchamia testy jednostkowe i integracyjne, generuje raport pokrycia kodu (JaCoCo), a następnie wysyła te dane do SonarQube.

Uruchom poniższe polecenie w głównym katalogu projektu w terminalu **Git Bash**:

```bash
MSYS_NO_PATHCONV=1 docker run --rm -it \
  -v "$(pwd):/app" \
  -w /app \
  --network cookmate_cookmate-net \
  maven:3.9-eclipse-temurin-25 \
  mvn clean verify sonar:sonar \
  "-Dsonar.host.url=http://cookmate-sonarqube:9000" \
  "-Dsonar.login=TWÓJ_TOKEN"
```

*Zastąp `TWÓJ_TOKEN` wartością skopiowaną w Kroku 2.*

> [!NOTE]
> Zmienna `MSYS_NO_PATHCONV=1` zapobiega problemom z tłumaczeniem ścieżek wolumenów w konsoli Git Bash w systemie Windows. Jest ona absolutnie wymagana.

---

## ⚛️ Krok 4: Skanowanie kodu Frontendu (TypeScript)

Skanowanie frontendowe składa się z dwóch etapów: wygenerowania raportu z lintera (ESLint) oraz właściwej analizy Sonar Scannera.

### A. Generowanie raportu z lintera
Uruchom w głównym katalogu projektu:

```bash
MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$(pwd)/frontend:/app" \
  -w /app \
  node:20-alpine \
  sh -c "npm install && npm run lint:sonar"
```

*Komenda ta zainstaluje zależności i utworzy plik `frontend/eslint-report.json` (który jest ignorowany przez Gita).*

### B. Uruchomienie skanera SonarQube
Uruchom w głównym katalogu projektu:

```bash
MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$(pwd)/frontend:/usr/src" \
  --network cookmate_cookmate-net \
  sonarsource/sonar-scanner-cli \
  -Dsonar.host.url=http://cookmate-sonarqube:9000 \
  -Dsonar.login=TWÓJ_TOKEN
```

---

## 📊 Krok 5: Podgląd wyników i konfiguracja Quality Gate

Po pomyślnym zakończeniu obu skanowań wejdź na **[http://localhost:9000](http://localhost:9000)**. Zobaczysz tam dwa osobne projekty:
1. **CookMate Parent** (Backend) – zawiera pokrycie kodu testami z JaCoCo, błędy Java oraz metryki długu technologicznego.
2. **CookMate Frontend** – zawiera analizę TypeScriptu oraz zaczytane reguły z ESLint.

### Wdrożenie własnego Quality Gate (Zalecane)
Domyślnie SonarQube stosuje profil *Sonar way*. Aby stworzyć reguły blokujące kod o niskiej jakości dla zespołu:
1. W panelu górnym kliknij **Quality Gates** -> **Create**.
2. Nazwij go np. `CookMate Gate`.
3. Kliknij **Add Condition** i ustaw pożądane progi akceptacji, na przykład:
   * **Bugs** (Błędy) > `0` (na Nowym Kodzie)
   * **Coverage** (Pokrycie kodu) < `50%` (na Nowym Kodzie)
   * **Duplicated Lines (%)** (Duplikaty) > `5%`
4. Na dole strony w sekcji **Projects** przypisz ten Quality Gate do projektów `cookmate-parent` oraz `cookmate-frontend`.

---

## 🔄 Czyszczenie środowiska (Gdy chcesz zacząć od nowa)

Jeśli chcesz całkowicie usunąć dane SonarQube i jego bazę danych, aby zacząć konfigurację od zera, wykonaj:
```bash
docker compose down -v
```
*(Flaga `-v` usunie wolumeny Dockera powiązane z bazą danych i plikami SonarQube).*
