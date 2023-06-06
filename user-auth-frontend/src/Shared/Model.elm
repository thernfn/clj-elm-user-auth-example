module Shared.Model exposing (Model, User)

{-| -}


{-| Normally, this value would live in "Shared.elm"
but that would lead to a circular dependency import cycle.

For that reason, both `Shared.Model` and `Shared.Msg` are in their
own file, so they can be imported by `Effect.elm`

Maybe because it might be Nothing, because we don't always have a user token.

-}
type alias Model =
    { user : Maybe User
    }


type alias User =
    { token : String
    , id : String
    , name : String
    , profileImageUrl : String
    , email : String
    }
