---
uri: syntagma/doc/textuality-diagnostics
title: Roland Barthes Textuality Diagnostics: Lisible vs. Scriptible & Tmesis
owner: edphos
status: living
updated: 2026-11-15
description: "Quantitative lattice diagnostics distinguishing readerly (lisible) and writerly (scriptible) textuality, factor meet convergence, supersequence join divergence, and tmesis in phosphene/syntagma."
---

# Textuality Diagnostics: *Lisible* vs. *Scriptible* & Tmesis in `phosphene/syntagma`

> *"Why is the writerly our value? Because the goal of literary work (of literature as work) is to make the reader no longer a consumer, but a producer of the text... The readerly texts are products; the writerly text is ourselves writing."*  
> — Roland Barthes, *S/Z* (1970, §I)

> *"Tmesis: we do not read everything with the same intensity of reading; a rhythm is established, casual, singular, not taking all the words into account... Tmesis is the source of the pleasure of the text."*  
> — Roland Barthes, *The Pleasure of the Text* (1973)

`phosphene.syntagma.textuality` translates Roland Barthes' critique of readerly versus writerly discourse into rigorous order-lattice invariants and quantitative diagnostic metrics.

---

## 1. Structural Lattice Topology

Barthes' two textuality poles map directly onto poset operations over sequence notation:

| Semiotic Dimension | Readerly (*Lisible*) Text | Writerly (*Scriptible*) Text |
| :--- | :--- | :--- |
| **Relational Tier** | Dominated by `:strict` contiguous factors | Dispersed across `:loose` subsequences & `:conjunction` |
| **Lattice Meet ($\sigma_1 \wedge \sigma_2$)** | High `factor-meet-ratio` (formulaic convergence) | Low factor meet; idiosyncratic vocabulary combinations |
| **Lattice Join ($\sigma_1 \vee \sigma_2$)** | Minimal `supersequence-join-ratio` | Expansive supersequence join (divergent pathways) |
| **Tmesis ($T$)** | Low/zero tmesis (compulsory linear reading) | High tmesis (hopping, selective skipping, non-linear) |
| **Syntactic Closure** | Monologic, single-path determination | Plural network inviting reader co-production |

---

## 2. Quantitative Diagnostics (`textuality-profile`)

Given a collection of notation sequences or Markdown paragraphs:
```clojure
(require '[phosphene.syntagma.textuality :as text])

(def readerly-corpus
  [[:once :upon :a :time :in :the :kingdom]
   [:once :upon :a :time :in :the :castle]
   [:once :upon :a :time :in :the :forest]])

(text/textuality-profile readerly-corpus)
;;=> {:profile/lisible-index 0.857,
;;    :profile/scriptible-index 0.143,
;;    :profile/tmesis-mean 0.0,
;;    :profile/meet-ratio-mean 0.857,
;;    :profile/join-ratio-mean 0.143,
;;    :profile/classification :readerly}
```

---

## 3. Tmesis Index (`tmesis-index`)

Measures the skipping dispersion when matching a target sequence within an empirical frame:
- **`0.0`**: Target occurs as a strict contiguous factor (no skipping).
- **`(0.0, 1.0]`**: Intervening tokens skipped during loose subsequence matching.
- **`1.0`**: Pure unordered conjunction.

```clojure
(def frame [:the :magnetic :lines :curve :around :the :conducting :wire])

(text/tmesis-index [:magnetic :lines :curve] frame)
;;=> 0.0 (strict contiguous factor)

(text/tmesis-index [:magnetic :curve :wire] frame)
;;=> 0.6 (intervening tokens skipped)
```
