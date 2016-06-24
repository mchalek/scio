/*
 * Copyright 2016 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.scio.examples.extra

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions
import org.apache.beam.sdk.{Pipeline, PipelineResult}
import org.apache.beam.sdk.coders.{AvroCoder, DoubleCoder, KvCoder, StringUtf8Coder}
import org.apache.beam.sdk.io.PubsubIO
import org.apache.beam.sdk.transforms.windowing._
import org.apache.beam.sdk.transforms.{PTransform, Sum}
import org.apache.beam.sdk.values._
import com.spotify.scio._
import com.spotify.scio.avro.Account
import com.spotify.scio.values.SCollection
import org.joda.time.Duration

// Use native Dataflow Java SDK code inside a Scio job
object DataflowExample {

  // A native Dataflow source PTransform
  def pubsubIn(topic: String): PTransform[PInput, PCollection[Account]] =
    PubsubIO.Read.named("PubsubIn")
      .topic(topic)
      .withCoder(AvroCoder.of(classOf[Account]))

  // A native Dataflow windowing PTransform
  val window: PTransform[PCollection[Account], PCollection[Account]] =
    Window.named("Window")
      .into[Account](FixedWindows.of(Duration.standardMinutes(60)))
      .triggering(
        AfterWatermark
          .pastEndOfWindow()
          .withEarlyFirings(
            AfterProcessingTime
              .pastFirstElementInPane()
              .plusDelayOf(Duration.standardMinutes(5)))
          .withLateFirings(
            AfterProcessingTime
              .pastFirstElementInPane()
              .plusDelayOf(Duration.standardMinutes(10))))
      .accumulatingFiredPanes()

  // A native Dataflow aggregation PTransform
  val sumByKey = Sum.doublesPerKey[String]()

  // A native Dataflow sink PTransform
  def pubsubOut(topic: String): PTransform[PCollection[KV[String, java.lang.Double]], PDone] =
    PubsubIO.Write.named("PubsubOut")
      .topic(topic)
      .withCoder(KvCoder.of(StringUtf8Coder.of(), DoubleCoder.of()))

  // scalastyle:off regex
  def main(cmdlineArgs: Array[String]): Unit = {
    // Parse command line arguments and create Dataflow specific options plus application specific
    // arguments.
    // opts: DataflowPipelineOptions - Dataflow specific options
    // args: Args - application specific arguments
    val (opts, args) = ScioContext.parseArguments[DataflowPipelineOptions](cmdlineArgs)

    val sc = ScioContext.apply(opts)

    // Underlying Dataflow pipeline
    val pipeline: Pipeline = sc.pipeline
    println(pipeline.getRunner)

    // Apply a native source PTransform and get a Scio SCollection
    val accounts: SCollection[Account] = sc.applyTransform(pubsubIn(args("inputTopic")))

    // Underlying Dataflow PCollection
    val p: PCollection[Account] = accounts.internal
    println(p.getName)

    accounts
      // Dataflow PTransform
      .applyTransform(window)
      // Scio transform
      .map(a => KV.of(a.getName.toString, a.getAmount))
      // Dataflow PTransform
      .applyTransform(sumByKey)
      // Dataflow PTransform of PCollection -> PDone
      .applyOutputTransform(pubsubOut(args("outputTopic")))

    val result = sc.close()

    // Underlying Dataflow pipeline result
    val pipelineResult: PipelineResult = result.internal
    println(pipelineResult.getState)
  }
  // scalastyle:on regex

}
