(ns phosphene.syntagma.spec
  "Fdef specifications and property invariants for phosphene.syntagma.core."
  (:require [clojure.spec.alpha :as s]
            [phosphene.syntagma.core :as core]))

;; --- Function Specs ---

(s/fdef core/strict-subsequence?
  :args (s/cat :target ::core/sequence :frame ::core/frame)
  :ret boolean?
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [target frame]} args]
          ;; Invariant: if strict holds, loose and conjunction must also hold
          (if ret
            (and (core/loose-subsequence? target frame)
                 (core/framed-conjunction? target frame))
            true))))

(s/fdef core/equiv-subsequence?
  :args (s/cat :target ::core/sequence :frame ::core/frame :equiv-fn ::core/equiv-fn)
  :ret boolean?)

(s/fdef core/loose-subsequence?
  :args (s/cat :target ::core/sequence :frame ::core/frame)
  :ret boolean?
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [target frame]} args]
          ;; Invariant: if loose holds, framed conjunction must hold
          (if ret
            (core/framed-conjunction? target frame)
            true))))

(s/fdef core/framed-conjunction?
  :args (s/cat :target ::core/sequence :frame ::core/frame)
  :ret boolean?)

(s/fdef core/relational-tier
  :args (s/cat :target ::core/sequence :frame ::core/frame)
  :ret #{:strict :loose :conjunction :none}
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [target frame]} args]
          (case ret
            :strict (and (core/strict-subsequence? target frame)
                         (core/loose-subsequence? target frame)
                         (core/framed-conjunction? target frame))
            :loose (and (not (core/strict-subsequence? target frame))
                        (core/loose-subsequence? target frame)
                        (core/framed-conjunction? target frame))
            :conjunction (and (not (core/loose-subsequence? target frame))
                              (core/framed-conjunction? target frame))
            :none (not (core/framed-conjunction? target frame))))))
