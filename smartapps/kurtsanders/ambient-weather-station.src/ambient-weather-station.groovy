/*
*  Copyright 2019 SanderSoft™
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*  Ambient Weather Station
*
*  Author: Kurt Sanders, SanderSoft™
*
*  Date: 2018,2019
*/

import groovy.time.*
import java.text.DecimalFormat
import groovy.time.TimeCategory

//************************************ Version Specific ***********************************
String version()				{ return "V4.0.3" }
String appModified()			{ return "Apr-20-2019"}

//*************************************** Constants ***************************************
String appNameVersion() 		{ return "Ambient Weather Station ${version()}" }
String appShortName() 			{ return "STAmbientWeather ${version()}" }

String DTHName() 				{ return "Ambient Weather Station" }
String DTHRemoteSensorName() 	{ return "Ambient Weather Station Remote Sensor"}
String DTHDNI() 				{ return "${app.id}:MyAmbientWeatherStation" }
String DTHDNIRemoteSensorName() { return "${app.id}:MyAmbientRemoteSensor"}
String DTHDNIActionTiles() 		{ return "${app.id}:MyAmbientSmartWeatherStationTile" }

String DTHNameActionTiles() 	{ return "SmartWeather Station Tile" }
String AWSNameActionTiles()		{ return "SmartWeather" }
String AWSNameActionTilesHide()	{ return false }

String DTHnamespace()			{ return "kurtsanders" }
String appAuthor()	 			{ return "SanderSoft" }
String getAppImg(imgName) 		{ return "https://raw.githubusercontent.com/KurtSanders/STAmbientWeather/master/images/$imgName" }
String wikiURL(pageName)		{ return "https://github.com/KurtSanders/STAmbientWeather/wiki/$pageName"}
Integer wm2lux(value)			{ return (value * 126.7).toInteger() }
Integer wm2fc(value)			{ return (wm2lux(value) * 0.0929).toInteger() }
// ============================================================================================================
// This APP key is ONLY for this application - Do not copy or use elsewhere
String appKey() 				{return "33054086b3d745779f5ac35e147baa76f13e75d44ea245388ba598911905fb50"}
// ============================================================================================================

definition(
    name: 			"Ambient Weather Station",
    namespace: 		"kurtsanders",
    author: 		"kurt@kurtsanders.com",
    description: 	"Connect your Ambient™ Weather Station and remote sensors to SmartThings.  Get local forecast weather along with real-time data from your PWS",
    category: 		"My Apps",
    iconUrl:   		getAppImg("blue-ball-100.jpg"),
    iconX2Url: 		getAppImg("blue-ball-200.jpg"),
    iconX3Url: 		getAppImg("blue-ball.jpg"),
    singleInstance: false
)
{
// The following API Key is to be entered in this SmartApp's settings in the SmartThings IDE App Setting.
    appSetting 	"apiKey"
}
preferences {
    page(name: "mainPage")
    page(name: "optionsPage")
    page(name: "remoteSensorPage")
    page(name: "notifyPage")
}

def mainPage() {
    if (state.apiKey) {
        log.debug "The Ambient Weather API being used is: ${state.apiKey}"
    } else {
        state.apiKey = appSettings.apiKey
        log.debug "*NEW* Ambient Weather API = ${state.apiKey}"
    }
    def apiappSetupCompleteBool = state.apiKey?true:false
    def setupMessage = ""
    def setupTitle = "${appNameVersion()} API Check"
    def nextPageName = "optionsPage"
    state.retry = 0
    def getAmbientStationDataRC = getAmbientStationData()
    if (apiappSetupCompleteBool && getAmbientStationDataRC) {
        setupMessage = "SUCCESS! You have completed entering a valid Ambient API Key for ${appNameVersion()}. "
        setupMessage += (weatherStationMac)?"Please Press 'Next' for additional configuration choices.":"I found ${state.ambientMap.size()} reporting weather station(s)."
        setupTitle = "Please confirm the Ambient Weather Station Information below and if correct, Tap 'NEXT' to continue to the 'Settings' page'"
    } else {
        setupMessage = "Ambient API Setup INCOMPLETE or MISSING!\n\nPlease check and/or complete the REQUIRED Ambient Weather API key setup in the SmartThings IDE (App Settings Section) for ${appNameVersion()}.\n\nAPI Error message: ${state.httpError}"
        nextPageName = null
    }
    dynamicPage(name: "mainPage", title: setupTitle, submitOnChange: true, nextPage: nextPageName, uninstall:true, install:false) {
        section(hideable: apiappSetupCompleteBool, hidden: apiappSetupCompleteBool, setupMessage ) {
            paragraph "The API string key is used to securely connect your weather station to ${appNameVersion()}."
            paragraph image: getAppImg("blue-ball.jpg"),
                title: "Required API Key",
                required: false,
                informationList("apiHelp")
            href(name: "hrefReadme",
                 title: "${appNameVersion()} Setup/Read Me Page",
                 required: false,
                 style: "external",
                 url: "https://github.com/KurtSanders/STAmbientWeather",
                 description: "tap to view the Setup/Read Me page")
            href(name: "hrefAmbient",
                 title: "Ambient Weather Dashboard Account Page for API Key",
                 required: false,
                 style: "external",
                 url: "https://dashboard.ambientweather.net/account",
                 description: "tap to login and view your Ambient Weather's dashboard")
            href(name: "hrefUSA",
                 title: "SmartThings IDE USA",
                 required: false,
                 style: "external",
                 url: "https://graph.api.smartthings.com/",
                 description: "tap to view the US SmartThings IDE website in mobile browser")
            href(name: "hrefEurope",
                 title: "SmartThings IDE Europe",
                 required: false,
                 style: "external",
                 url: "https://graph-eu01-euwest1.api.smartthings.com/",
                 description: "tap to view the Europe SmartThings IDE website in mobile browser")
        }
        if (apiappSetupCompleteBool && getAmbientStationDataRC) {
            if (weatherStationMac) {
                setStateWeatherStationData()
                state.weatherStationMac = weatherStationMac
                countRemoteTempHumiditySensors()
                section ("Ambient Weather Station Information") {
                    paragraph image: getAppImg("blue-ball.jpg"),
                        title: "${state.weatherStationName}",
                        required: false,
                        "Location: ${state?.ambientMap[state.weatherStationDataIndex].info.location}" +
                        "\nMac Address: ${state.ambientMap[state.weatherStationDataIndex].macAddress}" +
                        "\nRemote Temp/Hydro Sensors: ${state.countRemoteTempHumiditySensors}"
                }
            } else {
                def weatherStationList = [:]
                state.ambientMap.each {
                    weatherStationList << [[ "${it.macAddress}" : "${it.info.name}${it.info.location?' @ ':''}${it.info.location}" ]]
                }
                section ("Ambient Weather Station Information") {
                    input (name: "weatherStationMac", submitOnChange: true, type: "enum",
                           title: "Select the Weather Station to Install",
                           options: weatherStationList,
                           multiple: false,
                           required: true
                          )
                }
            }
        }
        section ("STAmbientWeather™ - ${appAuthor()}") {
            href(name: "hrefVersions",
                 image: getAppImg("wi-direction-right.png"),
                 title: "${version()} : ${appModified()}",
                 required: false,
                 style:"embedded",
                 url: wikiURL("Features-by-Version")
                )
        }
    }
}

def optionsPage () {
    log.info "Ambient Weather Station: Mac: ${weatherStationMac}, Name/Loc: ${state.weatherStationName}/${state.ambientMap[state.weatherStationDataIndex].info.location}"
    def remoteSensorsExist = (state.countRemoteTempHumiditySensors>0)
    def lastPageName = remoteSensorsExist?"remoteSensorPage":""
    def i = 0
    def AWSBaseNameEnum = [:]
    for (i; i <= state.weatherStationName.length(); i++) {
        if (state.weatherStationName.substring(0,i)==state.weatherStationName.substring(0,i).trim()) {
            AWSBaseNameEnum << ["${i}":"${(i==0)?'No Prefix':state.weatherStationName.substring(0,i)}"]
        }
    }
    dynamicPage(name: "optionsPage", title: "Ambient Tile Settings for: '${state.weatherStationName}'",
                nextPage: lastPageName,
                uninstall:false,
                install : !remoteSensorsExist ) {
        section("Weather Station Options") {
            input ( name: "zipCode", type: "text",
                   title: "Enter a 'USA 5 digit ZipCode' for TWC Weather Forecasts, Moon Day, etc (Required)",
                   required: true
                  )
            input ( name: "schedulerFreq", type: "enum",
                   title: "Run Ambient Weather Station Refresh Every (X mins)?",
                   options: ['0':'Off','1':'1 min','2':'2 mins','3':'3 mins','4':'4 mins','5':'5 mins','10':'10 mins','15':'15 mins','30':'Every ½ Hour','60':'Every Hour','180':'Every 3 Hours'],
                   required: true
                  )
            if ( (!state.deviceId) && (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('tempinf')) ) {
                input ( name: "${DTHDNIRemoteSensorName()}0", type: "text",
                       title: "Weather Station Console Room Location Short Name",
                       required: true
                      )
            }
            input ( name: "solarRadiationTileDisplayUnits", type: "enum",
                   title: "Select Solar Radiation ('Light') Units of Measure",
                   options: ['W/m²':'Imperial Units (W/m²)','lux':'Metric Units (lux)', 'fc':'Foot Candles (fc)'],
                   required: true
                  )
            input ( name: "productIdentifierFilterList", type: "enum",
                   title: "Select Weather Alert Product Identifiers to Filter (Optional)",
                   options: alertFilterList(),
                   multiple: true,
                   required: false
                  )
            if (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('tempf')) {
                input ( name: "createActionTileDevice", type: "bool",
                       title: "Create ${DTHNameActionTiles()} for use as an ActionTiles™ SmartWeather Station Tile?",
                       required: false
                      )
            }
            href(name: "Define Weather Alerts/Notification",
                 title: "Weather Alerts/Notification",
                 required: false,
                 defaultValue: "Tap to Select",
                 page: "notifyPage")
            input ( name: "AWSBaseNameLength", type: "enum",
                   title: "Select the base prefix used for each weather device",
                   options: AWSBaseNameEnum,
                   defaultValue: "${state.weatherStationName}",
                   required: true
                  )
            label ( name: "name",
                   title: "This SmartApp's Name",
                   state: (name ? "complete" : null),
                   defaultValue: "${state.weatherStationName}",
                   required: true
                  )
        }
        section(hideable: true, hidden: true, "Optional: SmartThings IDE Live Logging Levels") {
            input (name: "debugVerbose", type: "bool",
                   title: "Show Debug Messages in Live Logging IDE",
                   required: false
                  )
            input ( name: "infoVerbose", type: "bool",
                   title: "Show Info Messages in Live Logging IDE",
                   required: false
                  )
            input ( name: "WUVerbose", type: "bool",
                   title: "Show Local Weather Info Messages in Live Logging IDE",
                   required: false
                  )
        }
    }
}
def remoteSensorPage() {
    dynamicPage(name: "remoteSensorPage", title: "Ambient Tile Settings", uninstall:false, install : true ) {
        def i = 1
        section("Provide Location names for your ${state?.countRemoteTempHumiditySensors} remote temperature/hydro sensors") {
            paragraph "Information Regarding Ambient Remote Sensors"
            paragraph image: getAppImg("blue-ball.jpg"),
                title: "Information, Issues and Instructions",
                required: false,
                "You MUST create short descriptive names for each remote sensor. Do not use special characters in the names.\n\n" +
                "If you wish to change the short name of the remote sensor, DO NOT change or rename them in the Device Tile or ST IDE 'My Devices' editor, as this app will rename them automatically when you SAVE the page.\n\n" +
                "Please note that remote sensors are numbered based in the bit switch on the sensor (1-8) and reported on Ambient Network API as 'tempNf' where N is an integer 1-8.  " +
                "If a remote sensor is deleted from your network or non responsive from your group of Ambient remote sensors, you may have to re-verify and/or rename the remainder of the remote sensors in this app and manually delete that sensor from the IDE 'My Devices' editor."
            for (i; i <= state?.countRemoteTempHumiditySensors; i++) {
                input "${DTHDNIRemoteSensorName()}${i}", type: "text",
                    title: "Ambient Remote Sensor #${i}",
                    required: true
            }
        }
    }
}

