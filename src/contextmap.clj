(ns contextmap
  (:require
   [potemkin :refer [def-map-type import-vars]]
   [schema.core :as s]
   [contextmap.reader]
   [contextmap.protocols :as p]
   [contextmap.util :refer [resolve-class]])
  (:import [contextmap.protocols]))

(import-vars [contextmap.protocols IValidatable schema validate])
(import-vars [contextmap.protocols IParameterisable set-params set-param])

;; a template for an IContextMap...
;; - t : the type of the instance
;; - m : the map - values may be IRefs or IParams which will be deref'd against
;;                 the root / params
(def-map-type ContextTemplate [t m mta]
  (get [_ k default-value]
       (if (contains? m k)
         (get m k)
         default-value))
  (assoc [_ k v]
         (ContextTemplate. t (assoc m k v) mta))
  (dissoc [_ k]
          (ContextTemplate. t (dissoc m k) mta))
  (keys [_]
        (keys m))
  (meta [_]
        mta)
  (with-meta [_ mta]
    (ContextTemplate. t m mta))

  p/IContextTemplate
  (context-type [_] t)
  (create-context [this root path params]
                  (eval `(new ~t ~root ~path ~this ~params))))

(declare ctxmap-deref)

;; def an IContextMap type. arbitrary additional protocols can be given
;; in the body
(defmacro defcontext
  [nm sch & body]
  `(def-map-type ~nm [root*# path*# focus*# params*#]
       (~'get [this# k# default-value#]
            (if (contains? focus*# k#)
              (ctxmap-deref this# k# (get focus*# k#))
              default-value#))
       (~'assoc [_# k# v#]
                (let [new-root# (assoc-in root*# (conj path*# k#) v#)]
                  (new ~nm new-root# path*# params*# (get-in new-root# path*#))))
       (~'dissoc [_# k#]
                 (let [new-root# (assoc-in root*# path*# (dissoc (get-in root*# path*#) k#))]
                   (new ~nm new-root# path*# params*# (get-in new-root# path*#))))
       (~'keys [_#]
             (keys focus*#))
       (~'meta [_#]
               (meta focus*#))
       (~'with-meta [_# mta#]
         (let [new-root# (assoc-in root*# path*# (with-meta (get-in root*# path*#) mta#))]
           (new ~nm new-root# path*# params*# (get-in new-root# path*#))))

       p/IContextMap
       (~'root [_#] root*#)
       (~'path [_#] path*#)
       (~'params [_#] params*#)
       (~'focus [_#] focus*#)

       p/IParameterisable
       (~'set-params [this# new-params#]
                     (new ~nm root*# path*# focus*# new-params#))
       (~'set-param [this# param# val#]
                    (new ~nm root*# path*# focus*# (assoc params*# param# val#)))

       p/IValidatable
       (~'schema [_#] ~sch)
       (~'validate [this#] (schema.core/validate ~sch this#))

       ~@body))

;; GenericContext for any map in the context structure which doesn't have a specific type
(defcontext GenericContext {s/Keyword s/Any})

(defn make-sequential
  [s]
  (cond
    (nil? s) nil
    (sequential? s) s
    :else [s]))

(defn is-context-spec?
  [spec]
  (some->> spec :type (satisfies? p/IType)))

(declare convert-to-context-template)

(defn convert-map-to-context-template
  [ctx-spec]
  (into {} (map (fn [[k v]] [k (convert-to-context-template v)])
                ctx-spec)))

(defn convert-to-context-template
  "convert an associative structure into a similar
   structure where maps which are is-context-spec? are
   replaced with a ContextTemplates referencing the given type,
   and other maps are replaced with ContextTemplates referencing
   GenericContext. other values are left unchanged (should
   maybe introduce a SequenceTemplate so that sequential? values
   can contain contexts)"
  [ctx-spec]
  (cond

    (satisfies? p/IContextMapRef ctx-spec)
    ctx-spec

    (is-context-spec? ctx-spec)
    (let [t (-> ctx-spec :type p/type-name resolve-class)
          ctx-spec (dissoc ctx-spec :type)]
      (ContextTemplate. t (convert-map-to-context-template ctx-spec) (meta ctx-spec)))

    (map? ctx-spec)
    (ContextTemplate. GenericContext (convert-map-to-context-template ctx-spec) (meta ctx-spec))

    :else
    ctx-spec))

(defn create-if
  "if ctx-template-or-val is an IContextTemplate then return a new IContextMap
   with the ctx-template-or-val as the focus, otherwise return ctx-template-or-val"
  [ctxmap path ctx-template-or-val]
  (cond
    (satisfies? p/IContextTemplate ctx-template-or-val)
    (p/create-context ctx-template-or-val (p/root ctxmap) path (p/params ctxmap))

    :else
    ctx-template-or-val))

(defn deref-if
  "- if ref-or-val is an IRef, follow it, return [ref-path ref-val]
   - if ref-or-val is an IParam, follow it, return [nil ref-val]
   - otherwise return [path ref-or-val]"
  [ctxmap path ref-or-val]
  (cond
    (satisfies? p/IRef ref-or-val)
    (let [rp (make-sequential (p/ref-name ref-or-val))]
      [rp (get-in (p/root ctxmap) rp)])

    (satisfies? p/IParam ref-or-val)
    [nil (get-in (p/params ctxmap) (make-sequential (p/param-name ref-or-val)))]

    :else
    [path ref-or-val]))

(defn ctxmap-deref
  "get a key, follow refs and create a new IContextMap as necessary"
  [ctxmap key ref-or-val]
  (let [p (conj (p/path ctxmap) key)
        [p ctx-template-or-val] (deref-if ctxmap p ref-or-val)]
    (create-if ctxmap p ctx-template-or-val)))

(defn print-context
  [ctxmap writer]
  (.write writer "#")
  (.write writer (.getName (p/context-type (p/focus ctxmap))))
  (#'clojure.core/pr-on {:focus (p/focus ctxmap) :path (p/path ctxmap) :root (p/root ctxmap) :params (p/params ctxmap)} writer))

(defmethod print-method contextmap.protocols.IContextMap
  [ctxmap writer]
  (print-context ctxmap writer))

(prefer-method print-method contextmap.protocols.IContextMap clojure.lang.IPersistentMap)
(prefer-method print-method contextmap.protocols.IContextMap java.util.Map)

(defmethod print-dup contextmap.protocols.IContextMap
  [ctxmap writer]
  (print-context ctxmap writer))

(prefer-method print-dup contextmap.protocols.IContextMap clojure.lang.IPersistentMap)
(prefer-method print-dup contextmap.protocols.IContextMap java.util.Map)

(defn create-context
  ([ctx-spec] (create-context ctx-spec {}))
  ([ctx-spec params]
   (let [root (convert-to-context-template ctx-spec)]
     (p/create-context root root [] params))))
