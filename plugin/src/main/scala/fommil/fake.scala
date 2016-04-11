// Copyright: 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0
package fommil

import scala.collection.breakOut
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

    def transform: global.Tree => global.Tree
  }

  private val ImaginaryParameters = new TransformingComponent(global) {
    override val phaseName: String = "imaginary-params"
    import global._

    def imaginaryParameters(mods: global.Modifiers) = {
      mods.annotations.map {
        case ann @ Apply(Select(New(Ident(Imaginary)), nme.CONSTRUCTOR), _) =>
          // NOTE: ignores all parameters, even bad ones
          treeCopy.Apply(
            ann,
            ann.fun,
            Literal(Constant(true)) :: Literal(Constant(true)) :: Literal(Constant(0)) :: Nil
          )

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

  private val ImaginaryCompanion = new TransformingComponent(global) {
    override val phaseName: String = "imaginary-companion"
    import global._

    /** generates a zero-functionality companion for a class */
    def genCompanion(clazz: ClassDef): ModuleDef = ModuleDef(
      Modifiers(),
      clazz.name.companionName,
      Template(
        List(Select(Ident(nme.scala_), nme.AnyRef.toTypeName)),
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
          )
        )
      )
    )

    /** outputs an update to companion based on the content of clazz */
    def updateCompanion(clazz: ClassDef, companion: ModuleDef): ModuleDef = {
      // not sure these can be re-used, so create fresh ones
      //def name = newTermName(clazz.name.toString)
      def tpe = newTypeName(clazz.name.toString)
      def stripVariance(t: TypeDef): TypeDef = {
        val mods = t.mods &~ ModifierFlags.COVARIANT &~ ModifierFlags.CONTRAVARIANT
        val params = t.tparams.map(stripVariance)
        treeCopy.TypeDef(t, mods, t.name, params, t.rhs)
      }.duplicate

      clazz.impl.body.collectFirst {
        case DefDef(_, nme.CONSTRUCTOR, _, params, _, _) => params
      } match {
        case None => companion // traits don't have constructor parameters
        case Some(ps) =>
          val apply = DefDef(
            Modifiers(),
            newTermName("apply"),
            clazz.tparams.map(stripVariance),
            ps.map(_.map { v =>
              val mods = (v.mods &~ ModifierFlags.PARAMACCESSOR)
              treeCopy.ValDef(v, mods, v.name, v.tpt.duplicate, v.rhs)
            }),
            if (clazz.tparams.isEmpty) Ident(tpe)
            else AppliedTypeTree(Ident(tpe), clazz.tparams.map { t => Ident(t.name) }),
            // PC plugins don't need implementations
            Literal(Constant(null))
          )
          treeCopy.ModuleDef(
            companion, companion.mods, companion.name, {
            val impl = companion.impl
            treeCopy.Template(impl, impl.parents, impl.self, apply :: impl.body)
          }
          )
      }
    }

    def hasImaginaryClasses(t: PackageDef): Boolean =
      t.stats.collectFirst {
        case c: ClassDef if c.mods.hasAnnotationNamed(Imaginary) => c
      }.isDefined

    override def transform = {
      case t: PackageDef if hasImaginaryClasses(t) =>
        val classes: Map[TypeName, ClassDef] = t.stats.collect {
          case c: ClassDef => c.name -> c
        }(breakOut)

        val companions: Map[TermName, ModuleDef] = t.stats.collect {
          case m: ModuleDef => m.name -> m
        }(breakOut)

        object ClassWithoutCompanion {
          def unapply(t: Tree): Option[ClassDef] = t match {
            case c: ClassDef if !companions.contains(c.name.companionName) => Some(c)
            case _ => None
          }
        }

        object ClassWithCompanion {
          def unapply(t: Tree): Option[(ClassDef, ModuleDef)] = t match {
            case m: ModuleDef =>
              classes.get(m.name.companionName).map { c => (c, m) }
            case _ => None
          }
        }

        val updated = t.stats.flatMap {
          case ClassWithoutCompanion(c) if c.mods.hasAnnotationNamed(Imaginary) =>
            val companion = updateCompanion(c, genCompanion(c))
            List(c, companion)

          case ClassWithCompanion(c, companion) if c.mods.hasAnnotationNamed(Imaginary) =>
            List(updateCompanion(c, companion))

          case t =>
            List(t)
        }

        treeCopy.PackageDef(t, t.pid, updated)

      case t => t
    }

  }

  override val components = List(ImaginaryParameters, ImaginaryCompanion)
}
