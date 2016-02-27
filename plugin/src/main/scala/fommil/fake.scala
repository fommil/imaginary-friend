// Copyright: 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0
package fommil

import scala.tools.nsc._
import scala.tools.nsc.transform._
import scala.tools.nsc.plugins._

class ImaginaryPlugin(val global: Global) extends Plugin {
  import global._

  override val components: List[PluginComponent] = List(
    new PluginComponent with TypingTransformers {
      val global: ImaginaryPlugin.this.global.type = ImaginaryPlugin.this.global

      val imaginaryName = TermName("testing.imaginary")
      val imaginarySym = imaginaryName

      def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
        override def transform(tree: Tree): Tree = {
          val t = super.transform(tree)
          (t, t.symbol) match {
            case (ap: ClassDef, sym: TypeSymbol) =>
              println(ap)
              println(sym)
              ap

            case (t, _) => t
          }
        }
      }
      override def newPhase(prev: Phase): Phase = new StdPhase(prev) {
        override def apply(unit: CompilationUnit): Unit = {
          val translated = newTransformer(unit).transformUnit(unit)
          //println(show(unit.body))
          translated
        }
      }

      override val runsAfter: List[String] = "refchecks" :: Nil
      override val phaseName: String = "imaginary"

    }
  )
  override val description: String = "imaginary"
  override val name: String = "imaginary"
}
