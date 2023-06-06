(ns user-auth.core 
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.interceptor.chain :as interceptor.chain]
            [io.pedestal.interceptor.error :refer [error-dispatch]]
            [ring.util.http-response :as response]
            [buddy.auth :as auth]
            [buddy.auth.backends :as auth.backends]
            [buddy.auth.middleware :as auth.middleware]
            [buddy.core.nonce :as nonce]
            [buddy.core.codecs :as codecs]
            [muuntaja.interceptor :as mi]))

(def users 
  "A sample user store."
  {"aaron@example.com" {:name "Aaron Aardvark"
                        :username :aaron
                        :password "secret"
                        :role :user
                        :id "1"
                        :profileImageUrl "https://avatars.githubusercontent.com/u/6187256?v=4"}
   "gmw@example.com" {:name "Gerald M. Weinberg"
                      :email "gmw@example.com"
                      :username "gmw"
                      :password "rutabaga"
                      :role :admin
                      :id "2"}})

(defn random-token 
  []
  (let [random-data (nonce/random-bytes 16)]
    (codecs/bytes->hex random-data)))

(def tokens (atom {}))

(defn login 
  [{{:keys [email password]} :json-params}]
  (if (some-> users 
              (get-in [email :password])
              (= password))
    (let [token (random-token)]
      (swap! tokens assoc (keyword token) email)
      (response/ok {:token token}))
    (response/bad-request 
      {:errors [{:field "email"
                  :message "Incorrect Email or Password"}
                 {:field "password"
                  :message "Incorrect Email or Password"}]})))

(defn me 
  [{:keys [identity] :as request}]
  (if-not (auth/authenticated? request)
    (response/unauthorized 
      {:message "Token is required to access /api/me"})
    (response/ok {:id (get-in users [identity :id])
                  :name (get-in users [identity :name])
                  :profileImageUrl (get-in users [identity :profileImageUrl])
                  :email identity})))

(def token-auth-backend 
  "A budy-auth Token Authentication backend"
  (auth.backends/token {:authfn 
                        (fn [_request token] 
                            (when-let [email (get @tokens (keyword token))]
                              email))}))

(def authentication-interceptor 
  "Port of buddy-auth's wrap-authentication middleware."
  {:name ::authenticate 
   :enter (fn [context]
            (update context 
                    :request 
                    auth.middleware/authentication-request 
                    token-auth-backend))})

(defn authorization-interceptor 
  "Port of buddy-auth's wrap-autorization middleware"
  [backend]
  (error-dispatch [context ex]
                  [{:exception-type :clojure.lang.ExceptionInfo 
                    :stage :enter}]
                  (try 
                    (assoc context 
                           :response 
                           (auth.middleware/authorization-error 
                             (:request context)
                             ex backend))
                    (catch Exception e 
                      (assoc context ::interceptor.chain/error e)))
                  :else (assoc context ::interceptor.chain/error ex)))

(def common-interceptor 
  [(body-params/body-params)
   (mi/format-interceptor)
   authentication-interceptor 
   (authorization-interceptor token-auth-backend)])

(def routes 
  #{["/api/sign-in" :post (conj common-interceptor `login)]
    ["/api/me" :get (conj common-interceptor `me)]})



(comment 
  (keyword "aaron@example.com")
  (random-token)
  (get-in users ["aaron@example.com" :password]) 
  )
