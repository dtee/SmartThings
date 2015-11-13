/**
 *  Rule
 *
 *  Copyright 2015 Bruce Ravenel
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
 */
definition(
    name: "Rule",
    namespace: "bravenel",
    author: "Bruce Ravenel",
    description: "Rule",
    category: "Convenience",
    parent: "bravenel:Rule Machine",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps@2x.png"
)

preferences {
	page(name: "selectRule")
	page(name: "selectConditions")
	page(name: "defineRule")
	page(name: "certainTime")
    page(name: "selectActionsTrue")
    page(name: "selectActionsFalse")
    page(name: "selectMsgTrue")
    page(name: "selectMsgFalse")
}

def selectRule() {
	dynamicPage(name: "selectRule", title: "Select Conditions, Rule and Results", uninstall: true, install: true) {
		section() {     
			label title: "Name the Rule", required: true
			input "howMany", "number", title: "How many conditions?", required: true, range: "1..*" //temp fix for Android bug
			def condLabel = conditionLabel()
			href "selectConditions", title: "Define Conditions", description: condLabel ?: "Tap to set", required: true, state: condLabel ? "complete" : null
			href "defineRule", title: "Define the Rule", description: state.str ? state.str : "Tap to set", state: state.str ? "complete" : null
            href "selectActionsTrue", title: "Select the Actions for True", description: state.actsTrue ? state.actsTrue : "Tap to set", state: state.actsTrue ? "complete" : null
            href "selectActionsFalse", title: "Select the Actions for False", description: state.actsFalse ? state.actsFalse : "Tap to set", state: state.actsFalse ? "complete" : null
        }
        section() {mode(title: "Restrict to specific mode(s)")}
	}
}

def selectConditions() {
	dynamicPage(name: "selectConditions", title: "Select Conditions", uninstall: false) {
//		section("") {input "howMany", "number", title: "How many conditions?", required: true, range: "1..*", submitOnChange: true}
		if(howMany) {
			for (int i = 1; i <= howMany; i++) {
				def thisCapab = "rCapab$i"
				section("Condtion #$i") {
					getCapab(thisCapab)
					def myCapab = settings.find {it.key == thisCapab}
					if(myCapab) {
						def xCapab = myCapab.value
						if(!(xCapab in ["Time of day", "Days of week", "Mode"])) {
							def thisDev = "rDev$i"
							getDevs(xCapab, thisDev)
							def myDev = settings.find {it.key == thisDev}
							if(myDev) if(myDev.value.size() > 1) getAnyAll(thisDev)
							if(xCapab in ["Temperature", "Humidity", "Illuminance"]) getRelational(thisDev)
						}
						getState(xCapab, i)
					}
				}
			}
		}
	}
}

def setActTrue(dev, str) {
	if(dev) {
        if(state.actsTrue != "") state.actsTrue = state.actsTrue + "\n"
        def i = str.indexOf('[')
        def myStr = str
        if(dev == true) state.actsTrue = state.actsTrue + str
        else {
        	def j = str.indexOf(']')
        	myStr = str.substring(i + 1, j) + str.substring(j + 1)
        	state.actsTrue = state.actsTrue + str.substring(0, i) + myStr
        }
    }
}

def setActFalse(dev, str) {
	if(dev) {
        if(state.actsFalse != "") state.actsFalse = state.actsFalse + "\n"
        def i = str.indexOf('[')
        def myStr = str
        if(dev == true) state.actsFalse = state.actsFalse + str
        else {
        	def j = str.indexOf(']')
        	myStr = str.substring(i + 1, j) + str.substring(j + 1)
        	state.actsFalse = state.actsFalse + str.substring(0, i) + myStr
        }
    }
}

