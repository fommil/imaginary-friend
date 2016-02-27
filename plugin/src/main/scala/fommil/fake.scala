// Copyright: 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0
package fommil

import scala.tools.nsc._
import scala.tools.nsc.transform._
import scala.tools.nsc.plugins._

/**
 * A major source of bugs in macros and compiler plugins is failure to
 * preserve mutable information when transforming global.Tree
 * instances.
 *
 * The `copyWithPos` provided by this class should be preferred to the
 * `copy` methods on all `Tree` implementations.
 *
 * Note that by copying a `Tree` will result in the `pos`, `symbol`
 * and `tpe` fields being reset.
 *
 * http://docs.scala-lang.org/overviews/reflection/symbols-trees-types#trees
 */
trait CopyWithPosition {
  val global: Global

  // Modifiers is an inner class, oh joy
  implicit class RichModifiers(t: global.Modifiers) {
    def withPos(orig: global.Modifiers): global.Modifiers = {
      t.positions = orig.positions
      t
    }
  }

  // *sigh* doesn't work
  // implicit class RichTree[T <: global.Tree](t: T) {
  //   def withPos(orig: T): T = {
  //     t.pos = orig.pos
  //     t
  //   }
  // }

  // let's do this the long way...
  trait HasWithPos[T <: global.Tree] {
    def t: T
    def withPos(orig: T): T = {
      t.pos = orig.pos
      t
    }
  }
  implicit class RichApply(val t: global.Apply) extends HasWithPos[global.Apply]
  implicit class RichModuleDef(val t: global.ModuleDef) extends HasWithPos[global.ModuleDef]
  implicit class RichClassDef(val t: global.ClassDef) extends HasWithPos[global.ClassDef]

}

class ImaginaryPlugin(override val global: Global) extends Plugin {
  override val description: String = "imaginary"
  override val name: String = "imaginary"

  override val components: List[PluginComponent] = List(
    new PluginComponent with TypingTransformers with CopyWithPosition {
      override val global = ImaginaryPlugin.this.global

      override def newPhase(prev: Phase): Phase = new StdPhase(prev) {
        override def apply(unit: global.CompilationUnit): Unit = newTransformer(unit).transformUnit(unit)
      }

      override val runsAfter: List[String] = "parser" :: Nil
      override val phaseName: String = "imaginary"

      //      val copier = new CopyWithPosition(global)
      //      import copier._

      def newTransformer(unit: global.CompilationUnit) = new TypingTransformer(unit) {
        // BUG: this only detects annotations called "imaginary", not FQN
        val Imaginary = global.TypeName("imaginary")

        def imaginaryParameters(mods: global.Modifiers) = {
          import global._
          mods.annotations.map {
            case ann @ Apply(Select(New(Ident(Imaginary)), termNames.CONSTRUCTOR), _) =>
              // BUG: ignores all parameters, even bad ones
              ann.copy(args = Literal(Constant(null)) :: Literal(Constant(true)) :: Nil).withPos(ann)
            case other => other
          }
        }

        override def transform(tree: global.Tree): global.Tree = {
          super.transform(tree) match {
            case t: global.ModuleDef if t.mods.hasAnnotationNamed(Imaginary) =>
              val annotations = imaginaryParameters(t.mods)
              t.copy(mods = t.mods.copy(annotations = annotations).withPos(t.mods)).withPos(t)

            case t: global.ClassDef if t.mods.hasAnnotationNamed(Imaginary) =>
              val annotations = imaginaryParameters(t.mods)
              t.copy(mods = t.mods.copy(annotations = annotations).withPos(t.mods)).withPos(t)

            case t =>
              // println("\n")
              // println(t)
              // println(showRaw(t))
              // println("\n")

              t
          }
        }
      }
    }
  )
}
