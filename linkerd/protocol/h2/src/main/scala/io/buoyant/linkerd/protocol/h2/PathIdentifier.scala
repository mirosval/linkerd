package io.buoyant.linkerd
package protocol
package h2

import com.fasterxml.jackson.annotation.JsonIgnore
import com.twitter.finagle.{Dtab, Path, Stack}
import com.twitter.finagle.buoyant.Dst
import com.twitter.finagle.buoyant.h2.{LinkerdHeaders, Request}
import com.twitter.util.Future
import io.buoyant.router.H2
import io.buoyant.router.RoutingFactory._

class PathIdentifier(pfx: Path, baseDtab: () => Dtab)
  extends Identifier[Request] {

  override def apply(req: Request): Future[RequestIdentification[Request]] = {
    val dst = Dst.Path(pfx ++ reqPath(req), baseDtab(), Dtab.local)
    Future.value(new IdentifiedRequest(dst, req))
  }

  private def reqPath(req: Request): Path = req.path match {
    case "" | "/" => Path.empty
    case UriPath(path) => Path.read(path)
  }

  private object UriPath {
    def unapply(uri: String): Option[String] =
      uri.indexOf('?') match {
        case -1 => Some(uri.stripSuffix("/"))
        case idx => Some(uri.substring(idx + 1).stripSuffix("/"))
      }
  }
}

object PathIdentifier {
  def mk(params: Stack.Params) = {
    val DstPrefix(pfx) = params[DstPrefix]
    val BaseDtab(baseDtab) = params[BaseDtab]
    new PathIdentifier(pfx, baseDtab)
  }

  val param = H2.Identifier(mk)
}

class PathIdentifierConfig extends H2IdentifierConfig {
  @JsonIgnore
  override def newIdentifier(params: Stack.Params) = PathIdentifier.mk(params)
}

object PathIdentifierConfig {
  val kind = "io.l5d.h2.path"
  val defaultPath = LinkerdHeaders.Prefix + "name"
}

class PathIdentifierInitializer extends IdentifierInitializer {
  val configClass = classOf[PathIdentifierConfig]
  override val configId = PathIdentifierConfig.kind
}

object PathIdentifierInitializer extends PathIdentifierInitializer
