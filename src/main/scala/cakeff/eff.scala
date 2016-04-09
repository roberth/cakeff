package cakeff

import scala.language.higherKinds

/** Supertype for all effect handlers and therefore the supertype of all
  * effects as declared in the first type parameter of [[Eff]].
  *
  * Effect type extend this trait in order to add methods that correspond
  * to effectful calls.
  *
  * (In a (G)ADT approach, these would be the alternatives (constructors))
  * (This is basically a CPS-ed free monad expressed in a single trait)
  *
  * Advantages:
  *
  * - No implicits.
  * - Performance?
  *
  * Limitations:
  *
  * Note that because this approach uses trait composition instead of
  * delegation, we have the advantage that this effect system works without
  * any implicits, but also the disadvantage of composition not being as
  * flexible. For example, we can not run a computation of type
  * `Eff[Read[Int] with Read[String], Unit]` because the handler can not
  * implement both.
  * Also, it is not possible to have a generic function like
  * `runOnlyOneEffect[E, ES, T](...): Eff[E with Es, T] => Eff[Es, T]` or
  * `translateOneEffect[E, E2, ES, T](...): Eff[E with Es, T] => Eff[E2 with Es, T]`.
  * It is possible to write interpreters for the concrete situations, but
  * just not generically.
  *
  * Put similarly, the interpreters are not compositional. This is
  * easily demonstrated by the fact that the `Out` parameter must be
  * the same for all effects combined.
  *
  *
  * Interpreters can be made by hand, or using existing tools such as
  * ReaderWriterState, monad transformers etc.
  *
  * It might be interesting to add layers of effects using some kind of
  * monad transformer effect. :)
  */
trait Handler {
  type Out[+_]
  def pure[T](x : T) : Out[T]
  def bind[A, B](m : Out[A])(f : A => Out[B]) : Out[B]
  /* TBD.
  def ap[A, B](fun : Out[A => B], arg : Out[A]) : Out[B] =
    bind(fun){f =>
      bind(arg){a =>
        pure(f(a))
      }
    }
  */
}

/** Open union for effects.
  *
  * Effects are declared by means of the `Hs` type parameter,
  * which is contravariant, so the union of effects can be expressed using
  * `with`.
  */
trait Effect[-Hs <: Handler, +T] {
  /** Returns the result `T` in the handlers' monad type,
    * for any handlers.
    *
    * Note that the handlers' monad type is only chosen here, not in [[Effect]].
    * It is essentially a type parameter of this method
    * because `hs` is a parameter.
    */
  def handle(handler : Hs) : handler.Out[T]
}

/** 'Effectful' computation returning `T` using handlers `Hs`
  */
sealed trait Eff[-Hs <: Handler, +T] {
  def map[X](f : T => X) : Eff[Hs, X] = {
    flatMap(t => Eff.Pure(f(t)))
  }
  def flatMap[X,Hs2 <: Hs](f : T => Eff[Hs2, X]) : Eff[Hs2, X] = {
    Eff.Bind[Hs2, T, X](this, f)
  }
}
object Eff {
  /** Takes an effect representation and inserts it in an [[Eff]].
    */
  def apply[Hs <: Handler, T](e : Effect[Hs, T]) : Eff[Hs, T] = Perform(e)

  private case class Pure[-Hs <: Handler, +T](v : T) extends Eff[Hs, T]
  private case class Bind[-Hs <: Handler, A, +B](m : Eff[Hs, A], f : A => Eff[Hs, B]) extends Eff[Hs, B]
  private case class Perform[-Hs <: Handler, +A](b : Effect[Hs, A]) extends Eff[Hs, A]

  def run[Hs <: Handler, T](hs : Hs)(e : Eff[Hs, T]): hs.Out[T] = {
    e match {
      case Pure(x) =>
        hs.asInstanceOf[hs.type with Handler].pure(x) // asInstanceOf: compiler bug?
      case Bind(m, f) =>
        // Make skolems explicit (`A`, `B`)
        def onBind[A, B](m: Eff[Hs, A], f: A => Eff[Hs, B]): hs.Out[B] = {
          hs.bind(run(hs)(m)) { v: A =>
            run(hs)(f(v))
          }
        }
        onBind(m, f)
      case Perform(b) =>
        b.handle(hs)
    }
  }
}
