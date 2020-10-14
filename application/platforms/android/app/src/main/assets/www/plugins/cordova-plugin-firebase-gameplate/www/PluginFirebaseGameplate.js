cordova.define("cordova-plugin-firebase-gameplate.PluginFirebaseGameplate", function(require, exports, module) {
// definisco il plugin cordova
cordova.define("cordova-plugin-firebase-gameplate.PluginFirebaseGameplate", function(require, exports, module) {

    var exec = require('cordova/exec');

    let saveScore = function(score) {

        // Chiamata asincrona attraverso *new Promise*
        return new Promise(function (resolve, reject) {
            // la funzione *exec* di cordova chiama i metodi dei plugin
            exec(function (response) {  // viene chiamata se lo stato della risposta è *OK*
                // La promessa viene risolta correttamente con la funzine *resolve*
                resolve(response);
            },
            // la promessa viene rigettata
            function(response) { // viene chiamata se lo stato della risposta è *ERROR*
                var error = "Error: "+response.code+"\nMessage: "+response.message+"\nDetails: "+response.details;
                reject(error);
            }, 
            // nome della CLASSE del plugin
            'PluginFirebaseGameplate',
            // nome del METODO del plugin
            'saveScore',
            // parametri da passare al metodo
            [
                score
            ]);
        });

    };

    let getBest = function() {
        return new Promise(function (resolve, reject) {
            exec(function (response) {
                resolve(response);
            },
            function(response) {
                var error = "Error: "+response.code+"\nMessage: "+response.message+"\nDetails: "+response.details;
                reject(error);
            }, 
            'PluginFirebaseGameplate',
            'getBest',
            []);
        });

    };

    let getRank = function() {
        return new Promise(function (resolve, reject) {
            exec(function (response) {
                resolve(response);
            },
            function(response) {
                var error = "Error: "+response.code+"\nMessage: "+response.message+"\nDetails: "+response.details;
                reject(error);
            }, 
            'PluginFirebaseGameplate',
            'getRank',
            []);
        });

    };

    // chiude il gioco
    let exit = function() {
        
        exec(null, null, 'Exit', 'exit', []);

    };

    if (typeof module !== undefined && module.exports) {
        module.exports.exit = exit;
        module.exports.getRank = getRank;
        module.exports.getBest = getBest;
        module.exports.saveScore = saveScore;
    }
});

});
