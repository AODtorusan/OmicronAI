package be.angelcorp.omicron.base.util

import akka.event.{SubchannelClassification, EventBus}
import akka.actor.ActorRef
import akka.util.Subclassification

/**
 * A basic event bus capable of sending [[AnyRef]] messages,
 * and where subscribes subscribe a specific message class (an all there sub-classes)
 */
class GenericEventBus extends EventBus with SubchannelClassification {
  type Event = AnyRef
  type Classifier = Class[_]
  type Subscriber = ActorRef

  override protected implicit val subclassification = new Subclassification[Class[_]] {
    def isEqual(x: Class[_], y: Class[_]) = x == y
    def isSubclass(x: Class[_], y: Class[_]) = y isAssignableFrom x
  }

  override protected def classify(event: AnyRef): Class[_] =
    event.getClass

  override protected def publish(event: AnyRef, subscriber: ActorRef): Unit =
    subscriber ! event

}
