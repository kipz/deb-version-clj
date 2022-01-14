# deb-version-clj

[![Clojars Project](https://img.shields.io/clojars/v/org.kipz/deb-version-clj.svg)](https://clojars.org/org.kipz/deb-version-clj)

Parse debian-version scheme as per https://www.man7.org/linux/man-pages/man7/deb-version.7.html

**NOTE** the deprecated _ (underscore) is currently supported in version strings

Thanks to https://github.com/knqyf263/go-deb-version from which I've pulled some test-cases.




```clj
[org.kipz/deb-version-clj "<some version>"]
```

## Usage from Clojure

### Parse a version

```clj
(:require [org.kipz.deb-version.core :refer [parse-version]])
;; returns epoch upstream-version and debian-revision (if present) or nil if invalid
(parse-version "6.0-4.el6.x86_64")
; => ["0", "6.0" "4.el6.x86_64"]
```

### Compare two versions

```clj
(:require [org.kipz.deb-version.core :refer [compare-versions]])
(compare-versions "6.0-4.el6.x86_64" "6.0-5.el6.x86_64")
; => true first arg is lower/before second
```

### Sorting

As per normal Clojure awesomeness, we can use it as a normal comparator

```clj
(sort compare-versions ["2:6.0-1.el6.x86_64" "6.0-4.el6.x86_64" "6.0-5.el6.x86_64"])
; => ("6.0-4.el6.x86_64" "6.0-5.el6.x86_64" "2:6.0-1.el6.x86_64")
```

### Range checking

Easily check if a version is in a particular range (two ranges are supported optionally separated by an &)

The following operators are allowed: `< > <= >= =`

```clj
(:require [org.kipz.deb-version.core :refer [in-range?]])
(in-range? "2:7.4.052" "> 1:7.4.052")
; => true
(in-range? "7.4.052" "< 1:7.4.052 & > 1.2.3")
; => true
```
