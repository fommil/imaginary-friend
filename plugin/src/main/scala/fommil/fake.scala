// Copyright: 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0
package fommil

import scala.tools.nsc._
import scala.tools.nsc.transform._
import scala.tools.nsc.plugins._

class ImaginaryPlugin(override val global: Global) extends Plugin {
  override val description: String = "imaginary"
  override val name: String = "imaginary"
  override val components: List[PluginComponent] = List(
    new PluginComponent with TypingTransformers {
      override val global = ImaginaryPlugin.this.global
      import global._

      override def newPhase(prev: Phase): Phase = new StdPhase(prev) {
        override def apply(unit: CompilationUnit): Unit = newTransformer(unit).transformUnit(unit)
      }

      override val runsAfter: List[String] = "refchecks" :: Nil
      override val phaseName: String = "imaginary"

      // there has got to be a more efficient way to do this
      def hasAnnotation(sym: TypeSymbol, ann: String): Boolean =
        sym.annotations.exists(_.symbol.tpe.dealias.toLongString == ann)

      def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
        override def transform(tree: Tree): Tree = {
          val t = super.transform(tree)
          (t, t.symbol) match {
            case (ap: ClassDef, sym: TypeSymbol) if hasAnnotation(sym, "testing.imaginary") =>
              println(ap)
              ap

            case (t, _) => t
          }
        }
      }
    }
  )
}
