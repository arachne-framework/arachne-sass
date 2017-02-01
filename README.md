# arachne-sass

Arachne module for compiling SCSS/SASS as part of an asset pipeline.

## Basic Usage

```clojure
(a/input-dir :test/input "scss" :watch? true)
(a/input-dir :test/vendored-input "vendor/scss" :watch? true)

;; Output a file called "assets/css/application.css" with a sourcemap
(sass/build :test/build {:entrypoint "application.scss"
                         :output-to "application.css"
                         :output-dir "css"
                         :load-path ["bootstrap"]
                         :source-map true
                         :precision 6})

(a/output-dir :test/output "assets")
(a/pipeline [:test/input :test/build]
            [:test/vendored-input :test/build]
            [:test/build :test/output])
(ac/runtime :test/rt [:test/output]))
```

## WARNING

This repository is not yet ready for public consumption. Many key
elements will change before Arachne is ready to be used, even just for
experimentation.

The repository has been made public so people can follow the
development process, and to make CI integration easier, but it would
be foolish and counterproductive to try to actually use it, or even
play around with it just yet.

Stay tuned for this warning to disappear, then have at it :)
