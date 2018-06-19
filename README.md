# district-ui-component-form

A Clojure library designed to ... well, that part is up to you.

## Installation
```clojure
;; Add to dependencies
[district0x/ui-component-form "0.1.0-SNAPSHOT"]

## Usage
```clojure
(defn form []
  (let [form-data (r/atom {})
        errors (reaction {:local (some-validation-fn)})]
    (fn []
      [:div
       [:h1 "test"]
       [text-input {:form-data form-data
                    :id :example.place/state
                    :errors errors}]
       [textarea-input {:form-data form-data
                        :id :example.place/city
                        :errors errors}]])))
```
validation errors expected to mirror the shape of data

## License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
