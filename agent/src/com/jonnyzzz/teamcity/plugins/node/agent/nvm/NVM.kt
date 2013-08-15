/*
 * Copyright 2013-2013 Eugene Petrenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jonnyzzz.teamcity.plugins.node.agent.nvm

import java.io.File
import org.apache.http.client.methods.HttpGet
import jetbrains.buildServer.RunBuildException
import com.jonnyzzz.teamcity.plugins.node.common.catchIO
import java.util.zip.ZipInputStream
import jetbrains.buildServer.util.FileUtil
import com.jonnyzzz.teamcity.plugins.node.common.div
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import com.jonnyzzz.teamcity.plugins.node.common.trimStart
import com.jonnyzzz.teamcity.plugins.node.common.log4j

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 05.08.13 9:35
 */
///https://github.com/creationix/nvm
///http://ghb.freshblurbs.com/blog/2011/05/07/install-node-js-and-express-js-nginx-debian-lenny.html
public class NVMDownloader(val http:HttpClientWrapper) {
  private val LOG = log4j(javaClass<NVMDownloader>())
  private val url = "https://github.com/creationix/nvm/archive/master.zip"

  private fun error(message:String, e:Throwable? = null) : Throwable {
    if (e == null) {
      throw RunBuildException("Failed to download NVM from ${url}. ${message}")
    } else {
      throw RunBuildException("Failed to download NVM from ${url}. ${message}. ${e.getMessage()}", e)
    }
  }

  public fun downloadNVM(dest : File) {
    http.execute(HttpGet(url)) {
      val status = getStatusLine()!!.getStatusCode()
      if (status != 200) throw error("${status} returned")
      val entity = getEntity()

      if (entity == null) throw error("No data was returned")
      val contentType = entity.getContentType()?.getValue()
      if ("application/zip" != contentType) throw error("Invalid content-type: ${contentType}")

      catchIO(ZipInputStream(entity.getContent()!!), {error("Failed to extract NVM", it)}) { zip ->
        FileUtil.createEmptyDir(dest)

        while(true) {
          val ze = zip.getNextEntry()
          if (ze == null) break
          if (ze.isDirectory()) continue
          val name = ze.getName().replace("\\", "/").trimStart("/").trimStart("nvm-master/")
          LOG.debug("nvm content: ${name}")

          if (name startsWith ".") continue
          if (name startsWith "test/") continue

          val file = dest / name
          catchIO(BufferedOutputStream(FileOutputStream(file)), {error("Failed to create ${file}", it)}) {
            zip.copyTo(it)
          }
        }
      }
    }
  }
}
