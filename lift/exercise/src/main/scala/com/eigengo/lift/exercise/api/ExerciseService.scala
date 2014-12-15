package com.eigengo.lift.exercise.api

import java.util.UUID

import akka.actor.{ActorRef, Address}
import cakesolutions.api.RestApi
import com.eigengo.lift.exercise.ExerciseClassifiers.{GetMuscleGroups, MuscleGroup}
import com.eigengo.lift.exercise.UserExercises.{UserExerciseDataProcess, UserExerciseSessionEnd, UserExerciseSessionStart}
import com.eigengo.lift.exercise.UserExercisesView.{ExerciseSession, SessionSummary, UserGetExerciseSession, UserGetExerciseSessionsSummary}
import com.eigengo.lift.exercise.{ExerciseMarshallers, SessionProps}
import scodec.bits.BitVector
import spray.routing.Directives

import scala.concurrent.ExecutionContext

trait ExerciseService extends Directives with ExerciseMarshallers {
  import akka.pattern.ask
  import com.eigengo.lift.common.Timeouts.defaults._

  def boot(address: Address, userExercises: ActorRef, userExercisesView: ActorRef, exerciseClassifiers: ActorRef) = RestApi(
    route = Some({ ec: ExecutionContext => exerciseRoute(userExercises, userExercisesView, exerciseClassifiers)(ec) })
  )

  private def exerciseRoute(userExercises: ActorRef, userExercisesView: ActorRef, exerciseClassifiers: ActorRef)(implicit ec: ExecutionContext) =
    path("exercise" / "musclegroups") {
      get {
        complete {
          (exerciseClassifiers ? GetMuscleGroups).mapTo[List[MuscleGroup]]
        }
      }
    } ~
    path("exercise" / UserIdValue) { userId ⇒
      post {
        handleWith { sessionProps: SessionProps ⇒
          (userExercises ? UserExerciseSessionStart(userId, sessionProps)).mapRight[UUID]
        }
      } ~
      get {
        complete {
          (userExercisesView ? UserGetExerciseSessionsSummary(userId)).mapTo[List[SessionSummary]]
        }
      }
    } ~
    path("exercise" / UserIdValue / SessionIdValue) { (userId, sessionId) ⇒
      get {
        complete {
          (userExercisesView ? UserGetExerciseSession(userId, sessionId)).mapTo[Option[ExerciseSession]]
        }
      } ~
      put {
        handleWith { bits: BitVector ⇒
          (userExercises ? UserExerciseDataProcess(userId, sessionId, bits)).mapRight[Unit]
        }
      } ~
      delete {
        complete {
          (userExercises ? UserExerciseSessionEnd(userId, sessionId)).mapRight[Unit]
        }
      }
    }

}
