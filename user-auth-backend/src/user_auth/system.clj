(ns user-auth.system
  (:require [donut.system :as ds]
            [donut.system.repl :as dsr]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [user-auth.core :as core]))

(def routes core/routes)

(def service 
  "Prod Configuration"
  {:env :prod 
   ::http/routes routes 
   ::http/type :immutant
   ::http/port 5000})

(def dev-service 
  "Dev Configuration"
  (-> service 
      (merge {:env :dev 
              ::http/join? false 
              ::http/routes #(route/expand-routes (deref #'routes))
              ::http/allowed-origins {:cred true 
                                      :allowed-origins 
                                      (constantly true)}})
      http/default-interceptors
      http/dev-interceptors))

(def system 
  {::ds/defs 
   {:http 
    {:server #::ds{:start (fn [{:keys [::ds/config]}]
                            (http/start 
                              (http/create-server 
                                (:service-map config))))

                   :stop (fn [{:keys [::ds/instance]}]
                            (http/stop instance))

                   :config {:service-map service}}}}})


(defmethod ds/named-system :prod
  [_]
  system)

(defmethod ds/named-system :dev
  [_]
  (assoc-in system [::ds/defs 
                    :http 
                    :server 
                    ::ds/config 
                    :service-map] dev-service))


(comment 
  (dsr/start :dev)
  (dsr/stop)
  )
