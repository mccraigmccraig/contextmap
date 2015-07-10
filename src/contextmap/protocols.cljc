(ns contextmap.protocols)

(defprotocol IContextTemplate
  (context-type [this])
  (create-context [this root path params]))

(defprotocol IContextMap
  (root [this])
  (path [this])
  (params [this])
  (focus [this]))

(defprotocol IType
  (type-name [this]))

(defprotocol IContextMapRef)

(defprotocol IRef
  (ref-name [this]))

(defprotocol IParam
  (param-name [this]))
