# Meal Planner Service

## Przeznaczenie i Rola
`meal-planner-service` odpowiada za planowanie odżywiania i upraszczanie zakupów użytkownika. Służy do zautomatyzowanego generowania wielodniowych rozkładów potraw oraz dynamicznego kompilowania skonsolidowanej listy składników niezbędnych do przygotowania tychże potraw.

## Kluczowe Funkcjonalności
* **Generowanie Tygodniowego Planu:** System losuje (korzystając z danych np. TheMealDB API przez `DiscoveryClient`/OpenFeign) pełny harmonogram posiłków na dany tydzień (zależnie od parametru np. "3 potrawy dziennie").
* **Tworzenie Listy Zakupów:** System analizuje wygenerowane potrawy, rozbija je na składniki i używając inteligentnych algorytmów (Deduplikacja), tworzy wspólną listę, usuwając puste pozycje oraz powtarzające się produkty (np. łączenie ilości i formatowanie zapisu - "1 cup water").
* **Utrwalanie (Persistence):** Automatyczne zapisywanie (z wykorzystaniem wzorców JPA i warstwy Persistence) wygenerowanych planów oraz list w dedykowanych tabelach relacyjnych.
* **Odtwarzanie i Zarządzanie:** Użytkownik ma pełen dostęp do swojej historii zapisanych tygodni.

## Integracje i Technologie
- Komunikacja Bazy Danych: `meal_planner` schema w środowisku PostgreSQL.
- Złożone Relacje JPA: Mapowanie OneToMany / ElementCollection na poziomie składników i kroków (częściowo uproszczone na potrzeby list stringów).

## Flow i Algorytmy

### Algorytm Planowania Tygodniowego
Działa w oparciu o ustalone "sloty" posiłków dla wybranej ilości dań na dzień (od 1 do 5). 
1. Serwis pobiera pełną listę kategorii (np. Breakfast, Beef, Pasta) z *DiscoveryController* w `main-service`.
2. Filtruje kategorie tak, by wyłonić odpowiednią rotację dań dla slotu `MAIN` (z wykluczeniem posiłków śniadaniowych, deserów i przystawek).
3. Następnie dla każdego z siedmiu dni rotuje dostępnymi kategoriami i odpytuje TheMealDB o wybraną kategorię. Z otrzymanej listy potraw losuje jedną i wypełnia dany dzień.
4. Całość zwracana jest użytkownikowi do wglądu lub zapisu, co powoduje zrzut zlecenia do dedykowanej tabeli `weekly_plans`.

### Budowanie Listy Zakupów
1. Klient przesyła listę identyfikatorów posiłków wygenerowanych z planu tygodniowego.
2. Serwis przechodzi przez każde danie, wyciąga jego dokładne składniki (`strIngredient`) oraz ich miary (`strMeasure`).
3. Algorytm **deduplikuje** listę, wykorzystując słownik (mapę) do zliczania wystąpień i konsolidacji powielonych składników.
4. Finalnie powstaje zbiorcza lista produktów, na której poszczególne miary (np. "1 szklanka", "2 łyżki") są skondensowane w jedno miejsce wraz z wymienioną nazwą składnika oraz listą przepisów z jakich pochodzą. Wynik jest zapisywany w `shopping_lists`.
