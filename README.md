# district-ui-component-form

Reagent form library

## Installation
```clojure
;; Add to dependencies
[district0x/district-ui-component-form "0.1.8-SNAPSHOT"]

## Usage
```clojure
(ns my.application
  (:require
    [district.ui.component.form.chip-input :refer [chip-input]]
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
                        :errors errors}]
       [chip-input {:form-data form-data
                    :chip-set-path [:tags]
                    :ac-options ["some tag"
                                 "some other tag"
                                 "a nice tag"
                                 "a beautiful tag"
                                 "something else"
                                 "another"]
                    :chip-render-fn chip-render}]])))
```
Validation errors are expected to mirror the shape of data, divided into :local and :remote.
Local group is always on, where remote errors persists only until the inputs are touched.

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