def selectMsgTrue() {
	dynamicPage(name: "selectMsgTrue", title: "Select Message and Destination", uninstall: false) {
    	state.msgTrue = ""
    	section("") {
			input "pushTrue", "bool", title: "Send Push Notification?", required: false, submitOnChange: true
            if(pushTrue) state.msgTrue = state.msgTrue + "Push"
    		input "msgTrue", "text", title: "Custom message to send", required: false, submitOnChange: true
            if(msgTrue) state.msgTrue = state.msgTrue + state.msgTrue ? state.msgTrue + " and Send '$msgTrue'" : "Send '$msgTrue'"
            input "phoneTrue", "phone", title: "Phone number for SMS", required: false, submitOnChange: true
            if(phoneTrue) state.msgTrue = state.msgTrue + " to $phoneTrue"
        }
    }
}

def selectMsgFalse() {
	dynamicPage(name: "selectMsgFalse", title: "Select Message and Destination", uninstall: false) {
    	state.msgFalse = ""
    	section("") {
			input "pushFalse", "bool", title: "Send Push Notification?", required: false, submitOnChange: true
            if(pushFalse) state.msgFalse = state.msgFalse + "Push"
    		input "msgFalse", "text", title: "Custom message to send", required: false, submitOnChange: true
            if(msgFalse) state.msgFalse = state.msgFalse + state.False ? state.False + " and Send '$msgFalse'" : "Send '$msgFalse'"
            input "phoneFalse", "phone", title: "Phone number for SMS", required: false, submitOnChange: true
            if(phoneFalse) state.msgFalse = state.msgFalse + " to $phoneFalse"
        }
    }
}

def selectActionsTrue() {
	dynamicPage(name: "selectActionsTrue", title: "Select Actions for True", uninstall: false) {
		state.actsTrue = ""
		section("") {
			input "onSwitchTrue", "capability.switch", title: "Turn on these switches", multiple: true, required: false, submitOnChange: true
			setActTrue(onSwitchTrue, "On: $onSwitchTrue")
			input "offSwitchTrue", "capability.switch", title: "Turn off these switches", multiple: true, required: false, submitOnChange: true
			setActTrue(offSwitchTrue, "Off: $offSwitchTrue")
            input "delayedOffTrue", "capability.switch", title: "Turn off these switches after a delay", multiple: true, required: false, submitOnChange: true
            if(delayedOffTrue) input "delayMinutesTrue", "number", title: "Minutes of delay", required: true, range: "1..*", submitOnChange: true
            def delayStr = "Delayed Off: $delayedOffTrue: $delayMinutesTrue minute"
            if(delayMinutesTrue > 1) delayStr = "Delayed Off: $delayedOffTrue: $delayMinutesTrue minutes"
            setActTrue(delayedOffTrue, delayStr)
			input "dimATrue", "capability.switchLevel", title: "Set these dimmers", multiple: true, submitOnChange: true, required: false
			if(dimATrue) input "dimLATrue", "number", title: "To this level", range: "0..100", required: true, submitOnChange: true
			setActTrue(dimATrue, "Dim: $dimATrue: $dimLATrue")
			input "dimBTrue", "capability.switchLevel", title: "Set these other dimmers", multiple: true, submitOnChange: true, required: false
			if(dimBTrue) input "dimLBTrue", "number", title: "To this level", range: "0..100", required: true, submitOnChange: true
			setActTrue(dimBTrue, "Dim: $dimBTrue: $dimLBTrue")
			input "lockTrue", "capability.lock", title: "Lock these locks", multiple: true, required: false, submitOnChange: true
			setActTrue(lockTrue, "Lock: $lockTrue")
			input "unlockTrue", "capability.lock", title: "Unlock these locks", multiple: true, required: false, submitOnChange: true
			setActTrue(unlockTrue, "Unlock: $unlockTrue")
			input "modeTrue", "mode", title: "Set the mode", multiple: false, required: false, submitOnChange: true
			if(modeTrue) setActTrue(true, "Mode: $modeTrue")
			def phrases = location.helloHome?.getPhrases()*.label
			input "myPhraseTrue", "enum", title: "Routine to run", required: false, options: phrases.sort(), submitOnChange: true
			if(myPhraseTrue) setActTrue(true, "Routine: $myPhraseTrue")
            href "selectMsgTrue", title: "Send message", description: state.msgTrue ? state.msgTrue : "Tap to set", state: state.msgTrue ? "complete" : null
            if(state.actsTrue != "") state.actsTrue = state.actsTrue + "\n"
            state.actsTrue = state.actsTrue + state.msgTrue
		}
	}
}

