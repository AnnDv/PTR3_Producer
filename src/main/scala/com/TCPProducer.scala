package com

import java.net.InetSocketAddress
import akka.actor.{Actor, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import play.api.libs.json._

// create socket address
object TCPProducer {
  var prop : Props = _
  def props(host :String, port :Int) = {
    if(prop == null) prop = Props(classOf[TCPProducer], new InetSocketAddress(host,port))
    prop
  }
}

class TCPProducer(remote: InetSocketAddress) extends Actor{
  import akka.io.Tcp._
  import context.system

  println("Connecting to " +  remote.toString)

  // send connect message to the tcp manager
  val manager = IO(Tcp)
  manager ! Connect(remote)

  override def receive: Receive = {
    case Received(data) => {
    //   println(data)
    }
    case CommandFailed(con: Connect) =>
      println("Connection failed")
      println(con.failureMessage.toString)
      context stop self
    

    case c @ Connected(remote, local) =>
      println(s"Connection to $remote succeeded")
      val connection = sender
      connection ! Register(self)

      context.become {
        case (data : String, id : String) =>
            // println(data)
            val command = "sendFromProducer"
            val tuples = Seq(("command", command), ("data", data), ("id", id))
            
            // prepares object producerCommand to be transformed in JSON (beacause JSON parser can't transform simple object to JSON)
            implicit val commandWrites = new Writes[ProducerCommand] {
                def writes(producerCommand: ProducerCommand) = Json.obj(
                    "command"  -> producerCommand.command,
                    "data" -> producerCommand.data,
                    "id" -> producerCommand.id
                    )
                }
            // transforms producerCommand to JSON
            val commandObject = new ProducerCommand(command, data, id)
            val json = Json.toJson(commandObject)
            // println("Sending message: " + json)
            connection ! Write(ByteString(Json.stringify(json)))

        case CommandFailed(w: Write) =>
          println("Failed to write request.")
          println(w.failureMessage.toString)

        case Received(data) =>
          println("Received response: " + data.utf8String)

        case "close" =>
          println("Closing connection")
          connection ! Close

        case _: ConnectionClosed =>
          println("Connection closed by server.")
          context stop self
       }
  }
}