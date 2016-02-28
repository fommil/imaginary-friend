// Copyright: 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0

// intentionally not in fommil for testing
package testing

import org.scalatest._
import scala.annotation.StaticAnnotation

class imaginary(foo: String, bar: Boolean) extends StaticAnnotation

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
class Foo(foo: String, bar: Long)
// object Foo extends ((String, Long) => Foo) {
//   def apply(foo: String, bar: Long): Foo = new Foo(foo, bar)
// }
// object Foo {
//   def apply(foo: String, bar: Long): Foo = new Foo(foo, bar)
// }

//@imaginary
//class Mine(val foo: String = "foo", val bar: Long = 13)

class FakeSpec extends FlatSpec with Matchers {
  // "@imaginary" should "generate companion's apply with no parameters" in {
  //   val me: Me = Me()
  // }

  it should "create Foo's companion" in {
    //Foo.apply("foo", 13L) shouldBe a[Foo]
  }

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
