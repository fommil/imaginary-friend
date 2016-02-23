// Copyright: 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0
package fommil

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.Universe
import scala.reflect.macros.blackbox.Context

/**
 * Blackbox macros are not as powerful, so we can't do the same thing
 * as the `@imaginary` annotation.
 */
object Figment {
  def figment[T]: T = macro FigmentMacros.figment[T]
}

class FigmentMacros(val c: Context) {
  import c.universe._

  def figment[C: WeakTypeTag]: Tree = q"new ${weakTypeOf[C]}"
}
