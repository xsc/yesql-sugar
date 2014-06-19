(ns yesql.sugar
  (:require [yesql.core :as yq]))

;; ## Wrap Functions

(defn- ->lookup
  "Create lookup function for the given named parameter."
  [sym]
  {:pre [(symbol? sym)]}
  (let [k (keyword (name sym))
        k' (-> ^String (name sym)
               (.replaceAll "-" "_")
               (keyword))]
    (fn [data]
      (assert (or (contains? data k)
                  (contains? data k'))
              (str "value missing for key: " k))
      (or (get data k)
          (get data k')))))

(defn- wrap-named-parameters
  "Wrap the given yesql function with a translation from a map parameter
   to the given positional parameters (both are expecting the DB spec as
   first argument). For example:

     (def select-something
       (wrap-named-parameters select-something' '[a b]))

     ;; these two are identical:
     (select-something' db 0 1)
     (select-something  db {:a 0 :b 1})
   "
  [f params]
  (let [ps (map ->lookup params)]
    (fn [db-spec data]
      (->> ps
           (map #(% data))
           (apply f db-spec)))))

(defn- wrap-post-processor
  "Wrap function (generated by `wrap-named-parameters`!) to apply the given post-processor
   to the result."
  [f post-processor]
  (fn [db-spec data]
    (let [result (f db-spec data)]
      (if post-processor
        (post-processor result)
        result))))

(defn- normalize-named-parameter
  [sym]
  {:pre [(symbol? sym)]}
  (-> ^String (name sym)
      (.replaceAll "_" "-")
      (symbol)))

(defn- wrap-yesql-var
  "Wrap a yesql function stored in the given var to accept a map instead of
   positional parameters, as well as produce a result processed by the given
   (optional) function."
  ([fn-var] (wrap-yesql-var fn-var nil))
  ([fn-var post-processor]
   (when-let [{:keys [arglists]} (meta fn-var)]
     (when-let [params' (next (first arglists))]
       (let [params (map normalize-named-parameter params')]
         (->> #(-> %
                   (wrap-named-parameters params)
                   (wrap-post-processor post-processor))
              (alter-var-root fn-var))
         (->> (list ['db {:keys (vec params)}])
              (alter-meta! fn-var assoc :arglists)))))
   fn-var))

(defn ^:no-doc wrap-yesql-vars
  "Given a map of symbol->post-processor associations, wrap all the given yesql vars
   to accept a map instead of positional parameters and apply the respective post-
   processor."
  [post-processors fn-vars]
  (mapv
    #(->> %
          (meta)
          (:name)
          (get post-processors)
          (wrap-yesql-var %))
    fn-vars))

;; ## Post-Processors

(defn only
  "Select only a single field from the results."
  [k]
  {:pre [(keyword? k)]}
  #(map k %))

(defn map-of
  "Create a map with the given fields as keys/values."
  [k v]
  {:pre [(keyword? k)]}
  #(into {} (map (juxt k v) %)))

(defn group
  "Create a map grouping the query results by the given key."
  [k]
  {:pre [(keyword? k)]}
  #(group-by k %))

;; ## Macro

(defmacro defqueries+
  "Like `yesql.core/defqueries` this macro creates SQL query execution functions.
   The resulting functions will accept maps instead of positional parameters;
   additionally a post-processor can be given for each function that will be
   applied to the raw query result.

   `test.sql`:

   ```
      -- name: test-query
      SELECT id FROM people
      WHERE age < :max_age AND active = :active
   ```

   `test.clj`:

   ```
     (require '[yesql.sugar :refer :all])
     (defqueries+ \"sql/test.sql\"
       test-query (only :id))
   ```

   This function can be called with a map of parameters and will return only
   a seq of IDs instead of maps of `{:id ...}`:

   ```
     (test-query db-spec {:max_age 30 :active true})
     ;; => (123 456 ...)
   ```

   "
  [queries-file & wrappers]
  `(let [post-processors# ~(->> (for [[sym wrapper] (partition 2 wrappers)]
                                  (vector `(quote ~sym) wrapper))
                                (into {}))]
     (->> (yq/defqueries ~queries-file)
          (wrap-yesql-vars post-processors#))))