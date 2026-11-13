---
uri: syntagma/wiki/relational-morphisms
title: Relational Morphisms Between Notation Vocabularies (T-SYN-007)
owner: edphos
status: living
updated: 2026-11-13
description: "Theoretical framework and Clojure implementation for order-preserving mappings between notation vocabularies without hermeneutic inflation."
---

# Relational Morphisms Between Notation Vocabularies (`T-SYN-007`)

In the Phosphene philological and computational semiotics architecture, aligning historical notation sequences with modern scientific vocabularies must not rely on subjective or unconstrained hermeneutic paraphrasing.

Instead, translation is formalized as a **Relational Morphism** between two alphabets:
$$f: \Sigma_1 \to \Sigma_2$$

A valid vocabulary morphism preserves structural order relations across the constraint hierarchy:
1. **Strict Factor Inclusion:** If sequence $T$ is a contiguous subword of frame $F$ ($T \le_{\text{factor}} F$), then $f(T) \le_{\text{factor}} f(F)$.
2. **Monotonic Poset Subsequence:** If $T \le_{\text{subseq}} F$, then $f(T) \le_{\text{subseq}} f(F)$.
3. **Framed Conjunction:** If $T \le_{\text{conj}} F$, then $f(T) \le_{\text{conj}} f(F)$.

---

## 1. Mathematical Invariants

Let $\Sigma_1, \Sigma_2$ be discrete notation alphabets. Let $X \in \Sigma_1^*$ and $Y \in \Sigma_1^*$.

### Monotonicity
A morphism $f: \Sigma_1 \to \Sigma_2$ extends homomorphically to sequence strings $f(x_1 x_2 \dots x_n) = f(x_1) f(x_2) \dots f(x_n)$.
Because string homomorphisms preserve index order:
$$X \le_{\text{subseq}} Y \implies f(X) \le_{\text{subseq}} f(Y)$$

### Relational Fidelity & Amplification
While order cannot be degraded by a sequence homomorphism (subsequence order is strictly preserved), token-collapsing morphisms ($f(a) = f(b) = c$) can **amplify** a loose sequence into a strict contiguous factor:
$$\text{Fidelity}(X, Y) = \begin{cases}
\text{Preserved} & \text{rank}(\text{tier}(f(X), f(Y))) \ge \text{rank}(\text{tier}(X, Y)) \\
\text{Amplified} & \text{rank}(\text{tier}(f(X), f(Y))) > \text{rank}(\text{tier}(X, Y)) \\
\text{Degraded} & \text{rank}(\text{tier}(f(X), f(Y))) < \text{rank}(\text{tier}(X, Y))
\end{cases}$$

---

## 2. Clojure Realization in `phosphene.syntagma.morphism`

```clojure
(require '[phosphene.syntagma.morphism :as morphism]
         '[phosphene.syntagma.core :as syntagma])

;; 1. Define historical -> modern scientific translation morphism
(def lavoisier-morphism
  (morphism/vocabulary-morphism
    :morphism/phlogiston-to-oxygen
    {:phlogiston :oxygen
     :calx :metal-oxide
     :inflammable-air :hydrogen}))

;; 2. Map sequence tokens
(morphism/map-sequence lavoisier-morphism [:calx :phlogiston])
;;=> [:metal-oxide :oxygen]

;; 3. Verify order preservation and relational fidelity
(def hist-target [:calx :phlogiston])
(def hist-frame [:experiment :calcination :calx :phlogiston :residue])

(morphism/order-preserving? lavoisier-morphism hist-target hist-frame)
;;=> true

(morphism/relational-fidelity lavoisier-morphism hist-target hist-frame)
;;=> {:source-tier :strict, :target-tier :strict, :preserved? true, :degraded? false, :amplified? false}
```

---

## 3. Generative Verification (`s/fdef`)

`phosphene.syntagma.spec` binds formal contracts to:
* `morphism/map-sequence`: Guaranteed sequence length conservation ($|f(S)| = |S|$).
* `morphism/subsequence-preserving?`: Verified across 150 randomized trials using dense alphabets.
* `morphism/relational-fidelity`: Invariant that fidelity report carries valid boolean classification.
