module Auth exposing (User, onPageLoad)

import Auth.Action
import Dict
import Route exposing (Route)
import Route.Path
import Shared
import Shared.Model


type alias User =
    Shared.Model.User


{-| Called before an auth-only page is loaded.
By default, all auth-only pages redirect users to the NotFound\_ page
when the application starts up. Edit this file so it automatically passes
the user to any pags that need it, but redirect to /sign-in if there's no user
log in.
-}
onPageLoad : Shared.Model -> Route () -> Auth.Action.Action User
onPageLoad shared route =
    case shared.user of
        Just user ->
            Auth.Action.loadPageWithUser user

        Nothing ->
            Auth.Action.pushRoute
                { path = Route.Path.SignIn

                -- In the case that a usr isn't sign in, we add a query parameter
                -- to let us know which page they were on when the sign-in redirect
                -- took place. This means that if user was signed out and loaded the
                -- /settings page, their url would be /sign-in?from=/settings
                , query =
                    Dict.fromList
                        [ ( "from", route.url.path )
                        ]
                , hash = Nothing
                }
