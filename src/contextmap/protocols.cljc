(ns contextmap.protocols)

(defprotocol IValidatable
  (schema [this])
  (validate [this]))

(defprotocol IParameterisable
  (set-params [this params])
  (set-param [this param val]))

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
