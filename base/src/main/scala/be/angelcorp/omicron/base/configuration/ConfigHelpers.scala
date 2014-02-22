package be.angelcorp.omicron.base.configuration

import com.typesafe.config.{ConfigFactory, ConfigException, Config}

object ConfigHelpers {

  implicit class RichConfig(val underlying: Config) extends AnyVal {
    private def getOption[T]( f: => T ) =
      try {
        Some(f)
      } catch {
        case e: ConfigException.Missing   => None
        case e: ConfigException.WrongType => None
      }

    def getOptionalBoolean(path: String)    = getOption(underlying.getBoolean(path))
    def getOptionalNumber(path: String)     = getOption(underlying.getNumber(path))
    def getOptionalInt(path: String)        = getOption(underlying.getInt(path))
    def getOptionalLong(path: String)       = getOption(underlying.getLong(path))
    def getOptionalDouble(path: String)     = getOption(underlying.getDouble(path))
    def getOptionalString(path: String)     = getOption(underlying.getString(path))
    def getOptionalObject(path: String)     = getOption(underlying.getObject(path))
    def getOptionalConfig(path: String)     = getOption(underlying.getConfig(path))
    def getOptionalAnyRef(path: String)     = getOption(underlying.getAnyRef(path))
    def getOptionalValue(path: String)      = getOption(underlying.getValue(path))
    def getOptionalBytes(path: String)      = getOption(underlying.getBytes(path))
    def getOptionalMilliseconds(path: String) = getOption(underlying.getMilliseconds(path))
    def getOptionalNanoseconds(path: String)= getOption(underlying.getNanoseconds(path))
    def getOptionalList(path: String)       = getOption(underlying.getList(path))
    def getOptionalBooleanList(path: String)= getOption(underlying.getBooleanList(path))
    def getOptionalNumberList(path: String) = getOption(underlying.getNumberList(path))
    def getOptionalIntList(path: String)    = getOption(underlying.getIntList(path))
    def getOptionalLongList(path: String)   = getOption(underlying.getLongList(path))
    def getOptionalDoubleList(path: String) = getOption(underlying.getDoubleList(path))
    def getOptionalStringList(path: String) = getOption(underlying.getStringList(path))
    def getOptionalObjectList(path: String) = getOption(underlying.getObjectList(path))
    def getOptionalConfigList(path: String) = getOption(underlying.getConfigList(path))
    def getOptionalAnyRefList(path: String) = getOption(underlying.getAnyRefList(path))
    def getOptionalBytesList(path: String)  = getOption(underlying.getBytesList(path))
    def getOptionalMillisecondsList(path: String) = getOption(underlying.getMillisecondsList(path))
    def getOptionalNanosecondsList(path: String) = getOption(underlying.getNanosecondsList(path))
  }

  def mkConfig[K <: AnyRef](p: Iterable[(String, K)]): Config = {
    val properties = new java.util.HashMap[String, K]()
    p.foreach( e => properties.put(e._1, e._2) )
    properties.put("type", getClass.getSimpleName.asInstanceOf[K])
    ConfigFactory.parseMap( properties )
  }

  def mkConfig[K <: AnyRef](param: (String, K)*): Config = mkConfig(param)

}
