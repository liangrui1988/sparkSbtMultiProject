package com.cd.core.utils

import java.io.InputStreamReader
import com.cd.core.constant.LogConstant
import io.thekraken.grok.api.Grok
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

/**
  * create roy by 2019/09/19
  * spark utils
  */
object SparkUtils {
  @transient private var instance: SparkSession = _
  @transient private var grok: Grok = _

  def getInstance(sparkConf: SparkConf): SparkSession = {
    if (instance == null) {
      instance = SparkSession
        .builder
        .enableHiveSupport()
        .master("yarn")
        .appName("SparkSessionSingleton")
        .config(sparkConf)
        //        .config("spark.files.openCostInBytes", PropertyUtil.getInstance().getProperty("spark.files.openCostInBytes"))  
        //         .config("hive.metastore.uris","thrift://namenode01.cd:9083")////連接到hive元數據庫   --files hdfs:///user/processuser/hive-site.xml 集群上運行需要指定hive-site.xml的位置   
        //        .config("spark.sql.warehouse.dir","hdfs://namenode01.cd:8020/user/hive/warehouse")
        .getOrCreate()
    }
    instance
  }


  def getGrokInstance(): Grok = {
    if (grok == null) {
      grok = new Grok()
      val inputStream = this.getClass.getClassLoader.getResourceAsStream("patterns.txt")
      grok.addPatternFromReader(new InputStreamReader(inputStream))
      grok.compile(LogConstant.pattern)
    }
    grok
  }
}