def selectActionsFalse() {
	dynamicPage(name: "selectActionsFalse", title: "Select Actions for False", uninstall: false) {
		state.actsFalse = ""
		section("") {
			input "onSwitchFalse", "capability.switch", title: "Turn on these switches", multiple: true, required: false, submitOnChange: true
			setActFalse(onSwitchFalse, "On: $onSwitchFalse")
			input "offSwitchFalse", "capability.switch", title: "Turn off these switches", multiple: true, required: false, submitOnChange: true
			setActFalse(offSwitchFalse, "Off: $offSwitchFalse")
            input "delayedOffFalse", "capability.switch", title: "Turn off these switches after a delay", multiple: true, required: false, submitOnChange: true
            if(delayedOffFalse) input "delayMinutesFalse", "number", title: "Minutes of delay", required: true, range: "1..*", submitOnChange: true
            def delayStr = "Delayed Off: $delayedOffFalse: $delayMinutesFalse minute"
            if(delayMinutesFalse > 1) delayStr = "Delayed Off: $delayedOffFalse: $delayMinutesFalse minutes"
            setActFalse(delayedOffFalse, delayStr)
			input "dimAFalse", "capability.switchLevel", title: "Set these dimmers", multiple: true, submitOnChange: true, required: false
			if(dimAFalse) input "dimLAFalse", "number", title: "To this level", range: "0..100", required: true, submitOnChange: true
			setActFalse(dimAFalse, "Dim: $dimAFalse: $dimLAFalse")
			input "dimBFalse", "capability.switchLevel", title: "Set these other dimmers", multiple: true, submitOnChange: true, required: false
			if(dimBFalse) input "dimLBFalse", "number", title: "To this level", range: "0..100", required: true, submitOnChange: true
			setActFalse(dimBFalse, "Dim: $dimBFalse: $dimLBFalse")
			input "lockFalse", "capability.lock", title: "Lock these locks", multiple: true, required: false, submitOnChange: true
			setActFalse(lockFalse, "Lock: $lockFalse")
			input "unlockFalse", "capability.lock", title: "Unlock these locks", multiple: true, required: false, submitOnChange: true
			setActFalse(unlockFalse, "Unlock: $unlockFalse")
			input "modeFalse", "mode", title: "Set the mode", multiple: false, required: false, submitOnChange: true
			if(modeFalse) setActFalse(true, "Mode: $modeFalse")
			def phrases = location.helloHome?.getPhrases()*.label
			input "myPhraseFalse", "enum", title: "Routine to run", required: false, options: phrases.sort(), submitOnChange: true
			if(myPhraseFalse) setActFalse(true, "Routine: $myPhraseFalse")
            href "selectMsgFalse", title: "Send message", description: state.msgFalse ? state.msgFalse : "Tap to set", state: state.msgFalse ? "complete" : null
            if(state.actsFalse != "") state.actsFalse = state.actsFalse + "\n"
            state.actsFalse = state.actsFalse + state.msgFalse
		}
	}
}

def defineRule() {
	dynamicPage(name:"defineRule",title: "Define the Rule", uninstall: false) {
		state.n = 0
		state.str = ""
		state.eval = []
		section() {
		inputLeftAndRight(false)
		}
	}
}

