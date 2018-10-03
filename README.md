
# fxlabs-plugin

 To use fxlabs gradle plugin to invoke job include below script in build.gradle and replace jobid, username, pwd and region with your details from fxlabs and run the command

 "gradle fx" which invokes the build on fxlabs


plugins {
	id "io.fxlabs.job" version "0.2.8"
}



fx {

	jobId = "{fx_jobid}"
	username = "{fx_username}"
	password = "{fx-pwd}"
	region = "{bot_region}"

}

For latest plugin version go to  https://plugins.gradle.org/plugin/io.fxlabs.job
