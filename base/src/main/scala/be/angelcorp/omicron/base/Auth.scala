package be.angelcorp.omicron.base

import com.lyndir.omicron.api.model.{Security, PlayerKey, Player}

class Auth(val player: Player, private val key: PlayerKey) {

  def apply[T]( body: => T ) = Auth.withSecurity(player, key)( body )

}

object Auth {

  def withSecurity[T]( player: Player, key: PlayerKey )( body: => T ) = {
    if (Security.isAuthenticatedAs(player)) {
      body
    } else {
      try {
        Security.authenticate(player, key)
        body
      } finally {
        Security.invalidate()
      }
    }
  }

}