def certainTime() {
	dynamicPage(name:"certainTime",title: "Only during a certain time", uninstall: false) {
		section() {
			input "startingX", "enum", title: "Starting at", options: ["A specific time", "Sunrise", "Sunset"], defaultValue: "A specific time", submitOnChange: true
			if(startingX in [null, "A specific time"]) input "starting", "time", title: "Start time", required: false
			else {
				if(startingX == "Sunrise") input "startSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
				else if(startingX == "Sunset") input "startSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
			}
		}
		
		section() {
			input "endingX", "enum", title: "Ending at", options: ["A specific time", "Sunrise", "Sunset"], defaultValue: "A specific time", submitOnChange: true
			if(endingX in [null, "A specific time"]) input "ending", "time", title: "End time", required: false
			else {
				if(endingX == "Sunrise") input "endSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
				else if(endingX == "Sunset") input "endSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
			}
		}
	}
}

def getDevs(myCapab, dev) {
    def thisName = ""
    def thisCapab = ""
	switch(myCapab) {
		case "Switch":
			thisName = "Switches"
			thisCapab = "switch"
			break
		case "Motion":
			thisName = "Motion sensors"
			thisCapab = "motionSensor"
			break
		case "Acceleration":
			thisName = "Acceleration sensors"
			thisCapab = "accelerationSensor"
			break        
		case "Contact":
			thisName = "Contact sensors"
			thisCapab = "contactSensor"
			break
		case "Presence":
			thisName = "Presence sensors"
			thisCapab = "presenceSensor"
			break
		case "Lock":
			thisName = "Locks"
			thisCapab = "lock"
			break
		case "Temperature":
			thisName = "Temperature sensors"
			thisCapab = "temperatureMeasurement"
			break
		case "Humidity":
			thisName = "Humidity sensors"
			thisCapab = "relativeHumidityMeasurement"
			break
		case "Illuminance":
			thisName = "Illuminance sensors"
			thisCapab = "illuminanceMeasurement"
	}
	def result = input dev, "capability.$thisCapab", title: thisName, required: true, multiple: true, submitOnChange: true
}

def getAnyAll(myDev) {
	def result = input "All$myDev", "bool", title: "All of these?", defaultValue: false
}

def getRelational(myDev) {
	def result = input "Rel$myDev", "enum", title: "Choose comparison", required: true, options: ["=", "!=", "<", ">", "<=", ">="]
}

def getCapab(myCapab) {
	def myOptions = ["Switch", "Motion", "Acceleration", "Contact", "Presence", "Lock", "Temperature", "Humidity", "Illuminance", "Time of day", "Days of week", "Mode"]
	def result = input myCapab, "enum", title: "Select capability", required: true, options: myOptions.sort(), submitOnChange: true
}

def getState(myCapab, n) {
	def result = null
	def days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
	if     (myCapab == "Switch") 		result = input "state$n", "enum", title: "Switch state", options: ["on", "off"]
	else if(myCapab == "Motion") 		result = input "state$n", "enum", title: "Motion state", options: ["active", "inactive"], defaultValue: "active"
	else if(myCapab == "Acceleration")	result = input "state$n", "enum", title: "Acceleration state", options: ["active", "inactive"]
	else if(myCapab == "Contact") 		result = input "state$n", "enum", title: "Contact state", options: ["open", "closed"]
	else if(myCapab == "Presence") 		result = input "state$n", "enum", title: "Presence state", options: ["present", "not present"], defaultValue: "present"
	else if(myCapab == "Lock") 			result = input "state$n", "enum", title: "Lock state", options: ["locked", "unlocked"]
	else if(myCapab == "Temperature") 	result = input "state$n", "number", title: "Temperature"
	else if(myCapab == "Humidity") 		result = input "state$n", "number", title: "Humidity"
	else if(myCapab == "Illuminance") 	result = input "state$n", "number", title: "Illuminance"
	else if(myCapab == "Mode") 			result = input "modes", "mode", title: "When mode is", multiple: true, required: false
	else if(myCapab == "Days of week") 	result = input "days", "enum", title: "On certain days of the week", multiple: true, required: false, options: days
	else if(myCapab == "Time of day") {
		def timeLabel = timeIntervalLabel()
		href "certainTime", title: "During a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
    } 
}

