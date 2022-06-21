package com

import akka.actor.ActorSystem
import akka.actor.Props

object Main {
  def main (args: Array[String]) : Unit = {
    println("Starrt")
    // creates actor system
    val system = ActorSystem("main")
    // val host = "localhost"
    // docker
    val host = "server"
    val port = 8080
    // create tcp actor tells actor to start connection
    val tcpActor = system.actorOf(TCPProducer.props(host, port), "TCPProducer")
    // creates new actor connector
    val connector = system.actorOf(Props(new Connector(tcpActor)), name = "connector")
    // tells actor to start working (receive messages from server)
    connector ! "first"
  }
}