def notifyPage() {
    dynamicPage(name: "notifyPage", title: "Weather Alerts/Notification", uninstall: false, install: false) {
        section("Mobile SMS Notify Options") {
            input ( name: "mobilePhone", type: "phone",
                   title: "Required: Enter the mobile phone number to receive SMS weather events. Leave field blank to cancel all notifications",
                   required: true
                  )
            input ( name: "notifyAlertFreq", type: "enum",
                   required: true,
                   title: "Notify via SMS once every NUMBER of hours (Default is 24, Once/day)",
                   options: [1,2,4,6,12,24],
                   multiple: false
                  )
        }
        section ("Weather Station Notify Options") {
            input ( name: "notifySevereAlert", type: "bool", required: false,
                   title: "Notify when a SEVERE weather related ALERT is issued for your zipcode"
                  )
            if ( (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('tempf')) ) {
                input ( name: "notifyAlertLowTemp", type: "number", required: false,
                       title: "Notify when a temperature value is EQUAL OR BELOW this value. Leave field blank to cancel notification."
                      )
            }
            if ( (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('tempf')) ) {
                input ( name: "notifyAlertHighTemp", type: "number", required: false,
                       title: "Notify when a temperature value is EQUAL OR ABOVE this value.  Leave field blank to cancel notification."
                      )
            }
            if ( (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('hourlyrainin')) ) {
                input ( name: "notifyRain", type: "bool", required: false,
                       title: "Notify when RAIN is detected"
                      )
            }
        }
        section (hideable: true, hidden: true, "Last SMS Notifications") {
            paragraph image: "",
                required: false,
                SMSNotifcationHistory()
        }
    }
}


def initialize() {
    def now = now()
    // Initialize/Reset Alert Warnings DateTime values
    state.notifyAlertLowTempDT 		= state.notifyAlertLowTempDT?:now
    state.notifyAlertHighTempDT 	= state.notifyAlertHighTempDT?:now
    state.notifyRainDT 				= state.notifyRainDT?:now
    state.notifySevereAlertDT 		= state.notifySevereAlertDT?:now
    state.notifyAlertFreq 			= notifyAlertFreq?:24

    // Check for all devices needed to run this app
    addAmbientChildDevice()
    // Set user defined refresh rate
    if(state.schedulerFreq!=schedulerFreq) {
        log.info "Updating your Cron REFRESH schedule from ${state.schedulerFreq} mins to ${schedulerFreq} mins"
        state.schedulerFreq = schedulerFreq
        if(debugVerbose){log.debug "state.schedulerFreq->${state.schedulerFreq}"}
        setScheduler(schedulerFreq)
        def d = getChildDevice(state.deviceId)
        d.sendEvent(name: "scheduleFreqMin", value: state.schedulerFreq)
    }
}

def installed() {
    state.deviceId = DTHDNI()
    initialize()
    subscribe(app, appTouchHandler)
    runIn(10, main)
}

def updated() {
	initialize()
    runIn(10, main)
}

def uninstalled() {
    log.info "Removing Cron Job for Refresh"
    unschedule()
    // Remove all devices
    getAllChildDevices().each {
        log.info "Deleting Ambient device: ${it.label}"
        deleteChildDevice(it.deviceNetworkId)
    }
}

def scheduleCheckReset() {
    if (schedulerFreq!='0'){
        Date start = new Date()
        Date end = new Date()
        use( TimeCategory ) {
            end = start + schedulerFreq.toInteger().minutes
        }
        setScheduler(schedulerFreq)
        log.info "Reset the next CRON Refresh to ~${schedulerFreq} mins from now (${end.format("h:mm:ss a", location.timeZone)}) to avoid excessive HTTP requests"
    }
}

def appTouchHandler(evt="") {
    def timeStamp = new Date().format("h:mm:ss a", location.timeZone)
    log.info "App Touch: 'Refresh ALL' requested at ${timeStamp}"
    if (debugVerbose) {
        def children = app.getChildDevices()
        def thisdevice
        log.debug "SmartApp $app.name has ${children.size()} child devices"
        thisdevice = children.findAll { it.typeName }.sort { a, b -> a.deviceNetworkId <=> b.deviceNetworkId }.each {
            log.info "${it} <-> DNI: ${it.deviceNetworkId}"
        }
    }
    refresh()
}

def refresh() {
    log.info "Device: 'Refresh ALL'"
    scheduleCheckReset()
    main()
}

def autoScheduleHandler() {
    log.info "Executing Cron Schedule every ${schedulerFreq} min(s): 'Refresh ALL'"
    main()
}

def main() {
    def runID = new Random().nextInt(10000)
    if (state?.runID == runID as String) {
        log.warn "DUPLICATE EXECUTION RUN AVOIDED: Current runID: ${runID} Past runID: ${state?.runID}"
        return
    }
    state.runID = runID

    log.info "Main (#${runID}) Section: Executing Local Weather Routines for: ${zipCode} & Ambient Weather Station API's for: '${state.weatherStationName}'"

    // TWC Local Weather
    localWeatherInfo()

    // TWC Local Weather Alerts
    checkForSevereWeather()

    // Ambient Weather Station API
    ambientWeatherStation()

    // Notify Events Check
    notifyEvents()
}

def localWeatherInfo() {
    if(infoVerbose){log.info "Executing 'localWeatherInfo', zipcode: ${zipCode}"}
    if(infoVerbose){log.info "Getting TWC Current Weather Conditions"}
    // Verify zipCode for 5 digit numeric
    def zipcode = zipCode
    if (!zipcode.matches('^[0-9]{5}$')) {
        log.error "The zipCode entered ${zipCode} is an invalid USA 5 digit zipcode NNNNN'...  Using ST Hub's defualt zipcode"
        zipcode = ''
    }
    def obs = getTwcConditions(zipcode)
    def d = getChildDevice(state.deviceId)
    d.sendEvent(name:"lastSTupdate", value: tileLastUpdated(), displayed: false)
    state.weatherIcon = obs?.iconCode as String
    state.wxPhraseShort = obs?.wxPhraseShort
    if (obs) {
        d.sendEvent(name: "weatherIcon", value: state.weatherIcon, displayed: false)
    }

    if(infoVerbose){log.info "Getting TWC Location Info for ${zipcode}"}
    def loc = getTwcLocation(zipcode)?.location
    state.cityValue = "${loc?.city}, ${loc?.adminDistrictCode} ${loc.countryCode}"
    state.latitude = "${loc?.latitude}"
    state.longitude = "${loc?.longitude}"

    //Getting Sunrise & Sunset
    def dtf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    def sunriseDate = dtf.parse(obs.sunriseTimeLocal)
    def sunsetDate = dtf.parse(obs.sunsetTimeLocal)

    def tf = new java.text.SimpleDateFormat("h:mm")
    tf.setTimeZone(TimeZone.getTimeZone(loc.ianaTimeZone))

    state.localSunrise = "${tf.format(sunriseDate)}"
    state.localSunset = "${tf.format(sunsetDate)}"

    d.sendEvent(name: "localSunrise", value: state.localSunrise , displayed: false)
    d.sendEvent(name: "localSunset" , value: state.localSunset  , displayed: false)

    // Get the Weather Forecast
    if(infoVerbose){log.info "Getting TWC Forecast for ${state.cityValue}"}
    def f = getTwcForecast(zipcode)
    if (f) {
        state.forecastIcon = f.daypart[0].iconCode[0] ?: f.daypart[0].iconCode[1]
        state.precipChance = f.daypart[0].precipChance[0] ?: f.daypart[0].precipChance[1]
        def rainType = f.daypart[0].precipType[0] ?: f.daypart[0].precipType[1]
        def windPhrase = f.daypart[0].windPhrase[0] ?: f.daypart[0].windPhrase[1]
        d.sendEvent(name: "moonPhase", value: "Lunar Day: ${f.moonPhaseDay[0]}\n${f.moonPhase[0]}", displayed: false)
        d.sendEvent(name: "moonAge", value: "${f.moonPhaseDay[0]}", displayed: false)
        d.sendEvent(name: "rainForecast", value: "${rainType.capitalize()}\n${state.precipChance}%", displayed: false)
        d.sendEvent(name: "windPhrase", value: "${windPhrase}", displayed: false)

        def narrative = f.daypart[0].narrative
        def daypartName = f.daypart[0].daypartName
        def f2= sprintf(
            "%s forecast for %s\n%s %s %s %s %s %s",
            f.dayOfWeek[0],
            state.cityValue,
            daypartName[0]?:"",
            narrative[0]?narrative[0].uncapitalize():"",
            daypartName[1]?:"",
            narrative[1]?narrative[1].uncapitalize():"",
            daypartName[2]?:"",
            narrative[2]?narrative[2].uncapitalize():"",
        )
        d.sendEvent(name: "weather", value: f2, displayed: false)
    }
    else {
        log.warn "TWC Forecast Data not provided by API"
    }
}

