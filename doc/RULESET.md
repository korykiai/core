
# IQL-Rules for SQL generation

## Push logical expression to outer joined entities

Optional linked entities may have additional filters. In SQL these filters must appear at OUTER-JOIN-Clause. If
they appear at WHERE-Clause the OUTER-JOIN will turn to INNER-JOIN.

This rule moves expressions from filter to assigned tables.

Sample, move `o.order_data BETWEEN DATE '2023-01-01' AND DATE '2023-12-31'` to entity `orders`.

    // give customers and optional count of orders in 2023

    FIND customers c, c+orders o
    WHERE o.order_data BETWEEN DATE '2023-01-01' AND DATE '2023-12-31'
    RETURN c.company_name, count(o)

 Expected SQL is:

    SELECT
    c.company_name
    , count(o.order_id)
    FROM
     customers c
     LEFT OUTER JOIN orders o ON
      c.customer_id = o.customer_id
     AND
      o.order_data BETWEEN DATE '2023-01-01' AND DATE '2023-12-31'
    GROUP BY
     c.company_name

Without moving the expression to entity `oders` we will get this SQL:

    SELECT
    c.company_name
    , count(o.order_id)
    FROM
     customers c
     LEFT OUTER JOIN orders o ON
      c.customer_id = o.customer_id
    WHERE 
      o.order_data BETWEEN DATE '2023-01-01' AND DATE '2023-12-31'
    GROUP BY
     c.company_name

This will destroy OUTER-JOIN behavior and is supposed to be wrong in this query because user stated to search for 
optional count of orders.

## Add group-by expressions for non-aggregat RETURN-expressions

If the query has at least one aggregat RETURN-expression, we need to add GROUP-BY-expressions to get valid SQL.
In sample above, koryki.ai automaticly added the GROUP-BY-expression to company_name because count(o) was detected
as aggregat-expression.

## Push logical expression to HAVING-Clause

If a filter-expression is an aggregat expression, we need to use HAVING-Clause instead of WHERE-Clause.

Sample, move expression form filter to HAVING-Clause for aggregat RETURN-expressions

    // Find customers who have placed more than 10 orders in January 2023,
    // return companyname and count, sort by count.
    
    FIND customers c, c-orders o
    WHERE count(o) > 10 AND o.order_date BETWEEN DATE '2023-01-01' AND DATE '2023-12-31'
    RETURN c.company_name, count(o)
    ORDER count(o) DESC

Now we have an INNER-JOIN. We search for customers with more than 10 orders.
Filter `count(o) > 10` is an aggregat-expression.

Expected  SQL is:

    SELECT
      c.company_name
    , count(o.order_id)
    FROM
     customers c
      INNER JOIN orders o ON
       c.customer_id = o.customer_id
    WHERE
     o.order_date BETWEEN DATE '2023-01-01' AND DATE '2023-12-31'
    GROUP BY
     c.company_name
    HAVING
     count(o.order_id) > 10
    ORDER BY
     count(o.order_id) DESC

We must not have `count(o) > 10` inside WHERE-Clause because it is invalid SQL.

## Identity Rule

In examples above we used `count(O)` expression. This is more intuitive than SQL because users usually want to count
entities, not column-values `count(o.order_id)` or rows `count(*)`.
koryki.ai supports both count of entities and count of column-values. 

## Infer RETURN-expressions to queryblocks

koryki.ai hides JOIN-columns and therefor we need to add additional RETURN-expressions to use queryblocks in links.
koryki.ai manages this automatically.

Another sample:

    // find the sum of unit_price * quantity for orders
    // order by sum and limit result to first row
    // join with employees and return last_name, first_name and phone number
    
    
    WITH sales AS (
    FIND orders o, o-order_details d
    RETURN sum(d.unit_price * d.quantity) sum
    ORDER sum DESC
    LIMIT 1
    )
    FIND employees e, e-sales s
    RETURN e.last_name, e.first_name, e.home_phone

We have a `sales` queryblock and it is linked to `employee`. We need an extra RETURN of `o.employee_id` from queryblock
`sales` to make the JOIN-Clause working.

Expected  SQL is:

    WITH sales AS (
    SELECT
      sum(d.unit_price * d.quantity) AS sum
    , o.employee_id
    FROM
     orders o
      INNER JOIN order_details d ON
       o.order_id = d.order_id
    GROUP BY
      o.employee_id
    ORDER BY
      sum DESC
    FETCH FIRST 1 ROWS ONLY
    )
    SELECT
      e.last_name
    , e.first_name
    , e.home_phone
    FROM
     employees e
      INNER JOIN sales s ON
       e.employee_id = s.employee_id
