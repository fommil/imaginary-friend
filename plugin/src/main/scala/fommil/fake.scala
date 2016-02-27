// Copyright: 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0
package fommil

import scala.tools.nsc._
import scala.tools.nsc.transform._
import scala.tools.nsc.plugins._

/**
 * A major source of bugs in macros and compiler plugins is failure to
 * preserve mutable information when transforming `global.Tree`
 * instances.
 *
 * Copying a `global.Tree` will result in the `pos`, `symbol` and
 * `tpe` fields being reset.
 *
 * The `withPos` provided by this class should be used after any
 * `copy` methods on `Tree` implementations.
 *
 * http://docs.scala-lang.org/overviews/reflection/symbols-trees-types#trees
 */
trait WithPos {
  val global: Global

  // Modifiers is an inner class, oh joy
  implicit class RichModifiers(t: global.Modifiers) {
    def withPos(orig: global.Modifiers): global.Modifiers = { t.positions = orig.positions ; t }
  }

  // *sigh* doesn't work
  // implicit class RichTree[T <: global.Tree](t: T) {
  //   def withPos(orig: T): T = { t.pos = orig.pos ; t }
  // }

  // let's do this the long way...
  trait HasWithPos[T <: global.Tree] {
    def t: T
    def withPos(orig: T): T = { t.pos = orig.pos; t }
  }
  implicit class RichApply(val t: global.Apply) extends HasWithPos[global.Apply]
  implicit class RichModuleDef(val t: global.ModuleDef) extends HasWithPos[global.ModuleDef]
  implicit class RichClassDef(val t: global.ClassDef) extends HasWithPos[global.ClassDef]
  // ... add more as needed ...

}

/**
 * Convenient way to tell if you are running in an interactive context.
 */
trait IsIde {
  val global: Global

  val isIde = global.isInstanceOf[tools.nsc.interactive.Global]
}

class ImaginaryPlugin(override val global: Global) extends Plugin {
  private val target = "imaginary"
  override val description: String = s"Generates code when using an annotation named '$target'"
  override val name: String = "imaginary"

  abstract class TransformingComponent(override val global: Global) extends PluginComponent with TypingTransformers with WithPos with IsIde {

    val Imaginary = global.TypeName(target)

    override def newPhase(prev: Phase): Phase = new StdPhase(prev) {
      override def apply(unit: global.CompilationUnit): Unit = newTransformer(unit).transformUnit(unit)
    }
    override val runsAfter: List[String] = "parser" :: Nil

    def newTransformer(unit: global.CompilationUnit) = new TypingTransformer(unit) {
      override def transform(tree: global.Tree): global.Tree = {
        TransformingComponent.this.transform(super.transform(tree))
      }
    }

    def transform: Function[global.Tree, global.Tree]
  }

  private val ImaginaryParameters = new TransformingComponent(global) {
    override val phaseName: String = "imaginary-params"
    import global._

    def imaginaryParameters(mods: global.Modifiers) = {
      mods.annotations.map {
        case ann @ Apply(Select(New(Ident(Imaginary)), termNames.CONSTRUCTOR), _) =>
          // NOTE: ignores all parameters, even bad ones
          ann.copy(args = Literal(Constant(null)) :: Literal(Constant(true)) :: Nil).withPos(ann)
        case other => other
      }
    }

    override def transform = {
      case t: ModuleDef if t.mods.hasAnnotationNamed(Imaginary) =>
        val annotations = imaginaryParameters(t.mods)
        t.copy(mods = t.mods.copy(annotations = annotations).withPos(t.mods)).withPos(t)

      case t: ClassDef if t.mods.hasAnnotationNamed(Imaginary) =>
        val annotations = imaginaryParameters(t.mods)
        t.copy(mods = t.mods.copy(annotations = annotations).withPos(t.mods)).withPos(t)

      case t => t
    }
  }

  private val ImaginaryFriend = new TransformingComponent(global) {
    override val phaseName: String = "imaginary-companion"
    import global._

    override def transform = {
      case t => t
    }
  }

  override val components = List(ImaginaryParameters)
}
