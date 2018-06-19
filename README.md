# district-ui-component-form

Reagent form library

## Installation
```clojure
;; Add to dependencies
[district0x/ui-component-form "0.1.0-SNAPSHOT"]

## Usage
```clojure
(ns my.application
  (:require
    [district.ui.component.form.input :as inputs :refer [text-input textarea-input]]))

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
Validation errors are expected to mirror the shape of data, divided into :local and :remote.
Local group is always on, where remote errors persists only until the inputs are touched.

## License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
