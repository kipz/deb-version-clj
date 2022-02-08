(ns org.kipz.deb-version.core-t
  (:require [clojure.test :refer [deftest is testing]]
            [org.kipz.deb-version.core :refer [parse-version
                                               compare-versions
                                               in-range?] :as deb]))

(deftest version-parsing
  (testing "versions must start with a digit"
    (is (= nil (parse-version "1:abc")))
    (is (= nil (parse-version "abc")))
    (is (= ["0" "1"] (parse-version "1"))))

  (testing "can parse versions with epoch and no revision"
    (is (= ["0" "4:abvX"] (parse-version "0:4:abvX")))
    (is (= ["0" "4abvX"] (parse-version "0:4abvX"))))
  (testing "can parse versions with epoch and revision"
    (is (= ["0" "4:a~+b-vX" "fo~.+o"] (parse-version "0:4:a~+b-vX-fo~.+o"))))
  (testing "can parse versions with no epoc or revision"
    (is (= ["0" "1a.bc~+"] (parse-version "1a.bc~+"))))
  (testing "can parse versions with no epoc but with revision"
    (is (= ["0" "4abvXfoo" "bar"] (parse-version "4abvXfoo-bar")))
    (is (= ["0" "4abvXfoo" "bar~.+"] (parse-version "4abvXfoo-bar~.+")))
    (is (= ["0" "4a.+~bvXfoo" "bar~.+"] (parse-version "4a.+~bvXfoo-bar~.+"))))

  (testing "some more realistic versions can be parsed"
    (is (= ["2" "7.4.052" "1ubuntu3"] (parse-version "2:7.4.052-1ubuntu3")))
    (is (= ["2" "7.4.052" "1"] (parse-version "2:7.4.052-1")))
    (is (= ["0", "7.4.052", "1"] (parse-version "7.4.052-1")))
    (is (= ["0", "1.2.3"] (parse-version "1.2.3")))
    (is (= ["1", "1.2.3"] (parse-version "1:1.2.3")))
    (is (= nil (parse-version "A:1.2.3")))
    (is (= nil (parse-version "-1:1.2.3")))
    (is (= ["0", "6.0" "4.el6.x86_64"] (parse-version "6.0-4.el6.x86_64")))
    (is (= ["0", "6.0" "9ubuntu1.5"] (parse-version "6.0-9ubuntu1.5")))
    (is (= ["2", "7.4.052" "1ubuntu3.1"] (parse-version "2:7.4.052-1ubuntu3.1")))
    (is (= nil (parse-version "2:-1ubuntu3.1")))
    (is (= nil (parse-version  "2:A7.4.052-1ubuntu3.1")))
    (is (= nil (parse-version "2:7.4.!052-1ubuntu3.1")))
    (is (= nil (parse-version "7.4.052-!1ubuntu3.1")))))

