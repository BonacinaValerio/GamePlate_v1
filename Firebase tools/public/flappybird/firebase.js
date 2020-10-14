var gameplate = cordova.require('cordova-plugin-firebase-gameplate.PluginFirebaseGameplate');

var bestScore;
var rank
var downloadRank;
var downloadRank_2;
var downloadBest;

function initGame() {
	downloadRank = "false";
	downloadRank_2 = "false";
	downloadBest = "false";
	rank = {
		global:[],
		weekly:[]
	};
}

// salva il punteggio della partita e estrai la classifica aggiornata
function saveScore(score) {
	gameplate.saveScore(score).then(function(result) {
		var arrayGlobal = result.global;
		rank.global = JSON.flatten(arrayGlobal).reverse().complete(3);
		var arrayWeekly = result.weekly;
		rank.weekly = JSON.flatten(arrayWeekly).reverse().complete(7);
		downloadRank = "true"
	}).catch(alert);
}

// estrai i punteggi migliori [*global, *weekly]
function getBest() {
	gameplate.getBest().then(function(result) {
		bestScore = [result.global, result.weekly];
		downloadBest = "true"
	}).catch(alert);
}

// estrai la classifica aggiornata
function getRank() {
	gameplate.getRank().then(function(result) {
		var arrayGlobal = result.global;
		rank.global = JSON.flatten(arrayGlobal).reverse().complete(3);
		var arrayWeekly = result.weekly;
		rank.weekly = JSON.flatten(arrayWeekly).reverse().complete(7);
		downloadRank_2 = "true"
	}).catch(alert);
}

// esci dal gioco
function exit() {
	gameplate.exit();
}

// appiattisci il JSON per chi ha fatto lo stesso punteggio e sistemalo con
// la struttura di array composto da [*nickname, *punteggio]
JSON.flatten = function(data) {
	var result = [];
	function recurse (cur, prop, last) {
		if (Object(cur) !== cur) 
			result.push([cur, prop]);
		else {
			for (var p in cur) {
				if (!last)
					recurse(cur[p], p, true);
				else
					recurse(cur[p], prop, true);
			}
		}
	}
	recurse(data, "");
	return result;
}

// completa l'array con campi di default oppure taglialo dagli eccessi
Array.prototype.complete= function(L){
	if (this.length<L) {
		for (var i = this.length; i < L; i++ ) {
			this[i] = ['-','-'];
		}
	}
	else if (this.length>L) {
		this.splice(L);
	}
	return this;
}