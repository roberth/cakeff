## Advantages:
  
 - No implicits.
 - Performance?
 
## Limitations:
  
Note that because this approach uses trait composition instead of
delegation, we have the advantage that this effect system works without
any implicits, but also the disadvantage of composition not being as
flexible. For example, we can not run a computation of type
`Eff[Read[Int] with Read[String], Unit]` because the handler can not
implement both.
Also, it is not possible to have a generic function like
`runOnlyOneEffect[E, ES, T](...): Eff[E with Es, T] => Eff[Es, T]` or
`translateOneEffect[E, E2, ES, T](...): Eff[E with Es, T] => Eff[E2 with Es, T]`.
It is possible to write interpreters for the concrete situations, but
just not generically.

Put similarly, the interpreters are not compositional. This is
easily demonstrated by the fact that the `Out` parameter must be
the same for all effects combined.

## Notes

Interpreters can be made by hand, or using existing tools such as
ReaderWriterState, monad transformers etc.
It might be interesting to add layers of effects using some kind of
monad transformer effect. :)

## Example

See src/main/scala/cakeff/example/Example.scala

    sbt run

