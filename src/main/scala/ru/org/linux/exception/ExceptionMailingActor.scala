package ru.org.linux.exception

import java.util.Date
import javax.mail.internet.{AddressException, InternetAddress}
import javax.mail.{Message, Transport}

import akka.actor.{Actor, ActorLogging, Props}
import ru.org.linux.email.EmailService
import ru.org.linux.exception.ExceptionMailingActor._
import ru.org.linux.spring.SiteConfig

import scala.concurrent.duration._
import scala.language.existentials
import scala.util.control.NonFatal

class ExceptionMailingActor(siteConfig: SiteConfig) extends Actor with ActorLogging {
  import context.dispatcher

  context.system.scheduler.schedule(ResetAt, ResetAt, self, Reset)

  private var count = 0
  private var currentTypes = Set.empty[String]

  override def receive: Receive = {
    case Report(ex, msg) ⇒
      count += 1
      currentTypes = currentTypes + ex.toString

      if (count < MaxMessages || !currentTypes.contains(ex.toString)) {
        sendErrorMail(s"Linux.org.ru: $ex", msg)
      } else {
        log.warning(s"Too many errors; skipped logging of $ex")
      }
    case Reset ⇒
      if (count >= MaxMessages) {
        sendErrorMail(s"Linux.org.ru: high exception rate ($count in $ResetAt)", currentTypes.mkString("\n"))
      }

      count = 0
      currentTypes = Set.empty[String]
  }
  
  private def sendErrorMail(subject: String, text: String): Boolean = {
    val adminEmailAddress = siteConfig.getAdminEmailAddress

    val emailMessage = EmailService.createMessage

    try {
      val mail = new InternetAddress(adminEmailAddress, true)

      emailMessage.setFrom(new InternetAddress("no-reply@linux.org.ru"))
      emailMessage.addRecipient(Message.RecipientType.TO, mail)
      emailMessage.setSubject(subject)
      emailMessage.setSentDate(new Date)
      emailMessage.setText(text.toString, "UTF-8")
      Transport.send(emailMessage)

      log.info(s"Sent crash report to $adminEmailAddress")

      true
    } catch {
      case e: AddressException =>
        log.warning(s"Неправильный e-mail адрес: $adminEmailAddress")
        false
      case NonFatal(e) =>
        log.error(e, "An error occured while sending e-mail!")
        false
    }
  }
}

object ExceptionMailingActor {
  case class Report(ex: Class[_ <: Throwable], msg: String)
  case object Reset

  val ResetAt = 5 minutes
  val MaxMessages = 5

  def props(siteConfig: SiteConfig) = Props(classOf[ExceptionMailingActor], siteConfig)
}
