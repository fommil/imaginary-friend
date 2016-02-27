// Copyright: 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0

// intentionally not in fommil for testing
package testing

import org.scalatest._
import scala.annotation.StaticAnnotation

class imaginary extends StaticAnnotation

@imaginary
class Me

@imaginary
class Myself(val foo: String, val bar: Long)

class Irene

@imaginary
object MyObj {
  def apply(foo: String, bar: Long) = null
}

@imaginary
class Foo
object Foo {
  def ignore(foo: String, bar: Long) = null
}

//@imaginary
//class Mine(val foo: String = "foo", val bar: Long = 13)

class FakeSpec extends FlatSpec with Matchers {
  // "@imaginary" should "generate companion's apply with no parameters" in {
  //   val me: Me = Me()
  // }

  //  it should "generate companion apply with parameters" in {
  //    Myself("foo", 23L) shouldBe a[Myself]
  //  }
  //
  //  it should "generate companion apply with named / default parameters" in {
  //    Mine("foo") shouldBe a[Mine]
  //    Mine(foo = "foo") shouldBe a[Mine]
  //    Mine(bar = 10) shouldBe a[Mine]
  //  }

}
