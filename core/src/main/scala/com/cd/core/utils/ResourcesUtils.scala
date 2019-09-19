package com.cd.core.utils

import java.util.Properties
/**
  * create by roy 2019/09/10
  */
object ResourcesUtils {


  def getMysqlSettings(user: String = "user", passwd: String = "passwd", url: String = "url", filename: String = "localmysql.properties"): (String, String, String) = {
    val prop = new Properties()
    val inputStream = this.getClass.getClassLoader.getResourceAsStream(filename)
    prop.load(inputStream)
    (prop.getProperty(user), prop.getProperty(passwd), prop.getProperty(url))
  }

  def getSettings(): Properties = {
    val prop = new Properties()
    val inputStream = this.getClass.getClassLoader.getResourceAsStream("settings.properties")
    prop.load(inputStream)
    prop
  }


}
