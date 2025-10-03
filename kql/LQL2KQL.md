
# Regelsätze zur Umformung

## FIND-Clause

Die FIND-Clause erstellt ein Netz verknüpfter Tabellen.

Für optional verküpfte Tabellen kann ein zusätzlicher Ausdrück zum Filtern des outer-join angegeben werden.

        FIND customers c, c - customer_id + orders (order_date > DATE '2025-01-01') o

### Ausdrücke an optionalen Joins

Die Ausdrücke an optionalen Joins sind voraussichtlich nicht erforderlich, weil stattdessen ein CTE mit dem Ausdruck erstellt werden kann. 

## WHERE-Clause

Die WHERE-Clause enthält einen einzigen Ausdruck für
das Select.
Ggf. könnne unary_logical_expression wenn sie "tabellenrein" sind einer einzelnen Tabelle zugeordnet werden, so dass diese Tabelle bewertet werden kann.

Falls sich die WHERE-Clause auf eine Aggregat-Funktion bezieht, dann wird der Ausdruck automatisch als HAVING-Clause verwendet.

## RETURN-Clause, GROUP-Clause

Alle RETURN und GROUP Ausdrücke werden der ersten Tabelle zugeordnet. 
Falls in der RETURN-Clause Aggregationen verwendet werden, dann werden automatisch GROUP-Ausdrücke ergänzt.

# Grundlegende Strategie

Mithilfe von CTE sollen komplexe Fragen soweit zerlegt werden, dass die einzelne Frage entsprechend einfach ist. Und sowohl von Menschen, als auch von LLM einfache verstanden werden kann.