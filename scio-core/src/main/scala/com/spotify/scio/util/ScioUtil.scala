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

package com.spotify.scio.util

import java.net.URI

import com.google.protobuf.{GeneratedMessage, Message, Parser}

import scala.reflect.ClassTag

private[scio] object ScioUtil {

  def isLocalUri(uri: URI): Boolean = uri.getScheme == null || uri.getScheme == "file"

  def isGcsUri(uri: URI): Boolean = uri.getScheme == "gs"

  def classOf[T: ClassTag]: Class[T] = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]

  def getProtobufParser[T: ClassTag](implicit ev: T <:< GeneratedMessage with Message): Parser[T] =
    classOf[T].getDeclaredField("PARSER").get(null).asInstanceOf[Parser[T]]
}
