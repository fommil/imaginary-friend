// Copyright: 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0
package fommil

import scala.reflect.internal.ModifierFlags
import scala.tools.nsc._
import scala.tools.nsc.plugins._
import scala.tools.nsc.transform._

/**
 * A major source of bugs in macros and compiler plugins is failure to
 * preserve mutable information when transforming `global.Tree`
 * instances.
 *
 * Copying a `global.Tree` will result in the `pos`, `symbol` and
 * `tpe` fields being reset.
 *
 * The `global.treeCopy._` methods should be used instead of `copy` on
 * `global.Tree` implementations.
 *
 * Note that the `Modifiers.withAnnotations` method adds to existing
 * methods instead of replacing, so we provide a more cananocial form
 * here in `copyWithAnns`.
 *
 * http://docs.scala-lang.org/overviews/reflection/symbols-trees-types#trees
 */
trait WithPos {
  val global: Global

  // Modifiers is an inner class, oh joy
  implicit class RichModifiers(t: global.Modifiers) {
    /** withAnnotations appears to be broken */
    def copyWithAnns(anns: List[global.Tree]): global.Modifiers =
      t.copy(annotations = anns).setPositions(t.positions)
  }
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

    val Imaginary = global.newTypeName(target)

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
        case ann @ Apply(Select(New(Ident(Imaginary)), nme.CONSTRUCTOR), _) =>
          // NOTE: ignores all parameters, even bad ones
          treeCopy.Apply(ann, ann.fun, Literal(Constant(null)) :: Literal(Constant(true)) :: Nil)

        case other => other
      }
    }

    override def transform = {
      case t: ModuleDef if t.mods.hasAnnotationNamed(Imaginary) =>
        val annotations = imaginaryParameters(t.mods)
        treeCopy.ModuleDef(t, t.mods.copyWithAnns(annotations), t.name, t.impl)

      case t: ClassDef if t.mods.hasAnnotationNamed(Imaginary) =>
        val annotations = imaginaryParameters(t.mods)
        treeCopy.ClassDef(t, t.mods.copyWithAnns(annotations), t.name, t.tparams, t.impl)

      case t => t
    }
  }

  private val ImaginaryFriend = new TransformingComponent(global) {
    override val phaseName: String = "imaginary-companion"
    import global._

    override def transform = {
      case t: ClassDef if t.mods.hasAnnotationNamed(Imaginary) & t.name.toString == "Foo" =>
        // if (!isIde) {
        //   println("\n")
        //   println(t)
        //   println(showRaw(t))
        // }

        // FROM
        // ClassDef(Modifiers(NoFlags, tpnme.EMPTY, List(Apply(Select(New(Ident(newTypeName("imaginary"))), nme.CONSTRUCTOR), List()))), newTypeName("Foo"), List(), Template(List(Select(Ident(scala), newTypeName("AnyRef"))), emptyValDef, List(ValDef(Modifiers(PRIVATE | LOCAL | PARAMACCESSOR), newTermName("foo"), Ident(newTypeName("String")), EmptyTree), ValDef(Modifiers(PRIVATE | LOCAL | PARAMACCESSOR), newTermName("bar"), Ident(newTypeName("Long")), EmptyTree), DefDef(Modifiers(), nme.CONSTRUCTOR, List(), List(List(ValDef(Modifiers(PARAM | PARAMACCESSOR), newTermName("foo"), Ident(newTypeName("String")), EmptyTree), ValDef(Modifiers(PARAM | PARAMACCESSOR), newTermName("bar"), Ident(newTypeName("Long")), EmptyTree))), TypeTree(), Block(List(Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), List())), Literal(Constant(())))))))

        // not sure these can be re-used, so creating fresh ones
        def name = newTermName(t.name.toString)
        def tpe = newTypeName(t.name.toString)

        val companion = ModuleDef(
          Modifiers(),
          name,
          Template(
            List(Select(Ident("scala"), newTypeName("AnyRef"))),
            // TODO: this alternative block will use "extends (String, Long) => Foo" as parent
            // List(
            //   AppliedTypeTree(
            //     Select(Select(Ident(nme.ROOTPKG), "scala"), newTypeName("Function2")),
            //     List(Ident(newTypeName("String")), Ident(newTypeName("Long")), Ident(newTypeName(name)))
            //   )
            // ),
            emptyValDef,
            List(
              DefDef(
                Modifiers(),
                nme.CONSTRUCTOR,
                List(),
                List(List()),
                TypeTree(),
                Block(
                  List(
                    Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), List())
                  ),
                  Literal(Constant(()))
                )
              ),
              DefDef(
                Modifiers(),
                newTermName("apply"),
                List(),
                List(
                  List(
                    ValDef(Modifiers(ModifierFlags.PARAM), newTermName("foo"), Ident(newTypeName("String")), EmptyTree),
                    ValDef(Modifiers(ModifierFlags.PARAM), newTermName("bar"), Ident(newTypeName("Long")), EmptyTree)
                  )
                ),
                Ident(tpe),
                Apply(Select(New(Ident(tpe)), nme.CONSTRUCTOR), List(Ident(newTermName("foo")), Ident(newTermName("bar"))))
              )
            )
          )
        )

        // both of these result in exceptions, no owners set yet
        //companion.symbol.owner
        //t.symbol.owner

        treeCopy.Block(t, List(t, companion), Literal(Constant(())))

        // getting this error, the "anonymous class $anonfun" bit looks suspicious
        /*
[error]   last tree to typer: term apply
[error]               symbol: method apply (flags: <method> final)
[error]    symbol definition: final def apply(): Unit
[error]                  tpe: <notype>
[error]        symbol owners: method apply -> anonymous class $anonfun -> value <local FakeSpec> -> class FakeSpec -> package testing
[error]       context owners: class Foo
         */

        // symbol owners: method apply -> anonymous class $anonfun -> value <local FakeSpec> -> class FakeSpec -> package testing

      //t

      case t: ModuleDef =>
        if (!isIde) {
          println("\n")
          println(t)
          println(showRaw(t))
          println(showRaw(t.symbol))
        }
        t

      case t => t
    }
  }

  override val components = List(ImaginaryParameters, ImaginaryFriend)
}
