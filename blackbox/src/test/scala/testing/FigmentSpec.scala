// Copyright: 2016 Sam Halliday
// Licence: http://www.apache.org/licenses/LICENSE-2.0

// intentionally not in fommil for testing
package testing

import fommil.Figment

import org.scalatest._

class FigmentSpec extends FlatSpec with Matchers {

  "Figment" should "provide an A" in {
    class A
    Figment.figment[A] shouldBe a[A]
  }

}