def inputLeft(sub) {
	def conds = []
	for (int i = 1; i <= howMany; i++) conds << conditionLabelN(i)
	input "subCondL$state.n", "bool", title: "Enter subrule for left?", submitOnChange: true
	if(settings["subCondL$state.n"]) {
		state.str = state.str + "("
		state.eval << "("
		paragraph(state.str)
		inputLeftAndRight(true)
		input "moreConds$state.n", "bool", title: "More conditions?", submitOnChange: true
		if(settings["moreConds$state.n"]) inputRight(sub)
	} else {
		input "condL$state.n", "enum", title: "Which condition?", options: conds, submitOnChange: true
		if(settings["condL$state.n"]) {
			state.str = state.str + settings["condL$state.n"]
			def myCond = 0
			for (int i = 1; i <= howMany; i++) if(conditionLabelN(i) == settings["condL$state.n"]) myCond = i
			state.eval << myCond
			paragraph(state.str)
		}
	}
}

def inputRight(sub) {
	state.n = state.n + 1
	input "operator$state.n", "enum", title: "Choose: AND,  OR,  Done", options: ["AND", "OR", "Done"], submitOnChange: true
	if(settings["operator$state.n"]) if(settings["operator$state.n"] != "Done") {
		state.str = state.str + " " + settings["operator$state.n"] + " "
		state.eval << settings["operator$state.n"]
		paragraph(state.str)
		def conds = []
		for (int i = 1; i <= howMany; i++) conds << conditionLabelN(i)
		input "subCondR$state.n", "bool", title: "Enter subrule for right?", submitOnChange: true
		if(settings["subCondR$state.n"]) {
			state.str = state.str + "("
			state.eval << "("
			paragraph(state.str)
			inputLeftAndRight(true)
			input "moreConds$state.n", "bool", title: "More conditions on right?", submitOnChange: true
			if(settings["moreConds$state.n"]) inputRight(sub)
		} else {
			input "condR$state.n", "enum", title: "Which condition?", options: conds, submitOnChange: true
			if(settings["condR$state.n"]) {
				state.str = state.str + settings["condR$state.n"]
				def myCond = 0
				for (int i = 1; i <= howMany; i++) if(conditionLabelN(i) == settings["condR$state.n"]) myCond = i
				state.eval << myCond
				paragraph(state.str)
                        }
                        if(sub) {
                                input "endOfSub$state.n", "bool", title: "End of subrule?", submitOnChange: true
                                if(settings["endOfSub$state.n"]) {
                                        state.str = state.str + ")"
                                        state.eval << ")"
                                        paragraph(state.str)
                                        return
                                }
                        }
                        input "moreConds$state.n", "bool", title: "More conditions on right?", submitOnChange: true
                        if(settings["moreConds$state.n"]) inputRight()
		}
	} 
}

def inputLeftAndRight(sub) {
	state.n = state.n + 1
	inputLeft(sub)
	inputRight(sub)
}

// initialization code below

def scheduleTimeOfDay() {
	def start = null
	def stop = null
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: startSunriseOffset, sunsetOffset: startSunsetOffset)
	if(startingX == "Sunrise") start = s.sunrise.time
	else if(startingX == "Sunset") start = s.sunset.time
	else if(starting) start = timeToday(starting,location.timeZone).time
	s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: endSunriseOffset, sunsetOffset: endSunsetOffset)
	if(endingX == "Sunrise") stop = s.sunrise.time
	else if(endingX == "Sunset") stop = s.sunset.time
	else if(ending) stop = timeToday(ending,location.timeZone).time
	schedule(start, "startHandler")
	schedule(stop, "stopHandler")
	if(startingX in ["Sunrise", "Sunset"] || endingX in ["Sunrise", "Sunset"])
		schedule("2015-01-09T00:00:29.000-0700", "scheduleTimeOfDay") // in case sunset/sunrise; change daily
}

def installed() {
	initialize()
}

