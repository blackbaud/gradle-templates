package com.blackbaud.service

import com.blackbaud.gatling.bbauth.BBAuthHelper
import com.blackbaud.gatling.SimulationConfiguration
import io.gatling.core.Predef.{exec, forever, scenario}
import io.gatling.http.Predef.http
import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.protocol.HttpProtocolBuilder
import scala.concurrent.duration._
import scala.language.postfixOps

class ServiceTest extends SimulationConfiguration {

  var baseURL = ""
  var requestName = ""
  var scenarioName = "SCENARIO_NAME"

  if(environment.equals("local")) {
    baseURL = "http://localhost:PORT"
    requestName = "REPLACE_ME"
  } else if(environment.equals("test")) {
    baseURL = "TEST_ENV_URL"
    requestName = "REPLACE_ME"
  } else if(environment.equals("prod")) {
    baseURL = "PROD_ENV_URL"
    requestName = "REPLACE_ME"
  }

  val httpConf = http
    .silentUri(BBAuthHelper.SILENCE_BBAUTH_REGEX)
    .baseURL(baseURL)

  val authWorkflow: ChainBuilder = exitBlockOnFail {
    exec(BBAuthHelper.performLoginForNonBBUserWithEnvId(username, password, environmentId))
      .exec(BBAuthHelper.putAccessTokenInSession())
      .exec { session =>
        accessToken = session("access_token").as[String]
        session
      }
  }

  val workflow: ChainBuilder = exitBlockOnFail {
    exec { session =>
      session.set("access_token", accessToken)
    }
      .exec(
        http(requestName)
          .get("")
          .header("Authorization", "Add Access Token Here")
      )
  }

  val authScenario: ScenarioBuilder = scenario("Authenticate User")
      .exec(
        exec(authWorkflow)
      )

    val yourTestScenario: ScenarioBuilder = scenario("Receipt Manager Get Gifts Performance Test")
      .exec(
        forever() { 
          exec(workflow)
        }
      )
  // Execute the scenarios
  //---------------------------------------------------------
  setUp(
      authScenario
        .inject(atOnceUsers(1)),
      yourTestScenario
        .inject(nothingFor(authScenarioWaitTime), rampUsers(userCount) during (rampUserOverSec seconds))
        .protocols(httpConf)
    ).maxDuration(authScenarioWaitTime + maxTestRuntimeSec)
  }
}
