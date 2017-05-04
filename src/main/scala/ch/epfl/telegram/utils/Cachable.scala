package ch.epfl.telegram.utils

trait Cachable[T] {

  private var obj = Option.empty[T]

  def setCached(obj: T): Unit = {
    this.obj = Some(obj)
  }

  def cached: Option[T] =
    obj
}
