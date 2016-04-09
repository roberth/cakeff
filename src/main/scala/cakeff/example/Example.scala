package cakeff.example

import cakeff.effect.read.Read
import cakeff.effect.write.{StdoutWriter, Write}
import cakeff.{Eff, Handler}

import scala.annotation.tailrec

object Example {

  def main(argv: Array[String]) {

    val c = for {
      x <- Read.apply[Int]
      y <- Read.apply[Int]
    } yield {
      x + y
    }

    trait ReadRunner[T] extends Read[T] {
      def valueToRead: T

      override def atRead: Out[T] = pure(valueToRead)
    }

    trait IdentityMonad extends Handler {
      override type Out[+X] = X

      override def pure[X](x: X): X = x

      override def bind[A, B](m: A)(f: A => B): B = f(m)
    }

    trait ListMonad extends Handler {
      override type Out[+X] = List[X]

      override def pure[T](x: T): List[T] = List(x)

      override def bind[A, B](m: List[A])(f: (A) => List[B]): List[B] = m flatMap f
    }


    val c2 = for {
      _ <- Write("Performing addition")
      sum <- c
      _ <- Write(s"Sum is $sum")
    } yield sum


    val r = Eff.run(new IdentityMonad with Read[Int] {
      override def atRead: Int = 2
    })(c)

    val r2 = Eff.run(new Write[String] with Read[Int] {
      override type Out[+T] = (IndexedSeq[String], T)

      override def bind[A, B](m: (IndexedSeq[String], A))(f: (A) => (IndexedSeq[String], B)): (IndexedSeq[String], B) = {
        val (l1, mv) = m
        val (l2, r) = f(mv)
        (l1 ++ l2, r)
      }

      override def pure[T](x: T): (IndexedSeq[String], T) = (IndexedSeq.empty, x)

      override def atWrite(x: String): (IndexedSeq[String], Unit) = (IndexedSeq(x), ())

      override def atRead: Out[Int] = pure(3)
    })(c2)


    println(s"r is $r")
    println(s"r2 is $r2")


    // For those who don't like making choices but do like to mix i/o and other monads
    val nd = Eff.run(new StdoutWriter with Read[Int] with ListMonad {
      override def atRead: List[Int] = List(3, 23)
    })(c2)

    println(s"Nondeterministic results are $nd")


    // This one is really scary ;)
    Eff.run(new StdoutWriter with Read[Int] with IdentityMonad {
      @tailrec
      override def atRead: Int = {
        println("Enter a number:")
        try {
          scala.io.StdIn.readInt
        } catch {
          case e: NumberFormatException =>
            atRead
        }
      }
    })(c2)
  }
}
