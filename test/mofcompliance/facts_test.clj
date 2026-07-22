(ns mofcompliance.facts-test
  (:require [clojure.test :refer [deftest is testing]]
            [mofcompliance.facts :as facts]))

(deftest unified-qualification-has-spec-basis
  (let [sb (facts/spec-basis :unified-qualification)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (true? (:foreign-corporations-eligible? sb)))
    (is (true? (:unified-qualification-required? sb)))))

(deftest tender-method-has-spec-basis
  (let [sb (facts/spec-basis :tender-method)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (= "1947-03-31" (:promulgated sb)))))

(deftest fefta-notification-has-spec-basis
  (let [sb (facts/spec-basis :fefta-notification)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (= 3 (count (:sensitivity-tiers sb))))
    (is (= 30 (:standard-review-window-days sb)))
    (is (= "2026-06-05" (:amended sb)))))

(deftest customs-declaration-has-spec-basis
  (let [sb (facts/spec-basis :customs-declaration)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))))

(deftest corporate-number-boundary-is-not-a-filing-track
  (testing "the boundary entry is spec-basis-citable but never itself drafted/submitted"
    (let [sb (facts/spec-basis :corporate-number-boundary)]
      (is (some? sb))
      (is (string? (:provenance sb)))
      (is (empty? (:required-evidence sb)))
      (is (false? (:filing-track? sb)))
      (is (false? (facts/filing-track? :corporate-number-boundary))))))

(deftest filing-track-defaults-true-for-regulatory-entries
  (is (true? (facts/filing-track? :unified-qualification)))
  (is (true? (facts/filing-track? :tender-method)))
  (is (true? (facts/filing-track? :fefta-notification)))
  (is (true? (facts/filing-track? :customs-declaration)))
  (is (false? (facts/filing-track? :unknown-track)) "no spec-basis at all -> not a filing track"))

(deftest unknown-track-has-no-spec-basis
  (is (nil? (facts/spec-basis :unknown-track)))
  (is (nil? (facts/spec-basis :zzz))))

(deftest required-evidence-satisfied
  (let [sb (facts/spec-basis :unified-qualification)
        all (:required-evidence sb)]
    (is (true? (facts/required-evidence-satisfied? :unified-qualification all)))
    (is (not (facts/required-evidence-satisfied? :unified-qualification (take 1 all))))
    (is (nil? (facts/required-evidence-satisfied? :unknown-track all)))))

(deftest coverage-is-honest
  (let [c (facts/coverage [:unified-qualification :tender-method :unknown-track])]
    (is (= 3 (:requested c)))
    (is (= 2 (:covered c)))
    (is (= ["unknown-track"] (:missing-tracks c)))))

(deftest catalog-has-exactly-five-entries
  (is (= #{:unified-qualification :tender-method :fefta-notification
           :customs-declaration :corporate-number-boundary}
         facts/valid-tracks)))
