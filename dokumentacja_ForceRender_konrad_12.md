# Dokumentacja projektu ForceRender

**Autor:** Konrad  
**Projekt:** ForceRender  
**Wersja:** 1.1.0  
**Data:** czerwiec 2026

---

## 1. Nazwa i opis projektu

ForceRender to mod kliencki do gry Minecraft, napisany w Javie z wykorzystaniem frameworka Fabric. Rozwiązuje konkretny problem wizualny, który pojawia się przy używaniu niestandardowych paczek zasobów zawierających ozdobne meble i dekoracje (np. ItemsAdder).

**Problem, który rozwiązuje**

Minecraft domyślnie stosuje technikę zwaną frustum culling, czyli pomijanie renderowania obiektów niewidocznych z perspektywy gracza. Decyzja o tym, czy obiekt jest widoczny, opiera się na jego tzw. bounding box, czyli prostokątnym obszarze zajmowanym przez dany byt w przestrzeni gry. Stojaki na zbroję (armor stands) i ramki na obrazy (item frames) mają bardzo małe bounding boxy, bo w oryginalnej grze są to niewielkie przedmioty. Problem pojawia się, gdy paczki zasobów przyczepiają do tych bytów duże, niestandardowe modele mebli lub dekoracji. Silnik gry wiąże decyzję o renderowaniu z małym bounding boxem, więc model znika z ekranu, gdy tylko jego punkt zaczepienia opuści pole widzenia, mimo że sam model mebla jest nadal widoczny.

ForceRender wymusza renderowanie tych bytów w konfigurowalnym promieniu (domyślnie 32 bloki), całkowicie omijając sprawdzanie frustum culling dla wskazanych typów bytów. Poza ustawionym zasięgiem silnik gry działa normalnie, dzięki czemu wpływ na wydajność jest minimalny.

---

## 2. Co zrobiłem

### System wstrzykiwania kodu (Mixin)

Sercem moda jest klasa `ForceRenderMixin`, która używa biblioteki Mixin do wstrzyknięcia kodu bezpośrednio w metodę `shouldRender()` klasy `EntityRenderer` w silniku Minecrafta. Wstrzyknięcie następuje na samym początku metody (`@At("HEAD")`), co pozwala wcześnie przerwać jej wykonanie i zwrócić wynik `true`, zanim oryginalna logika frustum culling zostanie uruchomiona. Kod sprawdza typ bytu (stojak na zbroję lub ramka na obraz) oraz odległość od kamery gracza i na tej podstawie decyduje, czy ominąć standardowe sprawdzenie.

### System konfiguracji

Stworzyłem klasę `ForceRenderConfig`, która zarządza zapisywaniem i odczytywaniem ustawień moda z pliku `forcerender.properties` w katalogu konfiguracyjnym Minecrafta. Plik przechowuje dwa parametry: flagę włączenia moda oraz zasięg renderowania w blokach. Klasa zawiera logikę ograniczającą zasięg do zakresu od 1 do 256 bloków oraz obsługuje sytuacje, gdy plik konfiguracyjny nie istnieje lub jest uszkodzony, spokojnie wracając do wartości domyślnych.

### Interfejs graficzny ustawień

Klasa `ForceRenderConfigScreen` implementuje ekran ustawień dostępny z poziomu gry przez Mod Menu. Ekran zawiera przycisk przełączający mod między stanem włączonym a wyłączonym (z kolorowym podpisem: zielony dla ON, czerwony dla OFF) oraz suwak do ustawiania zasięgu renderowania w zakresie 1–256 bloków. Suwak zaimplementowałem jako wewnętrzną klasę `RangeSlider`, która przelicza ciągłą wartość pozycji (0.0–1.0) na dyskretną liczbę bloków.

### Integracja z Mod Menu

Klasa `ModMenuIntegration` implementuje interfejs `ModMenuApi`, co pozwala Mod Menu automatycznie odnaleźć ekran konfiguracyjny moda i udostępnić go użytkownikowi jako ikonkę koła zębatego na liście modów.

### Pipeline CI/CD

Skonfigurowałem workflow GitHub Actions, który automatycznie buduje mod przy każdym pushu i pull requeście. Zbudowany plik `.jar` jest udostępniany jako artefakt do pobrania bezpośrednio z zakładki Actions.

---

## 3. Technologie i narzędzia

| Technologia | Zastosowanie |
|---|---|
| Java 21 | Język implementacji całego moda |
| Fabric Loader | Framework modów klienckich dla Minecrafta 1.21.1+ |
| Fabric API | Zestaw API do interakcji z silnikiem gry |
| Mixin (SpongePowered) | Biblioteka do wstrzykiwania kodu w klasy silnika Minecrafta w czasie wykonania |
| Gradle 8.8 + Loom | System budowania projektu; Loom obsługuje mapowania kodu Minecrafta |
| Yarn Mappings | Zestaw nazw dla klas i metod Minecrafta (deobfuskacja) |
| Mod Menu | Opcjonalna integracja umożliwiająca dostęp do ekranu ustawień z menu gry |
| GitHub Actions | Automatyczny build i udostępnianie artefaktów |
| SLF4J | Logowanie stanu moda do konsoli Minecrafta |

---

## 4. Ciekawe rzeczy techniczne

