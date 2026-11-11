(ns phosphene.syntagma.io.markdown-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [phosphene.syntagma.core :as core]
            [phosphene.syntagma.io.markdown :as md]))

(deftest tokenization-test
  (testing "Notation tokenization"
    (let [tokens (md/tokenize-notation "Faraday, M. (1832). Experimental Researches in Electricity.")]
      (is (= [:faraday :m :1832 :experimental :researches :in :electricity] tokens)))))

(deftest parse-markdown-blocks-test
  (testing "AST block extraction with coordinate metadata"
    (let [sample "# Experimental Induction

Faraday observed lines of magnetic force around the conductor.

> By magnetic curves, I mean the lines of magnetic forces.

* Needle deflection noted at 1832.
"
          blocks (md/parse-markdown-blocks "doc1" sample)]
      (is (= 4 (count blocks)))

      ;; 1. Heading
      (is (= "doc1:h:0" (:coordinate (nth blocks 0))))
      (is (= :heading (:block-type (nth blocks 0))))
      (is (= [:experimental :induction] (:tokens (nth blocks 0))))

      ;; 2. Paragraph
      (is (= "doc1:p:1" (:coordinate (nth blocks 1))))
      (is (= :paragraph (:block-type (nth blocks 1))))
      (is (true? (core/strict-subsequence? [:lines :of :magnetic :force] (:tokens (nth blocks 1)))))

      ;; 3. Blockquote
      (is (= "doc1:quote:2" (:coordinate (nth blocks 2))))
      (is (= :blockquote (:block-type (nth blocks 2))))
      (is (true? (core/loose-subsequence? [:magnetic :curves :lines] (:tokens (nth blocks 2)))))

      ;; 4. List item
      (is (= "doc1:item:3" (:coordinate (nth blocks 3))))
      (is (= :list-item (:block-type (nth blocks 3))))
      (is (true? (core/framed-conjunction? [:needle :1832] (:tokens (nth blocks 3))))))))

(deftest disk-file-ingest-test
  (testing "Reading a temporary markdown file from disk"
    (let [tmp (java.io.File/createTempFile "syntagma_doc" ".md")]
      (.deleteOnExit tmp)
      (spit tmp "## Section 1\n\nRiemannian manifold curvature metric ds^2.\n")
      (let [blocks (md/read-markdown-file (.getAbsolutePath tmp))]
        (is (= 2 (count blocks)))
        (is (= :heading (:block-type (first blocks))))
        (is (= :paragraph (:block-type (second blocks))))
        (is (true? (core/framed-conjunction? [:riemannian :manifold :curvature] (:tokens (second blocks)))))))))
