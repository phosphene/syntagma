(ns phosphene.syntagma.distribution-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [phosphene.syntagma.distribution :as dist]))

(deftest distribution-primitives-test
  (testing "Sliding windows partitioning"
    (let [tokens [:a :b :c :d :e]
          windows (dist/sliding-windows tokens 3)]
      (is (= [[:a :b :c] [:b :c :d] [:c :d :e]] windows))))

  (testing "Co-occurrence matrix computation"
    (let [tokens [:faraday :magnetic :curve :faraday :magnetic :force]
          matrix (dist/co-occurrence-matrix tokens 1)]
      (is (= 2 (get-in matrix [:faraday :magnetic])))
      (is (= 2 (get-in matrix [:magnetic :faraday])))))

  (testing "Framed conjunction frequency over AST frames"
    (let [frames [[:faraday :observed :magnetic :lines]
                  [:maxwell :derived :equations :electric]
                  [:faraday :curves :and :lines :magnetic]]
          freq (dist/framed-conjunction-frequency [:faraday :magnetic] frames)]
      (is (= 2 freq))))

  (testing "Loose sequence frequency over AST frames"
    (let [frames [[:faraday :observed :magnetic :curve]
                  [:magnetic :observed :faraday]
                  [:faraday :skips :other :tokens :magnetic :finally :curve]]
          freq (dist/loose-sequence-frequency [:faraday :magnetic :curve] frames)]
      (is (= 2 freq)))))
