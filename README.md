# Need

Macros and compiler plugins are becoming popular in scala projects to reduce boilerplate, but the IDEs are unable to keep up (issuing many "false positive" errors). This results in a trade-off for developers between reducing boilerplate or reducing functionality in their editor.

Fixing the problem at its source (in the [interactive compiler](https://github.com/scala/scala/tree/2.12.x/src/interactive/scala/tools/nsc/interactive), aka Presentation Compiler) is impossible in the short term (we can't make sweeping changes to stable releases of the scala compiler), and fixing it in the long-term is a huge undertaking that nobody has the time for.

# Benefit

If macro and plugin authors were to test their code in the presentation compiler, as well as in the compiler plugin, it should result in better IDE adoption of their work.

If it is simply not possible to adapt a plugin or macro to work within the presentation compiler, the author may choose to release a mocked variant as a presentation compiler plugin. This will never be used in downstream compiles, but will be used by ENSIME and scala-ide.

As an example, consider the `cachedImplicit` macro within shapeless. It reports [false positives and can freeze the editor](https://github.com/milessabin/shapeless/issues/458). But if a variant of this macro was written to bypass the actual implementation, issuing a (configurable) warning "This implicit derivation will be skipped in the editor", many developers would appreciate the workaround.

# Approach

This project is an experiment in what breaks the presentation compiler (specifically whitebox macros, blackbox, compiler plugins) and will lay the foundation for a test framework aimed at macro/plugin authors to test their plugins in the presentation compiler (e.g. don't break the RangePositions).

# Prior Art

[ENSIME](https://github.com/ensime/ensime-server/tree/master/core/src/it/scala/org/ensime) and [scala-refactoring]( https://github.com/scala-ide/scala-refactoring/tree/master/src/test) provide mechanisms for testing user code within the presentation compiler, and could form the basis for a reusable test framework for plugin authors.

# Example

The example used in this experiment is an annotation

```scala
class imaginary(foo: String, bar: Boolean) extends StaticAnnotation
```

where each `class` annotated with `@imaginary` has the following generated:

1. parameters for `foo` and `bar`, unless provided.
2. `apply` method created on the companion.
3. a method on the companion for each `val param: T` of the same signature.

For example

```scala
@imaginary
class Baz(val fred: String)
```

expands to

```scala
@imaginary(foo = null, bar = true)
class Baz(val fred: String)
object Baz {
  def apply(fred: String): Baz = null
  def fred: String = null
}
```

Recall that values don't matter because the plugin runs in the presentation compiler.
