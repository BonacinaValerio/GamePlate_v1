cordova.define('cordova/plugin_list', function(require, exports, module) {
  module.exports = [
    {
      "id": "cordova-plugin-firebase-gameplate.PluginFirebaseGameplate",
      "file": "plugins/cordova-plugin-firebase-gameplate/www/PluginFirebaseGameplate.js",
      "pluginId": "cordova-plugin-firebase-gameplate",
      "clobbers": [
        "PluginFirebaseGameplate"
      ]
    }
  ];
  module.exports.metadata = {
    "cordova-plugin-whitelist": "1.3.4",
    "cordova-plugin-firebase-gameplate": "0.0.1"
  };
});