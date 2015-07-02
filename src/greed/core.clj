(ns greed.core
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as str])
  (:gen-class))

(def ^:dynamic *base-url* "http://www.mrporter.com")

(def ^:dynamic *common-projects-sale* "/mens/sale/designers/common_projects/all/")

(def ^:dynamic *selectors*
  {:products  [:div#product-list :div.description]
   :designer  [:span.product-designer]
   :title     [:span.product-title]
   :pre-sale  [:p.pre-sale-price]
   :post-sale [:p.sale-price :span.price-value]
   :percent   [[:p.sale-price (html/nth-child 3)]]
   :url       [:div.designer :a]})

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn split-on-space [word]
  (clojure.string/split word #"\s+"))

(defn squish [line]
  (str/triml (str/join " "
     (split-on-space (str/replace line #"\n" " ")))))

(defn extract [node]
  (let [designer  (first (html/select [node] (:designer  *selectors*)))
        title     (first (html/select [node] (:title     *selectors*)))
        pre-sale  (first (html/select [node] (:pre-sale  *selectors*)))
        post-sale (first (html/select [node] (:post-sale *selectors*)))
        percent   (first (html/select [node] (:percent   *selectors*)))
        url       (first (html/select [node] (:url       *selectors*)))
        result (conj
                 (map html/text [designer title pre-sale post-sale percent])
                 (str *base-url* (:href (:attrs url))))]
    (zipmap [:url :designer :title :pre-sale :post-sale :percent] (map squish result))))

(defn products []
  (html/select (fetch-url (str *base-url* *common-projects-sale*)) (:products *selectors*)))

(defn print-product [product]
  (println)
  (println (product :designer "No designer"))
  (println "\t" (product :title "No title"))
  (println "\t" (product :pre-sale "No pre-sale"))
  (println "\t" (product :post-sale "No post-sale"))
  (println "\t" (product :percent "No percent"))
  (println "\t" (product :url "No url")))

(defn print-products []
  (doseq [product (map extract (products))]
    (print-product product)))

(defn schedule [f ms]
    (future (while true (do (Thread/sleep ms) (f)))))

(defn -main [& args]
  (schedule print-products (* 1000 60 10)))
