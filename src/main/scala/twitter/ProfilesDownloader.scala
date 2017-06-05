package twitter

import akka.actor.Actor
import akka.event.LoggingReceive
import com.danielasfregola.twitter4s.TwitterRestClient
import twitter.ProfilesDownloader.DownloadResult

import scala.concurrent.Await
import scala.concurrent.duration._

class ProfilesDownloader extends Actor {

  val restClient = TwitterRestClient()

  def downloadProfiles(query: String, number: BigDecimal, current: Long, page: Integer): Seq[Long] = {
    if (current > number)
      return Seq.empty

    val future = restClient.searchForUser(query, page = page, include_entities = false)
    val result = Await.result(future, 100 second)

    result.data.foreach(d => println(d.id, d.location))
    result.data.map(f => f.id) ++ downloadProfiles(query, number, current + result.data.size, page + 1)
  }

  override def receive: Receive = LoggingReceive {
    case ProfilesDownloader.DownloadProfilesByLatLong(lat, long, range, number) =>
      sender() ! DownloadResult(downloadProfiles(s"geocode:$lat,$long,${range}km", number, 0, 1))
    case ProfilesDownloader.DownloadProfilesByLocation(location, number) =>
      sender() ! DownloadResult(downloadProfiles(s"from:$location", number, 0, 1))
  }
}

object ProfilesDownloader {

  case class DownloadProfilesByLatLong(lat: String, long: String, range: BigDecimal = 10, number: BigDecimal = 100)

  case class DownloadProfilesByLocation(location: String, number: BigDecimal = 100)

  case class DownloadResult(data: Seq[Long])

}

