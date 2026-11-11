(ns phosphene.syntagma.runner
  (:require [clojure.test :refer [run-tests]]
            [phosphene.syntagma.core-test]
            [phosphene.syntagma.distribution-test]))

(defn -main [& _args]
  (println "Running syntagma test suite (unit + clojure.spec/test.check)...")
  (let [summary (run-tests 'phosphene.syntagma.core-test
                           'phosphene.syntagma.distribution-test)]
    (if (and (zero? (:fail summary)) (zero? (:error summary)))
      (do
        (println "All tests and generative checks passed cleanly!")
        (System/exit 0))
      (do
        (println "Test failures encountered:" summary)
        (System/exit 1)))))
