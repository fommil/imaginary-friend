// Copyright: 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0
package fommil

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.Universe
import scala.reflect.macros.whitebox.Context

import macrocompat._

/**
 * Generates dummy methods on the companion, for use in the
 * presentation compiler to spoof more complex compiler plugins, but
 * without the overhead of what they are actually doing at compile
 * time.
 */
class imaginary extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ImaginaryFriendMacros.generate
}

object ImaginaryFriend {
  def figment[T]: T = macro ImaginaryFriendMacros.figment[T]
}

@bundle
class ImaginaryFriendMacros(val c: Context) {

  import c.universe._

  def generate(annottees: c.Expr[Any]*): c.Expr[Any] = {

    import c.universe._
    val anntTrees = annottees.map(_.tree).toList
    val outputs: List[c.universe.Tree] = anntTrees.flatMap { annt =>
      annt match {
        case clz: ClassDef => {

          val clzName = TermName(clz.name.toString)
          val body: List[c.universe.Tree] = clz.impl.body
          val consParmList: List[List[List[Universe#ValDef]]] = body.collect {
            case DefDef(_, termNames.CONSTRUCTOR, _, parms, _, _) => parms
          }

          //println(showRaw(annt))
          List(
            clz,
            q"""
              object ${clzName} {
               def apply () :${clz.name}= null
              }
          """
          )
        }
        case objDef: ModuleDef => {
          println("MODDEF:" + showRaw(objDef))
          List(objDef)
        }
      }
    }

    c.Expr[Any](Block(outputs, Literal(Constant(()))))

    //THIS IS WHAT WE NEED TO GENERATE  for the object def
    //    val md = ModuleDef(
    //      Modifiers(),
    //      TermName("MyObj"),
    //      Template(
    //        List(Select(
    //          Ident(scala),
    //          TypeName("AnyRef")
    //        )),
    //        noSelfType,
    //        List(
    //          DefDef(
    //            Modifiers(),
    //            termNames.CONSTRUCTOR,
    //            List(),
    //            List(List()),
    //            TypeTree(),
    //            Block(
    //              List(pendingSuperCall),
    //              Literal(Constant(()))
    //            )
    //          ),
    //          DefDef(
    //            Modifiers(),
    //            TermName("apply"),
    //            List(),
    //            List(List(
    //              ValDef(
    //                Modifiers(PARAM),
    //                TermName("foo"),
    //                Ident(TypeName("String")),
    //                EmptyTree
    //              ),
    //              ValDef(
    //                Modifiers(PARAM),
    //                TermName("bar"),
    //                Ident(TypeName("Long")),
    //                EmptyTree
    //              )
    //            )),
    //            TypeTree(),
    //            Literal(Constant(null))
    //          )
    //        )
    //      )
    //    )

    //contents of class constructor as showRaw

    //        DefDef(Modifiers(),
    //          termNames.CONSTRUCTOR,
    //          List(),
    //          List(List(ValDef(Modifiers(PARAM | PARAMACCESSOR),
    //            TermName("foo"),
    //            Ident(TypeName("String")),
    //            EmptyTree),
    //            ValDef(Modifiers(PARAM | PARAMACCESSOR),
    //              TermName("bar"),
    //              Ident(TypeName("Long")),
    //              EmptyTree))),
    //          TypeTree(),
    //          Block(List(pendingSuperCall),
    //            Literal(Constant(()))))))

    //WHOLE CLASSDEF for Myself class

    //    val t = ClassDef(
    //      Modifiers(),
    //      TypeName("Myself"),
    //      List(),
    //      Template(
    //        List(
    //          Select(
    //            Ident(scala),
    //            TypeName("AnyRef")
    //          )
    //        ),
    //        noSelfType,
    //        List(
    //          ValDef(
    //            Modifiers(PARAMACCESSOR),
    //            TermName("foo"),
    //            Ident(TypeName("String")),
    //            EmptyTree
    //          ),
    //          ValDef(
    //            Modifiers(PARAMACCESSOR),
    //            TermName("bar"),
    //            Ident(TypeName("Long")),
    //            EmptyTree
    //          ),
    //          DefDef(
    //            Modifiers(),
    //            termNames.CONSTRUCTOR,
    //            List(),
    //            List(List(
    //              ValDef(
    //              Modifiers(PARAM | PARAMACCESSOR),
    //              TermName("foo"),
    //              Ident(TypeName("String")),
    //              EmptyTree
    //            ),
    //              ValDef(
    //                Modifiers(PARAM | PARAMACCESSOR),
    //                TermName("bar"),
    //                Ident(TypeName("Long")),
    //                EmptyTree
    //              )
    //            )),
    //            TypeTree(),
    //            Block(
    //              List(pendingSuperCall),
    //              Literal(Constant(()))
    //            )
    //          )
    //        )
    //      )
    //    )
    //outputs.foreach(b => println(showCode(b)))

    //    //    val (annottee, expandees) = anntTrees match {
    //    //      case (param: ClassDef) :: (rest@(_ :: _)) => {
    //    //        println(showCode(param))
    //    //        (param, rest)
    //    //      }
    //    //      case _ => (EmptyTree, anntTrees)
    //    //    }
    //    //    println((annottee, expandees))
    //    //    val outputs = expandees
    //

  }

  def figment[C: WeakTypeTag]: Tree = {
    q"new ${weakTypeOf[C]} { def illusion = 4 }"
  }
}
