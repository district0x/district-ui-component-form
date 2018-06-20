(def project 'district0x/district-ui-component-form)
(def version "0.1.2-SNAPSHOT")

(set-env! :resource-paths #{"resources" "src"}
          :source-paths   #{"test"}
          :dependencies   '[[org.clojure/clojure "RELEASE"]
                            [org.clojure/clojurescript "1.10.238"]
                            [adzerk/boot-test "RELEASE" :scope "test"]
                            [adzerk/boot-cljs          "2.1.4"  :scope "test"];;:exclusions [org.clojure/clojurescript]
                            [adzerk/bootlaces    "0.1.13" :scope "test"]
                            [reagent "0.7.0" :exclusions [cljsjs/react]]
                            [cljsjs/react-with-addons "15.4.2-2"]
                            ])

(task-options!
 pom {:project     project
      :version     version
      :description "Forms library"
      :url         "https://github.com/district0x/distrirct-ui-component-form"
      :scm         {:url "https://github.com/github.com/district-ui-component-form"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask build
  "Build and install the project locally."
  []
  (comp (pom) (jar) (install)))

(require '[adzerk.boot-test :refer [test]]
         '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.bootlaces :refer :all])

(bootlaces! version)

(deftask production []
  (task-options! cljs {:optimizations :advanced})
  identity)

(deftask deploy []
  (comp
   (production)
   (cljs)
   (build-jar)
   (push-snapshot)))