def checkForSevereWeather() {
    if(infoVerbose){log.info "Getting TWC Alerts"}
    def d = getChildDevice(state.deviceId)
    def alertMsg = []
    def alertDescription = []
    def alertGeoLocation = ((state.latitude) && (state.longitude))?"${state.latitude},${state.longitude}":""
    def alerts = getTwcAlerts("${alertGeoLocation}")
    def timeStamp = new Date().format("h:mm:ss a", location.timeZone)
    def msg = ""
    // Filter Alerts
    if ((productIdentifierFilterList) && (alerts)) {
        def removeidx = []
        alerts.eachWithIndex { alert, idx ->
            productIdentifierFilterList.each { productIdentifier ->
                if (alert.productIdentifier.contains("${productIdentifier}")) {
                    log.info "Suppresing Weather Alert '${productIdentifier}' -> ${idx}:${alert}"
                    removeidx << idx
                }
            }
        }
        removeidx.each {
            alerts.remove(it)
        }
    }

    switch (alerts.size()) {
        case {it==1}:
        state.weatherAlerts = "(1 Alert) "
        break
        case {it>1}:
        state.weatherAlerts = "(${alerts.size()} Alerts) "
        break
        default:
            state.weatherAlerts = ""
        break
    }
    if (alerts) {
        alerts.each {alert ->
            msg += "${alert.productIdentifier} - ${alert.headlineText}"
            if (alert.effectiveTimeLocal && !msg.contains(" from ")) {
                msg += " from ${parseAlertTime(alert.effectiveTimeLocal).format("E hh:mm a", TimeZone.getTimeZone(alert.effectiveTimeLocalTimeZone))}"
            }
            if (alert.expireTimeLocal && !msg.contains(" until ")) {
                msg += " until ${parseAlertTime(alert.expireTimeLocal).format("E hh:mm a", TimeZone.getTimeZone(alert.expireTimeLocalTimeZone))}"
            }
            alertMsg << msg
            def detailKey = alert.detailKey
            def TwcAlertDetail = getTwcAlertDetail(detailKey)
            alertDescription << TwcAlertDetail.alertDetail.texts.description.join(",").replaceAll("[\\t\\n\\r&]+"," ").take((400/alerts.size()).toInteger())
            msg = ""
        }
    } else {
        alertMsg = "No current weather alerts for ${state.cityValue} at ${timeStamp}\nlatitude = ${state.latitude}º\nlongitude = ${state.longitude}º"
        if (productIdentifierFilterList) {
        alertMsg = "Filters: ${productIdentifierFilterList}\n${alertMsg}"
        }
    }
    if(infoVerbose){log.info "Alert msg: ${alertMsg}"}
    if(infoVerbose){log.info "Alert description: ${alertDescription}"}
    d.sendEvent(name: "alertMessage", value: informationList(alertMsg), displayed: false)
    d.sendEvent(name: "alertDescription", value: informationList(alertDescription), displayed: false)
    if ( (mobilePhone) && (notifySevereAlert) && (alerts) ) {
        if (lastNotifyDT(state.notifySevereAlertDT, "${alerts.size()} Weather Alert(s)")) {
            msg = "Ambient Weather Station ${state.weatherStationName}: SEVERE WEATHER ALERT for ${state.cityValue} at ${timeStamp}: ${alertMsg.join(', ')}"
            if(debugVerbose){log.debug "SMS: ${msg}"}
            state.notifySevereAlertDT = now()
            sendNotification("${msg}", [method: "both", phone: mobilePhone])
        }
    }
}

