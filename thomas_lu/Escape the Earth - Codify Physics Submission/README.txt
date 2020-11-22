# Escape the Earth - Codify Physics 2020 Submission

## Description

In the distant 20xx, you, the earth's premier astronaut, are stranded on the moon with only one shot of fuel left in your space pod to get you off. But it's not enough to leave the moon, for the world is counting on you to complete your original mission: To depart from the earth's gravitational pull altogether! You have only 20 days to do so and must fine-tune your starting direction and velocity to utilize the gravity of the earth (and moon!), gaining enough energy to attain escape velocity. Good luck!

This program is an Android app written in java, compatible with Android 8.0 Oreo (API Level 26) and above. Its purpose is to present a fun, stimulating challenge, with wonderful underlying physics, that may be ordinarily relegated to a supercomputer. Euler integration is used to produce each step of the simulation, and real physical values for the earth and moon are used (with the exception of radius, which is scaled up so the earth and moon are visible). Other physics features implemented include collision detection (for which real radius values, not the scaled ones, are used), escape velocity calculations, and velocity and acceleration vectors.

This specific implementation is a game, but this program also provides a framework for future educational apps, including a platform for intuitively grasping different types of orbits and their energies and a playground for N-body orbit simulations.

## Set-up

To run the program, the escapetheearth.apk file can be installed on any Android device/emulator with Android 8.0 Oreo and above by accessing the .apk file through any app. Examples include Chrome, Google Drive, and Gmail. Be aware that permissions may need to be enabled for these apps to install "unknown apps" (i.e. the .apk)--to allow this, navigate to "Settings --> Apps & notifications --> Special app access --> Install unknown apps" and enable the appropriate permissions for each app.

The specific Android device (emulator) the author uses has a resolution of 1080x1920 and was obtained through the Android Studio IDE, though most models should work. If a device or emulator is not available, an alternative such as this chrome extension (https://chrome.google.com/webstore/detail/android-online-emulator/lnhnebkkgjmlgomfkkmkoaefbknopmja/related?hl=en) will serve as a non-ideal but approximate solution.

Further questions can be directed to 2021tlu@tjhsst.edu. Thank you for considering this submission!