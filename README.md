# deb-version-clj

Parse debian-version scheme as per https://www.man7.org/linux/man-pages/man7/deb-version.7.html

**NOTE** the deprecated _ (underscore) is currently supported in version strings

Thanks to https://github.com/knqyf263/go-deb-version from which I've pulled some test-cases.

[![codecov](https://codecov.io/gh/kipz/deb-version-clj/branch/master/graph/badge.svg)](https://codecov.io/gh/kipz/deb-version-clj)
[![Clojars Project](https://img.shields.io/clojars/v/deb-version-clj.svg)](https://clojars.org/deb-version-clj)


```clj
[org.kipz/deb-version-clj "0.1-SNAPSHOT"]
```
## Usage

### Parse a version

```clj
(:require [org.kipz.deb-version.core :as deb])
;; returns epoch upstream-version and debian-revision (if present) or nil if invalid
(parse-version "6.0-4.el6.x86_64")
; => ["0", "6.0" "4.el6.x86_64"]
```

### Compare two versions

```clj
(:require [org.kipz.deb-version.core :as deb])
(compare-versions "6.0-4.el6.x86_64" "6.0-5.el6.x86_64")
; => true first arg is lower/before second
```

### Sorting

As per normal Clojure awesomeness, we can use it as a normal comparator

```clj
(:require [org.kipz.deb-version.core :as deb])
(sort compare-versions ["2:6.0-1.el6.x86_64" "6.0-4.el6.x86_64" "6.0-5.el6.x86_64"])
; => ("6.0-4.el6.x86_64" "6.0-5.el6.x86_64" "2:6.0-1.el6.x86_64")
```
