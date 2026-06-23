// Copyright (c) Decisa. MIT licensed. See LICENSE.

package ai.decisa.sdk

/** The canonical pixel event names accepted by `POST /v1/track`. */
enum class DecisaEventName(val wireName: String) {
    PageView("PageView"),
    ViewContent("ViewContent"),
    Search("Search"),
    AddToCart("AddToCart"),
    AddPaymentInfo("AddPaymentInfo"),
    InitiateCheckout("InitiateCheckout"),
    Lead("Lead"),
    CompleteRegistration("CompleteRegistration"),
    Purchase("Purchase"),
    StartTrial("StartTrial"),
    Subscribe("Subscribe"),
    AppInstall("AppInstall"),
    Custom("Custom"),
}