(deftest version-fragment-comparison
  (is (#'deb/compare-strings "~~" "~~a"))
  (is (not (#'deb/compare-strings "a" "~")))
  (is  (#'deb/compare-strings "~" "a"))
  (is (not (#'deb/compare-strings "~~a" "~~")))
  (is (#'deb/compare-strings "~~a" "~"))
  (is (not (#'deb/compare-strings "~" "~~a")))
  (is (#'deb/compare-strings "~" ""))
  (is (not (#'deb/compare-strings "" "~")))
  (is (#'deb/compare-strings "" "a"))
  (is (not (#'deb/compare-strings "a" "")))
  (is (#'deb/compare-strings "a" "."))
  (is (not (#'deb/compare-strings "." "a")))
  ;‘ ~~’, ‘ ~~a’, ‘ ~’, the empty part, ‘a’.
  (is (= ["~~" "~~a" "~" "" "a"  "."] (sort  #(#'deb/compare-strings %1 %2) ["~~" "a" "" "." "~" "~~a"])
         (sort  #(#'deb/compare-strings %1 %2) ["~~a" "~~" "a" "" "." "~"]))))

(deftest version-comparison
  (is (compare-versions "6.4.052" "7.4.052"))
  (is (not (compare-versions "7.4.052" "6.4.052")))

  (is (compare-versions "6.4.052" "6.5.052"))
  (is (not (compare-versions "6.5.052" "6.4.052")))

  (is (compare-versions "6.4.052" "6.4.053"))
  (is (not (compare-versions "6.4.053" "6.4.052")))

  (is (compare-versions "1ubuntu1" "1ubuntu3.1"))
  (is (not (compare-versions "1ubuntu3.1" "1ubuntu1")))

  (is (compare-versions "1" "1ubuntu1"))

  (is (not (compare-versions "1ubuntu1" "1")))

  (is (compare-versions "7.4.027" "7.4.052"))
  (is (not (compare-versions "7.4.052" "7.4.027")))


  (testing "redhat versions"
    (is (compare-versions "7.4.629-3" "7.4.629-5"))
    (is (not (compare-versions "7.4.629-5" "7.4.629-3")))

    (is (compare-versions "7.4.622-1" "7.4.629-1"))
    (is (not (compare-versions "7.4.629-1" "7.4.622-1")))

    (is (compare-versions "6.0-4.el6.x86_64" "6.0-5.el6.x86_64"))
    (is (not (compare-versions "6.0-5.el6.x86_64" "6.0-4.el6.x86_64")))

    (is (compare-versions "6.0-4.el6.x86_64" "6.1-3.el6.x86_64"))
    (is (not (compare-versions "6.1-3.el6.x86_64" "6.0-4.el6.x86_64")))

    (is (not (compare-versions "6.1-a" "6.1")))
    (is (compare-versions "6.1" "6.1-a"))

    (is (not (compare-versions "7.0-4.el6.x86_64" "6.1-3.el6.x86_64")))
    (is (compare-versions "6.1-3.el6.x86_64" "7.0-4.el6.x86_64")))

  (testing "debian versions"
    (is (compare-versions "2:7.4.052-1ubuntu3" "2:7.4.052-1ubuntu3.1"))
    (is (not (compare-versions "2:7.4.052-1ubuntu3.1" "2:7.4.052-1ubuntu3")))

    (is (compare-versions "2:7.4.052-1ubuntu2" "2:7.4.052-1ubuntu3"))
    (is (not (compare-versions "2:7.4.052-1ubuntu3" "2:7.4.052-1ubuntu2")))

    (is (compare-versions "2:7.4.052-1" "2:7.4.052-1ubuntu3"))
    (is (not (compare-versions "2:7.4.052-1ubuntu3" "2:7.4.052-1")))

    (is (compare-versions "2:7.4.052" "2:7.4.052-1"))
    (is (not (compare-versions "2:7.4.052-1" "2:7.4.052")))

    (is (compare-versions "1:7.4.052" "2:7.4.052"))
    (is (not (compare-versions "2:7.4.052" "1:7.4.052")))

    (is  (not (compare-versions "1:7.4.052" "7.4.052")))
    (is  (compare-versions "7.4.052" "1:7.4.052"))

    (is  (not (compare-versions "2:7.4.052-1ubuntu3.2" "2:7.4.052-1ubuntu3.1")))
    (is  (compare-versions "2:7.4.052-1ubuntu3.1" "2:7.4.052-1ubuntu3.2"))))

(deftest range-matching
  (testing "can match single ranges"
    (is (not (in-range? "7.4.052" "1:7.4.052")))
    (is (in-range? "7.4.052" "< 1:7.4.052"))
    (is (not (in-range? "7.4.052" "> 1:7.4.052")))
    (is (in-range? "2:7.4.052" "> 1:7.4.052"))
    (is (in-range? "2:7.4.052" ">= 2:7.4.052"))
    (is (in-range? "2:7.4.052" "<= 2:7.4.052"))
    (is (in-range? "2:7.4.052" "= 2:7.4.052"))
    (is (not (in-range? "2:7.4.052" "= 1:7.4.052"))))
  (testing "can match double ranges"
    (is (in-range? "7.4.052" "< 1:7.4.052 & > 1.2.3"))
    (is (not (in-range? "7.4.052" "< 1:7.4.052 < 1.2.3")))
    (is (not (in-range? "7.4.052" "< 1.2.3 < 1:7.4.052")))
    (is (in-range? "7.4.052" "> 1.2.3 < 1:7.4.052")))
  (testing "doesn't blow up if there's no major"
    (is (not (in-range? "4.4.0-210.242" "<.4.0-138.164")))))
