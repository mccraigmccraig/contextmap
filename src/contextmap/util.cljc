(ns contextmap.util
  (:require [clojure.string :as str]))

(defn safe-require
  [ns]
  (let [ns (symbol ns)]
    (when-not (= 'user ns)
      (require ns))))

(defn ns-class
  "take a classname and return [package-sym class-sym]"
  [c]
  (let [c (name c)
        p (str/split c #"\.")]
    [(symbol (str/join "." (take (dec (count p)) p)))
     (symbol c)]))

(defn resolve-class
  "resolve a class which may be referred to by
   - a symbol : the.package.ClassName
   - a keyword : :the.package.ClassName
   - a Class object"
  [c]
  (if (or (symbol? c) (keyword? c))
    (let [[ns c] (ns-class c)]
      (safe-require ns)
      (eval c))
    c))

(defn resolve-var
  "resolve a reference which may be
   - a symbol : the namespace part is required and the symbol resolved
                and dereferences
   - a keyword : converted to a symbol and resolved
   - a var : the var is dereferenced
   - something else : returned unchanged"
  [n]
  (cond (symbol? n) (do (safe-require (namespace n))
                        (deref (resolve n)))
        (keyword? n) (do (safe-require (namespace n))
                         (deref (ns-resolve (symbol (namespace n))
                                            (symbol (name n)))))
        (var? n) (deref n)
        :else n))
