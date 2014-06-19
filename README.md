# yesql-sugar

Syntactic Sugar for [yesql](https://github.com/krisajenkins/yesql).

[![endorse](https://api.coderwall.com/xsc/endorsecount.png)](https://coderwall.com/xsc)

## Usage

__Leiningen__ ([via Clojars](https://clojars.org/yesql-sugar))

[![Clojars Project](http://clojars.org/yesql-sugar/latest-version.svg)](http://clojars.org/yesql-sugar)

To be used with yesql 0.4.0 since this library relies on some implementation details that
might change between releases. Hopefully, future versions will have made yesql-sugar obsolete
anyways.

__SQL File__

```sql
-- name: test-query
SELECT id FROM people
WHERE age < :max_age AND active = :active
```

__REPL__

```clojure
(require '[yesql.sugar :refer :all])

(defqueries+ "/path/to/test.sql"
  test-query (only :id))
```

`defqueries+` takes - in addition to the path to a query file - pairs of functions and
post processors which will be applied to the query result. Additionally, parameters
have to be passed as maps now, automatically converting dashes to underscores:

```clojure
(test-query db-spec {:max-age 30 :active true})
;; => (1 2 3 ...)
```

As you can see, the function returns only the `:id` column.

## License

Copyright &copy; 2014 Yannick Scherer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