def ambientWeatherStation() {
    // Ambient Weather Station
    log.info "${app.name}: Executing 'Refresh Routine' auto every: ${schedulerFreq} min(s)"
    def d = getChildDevice(state.deviceId)
    def okTOSendEvent = true
    def remoteSensorDNI = ""
    def now = new Date().format('EEE MMM d, h:mm:ss a',location.timeZone)
    def nowTime = new Date().format('h:mm a',location.timeZone).toLowerCase()
    def currentDT = new Date()
    def tempUnits = getTemperatureScale()
    def windUnits = tempUnits == "C" ? "KPH" : "MPH"
    def measureUnits = tempUnits == "C" ? "CM" : "IN"
    def baroUnits = tempUnits == "C" ? "MMHG" : "INHg"
    def sendEventOptions = ""
    if (getAmbientStationData()) {
        if(debugVerbose){log.debug "httpget resp status = ${state.respStatus}"}
        if(infoVerbose){log.info "Processing Ambient Weather data returned from getAmbientStationData())"}
        setStateWeatherStationData()
        if(debugVerbose || infoVerbose) {
            state.ambientMap[state.weatherStationDataIndex].each{ k, v ->
                log.info "${k} = ${v}"
                if (k instanceof Map) {
                    k.each { x, y ->
                        log.info "${x} = ${y}"
                    }
                }
                if (v instanceof Map) {
                    v.each { x, y ->
                        log.info "${x} = ${y}"
                    }
                }
            }
        }
        if(debugVerbose){log.debug "Checking Weather Station data array for 'Last Rain Date' information..."}
        if (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('lastRain')) {
            if(debugVerbose){log.debug "Weather Station has 'Last Rain Date' information...Processing"}
            def dateRain = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", state.ambientMap[state.weatherStationDataIndex].lastData.lastRain)
            use (groovy.time.TimeCategory) {
                if(debugVerbose){log.debug ("lastRainDuration -> ${currentDT - dateRain}")}
                def lastRainDuration = currentDT - dateRain
                if (lastRainDuration) {
                    d.sendEvent(name:"lastRainDuration", value: lastRainDuration, displayed: false)
                }
            }
        } else {
            if(debugVerbose){log.debug "Weather Station does NOT provide 'Last Rain Date' information...Skipping"}
            d.sendEvent(name:"lastRainDuration", value: "N/A", displayed: false)
        }
        if ((state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('totalrainin')==false) || (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('yearlyrainin')==false)) {
            d.sendEvent(name:"totalrainin", value: "N/A", unit: measureUnits, displayed: false)
        }
        d.sendEvent(name:"lastSTupdate", value: tileLastUpdated(), displayed: false)
        d.sendEvent(name:"macAddress", value: state.ambientMap[state.weatherStationDataIndex].macAddress, displayed: false)

        if (!state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('feelsLike')) {
            if (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('humidity')) {
                d.sendEvent(name: 'secondaryControl', value: sprintf("%sHumidity is %s%% at %s", state.weatherAlerts, state.ambientMap[state.weatherStationDataIndex].lastData.humidity, nowTime), displayed: false )
            } else if (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('humidity1')) {
                d.sendEvent(name: 'secondaryControl', value: sprintf("%sHumidity is %s%% at %s", state.weatherAlerts, state.ambientMap[state.weatherStationDataIndex].lastData.humidity1, nowTime), displayed: false )
            } else {
                d.sendEvent(name: 'secondaryControl', value: state.weatherAlerts, displayed: false )
            }
        }

        // Update Main Weather Device with Remote Sensor 1 values if tempf does not exist, same with humidity
        if (!state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('tempf')) {
            if (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('temp1f')) {
                d.sendEvent(name:"temperature", value: state.ambientMap[state.weatherStationDataIndex].lastData.temp1f, units: tempUnits)
            }
            if (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('humidity1')) {
                d.sendEvent(name:"humidity", value: state.ambientMap[state.weatherStationDataIndex].lastData.humidity1, units: "%")
            }
        }
        // Update Main Weather Device with Remote Sensor 1 values if tempinf does not exist, same with humidityin
        if (!state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('tempinf')) {
            if(debugVerbose){log.debug "Fixing Main Station for inside temp"}
            if (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('temp1f')) {
                d.sendEvent(name:"tempinf", value: state.ambientMap[state.weatherStationDataIndex].lastData.temp1f, units: tempUnits)
            }
        }
        if (!state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('humidityin')) {
            if(debugVerbose){log.debug "Fixing Main Station for inside humidity"}
            if (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('humidity1')) {
                d.sendEvent(name:"humidityin", value: state.ambientMap[state.weatherStationDataIndex].lastData.humidity1, units: "%")
            }
        }

        state.ambientServerDate=Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", state.ambientMap[state.weatherStationDataIndex].lastData.date).format('EEE MMM d, h:mm:ss a',location.timeZone)

        state.ambientMap[state.weatherStationDataIndex].info.each{ k, v ->
            if(k=='name'){k='pwsName'}
            if(debugVerbose){log.debug "sendEvent(name: ${k}, value: ${v})"}
            d.sendEvent(name: k, value: v, displayed: false)
        }
        state.ambientMap[state.weatherStationDataIndex].lastData.each{ k, v ->
            // Send Real numeric weather values to devics hidden variables with _real suffix
            def realNumericWeatherVariablesList = [
                'windspeedmph',
                'windgustmph',
                'maxdailygust',
                'tempf_real',
                'hourlyrainin',
                'eventrainin',
                'dailyrainin',
                'weeklyrainin',
                'monthlyrainin',
                'totalrainin',
                'baromrelin',
                'baromabsin',
                'humidity',
                'tempinf',
                'humidityin',
                'solarradiation',
                'feelsLike',
                'dewPoint'
            ]
			if (realNumericWeatherVariablesList.contains(k)) {
                d.sendEvent(name: "${k}_real", value: v , displayed: false )
            }

            okTOSendEvent = true
            // SmartThings devices automatically force a 'round down' on ALL displayed numeric values in the Tile when less than 0.1 but GT 0.
            // Therefore, when an Ambient sensor reports a sensor that is below 0.1 but greater than 0, the value will be rounded up to .1
            try {
                if( (v.isNumber()) && (v>0) && (v<0.1) ) {
                    if(debugVerbose){log.debug "${k}: Rounding small weather number '${v}'<0.1 -> 0.1} for Tile Display"}
                    v = 0.1
                }
            }
            catch (e) {
                log.error("caught exception assigning ${k} : ${v} to a value of 0.1", e)
            }
            switch (k) {
                case 'dateutc':
                okTOSendEvent = false
                break
                case 'date':
                v = state.ambientServerDate
                break
                case 'battout':
                k='battery'
                v=v.toInteger()*100
                break
                case ~/^batt[0-9].*/:
                okTOSendEvent = false
                break
                case 'lastRain':
                v=Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", v).format('EEE MMM d, h:mm a',location.timeZone)
                break
                case 'tempf':
                k='temperature'
                break
                case 'hourlyrainin':
                def waterState = state.ambientMap[state.weatherStationDataIndex].lastData?.hourlyrainin?.toFloat()>0?'wet':'dry'
                if(debugVerbose){log.debug "water -> ${waterState}"}
                d.sendEvent(name:'water', value: waterState)
                break
                case 'feelsLike':
                def scText
                switch(true) {
                    case {( (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('hourlyrainin')) && (state.ambientMap[state.weatherStationDataIndex].lastData?.hourlyrainin.toFloat()>0 ))} :
                        DecimalFormat df = new DecimalFormat("0.00")
                    def numberForRainLevel = df.format(state.ambientMap[state.weatherStationDataIndex].lastData.hourlyrainin)
                    scText = sprintf("%sRaining at %s in/hr at %s", state.weatherAlerts,numberForRainLevel, nowTime)
                    break
                    case { (v > state.ambientMap[state.weatherStationDataIndex].lastData.tempf) } :
                        scText = sprintf("%sFeels like %sº at %s", state.weatherAlerts, v, nowTime)
                    break
                    case { ( (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('windspeedmph')) && (state.ambientMap[state.weatherStationDataIndex].lastData.windspeedmph.toFloat()>0)) } :
                        scText = sprintf("%sWind is %s %s %s at %s", state.weatherAlerts, state.ambientMap[state.weatherStationDataIndex].lastData.windspeedmph, windUnits, degToCompass(state.ambientMap[state.weatherStationDataIndex].lastData?.winddir, true), nowTime)
                    break
                    default :
                    scText = sprintf("%sHumidity is %s%% at %s", state.weatherAlerts, state.ambientMap[state.weatherStationDataIndex].lastData.humidity, nowTime)
                    break
                }
                d.sendEvent(name: 'secondaryControl', value: scText, displayed: false )
                break
                case 'windspeedmph':
                def motionState = state.ambientMap[state.weatherStationDataIndex].lastData?.windspeedmph?.toFloat()>0?'active':'inactive'
                if(debugVerbose){log.debug "Wind motion -> ${motionState}"}
                d.sendEvent(name:'motion', value: motionState)
                // Send windSpeed as power for pseudo tiles in ActionTiles™
                d.sendEvent(name: "power", value: v,  displayed: false)
                break
                case 'maxdailygust':
                // Send maxdailygust as energy for pseudo tiles in ActionTiles™
                d.sendEvent(name: "energy", value: v, displayed: false)
                break
                case 'winddir':
                def winddirectionState = degToCompass(state.ambientMap[state.weatherStationDataIndex].lastData?.winddir, true)
                if(debugVerbose){log.debug "Wind Direction -> ${winddirectionState}"}
                d.sendEvent(name:'winddirection', value: winddirectionState, displayed: false)
                d.sendEvent(name:'winddir2', value: winddirectionState + " (" + state.ambientMap[state.weatherStationDataIndex].lastData.winddir + "º)")
                break
                case 'uv':
                def UVInumRange
                switch (v) {
                    case {it < 3}:
                    UVInumRange="Low (${v})"
                    break
                    case {it < 6}:
                    UVInumRange="Medium (${v})"
                    break
                    case {it < 8}:
                    UVInumRange="High (${v})"
                    break
                    case {it < 11}:
                    UVInumRange="Very High (${v})"
                    break
                    default:
                    UVInumRange="Extreme (${v})"
                    break
                }
                d.sendEvent(name: 'ultravioletIndexDisplay', value: UVInumRange )
                k='ultravioletIndex'
                break
                case 'yearlyrainin':
                k='totalrainin'
                break
                case 'solarradiation':
                v = v.toInteger()
                    switch(solarRadiationTileDisplayUnits) {
                        case ('lux'):
                    v = wm2lux(v)
                        break
                        case ('fc'):
                    v = wm2fc(v)
                        break
                        default:
                            break
                    }
                d.sendEvent(name: k, value: sprintf("%,7d %s",v,solarRadiationTileDisplayUnits?:'W/m²'), units: solarRadiationTileDisplayUnits?:'W/m²')
                    k='illuminance'
                break
                // Weather Console Sensors
                case 'tempinf':
                remoteSensorDNI = getChildDevice("${DTHDNIRemoteSensorName()}0")
                if (remoteSensorDNI) {
                    if(debugVerbose){log.debug "Posted temperature with value ${v} -> ${remoteSensorDNI}"}
                    remoteSensorDNI.sendEvent(name: "temperature", value: v, units: tempUnits)
                    remoteSensorDNI.sendEvent(name:"date", value: state.ambientServerDate, displayed: false)
                    remoteSensorDNI.sendEvent(name:"lastSTupdate", value: tileLastUpdated(), displayed: false)
                    if (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('battout')) {
                        remoteSensorDNI.sendEvent(name:"battery", value: state.ambientMap[state.weatherStationDataIndex].lastData.battout.toInteger()*100, displayed: false)
                    }
                } else {
                    log.error "Missing ${DTHDNIRemoteSensorName()}0"
                }
                break
                case 'humidityin':
                remoteSensorDNI = getChildDevice("${DTHDNIRemoteSensorName()}0")
                if (remoteSensorDNI) {
                    if(debugVerbose){log.debug "Posted humidity with value ${v} -> ${remoteSensorDNI}"}
                    remoteSensorDNI.sendEvent(name: "humidity", value: v, units: "%")
                } else {
                    log.error "Missing ${DTHDNIRemoteSensorName()}0"
                }
                break
                // Post values for remote temperature & humidity sensors
                case ~/^temp[0-9][0-9]?f$|^soiltemp[0-9][0-9]?$/:
                remoteSensorDNI = getChildDevice("${DTHDNIRemoteSensorName()}${k.findAll( /\d+/ )[0]}")
                if(debugVerbose){log.debug "${k} = ${remoteSensorDNI}"}
                if (remoteSensorDNI) {
                    if(debugVerbose){log.debug "Posted temperature with value ${v} -> ${remoteSensorDNI}"}
                    remoteSensorDNI.sendEvent(name: "temperature", value: v, units: tempUnits)
                    remoteSensorDNI.sendEvent(name:"lastSTupdate", value: tileLastUpdated(), displayed: false)
                    remoteSensorDNI.sendEvent(name:"date", value: state.ambientServerDate, displayed: false)
                    if (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey("batt${k[4..4]}")) {
                        remoteSensorDNI.sendEvent(name:"battery", value: state.ambientMap[state.weatherStationDataIndex].lastData."batt${k[4..4]}".toInteger()*100, displayed: false)
                    }

                } else {
                    log.error "Missing ST Device ${DTHDNIRemoteSensorName()}${k.findAll( /\d+/ )[0]} for ${k}"
                }
                okTOSendEvent = false
                break
                case ~/^humidity[0-9][0-9]?$|^soilhum[0-9][0-9]?$/:
                remoteSensorDNI = getChildDevice("${DTHDNIRemoteSensorName()}${k.findAll( /\d+/ )[0]}")
                if(debugVerbose){log.debug "${k} = ${remoteSensorDNI}"}
                if (remoteSensorDNI) {
                    if(debugVerbose){log.debug "Posted humidity with value ${v} -> ${remoteSensorDNI}"}
                    remoteSensorDNI.sendEvent(name: "humidity", value: v, units: "%")
                } else {
                    log.error "Missing ST Device ${DTHDNIRemoteSensorName()}${k.findAll( /\d+/ )[0]} for ${k}"
                }
                okTOSendEvent = false
                break
                default:
                    break
            }
            if (okTOSendEvent){
                // , unit: "%"
                switch (k) {
                    case ('battery'):
                    case ('date'):
                    sendEventOptions = [displayed : false]
                    break
                    case ~/^temp.*/:
                    sendEventOptions = [units : tempUnits]
                    break
                    case ('feelsLike'):
                    case ('dewPoint'):
                    sendEventOptions = [units : tempUnits]
                    d.sendEvent(name: k.toLowerCase(), value: v, units : tempUnits, displayed: false )
                    break
                    case ('illuminance'):
                    sendEventOptions = [units: solarRadiationTileDisplayUnits?:'W/m²']
                    break
                    case ~/^humidity.*/:
                    sendEventOptions = [units : '%']
                    break
                    case ~/.*rain.*/:
                    sendEventOptions = [units : measureUnits]
                    break
                    case ('windir'):
                    sendEventOptions = [units : 'º']
                    break
                    case ~/^wind.*/:
                    case ('maxdailygust'):
                    sendEventOptions = [units : windUnits]
                    break
                    case ~/^barom.*/:
                    sendEventOptions = [units : baroUnits]
                    break
                    default:
                        sendEventOptions = []
                    break
                }
                sendEventOptions << [name: k, value: v]
                d.sendEvent(sendEventOptions)
                if(debugVerbose){log.debug "${d}.sendEvent(${sendEventOptions})"}
                sendEventOptions = []
            }
        }
    } else {
        if(debugVerbose){log.debug "getAmbientStationData() did not return any weather data"}
    }
    if (createActionTileDevice) {
        // Update kurtsanders:SmartWeather Station Tile device for ActionTiles™
        d = getChildDevice(DTHDNIActionTiles())
        d.sendEvent(name: "localSunrise", 		value : state.localSunrise, displayed: false)
        d.sendEvent(name: "localSunset", 		value : state.localSunset, displayed: false)
        d.sendEvent(name: "weatherIcon", 		value : state.weatherIcon as String)
        d.sendEvent(name: "forecastIcon", 		value : state.forecastIcon as String)
        d.sendEvent(name: "weather", 			value : state?.wxPhraseShort)
        d.sendEvent(name: "percentPrecip", 		value : state.precipChance, unit: "%")
        d.sendEvent(name: "city", 				value : "Ambient - ${state.weatherStationName}")
        d.sendEvent(name: "location", 			value : state.cityValue, displayed: false)
        d.sendEvent(name: "temperature", 		value : state.ambientMap[state.weatherStationDataIndex].lastData?.tempf, unit: tempUnits)
        d.sendEvent(name: "humidity", 			value : state.ambientMap[state.weatherStationDataIndex].lastData?.humidity, unit: "%")
        d.sendEvent(name: "feelsLike", 			value : state.ambientMap[state.weatherStationDataIndex].lastData?.feelsLike, unit: tempUnits)
        d.sendEvent(name: "wind",				value : state.ambientMap[state.weatherStationDataIndex].lastData?.windspeedmph ?:"0", unit: windUnits)
        d.sendEvent(name: "windVector",			value : "${degToCompass(state.ambientMap[state.weatherStationDataIndex].lastData?.winddir, false)} ${state.ambientMap[state.weatherStationDataIndex].lastData.windspeedmph?:""} ${windUnits}")
        d.sendEvent(name: "lastUpdate", 		value : tileLastUpdated(), displayed: false)
    }
}

