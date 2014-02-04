/*
 * BridJ - Dynamic and blazing-fast native interop for Java.
 * http://bridj.googlecode.com/
 *
 * Copyright (c) 2010-2013, Olivier Chafik (http://ochafik.com/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package be.angelcorp.omicronai

import java.io.File
import java.nio.file.{StandardCopyOption, Path, Paths, Files}
import java.util.MissingResourceException
import org.slf4j.LoggerFactory
import org.lwjgl.LWJGLUtil
import org.newdawn.slick.util.ResourceLoader
import com.typesafe.scalalogging.slf4j.Logger

object Platform {
  val logger = Logger(LoggerFactory.getLogger(getClass))

  val osName    = System.getProperty("os.name", "")
  val arch      = System.getProperty("os.arch")
  val cachePath = Paths.get("cache")

  val is64Bits = System.getProperty("sun.arch.data.model", System.getProperty("com.ibm.vm.bitmode")) match {
    case "32" => false
    case "64" => true
    case _ => arch.contains("64") || arch.equalsIgnoreCase("sparcv9")
  }

  def isLinux     = isUnix &&  osName.toLowerCase.contains("linux")
  def isMacOSX    = isUnix && (osName.startsWith("Mac") || osName.startsWith("Darwin"))
  def isSolaris   = isUnix && (osName.startsWith("SunOS") || osName.startsWith("Solaris"))
  def isBSD       = isUnix && (osName.contains("BSD") || isMacOSX)
  def isUnix      = File.separatorChar == '/'
  def isWindows   = File.separatorChar == '\\'
  def isWindows7  = osName.equals("Windows 7")
  def isAndroid   = "dalvik".equalsIgnoreCase(System.getProperty("java.vm.name")) && isLinux
  def isArm       = "arm".equals(arch)
  def isSparc     = "sparc".equals(arch) || "sparcv9".equals(arch)
  def isAmd64Arch = arch.equals("x86_64")

  def loadLWJGL() = {
    val nativesPath = cachePath.resolve("native").resolve(LWJGLUtil.getPlatformName)
    System.setProperty("org.lwjgl.librarypath", nativesPath.toFile.getAbsolutePath)
    System.setProperty("net.java.games.input.librarypath", System.getProperty("org.lwjgl.librarypath"))

    val files =
      if (isWindows)
        Seq( "OpenAL32.dll", "OpenAL64.dll", "lwjgl.dll", "lwjgl64.dll", "jinput-dx8.dll", "jinput-dx8_64.dll", "jinput-raw.dll", "jinput-raw_64.dll", "jinput-wintab.dll" )
      else if (isMacOSX)
        Seq( "liblwjgl.jnilib", "openal.dylib", "libjinput-osx.jnilib" )
      else if (isLinux)
        Seq( "liblwjgl.so", "liblwjgl64.so", "libopenal.so", "libopenal64.so", "libjinput-linux.so", "libjinput-linux64.so" )
      else
        ???

    for (fileName <- files)
      extractIfMissing(fileName, nativesPath.resolve(fileName))
  }
  
  def extractIfMissing(embeddedResource: String, extractedResource: Path = null) {
   extractIfMissingOr(embeddedResource, extractedResource, forceExtract = false)
  }

  def extractIfMissingOr(embeddedResource: String, extractedResource: Path = null, forceExtract: => Boolean) {
    val targetPath = if (extractedResource == null) cachePath.resolve( embeddedResource ) else extractedResource
    if (!Files.exists( targetPath ) || forceExtract) {
      val nativeLibraryStream = ResourceLoader.getResourceAsStream(embeddedResource)
      com.google.common.io.Files.createParentDirs(targetPath.toFile)
      if (nativeLibraryStream != null) {
        Files.copy(nativeLibraryStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
      } else {
        throw new MissingResourceException(s"Failed extract embedded resource $embeddedResource to $targetPath, resource could not be found", "-", "-")
      }
    }
  }


}
