package ch.epfl.telegram

import io.circe.generic.JsonCodec
import com.sksamuel.elastic4s.ElasticDsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object ExampleElasticsearch extends App {

  // should not use any waiting, for example purpose only
  def wait[T](f: Future[T]): T =
    Await.result(f, 10.seconds)

  // case classes
  @JsonCodec case class Place(name: String, code: Int)
  @JsonCodec case class User(name: String, place: Place)

  // elasticsearch connection is very slow 5s
  // !! is it also "near real time only" so insert + query one after the other will NOT show any results

  val index = "users"

  // index / type (read on elasticsearch documentation)
  val students = index / "students"

  val existing = wait {
    ElasticSearch {
      indexExists(index)
    }
  }

  if (!existing.isExists) {
    // or curl http://localhost:9200/users -X PUT
    wait {
      ElasticSearch {
        createIndex(index)
      }
    }
  }

  // use @JsonCodec from circe for elasticsearch
  import com.sksamuel.elastic4s.circe._

  val test = User("hey", Place("here", 42))

  wait {
    ElasticSearch {
      indexInto(students) doc test
    }
  }

  /*
{
   "took": 1,
   "timed_out": false,
   "_shards": {
      "total": 15,
      "successful": 15,
      "failed": 0
   },
   "hits": {
      "total": 1,
      "max_score": 1,
      "hits": [
         {
            "_index": "users",
            "_type": "students",
            "_id": "AVjP55perSkFuqRaoZcO",
            "_score": 1,
            "_source": {
               "name": "hey",
               "place": {
                  "name": "here",
                  "code": 42
               }
            }
         }
      ]
   }
}
   */

  wait {
    ElasticSearch {
      indexInto(students) doc test.copy(name = "moris")
    }
  }

  val first = wait {
    ElasticSearch {
      search(students)
    } map(_.to[User])
  }

  println(first) // should show nothing

  Thread.sleep(500)

  val second = wait {
    ElasticSearch {
      search(students)
    } map(_.to[User])
  }

  println(second) // Vector(User(hey,Place(here,42)), User(moris,Place(here,42)))

}
