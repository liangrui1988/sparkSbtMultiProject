package cd.lxj.flow.utils

import java.sql.Timestamp
import java.text.SimpleDateFormat

import cd.lxj.flow.structured.order.orderEventByGId
import com.cd.core.constant.LogConstant
import com.cd.core.utils.SparkUtils
import org.apache.commons.lang3.StringUtils

import scala.util.parsing.json.JSON

/**
  * create by roy 2019/09/10
  */
object StructuredUtils {


  /**
    * src str 转 json
    *
    * @param line
    * @return
    */
  def convertSrcStrToJosn(line: String): String = {
    var jsonstr = ""
    try {
      if (StringUtils.isNotBlank(line)) {
        val grok = SparkUtils.getGrokInstance()
        val m = grok.`match`(line)
        m.captures
        jsonstr = m.toJson()
      }
    } catch {
      case e: java.lang.Exception => // Exception
        println("Exception==" + e)
    }
    jsonstr
  }

  val DataF = new SimpleDateFormat("yyyyMMdd")

  /**
    * json提取，外卖和堂食的
    *
    * @param line
    */
  def convertJosnToOrderStructured(line: String, timestamp: Timestamp): orderEventByGId = {
    var rtLine: orderEventByGId = null
    val jsonstr = convertSrcStrToJosn(line)
    if (StringUtils.isNotBlank(jsonstr)) {
      val rooMap = JSON.parseFull(jsonstr).get.asInstanceOf[Map[String, Any]]
      if (rooMap.contains("flag") && rooMap.contains("msg") && rooMap.contains("actionName") &&
        StringUtils.isNotBlank(rooMap.getOrElse("flag", "").toString) && rooMap.getOrElse("flag", 0).asInstanceOf[Double].toInt == 1) {
        val msg = rooMap.getOrElse("msg", "").toString
        val actionName = rooMap.getOrElse("actionName", "").toString
        if (StringUtils.isNotBlank(msg) && StringUtils.isNotBlank(actionName)) {
          if ("candao.order.postOrder".equals(actionName) || "candao.order.pushOrder".equals(actionName)) {
            val jsonMsg = "{" + msg.replaceFirst(LogConstant.dataExtractRegexStr, "")
            val jsonMap = JSON.parseFull(jsonMsg).get.asInstanceOf[Map[String, Any]]
            //              println("jsonMap==" + jsonMap)
            if (jsonMap.contains("data") && jsonMap.contains("data") != None && jsonMap.get("data").get.isInstanceOf[Map[String, Any]]) {
              var orderMoney = 0.0
              var otype = -1
              var orderId = ""
              var orderDate = "20180102"
              val msgData = jsonMap.get("data").get.asInstanceOf[Map[String, Any]]
              val actionName = jsonMap.getOrElse("actionName", "")
              if (actionName.equals("candao.order.postOrder")) {
                orderMoney = msgData.getOrElse("merchantPrice", 0.0).asInstanceOf[Double]
                otype = 1
                orderId = "1" + msgData.getOrElse("extId", "").toString
                orderDate = msgData.getOrElse("createTime", "20180101").toString.substring(0, 8)
              } else if (actionName.equals("candao.order.pushOrder")) {
                orderMoney = msgData.getOrElse("price", 0.0).asInstanceOf[Double] //price
                orderId = "2" + msgData.getOrElse("orderId", "").asInstanceOf[Double].toInt.toString
                orderDate = msgData.getOrElse("orderDate", "20180101").toString.substring(0, 10).replace("-", "")
                otype = 2
              }
              val structureTime = DataF.format(timestamp.getTime)
              if (structureTime.equals(orderDate)) {
                val extStoreId = msgData.getOrElse("extStoreId", "").toString
                //              val orderStatus = msgData.getOrElse("orderStatus", -899.0).asInstanceOf[Double].toInt
                +otype + "," + orderId + "," + orderId + ","
                val gId = extStoreId + "_" + otype + "_" + orderDate //storeid+otype+orderdate
                rtLine = orderEventByGId(gId, orderId, orderMoney)
              }
              //              }
            }
          }
        }
      }
    }
    //    println("rtLine==" + rtLine)
    rtLine
  }

