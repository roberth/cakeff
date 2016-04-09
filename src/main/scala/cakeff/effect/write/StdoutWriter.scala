package cakeff.effect.write

import java.util.Date


trait StdoutWriter extends Write[String] {
  override def atWrite(x: String): Out[Unit] = {
    println(s"${new Date}: $x")
    pure(())
  }
}
