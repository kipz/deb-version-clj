(ns org.kipz.deb-version.core
  (:require [clojure.string :as str]))

; https://www.man7.org/linux/man-pages/man7/deb-version.7.html

(def ^:private epoch-upstream-re
  #"^(\d+):(\d+[A-Za-z0-9_\:\.\+\~]+)$")
(def ^:private epoch-upstream-revision-re
  #"^(\d+):(\d+[A-Za-z0-9:_\.\+\-\~]+?)(?:-([A-Za-z0-9_\+\.\~]+))$")
(def ^:private no-epoch-upstream-re
  #"^(\d+[A-Za-z0-9_\.\+\~]*)$")

(def ^:private no-epoch-upstream-revision-re
  #"^(\d+[A-Za-z0-9_\.\+\-\~]+?)(?:-([A-Za-z0-9_\+\.\~]+))$")

(defn parse-version
  "Return a tuple of epoch, upstream-version and optionally debian-version
   or nil if it cannot be parsed correctly"
  [version]
  (when (string? version)
    (not-empty
     (rest (or (re-matches epoch-upstream-re version)
               (re-matches epoch-upstream-revision-re version)
               (when-let [no-epoch (or (re-matches no-epoch-upstream-re version)
                                       (re-matches no-epoch-upstream-revision-re version))]
                 ;; make it look like the others - default to 0 epoch
                 (concat [(first no-epoch) "0"] (rest no-epoch))))))))

(defn- is-letter?
  [^Character c]
  (boolean (some-> c Character/isLetter)))

(defn- compare-chars
  [^Character x ^Character y]
  (cond
    (= x y)
    0
    (= \~ x)
    -1
    (= \~ y)
    1
    (and (nil? x) (some? y))
    -1
    (and (some? x) (nil? y))
    1
    (and (is-letter? x) (is-letter? y))
    (Character/compare x y)
    (and (is-letter? x) (not (is-letter? y)))
    -1
    (and (not (is-letter? x)) (is-letter? y))
    1
    :else
    (Character/compare x y)))

(defn- compare-strings
  [^String sx ^String sy]
  (loop [sx sx sy sy]
    (let [cx (first sx)
          cy (first sy)
          res (compare-chars cx cy)]
      (if (zero? res)
        (recur (rest sx) (rest sy))
        res))))

(defn- split-version
  [version-str]
  (map
   (fn [s]
     (if (re-matches #"\D+" s)
       s
       (Integer/parseInt s)))
   (re-seq #"\d+|\D+" version-str)))

(defn- integers?
  [i1 i2]
  (or (int? i2) (int? i1)))

(defn- compare-version
  "Compare upstream/debian revision on their own"
  [v1 v2]
  (boolean
   (when (and v1 v2)
     (loop [v1s (split-version v1)
            v2s (split-version v2)]
       (let [p1f (first v1s)
             p2f (first v2s)
             ints? (integers? p1f p2f)
             p1 (or p1f (when ints? 0))
             p2 (or p2f (when ints? 0))]
         (when (or p1 p2)
           (if ints?
             (if (not= p1 p2)
               (< p1 p2)
               (and p1f p2f
                    (recur (rest v1s) (rest v2s))))
             (if (not= p1 p2)
               (neg? (compare-strings p1 p2))
               (and p1f p2f
                    (recur (rest v1s) (rest v2s)))))))))))

(defn compare-versions
  "Compare to debian package version strings. Returns true if v1 is before/lower than v2"
  [v1 v2]
  (boolean
   (let [[epoch1 v1-upstream v1-debian] (parse-version v1)
         [epoch2 v2-upstream v2-debian] (parse-version v2)]

     ;; epochs take precedence
     (if (= epoch1 epoch2)
       (let [less? (compare-version v1-upstream v2-upstream)]
         (if (not (or less? (compare-version v2-upstream v1-upstream)))
           (or
             ;; absence of debian revision is lower
             (and v2-debian (not v1-debian))
             (compare-version v1-debian v2-debian))
           less?))
       (when (and epoch1 epoch2)
         (< (Integer/parseInt epoch1) (Integer/parseInt epoch2)))))))

(def ^:private range-operators #"(\>=|\<=|\<|\>|=)")

(defn- split-ranges
  "Pre-clean ranges for easier version matching"
  [range-str]
  (str/split
   (str/replace
    (str/trim
     (str/replace
      range-str
      #"[\<\>=,&]"
      " "))
    #"\s+"
    " ")
   #" "))

(defn- compare-to-range-deb
  [version operator range]
  (boolean
   (cond
     (= "=" operator)
     (= (parse-version version) (parse-version range))

     (= "<" operator)
     (compare-versions version range)

     (= ">" operator)
     (compare-versions range version)

     (= "<=" operator)
     (or (= (parse-version version) (parse-version range))
         (compare-versions version range))

     (= ">=" operator)
     (or (= (parse-version version) (parse-version range))
         (compare-versions range version)))))

(defn in-range?
  "Is version in range string (e.g. < 12.23 & > 14.1~foo)"
  [version range]
  (boolean
   (when (and
          (string? version)
          (string? range))
     (let [[range-version1 range-version2] (split-ranges range)
           [operator1 operator2] (map second (re-seq range-operators range))]
       (when (and range-version1 range-version1)
         (and
          (compare-to-range-deb version operator1 range-version1)
          (or
           (not (and range-version2 operator2))
           (compare-to-range-deb version operator2 range-version2))))))))
