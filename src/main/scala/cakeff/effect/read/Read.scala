package cakeff.effect.read

import cakeff.{Eff, Effect, Handler}

trait Read[T] extends Handler {
  def atRead : Out[T]
}
object Read {
  def apply[T] : Eff[Read[T], T] = Eff(new Effect[Read[T], T] {
    override def handle(handler: Read[T]): handler.Out[T] = handler.atRead
  })
}