def updated() {
	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	for (int i = 1; i <= howMany; i++) {
		def capab = (settings.find {it.key == "rCapab$i"}).value
		if     (capab == "Mode") subscribe(location, "mode", allHandler)
		else if(capab == "Time of day") scheduleTimeOfDay()
		else if(capab == "Days of week") schedule("2015-01-09T00:00:10.000-0700", "runRule")
		else subscribe((settings.find{it.key == "rDev$i"}).value, capab.toLowerCase(), allHandler)
	}
	state.success = null
    runRule()
}

// Main rule evaluation code follows

def compare(a, rel, b) {
	def result = true
	if     (rel == "=") 	result = a == b
	else if(rel == "!=") 	result = a != b
	else if(rel == ">") 	result = a > b
	else if(rel == "<") 	result = a < b
	else if(rel == ">=") 	result = a >= b
	else if(rel == "<=") 	result = a <= b
	return result
}

def checkCondAny(dev, state, cap, rel) {
	def result = false
	if     (cap == "Temperature") 	dev.currentTemperature.each {result = result || compare(it, rel, state)}
	else if(cap == "Humidity") 		dev.currentHumidity.each    {result = result || compare(it, rel, state)}
	else if(cap == "Illuminance") 	dev.currentIlluminance.each {result = result || compare(it, rel, state)}
	else if(cap == "Switch") 		result = state in dev.currentSwitch
	else if(cap == "Motion") 		result = state in dev.currentMotion
	else if(cap == "Acceleration") 	result = state in dev.currentAcceleration
	else if(cap == "Contact") 		result = state in dev.currentContact
	else if(cap == "Presence") 		result = state in dev.currentPresence
	else if(cap == "Lock") 			result = state in dev.currentLock
//	log.debug "CheckAny $cap $result"
	return result
}

def checkCondAll(dev, state, cap, rel) {
	def flip = ["on": "off",
				"off": "on",
                "active": "inactive",
                "inactive": "active",
                "open": "closed",
                "closed": "open",
                "present": "not present",
                "not present": "present",
                "locked": "unlocked",
                "unlocked": "locked"]
	def result = true
	if     (cap == "Temperature") 	dev.currentTemperature.each {result = result && compare(it, rel, state)}
	else if(cap == "Humidity") 		dev.currentHumidity.each    {result = result && compare(it, rel, state)}
	else if(cap == "Illuminance") 	dev.currentIlluminance.each {result = result && compare(it, rel, state)}
	else if(cap == "Switch") 		result = !(flip[state] in dev.currentSwitch)
	else if(cap == "Motion") 		result = !(flip[state] in dev.currentMotion)
	else if(cap == "Acceleration") 	result = !(flip[state] in dev.currentAcceleration)
	else if(cap == "Contact") 		result = !(flip[state] in dev.currentContact)
	else if(cap == "Presence") 		result = !(flip[state] in dev.currentPresence)
	else if(cap == "Lock") 			result = !(flip[state] in dev.currentLock)
//	log.debug "CheckAll $cap $result"
	return result
}

def getOperand(i) {
	def result = true
	def capab = (settings.find {it.key == "rCapab$i"}).value
	if     (capab == "Mode") result = modeOk
	else if(capab == "Time of day") result = timeOk
	else if(capab == "Days of week") result = daysOk
	else {
		def myDev = 	settings.find {it.key == "rDev$i"}
		def myState = 	settings.find {it.key == "state$i"}
		def myRel = 	settings.find {it.key == "RelrDev$i"}
		def myAll = 	settings.find {it.key == "AllrDev$i"}
		if(myAll) {
			if(myAll.value) result = checkCondAll(myDev.value, myState.value, capab, myRel ? myRel.value : 0)
			else result = checkCondAny(myDev.value, myState.value, capab, myRel ? myRel.value : 0)
		} else result = checkCondAny(myDev.value, myState.value, capab, myRel ? myRel.value : 0)
	}
//    log.debug "operand is $result"
	return result
}

