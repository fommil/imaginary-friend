// Copyright: 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0

// intentionally not in fommil for testing
package testing

import org.scalatest._
import scala.annotation.StaticAnnotation

class imaginary(foo: Boolean, bar: Boolean, baz: Int) extends StaticAnnotation

@imaginary
class Me

@imaginary
class Myself(val foo: String, val bar: Long)

class Irene

@imaginary
trait Mallgan

@imaginary
object MyObj {
  def apply(foo: String, bar: Long): Me = null
}

@imaginary
class Foo(foo: String, bar: Long) {
  val baz: String = foo // shouldn't be in constructor
}
object Foo {
  def ignore(foo: String, bar: Long): Foo = new Foo(foo, bar)
}

@imaginary
class Baz[T](val fred: T)

@imaginary
class Mine(val foo: String = "foo", val bar: Long = 13)

class FakeSpec extends FlatSpec with Matchers {
  "@imaginary" should "generate companion's apply with no parameters" in {
    { Me(): Me } shouldBe null
  }

  it should "create a companion for Mallgan" in {
    Mallgan shouldBe a[Mallgan.type]
  }

  it should "generate companion apply with parameters" in {
    { Myself("foo", 23L): Myself } shouldBe null
  }

  it should "update Foo's companion" in {
    Foo.ignore("foo", 13L) shouldBe a[Foo]

    { Foo("foo", 13L): Foo } shouldBe null
  }

  it should "generate companion apply with named / default parameters" in {
    { Mine("foo"): Mine } shouldBe null

    { Mine(foo = "foo"): Mine } shouldBe null

    { Mine(bar = 10): Mine } shouldBe null
  }

  it should "not create anything not @imaginary" in {
    "Irene" shouldNot compile
  }

  it should "handle typed classes" in {
    { Baz("hello"): Baz[String] } shouldBe null
  }

}
