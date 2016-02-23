// Copyright: 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0

package fommil

import scala.tools.nsc.{ Global, Phase }
import scala.tools.nsc.plugins.{ Plugin, PluginComponent }

class ImaginaryPlugin(val global: Global) extends Plugin {
  import global._

  val name = "imaginary"
  val description = "Fakes macro magic when it sees an annotation named imaginary"
  val components = List[PluginComponent](ImaginaryComponent)

  private object ImaginaryComponent extends PluginComponent {
    val global = ImaginaryPlugin.this.global
    import global._

    override val runsAfter = List("erasure")

    val phaseName = "Imaginary"

    override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
      override def apply(unit: CompilationUnit): Unit = {
        new ImaginaryTraverser(unit) traverse unit.body
      }
    }

    class ImaginaryTraverser(unit: CompilationUnit) extends Traverser {
      override def traverse(tree: Tree): Unit = tree match {
        case New(tpt) if exitingTyper(tpt.tpe.typeSymbol.isDerivedValueClass) =>
          unit.warning(tree.pos, s"Value class `${tpt.tpe.typeSymbol.fullName}` instantiated!")
        case _ =>
          super.traverse(tree)
      }
    }
  }
}
