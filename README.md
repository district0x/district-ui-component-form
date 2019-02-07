# district-ui-component-form

[![Build Status](https://travis-ci.org/district0x/district-ui-component-form.svg?branch=master)](https://travis-ci.org/district0x/district-ui-component-form)

Reagent forms library

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
## Installation

Add to dependencies <br>

[![Clojars Project](https://img.shields.io/clojars/v/district0x/district-ui-component-form.svg)](https://clojars.org/district0x/district-ui-component-form)

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