def getAmbientStationData() {
	if(!infoVerbose){log.info "Start: getAmbientStationData()"}
    if(!state.apiKey){
        log.error("Severe Error: The API key is UNDEFINED in ${app.name}'s IDE 'App Settings' field, fatal error now exiting")
        return false
    }
    state.retry = state.retry?:0
    if (state?.retry.toInteger()>0) {
        log.info "Executing Retry getAmbientStationData() re-attempt #${state.retry}"
    }
    def params = [
        uri			: "http://api.ambientweather.net/v1/devices?applicationKey=${appKey()}&apiKey=${state.apiKey}"
    ]
    try {
        httpGet(params) { resp ->
            // get the data from the response body
            state.ambientMap = resp.data
            state.respStatus = resp.status
            if (resp.status != 200) {
                log.error "AmbientWeather.Net: response status code: ${resp.status}: response: ${resp.data}"
                return false
            }
            if (state.weatherStationDataIndex) {
                countRemoteTempHumiditySensors()
            }
            if (state.retry.toInteger()>0) {
                log.info "Success: Retry getAmbientStationData() re-attempt #${state.retry}"
                state.retry = 0
            }
        }
    } catch (e) {
        log.warn("Ambient Weather Station API Data: ${e}")
        state.httpError = e.toString()
        if (e.toString().contains("Unauthorized")) {
            return false
        }
        state.retry = state.retry.toInteger() + 1
        if (state.retry.toInteger()<3) {
            log.info("Waiting 10 seconds to Try Again: Attempt #${state.retry}")
            runIn(10, ambientWeatherStation)
        }
        return false
    }
    if(infoVerbose){log.info "End: getAmbientStationData()"}
    return true
}

def addAmbientChildDevice() {
    // add Ambient Weather Reporter Station devices
    // Derive a Short Name for the Weather Station and Remote Sensors
    def AWSBaseName = state.weatherStationName?:"${app.name}"
    def AWSBaseNameLengthNum
    try {
        AWSBaseNameLengthNum = AWSBaseNameLength?AWSBaseNameLength.toInteger().abs():AWSBaseNameLength.length()
    } catch (e) {
        log.warn "Invalid AWSBaseNameLength '${AWSBaseNameLength}' ${e}: Using '${AWSBaseName.length()}' as default prefix length"
        AWSBaseNameLengthNum = AWSBaseName.length()
    }
    AWSBaseName = "${AWSBaseName.substring(0,AWSBaseNameLengthNum<=AWSBaseName.length()?AWSBaseNameLengthNum:AWSBaseName.length())}"
    AWSBaseName = AWSBaseNameLengthNum==0?'':"${AWSBaseName} - "
    // Create/Validate Weather Console Device
    def AWSName =  "${AWSBaseName}${AWSBaseNameLengthNum==0?'Weather Console':'Console'}"
    def AWSDNI = getChildDevice(state.deviceId)
    if (!AWSDNI) {
        log.info "NEW: Adding Ambient Device: ${AWSName} with DNI: ${state.deviceId}"
        try {
            addChildDevice(DTHnamespace(), DTHName(), DTHDNI(), null, ["name": AWSName, "label": AWSName, completedSetup: true])
        } catch(physicalgraph.app.exception.UnknownDeviceTypeException ex) {
            log.error "The Ambient Weather Device Handler '${DTHName()}' was not found in your 'My Device Handlers', Error-> '${ex}'.  Please install this in the IDE's 'My Device Handlers'"
            return false
        }
        log.info "Success: Added ${AWSName} with DNI: ${DTHDNI()}"
    } else {
        log.info "Verified Weather Station '${getChildDevice(state.deviceId)}' = DNI: '${DTHDNI()}'"
        if ( (AWSDNI.label == AWSName) && (AWSDNI.name == AWSName) ) {
            log.info "Device Label/Name Pref Match: Name: ${AWSDNI.name} = Label: ${AWSDNI.label} -> NO CHANGE"
        } else {
            log.warn "Device Label/Name Pref Mis-Match: Name: ${AWSDNI.name} <> Label: ${AWSDNI.label} <> Device Label: ${AWSName} -> RENAMING"
            AWSDNI.label = AWSName
            AWSDNI.name  = AWSName
            log.warn "Successfully Renamed Device Label for: ${AWSDNI}"
        }
    }

    // add Ambient Weather SmartWeather Station Tile for ActionTiles™ Integration
    def actionTileDNI = getChildDevice(DTHDNIActionTiles())
    def AWSSmartWeatherName = "${AWSBaseName}${AWSNameActionTiles()}"
    if (createActionTileDevice) {
        if (!actionTileDNI) {
            log.info "NEW: Adding Ambient ActionTiles™ Device: '${AWSSmartWeatherName}' with DNI: '${DTHDNIActionTiles()}'"
            try {
                addChildDevice(DTHnamespace(), DTHNameActionTiles(), DTHDNIActionTiles(), null, ["name": AWSSmartWeatherName, "label": AWSSmartWeatherName, isComponent: AWSNameActionTilesHide(), completedSetup: true])
            } catch(physicalgraph.app.exception.UnknownDeviceTypeException ex) {
                log.error "The Ambient Weather ActionTiles™ Device Handler '${DTHName()}' was not found in your 'My Device Handlers', Error-> '${ex}'.  Please install this in the IDE's 'My Device Handlers'"
                return false
            }
            log.info "Success: Added ${AWSSmartWeatherName} with DNI: ${DTHDNIActionTiles()}"
        } else {
            log.info "Verified Weather Station '${getChildDevice(DTHDNIActionTiles())}' = DNI: '${DTHDNIActionTiles()}'"
            if ( (actionTileDNI.label == AWSSmartWeatherName) && (actionTileDNI.name == AWSSmartWeatherName) ) {
                log.info "Device Label/Name Pref Match: Name: ${actionTileDNI.name} = Label: ${actionTileDNI.label} -> NO CHANGE"
            } else {
                log.warn "Device Label/Name Pref Mis-Match: Name: ${actionTileDNI.name} <> Label: ${actionTileDNI.label} <> Device Label: ${AWSSmartWeatherName} -> RENAMING"
                actionTileDNI.label = AWSSmartWeatherName
                actionTileDNI.name  = AWSSmartWeatherName
                log.warn "Successfully Renamed Device Label for: ${actionTileDNI}"
            }
        }
    } else {
        if (actionTileDNI) {
            log.info "Deleting ${AWSSmartWeatherName} with DNI: ${DTHDNIActionTiles()}"
            deleteChildDevice(actionTileDNI)
        }
    }

    // add Ambient Weather Remote Sensor Device(s)
    def remoteSensorNamePref
    def remoteSensorNameDNI
    def remoteSensorNumber
    settings.each { key, value ->
        if ( key.startsWith(DTHDNIRemoteSensorName()) ) {
            remoteSensorNamePref = "${AWSBaseName}${value}"
            remoteSensorNameDNI = getChildDevice(key)
            remoteSensorNumber = key.reverse()[0..0]
            if (remoteSensorNumber.toInteger() <= state.countRemoteTempHumiditySensors.toInteger()) {
                if (!remoteSensorNameDNI) {
                    log.info "NEW: Adding Remote Sensor: ${remoteSensorNamePref}"
                    try {
                        addChildDevice(DTHnamespace(), DTHRemoteSensorName(), "${key}", null, ["name": remoteSensorNamePref, "label": remoteSensorNamePref, completedSetup: true])
                    } catch(physicalgraph.app.exception.UnknownDeviceTypeException ex) {
                        log.error "The Ambient Weather Device Handler '${DTHRemoteSensorName()}' was not found in your 'My Device Handlers', Error-> '${ex}'.  Please install this in the IDE's 'My Device Handlers'"
                        return false
                    }
                    log.info "Success Added Ambient Remote Sensor: ${remoteSensorNamePref} with DNI: ${key}"
                } else {
                    if(infoVerbose){log.info "Verified Remote Sensor #${remoteSensorNumber} of ${state.countRemoteTempHumiditySensors} Exists: ${remoteSensorNamePref} = DNI: ${key}"}
                    if ( (remoteSensorNameDNI.label == remoteSensorNamePref) && (remoteSensorNameDNI.name == remoteSensorNamePref) ) {
                        if(infoVerbose){
                            log.info "Device Label/Name Pref Match: Name: ${remoteSensorNameDNI.name} = Label: ${remoteSensorNameDNI.label} == Pref Label: ${remoteSensorNamePref} -> NO CHANGE"
                        }
                    } else {
                        log.warn "Device Label/Name Pref Mis-Match: Name: ${remoteSensorNameDNI.name} <> Label: ${remoteSensorNameDNI.label} <> Pref Label: ${remoteSensorNamePref} -> RENAMING"
                        remoteSensorNameDNI.label = remoteSensorNamePref
                        remoteSensorNameDNI.name  = remoteSensorNamePref
                        log.warn "Successfully Renamed Device Label and Names for: ${remoteSensorNameDNI}"
                    }
                }
            } else {
                log.warn "Device ${remoteSensorNumber} DNI: ${key} '${remoteSensorNameDNI.name}' exceeds # of remote sensors (${state.countRemoteTempHumiditySensors}) reporting from Ambient -> ACTION REQUIRED"
                log.warn "Please verify that all Ambient Remote Sensors are online and reporting to Ambient Network.  If so, please manual delete the device in the SmartThings 'My Devices' view"
            }
        }
    }
}

