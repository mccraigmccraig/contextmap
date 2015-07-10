# contextmap

a persistent data structure which represents a graph of objects with nested map-like structures

* each object behaves like a Map
* objects can have attributes and reference other objects
* objects can implement arbitrary protocols
* graph traversal is with get / get-in
* objects can carry a Prismatic Schema and implement a protocol to validate
* the graph of objects is initialised from an EDN map
* objects can refer to other objects by their path in the nested structure
* objects can refer to objects in a separate map of params by a path into the param map

* the `defcontext` macro is provided to define types for objects
* the `#cm/type` tagged literal refers to a type defined with `defcontext`
* the `#cm/ref` tagged literal refers to an object by it's path in the nested structure
* the `#cm/param` tagged literal refers to a param by it's path in the params map
* `create-context` creates the structure from an EDN definition
* `update-params` and `update-param` can be used to update the parameter map - returning an updated version of the structure
* `validate` runs a Prismatic Schema validation on an object

        (require '[contextmap :as cm])
        (require '[schema.core :as s])

        ;; a protocol to be implemented by a contextmap
        (defprotocol INamed (get-name [self]))

        ;; define a contextmap object type Foo with a schema and implementing protocol INamed
        (cm/defcontext Foo {:baz (s/protocol contextmap.protocols/IContextMap)
                            :arg s/Int}
          INamed
          (get-name [self] (:arg self)))

        ;; define a contextmap object type Baz with a schema and implementing protocol INamed
        (cm/defcontext Baz {:foo (s/protocol contextmap.protocols/IContextMap)
                            :bloop s/Keyword}
          INamed
          (get-name [self] (:bloop self)))

        ;; create a graph of contextmap objects
        ;; if not :type is given maps will be instances of contextmap.GenericContext
        (def f (cm/create-context

                {:foo {:type #cm/type :user.Foo
                       :baz #cm/ref :baz
                       :arg 10}

                 :baz {:type #cm/type :user.Baz
                       :foo #cm/ref :foo
                       :bloop #cm/param :boo}}

                {:boo :hoo})) ;; optional params map for #cm/param refs

        ;; check the types of the :foo and :baz objects
        (type (:foo f)) ;; => user.Foo
        (type (:baz f)) ;; => user.Baz

        ;; validate schemas
        (cm/validate f)
        (cm/validate (:foo f))
        (cm/validate (:baz f))

        ;; traverse the graph, get attributes
        (get-in f [:foo :arg]);; => 10
        (get-in f [:baz :bloop]) ;; => :hoo
        (get-in f [:foo :baz :bloop]) ;; => :hoo
        (get-in f [:baz :foo :arg]);; => 10

        ;; use protocols to access objects
        (get-name (:foo f)) ;; => 10
        (get-name (:baz f)) ;; => :hooq

        ;; update params (returning a modified context)
        (def g (cm/set-param f :boo :blah))
        (get-in g [:foo :baz :bloop]) ;; => :blah

        ;; validation failure !
        (def h (cm/set-param f :boo "blah"))
        (cm/validate (:baz h)) ;; => fail!
