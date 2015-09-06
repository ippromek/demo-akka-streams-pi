/*
 * Copyright 2015 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Attribution:
 * This code was inspired by – well, more or less copied from – the work of my mate Mathias Doenitz,
 * which can be found under https://github.com/sirthias/rs-comparison.
 */

package de.heikoseeberger.pi

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Broadcast, Flow, FlowGraph, Keep, Merge, Sink, Source, ZipWith }
import akka.stream.{ ActorMaterializer, Attributes }
import scala.concurrent.duration.DurationInt
import scala.util.{ Random, Try }

object PiApp {

  type Point = (Double, Double)

  sealed abstract class Sample
  object Sample {
    case object Inside extends Sample
    case object Outside extends Sample
  }

  case class State(nrOfInsideSamples: Long, nrOfSamples: Long) {

    def pi: Double = nrOfInsideSamples.toDouble / nrOfSamples * 4

    def next(sample: Sample): State = sample match {
      case Sample.Inside  => State(nrOfInsideSamples + 1, nrOfSamples + 1)
      case Sample.Outside => State(nrOfInsideSamples, nrOfSamples + 1)
    }
  }

  def isInside(point: Point): Boolean = {
    val (x, y) = point
    x * x + y * y < 1
  }

  def isOutside(point: Point): Boolean = !isInside(point)

  def main(args: Array[String]): Unit = {
    val nrOfSteps = Try(args(0).toInt).getOrElse(10)

    implicit val system = ActorSystem("pi-system")
    implicit val mat = ActorMaterializer()

    Source(newRandomDoubleIterator _)
      .grouped(2)
      .map { case Seq(x, y) => (x, y) }
      .via(toSample)
      .scan(State(0, 0))(_.next(_))
      .conflate(identity)(Keep.right)
      .via(onePerSecond)
      .map(state => f"After ${state.nrOfSamples}%,10d samples π is approximated as ${state.pi}%.6f")
      .take(nrOfSteps)
      .map(println)
      .runWith(Sink.onComplete(_ => system.terminate()))

    def newRandomDoubleIterator() = new Iterator[Double] {
      override def hasNext = true
      override def next() = Random.nextDouble()
    }

    def toSample = Flow() { implicit builder =>
      import FlowGraph.Implicits._

      val broadcast = builder.add(Broadcast[Point](2))
      val collectInside = builder.add(Flow[Point].filter(isInside).map(_ => Sample.Inside))
      val collectOutside = builder.add(Flow[Point].filter(isOutside).map(_ => Sample.Outside))
      val merge = builder.add(Merge[Sample](2))

      broadcast.out(0) ~> collectInside ~> merge.in(0)
      broadcast.out(1) ~> collectOutside ~> merge.in(1)

      (broadcast.in, merge.out)
    }

    // Set the input buffer size to 1 to avoid prefetchig State instances, drop the one that still gets prefetched
    def onePerSecond = Flow() { implicit builder =>
      import FlowGraph.Implicits._

      val ticks = builder.add(Source(1.second, 1.second, ()))
      val zip = builder.add(ZipWith[State, Unit, State](Keep.left).withAttributes(Attributes.inputBuffer(1, 1)))
      val dropOne = builder.add(Flow[State].drop(1))

      ticks ~> zip.in1
      zip.out ~> dropOne.inlet

      (zip.in0, dropOne.outlet)
    }
  }
}
