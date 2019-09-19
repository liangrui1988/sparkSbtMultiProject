package cd.lxj.flow.structured.order

import java.sql.Timestamp
import java.util.concurrent.TimeUnit

import cd.lxj.flow.utils.{OrderJDBCSink, StructuredUtils}
import com.cd.core.streaming.StreamingLocal
import com.cd.core.utils.ResourcesUtils
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.current_timestamp
import org.apache.spark.sql.streaming._

/**
  * create by Roy 2019/09/06
  * Counting day order number and amount
  */
object OrderStructuredJob {
  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
  StreamingLocal.setStreamingLogLevels()

  def main(args: Array[String]): Unit = {
    //    if (args.length < 3) {
    //      System.err.println("Usage: StructuredKerberizedKafkaWordCount <bootstrap-servers> " +
    //        "<subscribe-type> <topics> [<checkpoint-location>]")
    //      System.exit(1)
    //    }
    //    val Array(bootstrapServers, subscribeType, topics, _*) = args
    var bootstrap_servers = "192.168.7.14:9092,192.168.7.14:9093,192.168.7.15:9092"
    val checkpointLocation = "/scheckpoint/structuredstoreorderstate"
    val spark = SparkSession
      .builder
      //.master("local[*]")
      .appName("StructuredStoreOrderState")
      .getOrCreate()
    import spark.implicits._
    // Create DataSet representing the stream of input lines from kafka
    val lines = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", bootstrap_servers)
      .option("subscribe", "zt_log") //"subscribe"
      //      .option("startingOffsets", "latest") //	earliest, latest, or json string {"topicA":{"0":23,"1":-1},"topicB":{"0":-2}}
      //      .option("kafkaConsumer.pollTimeoutMs", "50000") //512
      //      .option("kafka.isolation.level", "read_committed") //读事务消息 事务隔离
      //      .option("failOnDataLoss", "false")
      //      .option("kafka.security.protocol", SecurityProtocol.PLAINTEXT.name)
      .load()
      .selectExpr("CAST(value AS STRING)")
      .withColumn("current_timestamp", current_timestamp)
//      .as[String]

    val events = lines
      .as[(String, Timestamp)]
      .map { case (line, timestamp) => {
        StructuredUtils.convertJosnToOrderStructured(line, timestamp)
      }
      }.filter(obj => obj != null)
    val orderUpdates = events
      .groupByKey(event => event.gId)
      .mapGroupsWithState[storeOrderState, storeAggUpdate](GroupStateTimeout.ProcessingTimeTimeout) {
      case (key: String, values: Iterator[dataEvent], state: GroupState[storeOrderInfoState]) =>
        val seqs = values.toSeq
        if (state.hasTimedOut) {
          val finalUpdate =
            storeAggUpdate(key, state.get.orderNum, state.get.orderMoney, expired = true)
          state.remove()
          finalUpdate
        } else {
          val updatedSession = if (state.exists) {
            val stateMap = state.get.orderInfoStoreMap
            var norderMap2: Map[String, Double] = Map()
            var num = 0
            var money = 0.0
            seqs.foreach(e => {
              if (stateMap.contains(e.orderId)) { //新订单-旧订单，再进行+总合
                money += e.money - stateMap.get(e.orderId).get
              } else if (norderMap2.contains(e.orderId)) { //同一批次下，有重复订单的情况
                money += e.money - norderMap2.get(e.orderId).get
              } else {
                num += 1
                money += e.money
              }
              norderMap2 += (e.orderId -> e.money)
            })
            //取出所有的订单+流进来的订单，需要判断是否有重复订单
            storeOrderState(key, state.get.orderNum + num, state.get.orderMoney + money, stateMap ++ norderMap2)
          } else {
            var norderMap2: Map[String, Double] = Map()
            var money = 0.0
            seqs.foreach(e => {
              if (norderMap2.contains(e.orderId)) {
                //同一批次下，有重复订单的情况
                money += e.money - norderMap2.get(e.orderId).get
              } else {
                money += e.money
              }
              norderMap2 += (e.orderId -> e.money)
            })
            storeOrderState(key, norderMap2.size, money, norderMap2)
          }
          //更新缓存里面的这条数据信息
          state.update(updatedSession)
          // Set timeout such that the session will be expired if no data received for 10 seconds
          state.setTimeoutDuration("24 hour")
          storeAggUpdate(key, state.get.orderNum, state.get.orderMoney, expired = false)
        }
    }
    import org.apache.spark.sql.functions.{lit, udf}
    val storeIdCode = (gid: String, index: Int) => {
      gid.split("_")(index)
    }
    val storeidUDF = udf(storeIdCode)
    import spark.implicits._
    val resultDF = orderUpdates.withColumn("storeId", storeidUDF(orderUpdates("gId"), lit(0))).
      withColumn("otype", storeidUDF(orderUpdates("gId"), lit(1))).
      withColumn("orderDate", storeidUDF(orderUpdates("gId"), lit(2))).drop("gId").drop("timestamp")

    val (user, passwd, url) = ResourcesUtils.getMysqlSettings("lxj_ssd_bi_user", "lxj_ssd_bi_passwd", "lxj_ssd_bi_url")
    val writer = new OrderJDBCSink(url, user, passwd)
    //    resultDF.printSchema()
    val query =
      resultDF
        .writeStream
        .foreach(writer)
        .outputMode("update")
        .option("checkpointLocation", checkpointLocation)
         .trigger(Trigger.ProcessingTime(5 * 1000))
//        .trigger(Trigger.Continuous(10, TimeUnit.SECONDS)) // only change in query 连续处理流数据的触发器 "10 seconds"
        .start()

    query.awaitTermination()


  }

}


case class orderEventByGId(gId: String, orderId: String, money: Double)

//门店里面，维护一张所有订单
case class storeOrderState(storeId: String, orderNum: Int, orderMoney: Double, orderInfoStoreMap: Map[String, Double])

//返回计算后的结果
case class storeAggUpdate(gId: String, orderNum: Int, orderMoney: Double,
                          expired: Boolean)