def degToCompass(num,longTitles=true) {
    if (num) {
        def val = Math.floor((num.toFloat() / 22.5) + 0.5).toInteger()
        def arr = []
        if (longTitles) {
            arr = ["N", "North NE", "NE", "East NE", "E", "East SE", "SE", "South SE", "S", "South SW", "SW", "West SW", "W", "West NW", "NW", "North NW"]
        } else {
            arr = ["N", "N NE", "NE", "E NE", "E", "E SE", "SE", "S SE", "S", "S SW", "SW", "W SW", "W", "W NW", "NW", "N NW"]
        }
        return arr[(val % 16)]
    }
    return "N/A"
}


def setScheduler(schedulerFreq) {
    if(infoVerbose){log.info "Section: setScheduler(${schedulerFreq})"}
    def scheduleHandler = 'autoScheduleHandler'
    unschedule(scheduleHandler)
    if(infoVerbose){log.info "Auto Schedule Refresh Rate is now -> ${schedulerFreq} mins"}
    switch(schedulerFreq) {
        case '0':
        log.info "Auto Schedule Refresh Rate is now: OFF"
        break
        case '1':
        runEvery1Minute(scheduleHandler)
        break
        case '2':
        schedule("20 0/2 * * * ?",scheduleHandler)
        break
        case '3':
        schedule("20 0/3 * * * ?",scheduleHandler)
        break
        case '4':
        schedule("20 0/4 * * * ?",scheduleHandler)
        break
        case '5':
        runEvery5Minutes(scheduleHandler)
        break
        case '10':
        runEvery10Minutes(scheduleHandler)
        break
        case '15':
        runEvery15Minutes(scheduleHandler)
        break
        case '30':
        runEvery30Minutes(scheduleHandler)
        break
        case '60':
        runEvery1Hour(scheduleHandler)
        break
        case '180':
        runEvery3Hours(scheduleHandler)
        break
        default :
        unschedule()
        break
    }
}

def tileLastUpdated() {
    def now = new Date().format('EEE MMM d, h:mm:ss a',location.timeZone)
    return sprintf("%s Tile Last Refreshed at\n%s","${version()}", now)
}
def informationList(variable) {
    switch(variable) {
        case ("apiHelp") :
        // Help Text for API Key
        variable =  [
            "You MUST enter your Ambient Weather API key in the ${appNameVersion()} SmartApp Settings section.",
            "Visit your Ambient Weather Dashboards's Account page.",
            "Create/Copy your API key from the bottom of the page",
            "Return to your SmartThings IDE 'My SmartApps' browser page.",
            "EDIT the ${appNameVersion()} SmartApp.",
            "Press the App Settings button at the top right of the page.",
            "Scroll down the page, expand the 'Settings' section.",
            "Enter or paste your Ambient API key in the API value input box.",
            "Press Update on bottom of page to save.",
            "Exit the SmartApp and Start ${appNameVersion()} Setup again on your mobile phone."
        ]
        break
        default:
            break
    }
    if (variable instanceof List) {
        def numberedText = ""
        variable.eachWithIndex { item, index ->
            numberedText += "${index+1}. ${item}"
            numberedText += (index<variable.size()-1)?"\n":''
        }
        return numberedText
    }
    return variable
}

def parseAlertTime(s) {
    def dtf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    def s2 = s.replaceAll(/([0-9][0-9]):([0-9][0-9])$/,'$1$2')
    dtf.parse(s2)
}

def SMSNotifcationHistory() {
    def date = now()
    def dateToday = date
    def today = new Date(date).format("MMM-DD-YYYY", location.timeZone)
    def msg = ""
    def notifyVars = ["Severe Weather"	: state.notifySevereAlertDT ]
    if ( (state.deviceId) && (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('tempf')) ) {
        notifyVars << ["Low Temp" : state.notifyAlertLowTempDT, "High Temp" : state.notifyAlertHighTempDT]
    }
    if ( (state.deviceId) && (state.ambientMap[state.weatherStationDataIndex].lastData.containsKey('hourlyrainin')) ) {
        notifyVars << ["Rain Detected" : state.notifyRainDT]
    }

    notifyVars.eachWithIndex { k, index ->
        if (k.value) {
            date = new Date(k.value).format("MMM-DD-YYYY h:mm a", location.timeZone)
            dateToday = new Date(k.value).format("MMM-DD-YYYY", location.timeZone)
            if (today==dateToday) {
                date = "Today @ " + new Date(k.value).format("h:mm a", location.timeZone)
            }
            msg += "${index+1}) ${k.key} : ${date}\n"
        } else {
            msg += "${index+1}) ${k.key} : --\n"
        }
    }
    return msg
}

def notifyEvents() {
    if (mobilePhone){
        def now = now()
        //        state.notifyAlertLowTempDT = now-3600000*2
        def msg
        def ambientWeatherStationName = "${DTHName()} - '${state.weatherStationName}'"
        if ( (notifyAlertLowTemp) && (state.ambientMap[state.weatherStationDataIndex].lastData?.tempf<=notifyAlertLowTemp.toInteger()) ) {
            msg = "${ambientWeatherStationName}: LOW TEMP ALERT:  Current temperature of ${state.ambientMap[state.weatherStationDataIndex].lastData?.tempf}º <= ${notifyAlertLowTemp}º"
            if (lastNotifyDT(state.notifyAlertLowTempDT, "Low Temp")) {
                if(debugVerbose){log.debug "SMS: ${msg}"}
                sendNotification("${msg}", [method: "both", phone: mobilePhone])
                state.notifyAlertLowTempDT = now
            }
        }
        if ( (notifyAlertHighTemp) && (state.ambientMap[state.weatherStationDataIndex].lastData?.tempf.toInteger()>=notifyAlertHighTemp) ) {
            msg = "${ambientWeatherStationName}: HIGH TEMP ALERT:  Current temperature of ${state.ambientMap[state.weatherStationDataIndex].lastData?.tempf}º >= ${notifyAlertHighTemp}º"
            if (lastNotifyDT(state.notifyAlertHighTempDT, "High Temp")) {
                if(debugVerbose){log.debug "SMS: ${msg}"}
                state.notifyAlertHighTempDT = now
                sendNotification("${msg}", [method: "both", phone: mobilePhone])
            }
        }
        if ( (notifyRain) && (state.ambientMap[state.weatherStationDataIndex].lastData?.hourlyrainin.toFloat()>0) ){
            log.debug "${ambientWeatherStationName}: RAIN DETECTED ALERT: Current hourly rain sensor reading of ${state.ambientMap[state.weatherStationDataIndex].lastData?.hourlyrainin} in/hr"
            if (lastNotifyDT(state.notifyRainDT, "Rain")) {
                if(debugVerbose){log.debug "SMS: ${msg}"}
                state.notifyRainDT = now
                sendNotification("${msg}", [method: "both", phone: mobilePhone])
            }
        }
    }
}

def lastNotifyDT(lastDT, eventName) {
    if (!lastDT) { return true }
    def now = now()/1000
    def date = new Date(lastDT).format("MMM-DD-YYYY h:mm:ss a", location.timeZone)
    def hours = ((now-(lastDT/1000))/3600).toFloat().round(1)
    def days = (hours/24).toFloat().round(1)
    def rc = hours>=state.notifyAlertFreq.toInteger()
    if(infoVerbose){log.info "This '${eventName}' event was last sent on ${date}: ${days} days, ${hours} hours ago"}
    if(infoVerbose){log.info "${eventName} Alert Every ${notifyAlertFreq} hours: ${rc?'OK to SMS':'TOO EARLY TO SEND'}"}
    return rc
}

def setStateWeatherStationData() {
    if (weatherStationMac) {
        state.weatherStationDataIndex = state.ambientMap.findIndexOf {
            it.macAddress in [weatherStationMac]
        }
    }
    state.weatherStationDataIndex = state.weatherStationDataIndex?:0
    state.weatherStationMac = state.weatherStationMac?:state.ambientMap[state.weatherStationDataIndex].macAddress
    countRemoteTempHumiditySensors()
    state.weatherStationName = state.ambientMap[state.weatherStationDataIndex].info.name?:state.ambientMap[state.weatherStationDataIndex].info.location
}

def countRemoteTempHumiditySensors() {
    state.countRemoteTempHumiditySensors =  state.ambientMap[state.weatherStationDataIndex].lastData.keySet().count { it.matches('^temp[0-9][0-9]?f|^soiltemp[0-9]?[0-9]?') }
    return state.countRemoteTempHumiditySensors
}

