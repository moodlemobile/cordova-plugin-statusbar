# Cordova Plugin Push (fork)

This is a fork of `cordova-plugin-statusbar` by [Moodle HQ](https://moodle.com/). If you are looking for the documentation, you can read the original at [apache/cordova-plugin-statusbar](https://github.com/apache/cordova-plugin-statusbar).

## Modifications from the original

We created this fork because we needed to include the following modifications in [our mobile application](https://github.com/moodlehq/moodleapp):

| PR | Description |
| -- | ----------- |
| - | Automatically calculate status bar color based on background color |
| - | Allow changing Android's bottom navigation bar color |
| https://github.com/moodlemobile/cordova-plugin-statusbar/pull/3 | Colors can no longer be changed in Android 15+, in that case the backgroundColorByHexString and the navigationBackgroundColorByHexString actions will only calculate the right text color based on the preferred background color. |


## Installation

You can install this package using the [original installation instructions](https://github.com/apache/cordova-plugin-statusbar#installation), but installing this package instead:

```sh
cordova plugin add @moodlehq/cordova-plugin-statusbar@4.0.0-moodle.4
```
