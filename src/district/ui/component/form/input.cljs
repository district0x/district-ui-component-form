(ns district.ui.component.form.input
  (:require [clojure.set :refer [rename-keys]]
            [clojure.string :as str]
            [reagent.core :as r])
  (:require-macros [reagent.ratom :refer [reaction]]))

(declare chip-input)

(defn keys-in [m]
  (if (map? m)
    (vec
     (mapcat (fn [[k v]]
               (let [sub (keys-in v)
                     nested (map #(into [k] %) (filter (comp not empty?) sub))]
                 (if (seq nested)
                   nested
                   [[k]])))
             m))
    []))

(defn index-by-type [m k]
  (let [ks (keys-in m)
        f-ks (filter #(= (last %) k) ks)]
    (map (fn [i]
           [(butlast i) (get-in m i)]) f-ks)))

(def arg-keys [:id :form-data :errors :on-change :attrs :group-class])

(defn get-by-path
  ([doc path]
   (get-by-path doc path nil))
  ([doc path default]
   (let [n-path (flatten
                 (if-not (seq? path)
                   [path]
                   path))]
     (get-in doc n-path default))))

(defn get-in-errvec [doc path]
  (some (fn [e]
          (when (= (:field e)
                   path)
            (:message e)))
        doc))

(defn assoc-by-path
  [doc path value]
  (let [n-path (flatten
                (if-not (seq? path)
                  [path]
                  path))]
    (assoc-in doc n-path value)))

(defn update-by-path
  [doc path fn]
  (let [n-path (flatten
                (if-not (seq? path)
                  [path]
                  path))]
    (update-in doc n-path fn)))

(defn with-label [label body {:keys [:group-class :form-data :id :for]}]
  (let [filled? (when (and form-data
                           id)
                  (not (or
                         (nil? (get-by-path @form-data id))
                         (= "" (get-by-path @form-data id))
                         )))]
    [:div.labeled-input-group
     {:class (str (when group-class (name group-class))
                  (when filled? " filled")
                  (when (= (first body)
                           chip-input) " tall-version"))}
     [:label (when id {:for for}) label]
     body]))

(defn id-for-path [path]
  (if (sequential? path)
    (str/join "-" (map name path))
    (name path)))

(defn err-reported [{:keys [id form-data errors on-change group-class] :as opts} cmp]
  (let []
    (fn [{:keys [id form-data errors on-change] :as opts}]
      (let [on-touched (fn [new-val]
                         (let [old-meta (or (meta @form-data)
                                            {})
                               meta-with-touched (assoc old-meta :touched? true)]
                           (reset! form-data (with-meta @form-data meta-with-touched))
                           (when on-change
                             (on-change new-val))))
            touched? (:touched? (meta @form-data))
            errors (if (satisfies? IAtom errors)
                     @errors
                     errors)
            err (if-let [e (and
                            touched?
                            (or
                             (get-by-path errors [:local id])
                             (get-in-errvec (:local errors) id)))]
                  e
                  (when-let [e (and (not touched?)
                                    (get-by-path errors [:remote id]))]
                    e))
            err-map (if (map? err)
                      err
                      (when err
                        {:error err}))]
        [:div.input-group
         {:class (str (when group-class (name group-class))
                      (cond (:error err-map) " has-error"
                            (:warning err-map) " has-warning"
                            (:hint (get-by-path errors [:local id])) " has-hint"))}
         [cmp (assoc opts :on-change on-touched)]
         [:span.help-block  (get err-map :error
                                 (get err-map :warning
                                      (:hint (get-by-path errors [:local id]))))
          [:div {:dangerouslySetInnerHTML {:__html "&nbsp;"}}]]]))))


(defn text-input* [{:keys [id form-data errors on-change attrs input-type dom-id] :as opts}]
  (fn [{:keys [id form-data errors on-change attrs input-type] :as opts}]
    (let [a (if (= input-type :textarea)
              :textarea
              :input)
          other-opts (apply dissoc opts (conj arg-keys :input-type :dom-id))]
      [a (merge
          {:type "text"
           :id dom-id
           :value (get-by-path @form-data id "")
           :on-change #(let [v (-> % .-target .-value)]
                         (swap! form-data assoc-by-path id v)
                         (when on-change
                           (on-change v)))}
          other-opts
          attrs)])))

(defn text-input [opts]
  [err-reported opts text-input*])

(defn textarea-input* [opts]
  [text-input* (merge opts
                      {:input-type :textarea})])

(defn textarea-input [opts]
  [err-reported opts textarea-input*])

(defn select-input* [{:keys [id form-data errors on-change attrs options] :as opts}]
  (fn [{:keys [id form-data errors on-change attrs options] :as opts}]
    (let [other-opts (apply dissoc opts (conj arg-keys :options))]
      [:select
       (merge
        {:on-change (fn [item]
                      (let [val (.-target.value item)
                            iv (and (re-matches #"^\d*(\.|\.)?\d*$" val)
                                    (js/parseFloat val))
                            val (if
                                    (or
                                     (nil? iv)
                                     (js/isNaN iv))
                                  val
                                  iv)
                            label (-> (.-target.selectedOptions item)
                                        (aget 0)
                                        .-innerHTML)]
                        (swap! form-data assoc-by-path id val)
                        (when on-change
                          (on-change {:value val :label label}))))
         :value (get-by-path @form-data id)}
        other-opts
        attrs)

       (doall
        (map (fn [option]
               ^{:key (str (:key option))}
               [:option (rename-keys option {:key :value})
                (:value option)])
             options))])))

(defn select-input [{:keys [id form-data errors] :as opts}]
  [err-reported opts select-input*])

(defn checkbox-input* [{:keys [id form-data errors on-change attrs] :as opts}]
  (fn [{:keys [id form-data errors on-change attrs] :as opts}]
    (let [other-opts (apply dissoc opts (conj arg-keys :options))]
      [:input (merge
               {:type "checkbox"
                :checked (get-by-path @form-data id "")
                :on-change #(let [v (-> % .-target .-value)]
                              (swap! form-data update-by-path id not)
                              (when on-change
                                (on-change v)))}
               other-opts
               attrs)])))

(defn checkbox-input [{:keys [id form-data errors] :as opts}]
  [err-reported opts checkbox-input*])

(defn autocomplete-input* [{:keys [form-data id ac-options on-option-selected on-empty-backspace on-new-option select-keycodes] :as opts}]
  (let [selected-idx (r/atom 0)]
    (fn [{:keys [form-data id ac-options on-option-selected on-empty-backspace on-new-option select-keycodes]}]
      (let [other-opts (apply dissoc opts [:ac-options :on-option-selected :on-empty-backspace :on-new-option :select-keycodes])
            txt-id (str "txt-" id)
            clear-input! #(swap! form-data assoc txt-id "")
            select-opt (fn [o]
                         (clear-input!)
                         (reset! selected-idx 0)
                         (on-option-selected o))
            selectable-opts (let [input (get @form-data txt-id)]
                              (when (not-empty input)
                                (filter #(str/starts-with? % input) ac-options)))
            key-up-handler (fn [e]
                             (let [key-code (-> e .-keyCode)
                                   input (get @form-data txt-id)]

                               (cond
                                 (and (= key-code 8) ;; backspace key
                                      (empty? input))
                                 (on-empty-backspace)

                                 ((or select-keycodes #{13}) key-code) ;; "return" key
                                 (if (not-empty selectable-opts)
                                   (select-opt (nth selectable-opts @selected-idx)) ;; [return] over option
                                   (when-not (empty? input)
                                     (on-new-option (if (not= key-code 13)
                                                      (subs input 0 (dec (count input)))
                                                      input))
                                     (clear-input!))) ;; [return] with some text that is not a option

                                 (= key-code 40) ;; down key
                                 (swap! selected-idx #(min (inc %) (dec (count selectable-opts))))

                                 (= key-code 38) ;; up key
                                 (swap! selected-idx #(max (dec %) 0)))))]
        [:div.autocomplete-input
         [text-input* {:form-data form-data
                       :id txt-id
                       :on-key-up key-up-handler}]
         (when (not-empty selectable-opts)
           [:ol.options
            (doall
             (map-indexed
              (fn [idx opt]
                ^{:key opt}
                [:li.option {:class (when (= idx @selected-idx) "selected")
                             ;;:style (when (= idx @selected-idx) {:background-color "red"})
                             ;;for testing only
                             :on-click #(select-opt opt)}
                 opt])
              selectable-opts))])]))))

(defn autocomplete-input [{:keys [id form-data errors] :as opts}]
  [err-reported opts autocomplete-input*])

(defn chip-input* [{:keys [form-data chip-set-path ac-options chip-render-fn on-change select-keycodes id] :as opts}]
  (let [focus (r/atom false)]
    (fn [{:keys [form-data chip-set-path ac-options chip-render-fn on-change] :as opts}]
      ;;TODO: chip-set-path should be just id
      (let [other-opts (apply dissoc opts [:chip-set-path
                                           :ac-options
                                           :chip-render-fn
                                           :on-change
                                           :on-empty-backspace
                                           :on-focus
                                           :on-blur
                                           :select-keycodes])
            add-chip-fn (fn [chip]
                          (swap! form-data update-in chip-set-path (fn [cs] (conj (or cs []) chip)))
                          (when on-change (on-change)))]
        [:div.chip-input
         {:class (when @focus "focused")}
         [:ol.chips
          (for [c (get-in @form-data chip-set-path)]
            ^{:key c}
            [:li.chip
             (chip-render-fn c)
             [:span {:on-click (fn []
                                 (swap! form-data update-in chip-set-path (fn [cs] (remove #{c} cs)))
                                 (when on-change (on-change)))}
              "X"]])]
         [autocomplete-input* (merge
                               {:form-data form-data
                                :ac-options (->> ac-options
                                                 (remove (set (get-in @form-data chip-set-path)))
                                                 (filter #(not= % nil))
                                                 (into []))
                                :on-option-selected add-chip-fn
                                :on-new-option add-chip-fn
                                :on-focus #(reset! focus true)
                                :on-blur #(reset! focus false)
                                :select-keycodes select-keycodes
                                :on-empty-backspace #(do (swap! form-data update-in chip-set-path butlast)
                                                         (when on-change (on-change)))}
                               other-opts)]]))))


(defn chip-input [opts]
  [err-reported opts chip-input*])

(def empty-img-src "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=")


(defn file-drag-input* [{:keys [form-data id file-accept-pred on-file-accepted on-file-rejected comment]
                         :or {file-accept-pred (constantly true)}}]
  (let [allow-drop #(.preventDefault %)
        handle-files-select (fn [files]
                              (if-let [f (aget files 0)]
                                (let [fprops {:name (.-name f)
                                            :type (.-type f)
                                            :size (.-size f)
                                            :file f}]
                                    (if (file-accept-pred fprops)
                                      (let [url-reader (js/FileReader.)
                                            ab-reader (js/FileReader.)]
                                        (set! (.-onload url-reader) (fn [e]
                                                                      (let [img-data (-> e .-target .-result)
                                                                            fmap (assoc fprops :url-data img-data)]
                                                                        (swap! form-data assoc-in [id :selected-file] fmap))))
                                        (.readAsDataURL url-reader f)
                                        (set! (.-onload ab-reader) (fn [e]
                                                                     (let [img-data (-> e .-target .-result)
                                                                           fmap (assoc fprops :array-buffer img-data)]
                                                                       (swap! form-data update id merge fmap)
                                                                       (when on-file-accepted (on-file-accepted fmap)))))
                                        (.readAsArrayBuffer ab-reader f))
                                      (when on-file-rejected
                                        (on-file-rejected fprops))))))]
    (fn [{:keys [form-data id file-accept-pred on-file-accepted on-file-rejected comment]
         :as opts
         :or {file-accept-pred (constantly true)}}]
      (let [{:keys [name url-data]} (get-in @form-data [id :selected-file])]
        [:div.dropzone
         {:on-drag-over allow-drop
          :on-drop #(do
                      (.preventDefault %)
                      (handle-files-select (.. % -dataTransfer -files)))
          :on-drag-enter allow-drop}
         [:img {:src (or url-data empty-img-src)}]
         [:span.file-name name]
         (when-not (empty? comment) [:span.file-comment comment])
         [:label.file-input-label
          {:for (id-for-path id)}
          (get opts :label "File...")]
         [:input {:type :file
                  :id (id-for-path id)
                  :on-change (fn [e]
                               (handle-files-select (-> e .-target .-files)))}]
         ]))))

(defn file-drag-input [opts]
  [err-reported opts file-drag-input*])

(defn int-input* [{:keys [id form-data errors on-change attrs] :as opts}]
  (let [fallback (r/atom nil)]
    (fn [{:keys [id form-data errors on-change attrs] :as opts}]
      (let [other-opts (apply dissoc opts arg-keys)]
        [:input (merge
                 {:type "text"
                  :value (if-let [f @fallback]
                           f
                           (get-by-path @form-data id ""))
                  :on-change #(let [v (-> % .-target .-value)]
                                (when-let [iv (and (re-matches #"^\d*$" v)
                                                   (js/parseInt v))]
                                  (if-not (js/isNaN iv)
                                    (do
                                      (reset! fallback v)
                                      (when on-change
                                        (on-change iv))
                                      (swap! form-data assoc-by-path id iv))
                                    (do
                                      (swap! form-data assoc-by-path id nil)
                                      (reset! fallback v)))))}
                 other-opts
                 attrs)]))))

(defn int-input [{:keys [id form-data errors] :as opts}]
  [err-reported opts int-input*])

(defn amount-input* [{:keys [id form-data errors on-change attrs] :as opts}]
  (let [fallback (r/atom nil)
        last-input (r/atom nil)]
    (fn [{:keys [id form-data errors on-change attrs] :as opts}]
      (let [other-opts (apply dissoc opts arg-keys)]
        [:input (merge
                 {:type "text"
                  :value (if-let [f @fallback]
                           (if (= @last-input
                                  (get-by-path @form-data id 0))
                             f
                             (get-by-path @form-data id 0))
                           (get-by-path @form-data id 0))
                  ;; :value (get-by-path @form-data id 0)
                  :on-change #(let [v (-> % .-target .-value)]
                                (reset! last-input v)
                                (when-let [iv (and (re-matches #"^\d*(\.|\.)?\d*$" v)
                                                   (js/parseFloat v))]
                                  (if-not (js/isNaN iv)
                                    (do
                                      (reset! fallback v)
                                      (when on-change
                                        (on-change iv))
                                      (reset! last-input iv)
                                      (swap! form-data assoc-by-path id iv))
                                    (do
                                      (reset! last-input nil)
                                      (swap! form-data assoc-by-path id nil)
                                      (reset! fallback v)))))}
                 other-opts
                 attrs)]))))

(defn amount-input [{:keys [id form-data errors] :as opts}]
  [err-reported opts amount-input*])

(defn pending-button [{:keys [:pending? :pending-text] :as opts
                       :or {pending-text "Sending..."}} & children]
  (let [other-opts (dissoc opts :pending? :pending-text)]
    (into
     [:button (merge
              {}
              (when pending?
                {:disabled true})
              other-opts)]
     (if pending?
       [pending-text]
       children))))