def alertFilterList() {
    def x = [
        "ABV":"ABV - Rawinsonde Data Above 100 Millibars",
        "ADA":"ADA - Alarm/Alert Administrative Msg",
        "ADM":"ADM - Alert Administrative Message",
        "ADR":"ADR - NWS Administrative Message",
        "ADV":"ADV - Generic Space Environment Advisory",
        "AFD":"AFD - Area Forecast Discussion",
        "AFM":"AFM - Area Forecast Matrices",
        "AFP":"AFP - Area Forecast Product",
        "AFW":"AFW - Fire Weather Matrix",
        "AGF":"AGF - Agricultural Forecast",
        "AGO":"AGO - Agricultural Observations",
        "ALT":"ALT - Space Environment Alert",
        "AQA":"AQA - Air Quality Alert",
        "AQI":"AQI - Air Quality Index Statement",
        "ASA":"ASA - Air Stagnation Advisory",
        "AVA":"AVA - Avalanche Watch",
        "AVW":"AVW - Avalanche Warning",
        "AWO":"AWO - Area Weather Outlook",
        "AWS":"AWS - Area Weather Summary",
        "AWU":"AWU - Area Weather Update",
        "AWW":"AWW - Airport Weather Warning",
        "BOY":"BOY - Buoy Report",
        "BRG":"BRG - Coast Guard Observations",
        "BRT":"BRT - Hourly Roundup for Weather Radio",
        "CAE":"CAE - Child Abduction Emergency",
        "CCF":"CCF - Coded City Forecast",
        "CDW":"CDW - Civil Danger Warning",
        "CEM":"CEM - Civil Emergency Message",
        "CF6":"CF6 - WFO Monthly/Daily Climate Data",
        "CFP":"CFP - Convective Forecast Product",
        "CFW":"CFW - Coastal Flood Warnings/Watches/Statements",
        "CGR":"CGR - Coast Guard Surface Report",
        "CHG":"CHG - Computer Hurricane Guidance",
        "CLA":"CLA - Climatological Report (Annual)",
        "CLI":"CLI - Climatological Report (Daily)",
        "CLM":"CLM - Climatological Report (Monthly)",
        "CLQ":"CLQ - Climatological Report (Quarterly)",
        "CLS":"CLS - Climatological Report (Seasonal)",
        "CLT":"CLT - Climate Report",
        "CMM":"CMM - Coded Climatological Monthly Means",
        "COD":"COD - Coded Analysis and Forecasts",
        "CPF":"CPF - Great Lakes Port Forecast",
        "CUR":"CUR - Routine Space Environment Products",
        "CWA":"CWA - Center (CWSU) Weather Advisory",
        "CWF":"CWF - Coastal Waters Forecast",
        "CWS":"CWS - Center (CWSU) Weather Statement",
        "DAY":"DAY - Routine Space Environment Product (Daily)",
        "DDO":"DDO - Daily Dispersion Outlook",
        "DGT":"DGT - Drought Information Statement",
        "DSA":"DSA - Unnumbered Depression / Suspicious Area Advisory",
        "DSM":"DSM - ASOS Daily Summary",
        "DSW":"DSW - Dust Storm Warning and Dust Advisory",
        "EFP":"EFP - 3 To 5 Day Extended Forecast",
        "EOL":"EOL - Average 6 To 10 Day Weather Outlook (Local)",
        "EQI":"EQI - Tsunami Bulletin",
        "EQR":"EQR - Earthquake Report",
        "EQW":"EQW - Earthquake Warning",
        "ESF":"ESF - Flood Potential Outlook",
        "ESG":"ESG - Extended Streamflow Guidance",
        "ESP":"ESP - Extended Streamflow Prediction",
        "ESS":"ESS - Water Supply Outlook",
        "EVI":"EVI - Evacuation Immediate",
        "EWW":"EWW - Extreme Wind Warning",
        "FA0":"FA0 - Aviation Area Forecasts (Pacific)",
        "FA1":"FA1 - Aviation Area Forecasts (Northeast)",
        "FA2":"FA2 - Aviation Area Forecasts (Southeast)",
        "FA3":"FA3 - Aviation Area Forecasts (North Central)",
        "FA4":"FA4 - Aviation Area Forecasts (South Central)",
        "FA5":"FA5 - Aviation Area Forecasts (Rocky Mountains)",
        "FA6":"FA6 - Aviation Area Forecasts (West Coast)",
        "FA7":"FA7 - Aviation Area Forecasts (Juneau, AK)",
        "FA8":"FA8 - Aviation Area Forecasts (Anchorage, AK)",
        "FA9":"FA9 - Aviation Area Forecasts (Fairbanks, AK)",
        "FD0":"FD0 - 24 Hr Fd Winds Aloft Fcst (45,000 and 53,000 Ft)",
        "FD1":"FD1 - 6 Hour Winds Aloft Forecast",
        "FD2":"FD2 - 12 Hour Winds Aloft Forecast",
        "FD3":"FD3 - 24 Hour Winds Aloft Forecast",
        "FD4":"FD4 - Winds Aloft Forecast",
        "FD5":"FD5 - Winds Aloft Forecast",
        "FD6":"FD6 - Winds Aloft Forecast",
        "FD7":"FD7 - Winds Aloft Forecast",
        "FD8":"FD8 - 6 Hour Fd Winds Aloft Fcst (45,000 and 53,000 Ft)",
        "FD9":"FD9 - 12 Hr Fd Winds Aloft Fcst (45,000 and 53,000 Ft)",
        "FDI":"FDI - Fire Danger Indices",
        "FFA":"FFA - Flash Flood Watch",
        "FFG":"FFG - Flash Flood Guidance",
        "FFH":"FFH - Headwater Guidance",
        "FFS":"FFS - Flash Flood Statement",
        "FFW":"FFW - Flash Flood Warning",
        "FLN":"FLN - National Flood Summary",
        "FLS":"FLS - Flood Statement",
        "FLW":"FLW - Flood Warning",
        "FOF":"FOF - Upper Wind Fallout Forecast",
        "FRW":"FRW - Fire Warning",
        "FSH":"FSH - Natl Marine Fisheries Administrative Service Message",
        "FTM":"FTM - WSR-88D Radar Outage Notification / Free Text Message",
        "FTP":"FTP - FOUS Prog Max/Min Temp/Pop Guidance",
        "FWA":"FWA - Fire Weather Administrative Message",
        "FWD":"FWD - Fire Weather Outlook Discussion",
        "FWF":"FWF - Routine Fire Wx Fcst (With/Without 6-10 Day Outlook)",
        "FWL":"FWL - Land Management Forecasts",
        "FWM":"FWM - Miscellaneous Fire Weather Product",
        "FWN":"FWN - Fire Weather Notification",
        "FWO":"FWO - Fire Weather Observation",
        "FWS":"FWS - Suppression Forecast",
        "FZL":"FZL - Freezing Level Data (RADAT)",
        "GLF":"GLF - Great Lakes Forecast",
        "GLS":"GLS - Great Lakes Storm Summary",
        "GRE":"GRE - GREEN",
        "HD1":"HD1 - RFC Derived QPF Data Product",
        "HD2":"HD2 - RFC Derived QPF Data Product",
        "HD3":"HD3 - RFC Derived QPF Data Product",
        "HD4":"HD4 - RFC Derived QPF Data Product",
        "HD7":"HD7 - RFC Derived QPF Data Product",
        "HD8":"HD8 - RFC Derived QPF Data Product",
        "HD9":"HD9 - RFC Derived QPF Data Product",
        "HLS":"HLS - Hurricane Local Statement",
        "HMD":"HMD - Hydrometeorological Discussion",
        "HML":"HML - AHPS XML",
        "HMW":"HMW - Hazardous Materials Warning",
        "HP1":"HP1 - RFC QPF Verification Product",
        "HP2":"HP2 - RFC QPF Verification Product",
        "HP3":"HP3 - RFC QPF Verification Product",
        "HP4":"HP4 - RFC QPF Verification Product",
        "HP5":"HP5 - RFC QPF Verification Product",
        "HP6":"HP6 - RFC QPF Verification Product",
        "HP7":"HP7 - RFC QPF Verification Product",
        "HP8":"HP8 - RFC QPF Verification Product",
        "HRR":"HRR - Weather Roundup",
        "HSF":"HSF - High Seas Forecast",
        "HWO":"HWO - Hazardous Weather Outlook",
        "HWR":"HWR - Hourly Weather Roundup",
        "HYD":"HYD - Daily Hydrometeorological Products",
        "HYM":"HYM - Monthly Hydrometeorological Plain Language Product",
        "ICE":"ICE - Ice Forecast",
        "IDM":"IDM - Ice Drift Vectors",
        "INI":"INI - ADMINISTR [NOUS51 KWBC]",
        "IOB":"IOB - Ice Observation",
        "KPA":"KPA - Keep Alive Message",
        "LAE":"LAE - Local Area Emergency",
        "LCD":"LCD - Preliminary Local Climatological Data",
        "LCO":"LCO - Local Cooperative Observation",
        "LEW":"LEW - Law Enforcement Warning",
        "LFP":"LFP - Local Forecast",
        "LKE":"LKE - Lake Stages",
        "LLS":"LLS - Low-Level Sounding",
        "LOW":"LOW - Low Temperatures",
        "LSR":"LSR - Local Storm Report",
        "LTG":"LTG - Lightning Data",
        "MAN":"MAN - Rawinsonde Observation Mandatory Levels",
        "MAP":"MAP - Mean Areal Precipitation",
        "MAW":"MAW - Amended Marine Forecast",
        "MFM":"MFM - Marine Forecast Matrix",
        "MIM":"MIM - Marine Interpretation Message",
        "MIS":"MIS - Miscellaneous Local Product",
        "MOB":"MOB - MOB Observations",
        "MON":"MON - Routine Space Environment Product Issued Monthly",
        "MRP":"MRP - Techniques Development Laboratory Marine Product",
        "MSM":"MSM - ASOS Monthly Summary Message",
        "MTR":"MTR - METAR Formatted Surface Weather Observation",
        "MTT":"MTT - METAR Test Message",
        "MVF":"MVF - Marine Verification Coded Message",
        "MWS":"MWS - Marine Weather Statement",
        "MWW":"MWW - Marine Weather Message",
        "NOU":"NOU - Weather Reconnaisance Flights",
        "NOW":"NOW - Short Term Forecast",
        "NOX":"NOX - Data Mgt Message",
        "NPW":"NPW - Non-Precipitation Warnings / Watches / Advisories",
        "NSH":"NSH - Nearshore Marine Forecast",
        "NUW":"NUW - Nuclear Power Plant Warning",
        "NWR":"NWR - NOAA Weather Radio Forecast",
        "OAV":"OAV - Other Aviation Products",
        "OBS":"OBS - Observations",
        "OFA":"OFA - Offshore Aviation Area Forecast",
        "OFF":"OFF - Offshore Forecast",
        "OMR":"OMR - Other Marine Products",
        "OPU":"OPU - Other Public Products",
        "OSO":"OSO - Other Surface Observations",
        "OSW":"OSW - Ocean Surface Winds",
        "OUA":"OUA - Other Upper Air Data",
        "OZF":"OZF - Zone Forecast",
        "PFM":"PFM - Point Forecast Matrices",
        "PFW":"PFW - Fire Weather Point Forecast Matrices",
        "PLS":"PLS - Plain Language Ship Report",
        "PMD":"PMD - Prognostic Meteorological Discussion",
        "PNS":"PNS - Public Information Statement",
        "POE":"POE - Probability of Exceed",
        "PRB":"PRB - Heat Index Forecast Tables",
        "PRC":"PRC - State Pilot Report Collective",
        "PRE":"PRE - Preliminary Forecasts",
        "PSH":"PSH - Post Storm Hurricane Report",
        "PTS":"PTS - Probabilistic Outlook Points",
        "PWO":"PWO - Public Severe Weather Outlook",
        "PWS":"PWS - Tropical Cyclone Probabilities",
        "QPF":"QPF - Quantitative Precipitation Forecast",
        "QPS":"QPS - Quantitative Precipitation Statement",
        "RDF":"RDF - Revised Digital Forecast",
        "REC":"REC - Recreational Report",
        "RER":"RER - Record Report",
        "RET":"RET - EAS Activation Request",
        "RFD":"RFD - Rangeland Fire Danger Forecast",
        "RFI":"RFI - RFI Observation",
        "RFR":"RFR - Route Forecast",
        "RFW":"RFW - Red Flag Warning",
        "RHW":"RHW - Radiological Hazard Warning",
        "RNS":"RNS - Rain Information Statement",
        "RR1":"RR1 - Hydro-Met Data Report Part 1",
        "RR2":"RR2 - Hydro-Met Data Report Part 2",
        "RR3":"RR3 - Hydro-Met Data Report Part 3",
        "RR4":"RR4 - Hydro-Met Data Report Part 4",
        "RR5":"RR5 - Hydro-Met Data Report Part 5",
        "RR6":"RR6 - Hydro-Met Data Report Part 6",
        "RR7":"RR7 - Hydro-Met Data Report Part 7",
        "RR8":"RR8 - Hydro-Met Data Report Part 8",
        "RR9":"RR9 - Hydro-Met Data Report Part 9",
        "RRA":"RRA - Automated Hydrologic Observation Sta Report (AHOS)",
        "RRM":"RRM - Miscellaneous Hydrologic Data",
        "RRS":"RRS - HADS Data",
        "RRY":"RRY - ASOS SHEF Hourly Routine Test Message",
        "RSD":"RSD - Daily Snotel Data",
        "RSM":"RSM - Monthly Snotel Data",
        "RTP":"RTP - Regional Max/Min Temp and Precipitation Table",
        "RVA":"RVA - River Summary",
        "RVD":"RVD - Daily River Forecasts",
        "RVF":"RVF - River Forecast",
        "RVI":"RVI - River Ice Statement",
        "RVM":"RVM - Miscellaneous River Product",
        "RVR":"RVR - River Recreation Statement",
        "RVS":"RVS - River Statement",
        "RWR":"RWR - Regional Weather Roundup",
        "RWS":"RWS - Regional Weather Summary",
        "SAB":"SAB - Special Avalanche Bulletin",
        "SAF":"SAF - Speci Agri Wx Fcst / Advisory / Flying Farmer Fcst Outlook",
        "SAG":"SAG - Snow Avalanche Guidance",
        "SAT":"SAT - APT Prediction",
        "SAW":"SAW - Prelim Notice of Watch & Cancellation Msg (Aviation)",
        "SCC":"SCC - Storm Summary",
        "SCD":"SCD - Supplementary Climatological Data (ASOS)",
        "SCN":"SCN - Soil Climate Analysis Network Data",
        "SCP":"SCP - Satellite Cloud Product",
        "SCS":"SCS - Selected Cities Summary",
        "SDO":"SDO - Supplementary Data Observation (ASOS)",
        "SDS":"SDS - Special Dispersion Statement",
        "SEL":"SEL - Severe Local Storm Watch and Watch Cancellation Msg",
        "SEV":"SEV - SPC Watch Point Information Message",
        "SFP":"SFP - State Forecast",
        "SFT":"SFT - Tabular State Forecast",
        "SGL":"SGL - Rawinsonde Observation Significant Levels",
        "SHP":"SHP - Surface Ship Report at Synoptic Time",
        "SIG":"SIG - International Sigmet / Convective Sigmet",
        "SIM":"SIM - Satellite Interpretation Message",
        "SLS":"SLS - Severe Local Storm Watch and Areal Outline",
        "SMF":"SMF - Smoke Management Weather Forecast",
        "SMW":"SMW - Special Marine Warning",
        "SOO":"SOO - SOO Product",
        "SPE":"SPE - Satellite Precipitation Estimates (TXUS20 KWBC)",
        "SPF":"SPF - Storm Strike Probability Bulletin (TPC)",
        "SPS":"SPS - Special Weather Statement",
        "SPW":"SPW - Shelter in Place Warning",
        "SQW":"SQW - Snow Squall Warning",
        "SRD":"SRD - Surf Discussion",
        "SRF":"SRF - Surf Forecast",
        "SRG":"SRG - Soaring Guidance",
        "SSM":"SSM - Main Synoptic Hour Surface Observation",
        "STA":"STA - Network and Severe Weather Statistical Summaries",
        "STD":"STD - Satellite Tropical Disturbance Summary",
        "STO":"STO - Road Condition Reports (State Agencies)",
        "STP":"STP - State Max/Min Temperature and Precipitation Table",
        "STQ":"STQ - Spot Forecast Request",
        "SUM":"SUM - Space Weather Message",
        "SVR":"SVR - Severe Thunderstorm Warning",
        "SVS":"SVS - Severe Weather Statement",
        "SWO":"SWO - Severe Storm Outlook Narrative (AC)",
        "SWS":"SWS - State Weather Summary",
        "SYN":"SYN - Regional Weather Synopsis",
        "TAF":"TAF - Terminal Aerodrome Forecast",
        "TAP":"TAP - Terminal Alerting Products",
        "TAV":"TAV - Travelers Forecast Table",
        "TCA":"TCA - Aviation Tropical Cyclone Advisory",
        "TCD":"TCD - Tropical Cyclone Discussion",
        "TCE":"TCE - Tropical Cyclone Position Estimate",
        "TCM":"TCM - Marine/Aviation Tropical Cyclone Advisory",
        "TCP":"TCP - Public Tropical Cyclone Advisory",
        "TCS":"TCS - Satellite Tropical Cyclone Summary",
        "TCU":"TCU - Tropical Cyclone Update",
        "TCV":"TCV - Tropical Cyclone Watch/Warning Break Points",
        "TIB":"TIB - Tsunami Bulletin",
        "TID":"TID - Tide Report",
        "TMA":"TMA - Tsunami Tide/Seismic Message Acknowledgement",
        "TOE":"TOE - 911 Telephone Outage Emergency",
        "TOR":"TOR - Tornado Warning",
        "TPT":"TPT - Temperature Precipitation Table (Natl and Intnl)",
        "TSU":"TSU - Tsunami Watch/Warning",
        "TUV":"TUV - Weather Bulletin",
        "TVL":"TVL - Travelers Forecast",
        "TWB":"TWB - Transcribed Weather Broadcast",
        "TWD":"TWD - Tropical Weather Discussion",
        "TWO":"TWO - Tropical Weather Outlook and Summary",
        "TWS":"TWS - Tropical Weather Summary",
        "URN":"URN - Aircraft Reconnaissance",
        "UVI":"UVI - Ultraviolet Index",
        "VAA":"VAA - Volcanic Activity Advisory",
        "VER":"VER - Forecast Verification Statistics",
        "VFT":"VFT - Terminal Aerodrome Forecast (TAF) Verification",
        "VOW":"VOW - Volcano Warning",
        "WA0":"WA0 - Airmet (Pacific)",
        "WA1":"WA1 - Airmet (Northeast)",
        "WA2":"WA2 - Airmet (Southeast)",
        "WA3":"WA3 - Airmet (North Central)",
        "WA4":"WA4 - Airmet (South Central)",
        "WA5":"WA5 - Airmet (Rocky Mountains)",
        "WA6":"WA6 - Airmet (West Coast)",
        "WA7":"WA7 - Airmet (Juneau, AK)",
        "WA8":"WA8 - Airmet (Anchorage, AK)",
        "WA9":"WA9 - Airmet (Fairbanks, AK)",
        "WAR":"WAR - Space Environment Warning",
        "WAT":"WAT - Space Environment Watch",
        "WCN":"WCN - Weather Watch Clearance Notification",
        "WCR":"WCR - Weekly Weather and Crop Report",
        "WDA":"WDA - Weekly Data for Agriculture",
        "WDU":"WDU - Warning Decision Update",
        "WEK":"WEK - Routine Space Environment Product Issued Weekly",
        "WOU":"WOU - Tornado/Severe Thunderstorm Watch",
        "WS1":"WS1 - Sigmet (Northeast)",
        "WS2":"WS2 - Sigmet (Southeast)",
        "WS3":"WS3 - Sigmet (North Central)",
        "WS4":"WS4 - Sigmet (South Central)",
        "WS5":"WS5 - Sigmet (Rocky Mountains)",
        "WS6":"WS6 - Sigmet (West Coast)",
        "WST":"WST - Tropical Cyclone Sigmet",
        "WSV":"WSV - Volcanic Activity Sigmet",
        "WSW":"WSW - Winter Weather Warnings / Watches / Advisories",
        "WWA":"WWA - Watch Status Report",
        "WWP":"WWP - Severe Thunderstorm / Tornado Watch Probabilities",
        "ZFP":"ZFP - Zone Forecast Product"
    ]
    return x
}