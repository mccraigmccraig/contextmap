# contextmap

simulate a graph of objects with a nested map-like structure

* map-entries can refer to other map-entries by their path
* map-entries can refer to a map of params, kept separate from the context
* maps can implement arbitrary protocols
* maps carry associated Prismatic Schema
* the graph of objects is initialised from a vanilla map
* the `defcontext` macro is provided to define types for node
* the `#cm/type` tagged literal refers to a type defined with `defcontext`
* the `#cm/ref` tagged literal refers to a node by it's path in the nested structure
* the `#cm/param` tagged literal refers to a param by it's path in the params map
* `create-context` creates the structure from the definition
* `update-params` and `update-param` can be used to update the parameter map - returning an updated version of the structure
* `validate` runs a Prismatic Schema validation on a node

        (require '[contextmap :as cm])
        (require '[schema.core :as s])

        (defprotocol INamed (get-name [self]))

        (cm/defcontext Foo {:baz (s/protocol contextmap.protocols/IContextMap)
                            :arg s/Int}
          INamed
          (get-name [self] (:arg self)))

        (cm/defcontext Baz {:foo (s/protocol contextmap.protocols/IContextMap)
                            :bloop s/Keyword}
          INamed
          (get-name [self] (:bloop self)))

        (def f (cm/create-context

                {:foo {:type #cm/type :user.Foo
                       :baz #cm/ref :baz
                       :arg 10}

                 :baz {:type #cm/type :user.Baz
                       :foo #cm/ref :foo
                       :bloop #cm/param :boo}}

                {:boo :hoo}))

        ;; types
        (type (:foo f)) ;; => user.Foo
        (type (:baz f)) ;; => user.Baz

        ;; schema validation
        (cm/validate f)
        (cm/validate (:foo f))
        (cm/validate (:baz f))

        ;; get
        (get-in f [:foo :arg]);; => 10
        (get-in f [:baz :bloop]) ;; => :hoo
        (get-in f [:foo :baz :bloop]) ;; => :hoo
        (get-in f [:baz :foo :arg]);; => 10

        ;; protocols
        (get-name (:foo f)) ;; => 10
        (get-name (:baz f)) ;; => :hooq

        ;; setting params
        (def g (cm/set-param f :boo :blah))
        (get-in g [:foo :baz :bloop]) ;; => :blah

        ;; validation failure
        (def h (cm/set-param f :boo "blah"))
        (cm/validate (:baz h)) ;; => fail!
