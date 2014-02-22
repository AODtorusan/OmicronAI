package be.angelcorp.omicron.base.configuration

import com.typesafe.config.Config

trait ConfigBuilder {

  def toConfig: Config

}