### 4.1 Wstrzykiwanie kodu w skompilowany silnik gry

Nie możliwości zmieniania kodu Minecrafta bezpośrednio, ponieważ jest on skompilowany i zaciemniony. Biblioteka Mixin rozwiązuje ten problem, wstrzykując kod w bytecode klas Minecrafta w czasie ładowania gry, zanim zostaną one uruchomione. W pliku `forcerender.mixins.json` deklaruję, które klasy mają zostać zmodyfikowane, a adnotacje takie jak `@Inject` i `@At("HEAD")` precyzyjnie wskazują miejsce wstrzyknięcia. Parametr `cancellable = true` pozwala przerwać wykonanie oryginalnej metody i podać własny wynik zwrotny, co jest dokładnie tym, czego potrzebuję, żeby ominąć frustum culling.

### 4.2 Obliczanie odległości bez pierwiastka kwadratowego

Sprawdzanie, czy obiekt jest w zasięgu renderowania, mogłoby wyglądać tak: oblicz odległość euklidesową (sqrt(dx² + dy² + dz²)) i porównaj z promieniem. Operacja `sqrt` jest jednak kosztowna obliczeniowo i wykonywana jest dla każdego bytu w każdej klatce. Zamiast tego porównuję kwadraty odległości: obliczam `dx² + dy² + dz²` i porównuję z `renderRange²`. Matematycznie wynik jest identyczny, ale eliminuję wywołanie funkcji pierwiastkowej.

### 4.3 Wzorzec stanu oczekującego w GUI

Ekran konfiguracyjny przechowuje dwie kopie ustawień: aktualną (z pliku konfiguracyjnego) oraz roboczą (`pendingEnabled`, `pendingRange`). Użytkownik modyfikuje wartości robocze. Dopiero kliknięcie przycisku "Done" zatwierdza zmiany i zapisuje je do pliku. Kliknięcie "Cancel" po prostu zamyka ekran bez żadnych skutków ubocznych. To standardowy wzorzec projektowy dla okien ustawień, który chroni przed przypadkowymi zmianami.

### 4.4 Obsługa podwójnego shadera rozmycia

Mod Menu przed otwarciem ekranu ustawień moda aplikuje własny shader rozmycia tła. Gdybym w moim ekranie wywołał standardową metodę `renderBackground()`, Minecraft próbowałby zastosować shader rozmycia po raz drugi w tej samej klatce, co powoduje wyjątek. Rozwiązałem to, wywołując zamiast tego `renderDarkening()`, które nakłada tylko ciemne przyciemnienie bez ponownego uruchamiania shadera.

### 4.5 Opcjonalna zależność w czasie kompilacji

Mod Menu nie jest wymagany do działania moda. Zadeklarowałem je jako `modCompileOnly` w pliku `build.gradle`, co oznacza, że Gradle używa go tylko do kompilacji (żeby kod się skompilował z referencjami do klas Mod Menu), ale nie dołącza go do wynikowego pliku `.jar`. Jeśli gracz nie ma zainstalowanego Mod Menu, mod działa normalnie, a ustawienia można zmienić ręcznie edytując plik `forcerender.properties`.

---

## 5. Czego się nauczyłem

Ten projekt nauczył mnie, jak działają modyfikacje do gier od strony technicznej. Wcześniej wiedziałem, że moduły do Minecrafta istnieją, ale nie rozumiałem mechanizmu, który na to pozwala. Teraz rozumiem, jak Mixin pozwala modyfikować skompilowany kod bez dostępu do źródeł, co jest koncepcją przydatną daleko poza Minecraftem.

Nauczyłem się też, jak wygląda profesjonalny pipeline budowania projektu: Gradle z pluginem Loom automatyzuje pobranie i rozpakowanie Minecrafta, zastosowanie mapowań Yarn, kompilację i pakowanie moda. Zrozumiałem, dlaczego projekty korzystają z deobfuskacji i jak mapowania pozwalają pisać czytelny kod, który odwołuje się do sensownych nazw klas i metod zamiast zaciemnionych liter.

Z punktu widzenia projektowania kodu projekt pokazał mi wartość separacji odpowiedzialności: logika renderowania, zarządzanie konfiguracją, interfejs użytkownika i integracja z zewnętrznym modem to cztery oddzielne klasy, z których każda ma jedno zadanie. Dzięki temu kod jest łatwy do zrozumienia i modyfikacji.

---

## 6. Jak uruchomić

### Wymagania

Do zbudowania projektu potrzebny jest JDK 21 lub nowszy.

### Budowanie

```bash
./gradlew build
```

Zbudowany plik `.jar` pojawi się w katalogu `build/libs/`.

### Instalacja w grze

1. Zainstaluj Fabric Loader dla Minecrafta 1.21.1 lub nowszego.
2. Pobierz Fabric API i umieść w katalogu `mods/`.
3. Opcjonalnie zainstaluj Mod Menu (umożliwia konfigurację z poziomu gry).
4. Skopiuj plik `forcerender-1.1.0.jar` do katalogu `mods/`.

### Konfiguracja

Ustawienia dostępne są przez Mod Menu (ikona koła zębatego na liście modów) lub ręcznie w pliku `~/.minecraft/config/forcerender.properties`.
