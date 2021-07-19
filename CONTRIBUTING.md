
# Contributing
Everyone can contribute to the development of the app, and that's great!. Since we'll be using [gitflow](https://datasift.github.io/gitflow/IntroducingGitFlow.html), it's recommended to have feature branches that will eventually be merged back to the `develop` via pull requests, and later `develop` will be merged back to master when we're ready for a new release.

## How to run the app 
For running this project locally follow this steps:
 - Clone the repository using git: 
	 - `$ git clone https://github.com/labexp/osmtracker-android/`
 - Install AndroidStudio 
	 - [Here](https://developer.android.com/studio/install) is the official guide on how to install it on different operating systems
 - Now open the IDE and click `open an existing Android Studio project`, then look for the folder where you cloned the repository 
 - Once it opens your project folder, click the top green hammer icon or press `Ctrl + F9` to make sure the project builds successfully 

## How to run the tests 
This repository has an automated way to run the tests on branches but if you already have the project installed on you computer then you can also run them from a terminal.
It's recommended to run the tests locally before making a new pull request to make sure the changes doesn't break any previous functionality. You can run the tests locally as follows:
 - Make sure you are at the *root directory of the project*
	 - `$ cd YOUR_PATH/osmtracker-android`
 - For running **instrumentation** tests it's needed to previously start up an emulator (or real device),  you can do it from Android Studio but also without it using the command line. For that,  you need to move to the Android SDK installation directory and look for a folder called `emulator` once there, start any already created emulator by typing:
	-	`$ ./emulator -avd NAME` to start the emulator called *NAME* (run `$ ./emulator -list-avds` for a valid list of AVD names)
	- When  it's up, go back to the root project folder and run the instrumentation tests with
	- `$ ./gradlew connectedAndroidTest`

 - For running the **unit tests** no emulator or device is needed, just run 
	 - `$ ./gradlew test`
 - Now just wait for gradle to run the tests for you, it'll show the results of which tests passed or failed when it's finished

## Translations
OSMTracker is translated using Transifex (see the [wiki](https://github.com/labexp/osmtracker-android/wiki/Translating)).
To update translations:
* Create a python3 virtual environment: `$python3 -m venv .venv` (.venv folder is already added to gitignore file). Remember the virtualenv activates with the command `$source .venv/bin/activate` and deactivates with `deactivate`. 
* Install Transifex CLI Client following the instructions in [this page](https://docs.transifex.com/client/installing-the-client#installing-on-macos-and-linux). 
* Execute `(.venv) $ tx pull --minimum-perc 1` to get the updated translations with at least 1% of completeness.
