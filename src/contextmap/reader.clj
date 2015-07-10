(ns contextmap.reader
  (:require [contextmap.protocols]))

(defrecord Type [type-name*]
  contextmap.protocols.IType
  (type-name [this] type-name*))

(defn type-reader
  [type-name]
  (Type. type-name))

(defrecord Ref [ref-name*]
  contextmap.protocols.IContextMapRef
  contextmap.protocols.IRef
  (ref-name [this] ref-name*))

(defn ref-reader
  [ref-name]
  (Ref. ref-name))

(defrecord Param [param-name*]
  contextmap.protocols.IContextMapRef
  contextmap.protocols.IParam
  (param-name [this] param-name*))

(defn param-reader
  [param-name]
  (Param. param-name))