def findRParen() {
	def noMatch = true
	while(noMatch) {
		if(state.eval[state.token] == ")") {
			if(state.parenLev == 0) return
			else state.parenLev = state.parenLev - 1
		} else if(state.eval[state.token] == "(") state.parenLev = state.parenLev + 1
		state.token = state.token + 1
		if(state.token >= state.eval.size) return
	}
}

def disEval() {
    if(state.eval[state.token] == "(") {
    	state.parenLev = 0
        findRParen()
    }
    if(state.token >= state.eval.size) return
    state.token = state.token + 1
}

def evalTerm() {
	def result = true
	def thisTok = state.eval[state.token]
//    log.debug "evalTerm tok is $thisTok"
	if (thisTok == "(") {
		state.token = state.token + 1
		result = eval()
	} else result = getOperand(thisTok)
	state.token = state.token + 1
//    log.debug "evalTerm is $result"
	return result
}

def eval() {
	def result = evalTerm()
	while(true) {
		if(state.token >= state.eval.size) return result
		def thisTok = state.eval[state.token]
//        log.debug "eval: $thisTok"
		if (thisTok == "OR") {
			if(result) {
				disEval()
				return true
			} 
		} else if (thisTok == "AND") {
			if(!result) {
				disEval()
				return false
			} 
		} else if (thisTok == ")") return result
		state.token = state.token + 1
		result = evalTerm()
	}
}

def runRule() {
	state.token = 0
	def success = eval()
	if(success != state.success) {
		if(success) {
                        if(onSwitchTrue) 	onSwitchTrue.on()
                        if(offSwitchTrue) 	offSwitchTrue.off()
                        if(delayedOffTrue)	runIn(delayMinutesTrue * 60, delayOffTrue)
                        if(dimATrue) 		dimATrue.setLevel(dimLATrue)
                        if(dimBTrue) 		dimBTrue.setLevel(dimLBTrue)
                        if(lockTrue) 		lockTrue.lock()
                        if(unlockTrue) 		unlockTrue.unlock()
                        if(modeTrue) 		setLocationMode(modeTrue)
						if(myPhraseTrue)	location.helloHome.execute(myPhraseTrue)
                        if(pushTrue)		sendPush(msgTrue ?: "Rule $app.label True")
                        if(phoneTrue)		sendSms(phoneTrue, msgTrue ?: "Rule $app.label True")
		} else {
                        if(onSwitchFalse) 	onSwitchFalse.on()
                        if(offSwitchFalse) 	offSwitchFalse.off()
						if(delayedOffFalse)	runIn(delayMinutesFalse * 60, delayOffFalse)
						if(dimAFalse) 		dimAFalse.setLevel(dimLAFalse)
                        if(dimBFalse) 		dimBFalse.setLevel(dimLBFalse)
                        if(lockFalse) 		lockFalse.lock()
                        if(unlockFalse) 	unlockFalse.unlock()
                        if(modeFalse) 		setLocationMode(modeFalse)
						if(myPhraseFalse) 	location.helloHome.execute(myPhraseFalse)
                        if(pushFalse)		sendPush(msgFalse ?: "Rule $app.label False")
                        if(phoneFalse)		sendSms(phoneFalse, msgFalse ?: "Rule $app.label False")
		}
		state.success = success
		log.debug (success ? "Success" : "Failure")
	}
}

def allHandler(evt) {
	log.debug "Handler: $evt.displayName $evt.name $evt.value"
	runRule()
}

def startHandler() {
	runRule()
}

def stopHandler() {
	runRule()
}

def delayOffTrue() {
	delayedOffTrue.off()
}

def delayOffFalse() {
	delayedOffFalse.off()
}

//  private execution filter methods below
private conditionLabel() {
	def result = ""
	if(howMany) {
		for (int i = 1; i <= howMany; i++) {
			result = result + conditionLabelN(i)
			if((i + 1) <= howMany) result = result + "\n"
		}
    }
	return result
}