  def main(args: Array[String]): Unit = {
    //    val srcStr = ""
    val srcStr =
      """
        |[2019-09-11 14:51:22.498] [INFO] [com.candao.gateway.common.request.BaseActionServlet] [service] [resin-port-8080-26849] [http://lxjapi.can-dao.com/ZtAction?data={"accessKey":"d50fc2c94fe2a16c","timestamp":1568184682391,"sign":"d2530c98054fe8d6f63003dab12c2dfe","actionName":"candao.order.postOrder","data":{"extId":"2019091112572313736132300223","thirdSn":"223","extOrderId":"","extStoreId":"5011","storeName":"南京市观竹苑店","shift":"早班","name":"","phone":"","memberId":"","address":"","tableNum":"","userNote":"","fromType":96,"type":5,"payType":1,"isPayed":true,"orderDate":"20190911","actionTime":20190911143143,"createTime":"20190911125723","paymentDetails":[{"money":22.0,"type":1,"typeName":"支付宝支付","paySub":"11311020"}],"totalPrice":22.0,"preferentialPrice":0.0,"merchantPrice":22.0,"thirdPreferKeyWords":[],"peopleNum":1,"orderStatus":100,"products":[{"extId":"1300000050","num":1.0,"price":2.0,"sharePrice":2.0,"name":"米饭"},{"extId":"1300000037","num":1.0,"price":7.0,"sharePrice":7.0,"name":"葱油菜苔"},{"extId":"1300000031","num":1.0,"price":13.0,"sharePrice":13.0,"name":"鸡汁辣鱼"}]}}] [0] [1] [1] [10.233.49.9] [124.250.35.102] [0] [false] [] [27ed6505-c6c4-4f07-a350-bb46ae9a51c6] [api-gateway] [candao.order.postOrder] [candao.orderApi.postOrder] []
      """.stripMargin
    val arr = srcStr.split("\\] \\[")
    //    println(arr)
    //    println(arr.size)
    //    var j = 0
    //    arr.foreach(x => {
    //      println(x + "=" + j)
    //      j += 1
    //    })
    val errr =
    """
      |[2019-09-12 11:11:44.834] [INFO] [com.candao.sd.service.SdService] [flowManage] [DubboServerHandler-10.233.49.13:20886-thread-469] [supplierSubscribe.timeType:0] [0] [5] [4] [10.233.49.13] [106.2.20.34] [0] [false] [] [8952b17f-a761-4af7-b7ee-7cff679c3afb] [sd] [candao.order.postOrder] [candao.orderApi.postOrder] []
    """.stripMargin
    //flag    1=8
    //candao.order.postOrder=16
    //
    val srcStr2 =
    """
      |[2019-09-11 14:51:22.498] [INFO] [com.candao.gateway.common.request.BaseActionServlet] [service] [resin-port-8080-26849] [http://lxjapi.can-dao.com/ZtAction?data={"accessKey":"d50fc2c94fe2a16c","timestamp":1568184682391,"sign":"d2530c98054fe8d6f63003dab12c2dfe","actionName":"candao.order.postOrder","data":{"extId":"2019091112572313736132300223","thirdSn":"223","extOrderId":"","extStoreId":"5011","storeName":"南京市观竹苑店","shift":"早班","name":"","phone":"","memberId":"","address":"","tableNum":"","userNote":"","fromType":96,"type":5,"payType":1,"isPayed":true,"orderDate":"20190911","actionTime":20190911143143,"createTime":"20190911125723","paymentDetails":[{"money":22.0,"type":1,"typeName":"支付宝支付","paySub":"11311020"}],"totalPrice":22.0,"preferentialPrice":0.0,"merchantPrice":22.0,"thirdPreferKeyWords":[],"peopleNum":1,"orderStatus":100,"products":[{"extId":"1300000050","num":1.0,"price":2.0,"sharePrice":2.0,"name":"米饭"},{"extId":"1300000037","num":1.0,"price":7.0,"sharePrice":7.0,"name":"葱油菜苔"},{"extId":"1300000031","num":1.0,"price":13.0,"sharePrice":13.0,"name":"鸡汁辣鱼"}]}}] [0] [1] [1] [10.233.49.9] [124.250.35.102] [0] [false] [] [27ed6505-c6c4-4f07-a350-bb46ae9a51c6] [api-gateway] [candao.order.postOrder] [candao.orderApi.postOrder] []
    """.stripMargin
    val s: Timestamp = new Timestamp(1568169410000L)
    val obj = convertJosnToOrderStructured(srcStr2, s)
    if (obj == null) {
      println("==null")
    }
    println(obj)
  }
}
