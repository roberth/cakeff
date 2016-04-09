package cakeff.effect.write

import cakeff.{Eff, Effect, Handler}

trait Write[T] extends Handler {
  def atWrite(x : T) : Out[Unit]
}
object Write {
  def apply[T](v : T) : Eff[Write[T], Unit] = Eff(new Effect[Write[T], Unit] {
    override def handle(handler: Write[T]): handler.Out[Unit] = handler.atWrite(v)
  })
}
