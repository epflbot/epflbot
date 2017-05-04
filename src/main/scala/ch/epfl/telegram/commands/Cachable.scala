package ch.epfl.telegram.commands

trait Cachable[T] {

  private var obj = Option.empty[T]

  def setCached(obj: T): Unit = {
    this.obj = Some(obj)
  }

  def cached: Option[T] =
    obj
}