private conditionLabelN(i) {
	def result = ""
        def thisCapab = settings.find {it.key == "rCapab$i"}
        if(!thisCapab) return result
        if(thisCapab.value == "Time of day") result = "Time between " + timeIntervalLabel()
        else if(thisCapab.value == "Days of week") result = "Day i" + (days.size() > 1 ? "n " + days : "s " + days[0])
        else if (thisCapab.value == "Mode") result = "Mode i" + (modes.size() > 1 ? "n " + modes : "s " + modes[0])
        else {
			def thisDev = settings.find {it.key == "rDev$i"}
            if(!thisDev) return result
			def thisAll = settings.find {it.key == "AllrDev$i"}
			def myAny = thisAll ? "any " : ""
			if(thisCapab.value == "Temperature") result = "Temperature of "
			else if(thisCapab.value == "Humidity") result = "Humidity of "
			else if(thisCapab.value == "Illuminance") result = "Illuminance of "
			result = result + (myAny ? thisDev.value : thisDev.value[0]) + " " + ((thisAll ? thisAll.value : false) ? "all " : myAny)
			def thisRel = settings.find {it.key == "RelrDev$i"}
			if(thisCapab.value in ["Temperature", "Humidity", "Illuminance"]) result = result + " " + thisRel.value + " "
			def thisState = settings.find {it.key == "state$i"}
			result = result + thisState.value
        }
	return result
}

private msgLabel() {
	def result = state.msgTrue
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private offset(value) {
	def result = value ? ((value > 0 ? "+" : "") + value + " min") : ""
}

private timeIntervalLabel() {
	def result = ""
	if (startingX == "Sunrise" && endingX == "Sunrise") result = "Sunrise" + offset(startSunriseOffset) + " and Sunrise" + offset(endSunriseOffset)
	else if (startingX == "Sunrise" && endingX == "Sunset") result = "Sunrise" + offset(startSunriseOffset) + " and Sunset" + offset(endSunsetOffset)
	else if (startingX == "Sunset" && endingX == "Sunrise") result = "Sunset" + offset(startSunsetOffset) + " and Sunrise" + offset(endSunriseOffset)
	else if (startingX == "Sunset" && endingX == "Sunset") result = "Sunset" + offset(startSunsetOffset) + " and Sunset" + offset(endSunsetOffset)
	else if (startingX == "Sunrise" && ending) result = "Sunrise" + offset(startSunriseOffset) + " and " + hhmm(ending, "h:mm a z")
	else if (startingX == "Sunset" && ending) result = "Sunset" + offset(startSunsetOffset) + " and " + hhmm(ending, "h:mm a z")
	else if (starting && endingX == "Sunrise") result = hhmm(starting) + " and Sunrise" + offset(endSunriseOffset)
	else if (starting && endingX == "Sunset") result = hhmm(starting) + " and Sunset" + offset(endSunsetOffset)
	else if (starting && ending) result = hhmm(starting) + " and " + hhmm(ending, "h:mm a z")
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
//	log.trace "modeOk = $result"
	return result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) df.setTimeZone(location.timeZone)
		else df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		def day = df.format(new Date())
		result = days.contains(day)
	}
//	log.trace "daysOk = $result"
	return result
}

private getTimeOk() {
	def result = true
	if ((starting && ending) ||
	(starting && endingX in ["Sunrise", "Sunset"]) ||
	(startingX in ["Sunrise", "Sunset"] && ending) ||
	(startingX in ["Sunrise", "Sunset"] && endingX in ["Sunrise", "Sunset"])) {
		def currTime = now()
		def start = null
		def stop = null
		def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: startSunriseOffset, sunsetOffset: startSunsetOffset)
		if(startingX == "Sunrise") start = s.sunrise.time
		else if(startingX == "Sunset") start = s.sunset.time
		else if(starting) start = timeToday(starting,location.timeZone).time
		s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: endSunriseOffset, sunsetOffset: endSunsetOffset)
		if(endingX == "Sunrise") stop = s.sunrise.time
		else if(endingX == "Sunset") stop = s.sunset.time
		else if(ending) stop = timeToday(ending,location.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
//	log.trace "getTimeOk = $result"
	return result
}